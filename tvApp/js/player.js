/**
 * AuraMusic Desktop-Style Player & Synced Lyrics Engine for Smart TV
 * Dual-engine: Powered by hidden YouTube Audio Player with HTML5 Audio fallback.
 * Keeps persistent bottom bar and fullscreen LandscapePlayer synchronized.
 */

class AuraMusicPlayer {
  constructor() {
    this.audio = new Audio();
    this.ytPlayer = null;
    this.isYtReady = false;
    this.playbackEngine = 'html5'; // 'yt' or 'html5'

    this.currentTrack = null;
    this.queue = [];
    this.queueIndex = 0;
    this.isPlaying = false;
    this.isShuffle = false;
    this.sleepTimerMinutes = null;
    this.sleepTimerEndTimestamp = null;
    this.lyricsData = [];
    this.currentLyricIndex = -1;
    this.timerInterval = null;

    this.tracks = [
      {
        id: 'track-1',
        title: 'Blinding Lights',
        artist: 'The Weeknd',
        album: 'After Hours',
        art: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3',
        videoId: '4NRXx6U8ABQ',
        duration: 200
      },
      {
        id: 'track-2',
        title: 'Yellow',
        artist: 'Coldplay',
        album: 'Parachutes',
        art: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=lifelike-126735.mp3',
        videoId: 'yKNxeF4KMsY',
        duration: 269
      },
      {
        id: 'track-3',
        title: 'Birds of a Feather',
        artist: 'Billie Eilish',
        album: 'HIT ME HARD AND SOFT',
        art: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f77cb7.mp3?filename=chill-abstract-intention-12099.mp3',
        videoId: 'd5gf9dXHevw',
        duration: 196
      },
      {
        id: 'track-4',
        title: 'Midnight Echoes',
        artist: 'Aura Sound Lab',
        album: 'Spatial Acoustics',
        art: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800',
        audioUrl: 'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3',
        duration: 164
      }
    ];

    this.queue = [...this.tracks];
    this.init();
  }

  init() {
    this.bindAudioEvents();
    this.initYouTubeBridge();
    this.startScrubTimer();
    this.loadTrack(this.queue[0], false);
  }

  initYouTubeBridge() {
    window.onYouTubeIframeAPIReady = () => {
      this.setupYtPlayer();
    };

    if (window.YT && window.YT.Player) {
      this.setupYtPlayer();
    }
  }

  setupYtPlayer() {
    try {
      this.ytPlayer = new YT.Player('yt-player-container', {
        height: '200',
        width: '200',
        playerVars: {
          autoplay: 0,
          controls: 0,
          disablekb: 1,
          fs: 0,
          rel: 0,
          enablejsapi: 1
        },
        events: {
          onReady: () => {
            this.isYtReady = true;
            console.log('YouTube Audio Engine Ready on TV');
            if (this.currentTrack && this.currentTrack.videoId && this.isPlaying) {
              this.ytPlayer.loadVideoById(this.currentTrack.videoId);
            }
          },
          onStateChange: (event) => {
            if (event.data === YT.PlayerState.PLAYING) {
              this.isPlaying = true;
              this.playbackEngine = 'yt';
              this.audio.pause();
              document.body.classList.add('playing');
            } else if (event.data === YT.PlayerState.PAUSED) {
              this.isPlaying = false;
              document.body.classList.remove('playing');
            } else if (event.data === YT.PlayerState.ENDED) {
              this.nextTrack();
            }
          },
          onError: (err) => {
            console.warn('YouTube engine error, falling back to direct audio stream:', err);
            if (this.currentTrack && this.currentTrack.audioUrl) {
              this.playbackEngine = 'html5';
              this.audio.play().catch(e => console.warn(e));
            }
          }
        }
      });
    } catch (e) {
      console.warn('YT Player init error:', e);
    }
  }

