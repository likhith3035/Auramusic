package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"html"
	"html/template"
	"io"
	"log"
	"net"
	"net/http"
	"net/url"
	"sort"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"

	"github.com/KabirSinghBhatia/BitChord/backend/clock"
	"github.com/KabirSinghBhatia/BitChord/backend/codes"
	"github.com/KabirSinghBhatia/BitChord/backend/config"
	"github.com/KabirSinghBhatia/BitChord/backend/hub"
	"github.com/KabirSinghBhatia/BitChord/backend/party"
	"github.com/KabirSinghBhatia/BitChord/backend/presence"
	"github.com/KabirSinghBhatia/BitChord/backend/protocol"
)

var (
	store    = party.NewPartyStore()
	hubInst  = hub.NewHub()
	createLimiter = newIPRateLimiter(time.Minute, config.CreateRatePerMinute, config.RateLimitMaxEntries)
	presenceTracker = presence.NewTracker(
		time.Duration(config.PresenceIntervalSec+config.PresenceGraceSec)*time.Second,
		config.PresenceMaxEntries,
	)
	presenceLimiter = newIPRateLimiter(time.Minute, config.PresencePingsPerMinute, config.PresenceRateLimitMaxIPs)
	upgrader = websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool {
			origin := r.Header.Get("Origin")
			return origin == "" || config.IsAllowedOrigin(origin)
		},
	}
	totalPartiesCreated atomic.Int64
	totalChatMessages   atomic.Int64
)

func main() {
	go startHeartbeatTicker()
	go startPresenceSweeper()

	mux := http.NewServeMux()

	// REST endpoints
	mux.HandleFunc("GET /{$}", handleRoot)
	mux.HandleFunc("GET /healthz", handleHealthz)
	mux.HandleFunc("GET /api/time", handleTime)
	mux.HandleFunc("POST /api/parties", handleCreateParty)
	mux.HandleFunc("POST /api/parties/{code}/join", handleJoinParty)
	mux.HandleFunc("GET /api/parties/{code}", handleGetParty)
	mux.HandleFunc("GET /api/parties/{code}/preview", handlePreviewParty)
	mux.HandleFunc("POST /api/parties/{code}/leave", handleLeaveParty)

	// Open-app counter
	mux.HandleFunc("POST /api/presence", handlePresence)
	mux.HandleFunc("GET /api/stats/live", handleLiveStats)
	mux.HandleFunc("GET /api/stats/live/badge.svg", handleLiveBadge)
	mux.HandleFunc("GET /stats", handleStatsDashboard)

	// Web invite endpoint
	mux.HandleFunc("GET /invite/{code}", handleInviteLanding)

	// WebSocket endpoint
	mux.HandleFunc("GET /ws/parties/{code}", handleWebSocket)

	handler := corsMiddleware(mux)

	addr := fmt.Sprintf("0.0.0.0:%d", config.Port)
	log.Printf("BitChord Listen Together (Go) starting on %s...", addr)
	server := &http.Server{
		Addr:              addr,
		Handler:           handler,
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       20 * time.Second,
		WriteTimeout:      20 * time.Second,
		IdleTimeout:       60 * time.Second,
	}
	if err := server.ListenAndServe(); err != nil {
		log.Fatalf("Server stopped: %v", err)
	}
}

func corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		origin := r.Header.Get("Origin")
		if origin != "" && config.IsAllowedOrigin(origin) {
			w.Header().Set("Access-Control-Allow-Origin", origin)
			w.Header().Set("Vary", "Origin")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
		}
		if r.Method == "OPTIONS" {
			if origin != "" && !config.IsAllowedOrigin(origin) {
				http.Error(w, "origin not allowed", http.StatusForbidden)
				return
			}
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}

type ipRateLimiter struct {
	mu      sync.Mutex
	window  time.Duration
	limit   int
	maxKeys int
	entries map[string]ipRateEntry
	swept   time.Time
}

type ipRateEntry struct {
	started time.Time
	count   int
}

func newIPRateLimiter(window time.Duration, limit, maxKeys int) *ipRateLimiter {
	return &ipRateLimiter{window: window, limit: limit, maxKeys: maxKeys, entries: make(map[string]ipRateEntry)}
}

func (l *ipRateLimiter) Allow(ip string) bool {
	if l.limit <= 0 {
		return true
	}
	now := time.Now()
	l.mu.Lock()
	defer l.mu.Unlock()
	// Expired entries are also reset on lookup below, so a full sweep only needs
	// to run about once a second, or when the table is full and needs the room.
	if now.Sub(l.swept) >= time.Second || (l.maxKeys > 0 && len(l.entries) >= l.maxKeys) {
		for key, entry := range l.entries {
			if now.Sub(entry.started) >= l.window {
				delete(l.entries, key)
			}
		}
		l.swept = now
	}
	entry := l.entries[ip]
	if entry.started.IsZero() || now.Sub(entry.started) >= l.window {
		if entry.started.IsZero() && l.maxKeys > 0 && len(l.entries) >= l.maxKeys {
			return false
		}
		l.entries[ip] = ipRateEntry{started: now, count: 1}
		return true
	}
	if entry.count >= l.limit {
		return false
	}
	entry.count++
	l.entries[ip] = entry
	return true
}

func clientIP(r *http.Request) string {
	if config.TrustProxy {
		if forwarded := strings.TrimSpace(strings.Split(r.Header.Get("X-Forwarded-For"), ",")[0]); forwarded != "" {
			return forwarded
		}
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err == nil {
		return host
	}
	return r.RemoteAddr
}

func decodeJSONBody(w http.ResponseWriter, r *http.Request, dst interface{}) bool {
	r.Body = http.MaxBytesReader(w, r.Body, config.RequestMaxBytes)
	defer r.Body.Close()
	decoder := json.NewDecoder(r.Body)
	if err := decoder.Decode(dst); err != nil {
		var maxErr *http.MaxBytesError
		if errors.As(err, &maxErr) {
			jsonError(w, http.StatusRequestEntityTooLarge, "request_too_large", "Request body is too large.")
		} else {
			jsonError(w, http.StatusUnprocessableEntity, "invalid_json", "Malformed JSON body.")
		}
		return false
	}
	if err := decoder.Decode(&struct{}{}); err != io.EOF {
		jsonError(w, http.StatusUnprocessableEntity, "invalid_json", "Request body must contain one JSON object.")
		return false
	}
	return true
}

func jsonResponse(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(data)
}

func jsonError(w http.ResponseWriter, status int, errCode, msg string) {
	jsonResponse(w, status, map[string]string{
		"error":   errCode,
		"message": msg,
	})
}

func parseBearerToken(r *http.Request) string {
	auth := r.Header.Get("Authorization")
	parts := strings.SplitN(auth, " ", 2)
	if len(parts) == 2 && strings.EqualFold(parts[0], "Bearer") {
		return strings.TrimSpace(parts[1])
	}
	return ""
}

// REST Handlers

func handleRoot(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"service":    "bitchord-listen-together",
		"members":    store.MemberCount(),
		"parties":    store.Len(),
		"serverMs":   clock.NowMs(),
	})
}

func handleHealthz(w http.ResponseWriter, r *http.Request) {
	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"ok":       true,
		"serverMs": clock.NowMs(),
	})
}

func handleTime(w http.ResponseWriter, r *http.Request) {
	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"serverMs": clock.NowMs(),
	})
}

