const router = require('express').Router();
// A: tài khoản, hồ sơ và thông báo
router.use('/auth', require('./auth.routes'));
router.use(require('./profile.routes'));
router.use(require('./settings.routes'));
router.use(require('./notifications.routes'));
router.use(require('./admin.routes'));
module.exports = router;
