const db = require('../../db');
const AppError = require('../../common/AppError');

// Tra địa chỉ -> tọa độ bằng Photon (https://photon.komoot.io, dữ liệu OpenStreetMap).
// Không dùng nominatim.openstreetmap.org vì một số nhà mạng ở VN chặn tên miền này.
// Photon là dịch vụ cộng đồng: gọi qua backend, lưu cache (bảng geocode_cache), app chỉ gọi khi ngừng gõ.
const PHOTON_URL = 'https://photon.komoot.io';
const USER_AGENT = 'RoomieApp/1.0 (do an Roomie)';
const CACHE_DAYS = 30;
const VN_BBOX = '102.1,8.2,109.6,23.4';                 // minLon,minLat,maxLon,maxLat
const DEFAULT_BIAS = { lat: 10.776, lon: 106.700 };      // chưa chọn tỉnh: ưu tiên gần trung tâm TP.HCM

// "Phường Diên Hồng" -> "phuong dien hong" (bỏ dấu, kể cả "đ")
function normalize(text) {
  return String(text || '').normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().replace(/\s+/g, ' ').trim();
}

async function cached(key, loader) {
  const { rows } = await db.query(
    `SELECT results FROM geocode_cache
      WHERE query_key = $1 AND created_at > NOW() - make_interval(days => $2)`, [key, CACHE_DAYS]);
  if (rows[0]) return rows[0].results;
  const results = await loader();
  await db.query(
    `INSERT INTO geocode_cache (query_key, results) VALUES ($1, $2)
     ON CONFLICT (query_key) DO UPDATE SET results = EXCLUDED.results, created_at = NOW()`,
    [key, JSON.stringify(results)]);
  return results;
}

async function photon(pathname, params) {
  const url = new URL(pathname, PHOTON_URL);
  Object.entries(params).forEach(([k, v]) => v != null && url.searchParams.set(k, String(v)));
  let res;
  try {
    res = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal: AbortSignal.timeout(6000) });
  } catch {
    throw new AppError(503, 'GEOCODER_UNAVAILABLE', 'Không tra được địa chỉ lúc này, thử lại sau');
  }
  if (!res.ok) throw new AppError(503, 'GEOCODER_UNAVAILABLE', 'Không tra được địa chỉ lúc này, thử lại sau');
  return res.json();
}

// OSM đôi khi ghi tên thành phố bằng tiếng Anh
const CITY_NAMES_VI = { 'ho chi minh city': 'TP. Hồ Chí Minh', 'thanh pho ho chi minh': 'TP. Hồ Chí Minh',
  'ho chi minh': 'TP. Hồ Chí Minh', hanoi: 'Hà Nội', 'ha noi': 'Hà Nội', 'thanh pho ha noi': 'Hà Nội' };
const cityVi = (name) => (name ? CITY_NAMES_VI[normalize(name)] || name : null);

// Feature GeoJSON của Photon -> địa điểm gọn cho app
function toPlace(f) {
  const p = f.properties;
  const [lng, lat] = f.geometry.coordinates;
  const streetLine = [p.housenumber, p.street].filter(Boolean).join(' ');
  const title = p.name || streetLine || p.district || p.city;
  // dòng phụ: số nhà + đường (nếu tên là địa danh), phường, thành phố; bỏ trùng
  const parts = [p.name ? streetLine : null, p.district, cityVi(p.city || p.state)]
    .filter((x) => x && x !== title);
  return {
    title,
    subtitle: [...new Set(parts)].join(', '),
    latitude: Math.round(lat * 1e6) / 1e6,
    longitude: Math.round(lng * 1e6) / 1e6,
    // street | house | district | city | locality... để app chọn mức phóng bản đồ
    kind: p.type || 'house',
  };
}

// Tâm tỉnh/thành (provinces.center_lat/lng, migration 006) để ưu tiên kết quả gần tỉnh người dùng chọn
async function provinceBias(provinceCode) {
  if (!provinceCode) return DEFAULT_BIAS;
  const { rows } = await db.query(
    'SELECT center_lat, center_lng FROM provinces WHERE province_code = $1', [provinceCode]);
  const p = rows[0];
  return p && p.center_lat != null ? { lat: p.center_lat, lon: p.center_lng } : DEFAULT_BIAS;
}

// Gợi ý địa chỉ (số nhà, đường, địa danh) trong Việt Nam, ưu tiên gần tỉnh đang chọn
async function searchAddresses(q, { limit = 5, provinceCode } = {}) {
  const key = `addr:${normalize(q)}:${limit}:${provinceCode || ''}`;
  return cached(key, async () => {
    const bias = await provinceBias(provinceCode);
    const data = await photon('/api/', { q, limit: limit + 3, bbox: VN_BBOX, lat: bias.lat, lon: bias.lon });
    const seen = new Set();
    return data.features
      .filter((f) => f.properties.countrycode === 'VN')
      .map(toPlace)
      // bỏ kết quả trùng tên + dòng phụ (OSM chia 1 con đường thành nhiều đoạn)
      .filter((p) => {
        const k = `${normalize(p.title)}|${normalize(p.subtitle)}`;
        if (seen.has(k)) return false;
        seen.add(k);
        return true;
      })
      .slice(0, limit);
  });
}

