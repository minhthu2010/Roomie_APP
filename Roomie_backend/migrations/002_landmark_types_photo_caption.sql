-- 002: landmark types + subtitle for "Vị trí phòng", photo caption for "Thư viện ảnh"
BEGIN;

ALTER TABLE landmarks DROP CONSTRAINT IF EXISTS landmarks_landmark_type_check;
ALTER TABLE landmarks ADD CONSTRAINT landmarks_landmark_type_check
    CHECK (landmark_type IN ('university', 'workplace', 'supermarket', 'bus_stop', 'hospital', 'other'));

ALTER TABLE landmarks ADD COLUMN IF NOT EXISTS subtitle VARCHAR(150);  -- "Tuyến 08, 59, 69", "Mở cửa đến 22:00"
ALTER TABLE post_images ADD COLUMN IF NOT EXISTS caption VARCHAR(100); -- "Không gian phòng"

COMMIT;
