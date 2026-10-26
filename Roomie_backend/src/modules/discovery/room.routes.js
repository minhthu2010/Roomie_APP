const router = require('express').Router();
const { z } = require('zod');
const validate = require('../../common/validate');
const { ok, paged, getPaging } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');
const room = require('./room.service');

const postParams = { params: z.object({ postId: z.guid() }) };

// Có đăng nhập thì biết "đã lưu", % phù hợp, số điện thoại; không thì vẫn xem được
function optionalAuth(req, res, next) {
  if (req.get('Authorization') || req.get('X-Dev-User-Id')) return requireAuth(req, res, next);
  next();
}
const viewer = (req) => (req.user ? req.user.userId : null);

// Chi tiết phòng và thư viện ảnh
router.get('/room-posts/:postId', optionalAuth, validate(postParams), async (req, res) => {
  ok(res, await room.getRoomDetail(viewer(req), req.valid.params.postId));
});

router.get('/room-posts/:postId/images', optionalAuth, validate(postParams), async (req, res) => {
  ok(res, await room.getRoomImages(viewer(req), req.valid.params.postId));
});

// Vị trí phòng + trường, siêu thị, trạm xe buýt... trong bán kính
router.get('/room-posts/:postId/nearby', optionalAuth,
  validate({
    ...postParams,
    query: z.object({
      radiusKm: z.coerce.number().min(0.2).max(5).default(2),
      limit: z.coerce.number().int().min(1).max(20).default(6),
    }),
  }),
  async (req, res) => {
    ok(res, await room.getNearby(viewer(req), req.valid.params.postId, req.valid.query));
  });

// Bấm "Liên hệ Zalo" / "Gọi điện"
router.post('/room-posts/:postId/contact-clicks', requireAuth,
  validate({ ...postParams, body: z.object({ channel: z.enum(['zalo', 'call']) }) }),
  async (req, res) => {
    ok(res, await room.logContactClick(req.user.userId, req.valid.params.postId, req.valid.body.channel), 201);
  });

// Phòng đã lưu
router.get('/saved-posts', requireAuth, async (req, res) => {
  const paging = getPaging(req.query);
  const result = await room.listSaved(req.user.userId, paging);
  paged(res, result.items, paging, result.total);
});

router.put('/saved-posts/:postId', requireAuth, validate(postParams), async (req, res) => {
  ok(res, await room.savePost(req.user.userId, req.valid.params.postId));
});

router.delete('/saved-posts/:postId', requireAuth, validate(postParams), async (req, res) => {
  ok(res, await room.unsavePost(req.user.userId, req.valid.params.postId));
});

// Hồ sơ công khai của người đăng (tôn trọng cài đặt "Hiện hồ sơ")
router.get('/users/:userId/public-profile', optionalAuth,
  validate({ params: z.object({ userId: z.guid() }) }),
  async (req, res) => {
    ok(res, await room.getPublicProfile(viewer(req), req.valid.params.userId));
  });

module.exports = router;
