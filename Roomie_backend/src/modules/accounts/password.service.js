const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../../db');
const env = require('../../config/env');
const AppError = require('../../common/AppError');
const otp = require('./otp.service');
const tokens = require('./tokens');
const { sendOtpEmail } = require('./mailer');

const RESET_TOKEN_TTL = '10m';

async function findByEmail(email) {
  const { rows } = await db.query(
    `SELECT user_id, email, password_hash, account_status, email_verified_at
       FROM users WHERE LOWER(email) = LOWER($1) AND deleted_at IS NULL`, [email]);
  const user = rows[0];
  if (!user) throw AppError.notFound('Email này chưa được đăng ký');
  if (user.account_status !== 'active') throw new AppError(403, 'ACCOUNT_SUSPENDED', 'Tài khoản của bạn đã bị khóa');
  if (!user.email_verified_at) {
    throw new AppError(403, 'EMAIL_NOT_VERIFIED', 'Tài khoản chưa xác thực email, vui lòng hoàn tất đăng ký', { email: user.email });
  }
  return user;
}

// Dấu vân tay mật khẩu hiện tại: đặt lại xong thì resetToken cũ hết hiệu lực (dùng một lần)
function passwordFingerprint(hash) {
  return crypto.createHash('sha256').update(hash || 'no-password').digest('hex').slice(0, 16);
}

// Bước 1: gửi mã khôi phục tới email của tài khoản
async function requestReset(email) {
  const user = await findByEmail(email);
  const created = await otp.createChallenge({ userId: user.user_id, email: user.email, purpose: 'password_reset' });
  await sendOtpEmail(user.email, created.code, 'password_reset', created.ttlMinutes);
  return created.challenge;
}

// Bước 2: đúng mã thì cấp resetToken ngắn hạn cho màn đặt mật khẩu mới
async function verifyResetOtp(email, code) {
  const user = await findByEmail(email);
  await otp.consumeChallenge({ email: user.email, purpose: 'password_reset', code });
  const resetToken = jwt.sign(
    { purpose: 'password_reset', pf: passwordFingerprint(user.password_hash) },
    env.jwtSecret, { subject: user.user_id, expiresIn: RESET_TOKEN_TTL });
  return { resetToken, expiresIn: 600 };
}

// Bước 3: đặt mật khẩu mới, đăng xuất mọi thiết bị
async function resetPassword(resetToken, newPassword) {
  let payload;
  try {
    payload = jwt.verify(resetToken, env.jwtSecret);
  } catch {
    throw new AppError(400, 'RESET_TOKEN_INVALID', 'Phiên đặt lại mật khẩu đã hết hạn, vui lòng gửi lại mã');
  }
  if (payload.purpose !== 'password_reset') {
    throw new AppError(400, 'RESET_TOKEN_INVALID', 'Phiên đặt lại mật khẩu không hợp lệ');
  }

  const newHash = await bcrypt.hash(newPassword, 10);
  await db.withTransaction(async (c) => {
    const { rows } = await c.query(
      'SELECT password_hash FROM users WHERE user_id = $1 AND deleted_at IS NULL FOR UPDATE', [payload.sub]);
    if (!rows[0] || passwordFingerprint(rows[0].password_hash) !== payload.pf) {
      throw new AppError(400, 'RESET_TOKEN_INVALID', 'Mật khẩu đã được đặt lại, vui lòng gửi lại mã nếu cần');
    }
    await c.query('UPDATE users SET password_hash = $2 WHERE user_id = $1', [payload.sub, newHash]);
    await tokens.revokeAllForUser(payload.sub, c);
  });
  return { passwordReset: true };
}

module.exports = { requestReset, verifyResetOtp, resetPassword };
