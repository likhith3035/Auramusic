/**
 * AuraMusic TV Main Application Controller
 * Handles view switching, shelf rendering, virtual TV remote keyboard, modals, and drawers.
 */

class AuraAppController {
  constructor() {
    this.currentView = 'home';
    this.previousView = 'home';
    this.searchQuery = '';
    this.init();
  }

  init() {
    this.setupGreeting();
    this.populateShelves();
    this.bindActionButtons();
    this.setupVirtualKeyboard();
  }

  setupGreeting() {
    const greetingEl = document.getElementById('heroGreeting');
    if (!greetingEl) return;
    const hour = new Date().getHours();
    let text = "Good Evening, Audiophile";
    if (hour >= 5 && hour < 12) text = "Good Morning, Audiophile";
    else if (hour >= 12 && hour < 17) text = "Good Afternoon, Audiophile";
    greetingEl.textContent = text;
  }

  populateShelves() {
    const dailyMixes = [
      {
        id: 'mix-1',
        title: 'Midnight Echoes',
        artist: 'Aura Sound Lab',
        art: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&auto=format&fit=crop&q=80',
        badge: 'HI-RES'
      },
      {
        id: 'mix-2',
        title: 'Neon Horizon',
        artist: 'The Cyber Waves',
        art: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80',
        badge: 'LOSSLESS'
      },
      {
        id: 'mix-3',
        title: 'Tokyo Rain Walk',
        artist: 'Komorebi Quartet',
        art: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=80',
        badge: 'FLAC'
      },
      {
        id: 'mix-4',
        title: 'Starlight Odyssey',
        artist: 'Aura Symphony',
        art: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500&auto=format&fit=crop&q=80',
        badge: '24-BIT'
      },
      {
        id: 'mix-5',
        title: 'Velvet Sunset',
        artist: 'Solaris Duo',
        art: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80',
        badge: 'HI-RES'
      }
    ];

    const trending = [
      {
        id: 'trend-1',
        title: 'Starboy Euphoria',
        artist: 'The Weeknd & Daft Punk',
        art: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80',
        badge: 'TOP #1'
      },
      {
        id: 'trend-2',
        title: 'Blinding Lights',
        artist: 'The Weeknd',
        art: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80',
        badge: 'POPULAR'
      },
      {
        id: 'trend-3',
        title: 'Birds of a Feather',
        artist: 'Billie Eilish',
        art: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&auto=format&fit=crop&q=80',
        badge: 'CHART'
      },
      {
        id: 'trend-4',
        title: 'Espresso',
        artist: 'Sabrina Carpenter',
        art: 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=500&auto=format&fit=crop&q=80',
        badge: 'VIRAL'
      }
    ];

    const ambient = [
      {
        id: 'amb-1',
        title: 'Rain on Nordic Pines',
        artist: 'Fjord Audio',
        art: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=500&auto=format&fit=crop&q=80',
        badge: 'SLEEP'
      },
      {
        id: 'amb-2',
        title: 'Deep Theta Waves',
        artist: 'Delta Frequency',
        art: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=80',
        badge: 'REST'
      },
      {
        id: 'amb-3',
        title: 'Coffeehouse Midnight',
        artist: 'Aura Chill Room',
        art: 'https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=500&auto=format&fit=crop&q=80',
        badge: 'LO-FI'
      }
    ];

    this.renderShelf('shelf-daily-mixes', dailyMixes);
    this.renderShelf('shelf-trending', trending);
    this.renderShelf('shelf-ambient', ambient);
  }

  renderShelf(shelfId, items) {
    const track = document.getElementById(shelfId);
    if (!track) return;
    track.innerHTML = '';

    items.forEach(item => {
      const card = document.createElement('div');
      card.className = 'tv-card focusable';
      card.tabIndex = 0;
      card.innerHTML = `
        <div class="card-thumb-wrap">
          <img class="card-thumb" src="${item.art}" alt="${item.title}">
          <div class="card-overlay-badge">${item.badge}</div>
        </div>
        <div class="card-title">${item.title}</div>
        <div class="card-artist">${item.artist}</div>
      `;

      card.addEventListener('click', () => {
        if (window.AuraPlayer) {
          // Check if track is in sampleTracks
          const matched = window.AuraPlayer.sampleTracks.find(t => t.title.toLowerCase().includes(item.title.toLowerCase())) || window.AuraPlayer.sampleTracks[0];
          window.AuraPlayer.loadTrack(matched, true);
          this.switchView('player');
        }
      });

      track.appendChild(card);
    });
  }

