const router = require('express').Router();
const { z } = require('zod');
const validate = require('../../common/validate');
const { ok } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');
const auth = require('./auth.service');
const password = require('./password.service');
const google = require('./google.service');
const { email, otpCode, account, strongPassword } = require('./schemas');

const signupPassword = z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự').max(72, 'Mật khẩu tối đa 72 ký tự');
const deviceLabel = z.string().trim().max(120).optional();
const refreshBody = z.object({ refreshToken: z.string().min(20).max(200) });

// Đăng ký (RegisterActivity) -> gửi OTP qua email
router.post('/register',
  validate({
    body: z.object({
      fullName: z.string().trim().min(2, 'Vui lòng nhập họ và tên').max(150),
      email,
      password: signupPassword,
      acceptTerms: z.literal(true, 'Vui lòng đồng ý với Điều khoản sử dụng'),
    }),
  }),
  async (req, res) => {
    ok(res, await auth.register(req.valid.body), 201);
  });

// Xác thực OTP đăng ký (VerifyRegisterActivity) -> trả token, đăng nhập luôn
router.post('/register/verify',
  validate({ body: z.object({ email, code: otpCode, deviceLabel }) }),
  async (req, res) => {
    ok(res, await auth.verifySignup(req.valid.body));
  });

router.post('/register/resend-otp',
  validate({ body: z.object({ email }) }),
  async (req, res) => {
    ok(res, await auth.resendSignupOtp(req.valid.body.email));
  });

// Đăng nhập bằng email hoặc số điện thoại (LoginActivity)
router.post('/login',
  validate({
    body: z.object({
      account: z.string().trim().min(1, 'Vui lòng nhập số điện thoại hoặc email').max(254),
      password: z.string().min(1, 'Vui lòng nhập mật khẩu').max(72),
      deviceLabel,
    }),
  }),
  async (req, res) => {
    ok(res, await auth.login(req.valid.body));
  });

// Đổi refresh token lấy access token mới (SplashActivity, interceptor khi gặp 401)
router.post('/refresh', validate({ body: refreshBody }), async (req, res) => {
  ok(res, await auth.refresh(req.valid.body.refreshToken));
});

router.post('/logout', validate({ body: refreshBody }), async (req, res) => {
  await auth.logout(req.valid.body.refreshToken);
  ok(res, { loggedOut: true });
});

// "Tiếp tục với Google" (LoginActivity): app gửi idToken lấy từ Credential Manager
router.post('/google',
  validate({ body: z.object({ idToken: z.string().min(100).max(4096), deviceLabel }) }),
  async (req, res) => {
    ok(res, await google.loginWithGoogle(req.valid.body));
  });

// ---------- Quên mật khẩu (ForgotPassword -> VerifyOtp -> ResetPassword) ----------

router.post('/password/forgot', validate({ body: z.object({ account }) }), async (req, res) => {
  ok(res, await password.requestReset(req.valid.body.account));
});

router.post('/password/verify-otp',
  validate({ body: z.object({ account, code: otpCode }) }),
  async (req, res) => {
    ok(res, await password.verifyResetOtp(req.valid.body.account, req.valid.body.code));
  });

router.post('/password/reset',
  validate({ body: z.object({ resetToken: z.string().min(20).max(1000), newPassword: strongPassword }) }),
  async (req, res) => {
    ok(res, await password.resetPassword(req.valid.body.resetToken, req.valid.body.newPassword));
  });

// Kiểm tra token còn hạn + thông tin tài khoản gọn
router.get('/me', requireAuth, async (req, res) => {
  ok(res, await auth.getUserSummary(req.user.userId));
});

module.exports = router;
