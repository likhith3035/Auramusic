const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const DATA_FILE = path.join(__dirname, 'stats.json');

app.use(cors());
app.use(express.json());

// Load or initialize stats database
let db = {
  allTimeUsers: {},
  daily: {}, // { "2026-11-11": { total: 0, users: [], versions: {}, platforms: {} } }
  recentEvents: []
};

function loadData() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const raw = fs.readFileSync(DATA_FILE, 'utf8');
      db = JSON.parse(raw);
    }
  } catch (err) {
    console.error('Error loading data file:', err);
  }
}

function saveData() {
  try {
    fs.writeFileSync(DATA_FILE, JSON.stringify(db, null, 2), 'utf8');
  } catch (err) {
    console.error('Error writing data file:', err);
  }
}

loadData();

function getTodayKey() {
  return new Date().toISOString().slice(0, 10);
}

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'AuraMusic Telemetry', timestamp: new Date().toISOString() });
});

// JSON API endpoint for stats
app.get('/api/stats', (req, res) => {
  const today = getTodayKey();
  const todayStats = db.daily[today] || { total: 0, versions: {}, platforms: {} };
  const allTimeCount = Object.keys(db.allTimeUsers || {}).length;

  res.json({
    activeToday: todayStats.total || 0,
    allTimeDevices: allTimeCount,
    todayBreakdown: todayStats,
    dailyHistory: Object.keys(db.daily).slice(-14).map(date => ({
      date,
      count: db.daily[date].total || 0
    }))
  });
});

// Heartbeat endpoint: called once daily per app installation
app.post('/api/heartbeat', (req, res) => {
  const { installId, version, platform, osVersion } = req.body || {};

  if (!installId || typeof installId !== 'string') {
    return res.status(400).json({ error: 'Missing or invalid installId' });
  }

  const cleanVersion = (version || 'unknown').slice(0, 20);
  const cleanPlatform = (platform || 'Android').slice(0, 20);
  const cleanOs = (osVersion || 'unknown').slice(0, 20);
  const today = getTodayKey();

  if (!db.allTimeUsers) db.allTimeUsers = {};
  if (!db.allTimeUsers[installId]) {
    db.allTimeUsers[installId] = { firstSeen: today, lastSeen: today };
  } else {
    db.allTimeUsers[installId].lastSeen = today;
  }

  if (!db.daily[today]) {
    db.daily[today] = {
      total: 0,
      users: [],
      versions: {},
      platforms: {}
    };
  }

  const dayRecord = db.daily[today];

  // Count user only once per calendar day
  if (!dayRecord.users.includes(installId)) {
    dayRecord.users.push(installId);
    dayRecord.total = dayRecord.users.length;

    // Track app version distribution
    dayRecord.versions[cleanVersion] = (dayRecord.versions[cleanVersion] || 0) + 1;

    // Track platform distribution
    dayRecord.platforms[cleanPlatform] = (dayRecord.platforms[cleanPlatform] || 0) + 1;

    // Store recent event log (max 30 items)
    if (!db.recentEvents) db.recentEvents = [];
    db.recentEvents.unshift({
      timestamp: new Date().toISOString(),
      version: cleanVersion,
      platform: cleanPlatform,
      osVersion: cleanOs
    });
    if (db.recentEvents.length > 30) {
      db.recentEvents.pop();
    }

    saveData();
  }

  res.json({
    status: 'ok',
    activeToday: dayRecord.total
  });
});

