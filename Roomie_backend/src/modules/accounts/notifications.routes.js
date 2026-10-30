const router = require('express').Router();
const { z } = require('zod');
const db = require('../../db');
const validate = require('../../common/validate');
const AppError = require('../../common/AppError');
const { ok, getPaging } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');

const idParams = z.object({ notificationId: z.guid() });

function toDto(n) {
  return {
    notificationId: n.notification_id,
    type: n.notification_type,
    title: n.title,
    body: n.body,
    payload: n.payload,
    isRead: n.read_at !== null,
    createdAt: n.created_at,
  };
}

// Màn Thông báo: mới nhất trước; meta có thêm unreadCount cho badge "2 chưa đọc"
router.get('/notifications', requireAuth, async (req, res) => {
  const userId = req.user.userId;
  const paging = getPaging(req.query);
  const unreadOnly = req.query.unread === 'true';
  const where = `user_id = $1 AND deleted_at IS NULL${unreadOnly ? ' AND read_at IS NULL' : ''}`;
  const [list, counts] = await Promise.all([
    db.query(
      `SELECT notification_id, notification_type, title, body, payload, read_at, created_at
         FROM notifications WHERE ${where}
        ORDER BY created_at DESC LIMIT $2 OFFSET $3`, [userId, paging.limit, paging.offset]),
    db.query(
      `SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE read_at IS NULL) AS unread
         FROM notifications WHERE ${where}`, [userId]),
  ]);
  res.json({
    data: list.rows.map(toDto),
    meta: { page: paging.page, limit: paging.limit, total: counts.rows[0].total, unreadCount: counts.rows[0].unread },
  });
});

router.get('/notifications/unread-count', requireAuth, async (req, res) => {
  const { rows } = await db.query(
    'SELECT COUNT(*) AS count FROM notifications WHERE user_id = $1 AND read_at IS NULL AND deleted_at IS NULL',
    [req.user.userId]);
  ok(res, { unreadCount: rows[0].count });
});

router.post('/notifications/read-all', requireAuth, async (req, res) => {
  const { rowCount } = await db.query(
    'UPDATE notifications SET read_at = NOW() WHERE user_id = $1 AND read_at IS NULL AND deleted_at IS NULL',
    [req.user.userId]);
  ok(res, { updated: rowCount });
});

router.patch('/notifications/:notificationId/read', requireAuth, validate({ params: idParams }), async (req, res) => {
  const { rows } = await db.query(
    `UPDATE notifications SET read_at = COALESCE(read_at, NOW())
      WHERE notification_id = $1 AND user_id = $2 AND deleted_at IS NULL
      RETURNING notification_id, notification_type, title, body, payload, read_at, created_at`,
    [req.valid.params.notificationId, req.user.userId]);
  if (!rows[0]) throw AppError.notFound('Không tìm thấy thông báo');
  ok(res, toDto(rows[0]));
});

router.delete('/notifications/:notificationId', requireAuth, validate({ params: idParams }), async (req, res) => {
  const { rowCount } = await db.query(
    `UPDATE notifications SET deleted_at = NOW()
      WHERE notification_id = $1 AND user_id = $2 AND deleted_at IS NULL`,
    [req.valid.params.notificationId, req.user.userId]);
  if (!rowCount) throw AppError.notFound('Không tìm thấy thông báo');
  ok(res, { deleted: true });
});

// ---------- FCM token (app gọi sau khi đăng nhập / khi token đổi) ----------

const tokenBody = z.object({
  fcmToken: z.string().trim().min(20).max(4096),
  platform: z.enum(['android', 'web']).default('android'),
});

router.put('/me/device-tokens', requireAuth, validate({ body: tokenBody }), async (req, res) => {
  // Một máy chỉ thuộc về người đăng nhập gần nhất
  await db.query(
    `INSERT INTO device_tokens (user_id, fcm_token, platform) VALUES ($1, $2, $3)
     ON CONFLICT (fcm_token) DO UPDATE SET user_id = EXCLUDED.user_id, platform = EXCLUDED.platform, last_seen_at = NOW()`,
    [req.user.userId, req.valid.body.fcmToken, req.valid.body.platform]);
  ok(res, { registered: true });
});

// Gọi khi đăng xuất để máy này không nhận push của tài khoản cũ
router.delete('/me/device-tokens', requireAuth, validate({ body: tokenBody.pick({ fcmToken: true }) }), async (req, res) => {
  await db.query('DELETE FROM device_tokens WHERE user_id = $1 AND fcm_token = $2',
    [req.user.userId, req.valid.body.fcmToken]);
  ok(res, { removed: true });
});

module.exports = router;
