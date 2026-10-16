/**
 * AuraMusic Desktop-Style Application Controller
 * Handles Desktop navigation, quick picks, carousels, LandscapePlayer overlay, and panels.
 */

class AuraAppController {
  constructor() {
    this.currentView = 'home';
    this.searchQuery = '';
    this.init();
  }

  init() {
    this.setupGreeting();
    this.populateHomeFeed();
    this.bindEvents();
    this.setupKeyboard();
  }

  setupGreeting() {
    const el = document.getElementById('greetingTitle');
    if (!el) return;
    const hour = new Date().getHours();
    let text = "Good evening, Audiophile";
    if (hour >= 5 && hour < 12) text = "Good morning, Audiophile";
    else if (hour >= 12 && hour < 17) text = "Good afternoon, Audiophile";
    el.textContent = text;
  }

  populateHomeFeed() {
    // 1. Quick Picks (4-column compact track rows like Desktop HomeScreen.kt)
    const quickPicks = [
      { id: 'qp-1', title: 'Midnight Echoes', artist: 'Aura Sound Lab', art: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=200' },
      { id: 'qp-2', title: 'Neon Horizon', artist: 'The Cyber Waves', art: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=200' },
      { id: 'qp-3', title: 'Tokyo Rain Walk', artist: 'Komorebi Quartet', art: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=200' },
      { id: 'qp-4', title: 'Starlight Odyssey', artist: 'Aura Symphony', art: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=200' },
      { id: 'qp-5', title: 'Blinding Lights', artist: 'The Weeknd', art: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=200' },
      { id: 'qp-6', title: 'Birds of a Feather', artist: 'Billie Eilish', art: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=200' },
      { id: 'qp-7', title: 'Espresso', artist: 'Sabrina Carpenter', art: 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=200' },
      { id: 'qp-8', title: 'Velvet Sunset', artist: 'Solaris Duo', art: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200' }
    ];

    const qpGrid = document.getElementById('grid-quick-picks');
    if (qpGrid) {
      qpGrid.innerHTML = '';
      quickPicks.forEach(track => {
        const row = document.createElement('div');
        row.className = 'track-row-card focusable';
        row.tabIndex = 0;
        row.innerHTML = `
          <img class="track-row-thumb" src="${track.art}" alt="${track.title}">
          <div class="track-row-details">
            <div class="track-row-title">${track.title}</div>
            <div class="track-row-artist">${track.artist}</div>
          </div>
        `;
        row.addEventListener('click', () => {
          if (window.AuraPlayer) {
            const match = window.AuraPlayer.tracks.find(t => t.title === track.title) || window.AuraPlayer.tracks[0];
            window.AuraPlayer.loadTrack(match, true);
          }
        });
        qpGrid.appendChild(row);
      });
    }

    // 2. Listen Again (Square album covers)
    const listenAgain = [
      { title: 'Spatial Acoustics', sub: 'Album • Aura Sound Lab', art: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400' },
      { title: 'Nothing Synthwave', sub: 'Album • The Cyber Waves', art: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400' },
      { title: 'Chill Sessions', sub: 'Playlist • 45 songs', art: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400' },
      { title: 'Deep Space', sub: 'EP • 6 tracks', art: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=400' },
      { title: 'Late Night Lo-Fi', sub: 'Playlist • Curated', art: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400' }
    ];

    this.renderCarousel('carousel-listen-again', listenAgain);

    // 3. Forgotten Favorites
    const forgotten = [
      { title: 'After Hours', sub: 'Album • The Weeknd', art: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400' },
      { title: 'Happier Than Ever', sub: 'Album • Billie Eilish', art: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400' },
      { title: 'Coffeehouse Acoustic', sub: 'Playlist • Relax', art: 'https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=400' },
      { title: 'Nordic Forest Ambient', sub: 'Album • Fjord Audio', art: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=400' }
    ];

    this.renderCarousel('carousel-forgotten', forgotten);
  }

  renderCarousel(carouselId, items) {
    const el = document.getElementById(carouselId);
    if (!el) return;
    el.innerHTML = '';

    items.forEach(item => {
      const card = document.createElement('div');
      card.className = 'album-square-card focusable';
      card.tabIndex = 0;
      card.innerHTML = `
        <img class="album-cover-img" src="${item.art}" alt="${item.title}">
        <div class="album-card-title">${item.title}</div>
        <div class="album-card-sub">${item.sub}</div>
      `;
      card.addEventListener('click', () => {
        if (window.AuraPlayer) {
          window.AuraPlayer.loadTrack(window.AuraPlayer.tracks[0], true);
        }
      });
      el.appendChild(card);
    });
  }

  switchView(viewName) {
    this.currentView = viewName;
    const views = document.querySelectorAll('.feed-view');
    views.forEach(v => v.classList.remove('active'));

    const target = document.getElementById(`view-${viewName}`);
    if (target) target.classList.add('active');

    const navs = document.querySelectorAll('.nav-btn');
    navs.forEach(n => {
      if (n.getAttribute('data-view') === viewName) n.classList.add('active');
      else n.classList.remove('active');
    });

    if (window.SpatialNav) window.SpatialNav.refreshFocusables();
  }

  openFullscreenPlayer() {
    const fs = document.getElementById('fullscreenPlayer');
    if (fs) {
      fs.classList.add('open');
      setTimeout(() => {
        if (window.SpatialNav) {
          window.SpatialNav.refreshFocusables();
          const closeBtn = document.getElementById('btn-fs-close');
          if (closeBtn) window.SpatialNav.setFocus(closeBtn);
        }
      }, 100);
    }
  }

  closeFullscreenPlayer() {
    const fs = document.getElementById('fullscreenPlayer');
    if (fs) {
      fs.classList.remove('open');
      if (window.SpatialNav) window.SpatialNav.refreshFocusables();
    }
  }

  toggleQueuePanel() {
    const p = document.getElementById('desktopQueuePanel');
    if (p) {
      p.classList.toggle('open');
      if (window.SpatialNav) window.SpatialNav.refreshFocusables();
    }
  }

  toggleSleepTimer() {
    const modal = document.getElementById('sleepTimerSheet');
    if (modal) {
      modal.classList.toggle('open');
      if (window.SpatialNav) window.SpatialNav.refreshFocusables();
    }
  }

  closeSleepTimer() {
    const modal = document.getElementById('sleepTimerSheet');
    if (modal) {
      modal.classList.remove('open');
      if (window.SpatialNav) window.SpatialNav.refreshFocusables();
    }
  }

  handleBack() {
    const modal = document.getElementById('sleepTimerSheet');
    if (modal && modal.classList.contains('open')) {
      this.closeSleepTimer();
      return;
    }
    const qp = document.getElementById('desktopQueuePanel');
    if (qp && qp.classList.contains('open')) {
      this.toggleQueuePanel();
      return;
    }
    const fs = document.getElementById('fullscreenPlayer');
    if (fs && fs.classList.contains('open')) {
      this.closeFullscreenPlayer();
      return;
    }
    if (this.currentView !== 'home') {
      this.switchView('home');
    }
  }

  setupKeyboard() {
    const keys = document.querySelectorAll('.kbd-key');
    const input = document.getElementById('searchInput');

    keys.forEach(k => {
      k.addEventListener('click', () => {
        const ch = k.getAttribute('data-char');
        if (ch === 'SPACE') this.searchQuery += ' ';
        else if (ch === 'BACKSPACE') this.searchQuery = this.searchQuery.slice(0, -1);
        else this.searchQuery += ch;

        if (input) input.value = this.searchQuery;
        this.runSearch(this.searchQuery);
      });
    });
  }

  runSearch(query) {
    const container = document.getElementById('search-results-grid');
    if (!container) return;
    container.innerHTML = '';
    if (!query) return;

    if (window.AuraPlayer) {
      const matches = window.AuraPlayer.tracks.filter(t => 
        t.title.toLowerCase().includes(query.toLowerCase()) || 
        t.artist.toLowerCase().includes(query.toLowerCase())
      );
      matches.forEach(t => {
        const row = document.createElement('div');
        row.className = 'track-row-card focusable';
        row.tabIndex = 0;
        row.innerHTML = `
          <img class="track-row-thumb" src="${t.art}" alt="${t.title}">
          <div class="track-row-details">
            <div class="track-row-title">${t.title}</div>
            <div class="track-row-artist">${t.artist}</div>
          </div>
        `;
        row.addEventListener('click', () => {
          window.AuraPlayer.loadTrack(t, true);
        });
        container.appendChild(row);
      });
    }
  }

  bindEvents() {
    // Navigation
    document.querySelectorAll('.nav-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.switchView(btn.getAttribute('data-view'));
      });
    });

    // Topbar search click
    const topSearch = document.getElementById('topbarSearch');
    if (topSearch) topSearch.addEventListener('click', () => this.switchView('search'));

    // Persistent player bar controls
    const miniCard = document.getElementById('playerMiniCard');
    if (miniCard) miniCard.addEventListener('click', () => this.openFullscreenPlayer());

    const playPause = document.getElementById('barPlayPauseBtn');
    if (playPause) playPause.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.togglePlay());

    const next = document.getElementById('barNextBtn');
    if (next) next.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.nextTrack());

    const prev = document.getElementById('barPrevBtn');
    if (prev) prev.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.prevTrack());

    const shuffle = document.getElementById('barShuffleBtn');
    if (shuffle) shuffle.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.toggleShuffle());

    const lyricsBtn = document.getElementById('barLyricsBtn');
    if (lyricsBtn) lyricsBtn.addEventListener('click', () => this.openFullscreenPlayer());

    const queueBtn = document.getElementById('barQueueBtn');
    if (queueBtn) queueBtn.addEventListener('click', () => this.toggleQueuePanel());

    const timerBtn = document.getElementById('barTimerBtn');
    if (timerBtn) timerBtn.addEventListener('click', () => this.toggleSleepTimer());

    // Fullscreen controls
    const fsPlay = document.getElementById('fsPlayPauseBtn');
    if (fsPlay) fsPlay.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.togglePlay());

    const fsNext = document.getElementById('fsNextBtn');
    if (fsNext) fsNext.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.nextTrack());

    const fsPrev = document.getElementById('fsPrevBtn');
    if (fsPrev) fsPrev.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.prevTrack());

    const fsShuffle = document.getElementById('fsShuffleBtn');
    if (fsShuffle) fsShuffle.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.toggleShuffle());

    // Fullscreen Tabs (Lyrics vs Up Next Queue)
    const tabLyrics = document.getElementById('tab-fs-lyrics');
    const tabQueue = document.getElementById('tab-fs-queue');
    const containerLyrics = document.getElementById('fsLyricsContainer');
    const containerQueue = document.getElementById('fsQueueContainer');

    if (tabLyrics && tabQueue) {
      tabLyrics.addEventListener('click', () => {
        tabLyrics.classList.add('active');
        tabQueue.classList.remove('active');
        if (containerLyrics) containerLyrics.style.display = 'flex';
        if (containerQueue) containerQueue.style.display = 'none';
        if (window.SpatialNav) window.SpatialNav.refreshFocusables();
      });

      tabQueue.addEventListener('click', () => {
        tabQueue.classList.add('active');
        tabLyrics.classList.remove('active');
        if (containerLyrics) containerLyrics.style.display = 'none';
        if (containerQueue) containerQueue.style.display = 'flex';
        if (window.SpatialNav) window.SpatialNav.refreshFocusables();
      });
    }

    // Fullscreen close
    const fsClose = document.getElementById('btn-fs-close');
    if (fsClose) fsClose.addEventListener('click', () => this.closeFullscreenPlayer());

    const qClose = document.getElementById('btn-queue-close');
    if (qClose) qClose.addEventListener('click', () => this.toggleQueuePanel());

    const tCancel = document.getElementById('btn-timer-cancel');
    if (tCancel) tCancel.addEventListener('click', () => this.closeSleepTimer());

    // Timer pills
    document.querySelectorAll('.timer-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        const mins = pill.getAttribute('data-mins');
        if (window.AuraPlayer) {
          if (mins === 'album') window.AuraPlayer.setSleepTimer(45);
          else window.AuraPlayer.setSleepTimer(parseInt(mins, 10));
        }
        this.closeSleepTimer();
      });
    });
  }
}

window.AuraApp = new AuraAppController();