  startScrubTimer() {
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => {
      if (this.isPlaying) {
        this.onTimeUpdate();
      }
    }, 250);
  }

  loadTrack(track, autoPlay = true) {
    if (!track) return;
    this.currentTrack = track;
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

    // Dynamically fetch live synced lyrics if missing
    if ((!track.lyrics || track.lyrics.length === 0) && window.AuraMusicApi) {
      window.AuraMusicApi.fetchSyncedLyrics(track.title, track.artist).then(lyrics => {
        if (this.currentTrack && this.currentTrack.id === track.id) {
          this.lyricsData = lyrics;
          this.renderLyrics();
        }
      });
    }

    // Playback Engine Resolution
    const startPlayback = () => {
      if (track.videoId && this.isYtReady && this.ytPlayer && this.ytPlayer.loadVideoById) {
        this.playbackEngine = 'yt';
        this.audio.pause();
        this.ytPlayer.loadVideoById(track.videoId);
        if (!autoPlay) this.ytPlayer.pauseVideo();
      } else if (track.audioUrl) {
        this.playbackEngine = 'html5';
        this.audio.src = track.audioUrl;
        if (autoPlay) this.play();
      }
    };

    if (track.videoId) {
      startPlayback();
    } else if (window.AuraMusicApi) {
      // Resolve YouTube video ID in background
      window.AuraMusicApi.getYoutubeVideoId(track.title, track.artist).then(vid => {
        if (vid) {
          track.videoId = vid;
          if (this.currentTrack && this.currentTrack.id === track.id) {
            startPlayback();
          }
        } else if (track.audioUrl) {
          this.playbackEngine = 'html5';
          this.audio.src = track.audioUrl;
          if (autoPlay) this.play();
        }
      });

      // While resolving YouTube, start HTML5 audio preview immediately if present
      if (track.audioUrl) {
        this.playbackEngine = 'html5';
        this.audio.src = track.audioUrl;
        if (autoPlay) this.play();
      }
    } else if (track.audioUrl) {
      this.playbackEngine = 'html5';
      this.audio.src = track.audioUrl;
      if (autoPlay) this.play();
    }
  }

  play() {
    if (this.playbackEngine === 'yt' && this.isYtReady && this.ytPlayer && this.ytPlayer.playVideo) {
      this.ytPlayer.playVideo();
      this.isPlaying = true;
      document.body.classList.add('playing');
    } else {
      this.audio.play().then(() => {
        this.isPlaying = true;
        document.body.classList.add('playing');
      }).catch(e => console.warn('Audio play policy:', e));
    }
  }

  pause() {
    if (this.playbackEngine === 'yt' && this.isYtReady && this.ytPlayer && this.ytPlayer.pauseVideo) {
      this.ytPlayer.pauseVideo();
    } else {
      this.audio.pause();
    }
    this.isPlaying = false;
    document.body.classList.remove('playing');
  }

  togglePlay() {
    if (this.isPlaying) this.pause();
    else this.play();
  }

  seekTo(sec) {
    if (this.playbackEngine === 'yt' && this.isYtReady && this.ytPlayer && this.ytPlayer.seekTo) {
      this.ytPlayer.seekTo(sec, true);
    } else {
      this.audio.currentTime = sec;
    }
  }

  nextTrack() {
    if (this.queue.length === 0) return;
    this.queueIndex = (this.queueIndex + 1) % this.queue.length;
    this.loadTrack(this.queue[this.queueIndex], true);
  }

  prevTrack() {
    let cur = this.getCurrentTime();
    if (cur > 3) {
      this.seekTo(0);
      return;
    }
    if (this.queue.length === 0) return;
    this.queueIndex = (this.queueIndex - 1 + this.queue.length) % this.queue.length;
    this.loadTrack(this.queue[this.queueIndex], true);
  }

  toggleShuffle() {
    this.isShuffle = !this.isShuffle;
    const btn = document.getElementById('barShuffleBtn');
    const fsBtn = document.getElementById('fsShuffleBtn');
    [btn, fsBtn].forEach(b => {
      if (b) b.style.color = this.isShuffle ? 'var(--nt-red)' : 'var(--nt-muted)';
    });
  }

  bindAudioEvents() {
    this.audio.addEventListener('timeupdate', () => {
      if (this.playbackEngine === 'html5') this.onTimeUpdate();
    });

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

  getCurrentTime() {
    if (this.playbackEngine === 'yt' && this.isYtReady && this.ytPlayer && this.ytPlayer.getCurrentTime) {
      return this.ytPlayer.getCurrentTime() || 0;
    }
    return this.audio.currentTime || 0;
  }

  getDuration() {
    if (this.playbackEngine === 'yt' && this.isYtReady && this.ytPlayer && this.ytPlayer.getDuration) {
      const d = this.ytPlayer.getDuration();
      if (d && d > 0) return d;
    }
    return this.audio.duration || this.currentTrack?.duration || 180;
  }

  onTimeUpdate() {
    const cur = this.getCurrentTime();
    const dur = this.getDuration();
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
        this.seekTo(item.time);
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
