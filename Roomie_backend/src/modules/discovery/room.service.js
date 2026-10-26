const db = require('../../db');
const AppError = require('../../common/AppError');
const { scorePosts, compareUsers } = require('./matching.service');
const { toRoomCard, approxLocation, HAVERSINE_KM } = require('./search.service');

// Bài xem được: đã duyệt và chưa xóa; chủ bài luôn xem được bài của mình
async function loadPostRow(postId, viewerId) {
  const { rows } = await db.query(
    // DATE -> chuỗi để không bị lệch ngày do múi giờ khi pg đổi sang Date của JS
    `SELECT rp.*, to_char(rp.available_from, 'YYYY-MM-DD') AS available_from_text, p.province_name, w.ward_name, l.name AS landmark_name, l.short_name AS landmark_short_name,
            COALESCE(u.display_name, u.full_name) AS owner_name, u.avatar_url AS owner_avatar,
            u.created_at AS owner_since, up.occupation AS owner_occupation, up.birth_year AS owner_birth_year,
            up.school_or_workplace AS owner_school,
            COALESCE(us.show_contact_on_posts, TRUE) AS owner_show_contact,
            (rp.moderation_status = 'approved' AND rp.listing_status = 'searching'
              AND (rp.expires_at IS NULL OR rp.expires_at > NOW())) AS can_contact
       FROM room_posts rp
       JOIN users u ON u.user_id = rp.owner_id
       LEFT JOIN user_profiles up ON up.user_id = rp.owner_id
       LEFT JOIN user_settings us ON us.user_id = rp.owner_id
       LEFT JOIN provinces p ON p.province_code = rp.province_code
       LEFT JOIN wards w ON w.ward_code = rp.ward_code
       LEFT JOIN landmarks l ON l.landmark_id = rp.landmark_id
      WHERE rp.post_id = $1 AND rp.deleted_at IS NULL`, [postId]);
  const r = rows[0];
  if (!r || (r.moderation_status !== 'approved' && r.owner_id !== viewerId)) {
    throw AppError.notFound('Không tìm thấy bài đăng');
  }
  return r;
}

async function listImages(postId) {
  const { rows } = await db.query(
    `SELECT image_id, image_url, caption, is_cover FROM post_images
      WHERE post_id = $1 ORDER BY is_cover DESC, display_order`, [postId]);
  return rows.map((i) => ({ id: i.image_id, url: i.image_url, caption: i.caption, isCover: i.is_cover }));
}

// GET /room-posts/:postId - chi tiết phòng
async function getRoomDetail(viewerId, postId) {
  const r = await loadPostRow(postId, viewerId);
  const [images, amenities, saved, scores] = await Promise.all([
    listImages(postId),
    db.query(
      `SELECT a.amenity_code, a.amenity_name, a.icon_key FROM room_post_amenities ra
         JOIN amenities a ON a.amenity_id = ra.amenity_id
        WHERE ra.post_id = $1 ORDER BY a.display_order`, [postId]),
    db.query(
      `SELECT COUNT(*) AS saved_count,
              BOOL_OR(user_id = $2) AS is_saved
         FROM saved_posts WHERE post_id = $1`, [postId, viewerId]),
    scorePosts(viewerId, [postId]),
  ]);

  // Số điện thoại chỉ trả cho người đã đăng nhập, khi chủ bài bật "Hiện liên hệ" và bài còn đang tìm
  const showContact = Boolean(viewerId) && r.owner_show_contact && r.can_contact;
  const score = scores.get(postId);
  const area = [r.street_address, r.district_legacy || r.ward_name].filter(Boolean).join(', ');

  return {
    id: r.post_id,
    title: r.title,
    description: r.description,
    roomType: r.room_type,
    monthlyRent: r.monthly_rent,
    depositAmount: r.deposit_amount,
    areaM2: r.area_m2,
    utilityCostNote: r.utility_cost_note,
    availableFrom: r.available_from_text,
    currentOccupants: r.current_occupants,
    seekingRoommates: r.seeking_roommates,
    preferredGender: r.preferred_gender,
    preferredOccupation: r.preferred_occupation,
    smokingAllowed: r.smoking_allowed,
    petsAllowed: r.pets_allowed,
    roommateExpectations: r.roommate_expectations,
    addressText: area || r.address_text,
    districtName: r.district_legacy,
    provinceName: r.province_name,
    landmark: r.landmark_id ? { id: r.landmark_id, name: r.landmark_name, shortName: r.landmark_short_name } : null,
    ...approxLocation(r.post_id, r.latitude, r.longitude),
    listingStatus: r.listing_status,
    canContact: r.can_contact,
    firstPublishedAt: r.first_published_at,
    amenities: amenities.rows.map((a) => ({ code: a.amenity_code, name: a.amenity_name, iconKey: a.icon_key })),
    images,
    matchPercent: score ? score.match : null,
    isSaved: Boolean(saved.rows[0].is_saved),
    savedCount: saved.rows[0].saved_count,
    isOwnPost: r.owner_id === viewerId,
    owner: {
      userId: r.owner_id,
      displayName: r.owner_name,
      avatarUrl: r.owner_avatar,
      occupation: r.owner_occupation,
      birthYear: r.owner_birth_year,
      schoolOrWorkplace: r.owner_school,
      memberSince: r.owner_since,
    },
    contact: {
      phone: showContact ? r.contact_phone : null,
      allowZalo: r.allow_zalo,
      allowCall: r.allow_call,
      // lý do không hiện số: login | hidden_by_owner | not_available
      hiddenReason: showContact ? null : (!viewerId ? 'login' : (!r.can_contact ? 'not_available' : 'hidden_by_owner')),
    },
  };
}

