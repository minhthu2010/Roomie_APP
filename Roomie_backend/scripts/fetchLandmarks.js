// Tải trường ĐH/CĐ, chuỗi siêu thị / cửa hàng tiện lợi, bệnh viện, trạm xe buýt thật của cả nước
// từ OpenStreetMap (Overpass API), lọc rồi ghi ra:
//   data/vn-landmarks.json         địa điểm, mỗi cái gắn mã tỉnh/thành (provinceCode)
//   data/vn-province-centers.json  tâm 34 tỉnh/thành (trung tâm hành chính) cho bản đồ / gợi ý địa chỉ
// Chỉ cần chạy khi muốn cập nhật dữ liệu: npm run fetch:landmarks (mất vài phút, tải từng tỉnh)
// Sau đó nạp vào DB: npm run import:admin && npm run import:landmarks (seed.js cũng tự nạp).
const fs = require('fs');
const path = require('path');

const DATA_DIR = path.join(__dirname, '..', 'data');
const OUT_FILE = path.join(DATA_DIR, 'vn-landmarks.json');
const CENTERS_FILE = path.join(DATA_DIR, 'vn-province-centers.json');
const ADMIN_FILE = path.join(DATA_DIR, 'vn-admin-units.json');
const OVERPASS_URL = 'https://overpass-api.de/api/interpreter';
const USER_AGENT = 'RoomieApp/1.0 (student project)';
// Nghỉ giữa các lần gọi để không vượt giới hạn của máy chủ Overpass cộng đồng
const PAUSE_MS = 3000;

// 34 tỉnh/thành (ranh giới sau 01/07/2025) và tâm hành chính / nhãn của từng tỉnh
const PROVINCES_QUERY = `[out:json][timeout:120];
area["ISO3166-1"="VN"][admin_level=2]->.vn;
rel["admin_level"="4"]["boundary"="administrative"](area.vn)->.p;
.p out body;
node(r.p:"admin_centre");out;
node(r.p:"label");out;`;

const placesQuery = (relationId) => `[out:json][timeout:180];
area(${3600000000 + relationId})->.p;
(
  nwr["amenity"~"^(university|college)$"]["name"](area.p);
  nwr["amenity"="hospital"]["name"](area.p);
  nwr["shop"~"^(supermarket|convenience)$"]["name"](area.p);
  node["highway"="bus_stop"]["name"](area.p);
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

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

/** Gọi Overpass; máy chủ bận (429/504) thì chờ rồi thử lại */
async function overpass(query) {
  for (let attempt = 1; ; attempt++) {
    const res = await fetch(OVERPASS_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': USER_AGENT },
      body: new URLSearchParams({ data: query }),
    });
    if (res.ok) return (await res.json()).elements;
    if (attempt >= 4 || ![429, 502, 503, 504].includes(res.status)) throw new Error(`Overpass trả về HTTP ${res.status}`);
    await sleep(PAUSE_MS * attempt * 5);
  }
}

/** 34 tỉnh/thành: mã hành chính (khớp tên với vn-admin-units.json), relation OSM, tâm */
async function fetchProvinces() {
  const units = require(ADMIN_FILE);
  const codeByName = new Map(units.map((u) => [normalize(u.FullName), u.Code]));
  const elements = await overpass(PROVINCES_QUERY);
  const nodes = new Map(elements.filter((e) => e.type === 'node').map((n) => [n.id, n]));
  const provinces = [];
  for (const r of elements.filter((e) => e.type === 'relation')) {
    const code = codeByName.get(normalize(r.tags.name));
    if (!code) throw new Error(`Không khớp được tỉnh "${r.tags.name}" với dữ liệu hành chính`);
    const m = r.members.find((x) => x.type === 'node' && x.role === 'admin_centre')
      || r.members.find((x) => x.type === 'node' && x.role === 'label');
    const center = m && nodes.get(m.ref);
    if (!center) throw new Error(`Tỉnh "${r.tags.name}" không có tâm trên OSM`);
    provinces.push({ code, name: r.tags.name, relation: r.id,
      lat: Math.round(center.lat * 1e5) / 1e5, lng: Math.round(center.lon * 1e5) / 1e5 });
  }
  if (provinces.length !== units.length) throw new Error(`OSM có ${provinces.length} tỉnh, dữ liệu hành chính có ${units.length}`);
  return provinces.sort((a, b) => a.code.localeCompare(b.code));
}

/** Địa điểm trong 1 tỉnh, đã lọc và gộp điểm trùng */
function pickPlaces(elements, provinceCode) {
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
      provinceCode,
      name: t.name.trim().slice(0, 180),
      shortName: (t.short_name || t['name:short'] || '').trim().slice(0, 60) || undefined,
      subtitle: subtitle(type, t) || undefined,
      address: address(t) || undefined,
      lat: Math.round(lat * 1e6) / 1e6,
      lng: Math.round(lng * 1e6) / 1e6,
    };
    const key = `${type}|${normalize(item.name)}`;
    const same = byKey.get(key) || [];
    if (same.some((o) => distanceM(o, item) < SAME_PLACE_M[type])) continue;
    same.push(item);
    byKey.set(key, same);
    kept.push(item);
  }
  return kept;
}

async function fetchLandmarks() {
  const provinces = await fetchProvinces();
  const items = [];
  const seen = new Set();   // điểm nằm trên ranh giới 2 tỉnh chỉ giữ 1 lần
  for (const p of provinces) {
    await sleep(PAUSE_MS);
    const places = pickPlaces(await overpass(placesQuery(p.relation)), p.code).filter((i) => !seen.has(i.ref));
    places.forEach((i) => seen.add(i.ref));
    items.push(...places);
    console.log(`  ${p.name}: ${places.length} địa điểm`);
  }
  items.sort((a, b) => a.provinceCode.localeCompare(b.provinceCode) || a.type.localeCompare(b.type)
    || a.name.localeCompare(b.name, 'vi'));
  return { provinces, items };
}

module.exports = { fetchLandmarks };

if (require.main === module) {
  fetchLandmarks()
    .then(({ provinces, items }) => {
      const fetchedAt = new Date().toISOString().slice(0, 10);
      const source = '© OpenStreetMap contributors (ODbL)';
      // Mỗi dòng 1 mục cho dễ xem thay đổi trên git
      const lines = (arr) => arr.map((i) => JSON.stringify(i)).join(',\n');
      fs.writeFileSync(CENTERS_FILE,
        `{"source":"${source}","fetchedAt":"${fetchedAt}",\n"provinces":[\n${lines(provinces)}\n]}\n`);
      fs.writeFileSync(OUT_FILE,
        `{"source":"${source}","fetchedAt":"${fetchedAt}",\n"items":[\n${lines(items)}\n]}\n`);
      const count = {};
      for (const i of items) count[i.type] = (count[i.type] || 0) + 1;
      console.log(`Đã ghi ${provinces.length} tâm tỉnh/thành và ${items.length} địa điểm`, count);
    })
    .catch((err) => { console.error(err); process.exit(1); });
}
