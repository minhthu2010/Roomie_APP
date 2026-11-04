// Phòng mẫu thêm để test: 18 phòng gần các trường đại học thật ở TP.HCM, Hà Nội, Đà Nẵng, Cần Thơ, Huế
// và 5 chủ phòng mẫu (host1..host5@roomie.test, cùng mật khẩu với tài khoản test khác).
// Tọa độ = tọa độ trường thật (OpenStreetMap) dời 300-900 m; phường tra ngược từ tọa độ (Photon), mã phường thật.
// Chỉ dùng cho môi trường phát triển (seed.js gọi), không chạy trên production.
const { writeSampleImages } = require('./sampleImages');

const HOSTS = [
  // id, email, họ tên, tên hiển thị, giới tính, năm sinh, nghề, tỉnh, giới thiệu, thói quen
  ['00000000-0000-0000-0000-000000000011', 'host1@roomie.test', 'Lê Minh Khoa', 'Minh Khoa', 'male', 1998, 'worker', '79',
    'Mình làm IT ở Thủ Đức, thích yên tĩnh buổi tối, cuối tuần hay đá bóng.', 'quiet'],
  ['00000000-0000-0000-0000-000000000012', 'host2@roomie.test', 'Phạm Thu Hà', 'Thu Hà', 'female', 2003, 'student', '01',
    'Sinh viên năm 3 ở Hà Nội, thích nấu ăn, giữ phòng sạch sẽ.', 'tidy'],
  ['00000000-0000-0000-0000-000000000013', 'host3@roomie.test', 'Võ Thanh Tâm', 'Thanh Tâm', 'male', 2001, 'student', '48',
    'Mình học Bách khoa Đà Nẵng, hòa đồng, hay mời bạn về chơi.', 'social'],
  ['00000000-0000-0000-0000-000000000014', 'host4@roomie.test', 'Nguyễn Bảo Ngọc', 'Bảo Ngọc', 'female', 1999, 'worker', '79',
    'Nhân viên văn phòng quận 1, đi làm giờ hành chính, ngủ sớm.', 'tidy'],
  ['00000000-0000-0000-0000-000000000015', 'host5@roomie.test', 'Trần Quốc Huy', 'Quốc Huy', 'male', 2002, 'student', '92',
    'Sinh viên Đại học Cần Thơ, dễ tính, thường thức khuya học bài.', 'night'],
];

// Nếp sinh hoạt mẫu (option_key theo bộ câu hỏi đang phát hành)
const LIFESTYLES = {
  quiet: { sleep_time: 'before_23', wake_time: 'before_7', noise: 'very_quiet', cleanliness: 'daily',
    chores: 'schedule', cooking: 'few_times_week', smoking: 'no_smoke_free', pets: 'no_pets', bills_split: 'by_usage',
    air_conditioning: 'night_only', guests: 'limited', overnight_guests: 'not_allowed', communication: 'private' },
  tidy: { sleep_time: 'before_23', wake_time: 'from_7_to_9', noise: 'moderate', cleanliness: 'daily',
    chores: 'schedule', cooking: 'daily', smoking: 'no_smoke_free', pets: 'ok_if_clean', bills_split: 'equal',
    air_conditioning: 'night_only', guests: 'notify_first', overnight_guests: 'notify_rarely', communication: 'friendly_private' },
  social: { sleep_time: 'from_23_to_24', wake_time: 'from_7_to_9', noise: 'tolerant', cleanliness: 'few_times_week',
    chores: 'self_aware', cooking: 'few_times_week', smoking: 'no_outside_ok', pets: 'ok_if_clean', bills_split: 'flexible',
    air_conditioning: 'all_day', guests: 'social', overnight_guests: 'comfortable', communication: 'close' },
  night: { sleep_time: 'after_24', wake_time: 'after_9', noise: 'moderate', cleanliness: 'few_times_week',
    chores: 'self_aware', cooking: 'rarely', smoking: 'no_outside_ok', pets: 'has_pet', bills_split: 'equal',
    air_conditioning: 'all_day', guests: 'notify_first', overnight_guests: 'notify_rarely', communication: 'friendly_private' },
  relaxed: { sleep_time: 'from_23_to_24', wake_time: 'after_9', noise: 'tolerant', cleanliness: 'relaxed',
    chores: 'hire_cleaner', cooking: 'rarely', smoking: 'smoker_allowed_areas', pets: 'has_pet', bills_split: 'by_usage',
    air_conditioning: 'all_day', guests: 'social', overnight_guests: 'comfortable', communication: 'private' },
};

