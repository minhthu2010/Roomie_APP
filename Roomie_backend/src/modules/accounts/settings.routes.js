const router = require('express').Router();
const { z } = require('zod');
const db = require('../../db');
const validate = require('../../common/validate');
const { ok } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');

// camelCase -> cột user_settings (switch trên màn Cài đặt thông báo / Quyền riêng tư)
const FIELDS = {
  notifyRoomMatches: 'notify_room_matches',
  notifyPostActivity: 'notify_post_activity',
  notifyPostExpiring: 'notify_post_expiring',
  notifyTips: 'notify_tips',
  showProfile: 'show_profile',
  showContactOnPosts: 'show_contact_on_posts',
};

const SELECT = Object.entries(FIELDS).map(([key, col]) => `${col} AS "${key}"`).join(', ');

async function getSettings(userId) {
  await db.query('INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [userId]);
  const { rows } = await db.query(`SELECT ${SELECT} FROM user_settings WHERE user_id = $1`, [userId]);
  return rows[0];
}

router.get('/me/settings', requireAuth, async (req, res) => {
  ok(res, await getSettings(req.user.userId));
});

// Gửi một hoặc nhiều switch: { "notifyTips": true }
router.patch('/me/settings', requireAuth,
  validate({
    body: z.object(Object.fromEntries(Object.keys(FIELDS).map((k) => [k, z.boolean().optional()])))
      .refine((b) => Object.values(b).some((v) => v !== undefined), 'Không có cài đặt nào để lưu'),
  }),
  async (req, res) => {
    const userId = req.user.userId;
    await getSettings(userId);
    const entries = Object.entries(req.valid.body).filter(([, v]) => v !== undefined);
    const sets = entries.map(([key], i) => `${FIELDS[key]} = $${i + 2}`);
    await db.query(`UPDATE user_settings SET ${sets.join(', ')} WHERE user_id = $1`,
      [userId, ...entries.map(([, v]) => v)]);
    ok(res, await getSettings(userId));
  });

module.exports = router;
