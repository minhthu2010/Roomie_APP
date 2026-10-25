const AppError = require('../common/AppError');
const env = require('../config/env');

const PG_ERRORS = {
  '23505': [409, 'DUPLICATE', 'Dữ liệu đã tồn tại'],
  '23503': [409, 'REFERENCE_ERROR', 'Dữ liệu liên quan không tồn tại hoặc đang được sử dụng'],
  '23514': [422, 'CHECK_VIOLATION', 'Dữ liệu không thỏa điều kiện'],
  '23502': [422, 'MISSING_FIELD', 'Thiếu dữ liệu bắt buộc'],
  '22P02': [400, 'INVALID_FORMAT', 'Định dạng dữ liệu không hợp lệ'],
};

function notFound(req, res, next) {
  next(AppError.notFound(`Không có API ${req.method} ${req.originalUrl}`));
}

// eslint-disable-next-line no-unused-vars
function errorHandler(err, req, res, next) {
  if (err instanceof AppError) {
    return res.status(err.status).json({ error: { code: err.code, message: err.message, details: err.details } });
  }
  if (err.code && PG_ERRORS[err.code]) {
    const [status, code, message] = PG_ERRORS[err.code];
    return res.status(status).json({
      error: { code, message, details: env.nodeEnv === 'production' ? undefined : err.constraint || err.detail },
    });
  }
  console.error(err);
  res.status(500).json({ error: { code: 'INTERNAL_ERROR', message: 'Lỗi máy chủ' } });
}

module.exports = { notFound, errorHandler };