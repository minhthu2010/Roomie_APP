// Nạp 34 tỉnh/thành + phường/xã thật (data/vn-admin-units.json) và tâm tỉnh (data/vn-province-centers.json)
// vào bảng provinces / wards.
// Chạy riêng: npm run import:admin   (seed.js cũng gọi hàm này)
// Chạy lại nhiều lần được: chỉ thêm / cập nhật tên, không xóa dữ liệu đang được dùng.
const path = require('path');

const DATA_FILE = path.join(__dirname, '..', 'data', 'vn-admin-units.json');
const CENTERS_FILE = path.join(__dirname, '..', 'data', 'vn-province-centers.json');
// TP.HCM và Hà Nội lên đầu danh sách chọn
const DISPLAY_ORDER = { 79: 1, '01': 2 };

async function importAdminUnits(c) {
  const provinces = require(DATA_FILE);

  // Phường test cũ (DEV001...) trùng tên phường thật -> đổi tên tạm để không vi phạm UNIQUE(province_code, ward_name)
  await c.query(`UPDATE wards SET ward_name = ward_name || ' (dev)'
                  WHERE ward_code LIKE 'DEV%' AND ward_name NOT LIKE '% (dev)'`);

  for (const p of provinces) {
    await c.query(
      `INSERT INTO provinces (province_code, province_name, display_order) VALUES ($1, $2, $3)
       ON CONFLICT (province_code) DO UPDATE SET province_name = EXCLUDED.province_name,
                                                 display_order = EXCLUDED.display_order`,
      [p.Code, p.FullName, DISPLAY_ORDER[p.Code] || 100]);
    // Nạp theo lô cho nhanh (TP.HCM 168 phường, cả nước 3.321)
    const codes = p.Wards.map((w) => w.Code);
    const names = p.Wards.map((w) => w.FullName);
    await c.query(
      `INSERT INTO wards (ward_code, province_code, ward_name)
       SELECT code, $1, name FROM unnest($2::text[], $3::text[]) AS t(code, name)
       ON CONFLICT (ward_code) DO UPDATE SET ward_name = EXCLUDED.ward_name, province_code = EXCLUDED.province_code`,
      [p.Code, codes, names]);
  }

  // Chuyển mọi chỗ đang dùng phường test sang phường thật cùng tên rồi xóa phường test.
  // Đổi ward_code của bài đã duyệt là "material edit" -> tắt trigger duyệt lại trong lúc chuyển.
  const { rows: dev } = await c.query(
    `SELECT d.ward_code AS dev_code, r.ward_code AS real_code
       FROM wards d
       JOIN wards r ON r.province_code = d.province_code
                   AND r.ward_name = replace(d.ward_name, ' (dev)', '')
                   AND r.ward_code NOT LIKE 'DEV%'
      WHERE d.ward_code LIKE 'DEV%'`);
  if (dev.length) {
    await c.query('ALTER TABLE room_posts DISABLE TRIGGER trg_room_posts_material_reapproval');
    for (const { dev_code: from, real_code: to } of dev) {
      await c.query('UPDATE room_posts SET ward_code = $2 WHERE ward_code = $1', [from, to]);
      await c.query('UPDATE user_profiles SET living_ward_code = $2 WHERE living_ward_code = $1', [from, to]);
      await c.query('UPDATE user_preferred_areas SET ward_code = $2 WHERE ward_code = $1', [from, to]);
      await c.query('UPDATE landmarks SET ward_code = $2 WHERE ward_code = $1', [from, to]);
    }
    await c.query('ALTER TABLE room_posts ENABLE TRIGGER trg_room_posts_material_reapproval');
  }
  // Phường test không còn ai dùng thì xóa
  await c.query(`DELETE FROM wards w WHERE w.ward_code LIKE 'DEV%'
                    AND NOT EXISTS (SELECT 1 FROM room_posts rp WHERE rp.ward_code = w.ward_code)`);

  // Tâm tỉnh/thành (data/vn-province-centers.json, tải cùng địa điểm bằng npm run fetch:landmarks)
  const { provinces: centers } = require(CENTERS_FILE);
  await c.query(
    `UPDATE provinces p SET center_lat = t.lat, center_lng = t.lng
       FROM unnest($1::text[], $2::numeric[], $3::numeric[]) AS t(code, lat, lng)
      WHERE p.province_code = t.code`,
    [centers.map((x) => x.code), centers.map((x) => x.lat), centers.map((x) => x.lng)]);

  const { rows } = await c.query(
    'SELECT (SELECT COUNT(*) FROM provinces) AS provinces, (SELECT COUNT(*) FROM wards) AS wards');
  return { provinces: rows[0].provinces, wards: rows[0].wards, devWardsMoved: dev.length };
}

module.exports = { importAdminUnits };

if (require.main === module) {
  const { pool, withTransaction } = require('../src/db');
  withTransaction((c) => importAdminUnits(c))
    .then((r) => {
      console.log(`Đã nạp ${r.provinces} tỉnh/thành, ${r.wards} phường/xã` +
        (r.devWardsMoved ? ` (chuyển ${r.devWardsMoved} phường test sang phường thật)` : ''));
      return pool.end();
    })
    .catch((err) => { console.error(err); process.exit(1); });
}
