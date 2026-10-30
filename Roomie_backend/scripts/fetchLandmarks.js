// Tải trường ĐH/CĐ, siêu thị, cửa hàng tiện lợi, bệnh viện, trạm xe buýt thật của TP.HCM
// từ OpenStreetMap (Overpass API), lọc rồi ghi ra data/hcm-landmarks.json.
// Chỉ cần chạy khi muốn cập nhật dữ liệu: npm run fetch:landmarks
// Sau đó nạp vào DB: npm run import:landmarks (seed.js cũng tự nạp file này).
const fs = require('fs');
const path = require('path');

const OUT_FILE = path.join(__dirname, '..', 'data', 'hcm-landmarks.json');
const OVERPASS_URL = 'https://overpass-api.de/api/interpreter';
// Ranh giới TP.HCM sau sáp nhập 01/07/2025 (gồm cả Bình Dương, Bà Rịa - Vũng Tàu): relation 1973756
const AREA_ID = 3600000000 + 1973756;

const QUERY = `[out:json][timeout:180];
area(${AREA_ID})->.hcm;
(
  nwr["amenity"~"^(university|college)$"]["name"](area.hcm);
  nwr["amenity"="hospital"]["name"](area.hcm);
  nwr["shop"~"^(supermarket|convenience)$"]["name"](area.hcm);
  node["highway"="bus_stop"]["name"](area.hcm);
);
out center tags;`;

/** "Trường Đại học Bách Khoa" -> "truong dai hoc bach khoa" */
function normalize(s) {
  return s.normalize('NFD').replace(/\p{M}/gu, '').replace(/[đĐ]/g, 'd').toLowerCase().replace(/\s+/g, ' ').trim();
}