// host (1-5), tỉnh, mã phường, đường, vĩ độ, kinh độ, tiêu đề, loại, giá, cọc, m², số ngày đến khi vào ở,
// đang ở, cần tìm, giới tính, đối tượng, tiện ích, nếp sinh hoạt, mô tả, chi phí, mong muốn
const ROOMS = [
  [1, '79', '25951', 'Đường Thomas Edison, Làng Đại học', 10.883071, 106.804027,
    'Phòng ở ghép gần ĐH Quốc tế, có gác', 'shared_room', 1600000, 1600000, 22, 0, 2, 2, 'male', 'student',
    ['wifi', 'parking', 'window'], 'social', 'Phòng ở ghép trong khu Làng Đại học, đi bộ 7 phút tới ĐH Quốc tế. Có gác lửng, cửa sổ thoáng.',
    'Điện 3.500đ/kWh, nước 80.000đ/người, wifi 50.000đ', 'Ưu tiên bạn nam sinh viên, hòa đồng, giữ vệ sinh chung.'],
  [1, '79', '26890', 'Thiên Hộ Dương, Gò Vấp', 10.819289, 106.692064,
    'Phòng riêng mới xây gần ĐH Công nghiệp', 'private_room', 3000000, 3000000, 20, 7, 0, 1, 'any', 'any',
    ['air_conditioning', 'private_bathroom', 'wifi', 'parking'], 'quiet', 'Phòng riêng có WC trong phòng, máy lạnh mới, cách ĐH Công nghiệp 5 phút xe máy.',
    'Điện 4.000đ/kWh, nước 100.000đ/người', 'Ở một mình hoặc ở chung bạn bè, giữ yên tĩnh sau 22:00.'],
  [4, '79', '27475', 'Đường số 12, Tân Hưng', 10.736295, 106.701736,
    'Căn hộ 2PN gần ĐH Tôn Đức Thắng, cần 1 bạn nữ', 'apartment', 4200000, 4200000, 35, 14, 1, 1, 'female', 'any',
    ['air_conditioning', 'washing_machine', 'kitchen', 'refrigerator', 'elevator', 'security', 'balcony'], 'tidy',
    'Căn hộ chung cư 2 phòng ngủ, mình ở một phòng, cho thuê phòng còn lại. Có bảo vệ 24/7, hồ bơi.',
    'Phí quản lý 200.000đ, điện nước theo đồng hồ chia đôi', 'Bạn nữ đi làm hoặc sinh viên năm cuối, sạch sẽ, không hút thuốc.'],
  [4, '79', '26758', 'Hẻm 35 Trần Đình Xu, Cầu Ông Lãnh', 10.759225, 106.690539,
    'Studio trung tâm quận 1, gần ĐH Văn Lang', 'studio', 5500000, 5500000, 25, 0, 0, 1, 'any', 'worker',
    ['air_conditioning', 'furnished', 'private_bathroom', 'wifi', 'refrigerator', 'window'], 'quiet',
    'Studio đủ nội thất, cửa sổ lớn, đi bộ ra chợ Bến Thành 10 phút. Phù hợp người đi làm.',
    'Điện 4.000đ/kWh, nước khoán 100.000đ, wifi miễn phí', 'Người đi làm, giờ giấc ổn định.'],
  [1, '79', '27094', 'Lương Định Của, An Khánh', 10.77318, 106.710274,
    'Phòng riêng có ban công view sông, Thủ Thiêm', 'private_room', 4500000, 9000000, 28, 30, 1, 1, 'any', 'worker',
    ['air_conditioning', 'balcony', 'washing_machine', 'wifi', 'security'], 'quiet',
    'Phòng trong nhà phố mới, ban công nhìn ra sông Sài Gòn. Khu yên tĩnh, an ninh.',
    'Điện nước theo đồng hồ, phí rác 30.000đ', 'Người đi làm, không hút thuốc trong nhà.'],
  [4, '79', '26740', 'Hàn Thuyên, Sài Gòn', 10.778534, 106.697615,
    'Ở ghép nữ gần ĐH Kinh tế cơ sở A', 'shared_room', 2300000, 2300000, 30, 0, 2, 1, 'female', 'student',
    ['air_conditioning', 'washing_machine', 'kitchen', 'wifi'], 'tidy',
    'Phòng ở ghép 3 bạn nữ, đang có 2 bạn sinh viên Kinh tế. Gần nhà thờ Đức Bà, đi bộ tới trường.',
    'Chia đều điện nước, khoảng 250.000đ/người', 'Bạn nữ sinh viên, gọn gàng, ngủ trước 24:00.'],
  [4, '79', '27190', 'Hẻm 68 Hồ Thị Kỷ, Vườn Lài', 10.76507, 106.677225,
    'Phòng riêng giá rẻ gần ĐH Sư phạm', 'private_room', 2500000, 1000000, 16, 0, 0, 1, 'female', 'student',
    ['wifi', 'parking', 'window'], 'tidy', 'Phòng nhỏ gọn trong hẻm yên tĩnh, gần chợ hoa Hồ Thị Kỷ, cách ĐH Sư phạm 5 phút đi bộ.',
    'Điện 3.800đ/kWh, nước 70.000đ', 'Ưu tiên nữ sinh viên.'],
  [1, '79', '26842', 'Đường D1, Tăng Nhơn Phú', 10.837825, 106.813699,
    'Nhà nguyên căn 3PN gần ĐH FPT, tìm 2 bạn', 'whole_house', 3500000, 3500000, 60, 0, 1, 2, 'male', 'any',
    ['parking', 'kitchen', 'washing_machine', 'wifi', 'refrigerator'], 'social',
    'Nhà 1 trệt 1 lầu, 3 phòng ngủ, sân để xe rộng. Đang có 1 bạn, cần thêm 2 bạn nam (giá mỗi người).',
    'Chia đều theo đầu người, khoảng 300.000đ/tháng', 'Bạn nam hòa đồng, cuối tuần hay nấu ăn chung.'],
  [1, '79', '26800', 'Đỗ Mười, Linh Xuân', 10.866972, 106.787092,
    'Phòng trọ gần ĐH Nông Lâm, có chỗ nấu ăn', 'private_room', 1800000, 1000000, 18, 0, 0, 1, 'any', 'student',
    ['kitchen', 'parking', 'wifi'], 'night', 'Phòng trọ trong dãy 10 phòng, có kệ bếp riêng, giờ giấc tự do.',
    'Điện 3.500đ/kWh, nước 60.000đ', 'Sinh viên, không làm ồn sau 23:00.'],
  [4, '79', '26758', 'Hẻm 69 Đề Thám, Cầu Ông Lãnh', 10.763804, 106.694984,
    'Phòng có gác gần ĐH Mở, khu phố Tây', 'private_room', 3800000, 3800000, 24, 7, 1, 1, 'any', 'any',
    ['air_conditioning', 'wifi', 'washing_machine', 'window'], 'relaxed',
    'Phòng có gác lửng, gần phố đi bộ Bùi Viện. Khu sôi động, phù hợp người thích đi chơi tối.',
    'Điện 4.000đ/kWh, nước 100.000đ', 'Thoải mái giờ giấc, tôn trọng không gian riêng.'],
  [2, '01', '00256', 'Đường Đại Cồ Việt, Hai Bà Trưng', 21.008861, 105.842646,
    'Ở ghép nữ gần Bách khoa Hà Nội', 'shared_room', 1900000, 1900000, 25, 0, 2, 1, 'female', 'student',
    ['air_conditioning', 'washing_machine', 'wifi', 'kitchen'], 'tidy',
    'Phòng 25m² ở ghép 3 bạn nữ, cách cổng Parabol Bách khoa 5 phút đi bộ.',
    'Điện 3.800đ/kWh, nước 100.000đ/người', 'Bạn nữ sinh viên, sạch sẽ, không mang bạn nam qua đêm.'],
  [2, '01', '00166', 'Ngõ 57 Trần Quốc Vượng, Cầu Giấy', 21.035298, 105.7858,
    'Phòng riêng khép kín gần ĐH Quốc gia Hà Nội', 'private_room', 3200000, 3200000, 20, 14, 0, 1, 'any', 'student',
    ['air_conditioning', 'private_bathroom', 'wifi', 'refrigerator'], 'quiet',
    'Phòng khép kín, có nóng lạnh, gần ĐH Quốc gia và ĐH Sư phạm Hà Nội.',
    'Điện 4.000đ/kWh, nước 30.000đ/khối', 'Sinh viên yên tĩnh.'],
  [2, '01', '00199', 'Ngõ 54 Nguyễn Chí Thanh, Láng', 21.025281, 105.809514,
    'Căn hộ mini gần ĐH Ngoại thương', 'studio', 4800000, 4800000, 30, 0, 0, 1, 'any', 'any',
    ['air_conditioning', 'furnished', 'private_bathroom', 'washing_machine', 'elevator', 'wifi'], 'quiet',
    'Căn hộ mini có thang máy, đủ đồ, đi bộ tới Ngoại thương 10 phút.',
    'Dịch vụ 150.000đ, điện 4.000đ/kWh', 'Người đi làm hoặc sinh viên năm cuối.'],
  [2, '01', '00229', 'Phố Khương Thượng, Kim Liên', 21.006315, 105.825224,
    'Phòng ở ghép giá rẻ gần Học viện Ngân hàng', 'shared_room', 1500000, 1500000, 18, 0, 1, 1, 'any', 'student',
    ['wifi', 'washing_machine'], 'night', 'Phòng ở ghép 2 người, đang có 1 bạn học Học viện Ngân hàng.',
    'Chia đôi điện nước', 'Bạn sinh viên, dễ tính.'],
  [3, '48', '20200', 'Đường Đinh Đức Thiện, Hòa Khánh', 16.079022, 108.15591,
    'Phòng trọ gần Bách khoa Đà Nẵng', 'private_room', 1700000, 1000000, 20, 0, 0, 1, 'any', 'student',
    ['wifi', 'parking', 'window'], 'social', 'Phòng trọ mới, cách ĐH Bách khoa Đà Nẵng 5 phút xe đạp. Gần biển Nguyễn Tất Thành.',
    'Điện 3.500đ/kWh, nước 50.000đ', 'Sinh viên, hòa đồng.'],
  [3, '48', '20285', 'Đường Mỹ Đa Đông 8, Ngũ Hành Sơn', 16.044371, 108.243611,
    'Nhà nguyên căn gần biển, gần ĐH Kinh tế Đà Nẵng', 'whole_house', 3000000, 3000000, 70, 7, 1, 2, 'any', 'any',
    ['kitchen', 'parking', 'washing_machine', 'refrigerator', 'balcony', 'wifi'], 'social',
    'Nhà 2 tầng cách biển Mỹ Khê 800m, đang có 1 người, tìm thêm 2 bạn (giá mỗi người).',
    'Chia đều điện nước wifi', 'Thích biển, hòa đồng, giữ nhà sạch.'],
  [5, '92', '31147', 'Hẻm 169 Đường 3 Tháng 2, Tân An', 10.019505, 105.762844,
    'Phòng trọ gần Đại học Cần Thơ', 'private_room', 1400000, 700000, 18, 0, 0, 1, 'any', 'student',
    ['wifi', 'parking'], 'night', 'Phòng trọ trong hẻm, đi bộ tới Đại học Cần Thơ. Giờ giấc tự do.',
    'Điện 3.500đ/kWh, nước 50.000đ', 'Sinh viên.'],
  [3, '46', '19789', 'Kiệt 14 Nguyễn Công Trứ, Thuận Hóa', 16.47071, 107.595665,
    'Phòng ở ghép gần ĐH Sư phạm Huế', 'shared_room', 1200000, 1200000, 22, 14, 1, 1, 'female', 'student',
    ['wifi', 'kitchen', 'window'], 'tidy', 'Phòng ở ghép 2 bạn nữ, gần sông Hương và ĐH Sư phạm Huế.',
    'Chia đôi điện nước', 'Bạn nữ sinh viên, ngăn nắp.'],
];

