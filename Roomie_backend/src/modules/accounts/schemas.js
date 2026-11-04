const { z } = require('zod');

// Dùng chung cho đăng ký, đổi mật khẩu và đặt lại mật khẩu (UI: "Ít nhất 8 ký tự", "Có chữ hoa, chữ thường và số")
const strongPassword = z.string('Vui lòng nhập mật khẩu').min(8, 'Mật khẩu tối thiểu 8 ký tự')
  .max(72, 'Mật khẩu tối đa 72 ký tự')
  .regex(/[a-z]/, 'Mật khẩu cần có chữ thường')
  .regex(/[A-Z]/, 'Mật khẩu cần có chữ hoa')
  .regex(/\d/, 'Mật khẩu cần có chữ số');

const email = z.string('Vui lòng nhập email').trim().toLowerCase().max(254).pipe(z.email('Email không hợp lệ'));
const otpCode = z.string().trim().regex(/^\d{6}$/, 'Mã xác thực gồm 6 chữ số');

module.exports = { strongPassword, email, otpCode };