// GET /room-posts/:postId/images - thư viện ảnh
async function getRoomImages(viewerId, postId) {
  const r = await loadPostRow(postId, viewerId);
  const area = [r.street_address, r.district_legacy || r.ward_name].filter(Boolean).join(', ');
  return { postId, title: r.title, addressText: area, ownerName: r.owner_name, images: await listImages(postId) };
}

// GET /room-posts/:postId/nearby - màn Vị trí phòng: tọa độ gần đúng + địa điểm lân cận
async function getNearby(viewerId, postId, { radiusKm, limit }) {
  const r = await loadPostRow(postId, viewerId);
  if (r.latitude == null) {
    return { postId, title: r.title, addressText: r.street_address, latitude: null, longitude: null, places: [] };
  }
  // Khoảng cách tính từ tọa độ thật; app chỉ nhận tọa độ gần đúng của phòng
  const distance = HAVERSINE_KM('$1', '$2', 'l.latitude', 'l.longitude');
  const { rows } = await db.query(
    `SELECT l.landmark_id, l.landmark_type, l.name, l.short_name, l.subtitle, l.address_text,
            l.latitude, l.longitude, ${distance} AS distance_km
       FROM landmarks l
      WHERE l.is_active AND ${distance} <= $3
      ORDER BY distance_km
      LIMIT $4`, [r.latitude, r.longitude, radiusKm, limit]);
  const area = [r.street_address, r.district_legacy || r.ward_name].filter(Boolean).join(', ');
  return {
    postId,
    title: r.title,
    addressText: area || r.address_text,
    areaText: [r.ward_name, r.province_name].filter(Boolean).join(', '),
    ...approxLocation(r.post_id, r.latitude, r.longitude),
    places: rows.map((l) => ({
      id: l.landmark_id,
      type: l.landmark_type,
      name: l.short_name || l.name,
      fullName: l.name,
      subtitle: l.subtitle || l.address_text,
      latitude: l.latitude,
      longitude: l.longitude,
      distanceKm: Math.round(l.distance_km * 100) / 100,
    })),
  };
}

// ---------- Đã lưu ----------

async function savePost(userId, postId) {
  await loadPostRow(postId, userId);
  await db.query('INSERT INTO saved_posts (user_id, post_id) VALUES ($1, $2) ON CONFLICT DO NOTHING', [userId, postId]);
  return { postId, isSaved: true };
}

async function unsavePost(userId, postId) {
  await db.query('DELETE FROM saved_posts WHERE user_id = $1 AND post_id = $2', [userId, postId]);
  return { postId, isSaved: false };
}