// Trường làm gốc vị trí của từng phòng (tên đúng như trong landmarks), cùng thứ tự với ROOMS
const SCHOOLS = [
  "Trường Đại học Quốc tế",
  "Trường Đại học Công nghiệp Thành phố Hồ Chí Minh",
  "Trường Đại học Tôn Đức Thắng",
  "Đại học Văn Lang",
  "Trường Đại Học Ngân Hàng",
  "Đại học Kinh tế Thành phố Hồ Chí Minh - Cơ sở A",
  "Trường Đại học Sư phạm Thành phố Hồ Chí Minh",
  "Trường Đại học FPT Thành phố Hồ Chí Minh",
  "Trường Đại học Nông Lâm Thành phố Hồ Chí Minh",
  "Trường Đại học Mở Thành phố Hồ Chí Minh",
  "Đại học Bách khoa Hà Nội",
  "Đại học Quốc gia Hà Nội",
  "Trường Đại học Ngoại thương",
  "Học viện Ngân hàng",
  "Trường Đại học Bách khoa - Đại học Đà Nẵng",
  "Trường Đại học Kinh tế - Đại học Đà Nẵng",
  "Trường Đại học Cần Thơ Khu 1",
  "Trường Đại học Sư phạm - Đại học Huế",
];

const CAPTIONS = ['Không gian phòng', 'Góc cửa sổ', 'Khu bếp & kệ đồ', 'Góc học tập'];

