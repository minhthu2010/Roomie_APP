const { OAuth2Client } = require('google-auth-library');
const db = require('../../db');
const env = require('../../config/env');
const AppError = require('../../common/AppError');
const tokens = require('./tokens');
const { getUserSummary } = require('./auth.service');

const client = new OAuth2Client();

async function verifyIdToken(idToken) {
  if (!env.googleClientIds.length) {
    throw new AppError(503, 'GOOGLE_NOT_CONFIGURED', 'Máy chủ chưa cấu hình đăng nhập Google');
  }
  try {
    const ticket = await client.verifyIdToken({ idToken, audience: env.googleClientIds });
    return ticket.getPayload();
  } catch {
    throw new AppError(401, 'GOOGLE_TOKEN_INVALID', 'Không xác thực được tài khoản Google, vui lòng thử lại');
  }
}

// "Tiếp tục với Google": đã liên kết -> đăng nhập; trùng email -> liên kết; chưa có -> tạo tài khoản mới
async function loginWithGoogle({ idToken, deviceLabel }) {
  const g = await verifyIdToken(idToken);
  if (!g.email || !g.email_verified) {
    throw new AppError(400, 'GOOGLE_EMAIL_UNVERIFIED', 'Email Google chưa được xác minh');
  }
  const email = g.email.toLowerCase();

  return db.withTransaction(async (c) => {
    let userId;
    const linked = await c.query(
      `SELECT u.user_id, u.account_status FROM auth_identities ai JOIN users u ON u.user_id = ai.user_id
        WHERE ai.provider = 'google' AND ai.provider_subject = $1 AND u.deleted_at IS NULL`, [g.sub]);
    let user = linked.rows[0];

    if (!user) {
      const byEmail = await c.query(
        'SELECT user_id, account_status FROM users WHERE LOWER(email) = $1 AND deleted_at IS NULL', [email]);
      user = byEmail.rows[0];
      if (user) {
        // Google đã xác minh email nên tài khoản đăng ký dở cũng coi như đã xác thực
        await c.query('UPDATE users SET email_verified_at = COALESCE(email_verified_at, NOW()) WHERE user_id = $1',
          [user.user_id]);
      } else {
        const { rows } = await c.query(
          `INSERT INTO users (email, full_name, display_name, avatar_url, email_verified_at)
           VALUES ($1, $2, $2, $3, NOW()) RETURNING user_id, account_status`,
          [email, g.name || email.split('@')[0], g.picture || null]);
        user = rows[0];
      }
      await c.query(
        `INSERT INTO auth_identities (user_id, provider, provider_subject, provider_email)
         VALUES ($1, 'google', $2, $3) ON CONFLICT (user_id, provider) DO NOTHING`,
        [user.user_id, g.sub, email]);
    }
    userId = user.user_id;
    if (user.account_status !== 'active') throw new AppError(403, 'ACCOUNT_SUSPENDED', 'Tài khoản của bạn đã bị khóa');

    await c.query('INSERT INTO user_profiles (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [userId]);
    await c.query('INSERT INTO user_settings (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [userId]);
    await c.query(
      `INSERT INTO user_roles (user_id, role_id)
       SELECT $1, role_id FROM roles WHERE role_code = 'user' ON CONFLICT DO NOTHING`, [userId]);
    await c.query('UPDATE users SET last_login_at = NOW() WHERE user_id = $1', [userId]);

    const { tokens: t } = await tokens.issueTokens(userId, deviceLabel, c);
    return { ...t, user: await getUserSummary(userId, c) };
  });
}

module.exports = { loginWithGoogle };
