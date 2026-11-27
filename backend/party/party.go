package party

import (
	"crypto/rand"
	"crypto/subtle"
	"encoding/hex"
	"fmt"
	"sort"
	"sync"

	"github.com/KabirSinghBhatia/BitChord/backend/clock"
	"github.com/KabirSinghBhatia/BitChord/backend/codes"
	"github.com/KabirSinghBhatia/BitChord/backend/config"
)

// PartyError represents an error with an HTTP status code and wire error code.
type PartyError struct {
	Status  int    `json:"status"`
	Code    string `json:"error"`
	Message string `json:"message"`
}

func (e *PartyError) Error() string {
	return e.Message
}

func NewPartyError(status int, code, message string) *PartyError {
	return &PartyError{Status: status, Code: code, Message: message}
}

// Track represents a song with metadata needed for display and synchronization.
type Track struct {
	VideoId      string  `json:"videoId"`
	Title        string  `json:"title"`
	Artist       string  `json:"artist"`
	ThumbnailUrl *string `json:"thumbnailUrl,omitempty"`
	DurationMs   *int64  `json:"durationMs,omitempty"`
	FromAutoplay bool    `json:"fromAutoplay"`
}

func TrackFromWire(raw map[string]interface{}) *Track {
	if raw == nil {
		return nil
	}
	vid, _ := raw["videoId"].(string)
	if vid == "" {
		return nil
	}
	if len(vid) > 128 {
		vid = vid[:128]
	}

	title, _ := raw["title"].(string)
	if len(title) > 300 {
		title = title[:300]
	}

	artist, _ := raw["artist"].(string)
	if len(artist) > 300 {
		artist = artist[:300]
	}

	var thumbPtr *string
	if t, ok := raw["thumbnailUrl"].(string); ok && t != "" {
		if len(t) > 1000 {
			t = t[:1000]
		}
		thumbPtr = &t
	}

	var durPtr *int64
	if d, ok := raw["durationMs"].(float64); ok && d > 0 {
		dur := int64(d)
		durPtr = &dur
	}

	fromAutoplay, _ := raw["fromAutoplay"].(bool)

	return &Track{
		VideoId:      vid,
		Title:        title,
		Artist:       artist,
		ThumbnailUrl: thumbPtr,
		DurationMs:   durPtr,
		FromAutoplay: fromAutoplay,
	}
}

// PlaybackState tracks playhead, playback mode, anchor timestamps, and queue sequencing.
type PlaybackState struct {
	Track         *Track   `json:"track"`
	Queue         []*Track `json:"-"`
	QueueIndex    int      `json:"queueIndex"`
	IsPlaying     bool     `json:"isPlaying"`
	PositionMs    int64    `json:"positionMs"`
	AnchorMs      int64    `json:"anchorMs"`
	Seq           int      `json:"seq"`
	QueueSeq      int      `json:"queueSeq"`
	UpdatedBy     *string  `json:"updatedBy"`
	UpdatedAtMs   int64    `json:"updatedAtMs"`
	StartedBy     *string  `json:"startedBy"`
	StartedByName *string  `json:"startedByName"`
	AutoplayEnabled bool   `json:"autoplayEnabled"`
}

func NewPlaybackState() *PlaybackState {
	now := clock.NowMs()
	return &PlaybackState{
		Queue:       make([]*Track, 0),
		QueueIndex:  -1,
		AnchorMs:    now,
		UpdatedAtMs: now,
	}
}

func (p *PlaybackState) PositionAt(serverMs int64) int64 {
	if !p.IsPlaying {
		return p.PositionMs
	}
	elapsed := serverMs - p.AnchorMs
	if elapsed < 0 {
		elapsed = 0
	}
	pos := p.PositionMs + elapsed
	if p.Track != nil && p.Track.DurationMs != nil && *p.Track.DurationMs > 0 {
		if pos > *p.Track.DurationMs {
			return *p.Track.DurationMs
		}
	}
	return pos
}

func (p *PlaybackState) touch(memberId *string) {
	p.Seq++
	p.UpdatedBy = memberId
	p.UpdatedAtMs = clock.NowMs()
}

func (p *PlaybackState) touchQueue(memberId *string) {
	p.QueueSeq++
	p.UpdatedBy = memberId
	p.UpdatedAtMs = clock.NowMs()
}

func (p *PlaybackState) Play(memberId *string, positionMs *int64) {
	now := clock.NowMs()
	startAt := p.PositionAt(now)
	if positionMs != nil {
		startAt = *positionMs
	}
	if startAt < 0 {
		startAt = 0
	}
	p.PositionMs = startAt
	p.IsPlaying = true
	p.AnchorMs = now + int64(config.PlayLeadMs)
	p.touch(memberId)
}

func (p *PlaybackState) Pause(memberId *string, positionMs *int64) {
	now := clock.NowMs()
	pauseAt := p.PositionAt(now)
	if positionMs != nil {
		pauseAt = *positionMs
	}
	if pauseAt < 0 {
		pauseAt = 0
	}
	p.PositionMs = pauseAt
	p.IsPlaying = false
	p.AnchorMs = now
	p.touch(memberId)
}

