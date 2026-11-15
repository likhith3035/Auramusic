/**
 * AuraMusic Real Music & Synced Lyrics Engine for Smart TV (Samsung Tizen & LG webOS)
 * Connects to live music catalog and real-time karaoke lyrics.
 */

class AuraMusicApi {
  constructor() {
    this.lyricsCache = new Map();
    this.videoIdCache = new Map([
      ['blinding lights__the weeknd', '4NRXx6U8ABQ'],
      ['yellow__coldplay', 'yKNxeF4KMsY'],
      ['birds of a feather__billie eilish', 'd5gf9dXHevw'],
      ['die for you__the weeknd', 'uPD0QOGChkY'],
      ['viva la vida__coldplay', 'dvgZkm1xWPE'],
      ['starboy__the weeknd', 'dMMUH_ZpbB0'],
      ['espresso__sabrina carpenter', 'eVli-tstM5E']
    ]);
  }

  /**
   * Resolve a track's title & artist to a playable YouTube video ID
   */
  async getYoutubeVideoId(title, artist) {
    const key = `${(title || '').toLowerCase().trim()}__${(artist || '').toLowerCase().trim()}`;
    for (const [k, v] of this.videoIdCache.entries()) {
      if (key.includes(k) || k.includes(key)) return v;
    }

    try {
      const q = encodeURIComponent(`${title} ${artist} audio`);
      const res = await fetch(`https://invidious.f5.si/api/v1/search?q=${q}&type=video`);
      if (res.ok) {
        const items = await res.json();
        if (items && items.length > 0 && items[0].videoId) {
          this.videoIdCache.set(key, items[0].videoId);
          return items[0].videoId;
        }
      }
    } catch (e) {
      console.log('Video ID lookup error, using fallback');
    }
    return null;
  }

  /**
   * Search millions of real tracks with album art and audio streams
   */
  async search(query, limit = 12) {
    if (!query || !query.trim()) return [];
    try {
      const url = `https://itunes.apple.com/search?term=${encodeURIComponent(query)}&media=music&limit=${limit}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error('Search network error');
      const data = await res.json();
      return (data.results || []).map(item => this.normalizeTrack(item));
    } catch (err) {
      console.warn('AuraMusicApi search fallback:', err);
      return [];
    }
  }

  /**
   * Fetch real popular songs to populate the TV Home feed shelves
   */
  async getFeedShelves() {
    try {
      const [quickPicksRaw, listenAgainRaw, forgottenRaw] = await Promise.all([
        this.search('top hits 2026', 8),
        this.search('The Weeknd Billie Eilish Coldplay', 5),
        this.search('acoustic chill lo-fi', 5)
      ]);

      return {
        quickPicks: quickPicksRaw.length > 0 ? quickPicksRaw : null,
        listenAgain: listenAgainRaw.length > 0 ? listenAgainRaw : null,
        forgotten: forgottenRaw.length > 0 ? forgottenRaw : null
      };
    } catch (err) {
      console.warn('AuraMusicApi getFeedShelves error:', err);
      return { quickPicks: null, listenAgain: null, forgotten: null };
    }
  }

  /**
   * Normalize an API track into AuraMusic format
   */
  normalizeTrack(item) {
    const art = (item.artworkUrl100 || '')
      .replace('100x100bb', '600x600bb')
      .replace('100x100', '600x600');

    return {
      id: `itunes-${item.trackId || Math.random().toString(36).substr(2, 9)}`,
      title: item.trackName || 'Unknown Title',
      artist: item.artistName || 'Unknown Artist',
      album: item.collectionName || 'Single',
      art: art || 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600',
      audioUrl: item.previewUrl || '',
      duration: Math.floor((item.trackTimeMillis || 180000) / 1000)
    };
  }

  /**
   * Fetch real line-by-line synced lyrics from LRCLIB
   */
  async fetchSyncedLyrics(title, artist) {
    const cacheKey = `${title}__${artist}`.toLowerCase();
    if (this.lyricsCache.has(cacheKey)) {
      return this.lyricsCache.get(cacheKey);
    }

    try {
      // Clean title for higher match rate (remove features, clean parentheticals)
      const cleanTitle = title.replace(/\(.*?\)|\[.*?\]/g, '').trim();
      const cleanArtist = artist.split(/[,&]/)[0].trim();

      const url = `https://lrclib.net/api/get?track_name=${encodeURIComponent(cleanTitle)}&artist_name=${encodeURIComponent(cleanArtist)}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error('Lyrics not found');
      const data = await res.json();

      let parsed = [];
      if (data.syncedLyrics) {
        parsed = this.parseLrc(data.syncedLyrics);
      } else if (data.plainLyrics) {
        parsed = data.plainLyrics
          .split('\n')
          .filter(l => l.trim().length > 0)
          .map((text, i) => ({ time: i * 5, text }));
      }

      if (parsed.length > 0) {
        this.lyricsCache.set(cacheKey, parsed);
        return parsed;
      }
    } catch (e) {
      console.log('No synced lyrics found for track, using fallback.');
    }

    const fallback = [
      { time: 0, text: `♪ Now Playing: ${title}` },
      { time: 5, text: `Artist: ${artist}` },
      { time: 10, text: "High-Fidelity Audio • Pure Acoustics" },
      { time: 20, text: "Enjoy the rhythm and melody..." }
    ];
    this.lyricsCache.set(cacheKey, fallback);
    return fallback;
  }

  /**
   * Parse standard .LRC format [mm:ss.xx] into seconds & text
   */
  parseLrc(lrcText) {
    const lines = lrcText.split('\n');
    const result = [];
    const timeReg = /\[(\d{2}):(\d{2}(?:\.\d+)?)\]/g;

    for (const line of lines) {
      const match = timeReg.exec(line);
      if (match) {
        const min = parseInt(match[1], 10);
        const sec = parseFloat(match[2]);
        const time = min * 60 + sec;
        const text = line.replace(/\[\d{2}:\d{2}(?:\.\d+)?\]/g, '').trim();
        if (text) {
          result.push({ time, text });
        }
      }
    }
    return result.sort((a, b) => a.time - b.time);
  }
}

window.AuraMusicApi = new AuraMusicApi();
