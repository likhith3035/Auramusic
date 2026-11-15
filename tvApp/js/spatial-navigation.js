/**
 * AuraMusic Flagship TV Spatial Navigation Engine (D-Pad Controller)
 * Engineered for 10-foot living room interaction matching Spotify and Apple Music TV.
 * Compatible with Samsung Tizen OS, LG webOS, Android TV, Google TV, and Desktop Keyboard.
 */

class SpatialNavigation {
  constructor() {
    this.currentFocus = null;
    this.focusableElements = [];
    this.viewFocusMemory = new Map();
    this.audioCtx = null;
    this.idleTimer = null;
    this.isIdle = false;
    this.init();
  }

  init() {
    this.initAudioTick();
    this.registerTizenKeys();
    this.bindEvents();
    this.initOledWatchdog();

    window.addEventListener('load', () => {
      this.refreshFocusables();
      const initial = document.getElementById('btn-hero-play') ||
                      document.querySelector('.nav-btn.active') ||
                      document.querySelector('.focusable');
      if (initial) this.setFocus(initial, false);
    });
  }

  /**
   * Register Smart TV Remote keys on Samsung Tizen OS so media and numeric keys fire keydown events.
   */
  registerTizenKeys() {
    if (window.tizen && window.tizen.tvinputdevice) {
      const keys = [
        'MediaPlayPause', 'MediaPlay', 'MediaPause', 'MediaFastForward', 'MediaRewind', 'MediaStop',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
      ];
      keys.forEach(k => {
        try {
          window.tizen.tvinputdevice.registerKey(k);
        } catch (e) {}
      });
    }
  }

