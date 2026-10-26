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
    latitude: r.latitude,
    longitude: r.longitude,
  };
}

/**
 * Tìm phòng. Mọi điều kiện đều không bắt buộc:
 *   q           từ khóa không dấu: "ly thuong kiet", "binh thanh", "bach khoa"
 *   area        tên quận/phường: "Quận 10"
 *   landmarkId  gần trường/địa điểm, trong bán kính radiusKm (mặc định 3km)
 *   minRent, maxRent, roomTypes[], gender (female|male), amenities[] (phải có đủ)
 *   sort        match (mặc định) | newest | price_asc | price_desc | distance
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
    if (!landmark) throw AppError.notFound('Không tìm thấy trường / địa điểm');
    distanceSql = HAVERSINE_KM(p(landmark.latitude), p(landmark.longitude), 'r.latitude', 'r.longitude');
    where.push(`r.latitude IS NOT NULL AND ${distanceSql} <= ${p(f.radiusKm)}`);
  }

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
  if (f.minRent != null) where.push(`r.monthly_rent >= ${p(f.minRent)}`);
  if (f.maxRent != null) where.push(`r.monthly_rent <= ${p(f.maxRent)}`);
  if (f.roomTypes.length) where.push(`r.room_type = ANY(${p(f.roomTypes)}::text[])`);
  // "Nữ" = phòng nhận nữ hoặc không giới hạn giới tính
  if (f.gender) where.push(`r.preferred_gender IN ('any', ${p(f.gender)})`);
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
    distance: landmark ? 'distance_km ASC' : 'r.first_published_at DESC NULLS LAST',
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

// Gợi ý trường học / địa điểm cho ô tìm kiếm, kèm số phòng trong bán kính
async function suggestLandmarks({ q, type, limit, radiusKm }) {
  const params = [radiusKm];
  const where = ['l.is_active'];
  if (type) { params.push(type); where.push(`l.landmark_type = $${params.length}`); }
  if (q) {
    params.push(q);
    where.push(`immutable_unaccent(concat_ws(' ', l.name, l.short_name, l.address_text))
                ILIKE '%' || immutable_unaccent($${params.length}) || '%'`);
  }
  params.push(limit);
  const { rows } = await db.query(
    `SELECT l.landmark_id, l.landmark_type, l.name, l.short_name, l.address_text, l.subtitle,
            (SELECT COUNT(*) FROM public_room_posts r
              WHERE r.latitude IS NOT NULL
                AND ${HAVERSINE_KM('l.latitude', 'l.longitude', 'r.latitude', 'r.longitude')} <= $1) AS room_count
       FROM landmarks l
      WHERE ${where.join(' AND ')}
      ORDER BY room_count DESC, l.name
      LIMIT $${params.length}`, params);
  return rows.map((l) => ({
    id: l.landmark_id,
    type: l.landmark_type,
    name: l.name,
    shortName: l.short_name,
    addressText: l.address_text,
    subtitle: l.subtitle,
    roomCount: l.room_count,
  }));
}

module.exports = { searchRooms, suggestLandmarks, toRoomCard };