// handlePresence takes the open apps' heartbeat:
// {"id": "<install uuid>", "platform": "android"|"pc", "open": true|false}.
func handlePresence(w http.ResponseWriter, r *http.Request) {
	// Only the apps ping, and they send no Origin. Refusing browsers stops any web
	// page from making its visitors post made-up installs from their addresses.
	if r.Header.Get("Origin") != "" {
		jsonError(w, http.StatusForbidden, "origin_not_allowed", "Presence is only accepted from the apps.")
		return
	}
	if !presenceLimiter.Allow(clientIP(r)) {
		jsonError(w, http.StatusTooManyRequests, "rate_limited", "Too many presence pings from this address.")
		return
	}
	var body struct {
		ID       string `json:"id"`
		Platform string `json:"platform"`
		Open     *bool  `json:"open"`
	}
	if !decodeJSONBody(w, r, &body) {
		return
	}
	id, ok := presence.ParseID(body.ID)
	if !ok {
		jsonError(w, http.StatusUnprocessableEntity, "invalid_id", "id must be a UUID.")
		return
	}
	platform, ok := presence.ParsePlatform(body.Platform)
	if !ok {
		jsonError(w, http.StatusUnprocessableEntity, "invalid_platform", "platform must be android or pc.")
		return
	}
	if body.Open == nil {
		jsonError(w, http.StatusUnprocessableEntity, "invalid_open", "open must be true or false.")
		return
	}
	if *body.Open {
		presenceTracker.Ping(id, platform, clock.NowMs())
	} else {
		presenceTracker.Close(id)
	}
	jsonResponse(w, http.StatusOK, map[string]int{"intervalSec": config.PresenceIntervalSec})
}

// handleLiveStats is public on purpose: anyone, including a README badge or
// another site's script, may read how many apps are open.
func handleLiveStats(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Cache-Control", "public, max-age=15")
	jsonResponse(w, http.StatusOK, presenceTracker.Counts())
}

// handleLiveBadge draws the README's "listening now" badge itself. Going
// through shields.io meant its 2-5 minute cache stacked on GitHub's camo
// proxy, so the README trailed the real count by minutes. no-cache here is
// what camo honours, so every README view gets the current number.
//
// The geometry copies shields' for-the-badge render byte for byte: a fixed
// 124.25px label, bold Verdana digits at 8.25px each, 12px padding a side.
func handleLiveBadge(w http.ResponseWriter, r *http.Request) {
	value := fmt.Sprint(presenceTracker.Counts().Online)
	valueWidth := 8.25*float64(len(value)) + 24
	width := 124.25 + valueWidth
	svg := fmt.Sprintf(`<svg xmlns="http://www.w3.org/2000/svg" width="%g" height="28" role="img" aria-label="LISTENING NOW: %s"><title>LISTENING NOW: %s</title><g shape-rendering="crispEdges"><rect width="124.25" height="28" fill="#0d1117"/><rect x="124.25" width="%g" height="28" fill="#fb4f67"/></g><g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="100"><text transform="scale(.1)" x="621.25" y="175" textLength="1002.5">LISTENING NOW</text><text transform="scale(.1)" x="%g" y="175" textLength="%g" font-weight="bold">%s</text></g></svg>`,
		width, value, value, valueWidth, (124.25+valueWidth/2)*10, 82.5*float64(len(value)), value)
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Content-Type", "image/svg+xml;charset=utf-8")
	w.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(svg))
}

type statsPageData struct {
	Online        int
	Android       int
	PC            int
	ActiveParties int
	PartyMembers  int
	TotalParties  int64
	TotalChats    int64
	ServerTime    string
}

