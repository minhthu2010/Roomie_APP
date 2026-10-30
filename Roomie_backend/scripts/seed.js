const bcrypt = require('bcryptjs');
const { pool, withTransaction } = require('../src/db');
const { writeSampleImages } = require('./sampleImages');
const { importAdminUnits } = require('./importAdminUnits');

const IDS = {
  admin: '00000000-0000-0000-0000-000000000001',
  owner: '00000000-0000-0000-0000-000000000002',
  seeker: '00000000-0000-0000-0000-000000000003',
};

(async () => {
  const hash = await bcrypt.hash('Roomie@123', 10);

  await withTransaction(async (c) => {
    // 34 tỉnh/thành + 3.321 phường/xã thật (data/vn-admin-units.json)
    const admin = await importAdminUnits(c);
    console.log(`Hành chính: ${admin.provinces} tỉnh/thành, ${admin.wards} phường/xã`);

    const users = [
      [IDS.admin, 'admin@roomie.test', 'Quản trị Roomie', 'Admin'],
      [IDS.owner, 'ngocanh@roomie.test', 'Nguyễn Ngọc Anh', 'Ngọc Anh'],
      [IDS.seeker, 'mailinh@roomie.test', 'Trần Mai Linh', 'Mai Linh'],
    ];
    for (const [id, email, fullName, displayName] of users) {
      await c.query(
        `INSERT INTO users (user_id, email, password_hash, full_name, display_name, email_verified_at)
         VALUES ($1,$2,$3,$4,$5,NOW()) ON CONFLICT DO NOTHING`, [id, email, hash, fullName, displayName]);
      await c.query(`INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT DO NOTHING`, [id]);
      await c.query(`INSERT INTO user_profiles (user_id) VALUES ($1) ON CONFLICT DO NOTHING`, [id]);
      await c.query(`INSERT INTO user_roles (user_id, role_id)
                     SELECT $1, role_id FROM roles WHERE role_code = 'user' ON CONFLICT DO NOTHING`, [id]);
    }
    await c.query(`INSERT INTO user_roles (user_id, role_id)
                   SELECT $1, role_id FROM roles WHERE role_code = 'admin' ON CONFLICT DO NOTHING`, [IDS.admin]);

    const posts = [
      ['10000000-0000-0000-0000-000000000001', 'Phòng sáng thoáng, ở ghép cùng 2 bạn nữ', 'shared_room', 3200000, '27169', '268 Lý Thường Kiệt', 10.7725, 106.6580],
      ['10000000-0000-0000-0000-000000000002', 'Căn hộ có ban công gần ĐH Kinh tế', 'apartment', 3800000, '27139', 'Nguyễn Đình Chiểu', 10.7800, 106.6900],
      ['10000000-0000-0000-0000-000000000003', 'Studio đủ nội thất, vào ở ngay', 'studio', 4300000, '26944', 'Điện Biên Phủ', 10.8010, 106.7100],
    ];
        for (const [id, title, type, rent, ward, street, lat, lng] of posts) {
      await c.query(
        `INSERT INTO room_posts (post_id, owner_id, title, description, room_type, monthly_rent, area_m2,
            preferred_gender, contact_phone, province_code, ward_code, street_address, address_text,
            latitude, longitude, moderation_status, submitted_at, reviewed_by, reviewed_at,
            first_published_at, expires_at)
         VALUES ($1,$2,$3,'Phòng mẫu để test.',$4,$5,25,'female','0901234567','79',$6,$7,$11,
            $8,$9,'approved',NOW(),$10,NOW(),NOW(),NOW() + INTERVAL '30 days')
         ON CONFLICT DO NOTHING`,
        [id, IDS.owner, title, type, rent, ward, street, lat, lng, IDS.admin,
         `${street}, TP. Hồ Chí Minh`]);
    }

    // Thêm phòng để thử tìm kiếm / bộ lọc (khác quận, giá, loại phòng, giới tính)
    const morePosts = [
      ['10000000-0000-0000-0000-000000000004', 'Phòng riêng có gác, gần ĐH Khoa học Tự nhiên', 'private_room', 2800000, 'any', '27301', '280 An Dương Vương', 10.7615, 106.6825],
      ['10000000-0000-0000-0000-000000000005', 'Nhà nguyên căn yên tĩnh, cần thêm 1 bạn', 'whole_house', 4100000, 'male', '27058', 'Phan Xích Long', 10.7990, 106.6870],
      ['10000000-0000-0000-0000-000000000006', 'Ở ghép giá rẻ cho sinh viên, gần Bách Khoa', 'shared_room', 1900000, 'female', '27169', 'Tô Hiến Thành', 10.7760, 106.6630],
    ];
    for (const [id, title, type, rent, gender, ward, street, lat, lng] of morePosts) {
      await c.query(
        `INSERT INTO room_posts (post_id, owner_id, title, description, room_type, monthly_rent, area_m2,
            preferred_gender, contact_phone, province_code, ward_code, street_address, address_text,
            latitude, longitude, moderation_status, submitted_at, reviewed_by, reviewed_at,
            first_published_at, expires_at)
         VALUES ($1,$2,$3,'Phòng mẫu để test.',$4,$5,20,$6,'0901234567','79',$7,$8,$11,
            $9,$10,'approved',NOW(),$12,NOW(),NOW(),NOW() + INTERVAL '30 days')
         ON CONFLICT DO NOTHING`,
        [id, IDS.owner, title, type, rent, gender, ward, street, lat, lng, `${street}, TP. Hồ Chí Minh`, IDS.admin]);
    }

    // Landmark: gợi ý trường học + địa điểm lân cận (cần migration 002)
    await c.query(`INSERT INTO landmarks (landmark_type, name, short_name, subtitle, address_text, province_code, latitude, longitude) VALUES
      ('university', 'Trường Đại học Bách Khoa - ĐHQG TP.HCM', 'ĐH Bách Khoa', NULL, '268 Lý Thường Kiệt', '79', 10.7721, 106.6578),
      ('university', 'Trường Đại học Kinh tế TP.HCM', 'ĐH Kinh tế', NULL, '59C Nguyễn Đình Chiểu', '79', 10.7831, 106.6948),
      ('university', 'Trường Đại học Khoa học Tự nhiên - ĐHQG TP.HCM', 'ĐH KHTN', NULL, '227 Nguyễn Văn Cừ, Quận 5', '79', 10.7628, 106.6822),
      ('university', 'Trường Đại học Kiến trúc TP.HCM', 'ĐH Kiến trúc', NULL, '196 Pasteur, Quận 3', '79', 10.7834, 106.6955),
      ('supermarket', 'Co.op Mart Lý Thường Kiệt', NULL, 'Mở cửa đến 22:00', '497 Hòa Hảo', '79', 10.7612, 106.6601),
      ('bus_stop', 'Trạm xe buýt ĐH Bách Khoa', NULL, 'Tuyến 08, 59, 69', 'Lý Thường Kiệt', '79', 10.7730, 106.6590)
      ON CONFLICT DO NOTHING`);
    // Địa điểm lân cận cho màn Vị trí phòng (dữ liệu mẫu, tọa độ gần đúng)
    await c.query(`INSERT INTO landmarks (landmark_type, name, short_name, subtitle, address_text, province_code, latitude, longitude) VALUES
      ('hospital', 'Bệnh viện Nhân dân 115', 'BV 115', 'Cấp cứu 24/7', 'Thành Thái, Quận 10', '79', 10.7740, 106.6670),
      ('supermarket', 'Co.op Mart Nguyễn Đình Chiểu', NULL, 'Mở cửa đến 22:00', 'Nguyễn Đình Chiểu, Quận 3', '79', 10.7790, 106.6880),
      ('bus_stop', 'Trạm xe buýt Hồ Con Rùa', NULL, 'Tuyến 03, 19, 36', 'Công trường Quốc tế, Quận 3', '79', 10.7826, 106.6960),
      ('hospital', 'Bệnh viện Chợ Rẫy', NULL, 'Cấp cứu 24/7', 'Nguyễn Chí Thanh, Quận 5', '79', 10.7577, 106.6595),
      ('supermarket', 'Bách Hóa Xanh An Dương Vương', NULL, 'Mở cửa đến 21:30', 'An Dương Vương, Quận 5', '79', 10.7608, 106.6800),
      ('supermarket', 'Circle K Phan Xích Long', NULL, 'Mở cửa 24/7', 'Phan Xích Long, Phú Nhuận', '79', 10.7985, 106.6880),
      ('bus_stop', 'Trạm xe buýt Phan Đăng Lưu', NULL, 'Tuyến 24, 104', 'Phan Đăng Lưu, Phú Nhuận', '79', 10.8010, 106.6905),
      ('supermarket', 'WinMart+ Điện Biên Phủ', NULL, 'Mở cửa đến 22:00', 'Điện Biên Phủ, Bình Thạnh', '79', 10.8015, 106.7090),
      ('bus_stop', 'Trạm xe buýt Hàng Xanh', NULL, 'Tuyến 10, 52, 55', 'Ngã tư Hàng Xanh, Bình Thạnh', '79', 10.8019, 106.7116)
      ON CONFLICT DO NOTHING`);

    // Quận (hiển thị + lọc khu vực), trường gần nhất, tiện ích của các phòng mẫu.
    // Dùng UPDATE để chạy lại seed trên DB cũ vẫn bổ sung được. Không đổi address_text/giá...:
    // trigger trg_room_posts_material_reapproval sẽ đưa bài đã duyệt về 'pending'.
    const postDetails = [
      ['10000000-0000-0000-0000-000000000001', 'Quận 10', 'ĐH Bách Khoa', 'female', ['air_conditioning', 'washing_machine', 'kitchen']],
      ['10000000-0000-0000-0000-000000000002', 'Quận 3', 'ĐH Kinh tế', 'any', ['air_conditioning', 'kitchen', 'parking', 'balcony']],
      ['10000000-0000-0000-0000-000000000003', 'Bình Thạnh', null, 'any', ['air_conditioning', 'furnished', 'wifi', 'private_bathroom']],
      ['10000000-0000-0000-0000-000000000004', 'Quận 5', 'ĐH KHTN', 'any', ['wifi', 'parking']],
      ['10000000-0000-0000-0000-000000000005', 'Phú Nhuận', null, 'male', ['air_conditioning', 'washing_machine', 'kitchen', 'parking']],
      ['10000000-0000-0000-0000-000000000006', 'Quận 10', 'ĐH Bách Khoa', 'female', ['washing_machine', 'parking']],
    ];
    for (const [postId, district, landmark, gender, amenities] of postDetails) {
      await c.query(
        `UPDATE room_posts SET district_legacy = $2, preferred_gender = $4,
            landmark_id = (SELECT landmark_id FROM landmarks WHERE short_name = $3)
          WHERE post_id = $1`, [postId, district, landmark, gender]);
      await c.query(
        `INSERT INTO room_post_amenities (post_id, amenity_id)
         SELECT $1, amenity_id FROM amenities WHERE amenity_code = ANY($2::text[])
         ON CONFLICT DO NOTHING`, [postId, amenities]);
    }

    // Ảnh mẫu (vẽ bằng scripts/sampleImages.js vào uploads/seed). Thêm ảnh vào bài đã duyệt
    // sẽ kích hoạt trigger duyệt lại, nên tắt trigger đó trong transaction này.
    const CAPTIONS = ['Không gian phòng', 'Góc cửa sổ', 'Khu bếp & kệ đồ', 'Góc học tập'];
    await c.query('ALTER TABLE post_images DISABLE TRIGGER trg_post_images_material_reapproval');
    for (let i = 0; i < postDetails.length; i++) {
      const postId = postDetails[i][0];
      const urls = writeSampleImages(i, 4);
      for (let k = 0; k < urls.length; k++) {
        await c.query(
          `INSERT INTO post_images (post_id, image_url, display_order, is_cover, caption)
           SELECT $1, $2, $3, $4, $5
            WHERE NOT EXISTS (SELECT 1 FROM post_images WHERE post_id = $1 AND display_order = $3)`,
          [postId, urls[k], k + 1, k === 0, CAPTIONS[k]]);
      }
    }
    await c.query('ALTER TABLE post_images ENABLE TRIGGER trg_post_images_material_reapproval');

    // Nội dung chi tiết cho phòng mẫu (chỉ ghi đè mô tả mặc định). Các cột này là "material"
    // nên cũng tắt trigger duyệt lại để bài mẫu vẫn ở trạng thái đã duyệt.
    const details = {
      '10000000-0000-0000-0000-000000000001': ['Phòng nhiều ánh sáng, có ban công và khu bếp riêng. Phòng hiện có 2 bạn nữ là sinh viên Bách Khoa, cần thêm 1 bạn.', 3200000, 'Điện 3.500đ/kWh, nước 100.000đ/người, wifi + rác chia đều', 2, 'Ưu tiên bạn gọn gàng, tôn trọng giờ nghỉ sau 23:00 và không hút thuốc.'],
      '10000000-0000-0000-0000-000000000002': ['Căn hộ 2 phòng ngủ, ban công thoáng, cách ĐH Kinh tế 5 phút đi bộ. Có thang máy và bảo vệ.', 3800000, 'Điện nước theo đồng hồ, phí quản lý 150.000đ/tháng', 1, 'Mong bạn thân thiện, có thể nấu ăn chung cuối tuần.'],
      '10000000-0000-0000-0000-000000000003': ['Studio đủ nội thất: giường, tủ, bàn làm việc, máy lạnh. Vào ở ngay, phù hợp người đi làm.', 4300000, 'Điện 4.000đ/kWh, nước khoán 80.000đ', 0, 'Phù hợp người đi làm, giờ giấc linh hoạt.'],
      '10000000-0000-0000-0000-000000000004': ['Phòng riêng có gác lửng, cửa sổ lớn, gần ĐH Khoa học Tự nhiên và chợ.', 2800000, 'Điện 3.800đ/kWh, nước 70.000đ/người', 1, 'Ưu tiên sinh viên, yên tĩnh buổi tối.'],
      '10000000-0000-0000-0000-000000000005': ['Nhà nguyên căn 3 phòng trong hẻm yên tĩnh, đang có 2 bạn nam đi làm, cần thêm 1 bạn.', 4100000, 'Chia đều theo số người, khoảng 300.000đ/tháng', 2, 'Không hút thuốc trong nhà, dọn dẹp theo lịch trực.'],
      '10000000-0000-0000-0000-000000000006': ['Phòng ở ghép 4 người, giá rẻ cho sinh viên, đi bộ 7 phút tới Bách Khoa.', 1900000, 'Điện nước khoán 150.000đ/người', 3, 'Mong bạn nữ sinh viên, hòa đồng và giữ vệ sinh chung.'],
    };
    await c.query('ALTER TABLE room_posts DISABLE TRIGGER trg_room_posts_material_reapproval');
    // Phường thật của phòng mẫu (tra từ tọa độ); DB seed cũ đang gắn phường test DEV00x
    await c.query(
      `UPDATE room_posts rp SET ward_code = v.ward_code
         FROM (VALUES ('10000000-0000-0000-0000-000000000001'::uuid, '27169'),
                      ('10000000-0000-0000-0000-000000000002'::uuid, '27139'),
                      ('10000000-0000-0000-0000-000000000003'::uuid, '26944'),
                      ('10000000-0000-0000-0000-000000000004'::uuid, '27301'),
                      ('10000000-0000-0000-0000-000000000005'::uuid, '27058'),
                      ('10000000-0000-0000-0000-000000000006'::uuid, '27169')) AS v(post_id, ward_code)
        WHERE rp.post_id = v.post_id AND rp.ward_code IS DISTINCT FROM v.ward_code`);
    for (const [postId, [desc, deposit, bills, occupants, expectations]] of Object.entries(details)) {
      await c.query(
        `UPDATE room_posts SET description = $2, deposit_amount = $3, utility_cost_note = $4,
                current_occupants = $5, roommate_expectations = $6,
                available_from = CURRENT_DATE + 14, smoking_allowed = FALSE,
                preferred_occupation = CASE WHEN post_id = '10000000-0000-0000-0000-000000000003' THEN 'worker' ELSE 'student' END
          WHERE post_id = $1 AND description = 'Phòng mẫu để test.'`,
        [postId, desc, deposit, bills, occupants, expectations]);
    }
    await c.query('ALTER TABLE room_posts ENABLE TRIGGER trg_room_posts_material_reapproval');

    // Hồ sơ người đăng (Ngọc Anh) và người tìm (Mai Linh) cho màn hồ sơ công khai
    await c.query(
      `UPDATE user_profiles SET bio = $2, gender = 'female', birth_year = 2003, occupation = 'student',
              school_or_workplace = 'ĐH Bách Khoa TP.HCM'
        WHERE user_id = $1 AND bio IS NULL`,
      [IDS.owner, 'Mình là sinh viên năm 3, thích nấu ăn và giữ phòng gọn gàng. Mong tìm bạn ở ghép hòa đồng, tôn trọng giờ nghỉ.']);
    await c.query(
      `UPDATE user_profiles SET gender = 'female', birth_year = 2004, occupation = 'student',
              school_or_workplace = 'ĐH Kinh tế TP.HCM'
        WHERE user_id = $1 AND birth_year IS NULL`, [IDS.seeker]);

    // Câu trả lời bài test cho bộ câu hỏi đang phát hành (v2, 13 câu): question_key -> option_key.
    // Seeker gần giống phòng 1, khác nhiều phòng 3 để thấy điểm % chênh lệch.
    const userAnswers = {
      [IDS.seeker]: {
        sleep_time: 'before_23', wake_time: 'from_7_to_9', noise: 'moderate',
        cleanliness: 'daily', chores: 'schedule', cooking: 'few_times_week',
        smoking: 'no_smoke_free', pets: 'ok_if_clean',
        bills_split: 'equal', air_conditioning: 'night_only',
        guests: 'notify_first', overnight_guests: 'notify_rarely', communication: 'friendly_private',
      },
      [IDS.owner]: {
        sleep_time: 'from_23_to_24', wake_time: 'before_7', noise: 'moderate',
        cleanliness: 'daily', chores: 'schedule', cooking: 'daily',
        smoking: 'no_outside_ok', pets: 'no_pets',
        bills_split: 'by_usage', air_conditioning: 'night_only',
        guests: 'notify_first', overnight_guests: 'not_allowed', communication: 'close',
      },
    };
    const postAnswers = {
      '10000000-0000-0000-0000-000000000001': {
        sleep_time: 'before_23', wake_time: 'from_7_to_9', noise: 'very_quiet',
        cleanliness: 'daily', chores: 'schedule', cooking: 'daily',
        smoking: 'no_smoke_free', pets: 'ok_if_clean',
        bills_split: 'equal', air_conditioning: 'night_only',
        guests: 'notify_first', overnight_guests: 'notify_rarely', communication: 'friendly_private',
      },
      '10000000-0000-0000-0000-000000000002': {
        sleep_time: 'from_23_to_24', wake_time: 'from_7_to_9', noise: 'moderate',
        cleanliness: 'few_times_week', chores: 'self_aware', cooking: 'few_times_week',
        smoking: 'no_outside_ok', pets: 'ok_if_clean',
        bills_split: 'flexible', air_conditioning: 'all_day',
        guests: 'social', overnight_guests: 'notify_rarely', communication: 'close',
      },
      '10000000-0000-0000-0000-000000000003': {
        sleep_time: 'after_24', wake_time: 'after_9', noise: 'tolerant',
        cleanliness: 'relaxed', chores: 'hire_cleaner', cooking: 'rarely',
        smoking: 'smoker_allowed_areas', pets: 'has_pet',
        bills_split: 'by_usage', air_conditioning: 'all_day',
        guests: 'social', overnight_guests: 'comfortable', communication: 'private',
      },
      '10000000-0000-0000-0000-000000000004': {
        sleep_time: 'from_23_to_24', wake_time: 'from_7_to_9', noise: 'moderate',
        cleanliness: 'few_times_week', chores: 'self_aware', cooking: 'few_times_week',
        smoking: 'no_outside_ok', pets: 'no_pets',
        bills_split: 'equal', air_conditioning: 'night_only',
        guests: 'notify_first', overnight_guests: 'not_allowed', communication: 'friendly_private',
      },
      '10000000-0000-0000-0000-000000000005': {
        sleep_time: 'before_23', wake_time: 'before_7', noise: 'very_quiet',
        cleanliness: 'daily', chores: 'schedule', cooking: 'daily',
        smoking: 'no_smoke_free', pets: 'no_pets',
        bills_split: 'by_usage', air_conditioning: 'rarely',
        guests: 'limited', overnight_guests: 'not_allowed', communication: 'private',
      },
      '10000000-0000-0000-0000-000000000006': {
        sleep_time: 'from_23_to_24', wake_time: 'from_7_to_9', noise: 'moderate',
        cleanliness: 'daily', chores: 'schedule', cooking: 'few_times_week',
        smoking: 'no_smoke_free', pets: 'ok_if_clean',
        bills_split: 'equal', air_conditioning: 'night_only',
        guests: 'notify_first', overnight_guests: 'notify_rarely', communication: 'friendly_private',
      },
    };
    const answerSql = (table, ownerColumn) => `
      INSERT INTO ${table} (${ownerColumn}, questionnaire_version_id, question_id, answer_value)
      SELECT $1, q.questionnaire_version_id, q.question_id, jsonb_build_object('option_key', $3::text)
        FROM lifestyle_questions q
        JOIN questionnaire_versions v ON v.questionnaire_version_id = q.questionnaire_version_id
       WHERE v.status = 'published' AND q.question_key = $2
      ON CONFLICT DO NOTHING`;
    for (const [userId, answers] of Object.entries(userAnswers)) {
      for (const [questionKey, optionKey] of Object.entries(answers)) {
        await c.query(answerSql('user_lifestyle_answers', 'user_id'), [userId, questionKey, optionKey]);
      }
      // Trả lời đủ rồi nên đánh dấu hoàn thành (kể cả khi migration 003 đã tạo tiến độ dở dang)
      await c.query(`INSERT INTO user_lifestyle_test_progress (user_id, questionnaire_version_id, current_step, is_completed, completed_at)
                     SELECT $1, questionnaire_version_id, $2, TRUE, NOW() FROM questionnaire_versions WHERE status = 'published'
                     ON CONFLICT (user_id, questionnaire_version_id) DO UPDATE SET
                       current_step = EXCLUDED.current_step, is_completed = TRUE,
                       completed_at = COALESCE(user_lifestyle_test_progress.completed_at, NOW())`,
                    [userId, Object.keys(answers).length]);
    }
    for (const [postId, answers] of Object.entries(postAnswers)) {
      for (const [questionKey, optionKey] of Object.entries(answers)) {
        await c.query(answerSql('post_lifestyle_answers', 'post_id'), [postId, questionKey, optionKey]);
      }
    }
  });

  console.log('Seed xong. Đăng nhập test: *@roomie.test / Roomie@123');
  await pool.end();
})().catch((err) => { console.error(err); process.exit(1); });