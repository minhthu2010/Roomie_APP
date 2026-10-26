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

const searchQuery = z.object({
  q: z.string().trim().max(100).optional(),
  area: z.string().trim().max(100).optional(),
  landmarkId: z.coerce.number().int().positive().optional(),
  radiusKm: z.coerce.number().min(0.5).max(20).default(3),
  minRent: z.coerce.number().int().min(0).optional(),
  maxRent: z.coerce.number().int().min(0).optional(),
  roomTypes: csv(z.enum(ROOM_TYPES)),
  gender: z.enum(['female', 'male']).optional(),
  amenities: csv(z.string().max(50)),
  sort: z.enum(['match', 'newest', 'price_asc', 'price_desc', 'distance']).default('match'),
  page: z.string().optional(),
  limit: z.string().optional(),
}).refine((f) => f.minRent == null || f.maxRent == null || f.minRent <= f.maxRent,
  { message: 'minRent phải nhỏ hơn hoặc bằng maxRent', path: ['minRent'] });

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
      limit: z.coerce.number().int().min(1).max(20).default(8),
      radiusKm: z.coerce.number().min(0.5).max(20).default(3),
    }),
  }),
  async (req, res) => {
    ok(res, await search.suggestLandmarks(req.valid.query));
  });

module.exports = router;
