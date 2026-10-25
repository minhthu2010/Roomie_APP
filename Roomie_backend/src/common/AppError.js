class AppError extends Error {
  constructor(status, code, message, details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

AppError.badRequest = (msg, details) => new AppError(400, 'BAD_REQUEST', msg, details);
AppError.unauthorized = (msg = 'Bạn cần đăng nhập') => new AppError(401, 'UNAUTHORIZED', msg);
AppError.forbidden = (msg = 'Bạn không có quyền thực hiện thao tác này') => new AppError(403, 'FORBIDDEN', msg);
AppError.notFound = (msg = 'Không tìm thấy dữ liệu') => new AppError(404, 'NOT_FOUND', msg);
AppError.conflict = (msg) => new AppError(409, 'CONFLICT', msg);
AppError.validation = (details) => new AppError(422, 'VALIDATION_ERROR', 'Dữ liệu không hợp lệ', details);

module.exports = AppError;