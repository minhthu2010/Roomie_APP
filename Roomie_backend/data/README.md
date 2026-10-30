# Dữ liệu hành chính Việt Nam

`vn-admin-units.json`: 34 tỉnh/thành và 3.321 phường/xã theo sắp xếp hành chính từ 01/07/2025
(hai cấp, không còn cấp quận/huyện).

- Nguồn: https://github.com/thanglequoc/vietnamese-provinces-database
  (file `json/vn_only_simplified_json_generated_data_vn_units_minified.json`)
- Giấy phép: MIT, xem `LICENSE-vietnamese-provinces-database.txt`
- Nạp vào bảng `provinces` / `wards`: `npm run import:admin` (seed cũng tự gọi)

Cập nhật: tải lại file JSON từ nguồn trên, ghi đè `vn-admin-units.json`, chạy lại `npm run import:admin`.

# Địa điểm thật (trường ĐH/CĐ, siêu thị, bệnh viện, trạm xe buýt)

`hcm-landmarks.json`: trường đại học / cao đẳng / học viện, chuỗi siêu thị và cửa hàng tiện lợi,
bệnh viện, trạm xe buýt trong ranh giới TP.HCM (sau sáp nhập 01/07/2025).

- Nguồn: OpenStreetMap qua Overpass API (https://overpass-api.de), relation 1973756
- Giấy phép: ODbL, phải ghi nguồn "© OpenStreetMap contributors" (bản đồ trong app đã ghi)
- Đã lọc: bỏ tòa nhà / cổng trong khuôn viên trường, trung cấp, khoa / phòng khám lẻ của bệnh viện,
  tiệm tạp hóa nhỏ không thuộc chuỗi; gộp các điểm cùng tên đứng sát nhau (trạm 2 chiều đường...)
- Nạp vào bảng `landmarks`: `npm run import:landmarks` (seed cũng tự gọi). Mỗi địa điểm có
  `source_ref` = mã OSM (vd `osm:n123`, `osm:w456`); địa điểm không còn trong file thì bị ẩn.

Cập nhật: `npm run fetch:landmarks` (tải lại từ Overpass, ghi đè file) rồi `npm run import:landmarks`.