func (p *PlaybackState) Seek(memberId *string, positionMs int64) {
	if positionMs < 0 {
		positionMs = 0
	}
	p.PositionMs = positionMs
	lead := int64(0)
	if p.IsPlaying {
		lead = int64(config.PlayLeadMs)
	}
	p.AnchorMs = clock.NowMs() + lead
	p.touch(memberId)
}

func (p *PlaybackState) SetTrack(
	memberId *string,
	track *Track,
	positionMs int64,
	isPlaying bool,
	queueIndex *int,
	memberName *string,
) {
	p.Track = track
	if positionMs < 0 {
		positionMs = 0
	}
	p.PositionMs = positionMs
	p.IsPlaying = isPlaying && track != nil
	lead := int64(0)
	if p.IsPlaying {
		lead = int64(config.PlayLeadMs)
	}
	p.AnchorMs = clock.NowMs() + lead

	if queueIndex != nil && *queueIndex >= 0 && *queueIndex < len(p.Queue) &&
		track != nil && p.Queue[*queueIndex].VideoId == track.VideoId {
		p.QueueIndex = *queueIndex
	} else if track != nil {
		p.QueueIndex = nearestIndexOf(p.Queue, track.VideoId, p.QueueIndex)
	} else if queueIndex != nil {
		p.QueueIndex = *queueIndex
	}

	p.StartedBy = memberId
	p.StartedByName = memberName
	p.touch(memberId)
}

func (p *PlaybackState) SetQueue(memberId *string, queue []*Track, queueIndex int) {
	// The sender's own index wins when it names the playing track. Searching
	// from the top instead found the copy of that track already in history
	// whenever it had played before, and every device then showed what came
	// after that old copy as still to come.
	if p.Track != nil {
		if queueIndex < 0 || queueIndex >= len(queue) || queue[queueIndex].VideoId != p.Track.VideoId {
			queueIndex = nearestIndexOf(queue, p.Track.VideoId, queueIndex)
		}
	}

	if queueIndex >= 0 && queueIndex < len(queue) {
		pastAndCurrent := queue[:queueIndex+1]
		endUpcoming := queueIndex + 1 + config.MaxUpcomingQueue
		if endUpcoming > len(queue) {
			endUpcoming = len(queue)
		}
		upcoming := queue[queueIndex+1 : endUpcoming]
		newQ := make([]*Track, 0, len(pastAndCurrent)+len(upcoming))
		newQ = append(newQ, pastAndCurrent...)
		newQ = append(newQ, upcoming...)
		p.Queue = newQ
		p.QueueIndex = queueIndex
	} else {
		maxLen := config.MaxQueueLength
		if len(queue) < maxLen {
			maxLen = len(queue)
		}
		p.Queue = queue[:maxLen]
		if queueIndex >= 0 && queueIndex < len(p.Queue) {
			p.QueueIndex = queueIndex
		} else {
			p.QueueIndex = -1
		}
	}
	p.touchQueue(memberId)
}

// nearestIndexOf finds videoId in queue closest to around: the slot right after
// it first (the queue moving on), then the one before it (going back), then
// the nearest copy either way. A queue can hold one track twice, once played
// and once to come, and the first copy from the top is the wrong one as often
// as not.
func nearestIndexOf(queue []*Track, videoId string, around int) int {
	if around+1 >= 0 && around+1 < len(queue) && queue[around+1].VideoId == videoId {
		return around + 1
	}
	if around-1 >= 0 && around-1 < len(queue) && queue[around-1].VideoId == videoId {
		return around - 1
	}
	best := -1
	bestDistance := 0
	for i, item := range queue {
		if item.VideoId != videoId {
			continue
		}
		distance := i - around
		if distance < 0 {
			// Behind the needle costs a little more than ahead of it.
			distance = -distance*2 + 1
		}
		if best == -1 || distance < bestDistance {
			best = i
			bestDistance = distance
		}
	}
	return best
}

// withoutQueuedAutoplay drops AutoPlay suggestions that are already waiting
// after the current track, or repeated within the batch. Two devices can both
// believe they supply AutoPlay for a moment (a host reconnecting), and one can
// retry before its first batch has echoed back; either way the same station
// arrives twice and the queue shows every suggestion twice. Tracks somebody
// queued by hand are never filtered: queueing a song twice is allowed.
func withoutQueuedAutoplay(queue []*Track, queueIndex int, tracks []*Track) []*Track {
	waiting := make(map[string]bool)
	for i := queueIndex + 1; i < len(queue); i++ {
		if i >= 0 {
			waiting[queue[i].VideoId] = true
		}
	}
	out := make([]*Track, 0, len(tracks))
	for _, t := range tracks {
		if t == nil {
			continue
		}
		if t.FromAutoplay {
			if waiting[t.VideoId] {
				continue
			}
			waiting[t.VideoId] = true
		}
		out = append(out, t)
	}
	return out
}