var statsTemplate = template.Must(template.New("stats").Parse(`<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AuraMusic Analytics • Live Telemetry & Listener Insights</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #07070a;
            --surface: rgba(18, 18, 28, 0.72);
            --surface-hover: rgba(26, 26, 40, 0.85);
            --border: rgba(255, 255, 255, 0.08);
            --border-hover: rgba(167, 139, 250, 0.35);
            --primary: #a78bfa;
            --primary-glow: rgba(167, 139, 250, 0.25);
            --cyan: #38bdf8;
            --emerald: #10b981;
            --rose: #f43f5e;
            --text-main: #f8fafc;
            --text-muted: #94a3b8;
            --text-dim: #64748b;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            background-color: var(--bg);
            color: var(--text-main);
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 32px 16px;
            position: relative;
            overflow-x: hidden;
        }

        /* Ambient glowing orbs */
        .ambient-glow {
            position: fixed;
            width: 500px;
            height: 500px;
            border-radius: 50%;
            pointer-events: none;
            filter: blur(140px);
            opacity: 0.18;
            z-index: 0;
            animation: floatGlow 18s ease-in-out infinite alternate;
        }
        .glow-1 { top: -100px; left: -100px; background: radial-gradient(circle, #8b5cf6, transparent); }
        .glow-2 { bottom: -120px; right: -100px; background: radial-gradient(circle, #06b6d4, transparent); animation-delay: -9s; }
        .glow-3 { top: 40%; left: 50%; transform: translate(-50%, -50%); width: 600px; height: 600px; background: radial-gradient(circle, #6366f1, transparent); opacity: 0.10; }

        @keyframes floatGlow {
            0% { transform: scale(1) translate(0, 0); }
            50% { transform: scale(1.15) translate(30px, -20px); }
            100% { transform: scale(0.95) translate(-20px, 30px); }
        }

        .container {
            position: relative;
            z-index: 1;
            background: var(--surface);
            backdrop-filter: blur(36px);
            -webkit-backdrop-filter: blur(36px);
            border: 1px solid var(--border);
            border-radius: 32px;
            max-width: 680px;
            width: 100%;
            padding: 36px 32px;
            box-shadow: 0 30px 80px rgba(0, 0, 0, 0.75), 0 0 80px rgba(139, 92, 246, 0.08);
            transition: border-color 0.3s ease;
        }
        .container:hover {
            border-color: rgba(255, 255, 255, 0.12);
        }

        /* Top Bar & Wave Visualizer */
        .header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 24px;
            padding-bottom: 24px;
            border-bottom: 1px solid var(--border);
            gap: 16px;
            flex-wrap: wrap;
        }

        .branding {
            display: flex;
            align-items: center;
            gap: 14px;
        }

        .logo-icon {
            width: 48px;
            height: 48px;
            border-radius: 16px;
            background: linear-gradient(135deg, #7c3aed 0%, #3b82f6 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 8px 24px rgba(124, 58, 237, 0.4);
            flex-shrink: 0;
        }
        .logo-icon svg {
            width: 26px;
            height: 26px;
            fill: #ffffff;
        }

        .title-group h1 {
            font-size: 23px;
            font-weight: 800;
            letter-spacing: -0.6px;
            background: linear-gradient(135deg, #ffffff 30%, #c4b5fd 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            line-height: 1.2;
        }
        .title-group p {
            font-size: 13px;
            color: var(--text-muted);
            margin-top: 3px;
            font-weight: 500;
        }

        .header-badges {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .live-indicator {
            display: inline-flex;
            align-items: center;
            gap: 7px;
            background: rgba(16, 185, 129, 0.12);
            color: #34d399;
            font-size: 12px;
            font-weight: 700;
            padding: 7px 14px;
            border-radius: 9999px;
            border: 1px solid rgba(16, 185, 129, 0.25);
            letter-spacing: 0.5px;
        }
        .pulse-dot {
            width: 8px;
            height: 8px;
            background: #10b981;
            border-radius: 50%;
            box-shadow: 0 0 12px #10b981;
            animation: radarPulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
        }
        @keyframes radarPulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.4; transform: scale(0.85); }
        }

        .ping-pill {
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--border);
            color: var(--text-muted);
            font-size: 11px;
            font-family: 'JetBrains Mono', monospace;
            padding: 6px 12px;
            border-radius: 9999px;
            display: inline-flex;
            align-items: center;
            gap: 5px;
        }
        .ping-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: var(--emerald);
        }

        /* Soundwave Canvas Banner */
        .wave-banner {
            position: relative;
            background: linear-gradient(135deg, rgba(124, 58, 237, 0.14) 0%, rgba(56, 189, 248, 0.08) 100%);
            border: 1px solid rgba(139, 92, 246, 0.25);
            border-radius: 22px;
            padding: 18px 22px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 24px;
            overflow: hidden;
        }
        .canvas-bg {
            position: absolute;
            right: 0;
            top: 0;
            bottom: 0;
            width: 240px;
            pointer-events: none;
            opacity: 0.45;
        }
        .banner-info {
            position: relative;
            z-index: 2;
        }
        .banner-info h3 {
            font-size: 15px;
            font-weight: 700;
            color: #e2e8f0;
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .banner-info p {
            font-size: 12px;
            color: #94a3b8;
            margin-top: 3px;
            font-family: 'JetBrains Mono', monospace;
        }
        .sync-btn {
            position: relative;
            z-index: 2;
            background: rgba(139, 92, 246, 0.22);
            hover-background: rgba(139, 92, 246, 0.35);
            border: 1px solid rgba(167, 139, 250, 0.3);
            color: #c4b5fd;
            font-size: 12px;
            font-weight: 600;
            padding: 7px 14px;
            border-radius: 12px;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s ease;
        }
        .sync-btn:hover {
            background: rgba(139, 92, 246, 0.38);
            border-color: rgba(167, 139, 250, 0.6);
            transform: translateY(-1px);
        }
        .sync-btn.spinning svg {
            animation: spin 0.8s linear infinite;
        }
        @keyframes spin { 100% { transform: rotate(360deg); } }

        /* Metric Grid */
        .grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
            margin-bottom: 24px;
        }
        @media (max-width: 580px) {
            .grid { grid-template-columns: 1fr; }
            .container { padding: 24px 18px; }
        }

        .card {
            background: rgba(255, 255, 255, 0.025);
            border: 1px solid var(--border);
            border-radius: 22px;
            padding: 22px 24px;
            position: relative;
            transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
            overflow: hidden;
        }
        .card::before {
            content: "";
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 2px;
            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
            opacity: 0;
            transition: opacity 0.3s ease;
        }
        .card:hover {
            background: var(--surface-hover);
            border-color: var(--border-hover);
            transform: translateY(-3px);
            box-shadow: 0 12px 30px rgba(0, 0, 0, 0.4), 0 0 20px var(--primary-glow);
        }
        .card:hover::before { opacity: 1; }

        .card-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;
        }
        .card-label {
            font-size: 11px;
            font-weight: 700;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 1.2px;
        }
        .card-icon {
            width: 32px;
            height: 32px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .card-icon svg { width: 17px; height: 17px; }
        .icon-purple { background: rgba(167, 139, 250, 0.15); fill: #a78bfa; }
        .icon-cyan { background: rgba(56, 189, 248, 0.15); fill: #38bdf8; }
        .icon-emerald { background: rgba(16, 185, 129, 0.15); fill: #34d399; }
        .icon-rose { background: rgba(244, 63, 94, 0.15); fill: #fb7185; }

        .card-value {
            font-size: 38px;
            font-weight: 800;
            color: #ffffff;
            font-family: 'JetBrains Mono', monospace;
            letter-spacing: -1px;
            line-height: 1.1;
        }
        .card-sub {
            font-size: 13px;
            color: var(--text-muted);
            margin-top: 8px;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        /* Platform Breakdown Meter inside Card 1 */
        .platform-meter {
            margin-top: 12px;
            padding-top: 10px;
            border-top: 1px solid rgba(255, 255, 255, 0.05);
        }
        .meter-bar {
            height: 6px;
            background: rgba(255, 255, 255, 0.08);
            border-radius: 9999px;
            overflow: hidden;
            display: flex;
            margin-bottom: 6px;
        }
        .meter-android {
            background: linear-gradient(90deg, #10b981, #059669);
            width: 50%;
            transition: width 0.6s ease;
        }
        .meter-pc {
            background: linear-gradient(90deg, #6366f1, #3b82f6);
            width: 50%;
            transition: width 0.6s ease;
        }
        .meter-labels {
            display: flex;
            justify-content: space-between;
            font-size: 11px;
            color: var(--text-dim);
            font-family: 'JetBrains Mono', monospace;
        }

        /* Footer */
        .footer {
            text-align: center;
            font-size: 12px;
            color: var(--text-muted);
            line-height: 1.7;
            border-top: 1px solid var(--border);
            padding-top: 22px;
            display: flex;
            flex-direction: column;
            gap: 6px;
        }
        .footer-credits {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            flex-wrap: wrap;
        }
        .footer a {
            color: var(--primary);
            text-decoration: none;
            font-weight: 600;
            transition: color 0.2s ease;
        }
        .footer a:hover {
            color: #c4b5fd;
            text-decoration: underline;
        }
        .footer-sub {
            font-size: 11px;
            color: var(--text-dim);
            font-family: 'JetBrains Mono', monospace;
        }
    </style>
</head>
<body>
    <div class="ambient-glow glow-1"></div>
    <div class="ambient-glow glow-2"></div>
    <div class="ambient-glow glow-3"></div>

    <div class="container">
        <!-- Header -->
        <div class="header">
            <div class="branding">
                <div class="logo-icon">
                    <svg viewBox="0 0 24 24"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
                </div>
                <div class="title-group">
                    <h1>AuraMusic Analytics</h1>
                    <p>Live Telemetry & Global Listener Insights</p>
                </div>
            </div>
            <div class="header-badges">
                <div class="ping-pill">
                    <span class="ping-dot"></span>
                    <span id="ping-text">-- ms</span>
                </div>
                <div class="live-indicator">
                    <span class="pulse-dot"></span>
                    <span>LIVE</span>
                </div>
            </div>
        </div>

        <!-- Banner with animated sound wave -->
        <div class="wave-banner">
            <canvas id="wave-canvas" class="canvas-bg" width="240" height="70"></canvas>
            <div class="banner-info">
                <h3>AuraMusic v3.0 by Kami Likhith</h3>
                <p>Render Service: auramusic-kjov • <span id="last-updated">Updated just now</span></p>
            </div>
            <button class="sync-btn" id="refresh-btn" onclick="fetchLiveStats(true)">
                <svg id="refresh-icon" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/></svg>
                Sync
            </button>
        </div>

        <!-- Metric Grid -->
        <div class="grid">
            <!-- Listening Now -->
            <div class="card">
                <div class="card-top">
                    <span class="card-label">Listening Now</span>
                    <div class="card-icon icon-purple">
                        <svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 0 0-9 9v7c0 1.1.9 2 2 2h4v-8H5v-1a7 7 0 0 1 14 0v1h-4v8h4c1.1 0 2-.9 2-2v-7a9 9 0 0 0-9-9z"/></svg>
                    </div>
                </div>
                <div class="card-value" id="val-online">{{.Online}}</div>
                <div class="card-sub" id="sub-devices">
                    <span id="val-android">{{.Android}}</span> Android • <span id="val-pc">{{.PC}}</span> PC
                </div>
                <div class="platform-meter">
                    <div class="meter-bar">
                        <div class="meter-android" id="bar-android"></div>
                        <div class="meter-pc" id="bar-pc"></div>
                    </div>
                    <div class="meter-labels">
                        <span id="pct-android">Android</span>
                        <span id="pct-pc">Desktop PC</span>
                    </div>
                </div>
            </div>

            <!-- Active Parties -->
            <div class="card">
                <div class="card-top">
                    <span class="card-label">Active Parties</span>
                    <div class="card-icon icon-cyan">
                        <svg viewBox="0 0 24 24"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
                    </div>
                </div>
                <div class="card-value" id="val-parties">{{.ActiveParties}}</div>
                <div class="card-sub"><span id="val-members">{{.PartyMembers}}</span> listeners in session</div>
            </div>

            <!-- Parties Hosted -->
            <div class="card">
                <div class="card-top">
                    <span class="card-label">Parties Hosted</span>
                    <div class="card-icon icon-emerald">
                        <svg viewBox="0 0 24 24"><path d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94A5.01 5.01 0 0 0 11 15.9V19H7v2h10v-2h-4v-3.1c1.86-.48 3.25-1.99 3.61-3.96C19.08 11.63 21 9.55 21 7V5c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z"/></svg>
                    </div>
                </div>
                <div class="card-value" id="val-total-parties">{{.TotalParties}}</div>
                <div class="card-sub">Total rooms created</div>
            </div>

            <!-- Party Messages -->
            <div class="card">
                <div class="card-top">
                    <span class="card-label">Party Messages</span>
                    <div class="card-icon icon-rose">
                        <svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z"/></svg>
                    </div>
                </div>
                <div class="card-value" id="val-total-chats">{{.TotalChats}}</div>
                <div class="card-sub">Real-time chat & reactions</div>
            </div>
        </div>

        <!-- Footer -->
        <div class="footer">
            <div class="footer-credits">
                AuraMusic Engine • Developed by <a href="https://devlikhith.vercel.app/" target="_blank">Kami Likhith</a> • <a href="https://github.com/likhith3035/Auramusic" target="_blank">GitHub</a>
            </div>
            <div class="footer-sub">
                Server Time: <span id="server-time">{{.ServerTime}}</span>
            </div>
        </div>
    </div>

    <script>
        // Smooth Number Animation
        function animateValue(id, start, end, duration) {
            if (start === end) return;
            const el = document.getElementById(id);
            if (!el) return;
            const range = end - start;
            let current = start;
            const increment = end > start ? 1 : -1;
            const stepTime = Math.abs(Math.floor(duration / (range || 1)));
            const timer = setInterval(function() {
                current += increment;
                el.textContent = current;
                if (current == end) {
                    clearInterval(timer);
                }
            }, Math.max(stepTime, 20));
        }

        // Platform breakdown bar updater
        function updatePlatformMeter(android, pc) {
            const total = android + pc;
            const barAndroid = document.getElementById('bar-android');
            const barPc = document.getElementById('bar-pc');
            const pctAndroid = document.getElementById('pct-android');
            const pctPc = document.getElementById('pct-pc');

            if (!barAndroid || !barPc) return;

            if (total === 0) {
                barAndroid.style.width = '50%';
                barPc.style.width = '50%';
                pctAndroid.textContent = 'Android 0%';
                pctPc.textContent = 'PC 0%';
                return;
            }

            const aPct = Math.round((android / total) * 100);
            const pPct = 100 - aPct;
            barAndroid.style.width = aPct + '%';
            barPc.style.width = pPct + '%';
            pctAndroid.textContent = 'Android ' + aPct + '%';
            pctPc.textContent = 'PC ' + pPct + '%';
        }

        let lastFetchTime = Date.now();

        async function fetchLiveStats(manual = false) {
            const btn = document.getElementById('refresh-btn');
            if (manual && btn) btn.classList.add('spinning');
            const t0 = performance.now();

            try {
                const res = await fetch('/stats?format=json', {
                    headers: { 'Accept': 'application/json' }
                });
                const t1 = performance.now();
                const ping = Math.round(t1 - t0);
                const pingText = document.getElementById('ping-text');
                if (pingText) pingText.textContent = ping + ' ms';

                if (res.ok) {
                    const data = await res.json();
                    
                    const curOnline = parseInt(document.getElementById('val-online').textContent) || 0;
                    animateValue('val-online', curOnline, data.online, 400);

                    const curParties = parseInt(document.getElementById('val-parties').textContent) || 0;
                    animateValue('val-parties', curParties, data.activeParties, 400);

                    const curTotalParties = parseInt(document.getElementById('val-total-parties').textContent) || 0;
                    animateValue('val-total-parties', curTotalParties, data.totalParties, 400);

                    const curChats = parseInt(document.getElementById('val-total-chats').textContent) || 0;
                    animateValue('val-total-chats', curChats, data.totalChats, 400);

                    document.getElementById('val-android').textContent = data.android;
                    document.getElementById('val-pc').textContent = data.pc;
                    document.getElementById('val-members').textContent = data.partyMembers;
                    if (data.serverTime) {
                        document.getElementById('server-time').textContent = data.serverTime;
                    }

                    updatePlatformMeter(data.android, data.pc);
                    lastFetchTime = Date.now();
                    const updatedEl = document.getElementById('last-updated');
                    if (updatedEl) updatedEl.textContent = 'Updated just now';
                }
            } catch (e) {
                console.error('Failed to sync live stats:', e);
            } finally {
                if (btn) btn.classList.remove('spinning');
            }
        }

        // Relative timestamp ticker
        setInterval(() => {
            const el = document.getElementById('last-updated');
            if (!el) return;
            const sec = Math.floor((Date.now() - lastFetchTime) / 1000);
            if (sec < 4) {
                el.textContent = 'Updated just now';
            } else {
                el.textContent = 'Updated ' + sec + 's ago';
            }
        }, 3000);

        // Soundwave Canvas Animation
        const canvas = document.getElementById('wave-canvas');
        if (canvas) {
            const ctx = canvas.getContext('2d');
            const numBars = 28;
            let phase = 0;

            function drawWave() {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
                const barWidth = canvas.width / numBars - 2;

                for (let i = 0; i < numBars; i++) {
                    const hFactor = Math.sin(phase + i * 0.35) * 0.5 + 0.5;
                    const barHeight = Math.max(6, hFactor * (canvas.height * 0.85));
                    const x = i * (barWidth + 2);
                    const y = (canvas.height - barHeight) / 2;

                    const grad = ctx.createLinearGradient(0, y, 0, y + barHeight);
                    grad.addColorStop(0, 'rgba(167, 139, 250, 0.7)');
                    grad.addColorStop(1, 'rgba(56, 189, 248, 0.2)');

                    ctx.fillStyle = grad;
                    ctx.beginPath();
                    ctx.roundRect(x, y, barWidth, barHeight, 3);
                    ctx.fill();
                }

                phase += 0.055;
                requestAnimationFrame(drawWave);
            }
            drawWave();
        }

        // Initialize platform meter on load
        const initA = parseInt('{{.Android}}') || 0;
        const initP = parseInt('{{.PC}}') || 0;
        updatePlatformMeter(initA, initP);

        // Auto-refresh every 5 seconds seamlessly
        setInterval(() => fetchLiveStats(false), 5000);
        // Initial ping check
        setTimeout(() => fetchLiveStats(false), 800);
    </script>
</body>
</html>`))