/** Thêm chủ phòng + phòng mẫu (chạy lại nhiều lần được). c: client trong transaction của seed.js */
async function seedSampleRooms(c, { passwordHash, adminId }) {
  for (const [id, email, fullName, displayName, gender, birthYear, occupation, province, bio, style] of HOSTS) {
    await c.query(
      `INSERT INTO users (user_id, email, password_hash, full_name, display_name, email_verified_at)
       VALUES ($1,$2,$3,$4,$5,NOW()) ON CONFLICT DO NOTHING`, [id, email, passwordHash, fullName, displayName]);
    await c.query('INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [id]);
    await c.query(
      `INSERT INTO user_profiles (user_id, gender, birth_year, occupation, bio, living_province_code, profile_completed_at)
       VALUES ($1,$2,$3,$4,$5,$6,NOW()) ON CONFLICT (user_id) DO NOTHING`,
      [id, gender, birthYear, occupation, bio, province]);
    await c.query(`INSERT INTO user_roles (user_id, role_id) SELECT $1, role_id FROM roles WHERE role_code = 'user'
                   ON CONFLICT DO NOTHING`, [id]);
    await insertAnswers(c, 'user_lifestyle_answers', 'user_id', id, LIFESTYLES[style]);
    await c.query(`INSERT INTO user_lifestyle_test_progress (user_id, questionnaire_version_id, current_step, is_completed, completed_at)
                   SELECT $1, questionnaire_version_id, 13, TRUE, NOW() FROM questionnaire_versions WHERE status = 'published'
                   ON CONFLICT (user_id, questionnaire_version_id) DO NOTHING`, [id]);
  }

  // Ảnh / nội dung của bài đã duyệt là "material": tắt trigger duyệt lại để bài mẫu vẫn ở trạng thái đã duyệt
  await c.query('ALTER TABLE room_posts DISABLE TRIGGER trg_room_posts_material_reapproval');
  await c.query('ALTER TABLE post_images DISABLE TRIGGER trg_post_images_material_reapproval');
  for (let i = 0; i < ROOMS.length; i++) {
    const [host, province, ward, street, lat, lng, title, type, rent, deposit, area, days, occupants, slots,
      gender, occupation, amenities, style, description, bills, expectations] = ROOMS[i];
    const postId = `10000000-0000-0000-0000-${String(i + 7).padStart(12, '0')}`;
    const ownerId = HOSTS[host - 1][0];
    const { rows: names } = await c.query(
      `SELECT w.ward_name, p.province_name FROM wards w JOIN provinces p USING (province_code)
        WHERE w.ward_code = $1`, [ward]);
    const addressText = `${street}, ${names[0].ward_name}, ${names[0].province_name}`;
    await c.query(
      `INSERT INTO room_posts (post_id, owner_id, title, description, room_type, monthly_rent, deposit_amount, area_m2,
          utility_cost_note, available_from, current_occupants, seeking_roommates, preferred_gender, preferred_occupation,
          smoking_allowed, roommate_expectations, contact_phone, province_code, ward_code, street_address, address_text,
          latitude, longitude, moderation_status, submitted_at, reviewed_by, reviewed_at, first_published_at, expires_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,CURRENT_DATE + $10::int,$11,$12,$13,$14,FALSE,$15,$16,$17,$18,$19,$20,
               $21,$22,'approved',NOW(),$23,NOW(),NOW() - make_interval(days => $24::int),NOW() + INTERVAL '30 days')
       ON CONFLICT (post_id) DO NOTHING`,
      [postId, ownerId, title, description, type, rent, deposit, area, bills, days, occupants, slots, gender,
       occupation, expectations, `09${String(10000000 + i * 1371).slice(0, 8)}`, province, ward, street, addressText,
       lat, lng, adminId, i % 9]);
    // Trường làm gốc vị trí (khớp tiêu đề "gần ĐH ..."); trùng tên nhiều cơ sở thì lấy cơ sở gần phòng nhất
    await c.query(
      `UPDATE room_posts rp SET landmark_id = (
         SELECT l.landmark_id FROM landmarks l
          WHERE l.is_active AND l.landmark_type = 'university' AND l.name = $2 AND l.province_code = rp.province_code
          ORDER BY (l.latitude - rp.latitude) ^ 2 + (l.longitude - rp.longitude) ^ 2 LIMIT 1)
        WHERE rp.post_id = $1`, [postId, SCHOOLS[i]]);
    await c.query(
      `INSERT INTO room_post_amenities (post_id, amenity_id)
       SELECT $1, amenity_id FROM amenities WHERE amenity_code = ANY($2::text[]) ON CONFLICT DO NOTHING`, [postId, amenities]);
    const urls = writeSampleImages(i + 6, 3 + (i % 2));
    for (let k = 0; k < urls.length; k++) {
      await c.query(
        `INSERT INTO post_images (post_id, image_url, display_order, is_cover, caption)
         SELECT $1, $2, $3, $4, $5 WHERE NOT EXISTS (SELECT 1 FROM post_images WHERE post_id = $1 AND display_order = $3)`,
        [postId, urls[k], k + 1, k === 0, CAPTIONS[k]]);
    }
    await insertAnswers(c, 'post_lifestyle_answers', 'post_id', postId, LIFESTYLES[style]);
  }
  await c.query('ALTER TABLE post_images ENABLE TRIGGER trg_post_images_material_reapproval');
  await c.query('ALTER TABLE room_posts ENABLE TRIGGER trg_room_posts_material_reapproval');
  return { hosts: HOSTS.length, rooms: ROOMS.length };
}

async function insertAnswers(c, table, ownerColumn, ownerId, answers) {
  for (const [questionKey, optionKey] of Object.entries(answers)) {
    await c.query(
      `INSERT INTO ${table} (${ownerColumn}, questionnaire_version_id, question_id, answer_value)
       SELECT $1, q.questionnaire_version_id, q.question_id, jsonb_build_object('option_key', $3::text)
         FROM lifestyle_questions q
         JOIN questionnaire_versions v ON v.questionnaire_version_id = q.questionnaire_version_id
        WHERE v.status = 'published' AND q.question_key = $2
       ON CONFLICT DO NOTHING`, [ownerId, questionKey, optionKey]);
  }
}

module.exports = { seedSampleRooms };
