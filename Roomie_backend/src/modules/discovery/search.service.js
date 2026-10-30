const db = require('../../db');
const AppError = require('../../common/AppError');
const { scorePosts } = require('./matching.service');

// Giới hạn số bài lấy ra để tính % phù hợp rồi sắp xếp trong JS (đủ cho quy mô đồ án).
const MAX_CANDIDATES = 500;

// Khoảng cách (km) giữa 2 tọa độ - công thức haversine
const HAVERSINE_KM = (lat1, lng1, lat2, lng2) => `
  6371 * 2 * ASIN(SQRT(
    POWER(SIN(RADIANS(${lat2} - ${lat1}) / 2), 2) +
    COS(RADIANS(${lat1})) * COS(RADIANS(${lat2})) * POWER(SIN(RADIANS(${lng2} - ${lng1}) / 2), 2)))`;

// Quyền riêng tư: tọa độ trả ra app lệch cố định ~60-120m theo từng phòng (cùng phòng luôn lệch
// như nhau nên marker không nhảy), để bản đồ công khai không chỉ đúng căn nhà.
// Khoảng cách / lọc vùng vẫn tính bằng tọa độ thật trong DB.
function approxLocation(postId, lat, lng) {
  if (lat == null || lng == null) return { latitude: null, longitude: null };
  let h = 0;
  for (const ch of String(postId)) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  const angle = (h % 360) * (Math.PI / 180);
  const meters = 60 + (h % 61);                 // 60..120m
  const dLat = (meters * Math.cos(angle)) / 111320;
  const dLng = (meters * Math.sin(angle)) / (111320 * Math.cos((lat * Math.PI) / 180));
  return { latitude: Math.round((lat + dLat) * 1e6) / 1e6, longitude: Math.round((lng + dLng) * 1e6) / 1e6 };
}

// RoomCardDto dùng chung cho Home / Tìm kiếm / Bản đồ / Đã lưu
function toRoomCard(r, score, isSaved) {
  const area = [r.street_address, r.district_legacy || r.ward_name].filter(Boolean).join(', ');
  return {
    id: r.post_id,
    title: r.title,
    monthlyRent: r.monthly_rent,
    addressText: area || r.address_text,
    coverImageUrl: r.cover_image_url,
    matchPercent: score ? score.match : null,
    isSaved,
    roomType: r.room_type,
    preferredGender: r.preferred_gender,
    districtName: r.district_legacy,
    landmarkName: r.landmark_name,
    distanceKm: r.distance_km == null ? null : Math.round(r.distance_km * 10) / 10,
    ...approxLocation(r.post_id, r.latitude, r.longitude),
  };
}

/**
 * Tìm phòng. Mọi điều kiện đều không bắt buộc:
 *   q           từ khóa không dấu: "ly thuong kiet", "binh thanh", "bach khoa"
 *   area        tên quận/phường: "Quận 10"
 *   landmarkId  gần trường/địa điểm, trong bán kính radiusKm (mặc định 3km)
 *   nearLat/nearLng  quanh một địa chỉ đã tra (Photon), trong bán kính radiusKm
 *   wardCode    đúng phường/xã (mã hành chính thật)
 *   provinceCode  trong tỉnh/thành đang chọn (bỏ qua khi tìm quanh 1 điểm hoặc theo phường)
 *   minRent, maxRent, roomTypes[], gender (female|male), amenities[] (phải có đủ)
 *   moveInBy (YYYY-MM-DD), minArea (m²), maxDeposit, minSlots (số người cần), occupation (student|worker)
 *   sort       match (mặc định) | newest | price_asc | price_desc | distance
 */
