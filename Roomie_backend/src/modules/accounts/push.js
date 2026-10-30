const fs = require('fs');
const db = require('../../db');
const env = require('../../config/env');

// Firebase Admin chỉ khởi tạo khi có file service account (FIREBASE_SERVICE_ACCOUNT trong .env).
// Chưa cấu hình: bỏ qua push, thông báo vẫn nằm trong DB và hiện ở màn Thông báo.
let messaging = null;
let warned = false;

function getMessaging() {
  if (messaging || !env.firebaseServiceAccount) return messaging;
  const admin = require('firebase-admin');
  const credentials = JSON.parse(fs.readFileSync(env.firebaseServiceAccount, 'utf8'));
  if (!admin.apps.length) admin.initializeApp({ credential: admin.credential.cert(credentials) });
  messaging = admin.messaging();
  return messaging;
}

// FCM yêu cầu mọi giá trị trong data là chuỗi
function toStringMap(obj) {
  return Object.fromEntries(Object.entries(obj || {})
    .filter(([, v]) => v !== null && v !== undefined)
    .map(([k, v]) => [k, typeof v === 'string' ? v : JSON.stringify(v)]));
}

async function sendPush(userId, { notificationId, type, title, body, payload }) {
  let fcm;
  try {
    fcm = getMessaging();
  } catch (err) {
    console.error('Không khởi tạo được Firebase Admin:', err.message);
    return;
  }
  if (!fcm) {
    if (!warned && env.nodeEnv !== 'production') {
      console.log('[DEV] Chưa cấu hình FIREBASE_SERVICE_ACCOUNT, bỏ qua gửi push');
      warned = true;
    }
    return;
  }

  const { rows } = await db.query('SELECT fcm_token FROM device_tokens WHERE user_id = $1', [userId]);
  if (!rows.length) return;
  const tokens = rows.map((r) => r.fcm_token);

  const result = await fcm.sendEachForMulticast({
    tokens,
    notification: { title, body },
    data: { ...toStringMap(payload), notificationId, type },
    android: { priority: 'high', notification: { channelId: 'roomie_default' } },
  });

  // Token đã gỡ app / hết hạn: xóa khỏi DB
  const dead = result.responses
    .map((r, i) => (!r.success && ['messaging/registration-token-not-registered', 'messaging/invalid-registration-token']
      .includes(r.error && r.error.code) ? tokens[i] : null))
    .filter(Boolean);
  if (dead.length) await db.query('DELETE FROM device_tokens WHERE fcm_token = ANY($1)', [dead]);
}

module.exports = { sendPush };
