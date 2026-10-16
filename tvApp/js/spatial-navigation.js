/**
 * AuraMusic TV Spatial Navigation Engine (D-Pad Controller)
 * Handles remote control input (Up, Down, Left, Right, Enter, Back)
 * Compatible with Samsung Tizen, LG webOS, Android TV, and standard Desktop Keyboards.
 */

class SpatialNavigation {
  constructor() {
    this.currentFocus = null;
    this.focusableElements = [];
    this.init();
  }

  init() {
    this.bindEvents();
    window.addEventListener('load', () => {
      this.refreshFocusables();
      // Set initial focus to Home Nav item or Hero Play
      const initial = document.getElementById('btn-hero-play') || document.querySelector('.focusable');
      if (initial) this.setFocus(initial);
    });
  }

  refreshFocusables() {
    // Find all visible .focusable elements in the document
    const all = Array.from(document.querySelectorAll('.focusable'));
    this.focusableElements = all.filter(el => {
      // Must be visible and not inside an inactive view or hidden container
      const style = window.getComputedStyle(el);
      const isVisible = style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0';
      const closestView = el.closest('.tv-view');
      const isViewActive = !closestView || closestView.classList.contains('active');
      const closestModal = el.closest('.tv-modal');
      const isModalActive = !closestModal || closestModal.classList.contains('open');
      const closestDrawer = el.closest('.tv-drawer');
      const isDrawerActive = !closestDrawer || closestDrawer.classList.contains('open');

      return isVisible && isViewActive && isModalActive && isDrawerActive;
    });
  }

  setFocus(element) {
    if (!element) return;
    if (this.currentFocus) {
      this.currentFocus.classList.remove('focused');
    }
    this.currentFocus = element;
    this.currentFocus.classList.add('focused');
    this.currentFocus.focus();

    // Smoothly ensure element is visible in view/shelf
    this.ensureVisible(element);
  }

  ensureVisible(element) {
    // If inside a horizontal shelf, scroll horizontally
    const shelf = element.closest('.shelf-track');
    if (shelf) {
      const shelfRect = shelf.getBoundingClientRect();
      const elemRect = element.getBoundingClientRect();
      const offset = elemRect.left - shelfRect.left - (shelfRect.width / 2) + (elemRect.width / 2);
      shelf.scrollBy({ left: offset, behavior: 'smooth' });
    }

    // If inside a view, ensure vertical scroll
    const view = element.closest('.tv-view');
    if (view && !shelf) {
      element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
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
        const distance = Math.hypot(primaryDiff, secondaryDiff * 1.5);
        if (distance < minDistance) {
          minDistance = distance;
          bestTarget = candidate;
        }
      }
    }

    if (bestTarget) {
      this.setFocus(bestTarget);
    }
  }

  bindEvents() {
    window.addEventListener('keydown', (e) => {
      // Smart TV Key Codes:
      // Tizen: 38 (Up), 40 (Down), 37 (Left), 39 (Right), 13 (Enter), 10009 (Return)
      // webOS: 38, 40, 37, 39, 13, 461 (Back)
      // Android TV: 19 (Up), 20 (Down), 21 (Left), 22 (Right), 66 (Enter), 4 (Back)
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

        case 27: // Escape
        case 8:  // Backspace
        case 10009: // Tizen Return
        case 461:   // webOS Back
        case 4:     // Android TV Back
          e.preventDefault();
          if (window.AuraApp && typeof window.AuraApp.handleBack === 'function') {
            window.AuraApp.handleBack();
          }
          break;

        case 32: // Spacebar -> Play / Pause shortcut
          e.preventDefault();
          if (window.AuraPlayer) {
            window.AuraPlayer.togglePlay();
          }
          break;
      }
    });
  }
}

window.SpatialNav = new SpatialNavigation();
