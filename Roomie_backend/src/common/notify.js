const db = require('../db');
const { sendPush } = require('../modules/accounts/push');

const TYPE_TO_SETTING = {
  room_match: 'notify_room_matches',
  post_activity: 'notify_post_activity',
  post_expiring: 'notify_post_expiring',
  tip: 'notify_tips',
};

async function notify(userId, type, title, body, payload = {}, client = db) {
  const settingColumn = TYPE_TO_SETTING[type];
  if (settingColumn) {
    const { rows } = await client.query(
      `SELECT ${settingColumn} AS enabled FROM user_settings WHERE user_id = $1`, [userId]);
    if (rows[0] && rows[0].enabled === false) return null;
  }
  const { rows } = await client.query(
    `INSERT INTO notifications (user_id, notification_type, title, body, payload)
     VALUES ($1, $2, $3, $4, $5) RETURNING notification_id`,
    [userId, type, title, body, payload]);
  const notificationId = rows[0].notification_id;
  // Gửi push sau khi trả kết quả, lỗi push không làm hỏng thao tác chính (duyệt bài, ...)
  setImmediate(() => {
    sendPush(userId, { notificationId, type, title, body, payload })
      .catch((err) => console.error('Gửi push thất bại:', err.message));
  });
  return notificationId;
}

module.exports = { notify };