func (p *PlaybackState) AddUpcoming(memberId *string, tracks []*Track, playNext bool) (bool, string) {
	currentUpcoming := 0
	if p.QueueIndex >= 0 {
		currentUpcoming = len(p.Queue) - 1 - p.QueueIndex
		if currentUpcoming < 0 {
			currentUpcoming = 0
		}
	} else {
		currentUpcoming = len(p.Queue)
	}

	slotsLeft := config.MaxUpcomingQueue - currentUpcoming
	if slotsLeft <= 0 {
		return false, "queue_full"
	}

	toAdd := withoutQueuedAutoplay(p.Queue, p.QueueIndex, tracks)
	if len(toAdd) == 0 && len(tracks) > 0 {
		// Every suggestion is already waiting in the queue: a second supplier,
		// or the same one retrying before its first batch came back. Nothing to
		// do is not a refusal, so the sender's echo wait still completes.
		return true, ""
	}
	if len(toAdd) > slotsLeft {
		toAdd = toAdd[:slotsLeft]
	}
	if len(toAdd) == 0 {
		return false, "no_tracks"
	}

	if playNext && p.QueueIndex >= 0 && p.QueueIndex < len(p.Queue) {
		insertAt := p.QueueIndex + 1
		newQ := make([]*Track, 0, len(p.Queue)+len(toAdd))
		newQ = append(newQ, p.Queue[:insertAt]...)
		newQ = append(newQ, toAdd...)
		newQ = append(newQ, p.Queue[insertAt:]...)
		p.Queue = newQ
	} else {
		p.Queue = append(p.Queue, toAdd...)
	}

	p.touchQueue(memberId)
	return true, ""
}

func (p *PlaybackState) RemoveUpcoming(memberId *string, videoId string) bool {
	match := -1
	for i, item := range p.Queue {
		if item.VideoId == videoId {
			match = i
			break
		}
	}
	if match == -1 || match == p.QueueIndex {
		return false
	}

	p.Queue = append(p.Queue[:match], p.Queue[match+1:]...)
	if p.QueueIndex > match {
		p.QueueIndex--
	}
	p.touchQueue(memberId)
	return true
}

func (p *PlaybackState) ClearUpcoming(memberId *string) bool {
	if p.QueueIndex >= 0 {
		if len(p.Queue) <= p.QueueIndex+1 {
			return false
		}
		p.Queue = p.Queue[:p.QueueIndex+1]
	} else {
		if len(p.Queue) == 0 {
			return false
		}
		p.Queue = p.Queue[:0]
	}
	p.touchQueue(memberId)
	return true
}

func (p *PlaybackState) MoveUpcoming(memberId *string, fromIdx, toIdx int, videoId string) bool {
	if videoId != "" {
		match := -1
		for i, item := range p.Queue {
			if item.VideoId == videoId {
				match = i
				break
			}
		}
		if match == -1 {
			return false
		}
		fromIdx = match
	}

	if fromIdx < 0 || fromIdx >= len(p.Queue) || toIdx < 0 || toIdx >= len(p.Queue) {
		return false
	}
	if fromIdx <= p.QueueIndex || toIdx <= p.QueueIndex {
		return false
	}
	if fromIdx == toIdx {
		return true
	}

	item := p.Queue[fromIdx]
	p.Queue = append(p.Queue[:fromIdx], p.Queue[fromIdx+1:]...)
	// Insert at toIdx
	newQ := make([]*Track, 0, len(p.Queue)+1)
	newQ = append(newQ, p.Queue[:toIdx]...)
	newQ = append(newQ, item)
	newQ = append(newQ, p.Queue[toIdx:]...)
	p.Queue = newQ

	p.touchQueue(memberId)
	return true
}

func (p *PlaybackState) Step(memberId *string, delta int, memberName *string) bool {
	target := p.QueueIndex + delta
	if target < 0 || target >= len(p.Queue) {
		return false
	}
	p.SetTrack(memberId, p.Queue[target], 0, true, &target, memberName)
	return true
}

func (p *PlaybackState) ToWire(serverMs int64) map[string]interface{} {
	var trackWire interface{}
	if p.Track != nil {
		trackWire = p.Track
	}

	return map[string]interface{}{
		"seq":                 p.Seq,
		"track":               trackWire,
		"queueSeq":            p.QueueSeq,
		"queueIndex":          p.QueueIndex,
		"queueLength":         len(p.Queue),
		"isPlaying":           p.IsPlaying,
		"positionMs":          p.PositionMs,
		"anchorMs":            p.AnchorMs,
		"effectivePositionMs": p.PositionAt(serverMs),
		"updatedBy":           p.UpdatedBy,
		"startedBy":           p.StartedBy,
		"startedByName":       p.StartedByName,
		"autoplayEnabled":     p.AutoplayEnabled,
		"updatedAtMs":         p.UpdatedAtMs,
	}
}

