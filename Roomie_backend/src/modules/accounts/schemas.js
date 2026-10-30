const { z } = require('zod');

// Dùng chung cho đổi mật khẩu và đặt lại mật khẩu (UI: "Ít nhất 8 ký tự", "Có chữ hoa, chữ thường và số")
const strongPassword = z.string().min(8, 'Mật khẩu mới tối thiểu 8 ký tự').max(72)
  .regex(/[a-z]/, 'Mật khẩu mới cần có chữ thường')
  .regex(/[A-Z]/, 'Mật khẩu mới cần có chữ hoa')
  .regex(/\d/, 'Mật khẩu mới cần có chữ số');

const email = z.string('Vui lòng nhập email').trim().toLowerCase().max(254).pipe(z.email('Email không hợp lệ'));
const otpCode = z.string().trim().regex(/^\d{6}$/, 'Mã xác thực gồm 6 chữ số');

module.exports = { strongPassword, email, otpCode };
