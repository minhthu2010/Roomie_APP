const router = require('express').Router();
const { z } = require('zod');
const db = require('../../db');
const validate = require('../../common/validate');
const AppError = require('../../common/AppError');
const { ok, getPaging, paged } = require('../../common/response');
const { requireAuth, requireAdmin } = require('../../middleware/auth');
const tokens = require('./tokens');

router.use('/admin/users', requireAuth, requireAdmin);

// Danh sách người dùng cho trang admin: ?q=email/tên/SĐT&status=active|suspended
router.get('/admin/users',
  validate({
    query: z.object({
      q: z.string().trim().max(100).optional(),
      status: z.enum(['active', 'suspended']).optional(),
    }).passthrough(),
  }),
  async (req, res) => {
    const paging = getPaging(req.query);
    const { q, status } = req.valid.query;
    const params = [];
    const conds = ['u.deleted_at IS NULL'];
    if (status) {
      params.push(status);
      conds.push(`u.account_status = $${params.length}`);
    }
    if (q) {
      params.push(`%${q.toLowerCase()}%`);
      conds.push(`(LOWER(u.email) LIKE $${params.length} OR LOWER(COALESCE(u.full_name, '')) LIKE $${params.length}
                   OR LOWER(COALESCE(u.display_name, '')) LIKE $${params.length} OR u.phone_number LIKE $${params.length})`);
    }
    const where = conds.join(' AND ');
    const [list, count] = await Promise.all([
      db.query(
        `SELECT u.user_id, u.email, u.phone_number, COALESCE(u.display_name, u.full_name) AS display_name,
                u.account_status, u.email_verified_at IS NOT NULL AS email_verified, u.last_login_at, u.created_at,
                COALESCE(array_agg(r.role_code) FILTER (WHERE r.role_code IS NOT NULL), '{}') AS roles
           FROM users u
           LEFT JOIN user_roles ur ON ur.user_id = u.user_id
           LEFT JOIN roles r ON r.role_id = ur.role_id
          WHERE ${where}
          GROUP BY u.user_id
          ORDER BY u.created_at DESC
          LIMIT $${params.length + 1} OFFSET $${params.length + 2}`, [...params, paging.limit, paging.offset]),
      db.query(`SELECT COUNT(*) AS total FROM users u WHERE ${where}`, params),
    ]);
    paged(res, list.rows.map((u) => ({
      userId: u.user_id,
      email: u.email,
      phoneNumber: u.phone_number,
      displayName: u.display_name,
      accountStatus: u.account_status,
      emailVerified: u.email_verified,
      roles: u.roles,
      lastLoginAt: u.last_login_at,
      createdAt: u.created_at,
    })), paging, count.rows[0].total);
  });

// Khóa / mở khóa tài khoản. Khóa thì thu hồi mọi phiên đăng nhập của người đó.
router.patch('/admin/users/:userId/status',
  validate({
    params: z.object({ userId: z.guid() }),
    body: z.object({
      status: z.enum(['active', 'suspended']),
      reason: z.string().trim().max(500).optional(),
    }),
  }),
  async (req, res) => {
    const { userId } = req.valid.params;
    const { status, reason } = req.valid.body;
    if (userId === req.user.userId) throw AppError.badRequest('Không thể tự khóa tài khoản của chính mình');

    const result = await db.withTransaction(async (c) => {
      const { rows } = await c.query(
        `SELECT u.account_status, EXISTS (
                SELECT 1 FROM user_roles ur JOIN roles r ON r.role_id = ur.role_id
                 WHERE ur.user_id = u.user_id AND r.role_code = 'admin') AS is_admin
           FROM users u WHERE u.user_id = $1 AND u.deleted_at IS NULL FOR UPDATE OF u`, [userId]);
      const target = rows[0];
      if (!target) throw AppError.notFound('Không tìm thấy người dùng');
      if (target.is_admin && status === 'suspended') throw AppError.forbidden('Không thể khóa tài khoản quản trị viên');
      if (target.account_status === status) return { userId, accountStatus: status, changed: false };

      await c.query('UPDATE users SET account_status = $2 WHERE user_id = $1', [userId, status]);
      if (status === 'suspended') await tokens.revokeAllForUser(userId, c);
      await c.query(
        `INSERT INTO admin_action_logs (admin_id, action_type, target_type, target_id, details)
         VALUES ($1, $2, 'user', $3, $4)`,
        [req.user.userId, status === 'suspended' ? 'user_suspend' : 'user_activate', userId,
          { reason: reason || null, previousStatus: target.account_status }]);
      return { userId, accountStatus: status, changed: true };
    });
    ok(res, result);
  });

module.exports = router;
