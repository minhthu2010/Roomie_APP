const router = require('express').Router();
// C: khám phá, tìm phòng và tương thích
router.use(require('./quiz.routes'));
router.use(require('./search.routes'));     // /room-posts/search phải đứng trước /room-posts/:postId
router.use(require('./matching.routes'));
module.exports = router;
