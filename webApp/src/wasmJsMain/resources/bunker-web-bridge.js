/**
 * BunkerStock Web/iOS Safari Bridge:
 * - Native window.BarcodeDetector FIRST, seamless zxing-wasm (barcode-detector@3 ponyfill) fallback for iOS Safari
 * - 2-frame consensus debouncing matching Android's BarcodeAnalyzer.kt
 * - iOS-tuned getUserMedia (playsinline, rear camera, optional 2x zoom for iPhone Pro macro focus, torch)
 * - Web Crypto AES-256-GCM + SHA-256 + CompressionStream("gzip") wire-compatible with Android FamilySyncClient
 * - Offline QR PNG/Print/Batch A4 Print Sheet + Blob Export/Import + Web Audio beep
 */
(function (global) {
  'use strict';

  const FORMATS = ['qr_code', 'data_matrix', 'ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128'];
  let detectorPromise = null;

  async function getDetector() {
    if (!detectorPromise) {
      detectorPromise = (async () => {
        if ('BarcodeDetector' in global) {
          try {
            const supported = await global.BarcodeDetector.getSupportedFormats();
            if (supported && supported.includes('qr_code') && supported.includes('ean_13')) {
              return new global.BarcodeDetector({ formats: FORMATS.filter((f) => supported.includes(f)) });
            }
          } catch (_) {}
        }
        // Fallback for iOS Safari / Firefox using C++/WASM ZXing ponyfill
        const mod = await import('https://cdn.jsdelivr.net/npm/barcode-detector@3/ponyfill/+esm');
        return new mod.BarcodeDetector({ formats: FORMATS });
      })();
    }
    return detectorPromise;
  }

  function b64ToBytes(b64) {
    const bin = atob(b64);
    const out = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
    return out;
  }

  function bytesToB64(bytes) {
    let bin = '';
    const chunk = 0x8000;
    for (let i = 0; i < bytes.length; i += chunk) {
      bin += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
    }
    return btoa(bin);
  }

  async function gzipBytes(bytes) {
    const cs = new CompressionStream('gzip');
    const writer = cs.writable.getWriter();
    writer.write(bytes);
    writer.close();
    const buf = await new Response(cs.readable).arrayBuffer();
    return new Uint8Array(buf);
  }

  async function gunzipBytes(bytes) {
    const ds = new DecompressionStream('gzip');
    const writer = ds.writable.getWriter();
    writer.write(bytes);
    writer.close();
    const buf = await new Response(ds.readable).arrayBuffer();
    return new Uint8Array(buf);
  }

  async function deriveAesKey(secretKey, usages) {
    const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(secretKey));
    return crypto.subtle.importKey('raw', hash, { name: 'AES-GCM' }, false, usages);
  }

  // Camera state
  let cameraSessionId = 0;
  let activeStream = null;
  let activeTrack = null;
  let scanTimer = null;
  let scanBusy = false;
  let onCodeCb = null;
  let lastCandidate = null;
  let candidateCount = 0;
  let lastCandidateAt = 0;
  let lastEmitted = null;
  let lastEmittedAt = 0;
  let currentZoom = 1.0;

  function isPlausibleCode(text) {
    if (!text || text.length < 3 || text.length > 1024) return false;
    for (let i = 0; i < text.length; i++) {
      const c = text.charCodeAt(i);
      if (c < 32 && c !== 10 && c !== 13 && c !== 9) return false;
    }
    return true;
  }

  function stopStreamTracks(stream) {
    if (!stream) return;
    stream.getTracks().forEach((t) => {
      try { t.stop(); } catch (_) {}
    });
  }

  function stopCamera() {
    cameraSessionId++;
    onCodeCb = null;
    if (scanTimer) {
      clearInterval(scanTimer);
      scanTimer = null;
    }
    scanBusy = false;
    if (activeStream) {
      stopStreamTracks(activeStream);
      activeStream = null;
      activeTrack = null;
    }
    const video = document.getElementById('bunker-cam');
    if (video) {
      try { video.pause(); } catch (_) {}
      video.srcObject = null;
      video.style.display = 'none';
    }
  }

  // Always turn off camera LED / tracks when tab or PWA is hidden/closed
  global.addEventListener('pagehide', stopCamera);
  document.addEventListener('visibilitychange', () => {
    if (document.hidden && activeStream) stopCamera();
  });

  // PWA install prompt capture (Chrome / Edge / Android browsers)
  let deferredInstallPrompt = null;
  let pwaInstalledByEvent = false;
  global.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredInstallPrompt = e;
  });
  global.addEventListener('appinstalled', () => {
    deferredInstallPrompt = null;
    pwaInstalledByEvent = true;
  });

  function setVideoBounds(leftPx, topPx, widthPx, heightPx) {
    const video = document.getElementById('bunker-cam');
    if (!video) return;
    const dpr = global.devicePixelRatio || 1;
    const root = document.getElementById('compose-root');
    const rootRect = root ? root.getBoundingClientRect() : { left: 0, top: 0 };
    video.style.left = (rootRect.left + leftPx / dpr) + 'px';
    video.style.top = (rootRect.top + topPx / dpr) + 'px';
    video.style.width = Math.max(1, widthPx / dpr) + 'px';
    video.style.height = Math.max(1, heightPx / dpr) + 'px';
  }

  async function startCamera(useBack, callback) {
    stopCamera();
    const mySession = ++cameraSessionId;
    onCodeCb = callback;
    const video = document.getElementById('bunker-cam');
    if (!video || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      return JSON.stringify({ ok: false, hasFlash: false, supportsZoom: false, error: 'unavailable' });
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {
          facingMode: { ideal: useBack ? 'environment' : 'user' },
          width: { ideal: 1280 },
          height: { ideal: 720 }
        }
      });
      // If user already left Scanner while permission/camera was opening, immediately release tracks!
      if (mySession !== cameraSessionId) {
        stopStreamTracks(stream);
        return JSON.stringify({ ok: false, cancelled: true, hasFlash: false, supportsZoom: false });
      }
      activeStream = stream;
      activeTrack = stream.getVideoTracks()[0] || null;
      video.srcObject = stream;
      video.setAttribute('playsinline', 'true');
      video.muted = true;
      video.style.display = 'block';
      await video.play();

      if (mySession !== cameraSessionId) {
        stopCamera();
        return JSON.stringify({ ok: false, cancelled: true, hasFlash: false, supportsZoom: false });
      }

      const caps = activeTrack && activeTrack.getCapabilities ? activeTrack.getCapabilities() : {};
      const hasFlash = Boolean(caps && caps.torch);
      const supportsZoom = Boolean(caps && caps.zoom && caps.zoom.max >= 1.8);
      currentZoom = 1.0;

      // Kick off detector load and scan loop
      getDetector().then((detector) => {
        if (mySession !== cameraSessionId || !activeStream) return;
        scanTimer = setInterval(async () => {
          if (mySession !== cameraSessionId || scanBusy || !activeStream || video.readyState < 2) return;
          scanBusy = true;
          try {
            const found = await detector.detect(video);
            if (found && found.length > 0) {
              const raw = (found[0].rawValue || '').trim();
              if (isPlausibleCode(raw)) {
                const now = Date.now();
                if (raw === lastCandidate && now - lastCandidateAt <= 1500) {
                  candidateCount++;
                } else {
                  lastCandidate = raw;
                  candidateCount = 1;
                }
                lastCandidateAt = now;

                if (candidateCount >= 2) {
                  const sameActive = raw === lastEmitted && now - lastEmittedAt < 3500;
                  if (!sameActive) {
                    lastEmitted = raw;
                    lastEmittedAt = now;
                    if (onCodeCb) onCodeCb(raw);
                  }
                  candidateCount = 0;
                }
              }
            }
          } catch (_) {
          } finally {
            scanBusy = false;
          }
        }, 120);
      }).catch(() => {});

      return JSON.stringify({ ok: true, hasFlash, supportsZoom });
    } catch (err) {
      stopCamera();
      const denied = err && (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError');
      return JSON.stringify({ ok: false, denied: Boolean(denied), hasFlash: false, supportsZoom: false });
    }
  }

  function resetScanDebounce() {
    lastEmitted = null;
    lastCandidate = null;
    candidateCount = 0;
  }

  async function setTorch(enabled) {
    if (!activeTrack) return false;
    try {
      await activeTrack.applyConstraints({ advanced: [{ torch: Boolean(enabled) }] });
      return true;
    } catch (_) {
      return false;
    }
  }

  async function setZoom(zoomed) {
    if (!activeTrack || !activeTrack.getCapabilities) return false;
    try {
      const caps = activeTrack.getCapabilities();
      if (!caps || !caps.zoom) return false;
      const target = zoomed ? Math.min(2.0, caps.zoom.max) : Math.max(1.0, caps.zoom.min || 1.0);
      await activeTrack.applyConstraints({ advanced: [{ zoom: target }] });
      currentZoom = target;
      return true;
    } catch (_) {
      return false;
    }
  }

  function pickFileInput(accept, onFile) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.style.display = 'none';
    document.body.appendChild(input);
    input.onchange = () => {
      const file = input.files && input.files[0];
      document.body.removeChild(input);
      onFile(file || null);
    };
    input.oncancel = () => {
      if (input.parentNode) document.body.removeChild(input);
      onFile(null);
    };
    input.click();
  }

  function pickAndDecodeImage() {
    return new Promise((resolve) => {
      pickFileInput('image/*', async (file) => {
        if (!file) { resolve(''); return; }
        try {
          const detector = await getDetector();
          const bmp = await createImageBitmap(file);
          const found = await detector.detect(bmp);
          bmp.close();
          const raw = found && found[0] && found[0].rawValue ? found[0].rawValue.trim() : '';
          resolve(isPlausibleCode(raw) ? raw : '');
        } catch (_) {
          resolve('');
        }
      });
    });
  }

  function escapeHtml(s) {
    return String(s || '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  global.BunkerWeb = {
    hideLoader: function () {
      const el = document.getElementById('bunker-loader');
      if (el) {
        el.classList.add('hidden');
        setTimeout(() => { if (el.parentNode) el.parentNode.removeChild(el); }, 300);
      }
    },

    getInitialHashInvite: function () {
      const h = location.hash || '';
      if (h.toLowerCase().includes('#join?')) {
        const full = location.href;
        try { history.replaceState(null, '', location.pathname + location.search); } catch (_) {}
        return full;
      }
      return '';
    },

    getWebBaseUrl: function () {
      return location.origin + location.pathname.replace(/\/index\.html$/i, '');
    },

    // PWA Install helpers
    isStandalonePwa: function () {
      try {
        return Boolean(
          pwaInstalledByEvent ||
          (global.matchMedia && global.matchMedia('(display-mode: standalone)').matches) ||
          global.navigator.standalone === true
        );
      } catch (_) {
        return false;
      }
    },

    isIosSafari: function () {
      try {
        const ua = global.navigator.userAgent || '';
        return /iPad|iPhone|iPod/.test(ua) || (global.navigator.platform === 'MacIntel' && global.navigator.maxTouchPoints > 1);
      } catch (_) {
        return false;
      }
    },

    canPromptInstall: function () {
      return Boolean(deferredInstallPrompt);
    },

    promptInstall: async function () {
      if (!deferredInstallPrompt) return 'false';
      try {
        deferredInstallPrompt.prompt();
        const choice = await deferredInstallPrompt.userChoice;
        deferredInstallPrompt = null;
        if (choice && choice.outcome === 'accepted') {
          pwaInstalledByEvent = true;
          return 'true';
        }
        return 'false';
      } catch (_) {
        return 'false';
      }
    },

    // HTTP
    fetchText: async function (url, timeoutMs) {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), timeoutMs || 8000);
      try {
        const res = await fetch(url, { method: 'GET', signal: ctrl.signal });
        if (!res.ok) return null;
        return await res.text();
      } catch (_) {
        return null;
      } finally {
        clearTimeout(timer);
      }
    },

    postText: async function (url, body, contentType, timeoutMs) {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), timeoutMs || 10000);
      try {
        const res = await fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': contentType || 'text/plain' },
          body: body,
          signal: ctrl.signal
        });
        return res.ok;
      } catch (_) {
        return false;
      } finally {
        clearTimeout(timer);
      }
    },

    fetchBase64: async function (url, timeoutMs) {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), timeoutMs || 5000);
      try {
        const res = await fetch(url, { method: 'GET', mode: 'cors', signal: ctrl.signal });
        if (!res.ok) return null;
        const buf = await res.arrayBuffer();
        return bytesToB64(new Uint8Array(buf));
      } catch (_) {
        return null;
      } finally {
        clearTimeout(timer);
      }
    },

    // Crypto (100% wire-compatible with Android FamilySyncClient)
    sha256Hex: async function (input) {
      const hash = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(input));
      return Array.from(new Uint8Array(hash)).map((b) => b.toString(16).padStart(2, '0')).join('');
    },

    randomBase64: function (count) {
      const arr = new Uint8Array(count);
      crypto.getRandomValues(arr);
      return bytesToB64(arr);
    },

    encryptGzipAesGcm: async function (plainBase64, secretKey) {
      const plain = b64ToBytes(plainBase64);
      const compressed = await gzipBytes(plain);
      const iv = new Uint8Array(12);
      crypto.getRandomValues(iv);
      const key = await deriveAesKey(secretKey, ['encrypt']);
      const enc = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv, tagLength: 128 }, key, compressed));
      const combined = new Uint8Array(iv.length + enc.length);
      combined.set(iv, 0);
      combined.set(enc, iv.length);
      return bytesToB64(combined);
    },

    decryptAesGcm: async function (combinedBase64, secretKey, gunzip) {
      const combined = b64ToBytes(combinedBase64);
      const iv = combined.subarray(0, 12);
      const enc = combined.subarray(12);
      const key = await deriveAesKey(secretKey, ['decrypt']);
      const plain = new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-GCM', iv, tagLength: 128 }, key, enc));
      const out = gunzip ? await gunzipBytes(plain) : plain;
      return bytesToB64(out);
    },

    // Storage
    storageGet: function (key) {
      try { return localStorage.getItem(key); } catch (_) { return null; }
    },
    storageSet: function (key, value) {
      try { localStorage.setItem(key, value); } catch (_) {}
    },
    storageRemove: function (key) {
      try { localStorage.removeItem(key); } catch (_) {}
    },

    // Camera
    startCamera,
    stopCamera,
    setVideoBounds,
    setTorch,
    setZoom,
    resetScanDebounce,
    pickAndDecodeImage,

    // Audio Beep
    playBeep: function () {
      try {
        const AudioCtx = global.AudioContext || global.webkitAudioContext;
        if (!AudioCtx) return;
        const ctx = new AudioCtx();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.value = 1280;
        gain.gain.value = 0.12;
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        setTimeout(() => {
          try { osc.stop(); ctx.close(); } catch (_) {}
        }, 95);
      } catch (_) {}
    },

    // QR helpers
    generateQrFlat: function (content) {
      try { return global.BunkerQr.encodeFlatString(content); } catch (_) { return ''; }
    },

    printSingleQr: function (header, title, meta, qrContent) {
      try {
        const canvas = global.BunkerQr.toCanvas(qrContent, 420, 2);
        const dataUrl = canvas.toDataURL('image/png');
        const win = window.open('', '_blank');
        if (!win) return;
        win.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title>
          <style>body{font-family:sans-serif;display:flex;justify-content:center;padding:24px}
          .card{border:3px solid #0f172a;padding:18px;text-align:center;width:300px}
          .hdr{font-family:monospace;font-weight:700;color:#475569;font-size:12px;letter-spacing:1.2px}
          .ttl{font-weight:800;font-size:16px;margin:10px 0 4px}
          .meta{font-family:monospace;font-size:11px;color:#334155}</style></head>
          <body><div class="card"><div class="hdr">${escapeHtml(header)}</div>
          <img src="${dataUrl}" width="220" height="220"><div class="ttl">${escapeHtml(title)}</div>
          <div class="meta">${escapeHtml(meta)}</div></div>
          <script>window.onload=()=>{window.print();}</script></body></html>`);
        win.document.close();
      } catch (_) {}
    },

    downloadSingleQrPng: function (header, title, meta, qrContent) {
      try {
        const qrCanvas = global.BunkerQr.toCanvas(qrContent, 420, 2);
        const c = document.createElement('canvas');
        c.width = 620;
        c.height = 700;
        const ctx = c.getContext('2d');
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(0, 0, c.width, c.height);
        ctx.strokeStyle = '#0F172A';
        ctx.lineWidth = 6;
        ctx.strokeRect(16, 16, c.width - 32, c.height - 32);
        ctx.fillStyle = '#475569';
        ctx.font = 'bold 20px monospace';
        ctx.textAlign = 'center';
        ctx.fillText(header, c.width / 2, 64);
        ctx.drawImage(qrCanvas, (c.width - 420) / 2, 92);
        ctx.fillStyle = '#090D16';
        ctx.font = 'bold 26px sans-serif';
        ctx.fillText(title.length > 34 ? title.slice(0, 33) + '…' : title, c.width / 2, 568);
        ctx.fillStyle = '#334155';
        ctx.font = '18px monospace';
        ctx.fillText(meta, c.width / 2, 620);

        const a = document.createElement('a');
        a.href = c.toDataURL('image/png');
        a.download = `qr_${qrContent.replace(/[^a-zA-Z0-9_-]/g, '_')}.png`;
        a.click();
        return true;
      } catch (_) {
        return false;
      }
    },

    printBatchQrA4: function (itemsJson, headerBrand) {
      try {
        const items = JSON.parse(itemsJson);
        const win = window.open('', '_blank');
        if (!win) return false;
        let pagesHtml = '';
        for (let p = 0; p < items.length; p += 24) {
          const slice = items.slice(p, p + 24);
          let cells = '';
          for (const it of slice) {
            const dataUrl = global.BunkerQr.toCanvas(it.code, 240, 1).toDataURL('image/png');
            cells += `<div class="cell"><div class="b">${escapeHtml(headerBrand)}</div>
              <img src="${dataUrl}"><div class="n">${escapeHtml(it.name)}</div>
              <div class="m">${escapeHtml(it.code)} · ${escapeHtml(it.exp)}</div></div>`;
          }
          pagesHtml += `<div class="page">${cells}</div>`;
        }
        win.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>BunkerStock Batch QR A4</title>
          <style>@page{size:A4;margin:8mm}body{margin:0;font-family:sans-serif}
          .page{display:grid;grid-template-columns:repeat(4,1fr);grid-template-rows:repeat(6,1fr);gap:3mm;height:281mm;page-break-after:always}
          .cell{border:1px dashed #64748b;padding:2mm;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;overflow:hidden}
          .cell img{width:28mm;height:28mm;image-rendering:pixelated}
          .b{font:700 6pt monospace;color:#475569}.n{font:700 7.5pt sans-serif;margin-top:1mm;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:100%}
          .m{font:6pt monospace;color:#334155}</style></head>
          <body>${pagesHtml}<script>window.onload=()=>{window.print();}</script></body></html>`);
        win.document.close();
        return true;
      } catch (_) {
        return false;
      }
    },

    // File export / import / photo picker / share
    downloadText: function (filename, content, mime) {
      try {
        const blob = new Blob([content], { type: mime || 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        return true;
      } catch (_) {
        return false;
      }
    },

    pickTextFile: function () {
      return new Promise((resolve) => {
        pickFileInput('.json,application/json,text/plain', (file) => {
          if (!file) { resolve(''); return; }
          const reader = new FileReader();
          reader.onload = () => resolve(String(reader.result || ''));
          reader.onerror = () => resolve('');
          reader.readAsText(file, 'utf-8');
        });
      });
    },

    pickPhotoDataUrl: function (maxDim) {
      return new Promise((resolve) => {
        pickFileInput('image/*', (file) => {
          if (!file) { resolve(''); return; }
          const url = URL.createObjectURL(file);
          const img = new Image();
          img.onload = () => {
            try {
              const limit = maxDim || 900;
              const ratio = Math.min(1, limit / Math.max(img.width, img.height));
              const w = Math.max(1, Math.round(img.width * ratio));
              const h = Math.max(1, Math.round(img.height * ratio));
              const c = document.createElement('canvas');
              c.width = w;
              c.height = h;
              c.getContext('2d').drawImage(img, 0, 0, w, h);
              resolve(c.toDataURL('image/jpeg', 0.85));
            } catch (_) {
              resolve('');
            } finally {
              URL.revokeObjectURL(url);
            }
          };
          img.onerror = () => { URL.revokeObjectURL(url); resolve(''); };
          img.src = url;
        });
      });
    },

    shareText: async function (title, text) {
      try {
        if (navigator.share) {
          await navigator.share({ title, text });
          return true;
        }
      } catch (_) {}
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text);
          return true;
        }
      } catch (_) {}
      return false;
    }
  };
})(window);