// SetAutoplay changes the party-wide AutoPlay choice. It belongs to playback
// state so a replacement AutoPlay supplier can continue after the host leaves.
func (p *PlaybackState) SetAutoplay(memberId *string, enabled bool) {
	if p.AutoplayEnabled == enabled {
		return
	}
	p.AutoplayEnabled = enabled
	p.UpdatedBy = memberId
	p.UpdatedAtMs = clock.NowMs()
}

func (p *PlaybackState) QueueToWire() map[string]interface{} {
	return map[string]interface{}{
		"seq":   p.QueueSeq,
		"index": p.QueueIndex,
		"items": p.Queue,
	}
}

// Member represents a joined device.
type Member struct {
	MemberId           string  `json:"memberId"`
	UserId             string  `json:"userId"`
	DeviceId           string  `json:"-"`
	DisplayName        string  `json:"displayName"`
	AvatarUrl          *string `json:"avatarUrl,omitempty"`
	Token              string  `json:"-"`
	IsHost             bool    `json:"isHost"`
	Connected          bool    `json:"connected"`
	JoinedAtMs         int64   `json:"joinedAtMs"`
	LastSeenMs         int64   `json:"lastSeenMs"`
	ControlBudget      float64 `json:"-"`
	ControlBudgetAtMs  int64   `json:"-"`
	FrameBudget        float64 `json:"-"`
	FrameBudgetAtMs    int64   `json:"-"`
}

func (m *Member) ToWire() map[string]interface{} {
	return map[string]interface{}{
		"memberId":    m.MemberId,
		"userId":      m.UserId,
		"displayName": m.DisplayName,
		"avatarUrl":   m.AvatarUrl,
		"isHost":      m.IsHost,
		"connected":   m.Connected,
		"joinedAtMs":  m.JoinedAtMs,
		"lastSeenMs":  m.LastSeenMs,
	}
}

// Party holds members and playback state for a single room code.
type Party struct {
	mu         sync.Mutex
	Code       string
	Members    map[string]*Member
	Playback   *PlaybackState
	MaxMembers int
	// HostOnlyControl restricts every playback and queue action to the host.
	//
	// Deliberately on Party rather than on PlaybackState: it changes rarely and
	// PlaybackState.ToWire rides the heartbeat to every device every few
	// seconds. It travels with MaxMembers on the members frame instead, which
	// is only sent when something about the membership actually changes.
	HostOnlyControl bool
	AuxMemberId     string
	SkipVotes       map[string]bool
	ChatMsgs        map[string]*ChatMessage
	ChatOrder       []string
	CreatedAtMs     int64
	TouchedAtMs     int64
	EmptySinceMs    *int64
}

// ChatMessage holds server-authoritative state for a party chat item so clients
// cannot spoof sender identity, overwrite another user's text, or forge upvotes.
type ChatMessage struct {
	ID            string
	SenderId      string
	SenderName    string
	SenderAvatar  *string
	TimestampMs   int64
	Text          string
	SongWire      map[string]interface{}
	SongTrack     *Track
	ImageUrl      *string
	IsFounder     bool
	Upvoters      map[string]bool
	AutoQueued    bool
	Reactions     map[string]map[string]bool
	ReplyToId     *string
	ReplyToSender *string
	ReplyToText   *string
	IsPinned      bool
}

const maxChatHistory = 500

func NewParty(code string) *Party {
	return NewPartyWithMaxMembers(code, config.MaxMembers)
}

func NewPartyWithMaxMembers(code string, maxMembers int) *Party {
	if maxMembers < 2 || maxMembers > 10 {
		maxMembers = 5
	}
	now := clock.NowMs()
	empty := now
	return &Party{
		Code:         code,
		Members:      make(map[string]*Member),
		Playback:     NewPlaybackState(),
		MaxMembers:   maxMembers,
		SkipVotes:    make(map[string]bool),
		ChatMsgs:     make(map[string]*ChatMessage),
		CreatedAtMs:  now,
		TouchedAtMs:  now,
		EmptySinceMs: &empty,
	}
}

func (p *Party) Lock()   { p.mu.Lock() }
func (p *Party) Unlock() { p.mu.Unlock() }

func (p *Party) Touch() {
	p.TouchedAtMs = clock.NowMs()
}

func (p *Party) ToggleSkipVote(memberId string) (int, int, bool) {
	if p.SkipVotes == nil {
		p.SkipVotes = make(map[string]bool)
	}
	if p.SkipVotes[memberId] {
		delete(p.SkipVotes, memberId)
	} else {
		p.SkipVotes[memberId] = true
	}
	activeVotes := len(p.SkipVotes)
	connectedCount := 0
	for _, m := range p.Members {
		if m.Connected {
			connectedCount++
		}
	}
	if connectedCount == 0 {
		connectedCount = len(p.Members)
	}
	needed := (connectedCount / 2) + 1
	reached := activeVotes >= needed
	if reached {
		p.SkipVotes = make(map[string]bool)
	}
	return activeVotes, needed, reached
}

