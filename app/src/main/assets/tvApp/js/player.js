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
        art: 'https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/61/e7/3f/61e73f94-018d-5f50-50ec-8521952bc72e/20UM1IM11629.rgb.jpg/600x600bb.jpg',
        audioUrl: 'https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/12/73/ca/1273ca46-233a-5331-189b-25ac1d656533/mzaf_976341070785891411.plus.aac.p.m4a',
        videoId: '4NRXx6U8ABQ',
        duration: 200
      },
      {
        id: 'track-2',
        title: 'Yellow',
        artist: 'Coldplay',
        album: 'Parachutes',
        art: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/f5/93/8c/f5938c49-964c-31d1-4b33-78b634f71fb7/190295978075.jpg/600x600bb.jpg',
        audioUrl: 'https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/66/f3/1a/66f31a76-a6ed-cb4c-f353-23310a7ae9a8/mzaf_10593596652344378873.plus.aac.p.m4a',
        videoId: 'yKNxeF4KMsY',
        duration: 269
      },
      {
        id: 'track-3',
        title: 'BIRDS OF A FEATHER',
        artist: 'Billie Eilish',
        album: 'HIT ME HARD AND SOFT',
        art: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/92/9f/69/929f69f1-9977-3a44-d674-11f70c852d1b/24UMGIM36186.rgb.jpg/600x600bb.jpg',
        audioUrl: 'https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/34/31/d3/3431d34e-847f-5d66-df83-0bce688d997e/mzaf_18106743962423782018.plus.aac.p.m4a',
        videoId: 'd5gf9dXHevw',
        duration: 196
      },
      {
        id: 'track-4',
        title: 'Espresso',
        artist: 'Sabrina Carpenter',
        album: 'Short n Sweet',
        art: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/57/e8/7b/57e87ba0-5057-9bb9-c247-ce7dbe426e89/24UMGIM55213.rgb.jpg/600x600bb.jpg',
        audioUrl: 'https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/e9/4d/02/e94d0230-11ee-ef94-d2cf-a5d547bd73f4/mzaf_554140808559155562.plus.aac.p.m4a',
        videoId: 'eVli-tstM5E',
        duration: 175
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
    } else if (!document.getElementById('yt-api-script')) {
      const tag = document.createElement('script');
      tag.id = 'yt-api-script';
      tag.src = 'https://www.youtube.com/iframe_api';
      tag.async = true;
      tag.onerror = () => {
        console.warn('YouTube IFrame API unreachable; falling back to direct audio engine.');
      };
      document.head.appendChild(tag);
    }
  }

  setupYtPlayer() {
    try {
      const validOrigin = (window.location.protocol === 'http:' || window.location.protocol === 'https:')
        ? window.location.origin
        : undefined;

      const playerVars = {
        autoplay: 0,
        controls: 0,
        disablekb: 1,
        fs: 0,
        rel: 0,
        enablejsapi: 1
      };
      if (validOrigin) {
        playerVars.origin = validOrigin;
      }

      this.ytPlayer = new YT.Player('yt-player-container', {
        height: '120',
        width: '120',
        playerVars: playerVars,
        events: {
          onReady: () => {
            this.isYtReady = true;
            try { this.ytPlayer.unMute(); } catch (e) {}
            console.log('YouTube Audio Engine Ready on TV');
            if (this.currentTrack && this.currentTrack.videoId && this.isPlaying && this.playbackEngine === 'yt') {
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
            this.playbackEngine = 'html5';
            if (this.currentTrack && this.currentTrack.audioUrl) {
              this.audio.src = this.currentTrack.audioUrl;
              this.audio.play().then(() => {
                this.isPlaying = true;
                document.body.classList.add('playing');
              }).catch(e => console.warn(e));
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

    // Update dynamic ambient background gradient for TV
    this.updateAmbientGlow(track);

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

    // Playback Engine Resolution:
    // Full-length song priority: YouTube provides 100% full-length playback (3-5+ mins).
    // While YouTube initializes or resolves, HTML5 audio preview starts immediately for instant sub-50ms feedback.
    const playFullYouTube = (vid) => {
      if (this.isYtReady && this.ytPlayer && this.ytPlayer.loadVideoById) {
        this.playbackEngine = 'yt';
        this.audio.pause();
        this.ytPlayer.loadVideoById(vid);
        if (!autoPlay) this.ytPlayer.pauseVideo();
      }
    };

    if (track.videoId && this.isYtReady && this.ytPlayer && this.ytPlayer.loadVideoById) {
      playFullYouTube(track.videoId);
    } else {
      // Immediate low-latency start using direct audio stream
      if (track.audioUrl) {
        this.playbackEngine = 'html5';
        this.audio.src = track.audioUrl;
        if (autoPlay) this.play();
      } else if (window.AuraMusicApi) {
        window.AuraMusicApi.getDirectAudioStream(track.title, track.artist).then(info => {
          if (info && this.currentTrack && this.currentTrack.id === track.id) {
            track.audioUrl = info.audioUrl;
            if (!track.art || track.art.includes('unsplash')) {
              track.art = info.art;
              if (barThumb) barThumb.src = track.art;
              if (fsAlbumArt) fsAlbumArt.src = track.art;
            }
            if (this.playbackEngine !== 'yt') {
              this.audio.src = track.audioUrl;
              if (autoPlay) this.play();
            }
          }
        });
      }

      // Transition to full-length song as soon as videoId is ready
      if (window.AuraMusicApi) {
        if (track.videoId) {
          const checkReady = setInterval(() => {
            if (this.isYtReady && this.ytPlayer && this.ytPlayer.loadVideoById) {
              clearInterval(checkReady);
              if (this.currentTrack && this.currentTrack.id === track.id) {
                playFullYouTube(track.videoId);
              }
            }
          }, 250);
          setTimeout(() => clearInterval(checkReady), 4000);
        } else {
          window.AuraMusicApi.getYoutubeVideoId(track.title, track.artist).then(vid => {
            if (vid && this.currentTrack && this.currentTrack.id === track.id) {
              track.videoId = vid;
              playFullYouTube(vid);
            }
          });
        }
      }
    }
  }

  play() {
    if (this.playbackEngine === 'yt' && this.isYtReady && this.ytPlayer && this.ytPlayer.playVideo) {
      try {
        this.ytPlayer.playVideo();
        this.isPlaying = true;
        document.body.classList.add('playing');
        return;
      } catch (e) {}
    }

    if (this.audio.src) {
      this.playbackEngine = 'html5';
      this.audio.play().then(() => {
        this.isPlaying = true;
        document.body.classList.add('playing');
      }).catch(e => console.warn('Audio play policy:', e));
    } else if (this.currentTrack) {
      this.loadTrack(this.currentTrack, true);
    }
  }

  pause() {
    if (this.ytPlayer && this.ytPlayer.pauseVideo) {
      try { this.ytPlayer.pauseVideo(); } catch (e) {}
    }
    this.audio.pause();
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

  playNext() {
    this.nextTrack();
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

  playPrevious() {
    this.prevTrack();
  }

  toggleShuffle() {
    this.isShuffle = !this.isShuffle;
    const btn = document.getElementById('barShuffleBtn');
    const fsBtn = document.getElementById('fsShuffleBtn');
    [btn, fsBtn].forEach(b => {
      if (b) b.style.color = this.isShuffle ? 'var(--nt-red)' : 'var(--nt-muted)';
    });
  }

  updateAmbientGlow(track) {
    const fsPlayer = document.getElementById('fullscreenPlayer');
    if (!fsPlayer) return;
    // Derive a rich visual hue from the track title + artist
    const str = `${track.title || ''}${track.artist || ''}`;
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const h1 = Math.abs(hash) % 360;
    const h2 = (h1 + 45) % 360;
    fsPlayer.style.background = `radial-gradient(circle at 25% 35%, hsla(${h1}, 75%, 20%, 0.65) 0%, #0A0A0C 70%), radial-gradient(circle at 75% 65%, hsla(${h2}, 80%, 16%, 0.5) 0%, transparent 60%)`;
  }

  bindAudioEvents() {
    this.audio.addEventListener('timeupdate', () => {
      if (this.playbackEngine === 'html5') this.onTimeUpdate();
    });

    this.audio.addEventListener('ended', () => {
      if (this.sleepTimerEndTimestamp && Date.now() >= this.sleepTimerEndTimestamp) {
        this.pause();
        this.sleepTimerEndTimestamp = null;
        console.log('Aura Sleep Timer finished playback.');
        return;
      }
      this.nextTrack();
    });

    this.audio.addEventListener('error', (e) => {
      console.warn('HTML5 Audio stream error:', e);
      if (this.currentTrack && !this.currentTrack._retried && window.AuraMusicApi) {
        this.currentTrack._retried = true;
        window.AuraMusicApi.getDirectAudioStream(this.currentTrack.title, this.currentTrack.artist).then(info => {
          if (info && info.audioUrl && this.currentTrack) {
            this.currentTrack.audioUrl = info.audioUrl;
            this.audio.src = info.audioUrl;
            this.play();
          }
        });
      }
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
