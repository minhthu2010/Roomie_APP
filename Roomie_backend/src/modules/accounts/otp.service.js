const crypto = require('crypto');
const db = require('../../db');
const env = require('../../config/env');
const AppError = require('../../common/AppError');

const OTP_TTL_MINUTES = 5;      // UI: "Mã có hiệu lực trong 05:00"
const RESEND_COOLDOWN_SECONDS = 30;

// Lưu HMAC của mã, không lưu mã gốc.
function hashCode(code) {
  return crypto.createHmac('sha256', env.jwtSecret).update(code).digest('hex');
}

function toDto(row) {
  return {
    email: row.destination,
    expiresAt: row.expires_at,
    resendAvailableAt: row.resend_available_at,
    otpLength: 6,
  };
}

// Tạo OTP mới; trả về { code, challenge } để caller gửi mail SAU khi commit.
async function createChallenge({ userId = null, email, purpose }, client = db) {
  const { rows: recent } = await client.query(
    `SELECT resend_available_at FROM verification_challenges
      WHERE LOWER(destination) = LOWER($1) AND purpose = $2 AND consumed_at IS NULL
        AND resend_available_at > NOW()
      ORDER BY created_at DESC LIMIT 1`, [email, purpose]);
  if (recent[0]) {
    const waitSeconds = Math.ceil((new Date(recent[0].resend_available_at) - Date.now()) / 1000);
    throw new AppError(429, 'OTP_TOO_SOON', `Vui lòng đợi ${waitSeconds} giây rồi gửi lại mã`,
      { resendAvailableAt: recent[0].resend_available_at });
  }

  // Mã cũ chưa dùng hết hiệu lực khi có mã mới
  await client.query(
    `UPDATE verification_challenges SET expires_at = NOW()
      WHERE LOWER(destination) = LOWER($1) AND purpose = $2 AND consumed_at IS NULL AND expires_at > NOW()`,
    [email, purpose]);

  const code = String(crypto.randomInt(0, 1000000)).padStart(6, '0');
  const { rows } = await client.query(
    `INSERT INTO verification_challenges (user_id, destination, purpose, code_hash, expires_at, resend_available_at)
     VALUES ($1, $2, $3, $4, NOW() + make_interval(mins => $5), NOW() + make_interval(secs => $6))
     RETURNING destination, expires_at, resend_available_at`,
    [userId, email, purpose, hashCode(code), OTP_TTL_MINUTES, RESEND_COOLDOWN_SECONDS]);
  return { code, challenge: toDto(rows[0]), ttlMinutes: OTP_TTL_MINUTES };
}

// Kiểm tra mã. Đúng: đánh dấu đã dùng bằng client (trong transaction của caller).
// Sai: tăng số lần thử bằng pool riêng để vẫn được lưu dù transaction của caller rollback.
async function consumeChallenge({ email, purpose, code }, client = db) {
  const { rows } = await client.query(
    `SELECT challenge_id, code_hash, expires_at, attempts_used, max_attempts
       FROM verification_challenges
      WHERE LOWER(destination) = LOWER($1) AND purpose = $2 AND consumed_at IS NULL
      ORDER BY created_at DESC LIMIT 1`, [email, purpose]);
  const ch = rows[0];
  if (!ch || new Date(ch.expires_at) <= new Date()) {
    throw new AppError(400, 'OTP_EXPIRED', 'Mã xác thực đã hết hạn, vui lòng gửi lại mã mới');
  }
  if (ch.attempts_used >= ch.max_attempts) {
    throw new AppError(429, 'OTP_LOCKED', 'Bạn đã nhập sai quá nhiều lần, vui lòng gửi lại mã mới');
  }

  const expected = Buffer.from(ch.code_hash, 'hex');
  const actual = Buffer.from(hashCode(code), 'hex');
  if (!crypto.timingSafeEqual(expected, actual)) {
    const { rows: upd } = await db.query(
      `UPDATE verification_challenges SET attempts_used = attempts_used + 1
        WHERE challenge_id = $1 AND attempts_used < max_attempts
        RETURNING max_attempts - attempts_used AS remaining`, [ch.challenge_id]);
    const remaining = upd[0] ? upd[0].remaining : 0;
    throw new AppError(400, 'OTP_INVALID',
      remaining > 0 ? `Mã xác thực không đúng, bạn còn ${remaining} lần thử` : 'Mã xác thực không đúng, vui lòng gửi lại mã mới',
      { remainingAttempts: remaining });
  }

  // Không dùng SELECT ... FOR UPDATE: lệnh tăng số lần thử ở trên chạy trên kết nối khác sẽ bị khóa chờ.
  const { rowCount } = await client.query(
    'UPDATE verification_challenges SET consumed_at = NOW() WHERE challenge_id = $1 AND consumed_at IS NULL',
    [ch.challenge_id]);
  if (rowCount === 0) throw new AppError(400, 'OTP_EXPIRED', 'Mã xác thực đã được sử dụng, vui lòng gửi lại mã mới');
}

module.exports = { createChallenge, consumeChallenge };
