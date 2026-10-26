const bcrypt = require('bcryptjs');
const { pool, withTransaction } = require('../src/db');

const IDS = {
  admin: '00000000-0000-0000-0000-000000000001',
  owner: '00000000-0000-0000-0000-000000000002',
  seeker: '00000000-0000-0000-0000-000000000003',
};

(async () => {
  const hash = await bcrypt.hash('Roomie@123', 10);

  await withTransaction(async (c) => {
    await c.query(`INSERT INTO wards (ward_code, province_code, ward_name) VALUES
      ('DEV001','79','Phường Diên Hồng'), ('DEV002','79','Phường Bàn Cờ'), ('DEV003','79','Phường Gia Định')
      ON CONFLICT DO NOTHING`);

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
      ['10000000-0000-0000-0000-000000000001', 'Phòng sáng thoáng, ở ghép cùng 2 bạn nữ', 'shared_room', 3200000, 'DEV001', '268 Lý Thường Kiệt', 10.7725, 106.6580],
      ['10000000-0000-0000-0000-000000000002', 'Căn hộ có ban công gần ĐH Kinh tế', 'apartment', 3800000, 'DEV002', 'Nguyễn Đình Chiểu', 10.7800, 106.6900],
      ['10000000-0000-0000-0000-000000000003', 'Studio đủ nội thất, vào ở ngay', 'studio', 4300000, 'DEV003', 'Điện Biên Phủ', 10.8010, 106.7100],
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

    // Landmark: gợi ý trường học + địa điểm lân cận (cần migration 002)
    await c.query(`INSERT INTO landmarks (landmark_type, name, short_name, subtitle, address_text, province_code, latitude, longitude) VALUES
      ('university', 'Trường Đại học Bách Khoa - ĐHQG TP.HCM', 'ĐH Bách Khoa', NULL, '268 Lý Thường Kiệt', '79', 10.7721, 106.6578),
      ('university', 'Trường Đại học Kinh tế TP.HCM', 'ĐH Kinh tế', NULL, '59C Nguyễn Đình Chiểu', '79', 10.7831, 106.6948),
      ('supermarket', 'Co.op Mart Lý Thường Kiệt', NULL, 'Mở cửa đến 22:00', '497 Hòa Hảo', '79', 10.7612, 106.6601),
      ('bus_stop', 'Trạm xe buýt ĐH Bách Khoa', NULL, 'Tuyến 08, 59, 69', 'Lý Thường Kiệt', '79', 10.7730, 106.6590)
      ON CONFLICT DO NOTHING`);

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