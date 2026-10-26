// Vẽ ảnh phòng minh họa (PNG) cho seed, không cần thư viện ngoài.
// Mỗi ảnh: tường, sàn, cửa sổ, giường/kệ với bảng màu riêng.
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const W = 640;
const H = 480;

function png(width, height, pixel) {
  const raw = Buffer.alloc((width * 3 + 1) * height);
  for (let y = 0; y < height; y++) {
    const row = y * (width * 3 + 1);
    raw[row] = 0; // filter: none
    for (let x = 0; x < width; x++) {
      const [r, g, b] = pixel(x, y);
      raw[row + 1 + x * 3] = r;
      raw[row + 2 + x * 3] = g;
      raw[row + 3 + x * 3] = b;
    }
  }
  const chunk = (type, data) => {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(zlib.crc32(td));
    return Buffer.concat([len, td, crc]);
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;  // bit depth
  ihdr[9] = 2;  // RGB
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw)),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

const hex = (h) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
const inRect = (x, y, [x1, y1, x2, y2]) => x >= x1 && x < x2 && y >= y1 && y < y2;

// variant: 0 = phòng ngủ, 1 = góc cửa sổ, 2 = bếp/kệ
function drawRoom(palette, variant) {
  const [wall, floor, accent, furniture] = palette.map(hex);
  const windowRect = variant === 1 ? [200, 60, 440, 250] : [420, 70, 580, 220];
  const bedRect = variant === 2 ? [60, 230, 300, 400] : [80, 280, 380, 420];
  const shelfRect = [460, 260, 600, 420];
  return png(W, H, (x, y) => {
    if (inRect(x, y, windowRect)) {
      const frame = Math.abs(x - (windowRect[0] + windowRect[2]) / 2) < 4 || Math.abs(y - (windowRect[1] + windowRect[3]) / 2) < 4;
      return frame ? [255, 255, 255] : [190 + (y % 40), 220, 245];
    }
    if (inRect(x, y, bedRect)) return y < bedRect[1] + 30 ? accent : furniture;
    if (variant !== 1 && inRect(x, y, shelfRect)) return (y - shelfRect[1]) % 50 < 6 ? accent : furniture;
    if (y > 360) return floor.map((c) => c - ((x + y) % 64 < 2 ? 25 : 0));
    const shade = Math.round((y / 360) * 18);
    return wall.map((c) => Math.max(0, c - shade));
  });
}

const PALETTES = [
  ['#E9DCCB', '#B08968', '#7F5539', '#DDB892'],
  ['#D8E2DC', '#9C8B7A', '#4A6C6F', '#A7C4BC'],
  ['#F2E8CF', '#A98467', '#6C584C', '#ADC178'],
  ['#E0E1DD', '#8D99AE', '#2B2D42', '#BFC0C0'],
  ['#FAEDCD', '#D4A373', '#9C6644', '#E9EDC9'],
  ['#EDE0D4', '#B08968', '#583101', '#DDB892'],
];

// Ghi ảnh vào uploads/seed, trả về đường dẫn tương đối (lưu vào post_images.image_url)
function writeSampleImages(postIndex, count) {
  const dir = path.join(__dirname, '..', 'uploads', 'seed');
  fs.mkdirSync(dir, { recursive: true });
  const urls = [];
  for (let i = 0; i < count; i++) {
    const file = `room${postIndex + 1}-${i + 1}.png`;
    const full = path.join(dir, file);
    if (!fs.existsSync(full)) fs.writeFileSync(full, drawRoom(PALETTES[(postIndex + i) % PALETTES.length], i % 3));
    urls.push(`/uploads/seed/${file}`);
  }
  return urls;
}

module.exports = { writeSampleImages };
