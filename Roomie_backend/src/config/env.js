require('dotenv').config();

const required = ['DATABASE_URL', 'JWT_SECRET'];
for (const key of required) {
  if (!process.env[key]) {
    throw new Error(`Thiếu biến môi trường ${key} trong .env`);
  }
}

module.exports = {
  nodeEnv: process.env.NODE_ENV || 'development',
  port: Number(process.env.PORT || 3000),
  databaseUrl: process.env.DATABASE_URL,
  jwtSecret: process.env.JWT_SECRET,
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '15m',
  authDevBypass: process.env.NODE_ENV !== 'production' && process.env.AUTH_DEV_BYPASS === 'true',
  publicBaseUrl: process.env.PUBLIC_BASE_URL || 'http://localhost:3000',
  refreshTokenDays: Number(process.env.REFRESH_TOKEN_DAYS || 30),
  // Đường dẫn file JSON service account của Firebase (gửi push). Để trống: không gửi push.
  firebaseServiceAccount: process.env.FIREBASE_SERVICE_ACCOUNT || '',
  // OAuth client ID (loại Web) dùng để kiểm tra idToken Google; nhiều ID cách nhau bằng dấu phẩy
  googleClientIds: (process.env.GOOGLE_CLIENT_IDS || '').split(',').map((s) => s.trim()).filter(Boolean),
  // Gửi OTP qua email. Để trống SMTP_HOST khi dev: mã OTP được in ra console thay vì gửi mail.
  smtp: {
    host: process.env.SMTP_HOST || '',
    port: Number(process.env.SMTP_PORT || 465),
    user: process.env.SMTP_USER || '',
    pass: process.env.SMTP_PASS || '',
    from: process.env.MAIL_FROM || 'Roomie <no-reply@roomie.app>',
  },
};