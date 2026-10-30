# Dữ liệu hành chính Việt Nam

`vn-admin-units.json`: 34 tỉnh/thành và 3.321 phường/xã theo sắp xếp hành chính từ 01/07/2025
(hai cấp, không còn cấp quận/huyện).

- Nguồn: https://github.com/thanglequoc/vietnamese-provinces-database
  (file `json/vn_only_simplified_json_generated_data_vn_units_minified.json`)
- Giấy phép: MIT, xem `LICENSE-vietnamese-provinces-database.txt`
- Nạp vào bảng `provinces` / `wards`: `npm run import:admin` (seed cũng tự gọi)

Cập nhật: tải lại file JSON từ nguồn trên, ghi đè `vn-admin-units.json`, chạy lại `npm run import:admin`.

# Địa điểm thật cả nước (trường ĐH/CĐ, siêu thị, bệnh viện, trạm xe buýt)

`vn-landmarks.json`: trường đại học / cao đẳng / học viện, chuỗi siêu thị và cửa hàng tiện lợi,
bệnh viện, trạm xe buýt của 34 tỉnh/thành (ranh giới sau 01/07/2025); mỗi địa điểm có `provinceCode`.

`vn-province-centers.json`: tâm (trung tâm hành chính) của 34 tỉnh/thành, dùng để mở bản đồ đúng tỉnh
và ưu tiên gợi ý địa chỉ gần tỉnh người dùng chọn. Nạp vào `provinces.center_lat/center_lng` bằng
`npm run import:admin`.

- Nguồn: OpenStreetMap qua Overpass API (https://overpass-api.de), tải lần lượt từng tỉnh
- Giấy phép: ODbL, phải ghi nguồn "© OpenStreetMap contributors" (bản đồ trong app đã ghi)
- Đã lọc: bỏ tòa nhà / cổng trong khuôn viên trường, trung cấp, khoa / phòng khám lẻ của bệnh viện,
  tiệm tạp hóa nhỏ không thuộc chuỗi; gộp các điểm cùng tên đứng sát nhau (trạm 2 chiều đường...)
- Nạp vào bảng `landmarks`: `npm run import:landmarks` (seed cũng tự gọi). Mỗi địa điểm có
  `source_ref` = mã OSM (vd `osm:n123`, `osm:w456`); địa điểm không còn trong file thì bị ẩn.

Cập nhật: `npm run fetch:landmarks` (tải lại từ Overpass, ghi đè 2 file, mất vài phút)
rồi `npm run import:admin` và `npm run import:landmarks`.
