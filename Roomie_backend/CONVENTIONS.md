# Quy ước Roomie API
- Tiền tố: /api/v1. Tên route dạng danh từ số nhiều, kebab-case: /room-posts, /saved-posts
- Thành công: { "data": ... } — danh sách có phân trang: { "data": [...], "meta": { page, limit, total } }
- Lỗi: { "error": { "code", "message", "details" } }
  400 sai định dạng · 401 chưa đăng nhập · 403 không có quyền · 404 không thấy · 409 trùng · 422 validate
- JSON dùng camelCase (monthlyRent); cột DB dùng snake_case — đổi trong tầng service.
- Mỗi mảng chỉ sửa trong src/modules/<mảng>/ ; app.js chỉ thêm 1 dòng đăng ký.
- Thay đổi schema = thêm file migrations/00X_mo_ta.sql mới, KHÔNG sửa 001.
- Mỗi tính năng 1 nhánh + 1 Pull Request, có người khác review trước khi merge.
- Không commit .env, không log mật khẩu / OTP / token.