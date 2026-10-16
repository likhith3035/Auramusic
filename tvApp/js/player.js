/**
 * AuraMusic TV Audio Playback & Synced Lyrics Engine
 * Provides audio playback, lyrics synchronization, dynamic ambient lighting, and OLED burn-in shielding.
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
    this.burnInTimer = null;
    this.lyricsData = [];
    this.currentLyricIndex = -1;

    this.sampleTracks = [
      {
        id: 'track-1',
        title: 'Midnight Echoes (Aura Lo-Fi)',
        artist: 'Aura Sound Lab',
        album: 'Spatial Living Room Vol. 1',
        art: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800&auto=format&fit=crop&q=80',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3',
        glowColor: '#00E5FF',
        duration: 164,
        lyrics: [
          { time: 0, text: "♪ Ambient soundscapes opening..." },
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
        album: 'Nothing Synthwave 2026',
        art: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=lifelike-126735.mp3',
        glowColor: '#818CF8',
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
        glowColor: '#38BDF8',
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
        glowColor: '#E11D48',
        duration: 210,
        lyrics: [
          { time: 0, text: "♪ Cosmic drone fills the space..." },
          { time: 20, text: "Beyond the clouds, past the moon" },
          { time: 40, text: "Endless galaxies in silence" },
          { time: 65, text: "Music without gravity" },
          { time: 95, text: "Lost in the celestial stream" }
        ]
      }
    ];

    this.queue = [...this.sampleTracks];
    this.init();
  }

  init() {
    this.bindAudioEvents();
    // Load first track into player
    this.loadTrack(this.queue[0], false);
    this.initBurnInShield();
  }

  loadTrack(track, autoPlay = true) {
    if (!track) return;
    this.currentTrack = track;
    this.audio.src = track.audioUrl;
    this.lyricsData = track.lyrics || [];
    this.currentLyricIndex = -1;

    // Update UI elements
    const playerTitle = document.getElementById('playerTitle');
    const playerArtist = document.getElementById('playerArtist');
    const playerAlbumArt = document.getElementById('playerAlbumArt');
    const vinylGlow = document.getElementById('vinylGlow');
    const ambientBackdrop = document.getElementById('ambientBackdrop');

    if (playerTitle) playerTitle.textContent = track.title;
    if (playerArtist) playerArtist.textContent = track.artist;
    if (playerAlbumArt) playerAlbumArt.src = track.art;

    // Update dynamic colors
    if (vinylGlow && track.glowColor) {
      vinylGlow.style.background = track.glowColor;
    }
    if (ambientBackdrop && track.glowColor) {
      const orb1 = ambientBackdrop.querySelector('.orb-1');
      if (orb1) orb1.style.background = `radial-gradient(circle, ${track.glowColor} 0%, transparent 70%)`;
    }

    // Render lyrics in Now Playing column
    this.renderLyrics();

    // Update queue list
    this.renderQueue();

    if (autoPlay) {
      this.play();
    }
  }

  play() {
    this.audio.play().then(() => {
      this.isPlaying = true;
      document.body.classList.add('playing');
    }).catch(err => {
      console.warn("Autoplay policy or audio fetch:", err);
    });
  }

  pause() {
    this.audio.pause();
    this.isPlaying = false;
    document.body.classList.remove('playing');
  }

  togglePlay() {
    if (this.isPlaying) {
      this.pause();
    } else {
      this.play();
    }
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
    const btn = document.getElementById('btn-shuffle');
    if (btn) {
      btn.style.color = this.isShuffle ? 'var(--aura-cyan)' : '#FFFFFF';
    }
  }

  bindAudioEvents() {
    this.audio.addEventListener('timeupdate', () => {
      this.onTimeUpdate();
    });

    this.audio.addEventListener('ended', () => {
      // Check sleep timer end-of-track condition
      if (this.sleepTimerEndTimestamp && Date.now() >= this.sleepTimerEndTimestamp) {
        this.triggerSleepTimerStop();
        return;
      }
      this.nextTrack();
    });
  }

  onTimeUpdate() {
    const cur = this.audio.currentTime || 0;
    const dur = this.audio.duration || this.currentTrack?.duration || 1;

    // Progress bar update
    const percent = Math.min((cur / dur) * 100, 100);
    const progressFill = document.getElementById('progressFill');
    const currentTime = document.getElementById('currentTime');
    const totalDuration = document.getElementById('totalDuration');

    if (progressFill) progressFill.style.width = `${percent}%`;
    if (currentTime) currentTime.textContent = this.formatTime(cur);
    if (totalDuration) totalDuration.textContent = this.formatTime(dur);

    // Synchronized Karaoke Lyrics Scrolling
    this.syncLyrics(cur);
  }

  formatTime(seconds) {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  renderLyrics() {
    const container = document.getElementById('lyricsList');
    if (!container) return;
    container.innerHTML = '';

    if (!this.lyricsData || this.lyricsData.length === 0) {
      container.innerHTML = '<div class="lyric-line active">Instrumental playback • Enjoy the music</div>';
      return;
    }

    this.lyricsData.forEach((item, index) => {
      const line = document.createElement('div');
      line.className = 'lyric-line';
      line.id = `lyric-line-${index}`;
      line.textContent = item.text;
      line.addEventListener('click', () => {
        this.audio.currentTime = item.time;
      });
      container.appendChild(line);
    });
  }

  syncLyrics(currentTime) {
    if (!this.lyricsData || this.lyricsData.length === 0) return;

    let activeIndex = -1;
    for (let i = 0; i < this.lyricsData.length; i++) {
      if (currentTime >= this.lyricsData[i].time) {
        activeIndex = i;
      } else {
        break;
      }
    }

    if (activeIndex !== this.currentLyricIndex && activeIndex !== -1) {
      this.currentLyricIndex = activeIndex;

      // Update classes
      const allLines = document.querySelectorAll('.lyric-line');
      allLines.forEach((el, idx) => {
        if (idx === activeIndex) {
          el.classList.add('active');
          // Smooth scroll to center this lyric line in TV viewport
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        } else {
          el.classList.remove('active');
        }
      });
    }
  }

  renderQueue() {
    const list = document.getElementById('queueList');
    const count = document.getElementById('queueCount');
    if (!list) return;
    list.innerHTML = '';
    if (count) count.textContent = `${this.queue.length} tracks`;

    this.queue.forEach((track, index) => {
      const item = document.createElement('div');
      item.className = `queue-item focusable ${index === this.queueIndex ? 'now-playing' : ''}`;
      item.tabIndex = 0;
      item.innerHTML = `
        <img class="queue-item-thumb" src="${track.art}" alt="${track.title}">
        <div class="queue-item-details">
          <div class="queue-item-title">${track.title}</div>
          <div class="queue-item-artist">${track.artist}</div>
        </div>
      `;
      item.addEventListener('click', () => {
        this.queueIndex = index;
        this.loadTrack(this.queue[index], true);
      });
      list.appendChild(item);
    });
  }

  setSleepTimer(minutes) {
    this.sleepTimerMinutes = minutes;
    this.sleepTimerEndTimestamp = Date.now() + (minutes * 60 * 1000);
    console.log(`Sleep timer set for ${minutes} minutes`);
  }

  triggerSleepTimerStop() {
    this.pause();
    this.sleepTimerMinutes = null;
    this.sleepTimerEndTimestamp = null;
    alert("🌙 Aura Sleep Timer completed. Pleasant dreams!");
  }

  initBurnInShield() {
    // OLED screen saver: subtle drifting of album art during long playback sessions
    let driftOffset = 0;
    setInterval(() => {
      if (this.isPlaying) {
        driftOffset = (driftOffset + 4) % 16;
        const container = document.getElementById('albumArtContainer');
        if (container) {
          container.style.transform = `translate(${driftOffset - 8}px, ${Math.sin(driftOffset) * 6}px)`;
        }
      }
    }, 15000);
  }
}

window.AuraPlayer = new AuraMusicPlayer();