// Danh sách đã lưu: giữ cả bài đã đủ người / ẩn / hết hạn để app hiện nhãn trạng thái
async function listSaved(userId, { limit, offset }) {
  const { rows } = await db.query(
    `SELECT s.post_id, s.saved_at, s.listing_status, s.can_contact,
            rp.title, rp.monthly_rent, rp.address_text, rp.street_address, rp.district_legacy,
            w.ward_name, rp.room_type, rp.preferred_gender, rp.latitude, rp.longitude,
            l.name AS landmark_name,
            (SELECT pi.image_url FROM post_images pi WHERE pi.post_id = s.post_id
              ORDER BY pi.is_cover DESC, pi.display_order LIMIT 1) AS cover_image_url,
            COUNT(*) OVER () AS total
       FROM saved_posts_with_status s
       JOIN room_posts rp ON rp.post_id = s.post_id
       LEFT JOIN wards w ON w.ward_code = rp.ward_code
       LEFT JOIN landmarks l ON l.landmark_id = rp.landmark_id
      WHERE s.user_id = $1 AND s.moderation_status = 'approved'
      ORDER BY s.saved_at DESC
      LIMIT $2 OFFSET $3`, [userId, limit, offset]);
  const scores = await scorePosts(userId, rows.map((r) => r.post_id));
  return {
    items: rows.map((r) => ({
      ...toRoomCard(r, scores.get(r.post_id), true),
      listingStatus: r.listing_status,
      canContact: r.can_contact,
      savedAt: r.saved_at,
    })),
    total: rows.length ? rows[0].total : 0,
  };
}

// POST /room-posts/:postId/contact-clicks - đếm "Người quan tâm" ở Bài đăng của tôi
async function logContactClick(userId, postId, channel) {
  const r = await loadPostRow(postId, userId);
  if (r.owner_id !== userId) {
    await db.query('INSERT INTO post_contact_clicks (post_id, user_id, channel) VALUES ($1, $2, $3)',
      [postId, userId, channel]);
  }
  return { postId, channel };
}

// ---------- Hồ sơ người đăng ----------

async function getPublicProfile(viewerId, userId) {
  const { rows } = await db.query(
    `SELECT u.user_id, COALESCE(u.display_name, u.full_name) AS display_name, u.avatar_url, u.created_at,
            u.email_verified_at IS NOT NULL AS email_verified,
            up.bio, up.gender, up.birth_year, up.occupation, up.school_or_workplace,
            COALESCE(us.show_profile, TRUE) AS show_profile
       FROM users u
       LEFT JOIN user_profiles up ON up.user_id = u.user_id
       LEFT JOIN user_settings us ON us.user_id = u.user_id
      WHERE u.user_id = $1 AND u.account_status = 'active'`, [userId]);
  const u = rows[0];
  if (!u) throw AppError.notFound('Không tìm thấy người dùng');

  const base = { userId: u.user_id, displayName: u.display_name, avatarUrl: u.avatar_url, isMe: u.user_id === viewerId };
  // Người dùng tắt "Hiện hồ sơ": chỉ trả tên + ảnh
  if (!u.show_profile && u.user_id !== viewerId) return { ...base, isHidden: true };

  // Thói quen nổi bật: câu trả lời của các câu có trọng số cao nhất
  const [habits, posts, match] = await Promise.all([
    db.query(
      `SELECT q.question_key, o.option_label
         FROM user_lifestyle_answers a
         JOIN lifestyle_questions q ON q.question_id = a.question_id
         JOIN questionnaire_versions v ON v.questionnaire_version_id = q.questionnaire_version_id AND v.status = 'published'
         JOIN lifestyle_question_options o ON o.question_id = q.question_id AND o.option_key = a.answer_value->>'option_key'
        WHERE a.user_id = $1
        ORDER BY q.weight DESC, q.display_order
        LIMIT 4`, [userId]),
    db.query(
      `SELECT r.*, NULL::float AS distance_km FROM public_room_posts r
        WHERE r.owner_id = $1 ORDER BY r.first_published_at DESC LIMIT 10`, [userId]),
    viewerId && viewerId !== userId ? compareUsers(viewerId, userId) : Promise.resolve(null),
  ]);
  const scores = await scorePosts(viewerId, posts.rows.map((p) => p.post_id));

  return {
    ...base,
    isHidden: false,
    bio: u.bio,
    gender: u.gender,
    birthYear: u.birth_year,
    occupation: u.occupation,
    schoolOrWorkplace: u.school_or_workplace,
    memberSince: u.created_at,
    emailVerified: u.email_verified,
    habits: habits.rows.map((h) => ({ questionKey: h.question_key, label: h.option_label })),
    matchPercent: match ? match.overall : null,
    matchLevel: match ? match.level : null,
    activePosts: posts.rows.map((p) => toRoomCard(p, scores.get(p.post_id), false)),
  };
}

module.exports = {
  getRoomDetail, getRoomImages, getNearby, savePost, unsavePost, listSaved, logContactClick, getPublicProfile,
};
