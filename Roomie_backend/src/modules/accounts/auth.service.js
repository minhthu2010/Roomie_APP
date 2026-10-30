const bcrypt = require('bcryptjs');
const db = require('../../db');
const AppError = require('../../common/AppError');
const otp = require('./otp.service');
const tokens = require('./tokens');
const { sendOtpEmail } = require('./mailer');

const POLICY_VERSION = '2025-10';

// Thông tin tài khoản gọn, trả kèm token sau khi đăng nhập / đăng ký
async function getUserSummary(userId, client = db) {
  const { rows } = await client.query(
    `SELECT u.user_id, u.email, u.phone_number, u.full_name, u.display_name, u.avatar_url,
            u.email_verified_at, u.created_at, up.profile_completed_at
       FROM users u LEFT JOIN user_profiles up ON up.user_id = u.user_id
      WHERE u.user_id = $1`, [userId]);
  const u = rows[0];
  return {
    userId: u.user_id,
    email: u.email,
    phoneNumber: u.phone_number,
    fullName: u.full_name,
    displayName: u.display_name || u.full_name,
    avatarUrl: u.avatar_url,
    emailVerified: u.email_verified_at !== null,
    profileCompleted: u.profile_completed_at !== null,
    createdAt: u.created_at,
  };
}

async function findActiveUserByEmail(email, client = db) {
  const { rows } = await client.query(
    `SELECT user_id, email, password_hash, account_status, email_verified_at
       FROM users WHERE LOWER(email) = LOWER($1) AND deleted_at IS NULL`, [email]);
  return rows[0];
}

async function sendOtp(created, purpose) {
  await sendOtpEmail(created.challenge.email, created.code, purpose, created.ttlMinutes);
  return created.challenge;
}

// Bước 1 đăng ký: tạo tài khoản chưa xác thực + gửi OTP.
// Email đã đăng ký nhưng chưa xác thực thì cho đăng ký lại (cập nhật tên, mật khẩu).
async function register({ fullName, email, password }) {
  const passwordHash = await bcrypt.hash(password, 10);
  const created = await db.withTransaction(async (c) => {
    const existing = await findActiveUserByEmail(email, c);
    let userId;
    if (existing) {
      if (existing.email_verified_at) throw new AppError(409, 'EMAIL_TAKEN', 'Email này đã được đăng ký');
      userId = existing.user_id;
      await c.query('UPDATE users SET full_name = $2, password_hash = $3 WHERE user_id = $1',
        [userId, fullName, passwordHash]);
    } else {
      const { rows } = await c.query(
        `INSERT INTO users (email, password_hash, full_name, display_name)
         VALUES ($1, $2, $3, $3) RETURNING user_id`, [email, passwordHash, fullName]);
      userId = rows[0].user_id;
    }
    await c.query(
      `INSERT INTO policy_acceptances (user_id, policy_type, policy_version)
       VALUES ($1, 'terms_of_service', $2), ($1, 'privacy_policy', $2) ON CONFLICT DO NOTHING`,
      [userId, POLICY_VERSION]);
    return otp.createChallenge({ userId, email, purpose: 'signup' }, c);
  });
  return sendOtp(created, 'signup');
}

async function resendSignupOtp(email) {
  const user = await findActiveUserByEmail(email);
  if (!user) throw AppError.notFound('Không tìm thấy tài khoản đăng ký với email này');
  if (user.email_verified_at) throw new AppError(409, 'ALREADY_VERIFIED', 'Email đã được xác thực, bạn có thể đăng nhập');
  const created = await otp.createChallenge({ userId: user.user_id, email: user.email, purpose: 'signup' });
  return sendOtp(created, 'signup');
}

// Bước 2 đăng ký: đúng OTP thì kích hoạt tài khoản và đăng nhập luôn.
async function verifySignup({ email, code, deviceLabel }) {
  return db.withTransaction(async (c) => {
    const user = await findActiveUserByEmail(email, c);
    if (!user) throw AppError.notFound('Không tìm thấy tài khoản đăng ký với email này');
    if (user.email_verified_at) throw new AppError(409, 'ALREADY_VERIFIED', 'Email đã được xác thực, bạn có thể đăng nhập');

    await otp.consumeChallenge({ email, purpose: 'signup', code }, c);

    const userId = user.user_id;
    await c.query('UPDATE users SET email_verified_at = NOW(), last_login_at = NOW() WHERE user_id = $1', [userId]);
    await c.query('INSERT INTO user_profiles (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [userId]);
    await c.query('INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [userId]);
    await c.query(
      `INSERT INTO user_roles (user_id, role_id)
       SELECT $1, role_id FROM roles WHERE role_code = 'user' ON CONFLICT DO NOTHING`, [userId]);

    const { tokens: t } = await tokens.issueTokens(userId, deviceLabel, c);
    return { ...t, user: await getUserSummary(userId, c) };
  });
}

// Đăng nhập bằng email hoặc số điện thoại + mật khẩu
async function login({ account, password, deviceLabel }) {
  const isEmail = account.includes('@');
  const value = isEmail ? account : account.replace(/[\s.-]/g, '');
  const { rows } = await db.query(
    `SELECT user_id, email, password_hash, account_status, email_verified_at
       FROM users
      WHERE deleted_at IS NULL AND ${isEmail ? 'LOWER(email) = LOWER($1)' : 'phone_number = $1'}`, [value]);
  const user = rows[0];

  const ok = user && user.password_hash && await bcrypt.compare(password, user.password_hash);
  if (!ok) throw new AppError(401, 'INVALID_CREDENTIALS', 'Tài khoản hoặc mật khẩu không đúng');
  if (user.account_status !== 'active') throw new AppError(403, 'ACCOUNT_SUSPENDED', 'Tài khoản của bạn đã bị khóa');
  if (!user.email_verified_at) {
    throw new AppError(403, 'EMAIL_NOT_VERIFIED', 'Tài khoản chưa xác thực email', { email: user.email });
  }

  await db.query('UPDATE users SET last_login_at = NOW() WHERE user_id = $1', [user.user_id]);
  const { tokens: t } = await tokens.issueTokens(user.user_id, deviceLabel);
  return { ...t, user: await getUserSummary(user.user_id) };
}

module.exports = {
  register, resendSignupOtp, verifySignup, login, getUserSummary,
  refresh: tokens.rotateRefreshToken,
  logout: tokens.revokeRefreshToken,
};
