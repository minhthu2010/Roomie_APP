# Dữ liệu hành chính Việt Nam

`vn-admin-units.json`: 34 tỉnh/thành và 3.321 phường/xã theo sắp xếp hành chính từ 01/07/2025
(hai cấp, không còn cấp quận/huyện).

- Nguồn: https://github.com/thanglequoc/vietnamese-provinces-database
  (file `json/vn_only_simplified_json_generated_data_vn_units_minified.json`)
- Giấy phép: MIT, xem `LICENSE-vietnamese-provinces-database.txt`
- Nạp vào bảng `provinces` / `wards`: `npm run import:admin` (seed cũng tự gọi)

Cập nhật: tải lại file JSON từ nguồn trên, ghi đè `vn-admin-units.json`, chạy lại `npm run import:admin`.