// Admin Web Dashboard (Obsidian / Nothing OS Dark Aesthetics)
app.get('/', (req, res) => {
  const today = getTodayKey();
  const todayData = db.daily[today] || { total: 0, versions: {}, platforms: {} };
  const allTimeCount = Object.keys(db.allTimeUsers || {}).length;

  // Past 7 days calculation
  const past7Days = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    const count = (db.daily[key] && db.daily[key].total) || 0;
    past7Days.push({ date: key, count });
  }

  const maxDailyCount = Math.max(1, ...past7Days.map(d => d.count));

  // Version rows
  const versionEntries = Object.entries(todayData.versions || {});
  const versionsHtml = versionEntries.length > 0
    ? versionEntries.map(([v, count]) => {
        const pct = todayData.total > 0 ? Math.round((count / todayData.total) * 100) : 0;
        return `
          <div style="margin-bottom: 12px;">
            <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px;">
              <span>v${escapeHtml(v)}</span>
              <span style="color: #a1a1aa;">${count} users (${pct}%)</span>
            </div>
            <div style="background: #27272a; height: 6px; border-radius: 999px; overflow: hidden;">
              <div style="background: #e11d48; height: 100%; width: ${pct}%;"></div>
            </div>
          </div>
        `;
      }).join('')
    : '<div style="color: #71717a; font-size: 13px;">No check-ins yet today.</div>';

  // Recent events rows
  const recentRows = (db.recentEvents || []).slice(0, 8).map(ev => `
    <tr style="border-bottom: 1px solid #27272a;">
      <td style="padding: 10px 8px; color: #a1a1aa; font-size: 12px;">${new Date(ev.timestamp).toLocaleTimeString()}</td>
      <td style="padding: 10px 8px; font-weight: 500;">v${escapeHtml(ev.version)}</td>
      <td style="padding: 10px 8px; color: #a1a1aa;">${escapeHtml(ev.platform)}</td>
      <td style="padding: 10px 8px; color: #71717a; font-size: 12px;">${escapeHtml(ev.osVersion)}</td>
    </tr>
  `).join('');

  // Daily bars
  const barsHtml = past7Days.map(d => {
    const heightPct = Math.round((d.count / maxDailyCount) * 85) + 15;
    const dayLabel = d.date.slice(5);
    return `
      <div style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 120px;">
        <div style="font-size: 11px; color: #e11d48; margin-bottom: 4px; font-weight: 600;">${d.count}</div>
        <div style="width: 22px; height: ${heightPct}px; background: linear-gradient(180deg, #e11d48 0%, #881337 100%); border-radius: 4px 4px 0 0;"></div>
        <div style="font-size: 10px; color: #71717a; margin-top: 6px;">${dayLabel}</div>
      </div>
    `;
  }).join('');

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AuraMusic - Telemetry Hub</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #09090b;
      color: #f4f4f5;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      padding: 24px 16px;
      min-height: 100vh;
      display: flex;
      justify-content: center;
    }
    .container {
      width: 100%;
      max-width: 760px;
    }
    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24px;
      border-bottom: 1px solid #27272a;
      padding-bottom: 16px;
    }
    .logo-badge {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: #22c55e;
      margin-right: 8px;
      box-shadow: 0 0 8px #22c55e;
    }
    .card {
      background: #18181b;
      border: 1px solid #27272a;
      border-radius: 16px;
      padding: 20px;
      margin-bottom: 18px;
    }
    .grid-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
      margin-bottom: 18px;
    }
    @media (max-width: 500px) {
      .grid-2 { grid-template-columns: 1fr; }
    }
    .stat-number {
      font-size: 38px;
      font-weight: 700;
      letter-spacing: -1px;
      color: #fafafa;
      margin-top: 6px;
    }
    .stat-label {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: #a1a1aa;
      font-weight: 600;
    }
    table { width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; }
    th { padding: 8px; color: #71717a; font-size: 11px; text-transform: uppercase; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div>
        <h1 style="font-size: 20px; font-weight: 700; letter-spacing: -0.5px;">AuraMusic Telemetry</h1>
        <p style="color: #71717a; font-size: 12px; margin-top: 2px;">Realtime Active Users & Release Analytics</p>
      </div>
      <div style="display: flex; align-items: center; font-size: 12px; color: #22c55e;">
        <span class="logo-badge"></span> LIVE
      </div>
    </div>

    <div class="grid-2">
      <div class="card" style="margin-bottom: 0;">
        <div class="stat-label">Active Users Today</div>
        <div class="stat-number" style="color: #e11d48;">${todayData.total || 0}</div>
        <div style="color: #71717a; font-size: 12px; margin-top: 6px;">Unique devices seen today</div>
      </div>
      <div class="card" style="margin-bottom: 0;">
        <div class="stat-label">All-Time Devices</div>
        <div class="stat-number">${allTimeCount}</div>
        <div style="color: #71717a; font-size: 12px; margin-top: 6px;">Cumulative installations</div>
      </div>
    </div>

    <div class="card">
      <div class="stat-label" style="margin-bottom: 16px;">Past 7 Days Activity</div>
      <div style="display: flex; gap: 8px; align-items: flex-end; padding-top: 8px;">
        ${barsHtml}
      </div>
    </div>

    <div class="card">
      <div class="stat-label" style="margin-bottom: 14px;">Version Distribution Today</div>
      ${versionsHtml}
    </div>

    <div class="card">
      <div class="stat-label" style="margin-bottom: 12px;">Recent Heartbeat Events</div>
      <table>
        <thead>
          <tr>
            <th>Time</th>
            <th>Version</th>
            <th>Platform</th>
            <th>OS</th>
          </tr>
        </thead>
        <tbody>
          ${recentRows || '<tr><td colspan="4" style="padding: 12px; color: #71717a; text-align: center;">No events recorded yet.</td></tr>'}
        </tbody>
      </table>
    </div>

    <div style="text-align: center; color: #52525b; font-size: 11px; margin-top: 24px;">
      AuraMusic Privacy-First Telemetry · Zero PII Collected · Developed by kami likhith
    </div>
  </div>
</body>
</html>`;

  res.send(html);
});

function escapeHtml(str) {
  return String(str || '').replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}

app.listen(PORT, () => {
  console.log(`AuraMusic Telemetry server running on port ${PORT}`);
});

