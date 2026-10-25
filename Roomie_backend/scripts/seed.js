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
  });

  console.log('Seed xong. Đăng nhập test: *@roomie.test / Roomie@123');
  await pool.end();
})().catch((err) => { console.error(err); process.exit(1); });