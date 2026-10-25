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
};