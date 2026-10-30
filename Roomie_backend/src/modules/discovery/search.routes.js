const router = require('express').Router();
const { z } = require('zod');
const validate = require('../../common/validate');
const { ok, paged, getPaging } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');
const search = require('./search.service');

const ROOM_TYPES = ['shared_room', 'private_room', 'apartment', 'whole_house', 'studio'];

// "a,b,c" -> ['a','b','c']
const csv = (values) => z.string().optional()
  .transform((s) => (s ? s.split(',').map((x) => x.trim()).filter(Boolean) : []))
  .pipe(z.array(values));

// Có đăng nhập (token hoặc X-Dev-User-Id) thì tính % phù hợp và "đã lưu"; không thì vẫn tìm được
function optionalAuth(req, res, next) {
  if (req.get('Authorization') || req.get('X-Dev-User-Id')) return requireAuth(req, res, next);
  next();
}

const filters = {
  q: z.string().trim().max(100).optional(),
  area: z.string().trim().max(100).optional(),
  landmarkId: z.coerce.number().int().positive().optional(),
  radiusKm: z.coerce.number().min(0.5).max(20).default(3),
  nearLat: z.coerce.number().min(-90).max(90).optional(),
  nearLng: z.coerce.number().min(-180).max(180).optional(),
  wardCode: z.string().max(10).optional(),
  // Tỉnh/thành người dùng chọn (feed Home, tìm theo từ khóa); không áp dụng khi tìm quanh 1 điểm
  provinceCode: z.string().max(10).optional(),
  minRent: z.coerce.number().int().min(0).optional(),
  maxRent: z.coerce.number().int().min(0).optional(),
  roomTypes: csv(z.enum(ROOM_TYPES)),
  gender: z.enum(['female', 'male']).optional(),
  amenities: csv(z.string().max(50)),
  // Chuyển vào được trước ngày này (phòng chưa ghi ngày = ở được ngay)
  moveInBy: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'moveInBy phải có dạng YYYY-MM-DD').optional(),
  minArea: z.coerce.number().min(0).max(1000).optional(),
  maxDeposit: z.coerce.number().int().min(0).optional(),
  // Số người cần ở cùng lúc (đi cùng bạn bè): phòng phải còn tìm ít nhất ngần ấy người
  minSlots: z.coerce.number().int().min(1).max(10).optional(),
  occupation: z.enum(['student', 'worker']).optional(),
  sort: z.enum(['match', 'newest', 'price_asc', 'price_desc', 'distance']).default('match'),
  minMatch: z.coerce.number().int().min(0).max(100).optional(),
  page: z.string().optional(),
  limit: z.string().optional(),
};
const rentOrder = [(f) => f.minRent == null || f.maxRent == null || f.minRent <= f.maxRent,
  { message: 'minRent phải nhỏ hơn hoặc bằng maxRent', path: ['minRent'] }];

const lat = z.coerce.number().min(-90).max(90);
const lng = z.coerce.number().min(-180).max(180);
const bbox = { minLat: lat, maxLat: lat, minLng: lng, maxLng: lng };

const searchQuery = z.object({ ...filters, ...z.object(bbox).partial().shape }).refine(...rentOrder);
const mapQuery = z.object({ ...filters, ...bbox })
  .refine(...rentOrder)
  .refine((f) => f.minLat < f.maxLat && f.minLng < f.maxLng, { message: 'Vùng bản đồ không hợp lệ', path: ['minLat'] });

// Bản đồ giá phòng: các phòng trong vùng đang xem (tối đa MAP_LIMIT), cùng bộ lọc với tìm kiếm
const MAP_LIMIT = 200;
router.get('/room-posts/map', optionalAuth, validate({ query: mapQuery }), async (req, res) => {
  const result = await search.searchRooms(req.user ? req.user.userId : null,
    { ...req.valid.query, offset: 0, limit: MAP_LIMIT });
  ok(res, { items: result.items, total: result.total, truncated: result.total > MAP_LIMIT });
});

// Tìm phòng + bộ lọc; trả RoomCardDto có matchPercent, sắp xếp theo % phù hợp mặc định
router.get('/room-posts/search', optionalAuth, validate({ query: searchQuery }), async (req, res) => {
  const paging = getPaging(req.query);
  const result = await search.searchRooms(req.user ? req.user.userId : null, { ...req.valid.query, ...paging });
  paged(res, result.items, paging, result.total);
});

// Gợi ý trường học (ô tìm kiếm, "Gợi ý trường học")
router.get('/landmarks/suggest',
  validate({
    query: z.object({
      q: z.string().trim().max(100).optional(),
      type: z.enum(['university', 'workplace', 'supermarket', 'bus_stop', 'hospital', 'other']).optional(),
      provinceCode: z.string().max(10).optional(),
      limit: z.coerce.number().int().min(1).max(20).default(8),
      radiusKm: z.coerce.number().min(0.5).max(20).default(3),
    }),
  }),
  async (req, res) => {
    ok(res, await search.suggestLandmarks(req.valid.query));
  });

module.exports = router;
