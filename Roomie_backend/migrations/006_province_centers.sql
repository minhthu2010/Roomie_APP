-- 006: tâm của 34 tỉnh/thành (trung tâm hành chính, data/vn-province-centers.json, nạp bằng npm run import:admin).
-- App dùng để mở bản đồ đúng tỉnh người dùng chọn; backend dùng để ưu tiên gợi ý địa chỉ gần tỉnh đó.
BEGIN;

ALTER TABLE provinces ADD COLUMN IF NOT EXISTS center_lat NUMERIC(9,6)
    CHECK (center_lat IS NULL OR center_lat BETWEEN -90 AND 90);
ALTER TABLE provinces ADD COLUMN IF NOT EXISTS center_lng NUMERIC(9,6)
    CHECK (center_lng IS NULL OR center_lng BETWEEN -180 AND 180);

-- Lọc địa điểm (trường, bệnh viện...) theo tỉnh
CREATE INDEX IF NOT EXISTS idx_landmarks_province ON landmarks(province_code, landmark_type) WHERE is_active;

COMMIT;
