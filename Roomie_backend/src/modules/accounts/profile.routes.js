const router = require('express').Router();
const multer = require('multer');
const { z } = require('zod');
const validate = require('../../common/validate');
const AppError = require('../../common/AppError');
const { ok } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');
const profile = require('./profile.service');
const { strongPassword } = require('./schemas');

// Chuỗi rỗng từ app => null (xóa giá trị)
const optionalText = (max) => z.string().trim().max(max)
  .transform((v) => (v === '' ? null : v)).nullable().optional();
const money = z.coerce.number().int().min(0).max(1_000_000_000).nullable().optional();

const updateProfileBody = z.object({
  fullName: z.string().trim().min(2).max(150).optional(),
  displayName: z.string().trim().min(1, 'Vui lòng nhập tên hiển thị').max(120).optional(),
  phoneNumber: z.string().trim().transform((v) => v.replace(/[\s.-]/g, ''))
    .pipe(z.union([z.literal(''), z.string().regex(/^\+?[0-9]{9,15}$/, 'Số điện thoại không hợp lệ')]))
    .transform((v) => (v === '' ? null : v)).nullable().optional(),
  bio: optionalText(500),
  gender: z.enum(['female', 'male', 'other', 'prefer_not_to_say']).nullable().optional(),
  birthYear: z.coerce.number().int().min(1940, 'Năm sinh không hợp lệ').max(2015, 'Năm sinh không hợp lệ').nullable().optional(),
  occupation: z.enum(['student', 'worker', 'other']).nullable().optional(),
  schoolOrWorkplace: optionalText(180),
  // 0 = bỏ trường đã chọn (app Android không gửi được null qua Gson)
  landmarkId: z.coerce.number().int().min(0).transform((v) => (v === 0 ? null : v)).nullable().optional(),
  livingAreaText: optionalText(150),
  livingProvinceCode: optionalText(10),
  livingWardCode: optionalText(10),
  budgetMin: money,
  budgetMax: money,
  moveInDate: z.iso.date('Ngày dọn vào không hợp lệ').nullable().optional(),
  preferredGender: z.enum(['any', 'female', 'male']).optional(),
  onboardingGoal: z.enum(['find_room', 'find_roommate', 'both']).nullable().optional(),
  completeSetup: z.boolean().optional(),
  notificationsEnabled: z.boolean().optional(),
}).refine((b) => b.budgetMin == null || b.budgetMax == null || b.budgetMin <= b.budgetMax,
  { message: 'Ngân sách tối thiểu phải nhỏ hơn tối đa', path: ['budgetMin'] });

const avatarUpload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024, files: 1 },
  fileFilter: (req, file, cb) => {
    if (['image/jpeg', 'image/png', 'image/webp'].includes(file.mimetype)) return cb(null, true);
    cb(new AppError(422, 'INVALID_FILE', 'Ảnh đại diện phải là JPG, PNG hoặc WEBP'));
  },
}).single('avatar');

function handleAvatarUpload(req, res, next) {
  avatarUpload(req, res, (err) => {
    if (err instanceof multer.MulterError) {
      return next(new AppError(422, 'INVALID_FILE',
        err.code === 'LIMIT_FILE_SIZE' ? 'Ảnh đại diện tối đa 5MB' : 'File tải lên không hợp lệ'));
    }
    if (err) return next(err);
    if (!req.file) return next(new AppError(422, 'INVALID_FILE', 'Vui lòng chọn ảnh (field "avatar")'));
    next();
  });
}

// Hồ sơ của tôi (ProfileActivity, Chỉnh sửa hồ sơ, ProfileSetupActivity)
router.get('/me/profile', requireAuth, async (req, res) => {
  ok(res, await profile.getMyProfile(req.user.userId));
});

router.patch('/me/profile', requireAuth, validate({ body: updateProfileBody }), async (req, res) => {
  ok(res, await profile.updateMyProfile(req.user.userId, req.valid.body));
});

// multipart/form-data, field "avatar"
router.put('/me/avatar', requireAuth, handleAvatarUpload, async (req, res) => {
  ok(res, await profile.updateAvatar(req.user.userId, req.file));
});

router.put('/me/password', requireAuth,
  validate({
    body: z.object({
      currentPassword: z.string().max(72).optional(),
      newPassword: strongPassword,
      deviceLabel: z.string().trim().max(120).optional(),
    }),
  }),
  async (req, res) => {
    ok(res, await profile.changePassword(req.user.userId, req.valid.body));
  });

module.exports = router;
