-- 004: lưu kết quả tra địa chỉ (Photon) để không gọi lại dịch vụ ngoài cho cùng một từ khóa.
-- Dịch vụ cộng đồng nên phải dùng tiết kiệm; kết quả cũ hơn 30 ngày sẽ được tra lại.
BEGIN;

CREATE TABLE IF NOT EXISTS geocode_cache (
    query_key   TEXT PRIMARY KEY,          -- từ khóa đã chuẩn hóa (thường, không dấu) + loại tra cứu
    results     JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_geocode_cache_created ON geocode_cache(created_at);

-- Tìm phường/xã không dấu ("dien hong" -> "Phường Diên Hồng")
CREATE INDEX IF NOT EXISTS idx_wards_name_unaccent
    ON wards (lower(immutable_unaccent(ward_name)) text_pattern_ops);

COMMIT;
