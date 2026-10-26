const router = require('express').Router();
const { z } = require('zod');
const validate = require('../../common/validate');
const { ok } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');
const matching = require('./matching.service');

// Kết quả tương thích của tôi với một phòng: so với phòng (bài test bước 6) và so với chủ phòng
router.get('/room-posts/:postId/compatibility', requireAuth,
  validate({ params: z.object({ postId: z.guid() }) }),
  async (req, res) => {
    ok(res, await matching.getCompatibility(req.user.userId, req.valid.params.postId));
  });

module.exports = router;
