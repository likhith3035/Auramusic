/**
 * AuraMusic Desktop-Style Player & Synced Lyrics Engine
 * Keeps persistent bottom bar and fullscreen LandscapePlayer synchronized.
 */

class AuraMusicPlayer {
  constructor() {
    this.audio = new Audio();
    this.currentTrack = null;
    this.queue = [];
    this.queueIndex = 0;
    this.isPlaying = false;
    this.isShuffle = false;
    this.sleepTimerMinutes = null;
    this.sleepTimerEndTimestamp = null;
    this.lyricsData = [];
    this.currentLyricIndex = -1;

    this.tracks = [
      {
        id: 'track-1',
        title: 'Midnight Echoes',
        artist: 'Aura Sound Lab',
        album: 'Spatial Acoustics',
        art: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800&auto=format&fit=crop&q=80',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3',
        duration: 164,
        lyrics: [
          { time: 0, text: "♪ Ambient instrumental opening..." },
          { time: 10, text: "Dim the living room lights softly" },
          { time: 18, text: "Let the soundwaves fill every corner" },
          { time: 26, text: "Lossless frequencies in 24-bit clarity" },
          { time: 35, text: "Sink deep into the couch tonight" },
          { time: 45, text: "No rushing, just pure acoustic aura" },
          { time: 58, text: "Midnight echoes through the room" },
          { time: 70, text: "Floating between the drums and keys" },
          { time: 82, text: "Nothing in between, just the music" },
          { time: 98, text: "Relax, breathe in, let it drift..." },
          { time: 120, text: "♪ Gentle outro fades into quietude..." }
        ]
      },
      {
        id: 'track-2',
        title: 'Neon Horizon',
        artist: 'The Cyber Waves',
        album: 'Nothing Synthwave',
        art: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=lifelike-126735.mp3',
        duration: 142,
        lyrics: [
          { time: 0, text: "♪ Heavy analog synthesizers pulse..." },
          { time: 12, text: "Rays of magenta across the glass" },
          { time: 22, text: "Electric memories from yesterday" },
          { time: 34, text: "High fidelity in motion" },
          { time: 46, text: "Speeding towards the neon horizon" },
          { time: 60, text: "Feel the sub-bass reverberate" },
          { time: 75, text: "Pure atmosphere, pure rhythm" }
        ]
      },
      {
        id: 'track-3',
        title: 'Tokyo Rain Walk',
        artist: 'Komorebi Quartet',
        album: 'Chill Ambient Sessions',
        art: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=80',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f77cb7.mp3?filename=chill-abstract-intention-12099.mp3',
        duration: 188,
        lyrics: [
          { time: 0, text: "♪ Raindrops falling on quiet pavement..." },
          { time: 14, text: "Soft piano chords in the night" },
          { time: 28, text: "Walking slow through Shibuya streets" },
          { time: 42, text: "Warm tea and headphones on" },
          { time: 59, text: "The city sleeps while the melody plays" },
          { time: 80, text: "Drifting in tranquility..." }
        ]
      },
      {
        id: 'track-4',
        title: 'Starlight Odyssey',
        artist: 'Aura Symphony',
        album: 'Deep Space Reverie',
        art: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=800&auto=format&fit=crop&q=80',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=solitude-dark-ambient-electronic-10777.mp3',
        duration: 210,
        lyrics: [
          { time: 0, text: "♪ Cosmic drone fills the space..." },
          { time: 20, text: "Beyond the clouds, past the moon" },
          { time: 40, text: "Endless galaxies in silence" },
          { time: 65, text: "Music without gravity" }
        ]
      }
    ];

    this.queue = [...this.tracks];
    this.init();
  }

  init() {
    this.bindAudioEvents();
    this.loadTrack(this.queue[0], false);
  }

  loadTrack(track, autoPlay = true) {
    if (!track) return;
    this.currentTrack = track;
    this.audio.src = track.audioUrl;
    this.lyricsData = track.lyrics || [];
    this.currentLyricIndex = -1;

    // Update Persistent Bottom Bar
    const barThumb = document.getElementById('barThumb');
    const barTitle = document.getElementById('barTitle');
    const barArtist = document.getElementById('barArtist');

    if (barThumb) barThumb.src = track.art;
    if (barTitle) barTitle.textContent = track.title;
    if (barArtist) barArtist.textContent = `${track.artist} • 24-bit FLAC`;

    // Update Fullscreen LandscapePlayer elements
    const fsAlbumArt = document.getElementById('fsAlbumArt');
    const fsSongTitle = document.getElementById('fsSongTitle');
    const fsArtistName = document.getElementById('fsArtistName');

    if (fsAlbumArt) fsAlbumArt.src = track.art;
    if (fsSongTitle) fsSongTitle.textContent = track.title;
    if (fsArtistName) fsArtistName.textContent = track.artist;

    this.renderLyrics();
    this.renderQueue();

    if (autoPlay) {
      this.play();
    }
  }

  play() {
    this.audio.play().then(() => {
      this.isPlaying = true;
      document.body.classList.add('playing');
    }).catch(e => console.warn('Audio play policy:', e));
  }

