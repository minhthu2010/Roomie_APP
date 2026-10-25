const jwt = require('jsonwebtoken');
const env = require('../config/env');
const db = require('../db');
const AppError = require('../common/AppError');

async function loadUser(userId) {
  const { rows } = await db.query(
    `SELECT u.user_id, u.account_status,
            COALESCE(array_agg(r.role_code) FILTER (WHERE r.role_code IS NOT NULL), '{}') AS roles
       FROM users u
       LEFT JOIN user_roles ur ON ur.user_id = u.user_id
       LEFT JOIN roles r ON r.role_id = ur.role_id
      WHERE u.user_id = $1
      GROUP BY u.user_id`, [userId]);
  return rows[0];
}

async function requireAuth(req, res, next) {
  try {
    let userId;
    const devUserId = req.get('X-Dev-User-Id');
    if (env.authDevBypass && devUserId) {
      userId = devUserId;
    } else {
      const header = req.get('Authorization') || '';
      const token = header.startsWith('Bearer ') ? header.slice(7) : null;
      if (!token) throw AppError.unauthorized();
      try {
        userId = jwt.verify(token, env.jwtSecret).sub;
      } catch {
        throw new AppError(401, 'TOKEN_INVALID', 'Phiên đăng nhập đã hết hạn');
      }
    }

    const user = await loadUser(userId);
    if (!user) throw AppError.unauthorized();
    if (user.account_status !== 'active') throw AppError.forbidden('Tài khoản đã bị khóa');

    req.user = { userId: user.user_id, roles: user.roles };
    next();
  } catch (err) {
    next(err);
  }
}

function requireAdmin(req, res, next) {
  if (!req.user || !req.user.roles.includes('admin')) return next(AppError.forbidden());
  next();
}

module.exports = { requireAuth, requireAdmin };