func handleStatsDashboard(w http.ResponseWriter, r *http.Request) {
	counts := presenceTracker.Counts()
	activeParties := store.Len()
	partyMembers := store.MemberCount()

	if r.URL.Query().Get("format") == "json" || strings.Contains(r.Header.Get("Accept"), "application/json") {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		jsonResponse(w, http.StatusOK, map[string]interface{}{
			"online":        counts.Online,
			"android":       counts.Android,
			"pc":            counts.PC,
			"activeParties": activeParties,
			"partyMembers":  partyMembers,
			"totalParties":  totalPartiesCreated.Load(),
			"totalChats":    totalChatMessages.Load(),
			"serverTime":    time.Now().UTC().Format("2006-01-02 15:04:05 UTC"),
		})
		return
	}

	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_ = statsTemplate.Execute(w, statsPageData{
		Online:        counts.Online,
		Android:       counts.Android,
		PC:            counts.PC,
		ActiveParties: activeParties,
		PartyMembers:  partyMembers,
		TotalParties:  totalPartiesCreated.Load(),
		TotalChats:    totalChatMessages.Load(),
		ServerTime:    time.Now().UTC().Format("2006-01-02 15:04:05 UTC"),
	})
}

func handleCreateParty(w http.ResponseWriter, r *http.Request) {
	if !createLimiter.Allow(clientIP(r)) {
		jsonError(w, http.StatusTooManyRequests, "create_rate_limited", "You can create up to two parties per minute. Please try again shortly.")
		return
	}
	var req protocol.JoinRequest
	if !decodeJSONBody(w, r, &req) {
		return
	}
	if err := req.Validate(); err != nil {
		jsonError(w, http.StatusUnprocessableEntity, "validation_error", err.Error())
		return
	}

	maxMembers := 5
	if req.MaxMembers != nil { maxMembers = *req.MaxMembers }
	p, err := store.CreateWithLimits(config.MaxParties, maxMembers)
	if err != nil {
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusInternalServerError, "server_error", "Failed to create party.")
		return
	}
	totalPartiesCreated.Add(1)
	// This must be present in the creation response. Waiting for the first
	// WebSocket control can lose the setting before that socket is established.
	if req.AutoplayEnabled != nil {
		p.Playback.AutoplayEnabled = *req.AutoplayEnabled
	}

	p.Lock()
	m, err := p.Join(req.UserId, req.DeviceId, req.DisplayName, req.AvatarUrl)
	if err != nil {
		p.Unlock()
		store.Drop(p.Code)
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusInternalServerError, "server_error", err.Error())
		return
	}
	partyWire := p.ToWire()
	youWire := m.ToWire()
	token := m.Token
	code := p.Code
	p.Unlock()

	jsonResponse(w, http.StatusCreated, map[string]interface{}{
		"code":  code,
		"token": token,
		"you":   youWire,
		"party": partyWire,
	})
}