  /**
   * Synthesize a subtle, low-latency audio tick on D-pad navigation.
   * Completely self-contained via Web Audio API with zero asset dependencies.
   */
  initAudioTick() {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) {
        this.audioCtx = new AudioCtx();
      }
    } catch (e) {
      // Audio context may require first user gesture on some ROMs
    }
  }

  playTick() {
    if (!this.audioCtx) return;
    try {
      if (this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      const osc = this.audioCtx.createOscillator();
      const gain = this.audioCtx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, this.audioCtx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(440, this.audioCtx.currentTime + 0.025);
      gain.gain.setValueAtTime(0.04, this.audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.audioCtx.currentTime + 0.025);
      osc.connect(gain);
      gain.connect(this.audioCtx.destination);
      osc.start();
      osc.stop(this.audioCtx.currentTime + 0.025);
    } catch (e) {}
  }

  /**
   * OLED Screen Protection Watchdog:
   * Dims static UI after 2 minutes of remote inactivity while playing.
   */
  initOledWatchdog() {
    const resetIdle = () => {
      if (this.isIdle) {
        this.isIdle = false;
        document.body.classList.remove('oled-idle-dim');
      }
      clearTimeout(this.idleTimer);
      this.idleTimer = setTimeout(() => {
        const fsPlayer = document.getElementById('fullscreenPlayer');
        if (fsPlayer && fsPlayer.classList.contains('active')) {
          this.isIdle = true;
          document.body.classList.add('oled-idle-dim');
        }
      }, 120000); // 2 minutes
    };

    window.addEventListener('keydown', resetIdle);
    resetIdle();
  }

  refreshFocusables() {
    const all = Array.from(document.querySelectorAll('.focusable'));
    this.focusableElements = all.filter(el => {
      const style = window.getComputedStyle(el);
      const isVisible = style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
      const closestView = el.closest('.feed-view');
      const isViewActive = !closestView || closestView.classList.contains('active');
      const closestModal = el.closest('.tv-modal');
      const isModalActive = !closestModal || closestModal.classList.contains('open');
      const closestDrawer = el.closest('.desktop-queue-panel');
      const isDrawerActive = !closestDrawer || closestDrawer.classList.contains('open');

      return isVisible && isViewActive && isModalActive && isDrawerActive;
    });
  }

  setFocus(element, playSound = true) {
    if (!element) return;
    if (this.currentFocus) {
      this.currentFocus.classList.remove('focused');
    }
    this.currentFocus = element;
    this.currentFocus.classList.add('focused');
    this.currentFocus.focus();

    // Remember focus for current active view
    const currentView = element.closest('.feed-view');
    if (currentView && currentView.id) {
      this.viewFocusMemory.set(currentView.id, element);
    }

    if (playSound) this.playTick();
    this.ensureVisible(element);
  }

  ensureVisible(element) {
    const shelf = element.closest('.shelf-track');
    if (shelf) {
      const shelfRect = shelf.getBoundingClientRect();
      const elemRect = element.getBoundingClientRect();
      const offset = elemRect.left - shelfRect.left - (shelfRect.width / 2) + (elemRect.width / 2);
      shelf.scrollBy({ left: offset, behavior: 'smooth' });
    }

    const view = element.closest('.feed-view') || element.closest('.desktop-feed');
    if (view && !shelf) {
      element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }

  /**
   * Restore the last focused element in a given view
   */
  restoreViewFocus(viewId) {
    this.refreshFocusables();
    const remembered = this.viewFocusMemory.get(viewId);
    if (remembered && this.focusableElements.includes(remembered)) {
      this.setFocus(remembered, false);
      return;
    }
    const viewEl = document.getElementById(viewId);
    if (viewEl) {
      const first = viewEl.querySelector('.focusable');
      if (first) this.setFocus(first, false);
    }
  }

  move(direction) {
    this.refreshFocusables();
    if (!this.currentFocus) {
      if (this.focusableElements.length > 0) {
        this.setFocus(this.focusableElements[0]);
      }
      return;
    }

    const currentRect = this.currentFocus.getBoundingClientRect();
    let bestTarget = null;
    let minDistance = Infinity;

    for (const candidate of this.focusableElements) {
      if (candidate === this.currentFocus) continue;
      const targetRect = candidate.getBoundingClientRect();

      let isCandidate = false;
      let primaryDiff = 0;
      let secondaryDiff = 0;

      switch (direction) {
        case 'right':
          if (targetRect.left >= currentRect.right - 8) {
            isCandidate = true;
            primaryDiff = targetRect.left - currentRect.right;
            secondaryDiff = Math.abs((targetRect.top + targetRect.height / 2) - (currentRect.top + currentRect.height / 2));
          }
          break;
        case 'left':
          if (targetRect.right <= currentRect.left + 8) {
            isCandidate = true;
            primaryDiff = currentRect.left - targetRect.right;
            secondaryDiff = Math.abs((targetRect.top + targetRect.height / 2) - (currentRect.top + currentRect.height / 2));
          }
          break;
        case 'down':
          if (targetRect.top >= currentRect.bottom - 8) {
            isCandidate = true;
            primaryDiff = targetRect.top - currentRect.bottom;
            secondaryDiff = Math.abs((targetRect.left + targetRect.width / 2) - (currentRect.left + currentRect.width / 2));
          }
          break;
        case 'up':
          if (targetRect.bottom <= currentRect.top + 8) {
            isCandidate = true;
            primaryDiff = currentRect.top - targetRect.bottom;
            secondaryDiff = Math.abs((targetRect.left + targetRect.width / 2) - (currentRect.left + currentRect.width / 2));
          }
          break;
      }

      if (isCandidate) {
        // Geometric distance weight favoring direct axis alignment
        const distance = Math.hypot(primaryDiff, secondaryDiff * 1.6);
        if (distance < minDistance) {
          minDistance = distance;
          bestTarget = candidate;
        }
      }
    }

    // Boundary Catchers (Spotify TV UX Rules):
    // 1. Moving Left at the leftmost card of content -> Focus the active sidebar navigation button
    if (!bestTarget && direction === 'left') {
      const activeNav = document.querySelector('.desktop-sidebar .nav-btn.active') ||
                        document.querySelector('.desktop-sidebar .focusable');
      if (activeNav && activeNav !== this.currentFocus) {
        bestTarget = activeNav;
      }
    }

    // 2. Moving Down from the bottom shelf -> Focus the bottom Now Playing bar
    if (!bestTarget && direction === 'down') {
      const playerPlayBtn = document.getElementById('btn-play-pause') ||
                            document.querySelector('.desktop-player-bar .focusable');
      if (playerPlayBtn && playerPlayBtn !== this.currentFocus) {
        bestTarget = playerPlayBtn;
      }
    }

    // 3. Moving Up from the bottom Now Playing bar -> Restore content focus
    if (!bestTarget && direction === 'up' && this.currentFocus.closest('.desktop-player-bar')) {
      const activeView = document.querySelector('.feed-view.active');
      if (activeView) {
        const remembered = this.viewFocusMemory.get(activeView.id) || activeView.querySelector('.focusable');
        if (remembered) bestTarget = remembered;
      }
    }

    if (bestTarget) {
      this.setFocus(bestTarget);
    }
  }

  bindEvents() {
    window.addEventListener('keydown', (e) => {
      const code = e.keyCode || e.which;

      switch (code) {
        case 38: // ArrowUp
        case 19: // Android DPAD_UP
          e.preventDefault();
          this.move('up');
          break;

        case 40: // ArrowDown
        case 20: // Android DPAD_DOWN
          e.preventDefault();
          this.move('down');
          break;

        case 37: // ArrowLeft
        case 21: // Android DPAD_LEFT
          e.preventDefault();
          this.move('left');
          break;

        case 39: // ArrowRight
        case 22: // Android DPAD_RIGHT
          e.preventDefault();
          this.move('right');
          break;

        case 13: // Enter
        case 66: // Android DPAD_CENTER
          if (this.currentFocus) {
            e.preventDefault();
            this.currentFocus.click();
          }
          break;

        case 27:    // Escape
        case 10009: // Samsung Tizen Return / Back
        case 461:   // LG webOS Back
        case 4:     // Android TV Back
          e.preventDefault();
          if (window.AuraApp && typeof window.AuraApp.handleBack === 'function') {
            window.AuraApp.handleBack();
          }
          break;

        case 8:     // Backspace
          if (window.AuraApp && window.AuraApp.currentView === 'search' && window.AuraApp.searchQuery && window.AuraApp.searchQuery.length > 0) {
            e.preventDefault();
            window.AuraApp.searchQuery = window.AuraApp.searchQuery.slice(0, -1);
            const input = document.getElementById('searchInput');
            if (input) input.value = window.AuraApp.searchQuery;
            window.AuraApp.runSearch(window.AuraApp.searchQuery);
            return;
          }
          e.preventDefault();
          if (window.AuraApp && typeof window.AuraApp.handleBack === 'function') {
            window.AuraApp.handleBack();
          }
          break;

        // Dedicated TV Remote Media Keys (Samsung, LG, Android TV):
        case 32:    // Spacebar
          if (this.currentFocus && this.currentFocus.classList.contains('kbd-key')) {
            // Let space click the keyboard key
            break;
          }
        case 179:   // MediaPlayPause
        case 10252: // Tizen MediaPlayPause
        case 85:    // Android KEYCODE_MEDIA_PLAY_PAUSE
          e.preventDefault();
          if (window.AuraPlayer) window.AuraPlayer.togglePlay();
          break;

        case 415:   // Tizen / webOS MediaPlay
        case 126:   // Android KEYCODE_MEDIA_PLAY
          e.preventDefault();
          if (window.AuraPlayer && !window.AuraPlayer.isPlaying) window.AuraPlayer.togglePlay();
          break;

        case 19:    // MediaPause (when standalone)
        case 127:   // Android KEYCODE_MEDIA_PAUSE
          if (e.target.tagName !== 'INPUT') {
            e.preventDefault();
            if (window.AuraPlayer && window.AuraPlayer.isPlaying) window.AuraPlayer.togglePlay();
          }
          break;

        case 176: // MediaTrackNext
        case 417: // Tizen / webOS FastForward
        case 87:  // Android KEYCODE_MEDIA_NEXT
          e.preventDefault();
          if (window.AuraPlayer) window.AuraPlayer.nextTrack();
          break;

        case 177: // MediaTrackPrevious
        case 412: // Tizen / webOS Rewind
        case 88:  // Android KEYCODE_MEDIA_PREVIOUS
          e.preventDefault();
          if (window.AuraPlayer) window.AuraPlayer.prevTrack();
          break;
      }

      // Physical alphanumeric typing on search view
      if (window.AuraApp && window.AuraApp.currentView === 'search' && !e.ctrlKey && !e.altKey && !e.metaKey) {
        if (e.key && e.key.length === 1 && /[0-9a-zA-Z ]/.test(e.key)) {
          if (!this.currentFocus || !this.currentFocus.classList.contains('kbd-key')) {
            window.AuraApp.searchQuery += e.key;
            const input = document.getElementById('searchInput');
            if (input) input.value = window.AuraApp.searchQuery;
            window.AuraApp.runSearch(window.AuraApp.searchQuery);
          }
        }
      }
    });
  }
}

window.SpatialNav = new SpatialNavigation();
