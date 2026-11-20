/**
 * Compact ISO/IEC 18004 QR Code Byte-Mode (ECC Level M, Versions 1..12) encoder.
 * Zero external dependencies — works 100% offline in iOS Safari & desktop browsers.
 */
(function (global) {
  'use strict';

  // GF(256) tables with primitive polynomial 0x11d
  const EXP = new Uint8Array(512);
  const LOG = new Uint8Array(256);
  {
    let x = 1;
    for (let i = 0; i < 255; i++) {
      EXP[i] = x;
      LOG[x] = i;
      x <<= 1;
      if (x & 0x100) x ^= 0x11d;
    }
    for (let i = 255; i < 512; i++) EXP[i] = EXP[i - 255];
  }

  function gfMul(a, b) {
    return a === 0 || b === 0 ? 0 : EXP[LOG[a] + LOG[b]];
  }

  function rsGeneratorPoly(ecLen) {
    let poly = [1];
    for (let i = 0; i < ecLen; i++) {
      const next = new Array(poly.length + 1).fill(0);
      for (let j = 0; j < poly.length; j++) {
        next[j] ^= poly[j];
        next[j + 1] ^= gfMul(poly[j], EXP[i]);
      }
      poly = next;
    }
    return poly;
  }

  function rsRemainder(data, ecLen) {
    const gen = rsGeneratorPoly(ecLen);
    const rem = new Uint8Array(ecLen);
    for (let i = 0; i < data.length; i++) {
      const factor = data[i] ^ rem[0];
      for (let j = 0; j < ecLen - 1; j++) rem[j] = rem[j + 1];
      rem[ecLen - 1] = 0;
      if (factor !== 0) {
        for (let j = 0; j < ecLen; j++) rem[j] ^= gfMul(gen[j + 1], factor);
      }
    }
    return rem;
  }

  // Version tables for ECC Level M: [totalDataCodewords, ecPerBlock, [[numBlocks, dataPerBlock], ...]]
  const VERSION_M = [
    null,
    [16, 10, [[1, 16]]],
    [28, 16, [[1, 28]]],
    [44, 26, [[1, 44]]],
    [64, 18, [[2, 32]]],
    [86, 24, [[2, 43]]],
    [108, 16, [[4, 27]]],
    [124, 18, [[4, 31]]],
    [154, 22, [[2, 38], [2, 39]]],
    [182, 22, [[3, 36], [2, 37]]],
    [216, 26, [[4, 43], [1, 44]]],
    [254, 30, [[1, 50], [4, 51]]],
    [290, 22, [[6, 36], [2, 37]]]
  ];

  const ALIGN_POS = [
    null, [], [6, 18], [6, 22], [6, 26], [6, 30], [6, 34],
    [6, 22, 38], [6, 24, 42], [6, 26, 46], [6, 28, 50], [6, 30, 54], [6, 32, 58]
  ];

  function chooseVersion(byteLen) {
    for (let v = 1; v < VERSION_M.length; v++) {
      const totalDataBits = VERSION_M[v][0] * 8;
      const countBits = v <= 9 ? 8 : 16;
      if (4 + countBits + byteLen * 8 <= totalDataBits) return v;
    }
    return VERSION_M.length - 1;
  }

  function encodeData(bytes, version) {
    const [totalDataCw, ecPerBlock, groups] = VERSION_M[version];
    const bits = [];
    const pushBits = (val, len) => {
      for (let i = len - 1; i >= 0; i--) bits.push((val >>> i) & 1);
    };

    const countBits = version <= 9 ? 8 : 16;
    const maxBytes = Math.min(bytes.length, Math.floor((totalDataCw * 8 - 4 - countBits) / 8));
    pushBits(0b0100, 4); // Byte mode
    pushBits(maxBytes, countBits);
    for (let i = 0; i < maxBytes; i++) pushBits(bytes[i], 8);

    // Terminator + pad to byte
    const totalBits = totalDataCw * 8;
    const term = Math.min(4, totalBits - bits.length);
    for (let i = 0; i < term; i++) bits.push(0);
    while (bits.length % 8 !== 0) bits.push(0);

    const dataCw = new Uint8Array(totalDataCw);
    for (let i = 0; i < bits.length / 8; i++) {
      let b = 0;
      for (let j = 0; j < 8; j++) b = (b << 1) | bits[i * 8 + j];
      dataCw[i] = b;
    }
    const padBytes = [0xec, 0x11];
    let p = 0;
    for (let i = bits.length / 8; i < totalDataCw; i++) {
      dataCw[i] = padBytes[p++ & 1];
    }

    // Split into blocks & compute RS ECC
    const dataBlocks = [];
    const ecBlocks = [];
    let offset = 0;
    for (const [count, len] of groups) {
      for (let i = 0; i < count; i++) {
        const blk = dataCw.slice(offset, offset + len);
        offset += len;
        dataBlocks.push(blk);
        ecBlocks.push(rsRemainder(blk, ecPerBlock));
      }
    }

    // Interleave data then ECC
    const out = [];
    const maxDataLen = Math.max(...dataBlocks.map((b) => b.length));
    for (let i = 0; i < maxDataLen; i++) {
      for (const b of dataBlocks) if (i < b.length) out.push(b[i]);
    }
    for (let i = 0; i < ecPerBlock; i++) {
      for (const e of ecBlocks) out.push(e[i]);
    }
    return new Uint8Array(out);
  }

  function formatBitsM(mask) {
    // ECC Level M indicator = 00
    const data = (0b00 << 3) | (mask & 7);
    let rem = data << 10;
    for (let i = 4; i >= 0; i--) {
      if ((rem >>> (i + 10)) & 1) rem ^= 0x537 << i;
    }
    return ((data << 10) | rem) ^ 0x5412;
  }

  function versionBits(v) {
    let rem = v << 12;
    for (let i = 5; i >= 0; i--) {
      if ((rem >>> (i + 12)) & 1) rem ^= 0x1f25 << i;
    }
    return (v << 12) | rem;
  }

  function buildMatrix(codewords, version) {
    const size = version * 4 + 17;
    const mods = Array.from({ length: size }, () => new Uint8Array(size));
    const isFunc = Array.from({ length: size }, () => new Uint8Array(size));

    const setFunc = (r, c, dark) => {
      if (r >= 0 && r < size && c >= 0 && c < size) {
        mods[r][c] = dark ? 1 : 0;
        isFunc[r][c] = 1;
      }
    };

    // Finder patterns + separators
    const drawFinder = (r0, c0) => {
      for (let dr = -1; dr <= 7; dr++) {
        for (let dc = -1; dc <= 7; dc++) {
          const r = r0 + dr, c = c0 + dc;
          if (r < 0 || r >= size || c < 0 || c >= size) continue;
          const dist = Math.max(Math.abs(dr - 3), Math.abs(dc - 3));
          setFunc(r, c, dist !== 2 && dist !== 4);
        }
      }
    };
    drawFinder(0, 0);
    drawFinder(0, size - 7);
    drawFinder(size - 7, 0);

    // Timing patterns
    for (let i = 8; i < size - 8; i++) {
      setFunc(6, i, i % 2 === 0);
      setFunc(i, 6, i % 2 === 0);
    }

    // Alignment patterns
    const pos = ALIGN_POS[version];
    for (let i = 0; i < pos.length; i++) {
      for (let j = 0; j < pos.length; j++) {
        if ((i === 0 && j === 0) || (i === 0 && j === pos.length - 1) || (i === pos.length - 1 && j === 0)) continue;
        const r0 = pos[i], c0 = pos[j];
        for (let dr = -2; dr <= 2; dr++) {
          for (let dc = -2; dc <= 2; dc++) {
            const dist = Math.max(Math.abs(dr), Math.abs(dc));
            setFunc(r0 + dr, c0 + dc, dist !== 1);
          }
        }
      }
    }

    // Reserve format areas + dark module
    for (let i = 0; i < 9; i++) {
      if (i !== 6) {
        setFunc(8, i, false);
        setFunc(i, 8, false);
      }
    }
    for (let i = 0; i < 8; i++) {
      setFunc(8, size - 1 - i, false);
      setFunc(size - 1 - i, 8, false);
    }
    setFunc(size - 8, 8, true);

    // Version info for v >= 7
    if (version >= 7) {
      const vb = versionBits(version);
      for (let i = 0; i < 18; i++) {
        const bit = ((vb >>> i) & 1) !== 0;
        const a = Math.floor(i / 3);
        const b = size - 11 + (i % 3);
        setFunc(a, b, bit);
        setFunc(b, a, bit);
      }
    }

    // Zigzag placement
    let bitIdx = 0;
    const totalBits = codewords.length * 8;
    for (let right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right = 5;
      const upward = ((right + 1) & 2) === 0;
      for (let vert = 0; vert < size; vert++) {
        const r = upward ? size - 1 - vert : vert;
        for (let j = 0; j < 2; j++) {
          const c = right - j;
          if (!isFunc[r][c]) {
            let dark = false;
            if (bitIdx < totalBits) {
              dark = ((codewords[bitIdx >>> 3] >>> (7 - (bitIdx & 7))) & 1) !== 0;
              bitIdx++;
            }
            mods[r][c] = dark ? 1 : 0;
          }
        }
      }
    }

    // Pick lowest-penalty mask (0..7)
    const maskCond = [
      (r, c) => (r + c) % 2 === 0,
      (r, c) => r % 2 === 0,
      (r, c) => c % 3 === 0,
      (r, c) => (r + c) % 3 === 0,
      (r, c) => (Math.floor(r / 2) + Math.floor(c / 3)) % 2 === 0,
      (r, c) => ((r * c) % 2) + ((r * c) % 3) === 0,
      (r, c) => (((r * c) % 2) + ((r * c) % 3)) % 2 === 0,
      (r, c) => (((r + c) % 2) + ((r * c) % 3)) % 2 === 0
    ];

    const writeFormat = (grid, mask) => {
      const fb = formatBitsM(mask);
      for (let i = 0; i <= 5; i++) grid[8][i] = (fb >>> i) & 1;
      grid[8][7] = (fb >>> 6) & 1;
      grid[8][8] = (fb >>> 7) & 1;
      grid[7][8] = (fb >>> 8) & 1;
      for (let i = 9; i < 15; i++) grid[14 - i][8] = (fb >>> i) & 1;

      for (let i = 0; i < 8; i++) grid[8][size - 1 - i] = (fb >>> (14 - i)) & 1;
      for (let i = 8; i < 15; i++) grid[size - 15 + i][8] = (fb >>> (14 - i)) & 1;
    };

    let bestMask = 0;
    let bestScore = Infinity;
    let bestGrid = null;

    for (let m = 0; m < 8; m++) {
      const grid = mods.map((row) => row.slice());
      const fn = maskCond[m];
      for (let r = 0; r < size; r++) {
        for (let c = 0; c < size; c++) {
          if (!isFunc[r][c] && fn(r, c)) grid[r][c] ^= 1;
        }
      }
      writeFormat(grid, m);

      // Simplified penalty: runs + 2x2 blocks + balance
      let penalty = 0;
      let darkCount = 0;
      for (let r = 0; r < size; r++) {
        let runColor = grid[r][0], runLen = 1;
        darkCount += grid[r][0];
        for (let c = 1; c < size; c++) {
          darkCount += grid[r][c];
          if (grid[r][c] === runColor) {
            runLen++;
            if (runLen === 5) penalty += 3;
            else if (runLen > 5) penalty += 1;
          } else {
            runColor = grid[r][c];
            runLen = 1;
          }
        }
      }
      for (let c = 0; c < size; c++) {
        let runColor = grid[0][c], runLen = 1;
        for (let r = 1; r < size; r++) {
          if (grid[r][c] === runColor) {
            runLen++;
            if (runLen === 5) penalty += 3;
            else if (runLen > 5) penalty += 1;
          } else {
            runColor = grid[r][c];
            runLen = 1;
          }
        }
      }
      for (let r = 0; r < size - 1; r++) {
        for (let c = 0; c < size - 1; c++) {
          const v = grid[r][c];
          if (v === grid[r][c + 1] && v === grid[r + 1][c] && v === grid[r + 1][c + 1]) penalty += 3;
        }
      }
      const pct = Math.abs((darkCount * 100) / (size * size) - 50);
      penalty += Math.floor(pct / 5) * 10;

      if (penalty < bestScore) {
        bestScore = penalty;
        bestMask = m;
        bestGrid = grid;
      }
    }

    return { size, grid: bestGrid };
  }

  global.BunkerQr = {
    encode: function (text) {
      const bytes = new TextEncoder().encode(String(text || ''));
      const version = chooseVersion(bytes.length);
      const cw = encodeData(bytes, version);
      return buildMatrix(cw, version);
    },
    encodeFlatString: function (text) {
      const { size, grid } = this.encode(text);
      let out = size + ':';
      for (let r = 0; r < size; r++) {
        for (let c = 0; c < size; c++) out += grid[r][c] ? '1' : '0';
      }
      return out;
    },
    toCanvas: function (text, pixelSize, marginModules) {
      const { size, grid } = this.encode(text);
      const margin = marginModules == null ? 2 : marginModules;
      const total = size + margin * 2;
      const canvas = document.createElement('canvas');
      canvas.width = pixelSize;
      canvas.height = pixelSize;
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(0, 0, pixelSize, pixelSize);
      ctx.fillStyle = '#090D16';
      const cell = pixelSize / total;
      for (let r = 0; r < size; r++) {
        for (let c = 0; c < size; c++) {
          if (grid[r][c]) {
            ctx.fillRect(
              Math.floor((c + margin) * cell),
              Math.floor((r + margin) * cell),
              Math.ceil(cell),
              Math.ceil(cell)
            );
          }
        }
      }
      return canvas;
    }
  };
})(window);
