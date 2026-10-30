const router = require('express').Router();
const { z } = require('zod');
const validate = require('../../common/validate');
const { ok } = require('../../common/response');
const places = require('./places.service');

// Gợi ý khi gõ ở ô tìm kiếm: phường/xã thật (DB) + địa chỉ cụ thể có tọa độ (Photon).
// provinceCode: tỉnh người dùng đang chọn -> phường trong tỉnh đó, địa chỉ ưu tiên gần tỉnh đó
router.get('/places/suggest',
  validate({
    query: z.object({
      q: z.string().trim().min(2).max(120),
      provinceCode: z.string().max(10).optional(),
      limit: z.coerce.number().int().min(1).max(10).default(5),
    }),
  }),
  async (req, res) => {
    const { q, provinceCode, limit } = req.valid.query;
    const [wards, addresses] = await Promise.all([
      places.searchWards({ q, provinceCode, limit: 3 }),
      // Dịch vụ ngoài lỗi thì vẫn trả phường/xã, không làm hỏng cả ô tìm kiếm
      places.searchAddresses(q, { limit, provinceCode }).catch(() => []),
    ]);
    ok(res, { wards, addresses });
  });

// Đơn vị hành chính (34 tỉnh/thành, 3.321 phường/xã sau 01/07/2025)
router.get('/admin-units/provinces', async (req, res) => {
  ok(res, await places.listProvinces());
});

router.get('/admin-units/wards',
  validate({
    query: z.object({
      // Không truyền tỉnh thì tìm trong cả nước (nên kèm q, vì cả nước có 3.321 phường/xã)
      provinceCode: z.string().max(10).optional(),
      q: z.string().trim().max(100).optional(),
      limit: z.coerce.number().int().min(1).max(400).default(400),
    }),
  }),
  async (req, res) => {
    ok(res, await places.searchWards(req.valid.query));
  });

// Tâm gần đúng của phường/xã để bản đồ nhảy tới
router.get('/admin-units/wards/:wardCode/center',
  validate({ params: z.object({ wardCode: z.string().max(10) }) }),
  async (req, res) => {
    ok(res, await places.wardCenter(req.valid.params.wardCode));
  });

module.exports = router;
