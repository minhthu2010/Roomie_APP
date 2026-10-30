const nodemailer = require('nodemailer');
const env = require('../../config/env');

const transporter = env.smtp.host
  ? nodemailer.createTransport({
    host: env.smtp.host,
    port: env.smtp.port,
    secure: env.smtp.port === 465,
    auth: { user: env.smtp.user, pass: env.smtp.pass },
  })
  : null;

const PURPOSE_TEXT = {
  signup: 'xác thực đăng ký tài khoản',
  password_reset: 'đặt lại mật khẩu',
  verify_email: 'xác minh email',
  change_email: 'đổi email',
};

async function sendOtpEmail(to, code, purpose, expiresMinutes) {
  const action = PURPOSE_TEXT[purpose] || 'xác thực';
  if (!transporter) {
    if (env.nodeEnv === 'production') throw new Error('Chưa cấu hình SMTP để gửi email');
    // Chỉ khi dev và chưa cấu hình SMTP: in mã ra console để test.
    console.log(`[DEV] OTP ${action} cho ${to}: ${code}`);
    return;
  }
  await transporter.sendMail({
    from: env.smtp.from,
    to,
    subject: `Roomie - Mã ${action}: ${code}`,
    text: `Mã ${action} của bạn là ${code}. Mã có hiệu lực trong ${expiresMinutes} phút.\n`
      + 'Nếu bạn không yêu cầu, hãy bỏ qua email này.',
    html: `<p>Mã ${action} Roomie của bạn là:</p>
           <p style="font-size:28px;font-weight:bold;letter-spacing:6px">${code}</p>
           <p>Mã có hiệu lực trong ${expiresMinutes} phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>`,
  });
}

module.exports = { sendOtpEmail };