func (p *Party) ClearSkipVotes() {
	p.SkipVotes = make(map[string]bool)
}

func (p *Party) Host() *Member {
	for _, m := range p.Members {
		if m.IsHost {
			return m
		}
	}
	return nil
}

func (p *Party) Join(userId, deviceId, displayName string, avatarUrl *string) (*Member, error) {
	return p.JoinWithToken(userId, deviceId, displayName, avatarUrl, "")
}

// JoinWithToken binds or creates a member slot. Reusing an existing deviceId slot
// requires proving possession of the slot's active bearer token so another caller
// cannot hijack an existing session (or host) merely by copying its deviceId.
func (p *Party) JoinWithToken(userId, deviceId, displayName string, avatarUrl *string, priorToken string) (*Member, error) {
	now := clock.NowMs()
	// Rejoining device check
	for _, m := range p.Members {
		if m.DeviceId == deviceId {
			if priorToken == "" || subtle.ConstantTimeCompare([]byte(m.Token), []byte(priorToken)) != 1 {
				return nil, NewPartyError(
					409,
					"device_slot_in_use",
					"That device already holds a slot in this party; reconnect using your active session token.",
				)
			}
			m.DisplayName = displayName
			m.AvatarUrl = avatarUrl
			m.UserId = userId
			m.LastSeenMs = now
			m.Token = randomToken(24)
			p.Touch()
			return m, nil
		}
	}

	if len(p.Members) >= p.MaxMembers {
		return nil, NewPartyError(409, "party_full", fmt.Sprintf("This party is full (%d devices).", p.MaxMembers))
	}

	m := &Member{
		MemberId:          randomHex(8),
		UserId:            userId,
		DeviceId:          deviceId,
		DisplayName:       displayName,
		AvatarUrl:         avatarUrl,
		Token:             randomToken(24),
		IsHost:            len(p.Members) == 0,
		Connected:         false,
		JoinedAtMs:        now,
		LastSeenMs:        now,
		ControlBudget:     config.ControlRatePerSecond,
		ControlBudgetAtMs: now,
		FrameBudget:       config.FrameRatePerSecond,
		FrameBudgetAtMs:   now,
	}
	p.Members[m.MemberId] = m
	p.Touch()
	return m, nil
}