func handleJoinParty(w http.ResponseWriter, r *http.Request) {
	code := r.PathValue("code")
	var req protocol.JoinRequest
	if !decodeJSONBody(w, r, &req) {
		return
	}
	if err := req.Validate(); err != nil {
		jsonError(w, http.StatusUnprocessableEntity, "validation_error", err.Error())
		return
	}

	p, err := store.Get(code)
	if err != nil {
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusNotFound, "no_such_party", "No party with that code.")
		return
	}

	p.Lock()
	m, err := p.Join(req.UserId, req.DeviceId, req.DisplayName, req.AvatarUrl)
	if err != nil {
		p.Unlock()
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusInternalServerError, "server_error", err.Error())
		return
	}
	partyWire := p.ToWire()
	youWire := m.ToWire()
	token := m.Token
	pCode := p.Code
	p.Unlock()

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"code":  pCode,
		"token": token,
		"you":   youWire,
		"party": partyWire,
	})
}

// handlePreviewParty answers who is in a party, without a token and without
// joining it.
//
// Deliberately unauthenticated: the whole point is to let somebody who has been
// handed a code see who they would be joining before they commit a device slot
// to it. What it discloses — display names, avatars, how full the party is — is
// exactly what joining would disclose a second later, and anyone holding a code
// can join. What it does not disclose is what the party is playing, its queue,
// member or user ids, or anything that would let a caller act on the party.
//
// Not rate-limited beyond the service-wide limits: it takes no locks it does
// not release, allocates a short slice, and creates nothing.
func handlePreviewParty(w http.ResponseWriter, r *http.Request) {
	code := r.PathValue("code")
	p, err := store.Get(code)
	if err != nil {
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusNotFound, "no_such_party", "No party with that code.")
		return
	}

	p.Lock()
	// Joined order, as the snapshot uses: the caller draws the first few faces
	// and counts the rest, so which faces those are must not change between two
	// reads of an unchanged party.
	ordered := make([]*party.Member, 0, len(p.Members))
	for _, m := range p.Members {
		ordered = append(ordered, m)
	}
	sort.Slice(ordered, func(i, j int) bool {
		return ordered[i].JoinedAtMs < ordered[j].JoinedAtMs
	})
	members := make([]map[string]interface{}, 0, len(ordered))
	hostName := ""
	for _, m := range ordered {
		members = append(members, map[string]interface{}{
			"displayName": m.DisplayName,
			"avatarUrl":   m.AvatarUrl,
			"isHost":      m.IsHost,
		})
		if m.IsHost {
			hostName = m.DisplayName
		}
	}
	preview := map[string]interface{}{
		"code":        p.Code,
		"hostName":    hostName,
		"memberCount": len(p.Members),
		"maxMembers":  p.MaxMembers,
		"isFull":      len(p.Members) >= p.MaxMembers,
		"members":     members,
	}
	p.Unlock()

	jsonResponse(w, http.StatusOK, preview)
}

func handleGetParty(w http.ResponseWriter, r *http.Request) {
	code := r.PathValue("code")
	token := parseBearerToken(r)
	if token == "" {
		jsonError(w, http.StatusUnauthorized, "unauthorized", "Missing bearer token.")
		return
	}

	p, err := store.Get(code)
	if err != nil {
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusNotFound, "no_such_party", "No party with that code.")
		return
	}

	p.Lock()
	_, err = p.Authenticate(token)
	if err != nil {
		p.Unlock()
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusUnauthorized, "unauthorized", "Bad token.")
		return
	}
	partyWire := p.ToWire()
	p.Unlock()

	jsonResponse(w, http.StatusOK, partyWire)
}

func handleLeaveParty(w http.ResponseWriter, r *http.Request) {
	code := r.PathValue("code")
	token := parseBearerToken(r)
	if token == "" {
		jsonError(w, http.StatusUnauthorized, "unauthorized", "Missing bearer token.")
		return
	}

	p, err := store.Get(code)
	if err != nil {
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusNotFound, "no_such_party", "No party with that code.")
		return
	}

	p.Lock()
	member, err := p.Authenticate(token)
	if err != nil {
		p.Unlock()
		if pe, ok := err.(*party.PartyError); ok {
			jsonError(w, pe.Status, pe.Code, pe.Message)
			return
		}
		jsonError(w, http.StatusUnauthorized, "unauthorized", "Bad token.")
		return
	}

	memberId := member.MemberId
	p.Remove(memberId)
	membersFrame := membersFrame(p)
	p.Unlock()

	// Notify room and drop leaving member socket
	hubInst.Send(p.Code, memberId, map[string]interface{}{
		"type":   protocol.FrameBye,
		"reason": "left",
	})
	hubInst.Broadcast(p.Code, membersFrame, memberId)

	jsonResponse(w, http.StatusOK, map[string]bool{"ok": true})
}

// Web Invite Landing Handler

type invitePageData struct {
	Code              string
	DeepLink          string
	IntentURI         template.URL
	SafeDeepLink      template.URL
	ServerOrigin      string
	CurrentSongTitle  string
	CurrentSongArtist string
	MemberCount       int
	IsActive          bool
}

