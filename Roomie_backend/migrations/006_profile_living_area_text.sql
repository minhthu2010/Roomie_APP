-- Người A: màn Thiết lập / Chỉnh sửa hồ sơ có ô "Khu vực sinh sống" dạng chữ
-- ("Quận 10, TP. Hồ Chí Minh"), giống cách bộ lọc tìm phòng lưu khu vực.
-- living_province_code / living_ward_code vẫn giữ để sau này chọn theo danh mục.
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS living_area_text VARCHAR(150);