// ApplyChatFrame processes new chat messages and per-member updates (upvotes, reactions, host pins)
// while enforcing server authority over sender identity, song immutability, and vote counting.
func (p *Party) ApplyChatFrame(member *Member, frame map[string]interface{}) (map[string]interface{}, *Track, bool) {
	if p.ChatMsgs == nil {
		p.ChatMsgs = make(map[string]*ChatMessage)
	}
	msgId, _ := frame["id"].(string)
	if msgId == "" || len(msgId) > 128 {
		return nil, nil, false
	}

	rec, exists := p.ChatMsgs[msgId]
	if !exists {
		msgText, _ := frame["text"].(string)
		if len(msgText) > 2000 {
			msgText = msgText[:2000]
		}
		var songWire map[string]interface{}
		var songTrack *Track
		if rawSong, ok := frame["song"].(map[string]interface{}); ok && rawSong != nil {
			vid, _ := rawSong["videoId"].(string)
			title, _ := rawSong["title"].(string)
			artist, _ := rawSong["artist"].(string)
			if vid != "" {
				if len(vid) > 128 {
					vid = vid[:128]
				}
				if len(title) > 300 {
					title = title[:300]
				}
				if len(artist) > 300 {
					artist = artist[:300]
				}
				songWire = map[string]interface{}{
					"videoId": vid,
					"title":   title,
					"artist":  artist,
				}
				if cover, ok := rawSong["coverUrl"].(string); ok && cover != "" {
					if len(cover) > 1000 {
						cover = cover[:1000]
					}
					songWire["coverUrl"] = cover
				}
				var durMsPtr *int64
				if durSec, ok := rawSong["durationSec"].(float64); ok && durSec > 0 {
					secInt := int64(durSec)
					songWire["durationSec"] = secInt
					ms := secInt * 1000
					durMsPtr = &ms
				} else if durMs, ok := rawSong["durationMs"].(float64); ok && durMs > 0 {
					ms := int64(durMs)
					durMsPtr = &ms
				}
				var thumbPtr *string
				if cover, ok := songWire["coverUrl"].(string); ok && cover != "" {
					thumbPtr = &cover
				}
				songTrack = &Track{
					VideoId:      vid,
					Title:        title,
					Artist:       artist,
					ThumbnailUrl: thumbPtr,
					DurationMs:   durMsPtr,
				}
			}
		}

		var imagePtr *string
		if img, ok := frame["imageUrl"].(string); ok && img != "" && len(img) <= 128*1024 {
			imagePtr = &img
		}
		if msgText == "" && songTrack == nil && imagePtr == nil {
			return nil, nil, false
		}

		var replyToId, replyToSender, replyToText *string
		if v, ok := frame["replyToId"].(string); ok && v != "" && len(v) <= 128 {
			replyToId = &v
		}
		if v, ok := frame["replyToSender"].(string); ok && v != "" && len(v) <= 80 {
			replyToSender = &v
		}
		if v, ok := frame["replyToText"].(string); ok && v != "" {
			if len(v) > 200 {
				v = v[:200]
			}
			replyToText = &v
		}

		if len(p.ChatOrder) >= maxChatHistory {
			evict := p.ChatOrder[0]
			p.ChatOrder = p.ChatOrder[1:]
			delete(p.ChatMsgs, evict)
		}

		rec = &ChatMessage{
			ID:            msgId,
			SenderId:      member.MemberId,
			SenderName:    member.DisplayName,
			SenderAvatar:  member.AvatarUrl,
			TimestampMs:   clock.NowMs(),
			Text:          msgText,
			SongWire:      songWire,
			SongTrack:     songTrack,
			ImageUrl:      imagePtr,
			IsFounder:     false,
			Upvoters:      make(map[string]bool),
			Reactions:     make(map[string]map[string]bool),
			ReplyToId:     replyToId,
			ReplyToSender: replyToSender,
			ReplyToText:   replyToText,
		}
		p.ChatMsgs[msgId] = rec
		p.ChatOrder = append(p.ChatOrder, msgId)
		return rec.toWire(), nil, true
	}

	// Update existing message: each member can only toggle their own vote & reactions;
	// only the host may toggle pin state. Immutable message content cannot be mutated.
	var autoQueuedTrack *Track
	if rawUpvoters, hasList := frame["upvoterIds"].([]interface{}); hasList {
		memberWantsVote := false
		for _, item := range rawUpvoters {
			if s, ok := item.(string); ok && s == member.MemberId {
				memberWantsVote = true
				break
			}
		}
		if memberWantsVote {
			rec.Upvoters[member.MemberId] = true
		} else {
			delete(rec.Upvoters, member.MemberId)
		}
	}
	if len(rec.Upvoters) >= 2 && !rec.AutoQueued && rec.SongTrack != nil && p.MayControl(member) {
		if ok, _ := p.Playback.AddUpcoming(&member.MemberId, []*Track{rec.SongTrack}, false); ok {
			rec.AutoQueued = true
			autoQueuedTrack = rec.SongTrack
		}
	}

	if rawReactions, ok := frame["reactions"].(map[string]interface{}); ok {
		nextMemberEmojis := make(map[string]bool)
		for emoji, rawUsers := range rawReactions {
			if emoji == "" || len(emoji) > 32 || len(nextMemberEmojis) >= 20 {
				continue
			}
			users, ok := rawUsers.([]interface{})
			if !ok {
				continue
			}
			for _, u := range users {
				if idStr, ok := u.(string); ok && idStr == member.MemberId {
					nextMemberEmojis[emoji] = true
					break
				}
			}
		}
		for emoji, set := range rec.Reactions {
			if !nextMemberEmojis[emoji] {
				delete(set, member.MemberId)
				if len(set) == 0 {
					delete(rec.Reactions, emoji)
				}
			}
		}
		for emoji := range nextMemberEmojis {
			if len(rec.Reactions) >= 20 {
				if _, existsEmoji := rec.Reactions[emoji]; !existsEmoji {
					continue
				}
			}
			set := rec.Reactions[emoji]
			if set == nil {
				set = make(map[string]bool)
				rec.Reactions[emoji] = set
			}
			set[member.MemberId] = true
		}
	}

	if member.IsHost {
		if pinned, ok := frame["isPinned"].(bool); ok {
			rec.IsPinned = pinned
		}
	}

	return rec.toWire(), autoQueuedTrack, true
}

func (c *ChatMessage) toWire() map[string]interface{} {
	upvoterList := make([]string, 0, len(c.Upvoters))
	for id := range c.Upvoters {
		upvoterList = append(upvoterList, id)
	}
	sort.Strings(upvoterList)

	reactionsWire := make(map[string][]string, len(c.Reactions))
	for emoji, members := range c.Reactions {
		if len(members) == 0 {
			continue
		}
		ids := make([]string, 0, len(members))
		for id := range members {
			ids = append(ids, id)
		}
		sort.Strings(ids)
		reactionsWire[emoji] = ids
	}

	out := map[string]interface{}{
		"type":        "chat",
		"id":          c.ID,
		"senderId":    c.SenderId,
		"senderName":  c.SenderName,
		"timestampMs": c.TimestampMs,
		"text":        c.Text,
		"isFounder":   c.IsFounder,
		"upvotes":     len(upvoterList),
		"upvoterIds":  upvoterList,
		"autoQueued":  c.AutoQueued,
		"reactions":   reactionsWire,
		"isPinned":    c.IsPinned,
	}
	if c.SenderAvatar != nil {
		out["senderAvatar"] = *c.SenderAvatar
	}
	if c.SongWire != nil {
		out["song"] = c.SongWire
	}
	if c.ImageUrl != nil {
		out["imageUrl"] = *c.ImageUrl
	}
	if c.ReplyToId != nil {
		out["replyToId"] = *c.ReplyToId
	}
	if c.ReplyToSender != nil {
		out["replyToSender"] = *c.ReplyToSender
	}
	if c.ReplyToText != nil {
		out["replyToText"] = *c.ReplyToText
	}
	return out
}