var inviteTemplate = template.Must(template.New("invite").Parse(`<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AuraMusic Listen Together - Party {{.Code}}</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: #0b0b0e;
            color: #f3f3f7;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }
        .container {
            background: rgba(22, 22, 30, 0.85);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 28px;
            max-width: 440px;
            width: 100%;
            padding: 36px 28px;
            text-align: center;
            box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5), 0 0 40px rgba(124, 77, 255, 0.1);
        }
        .badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(124, 77, 255, 0.15);
            color: #b388ff;
            font-size: 13px;
            font-weight: 600;
            padding: 6px 14px;
            border-radius: 9999px;
            margin-bottom: 20px;
            letter-spacing: 0.5px;
        }
        .badge-dot {
            width: 8px;
            height: 8px;
            background: #00e676;
            border-radius: 50%;
            box-shadow: 0 0 8px #00e676;
        }
        .badge-dot.offline {
            background: #ff5252;
            box-shadow: 0 0 8px #ff5252;
        }
        h1 {
            font-size: 24px;
            font-weight: 700;
            margin-bottom: 8px;
            letter-spacing: -0.5px;
        }
        .subtitle {
            color: #9e9ea7;
            font-size: 15px;
            line-height: 1.5;
            margin-bottom: 28px;
        }
        .code-box {
            background: rgba(255, 255, 255, 0.04);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 18px;
            padding: 16px 20px;
            margin-bottom: 28px;
        }
        .code-label {
            font-size: 12px;
            color: #71717a;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            margin-bottom: 6px;
        }
        .code-val {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            font-size: 32px;
            font-weight: 800;
            letter-spacing: 6px;
            color: #ffffff;
        }
        .now-playing {
            font-size: 14px;
            color: #d1d1d6;
            margin-top: 10px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .btn {
            display: block;
            width: 100%;
            padding: 16px 24px;
            background: linear-gradient(135deg, #7c4dff 0%, #3d5afe 100%);
            color: #ffffff;
            font-size: 16px;
            font-weight: 600;
            text-decoration: none;
            border-radius: 16px;
            border: none;
            cursor: pointer;
            transition: transform 0.15s ease, opacity 0.15s ease;
            box-shadow: 0 8px 24px rgba(124, 77, 255, 0.35);
        }
        .btn:hover {
            transform: translateY(-1px);
            opacity: 0.95;
        }
        .btn:active {
            transform: scale(0.98);
        }
        .footer-note {
            margin-top: 24px;
            font-size: 13px;
            color: #71717a;
            line-height: 1.5;
        }
        .footer-note a {
            color: #b388ff;
            text-decoration: none;
        }
        .footer-note a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="container">
        {{if .IsActive}}
            <div class="badge">
                <span class="badge-dot"></span>
                <span>Listen Together • {{.MemberCount}} in party</span>
            </div>
            <h1>Join the Music Party</h1>
            <p class="subtitle">Opening AuraMusic by Likhith to sync playback in real time.</p>
            <div class="code-box">
                <div class="code-label">Party Code</div>
                <div class="code-val">{{.Code}}</div>
                {{if .CurrentSongTitle}}
                    <div class="now-playing">🎵 {{.CurrentSongTitle}} - {{.CurrentSongArtist}}</div>
                {{end}}
            </div>
            <a id="joinBtn" href="{{.IntentURI}}" class="btn">Join Party in AuraMusic</a>
            <p class="footer-note">
                Didn’t open automatically? Tap the button above.<br>
                Don't have AuraMusic yet? <a href="https://github.com/likhith3035/Auramusic/releases" target="_blank" rel="noopener">Download AuraMusic here</a>.
            </p>
            <script>
                var intentUri = {{.IntentURI}};
                var deepLink = {{.SafeDeepLink}};
                function launch() {
                    if (/Android/i.test(navigator.userAgent)) {
                        window.location.href = intentUri;
                    } else {
                        window.location.href = deepLink;
                    }
                }
                setTimeout(launch, 100);
            </script>
        {{else}}
            <div class="badge">
                <span class="badge-dot offline"></span>
                <span>Party Inactive</span>
            </div>
            <h1>Party Not Found</h1>
            <p class="subtitle">This party code has expired or does not exist on this server.</p>
            <div class="code-box">
                <div class="code-label">Party Code</div>
                <div class="code-val" style="color: #a1a1aa;">{{.Code}}</div>
            </div>
            <p class="footer-note">
                Please ask the host for a new invite link or check your server configuration.
            </p>
        {{end}}
    </div>
</body>
</html>`))

func requestOrigin(r *http.Request) string {
	if config.PublicOrigin != "" {
		return config.PublicOrigin
	}
	proto := "http"
	if r.TLS != nil {
		proto = "https"
	}
	if config.TrustProxy {
		if forwardedProto := r.Header.Get("X-Forwarded-Proto"); forwardedProto != "" {
			proto = strings.TrimSpace(strings.Split(forwardedProto, ",")[0])
		}
	}
	host := r.Host
	if host == "" {
		host = "localhost"
	}
	return fmt.Sprintf("%s://%s", proto, host)
}

func handleInviteLanding(w http.ResponseWriter, r *http.Request) {
	code := codes.Normalise(r.PathValue("code"))
	origin := requestOrigin(r)
	w.Header().Set("Content-Type", "text/html; charset=utf-8")

	if len(code) != codes.CodeLength {
		w.WriteHeader(http.StatusBadRequest)
		_ = inviteTemplate.Execute(w, invitePageData{
			Code:     html.EscapeString(r.PathValue("code")),
			IsActive: false,
		})
		return
	}

	p := store.Find(code)
	if p == nil {
		w.WriteHeader(http.StatusNotFound)
		_ = inviteTemplate.Execute(w, invitePageData{
			Code:     code,
			IsActive: false,
		})
		return
	}

	deepLink := fmt.Sprintf("auramusic://party/%s?server=%s", url.PathEscape(code), url.QueryEscape(origin))
	intentURI := fmt.Sprintf("intent://party/%s?server=%s#Intent;scheme=auramusic;package=com.music.bitchord;S.browser_fallback_url=https://github.com/likhith3035/Auramusic/releases;end", url.PathEscape(code), url.QueryEscape(origin))

	currentSongTitle := ""
	currentSongArtist := ""
	p.Lock()
	if p.Playback != nil && p.Playback.Track != nil {
		currentSongTitle = p.Playback.Track.Title
		currentSongArtist = p.Playback.Track.Artist
	}
	memberCount := len(p.Members)
	p.Unlock()

	w.WriteHeader(http.StatusOK)
	_ = inviteTemplate.Execute(w, invitePageData{
		Code:              code,
		DeepLink:          deepLink,
		IntentURI:         template.URL(intentURI),
		SafeDeepLink:      template.URL(deepLink),
		ServerOrigin:      origin,
		CurrentSongTitle:  currentSongTitle,
		CurrentSongArtist: currentSongArtist,
		MemberCount:       memberCount,
		IsActive:          true,
	})
}

// WebSocket Handler

func handleWebSocket(w http.ResponseWriter, r *http.Request) {
	code := r.PathValue("code")
	token := parseBearerToken(r)
	if token == "" {
		http.Error(w, "missing token", http.StatusUnauthorized)
		return
	}

	p, err := store.Get(code)
	if err != nil {
		http.Error(w, "no such party", http.StatusNotFound)
		return
	}

	p.Lock()
	member, err := p.Authenticate(token)
	if err != nil {
		p.Unlock()
		http.Error(w, "bad token", http.StatusUnauthorized)
		return
	}
	p.Unlock()

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("WebSocket upgrade failed: %v", err)
		return
	}
	conn.SetReadLimit(config.WebSocketMaxBytes)
	_ = conn.SetReadDeadline(time.Now().Add(time.Duration(config.ConnectionIdleMs) * time.Millisecond))

	p.Lock()
	p.MarkConnected(member, true)
	p.Unlock()

	sc := hubInst.Attach(p.Code, member.MemberId, conn)

	// Send welcome frame
	p.Lock()
	welcome := map[string]interface{}{
		"type":     protocol.FrameWelcome,
		"you":      member.ToWire(),
		"party":    p.ToWire(),
		"serverMs": clock.NowMs(),
	}
	membersF := membersFrame(p)
	p.Unlock()

	_ = sc.WriteJSON(welcome)
	hubInst.Broadcast(p.Code, membersF, "")

	// Read loop
	defer func() {
		hubInst.Detach(p.Code, member.MemberId, sc)
		_ = sc.Close()

		p.Lock()
		p.MarkConnected(member, false)
		mFrame := membersFrame(p)
		p.Unlock()

		hubInst.Broadcast(p.Code, mFrame, "")
	}()

	for {
		var raw map[string]interface{}
		if err := conn.ReadJSON(&raw); err != nil {
			break
		}
		_ = conn.SetReadDeadline(time.Now().Add(time.Duration(config.ConnectionIdleMs) * time.Millisecond))
		handleSocketFrame(p, member, sc, raw)
	}
}