  switchView(viewName) {
    if (this.currentView === viewName) return;
    this.previousView = this.currentView;
    this.currentView = viewName;

    const allViews = document.querySelectorAll('.tv-view');
    allViews.forEach(v => v.classList.remove('active'));

    const targetView = document.getElementById(`view-${viewName}`);
    if (targetView) targetView.classList.add('active');

    // Update nav items
    const allNavs = document.querySelectorAll('.nav-item');
    allNavs.forEach(n => {
      if (n.getAttribute('data-nav') === viewName) {
        n.classList.add('active');
      } else {
        n.classList.remove('active');
      }
    });

    // Close drawers and modals on view switch
    this.closeDrawer();
    this.closeModal();

    // Auto-focus first element in new view
    setTimeout(() => {
      if (window.SpatialNav) {
        window.SpatialNav.refreshFocusables();
        if (viewName === 'player') {
          const playBtn = document.getElementById('btn-play-pause');
          if (playBtn) window.SpatialNav.setFocus(playBtn);
        } else if (viewName === 'search') {
          const firstKey = document.querySelector('.key-btn');
          if (firstKey) window.SpatialNav.setFocus(firstKey);
        } else {
          const heroPlay = document.getElementById('btn-hero-play');
          if (heroPlay) window.SpatialNav.setFocus(heroPlay);
        }
      }
    }, 150);
  }

  toggleQueue() {
    const drawer = document.getElementById('queueDrawer');
    if (!drawer) return;
    drawer.classList.toggle('open');
    if (drawer.classList.contains('open')) {
      setTimeout(() => {
        if (window.SpatialNav) {
          window.SpatialNav.refreshFocusables();
          const firstQueueItem = drawer.querySelector('.queue-item');
          if (firstQueueItem) window.SpatialNav.setFocus(firstQueueItem);
        }
      }, 150);
    } else {
      if (window.SpatialNav) window.SpatialNav.refreshFocusables();
    }
  }

  closeDrawer() {
    const drawer = document.getElementById('queueDrawer');
    if (drawer) drawer.classList.remove('open');
  }

  toggleSleepTimer() {
    const modal = document.getElementById('sleepTimerModal');
    if (!modal) return;
    modal.classList.toggle('open');
    if (modal.classList.contains('open')) {
      setTimeout(() => {
        if (window.SpatialNav) {
          window.SpatialNav.refreshFocusables();
          const firstTimer = modal.querySelector('.timer-card');
          if (firstTimer) window.SpatialNav.setFocus(firstTimer);
        }
      }, 150);
    } else {
      if (window.SpatialNav) window.SpatialNav.refreshFocusables();
    }
  }

  closeModal() {
    const modal = document.getElementById('sleepTimerModal');
    if (modal) modal.classList.remove('open');
  }

  handleBack() {
    // If a modal is open, close it
    const modal = document.getElementById('sleepTimerModal');
    if (modal && modal.classList.contains('open')) {
      this.closeModal();
      return;
    }

    // If queue drawer is open, close it
    const drawer = document.getElementById('queueDrawer');
    if (drawer && drawer.classList.contains('open')) {
      this.closeDrawer();
      return;
    }

    // If in Player or Search, go back to Home
    if (this.currentView !== 'home') {
      this.switchView('home');
    }
  }