// DeleteChatMessage validates that only the original sender or room host deletes a chat entry.
func (p *Party) DeleteChatMessage(msgId string, member *Member) bool {
	if msgId == "" || p.ChatMsgs == nil {
		return false
	}
	rec, ok := p.ChatMsgs[msgId]
	if !ok {
		return false
	}
	if rec.SenderId != member.MemberId && !member.IsHost {
		return false
	}
	delete(p.ChatMsgs, msgId)
	return true
}

func (p *Party) SetMaxMembers(member *Member, maxMembers int) error {
	if !member.IsHost {
		return NewPartyError(403, "host_only", "Only the host can change the party size.")
	}
	if maxMembers < 2 || maxMembers > 10 {
		return NewPartyError(422, "invalid_capacity", "Party size must be between 2 and 10.")
	}
	if maxMembers < len(p.Members) {
		return NewPartyError(409, "party_too_small", "Party size cannot be smaller than the current member count.")
	}
	p.MaxMembers = maxMembers
	p.Touch()
	return nil
}

// SetHostOnlyControl restricts the music to the host, or hands it back to
// everyone. Host-only, like SetMaxMembers: a listener who could turn this off
// is not restricted by it.
func (p *Party) SetHostOnlyControl(member *Member, enabled bool) error {
	if !member.IsHost {
		return NewPartyError(403, "host_only", "Only the host can change who controls the music.")
	}
	if p.HostOnlyControl == enabled {
		return nil
	}
	p.HostOnlyControl = enabled
	p.Touch()
	return nil
}

// MayControl reports whether this member is allowed to drive playback and the
// queue right now.
//
// The host always may. Everyone else may until the host says otherwise — which
// is the behaviour this feature shipped with and stays the default for a party
// that never touches the setting.
func (p *Party) MayControl(member *Member) bool {
	return !p.HostOnlyControl || member.IsHost || (p.AuxMemberId != "" && member.MemberId == p.AuxMemberId)
}

func (p *Party) Authenticate(token string) (*Member, error) {
	for _, m := range p.Members {
		if subtle.ConstantTimeCompare([]byte(m.Token), []byte(token)) == 1 {
			return m, nil
		}
	}
	return nil, NewPartyError(401, "bad_token", "This device is not a member of that party.")
}

func (p *Party) Remove(memberId string) *Member {
	m, ok := p.Members[memberId]
	if !ok {
		return nil
	}
	delete(p.Members, memberId)
	if p.AuxMemberId == memberId {
		p.AuxMemberId = ""
	}
	if m.IsHost && len(p.Members) > 0 {
		for _, next := range p.Members {
			next.IsHost = true
			break
		}
	}
	p.Touch()
	p.refreshEmptiness()
	return m
}

func (p *Party) SpendControlBudget(member *Member) bool {
	now := clock.NowMs()
	elapsedS := float64(now-member.ControlBudgetAtMs) / 1000.0
	if elapsedS < 0 {
		elapsedS = 0
	}
	member.ControlBudget = member.ControlBudget + elapsedS*config.ControlRatePerSecond
	if member.ControlBudget > config.ControlRatePerSecond {
		member.ControlBudget = config.ControlRatePerSecond
	}
	member.ControlBudgetAtMs = now
	if member.ControlBudget < 1.0 {
		return false
	}
	member.ControlBudget -= 1.0
	return true
}

// SpendFrameBudget limits every received WebSocket frame, not just controls.
// This prevents ping/sync frames from bypassing the control rate limit.
func (p *Party) SpendFrameBudget(member *Member) bool {
	now := clock.NowMs()
	elapsedS := float64(now-member.FrameBudgetAtMs) / 1000.0
	if elapsedS < 0 {
		elapsedS = 0
	}
	member.FrameBudget += elapsedS * config.FrameRatePerSecond
	if member.FrameBudget > config.FrameRatePerSecond {
		member.FrameBudget = config.FrameRatePerSecond
	}
	member.FrameBudgetAtMs = now
	if member.FrameBudget < 1.0 {
		return false
	}
	member.FrameBudget -= 1.0
	return true
}

func (p *Party) MarkConnected(member *Member, connected bool) {
	member.Connected = connected
	member.LastSeenMs = clock.NowMs()
	p.Touch()
	p.refreshEmptiness()
}

func (p *Party) refreshEmptiness() {
	anyConn := false
	for _, m := range p.Members {
		if m.Connected {
			anyConn = true
			break
		}
	}
	if anyConn {
		p.EmptySinceMs = nil
	} else if p.EmptySinceMs == nil {
		now := clock.NowMs()
		p.EmptySinceMs = &now
	}
}