// ---------- Đơn vị hành chính (dữ liệu thật, bảng provinces / wards) ----------

async function listProvinces() {
  const { rows } = await db.query(
    `SELECT province_code, province_name, center_lat, center_lng FROM provinces
      ORDER BY display_order, province_name`);
  return rows.map((r) => ({
    code: r.province_code, name: r.province_name, latitude: r.center_lat, longitude: r.center_lng,
  }));
}

// Tìm phường/xã không dấu, có thể giới hạn trong 1 tỉnh
async function searchWards({ q, provinceCode, limit }) {
  const params = [];
  const where = [];
  if (provinceCode) { params.push(provinceCode); where.push(`w.province_code = $${params.length}`); }
  if (q) {
    params.push(normalize(q));
    // "dien hong" khớp "Phường Diên Hồng"; "phuong dien" cũng khớp
    where.push(`lower(immutable_unaccent(w.ward_name)) LIKE '%' || $${params.length} || '%'`);
  }
  params.push(limit);
  const { rows } = await db.query(
    `SELECT w.ward_code, w.ward_name, w.province_code, p.province_name,
            (SELECT COUNT(*) FROM public_room_posts r WHERE r.ward_code = w.ward_code) AS room_count
       FROM wards w JOIN provinces p ON p.province_code = w.province_code
      ${where.length ? `WHERE ${where.join(' AND ')}` : ''}
      ORDER BY room_count DESC, w.ward_name
      LIMIT $${params.length}`, params);
  return rows.map((r) => ({
    code: r.ward_code, name: r.ward_name, provinceCode: r.province_code,
    provinceName: r.province_name, roomCount: r.room_count,
  }));
}

// Tâm gần đúng của 1 phường/xã để bản đồ nhảy tới (dữ liệu hành chính không có tọa độ -> tra Photon)
async function wardCenter(wardCode) {
  const { rows } = await db.query(
    `SELECT w.ward_code, w.ward_name, p.province_name FROM wards w
       JOIN provinces p ON p.province_code = w.province_code WHERE w.ward_code = $1`, [wardCode]);
  const w = rows[0];
  if (!w) throw AppError.notFound('Không tìm thấy phường/xã');
  const results = await cached(`ward:${w.ward_code}`, async () => {
    // Bỏ tiền tố "Phường"/"Xã" rồi tìm trong tỉnh; ưu tiên kết quả có district trùng tên phường
    const shortName = w.ward_name.replace(/^(Phường|Xã|Đặc khu)\s+/u, '');
    const data = await photon('/api/', { q: `${w.ward_name}, ${w.province_name}`, limit: 8, bbox: VN_BBOX });
    const features = data.features.filter((f) => f.properties.countrycode === 'VN');
    const match = features.find((f) => normalize(f.properties.district) === normalize(shortName)
        || normalize(f.properties.name) === normalize(w.ward_name))
      || features.find((f) => normalize(f.properties.name).includes(normalize(shortName)));
    return match ? [toPlace(match)] : [];
  });
  const place = results[0];
  return {
    code: w.ward_code,
    name: w.ward_name,
    provinceName: w.province_name,
    latitude: place ? place.latitude : null,
    longitude: place ? place.longitude : null,
  };
}

// ---------- Mặc định tìm kiếm lấy từ hồ sơ ----------

// Cách viết tắt tên thành phố hay gặp trong tên trường gõ tay -> chữ có trong tên đầy đủ (không dấu)
const CITY_ALIASES = [
  ['tp hcm', 'ho chi minh'], ['tphcm', 'ho chi minh'], ['hcm', 'ho chi minh'], ['ho chi minh', 'ho chi minh'],
  ['sai gon', 'ho chi minh'], ['ha noi', 'ha noi'], ['hn', 'ha noi'], ['da nang', 'da nang'],
  ['can tho', 'can tho'], ['hue', 'hue'], ['hai phong', 'hai phong'],
];

/**
 * GET /me/search-defaults: tỉnh / phường / trường trong hồ sơ người dùng, để app tự đặt tỉnh ở Home
 * và hiện lối tắt "Khu vực của bạn", "Gần trường của bạn". Hồ sơ chỉ là gợi ý, không lọc bớt phòng.
 * Hồ sơ cũ chỉ có chữ (living_area_text, school_or_workplace) thì thử khớp tên với dữ liệu thật.
 */
