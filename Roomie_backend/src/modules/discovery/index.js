const router = require('express').Router();
// C: khám phá, tìm phòng và tương thích
router.use(require('./quiz.routes'));
module.exports = router;