func (p *Party) ExpiredMembers(now int64) []*Member {
	var expired []*Member
	for _, m := range p.Members {
		if !m.Connected && now-m.LastSeenMs > config.DisconnectGraceMs {
			expired = append(expired, m)
		}
	}
	return expired
}

func (p *Party) IsExpired(now int64) bool {
	if now-p.CreatedAtMs > config.PartyMaxAgeMs {
		return true
	}
	if p.EmptySinceMs != nil {
		return now-*p.EmptySinceMs > config.EmptyPartyTTLMs
	}
	return false
}

func (p *Party) ToWire() map[string]interface{} {
	now := clock.NowMs()
	membersList := make([]*Member, 0, len(p.Members))
	for _, m := range p.Members {
		membersList = append(membersList, m)
	}
	sort.Slice(membersList, func(i, j int) bool {
		return membersList[i].JoinedAtMs < membersList[j].JoinedAtMs
	})

	membersWire := make([]map[string]interface{}, 0, len(membersList))
	for _, m := range membersList {
		membersWire = append(membersWire, m.ToWire())
	}

	return map[string]interface{}{
		"code":            p.Code,
		"createdAtMs":     p.CreatedAtMs,
		"maxMembers":      p.MaxMembers,
		"hostOnlyControl": p.HostOnlyControl,
		"auxMemberId":     p.AuxMemberId,
		"members":         membersWire,
		"playback":        p.Playback.ToWire(now),
		"queue":           p.Playback.QueueToWire(),
		"serverMs":        now,
	}
}

// PartyStore holds all live parties in process memory.
type PartyStore struct {
	mu      sync.RWMutex
	parties map[string]*Party
}

func NewPartyStore() *PartyStore {
	return &PartyStore{
		parties: make(map[string]*Party),
	}
}

func (s *PartyStore) Len() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return len(s.parties)
}

// MemberCount is the number of members across every live party. Parties are
// copied out first so no party lock is ever taken under the store lock.
func (s *PartyStore) MemberCount() int {
	s.mu.RLock()
	parties := make([]*Party, 0, len(s.parties))
	for _, p := range s.parties {
		parties = append(parties, p)
	}
	s.mu.RUnlock()
	total := 0
	for _, p := range parties {
		p.mu.Lock()
		total += len(p.Members)
		p.mu.Unlock()
	}
	return total
}

func (s *PartyStore) Create() (*Party, error) {
	return s.CreateWithLimit(0)
}

// CreateWithLimit atomically rejects creation once the service-wide room limit
// is reached. A non-positive limit means unlimited (used by unit tests).
func (s *PartyStore) CreateWithLimit(maxParties int) (*Party, error) {
	return s.CreateWithLimits(maxParties, config.MaxMembers)
}

func (s *PartyStore) CreateWithLimits(maxParties, maxMembers int) (*Party, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if maxParties > 0 && len(s.parties) >= maxParties {
		return nil, NewPartyError(503, "server_full", "The party server is busy. Please try again in a few minutes.")
	}

	for i := 0; i < 12; i++ {
		code := codes.NewCode()
		if _, exists := s.parties[code]; !exists {
		p := NewPartyWithMaxMembers(code, maxMembers)
			s.parties[code] = p
			return p, nil
		}
	}
	return nil, NewPartyError(503, "code_exhausted", "Could not allocate a party code.")
}

func (s *PartyStore) Get(code string) (*Party, error) {
	norm := codes.Normalise(code)
	s.mu.RLock()
	p, ok := s.parties[norm]
	s.mu.RUnlock()
	if !ok {
		return nil, NewPartyError(404, "no_such_party", "No party with that code.")
	}
	return p, nil
}

func (s *PartyStore) Find(code string) *Party {
	norm := codes.Normalise(code)
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.parties[norm]
}

func (s *PartyStore) Drop(code string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.parties, code)
}

func (s *PartyStore) All() []*Party {
	s.mu.RLock()
	defer s.mu.RUnlock()
	res := make([]*Party, 0, len(s.parties))
	for _, p := range s.parties {
		res = append(res, p)
	}
	return res
}

func (s *PartyStore) Sweep(now int64) []*Party {
	s.mu.Lock()
	defer s.mu.Unlock()

	var changed []*Party
	for code, party := range s.parties {
		party.Lock()
		gone := party.ExpiredMembers(now)
		for _, m := range gone {
			party.Remove(m.MemberId)
		}
		expired := party.IsExpired(now)
		party.Unlock()

		if expired {
			delete(s.parties, code)
			continue
		}
		if len(gone) > 0 {
			changed = append(changed, party)
		}
	}
	return changed
}

func randomHex(n int) string {
	b := make([]byte, n)
	rand.Read(b)
	return hex.EncodeToString(b)
}

func randomToken(n int) string {
	b := make([]byte, n)
	rand.Read(b)
	return hex.EncodeToString(b)
}