  pause() {
    this.audio.pause();
    this.isPlaying = false;
    document.body.classList.remove('playing');
  }

  togglePlay() {
    if (this.isPlaying) this.pause();
    else this.play();
  }

  nextTrack() {
    if (this.queue.length === 0) return;
    this.queueIndex = (this.queueIndex + 1) % this.queue.length;
    this.loadTrack(this.queue[this.queueIndex], true);
  }

  prevTrack() {
    if (this.audio.currentTime > 3) {
      this.audio.currentTime = 0;
      return;
    }
    if (this.queue.length === 0) return;
    this.queueIndex = (this.queueIndex - 1 + this.queue.length) % this.queue.length;
    this.loadTrack(this.queue[this.queueIndex], true);
  }

  toggleShuffle() {
    this.isShuffle = !this.isShuffle;
    const btn = document.getElementById('barShuffleBtn');
    if (btn) btn.style.color = this.isShuffle ? 'var(--nt-red)' : 'var(--nt-muted)';
  }

  bindAudioEvents() {
    this.audio.addEventListener('timeupdate', () => this.onTimeUpdate());
    this.audio.addEventListener('ended', () => {
      if (this.sleepTimerEndTimestamp && Date.now() >= this.sleepTimerEndTimestamp) {
        this.pause();
        this.sleepTimerEndTimestamp = null;
        alert("🌙 Aura Sleep Timer finished.");
        return;
      }
      this.nextTrack();
    });
  }

  onTimeUpdate() {
    const cur = this.audio.currentTime || 0;
    const dur = this.audio.duration || this.currentTrack?.duration || 1;
    const percent = Math.min((cur / dur) * 100, 100);

    // Update Persistent Bottom Bar
    const fill = document.getElementById('barScrubFill');
    const curTime = document.getElementById('barCurrentTime');
    const totTime = document.getElementById('barTotalTime');

    if (fill) fill.style.width = `${percent}%`;
    if (curTime) curTime.textContent = this.formatTime(cur);
    if (totTime) totTime.textContent = this.formatTime(dur);

    // Update Fullscreen Controls
    const fsFill = document.getElementById('fsScrubFill');
    const fsCurTime = document.getElementById('fsCurrentTime');
    const fsTotTime = document.getElementById('fsTotalTime');

    if (fsFill) fsFill.style.width = `${percent}%`;
    if (fsCurTime) fsCurTime.textContent = this.formatTime(cur);
    if (fsTotTime) fsTotTime.textContent = this.formatTime(dur);

    this.syncLyrics(cur);
  }

  formatTime(sec) {
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  renderLyrics() {
    const stack = document.getElementById('fsLyricsStack');
    if (!stack) return;
    stack.innerHTML = '';

    if (!this.lyricsData || this.lyricsData.length === 0) {
      stack.innerHTML = '<div class="lyric-item active">Instrumental • Enjoy the music</div>';
      return;
    }

    this.lyricsData.forEach((item, idx) => {
      const line = document.createElement('div');
      line.className = 'lyric-item';
      line.id = `fs-lyric-${idx}`;
      line.textContent = item.text;
      line.addEventListener('click', () => {
        this.audio.currentTime = item.time;
      });
      stack.appendChild(line);
    });
  }

  syncLyrics(curTime) {
    if (!this.lyricsData || this.lyricsData.length === 0) return;

    let active = -1;
    for (let i = 0; i < this.lyricsData.length; i++) {
      if (curTime >= this.lyricsData[i].time) active = i;
      else break;
    }

    if (active !== this.currentLyricIndex && active !== -1) {
      this.currentLyricIndex = active;
      const all = document.querySelectorAll('.lyric-item');
      all.forEach((el, idx) => {
        if (idx === active) {
          el.classList.add('active');
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        } else {
          el.classList.remove('active');
        }
      });
    }
  }

  renderQueue() {
    const list = document.getElementById('panelQueueList');
    const fsList = document.getElementById('fsQueueList');

    [list, fsList].forEach(container => {
      if (!container) return;
      container.innerHTML = '';

      this.queue.forEach((track, idx) => {
        const row = document.createElement('div');
        row.className = `track-row-card focusable ${idx === this.queueIndex ? 'active' : ''}`;
        row.tabIndex = 0;
        row.innerHTML = `
          <img class="track-row-thumb" src="${track.art}" alt="${track.title}">
          <div class="track-row-details">
            <div class="track-row-title">${track.title}</div>
            <div class="track-row-artist">${track.artist}</div>
          </div>
        `;
        row.addEventListener('click', () => {
          this.queueIndex = idx;
          this.loadTrack(this.queue[idx], true);
        });
        container.appendChild(row);
      });
    });
  }

  setSleepTimer(minutes) {
    this.sleepTimerMinutes = minutes;
    this.sleepTimerEndTimestamp = Date.now() + (minutes * 60 * 1000);
    console.log(`Sleep timer: ${minutes}m`);
  }
}

window.AuraPlayer = new AuraMusicPlayer();