async function searchDefaults(userId) {
  const { rows } = await db.query(
    `SELECT living_province_code, living_ward_code, living_area_text, landmark_id, school_or_workplace
       FROM user_profiles WHERE user_id = $1`, [userId]);
  const p = rows[0] || {};
  let provinceCode = p.living_province_code;
  let wardCode = p.living_ward_code;
  let landmarkId = p.landmark_id;

  // "Phường Xuân Hoà, TP. Hồ Chí Minh" / "Xuân Hòa" / "Đà Nẵng" -> mã phường hoặc tỉnh thật
  if (!wardCode && p.living_area_text) {
    const parts = p.living_area_text.split(',').map((x) => normalize(x)).filter(Boolean);
    const { rows: w } = await db.query(
      `SELECT ward_code, province_code FROM wards
        WHERE lower(immutable_unaccent(ward_name)) = ANY($1::text[])
           OR lower(immutable_unaccent(regexp_replace(ward_name, '^(Phường|Xã|Đặc khu) ', ''))) = ANY($1::text[])
        ORDER BY (province_code = $2) DESC NULLS LAST LIMIT 1`, [parts, provinceCode || null]);
    if (w[0]) { wardCode = w[0].ward_code; provinceCode = provinceCode || w[0].province_code; }
  }
  if (!provinceCode && p.living_area_text) {
    const text = normalize(p.living_area_text);
    const { rows: all } = await db.query('SELECT province_code, province_name FROM provinces');
    const hit = all.find((x) => text.includes(normalize(x.province_name.replace(/^(Thành phố|Tỉnh) /u, ''))));
    if (hit) provinceCode = hit.province_code;
  }
  // Trường gõ tay ("ĐH Bách Khoa TP.HCM"): tìm trường thật có đủ các chữ chính ("bach khoa"),
  // ưu tiên trường ở thành phố ghi kèm ("TP.HCM" -> "ho chi minh"), rồi ở tỉnh của người dùng
  if (!landmarkId && p.school_or_workplace && p.school_or_workplace.trim().length >= 4) {
    let text = ` ${normalize(p.school_or_workplace).replace(/[^a-z0-9 ]/g, ' ')} `;
    const city = CITY_ALIASES.find(([alias]) => text.includes(` ${alias} `));
    if (city) text = text.replace(` ${city[0]} `, ' ');
    // "ĐH ..." thì ưu tiên trường đại học, "CĐ ..." thì cao đẳng
    const level = / (dh|dai hoc) /.test(text) ? '%dai hoc%' : / (cd|cao dang) /.test(text) ? '%cao dang%' : '';
    let words = text.replace(/ (dh|truong|dai hoc|hoc vien|cd|cao dang|tp|thanh pho) /g, ' ')
      .split(' ').filter((x) => x.length > 1);
    // "Đại học Đà Nẵng": tên trường chính là tên thành phố
    if (!words.length && city) words = city[1].split(' ');
    if (words.length) {
      const { rows: l } = await db.query(
        `SELECT landmark_id FROM landmarks
          WHERE is_active AND landmark_type = 'university'
            AND lower(immutable_unaccent(name)) LIKE ALL($1::text[])
          ORDER BY (lower(immutable_unaccent(name)) LIKE $2) DESC, (lower(immutable_unaccent(name)) LIKE $3) DESC,
                   (province_code = $4) DESC NULLS LAST, length(name)
          LIMIT 1`,
        [words.map((x) => `%${x}%`), city ? `%${city[1]}%` : '', level, provinceCode || null]);
      if (l[0]) landmarkId = l[0].landmark_id;
    }
  }

  const [province, ward, school] = await Promise.all([
    provinceCode ? db.query(
      'SELECT province_code, province_name, center_lat, center_lng FROM provinces WHERE province_code = $1',
      [provinceCode]) : { rows: [] },
    wardCode ? db.query(
      'SELECT ward_code, ward_name, province_code FROM wards WHERE ward_code = $1', [wardCode]) : { rows: [] },
    landmarkId ? db.query(
      `SELECT landmark_id, name, short_name, latitude, longitude, province_code FROM landmarks
        WHERE landmark_id = $1 AND is_active`, [landmarkId]) : { rows: [] },
  ]);
  // Chưa có tỉnh nhưng có trường -> lấy tỉnh của trường
  if (!province.rows[0] && school.rows[0]) {
    province.rows = (await db.query(
      'SELECT province_code, province_name, center_lat, center_lng FROM provinces WHERE province_code = $1',
      [school.rows[0].province_code])).rows;
  }
  const pr = province.rows[0];
  const wd = ward.rows[0];
  const sc = school.rows[0];
  return {
    province: pr ? { code: pr.province_code, name: pr.province_name, latitude: pr.center_lat, longitude: pr.center_lng } : null,
    ward: wd ? { code: wd.ward_code, name: wd.ward_name, provinceCode: wd.province_code } : null,
    school: sc ? { id: sc.landmark_id, name: sc.name, shortName: sc.short_name,
      latitude: sc.latitude, longitude: sc.longitude, provinceCode: sc.province_code } : null,
  };
}

module.exports = { searchAddresses, listProvinces, searchWards, wardCenter, searchDefaults, normalize };