func handleSocketFrame(p *party.Party, member *party.Member, sc *hub.SafeConn, frame map[string]interface{}) {
	kind, _ := frame["type"].(string)
	if kind == "" {
		return
	}

	p.Lock()
	defer p.Unlock()
	if !p.SpendFrameBudget(member) {
		_ = sc.WriteJSON(map[string]interface{}{
			"type": protocol.FrameError, "error": "rate_limited", "message": "Too many messages at once.",
		})
		return
	}

	member.LastSeenMs = clock.NowMs()

	switch kind {
	case protocol.FramePing:
		_ = sc.WriteJSON(map[string]interface{}{
			"type":     protocol.FramePong,
			"clientMs": frame["clientMs"],
			"serverMs": clock.NowMs(),
		})

	case protocol.FrameSync:
		_ = sc.WriteJSON(stateFrame(p))

	case protocol.FrameSyncQueue:
		_ = sc.WriteJSON(queueFrame(p))

	case protocol.FrameReport:
		// Playhead position report from client for monitoring
		if posNum, ok := frame["positionMs"].(float64); ok && p.Playback.IsPlaying {
			drift := int64(posNum) - p.Playback.PositionAt(clock.NowMs())
			if drift > 1500 || drift < -1500 {
				log.Printf("party %s: %s drifted %dms", p.Code, member.DisplayName, drift)
			}
		}

	case protocol.FrameControl:
		if !p.SpendControlBudget(member) {
			_ = sc.WriteJSON(map[string]interface{}{
				"type":    protocol.FrameError,
				"error":   "rate_limited",
				"message": "Too many controls at once.",
			})
			return
		}

		queueBefore := p.Playback.QueueSeq
		action, _ := frame["action"].(string)
		success, errCode, errMsg := applyControl(p, member, action, frame)
		if success {
			p.Touch()
			// If queue changed, broadcast queue first
			if p.Playback.QueueSeq != queueBefore {
				hubInst.Broadcast(p.Code, queueFrame(p), "")
			}
			hubInst.Broadcast(p.Code, stateFrame(p), "")
			if action == protocol.ActionKick ||
				action == protocol.ActionSetMaxMembers ||
				action == protocol.ActionSetHostOnlyControl {
				hubInst.Broadcast(p.Code, membersFrame(p), "")
			}
			hubInst.Broadcast(p.Code, activityFrame(member, action, frame), "")
		} else if errCode != "" {
			_ = sc.WriteJSON(map[string]interface{}{
				"type":    protocol.FrameError,
				"error":   errCode,
				"message": errMsg,
			})
		}

	case protocol.FrameChat:
		// Check for Democracy Queue auto-enqueue on upvoted songs
		if songMap, ok := frame["song"].(map[string]interface{}); ok && songMap != nil {
			autoQueued, _ := frame["autoQueued"].(bool)
			upvotes, _ := frame["upvotes"].(float64)
			if int(upvotes) >= 2 && !autoQueued {
				track := party.TrackFromWire(map[string]interface{}{
					"videoId":      songMap["videoId"],
					"title":        songMap["title"],
					"artist":       songMap["artist"],
					"thumbnailUrl": songMap["coverUrl"],
					"durationMs":   songMap["durationSec"],
				})
				if track != nil {
					ok, _ := p.Playback.AddUpcoming(&member.MemberId, []*party.Track{track}, false)
					if ok {
						frame["autoQueued"] = true
						p.Touch()
						hubInst.Broadcast(p.Code, queueFrame(p), "")
						hubInst.Broadcast(p.Code, stateFrame(p), "")
						act := activityFrame(member, "democracy_queue", map[string]interface{}{
							"detail": fmt.Sprintf("Auto-queued \"%s\" (%d upvotes)", track.Title, int(upvotes)),
						})
						hubInst.Broadcast(p.Code, act, "")
					}
				}
			}
		}
		// Broadcast in-party chat message / song suggestion to all connected members
		totalChatMessages.Add(1)
		hubInst.Broadcast(p.Code, frame, "")

	case protocol.FrameReaction:
		reactionEmoji, _ := frame["emoji"].(string)
		if reactionEmoji != "" {
			totalChatMessages.Add(1)
			hubInst.Broadcast(p.Code, map[string]interface{}{
				"type":        protocol.FrameReaction,
				"emoji":       reactionEmoji,
				"senderId":    member.MemberId,
				"senderName":  member.DisplayName,
				"timestampMs": clock.NowMs(),
			}, "")
		}

	case protocol.FrameSoundbite:
		sound, _ := frame["sound"].(string)
		if sound != "" {
			totalChatMessages.Add(1)
			hubInst.Broadcast(p.Code, map[string]interface{}{
				"type":        protocol.FrameSoundbite,
				"sound":       sound,
				"senderId":    member.MemberId,
				"senderName":  member.DisplayName,
				"timestampMs": clock.NowMs(),
			}, "")
		}

	case protocol.FrameVoteSkip:
		active, needed, reached := p.ToggleSkipVote(member.MemberId)
		skipStateFrame := map[string]interface{}{
			"type":        protocol.FrameVoteSkip,
			"activeVotes": active,
			"totalNeeded": needed,
			"voterId":     member.MemberId,
		}
		hubInst.Broadcast(p.Code, skipStateFrame, "")
		if reached {
			// Room reached majority (>50%) veto threshold! Skip to next track
			applyControl(p, member, protocol.ActionNext, frame)
			p.ClearSkipVotes()
			p.Touch()
			hubInst.Broadcast(p.Code, stateFrame(p), "")
		}

	case protocol.FrameSyncEq:
		// Broadcast host soundstage & visualizer theme
		hubInst.Broadcast(p.Code, frame, "")
	}
}