async function searchRooms(userId, f) {
  const where = [];
  const params = [];
  const p = (value) => { params.push(value); return `$${params.length}`; };

  let landmark = null;
  let distanceSql = 'NULL::float';
  if (f.landmarkId) {
    const { rows } = await db.query(
      'SELECT landmark_id, name, short_name, latitude, longitude FROM landmarks WHERE landmark_id = $1 AND is_active',
      [f.landmarkId]);
    landmark = rows[0];
    // Không còn trường này (dữ liệu nạp lại) mà app có gửi tọa độ thì tìm quanh tọa độ đó
    if (!landmark && (f.nearLat == null || f.nearLng == null)) throw AppError.notFound('Không tìm thấy trường / địa điểm');
  }
  if (landmark) {
    distanceSql = HAVERSINE_KM(p(landmark.latitude), p(landmark.longitude), 'r.latitude', 'r.longitude');
    where.push(`r.latitude IS NOT NULL AND ${distanceSql} <= ${p(f.radiusKm)}`);
  } else if (f.nearLat != null && f.nearLng != null) {
    // Quanh một địa chỉ người dùng chọn (số nhà, đường, địa danh)
    distanceSql = HAVERSINE_KM(p(f.nearLat), p(f.nearLng), 'r.latitude', 'r.longitude');
    where.push(`r.latitude IS NOT NULL AND ${distanceSql} <= ${p(f.radiusKm)}`);
  }
  const hasDistance = distanceSql !== 'NULL::float';

  if (f.q) {
    const q = p(f.q);
    // full-text không dấu trên tiêu đề/mô tả/địa chỉ, hoặc khớp một phần tên trường/phường/quận
    where.push(`(r.search_document @@ plainto_tsquery('simple', immutable_unaccent(${q}))
      OR immutable_unaccent(concat_ws(' ', r.landmark_name, r.ward_name, r.district_legacy, r.street_address))
         ILIKE '%' || immutable_unaccent(${q}) || '%')`);
  }
  if (f.area) {
    const area = p(f.area);
    where.push(`(lower(immutable_unaccent(coalesce(r.district_legacy, ''))) = lower(immutable_unaccent(${area}))
      OR lower(immutable_unaccent(coalesce(r.ward_name, ''))) = lower(immutable_unaccent(${area})))`);
  }
  if (f.wardCode) where.push(`r.ward_code = ${p(f.wardCode)}`);
  // Tìm quanh trường / địa chỉ / vị trí thì bán kính có thể vượt ranh giới tỉnh -> bỏ qua tỉnh
  if (f.provinceCode && !hasDistance && !f.wardCode) where.push(`r.province_code = ${p(f.provinceCode)}`);
  // Vùng đang xem trên bản đồ
  if (f.minLat != null && f.maxLat != null && f.minLng != null && f.maxLng != null) {
    where.push(`r.latitude BETWEEN ${p(f.minLat)} AND ${p(f.maxLat)}
      AND r.longitude BETWEEN ${p(f.minLng)} AND ${p(f.maxLng)}`);
  }
  if (f.minRent != null) where.push(`r.monthly_rent >= ${p(f.minRent)}`);
  if (f.maxRent != null) where.push(`r.monthly_rent <= ${p(f.maxRent)}`);
  if (f.roomTypes.length) where.push(`r.room_type = ANY(${p(f.roomTypes)}::text[])`);
  // "Nữ" = phòng nhận nữ hoặc không giới hạn giới tính
  if (f.gender) where.push(`r.preferred_gender IN ('any', ${p(f.gender)})`);
  if (f.moveInBy) where.push(`(r.available_from IS NULL OR r.available_from <= ${p(f.moveInBy)}::date)`);
  // Phòng không ghi diện tích / tiền cọc thì không chắc đạt điều kiện -> loại
  if (f.minArea != null) where.push(`r.area_m2 >= ${p(f.minArea)}`);
  if (f.maxDeposit != null) where.push(`r.deposit_amount <= ${p(f.maxDeposit)}`);
  if (f.minSlots != null) where.push(`r.seeking_roommates >= ${p(f.minSlots)}`);
  // "Sinh viên" = phòng nhận sinh viên hoặc không giới hạn
  if (f.occupation) where.push(`r.preferred_occupation IN ('any', ${p(f.occupation)})`);
  if (f.amenities.length) {
    where.push(`NOT EXISTS (
      SELECT 1 FROM amenities a
       WHERE a.amenity_code = ANY(${p(f.amenities)}::text[])
         AND NOT EXISTS (SELECT 1 FROM room_post_amenities ra
                          WHERE ra.post_id = r.post_id AND ra.amenity_id = a.amenity_id))`);
  }

  const orderBy = {
    newest: 'r.first_published_at DESC NULLS LAST',
    price_asc: 'r.monthly_rent ASC',
    price_desc: 'r.monthly_rent DESC',
    distance: hasDistance ? 'distance_km ASC' : 'r.first_published_at DESC NULLS LAST',
    match: 'r.first_published_at DESC NULLS LAST',   // sắp lại theo % ở dưới
  }[f.sort];

  const { rows } = await db.query(
    `SELECT r.post_id, r.title, r.monthly_rent, r.address_text, r.street_address, r.district_legacy,
            r.ward_name, r.landmark_name, r.cover_image_url, r.room_type, r.preferred_gender,
            r.latitude, r.longitude, r.first_published_at, ${distanceSql} AS distance_km
       FROM public_room_posts r
      ${where.length ? `WHERE ${where.join(' AND ')}` : ''}
      ORDER BY ${orderBy}
      LIMIT ${MAX_CANDIDATES}`, params);

  const scores = await scorePosts(userId, rows.map((r) => r.post_id));
  // Chip "≥ 80% phù hợp": lọc sau khi tính điểm (cần đăng nhập và đã làm bài test)
  if (f.minMatch != null) {
    for (let i = rows.length - 1; i >= 0; i--) {
      const m = scores.get(rows[i].post_id)?.match;
      if (m == null || m < f.minMatch) rows.splice(i, 1);
    }
  }
  if (f.sort === 'match') {
    // % cao trước; phòng chưa tính được % xếp cuối, giữ thứ tự mới nhất
    rows.sort((a, b) => (scores.get(b.post_id)?.match ?? -1) - (scores.get(a.post_id)?.match ?? -1));
  }

  const pageRows = rows.slice(f.offset, f.offset + f.limit);
  let saved = new Set();
  if (userId && pageRows.length) {
    const { rows: s } = await db.query(
      'SELECT post_id FROM saved_posts WHERE user_id = $1 AND post_id = ANY($2::uuid[])',
      [userId, pageRows.map((r) => r.post_id)]);
    saved = new Set(s.map((x) => x.post_id));
  }

  return {
    items: pageRows.map((r) => toRoomCard(r, scores.get(r.post_id), saved.has(r.post_id))),
    total: rows.length,
    landmark: landmark && { id: landmark.landmark_id, name: landmark.name, shortName: landmark.short_name },
  };
}