// Tòa nhà, cổng, ký túc xá... nằm trong khuôn viên trường cũng được gắn amenity=university
const NOT_A_SCHOOL = /^(toa|nha |khu |day |ky tuc|ktx|cong |thu vien|can tin|canteen|building|block|hoi truong|san |bai xe|cafe|ca phe)/;
// Chỉ giữ đại học, cao đẳng, học viện (bỏ trung cấp, trường chính trị, trường đoàn...)
const SCHOOL = /(dai hoc|cao dang|hoc vien|university|college|academy)/;
const NOT_HIGHER_ED = /(trung cap|chinh tri|truong doan)/;
// "Bệnh viện X - Khoa Nội A1", "Phòng khám...", "Điểm sơ cấp cứu" -> không phải bệnh viện
const HOSPITAL = /(benh vien|hospital|^bv )/;
const HOSPITAL_PART = /(\s-\s|^)(khoa|phong|toa|khu|nha|cong|bai xe) /;
// shop=supermarket nhưng không bán đồ ăn / nhu yếu phẩm
const NOT_GROCERY = /(dien may|dien thoai|di dong|noi that|vat lieu|xe may|o to|my pham|thoi trang|sach|thuoc|bebe|me va be|baby|kids)/;
// Chuỗi siêu thị / cửa hàng tiện lợi. Tiệm tạp hóa nhỏ (tên chủ tiệm, số điện thoại) thì bỏ
const GROCERY_CHAIN = /(co\.?op|bach hoa xanh|winmart|vinmart|circle k|ministop|familymart|7-eleven|gs25|satra|b's mart|shop ?& ?go|kingfood|emart|lotte|aeon|big c|go!|mm mega|mega market|tops market|finelife|annam|lanchi|nhu yeu pham|sieu thi|supermarket|mart)/;

function classify(t) {
  const name = normalize(t.name);
  if (t.amenity === 'university' || t.amenity === 'college') {
    return SCHOOL.test(name) && !NOT_A_SCHOOL.test(name) && !NOT_HIGHER_ED.test(name) ? 'university' : null;
  }
  if (t.amenity === 'hospital') return HOSPITAL.test(name) && !HOSPITAL_PART.test(name) ? 'hospital' : null;
  if (t.shop === 'supermarket' || t.shop === 'convenience') {
    const chain = t.brand || GROCERY_CHAIN.test(name);
    return chain && !NOT_GROCERY.test(name) ? 'supermarket' : null;
  }
  if (t.highway === 'bus_stop') return 'bus_stop';
  return null;
}

function subtitle(type, t) {
  if (type === 'bus_stop') return t.route_ref ? `Tuyến ${t.route_ref.split(/\s*;\s*/).join(', ')}` : 'Trạm xe buýt';
  if (type === 'hospital') return t.emergency === 'yes' ? 'Có cấp cứu' : 'Bệnh viện';
  if (type === 'supermarket') {
    if (t.opening_hours === '24/7') return 'Mở cửa 24/7';
    return t.shop === 'convenience' ? 'Cửa hàng tiện lợi' : 'Siêu thị';
  }
  return null;
}

function address(t) {
  const street = [t['addr:housenumber'], t['addr:street']].filter(Boolean).join(' ');
  return street || null;
}

function distanceM(a, b) {
  const rad = Math.PI / 180;
  const x = (b.lng - a.lng) * rad * Math.cos(((a.lat + b.lat) / 2) * rad);
  const y = (b.lat - a.lat) * rad;
  return Math.sqrt(x * x + y * y) * 6371000;
}

// Cùng loại, cùng tên, cách nhau dưới khoảng này thì coi là 1 địa điểm
// (trạm xe buýt 2 chiều đường, trường vừa vẽ điểm vừa vẽ khuôn viên)
const SAME_PLACE_M = { university: 500, hospital: 300, supermarket: 60, bus_stop: 120 };

async function fetchLandmarks() {
  const res = await fetch(OVERPASS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'RoomieApp/1.0 (student project)' },
    body: new URLSearchParams({ data: QUERY }),
  });
  if (!res.ok) throw new Error(`Overpass trả về HTTP ${res.status}`);
  const { elements } = await res.json();

  const kept = [];
  const byKey = new Map();   // "loại|tên chuẩn hóa" -> các điểm đã giữ
  // Khuôn viên (way/relation) trước điểm (node): trường có cả 2 thì giữ khuôn viên
  const order = { relation: 0, way: 1, node: 2 };
  elements.sort((a, b) => order[a.type] - order[b.type]);
  for (const e of elements) {
    const t = e.tags || {};
    const lat = e.lat ?? e.center?.lat;
    const lng = e.lon ?? e.center?.lon;
    if (!t.name || lat == null) continue;
    const type = classify(t);
    if (!type) continue;
    const item = {
      ref: `${e.type[0]}${e.id}`,
      type,
      name: t.name.trim().slice(0, 180),
      shortName: (t.short_name || t['name:short'] || '').trim().slice(0, 60) || null,
      subtitle: subtitle(type, t),
      address: address(t),
      lat: Math.round(lat * 1e7) / 1e7,
      lng: Math.round(lng * 1e7) / 1e7,
    };
    const key = `${type}|${normalize(item.name)}`;
    const same = byKey.get(key) || [];
    if (same.some((o) => distanceM(o, item) < SAME_PLACE_M[type])) continue;
    same.push(item);
    byKey.set(key, same);
    kept.push(item);
  }
  kept.sort((a, b) => a.type.localeCompare(b.type) || a.name.localeCompare(b.name, 'vi'));
  return kept;
}

module.exports = { fetchLandmarks };

if (require.main === module) {
  fetchLandmarks()
    .then((items) => {
      // Mỗi địa điểm 1 dòng cho dễ xem thay đổi trên git
      const body = items.map((i) => JSON.stringify(i)).join(',\n');
      fs.writeFileSync(OUT_FILE,
        `{"source":"© OpenStreetMap contributors (ODbL)","fetchedAt":"${new Date().toISOString().slice(0, 10)}",\n"items":[\n${body}\n]}\n`);
      const count = {};
      for (const i of items) count[i.type] = (count[i.type] || 0) + 1;
      console.log(`Đã ghi ${items.length} địa điểm vào data/hcm-landmarks.json`, count);
    })
    .catch((err) => { console.error(err); process.exit(1); });
}
