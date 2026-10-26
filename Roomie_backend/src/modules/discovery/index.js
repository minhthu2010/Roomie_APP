const router = require('express').Router();
// C: khám phá, tìm phòng và tương thích
router.use(require('./quiz.routes'));
router.use(require('./matching.routes'));
module.exports = router;
