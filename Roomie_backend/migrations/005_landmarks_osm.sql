-- 005: địa điểm thật từ OpenStreetMap (data/hcm-landmarks.json, nạp bằng npm run import:landmarks).
-- Chuỗi cửa hàng / trạm xe buýt trùng tên nhau ("Circle K", "Bến Thành") nên bỏ UNIQUE(province_code, name),
-- mỗi địa điểm nhận diện bằng mã OSM (vd "osm:n123456", "osm:w789").
BEGIN;

ALTER TABLE landmarks ADD COLUMN IF NOT EXISTS source_ref VARCHAR(30);
ALTER TABLE landmarks DROP CONSTRAINT IF EXISTS landmarks_province_code_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uq_landmarks_source_ref ON landmarks(source_ref) WHERE source_ref IS NOT NULL;

-- Màn Vị trí phòng tìm địa điểm quanh 1 tọa độ; gợi ý trường lọc theo loại
CREATE INDEX IF NOT EXISTS idx_landmarks_lat_lng ON landmarks(latitude, longitude) WHERE is_active;
CREATE INDEX IF NOT EXISTS idx_landmarks_type ON landmarks(landmark_type) WHERE is_active;

COMMIT;