  setupVirtualKeyboard() {
    const keys = document.querySelectorAll('.key-btn');
    const display = document.getElementById('searchDisplay');

    keys.forEach(btn => {
      btn.addEventListener('click', () => {
        const val = btn.getAttribute('data-key');
        if (val === 'SPACE') {
          this.searchQuery += ' ';
        } else if (val === 'BACKSPACE') {
          this.searchQuery = this.searchQuery.slice(0, -1);
        } else if (val === 'SEARCH') {
          this.executeSearch(this.searchQuery);
        } else {
          this.searchQuery += val;
        }

        if (display) {
          display.textContent = this.searchQuery.length > 0 ? this.searchQuery : "Search artists, songs, or albums...";
        }
        this.executeSearch(this.searchQuery);
      });
    });
  }

  executeSearch(query) {
    const resultsContainer = document.getElementById('shelf-search-results');
    if (!resultsContainer) return;
    resultsContainer.innerHTML = '';
    if (!query || query.trim() === '') return;

    if (window.AuraPlayer) {
      const filtered = window.AuraPlayer.sampleTracks.filter(t => 
        t.title.toLowerCase().includes(query.toLowerCase()) || 
        t.artist.toLowerCase().includes(query.toLowerCase())
      );

      filtered.forEach(track => {
        const card = document.createElement('div');
        card.className = 'tv-card focusable';
        card.tabIndex = 0;
        card.innerHTML = `
          <div class="card-thumb-wrap">
            <img class="card-thumb" src="${track.art}" alt="${track.title}">
            <div class="card-overlay-badge">MATCH</div>
          </div>
          <div class="card-title">${track.title}</div>
          <div class="card-artist">${track.artist}</div>
        `;
        card.addEventListener('click', () => {
          window.AuraPlayer.loadTrack(track, true);
          this.switchView('player');
        });
        resultsContainer.appendChild(card);
      });
    }
  }

  bindActionButtons() {
    // Navigation bar click listeners
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
      item.addEventListener('click', () => {
        const action = item.getAttribute('data-action');
        const nav = item.getAttribute('data-nav');
        if (action === 'switchView') this.switchView(nav);
        else if (action === 'toggleQueue') this.toggleQueue();
        else if (action === 'toggleSleepTimer') this.toggleSleepTimer();
      });
    });

    // Hero buttons
    const heroPlay = document.getElementById('btn-hero-play');
    if (heroPlay) {
      heroPlay.addEventListener('click', () => {
        if (window.AuraPlayer) {
          window.AuraPlayer.loadTrack(window.AuraPlayer.sampleTracks[0], true);
          this.switchView('player');
        }
      });
    }

    const heroLyrics = document.getElementById('btn-hero-lyrics');
    if (heroLyrics) {
      heroLyrics.addEventListener('click', () => {
        this.switchView('player');
      });
    }

    // Player action buttons
    const playPause = document.getElementById('btn-play-pause');
    if (playPause) playPause.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.togglePlay());

    const nextBtn = document.getElementById('btn-next');
    if (nextBtn) nextBtn.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.nextTrack());

    const prevBtn = document.getElementById('btn-prev');
    if (prevBtn) prevBtn.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.prevTrack());

    const shuffleBtn = document.getElementById('btn-shuffle');
    if (shuffleBtn) shuffleBtn.addEventListener('click', () => window.AuraPlayer && window.AuraPlayer.toggleShuffle());

    // Timer modal buttons
    const timerCards = document.querySelectorAll('.timer-card');
    timerCards.forEach(card => {
      card.addEventListener('click', () => {
        const timerVal = card.getAttribute('data-timer');
        if (window.AuraPlayer) {
          if (timerVal === 'screen-off') {
            alert("🌙 Screen saver activated: Audio continues while display minimizes energy.");
          } else if (timerVal === 'end-of-album') {
            window.AuraPlayer.setSleepTimer(45);
          } else {
            window.AuraPlayer.setSleepTimer(parseInt(timerVal, 10));
          }
        }
        this.closeModal();
      });
    });

    const cancelTimer = document.getElementById('btn-cancel-timer');
    if (cancelTimer) {
      cancelTimer.addEventListener('click', () => this.closeModal());
    }
  }
}

window.AuraApp = new AuraAppController();