func applyControl(p *party.Party, member *party.Member, action string, frame map[string]interface{}) (bool, string, string) {
	// Enforced here rather than left to the clients. An app that hides its own
	// next button is a courtesy; this is what actually stops a listener's
	// device — a stale build, a backgrounded one still echoing an old intent,
	// or something else entirely — from moving the music for everybody.
	if protocol.ControlActions[action] && !p.MayControl(member) {
		return false, "host_only", "Only the host can control the music in this party."
	}

	var posPtr *int64
	if pos, ok := frame["positionMs"].(float64); ok {
		pVal := int64(pos)
		posPtr = &pVal
	}

	switch action {
	case protocol.ActionPlay:
		p.Playback.Play(&member.MemberId, posPtr)
		return true, "", ""

	case protocol.ActionPause:
		p.Playback.Pause(&member.MemberId, posPtr)
		return true, "", ""

	case protocol.ActionSeek:
		if posPtr == nil {
			return false, "missing_position", "seek requires positionMs"
		}
		p.Playback.Seek(&member.MemberId, *posPtr)
		return true, "", ""

	case protocol.ActionSetTrack:
		trackMap, _ := frame["track"].(map[string]interface{})
		t := party.TrackFromWire(trackMap)
		if t == nil {
			return false, "invalid_track", "Track must include videoId"
		}
		pos := int64(0)
		if posPtr != nil {
			pos = *posPtr
		}
		isPlaying := true
		if pVal, ok := frame["isPlaying"].(bool); ok {
			isPlaying = pVal
		}
		var qIndexPtr *int
		if qIndex, ok := frame["queueIndex"].(float64); ok {
			qi := int(qIndex)
			qIndexPtr = &qi
		}
		p.Playback.SetTrack(&member.MemberId, t, pos, isPlaying, qIndexPtr, &member.DisplayName)
		p.ClearSkipVotes()
		return true, "", ""

	case protocol.ActionSetQueue:
		itemsRaw, _ := frame["queue"].([]interface{})
		var tracks []*party.Track
		for _, item := range itemsRaw {
			if m, ok := item.(map[string]interface{}); ok {
				if tr := party.TrackFromWire(m); tr != nil {
					tracks = append(tracks, tr)
				}
			}
		}
		qIdx := -1
		if idxNum, ok := frame["queueIndex"].(float64); ok {
			qIdx = int(idxNum)
		}
		p.Playback.SetQueue(&member.MemberId, tracks, qIdx)
		return true, "", ""

	case protocol.ActionQueueAdd:
		var tracksToAdd []*party.Track
		if singleMap, ok := frame["track"].(map[string]interface{}); ok {
			if tr := party.TrackFromWire(singleMap); tr != nil {
				tracksToAdd = append(tracksToAdd, tr)
			}
		} else if itemsRaw, ok := frame["tracks"].([]interface{}); ok {
			for _, item := range itemsRaw {
				if m, ok := item.(map[string]interface{}); ok {
					if tr := party.TrackFromWire(m); tr != nil {
						tracksToAdd = append(tracksToAdd, tr)
					}
				}
			}
		}
		playNext, _ := frame["playNext"].(bool)
		ok, reason := p.Playback.AddUpcoming(&member.MemberId, tracksToAdd, playNext)
		if !ok {
			if reason == "queue_full" {
				return false, "queue_full", fmt.Sprintf("Queue is full (maximum %d upcoming songs).", config.MaxUpcomingQueue)
			}
			return false, reason, "Could not add track to queue."
		}
		return true, "", ""

	case protocol.ActionQueueRemove:
		vid, _ := frame["videoId"].(string)
		if vid == "" {
			if idxNum, ok := frame["index"].(float64); ok {
				idx := int(idxNum)
				if idx >= 0 && idx < len(p.Playback.Queue) {
					vid = p.Playback.Queue[idx].VideoId
				}
			}
		}
		if vid == "" {
			return false, "missing_track", "queueRemove requires videoId or index"
		}
		if !p.Playback.RemoveUpcoming(&member.MemberId, vid) {
			return false, "not_found", "Track is not in the upcoming queue"
		}
		return true, "", ""

	case protocol.ActionQueueClear:
		if !p.Playback.ClearUpcoming(&member.MemberId) {
			return false, "no_upcoming", "No upcoming tracks to clear"
		}
		return true, "", ""

	case protocol.ActionQueueMove:
		fromNum, okFrom := frame["fromIndex"].(float64)
		toNum, okTo := frame["toIndex"].(float64)
		if !okFrom || !okTo {
			return false, "missing_indices", "queueMove requires fromIndex and toIndex"
		}
		var videoId string
		if v, ok := frame["videoId"].(string); ok {
			videoId = v
		}
		if !p.Playback.MoveUpcoming(&member.MemberId, int(fromNum), int(toNum), videoId) {
			return false, "invalid_move", "Invalid queue move indices"
		}
		return true, "", ""

	case protocol.ActionNext:
		if !p.Playback.Step(&member.MemberId, 1, &member.DisplayName) {
			return false, "end_of_queue", "Already at the end of the queue."
		}
		p.ClearSkipVotes()
		return true, "", ""

	case protocol.ActionPrevious:
		if !p.Playback.Step(&member.MemberId, -1, &member.DisplayName) {
			return false, "start_of_queue", "Already at the beginning of the queue."
		}
		p.ClearSkipVotes()
		return true, "", ""

	case protocol.ActionSetMaxMembers:
		value, ok := frame["maxMembers"].(float64)
		if !ok { return false, "invalid_capacity", "Choose a party size between 2 and 10." }
		if err := p.SetMaxMembers(member, int(value)); err != nil {
			pe := err.(*party.PartyError)
			return false, pe.Code, pe.Message
		}
		return true, "", ""

	case protocol.ActionSetAutoplay:
		enabled, ok := frame["enabled"].(bool)
		if !ok {
			return false, "invalid_autoplay", "AutoPlay must be enabled or disabled."
		}
		p.Playback.SetAutoplay(&member.MemberId, enabled)
		return true, "", ""

	case protocol.ActionSetHostOnlyControl:
		enabled, ok := frame["enabled"].(bool)
		if !ok {
			return false, "invalid_control_policy", "Host-only control must be enabled or disabled."
		}
		if err := p.SetHostOnlyControl(member, enabled); err != nil {
			pe := err.(*party.PartyError)
			return false, pe.Code, pe.Message
		}
		return true, "", ""

	case protocol.ActionKick:
		targetID, _ := frame["memberId"].(string)
		if !member.IsHost { return false, "host_only", "Only the host can remove listeners." }
		if targetID == "" || targetID == member.MemberId { return false, "invalid_member", "Choose another listener to remove." }
		if p.Remove(targetID) == nil { return false, "not_found", "That listener is no longer in this party." }
		hubInst.Send(p.Code, targetID, map[string]interface{}{
			"type":    protocol.FrameBye,
			"reason":  "kicked",
			"message": "The host removed you from this party.",
		})
		hubInst.CloseMember(p.Code, targetID)
		return true, "", ""

	default:
		return false, "unknown_action", fmt.Sprintf("Unknown control action '%s'", action)
	}
}

// Frame builders

func stateFrame(p *party.Party) map[string]interface{} {
	return map[string]interface{}{
		"type":     protocol.FrameState,
		"playback": p.Playback.ToWire(clock.NowMs()),
		"serverMs": clock.NowMs(),
	}
}

func activityFrame(member *party.Member, action string, frame map[string]interface{}) map[string]interface{} {
	detail := action
	trackTitle := func(raw interface{}) string {
		track, _ := raw.(map[string]interface{})
		title, _ := track["title"].(string)
		return title
	}
	switch action {
	case protocol.ActionSetTrack:
		if title := trackTitle(frame["track"]); title != "" { detail = "Changed the song to \"" + title + "\"" }
	case protocol.ActionQueueAdd:
		if tracks, ok := frame["tracks"].([]interface{}); ok && len(tracks) > 0 {
			title := trackTitle(tracks[0])
			if title != "" {
				if len(tracks) == 1 { detail = "Added \"" + title + "\" to the queue" } else { detail = fmt.Sprintf("Added \"%s\" and %d more to the queue", title, len(tracks)-1) }
			}
		}
	case protocol.ActionSetQueue:
		if tracks, ok := frame["queue"].([]interface{}); ok { detail = fmt.Sprintf("Replaced the queue with %d tracks", len(tracks)) }
	case protocol.ActionQueueRemove: detail = "Removed a track from the queue"
	case protocol.ActionQueueClear: detail = "Cleared upcoming tracks"
	case protocol.ActionQueueMove: detail = "Reordered the upcoming queue"
	case protocol.ActionNext: detail = "Skipped to the next song"
	case protocol.ActionPrevious: detail = "Went back to the previous song"
	case protocol.ActionPlay: detail = "Started playback"
	case protocol.ActionPause: detail = "Paused playback"
	case protocol.ActionSeek: detail = "Changed the playback position"
	case protocol.ActionKick: detail = "Removed a listener from the party"
	case protocol.ActionSetMaxMembers: detail = "Changed the party size"
	case protocol.ActionSetAutoplay: detail = "Changed AutoPlay"
	}
	return map[string]interface{}{"type": protocol.FrameActivity, "action": action, "by": member.DisplayName, "detail": detail, "atMs": clock.NowMs()}
}

func queueFrame(p *party.Party) map[string]interface{} {
	return map[string]interface{}{
		"type":     protocol.FrameQueue,
		"queue":    p.Playback.QueueToWire(),
		"serverMs": clock.NowMs(),
	}
}

func membersFrame(p *party.Party) map[string]interface{} {
	membersList := make([]map[string]interface{}, 0, len(p.Members))
	for _, m := range p.Members {
		membersList = append(membersList, m.ToWire())
	}
	return map[string]interface{}{
		"type":            protocol.FrameMembers,
		"members":         membersList,
		"maxMembers":      p.MaxMembers,
		"hostOnlyControl": p.HostOnlyControl,
		"serverMs":        clock.NowMs(),
	}
}

// Background Heartbeat Ticker

// startPresenceSweeper refreshes the live counts. A pass over 10k installs is
// well under a millisecond, and stats reads never wait on it.
func startPresenceSweeper() {
	ticker := time.NewTicker(15 * time.Second)
	defer ticker.Stop()
	for range ticker.C {
		presenceTracker.Sweep(clock.NowMs())
	}
}

func startHeartbeatTicker() {
	interval := time.Duration(config.StateHeartbeatMs) * time.Millisecond
	if interval < 1*time.Second {
		interval = 1 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for range ticker.C {
		now := clock.NowMs()
		for _, p := range store.All() {
			if len(hubInst.MembersOnline(p.Code)) > 0 {
				p.Lock()
				sf := stateFrame(p)
				p.Unlock()
				hubInst.Broadcast(p.Code, sf, "")
			}
		}

		changed := store.Sweep(now)
		for _, p := range changed {
			p.Lock()
			mf := membersFrame(p)
			p.Unlock()
			hubInst.Broadcast(p.Code, mf, "")
		}

		activeCodes := hubInst.ActiveCodes()
		liveParties := store.All()
		liveMap := make(map[string]bool)
		for _, lp := range liveParties {
			liveMap[lp.Code] = true
		}
		for _, code := range activeCodes {
			if !liveMap[code] {
				hubInst.DropParty(code)
			}
		}
	}
}
