// Nạp trường ĐH/CĐ, siêu thị, bệnh viện, trạm xe buýt thật của cả nước (data/vn-landmarks.json) vào bảng landmarks.
// Chạy riêng: npm run import:landmarks   (seed.js cũng gọi hàm này; cần chạy import:admin trước)
// Chạy lại nhiều lần được: cập nhật theo mã OSM; địa điểm không còn trong file thì ẩn (is_active = false).
const path = require('path');

const DATA_FILE = path.join(__dirname, '..', 'data', 'vn-landmarks.json');
// Địa điểm mẫu cũ đang được bài đăng / hồ sơ trỏ tới: chuyển sang địa điểm thật cùng loại gần nhất trong bán kính này
const REMAP_RADIUS_KM = 1.5;

async function importLandmarks(c) {
  const { items } = require(DATA_FILE);
  // Trường không có trong file (shortName, subtitle, address) -> NULL
  const col = (key) => items.map((i) => i[key] ?? null);
  const refs = items.map((i) => `osm:${i.ref}`);

  await c.query(
    `INSERT INTO landmarks (source_ref, landmark_type, name, short_name, subtitle, address_text,
                            province_code, latitude, longitude, is_active)
     SELECT ref, type, name, short_name, subtitle, address, province, lat, lng, TRUE
       FROM unnest($1::text[], $2::text[], $3::text[], $4::text[], $5::text[], $6::text[], $7::text[],
                   $8::numeric[], $9::numeric[])
            AS t(ref, type, name, short_name, subtitle, address, province, lat, lng)
     ON CONFLICT (source_ref) WHERE source_ref IS NOT NULL DO UPDATE SET
       landmark_type = EXCLUDED.landmark_type, name = EXCLUDED.name, short_name = EXCLUDED.short_name,
       subtitle = EXCLUDED.subtitle, address_text = EXCLUDED.address_text, province_code = EXCLUDED.province_code,
       latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude, is_active = TRUE`,
    [refs, col('type'), col('name'), col('shortName'), col('subtitle'), col('address'), col('provinceCode'),
     col('lat'), col('lng')]);
  await c.query(
    `UPDATE landmarks SET is_active = FALSE
      WHERE source_ref LIKE 'osm:%' AND source_ref <> ALL($1::text[])`, [refs]);

  // Địa điểm mẫu (seed cũ, không có source_ref): chuyển tham chiếu sang địa điểm thật gần nhất rồi xóa.
  // Đổi landmark_id không làm bài đăng phải duyệt lại (không phải cột "material").
  const { rows: remap } = await c.query(
    `SELECT s.landmark_id AS from_id,
            (SELECT r.landmark_id FROM landmarks r
              WHERE r.source_ref IS NOT NULL AND r.is_active AND r.landmark_type = s.landmark_type
                AND abs(r.latitude - s.latitude) < $1 / 111.0 AND abs(r.longitude - s.longitude) < $1 / 109.0
              ORDER BY (r.latitude - s.latitude)^2 + (r.longitude - s.longitude)^2
              LIMIT 1) AS to_id
       FROM landmarks s
      WHERE s.source_ref IS NULL`, [REMAP_RADIUS_KM]);
  for (const { from_id: from, to_id: to } of remap) {
    await c.query('UPDATE room_posts SET landmark_id = $2 WHERE landmark_id = $1', [from, to]);
    await c.query('UPDATE user_profiles SET landmark_id = $2 WHERE landmark_id = $1', [from, to]);
  }
  await c.query('DELETE FROM landmarks WHERE source_ref IS NULL');

  const { rows } = await c.query(
    `SELECT landmark_type, COUNT(*)::int AS n FROM landmarks WHERE is_active GROUP BY 1 ORDER BY 1`);
  return { byType: Object.fromEntries(rows.map((r) => [r.landmark_type, r.n])), samplesRemoved: remap.length };
}

module.exports = { importLandmarks };

if (require.main === module) {
  const { pool, withTransaction } = require('../src/db');
  withTransaction((c) => importLandmarks(c))
    .then((r) => {
      console.log('Đã nạp địa điểm thật:', r.byType,
        r.samplesRemoved ? `(thay ${r.samplesRemoved} địa điểm mẫu)` : '');
      return pool.end();
    })
    .catch((err) => { console.error(err); process.exit(1); });
}