// Gợi ý trường học / địa điểm cho ô tìm kiếm, kèm số phòng trong bán kính.
// Có tỉnh: chưa gõ thì chỉ gợi ý trong tỉnh; đang gõ thì tìm cả nước, trường trong tỉnh lên trước
async function suggestLandmarks({ q, type, limit, radiusKm, provinceCode }) {
  const params = [radiusKm];
  const where = ['l.is_active'];
  let provinceFirst = '';
  if (type) { params.push(type); where.push(`l.landmark_type = $${params.length}`); }
  if (provinceCode) {
    params.push(provinceCode);
    if (q) provinceFirst = `(l.province_code = $${params.length}) DESC, `;
    else where.push(`l.province_code = $${params.length}`);
  }
  if (q) {
    params.push(q);
    where.push(`immutable_unaccent(concat_ws(' ', l.name, l.short_name, l.address_text))
                ILIKE '%' || immutable_unaccent($${params.length}) || '%'`);
  }
  params.push(limit);
  const { rows } = await db.query(
    `SELECT l.landmark_id, l.landmark_type, l.name, l.short_name, l.address_text, l.subtitle, l.latitude, l.longitude,
            (SELECT COUNT(*) FROM public_room_posts r
              WHERE r.latitude IS NOT NULL
                AND ${HAVERSINE_KM('l.latitude', 'l.longitude', 'r.latitude', 'r.longitude')} <= $1) AS room_count
       FROM landmarks l
      WHERE ${where.join(' AND ')}
      ORDER BY ${provinceFirst}room_count DESC, l.name
      LIMIT $${params.length}`, params);
  return rows.map((l) => ({
    id: l.landmark_id,
    type: l.landmark_type,
    name: l.name,
    shortName: l.short_name,
    addressText: l.address_text,
    subtitle: l.subtitle,
    latitude: l.latitude,
    longitude: l.longitude,
    roomCount: l.room_count,
  }));
}

module.exports = { searchRooms, suggestLandmarks, toRoomCard, approxLocation, HAVERSINE_KM };
