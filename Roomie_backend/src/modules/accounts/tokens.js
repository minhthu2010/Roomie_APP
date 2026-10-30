const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const db = require('../../db');
const env = require('../../config/env');
const AppError = require('../../common/AppError');

const sha256 = (value) => crypto.createHash('sha256').update(value).digest('hex');

function signAccessToken(userId) {
  return jwt.sign({}, env.jwtSecret, { subject: userId, expiresIn: env.jwtExpiresIn });
}

function accessTtlSeconds(token) {
  const { exp, iat } = jwt.decode(token);
  return exp - iat;
}

// Cấp cặp token mới; refresh token chỉ lưu SHA-256 trong DB.
async function issueTokens(userId, deviceLabel = null, client = db) {
  const refreshToken = crypto.randomBytes(48).toString('base64url');
  const { rows } = await client.query(
    `INSERT INTO refresh_tokens (user_id, token_hash, device_label, expires_at)
     VALUES ($1, $2, $3, NOW() + make_interval(days => $4)) RETURNING token_id`,
    [userId, sha256(refreshToken), deviceLabel, env.refreshTokenDays]);
  const accessToken = signAccessToken(userId);
  return {
    tokenId: rows[0].token_id,
    tokens: { accessToken, refreshToken, expiresIn: accessTtlSeconds(accessToken) },
  };
}

const invalidRefresh = () => new AppError(401, 'REFRESH_TOKEN_INVALID', 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại');

// Đổi refresh token cũ lấy cặp mới (rotation). Token cũ bị dùng lại => thu hồi mọi phiên của user.
async function rotateRefreshToken(refreshToken) {
  return db.withTransaction(async (c) => {
    const { rows } = await c.query(
      `SELECT rt.token_id, rt.user_id, rt.device_label, rt.expires_at, rt.revoked_at, u.account_status
         FROM refresh_tokens rt JOIN users u ON u.user_id = rt.user_id
        WHERE rt.token_hash = $1
        FOR UPDATE OF rt`, [sha256(refreshToken)]);
    const row = rows[0];
    if (!row) throw invalidRefresh();
    if (row.revoked_at) {
      await db.query('UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id = $1 AND revoked_at IS NULL',
        [row.user_id]);
      throw invalidRefresh();
    }
    if (new Date(row.expires_at) <= new Date()) throw invalidRefresh();
    if (row.account_status !== 'active') throw AppError.forbidden('Tài khoản đã bị khóa');

    const { tokenId, tokens } = await issueTokens(row.user_id, row.device_label, c);
    await c.query('UPDATE refresh_tokens SET revoked_at = NOW(), replaced_by = $2 WHERE token_id = $1',
      [row.token_id, tokenId]);
    return tokens;
  });
}

async function revokeRefreshToken(refreshToken) {
  await db.query('UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_hash = $1 AND revoked_at IS NULL',
    [sha256(refreshToken)]);
}

// Đổi mật khẩu: đăng xuất mọi thiết bị khác.
async function revokeAllForUser(userId, client = db) {
  await client.query('UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id = $1 AND revoked_at IS NULL',
    [userId]);
}

module.exports = { issueTokens, rotateRefreshToken, revokeRefreshToken, revokeAllForUser };
