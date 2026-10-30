const fs = require('fs/promises');
const path = require('path');
const bcrypt = require('bcryptjs');
const db = require('../../db');
const AppError = require('../../common/AppError');
const tokens = require('./tokens');

const UPLOAD_ROOT = path.join(__dirname, '..', '..', '..', 'uploads');

// camelCase (JSON) -> cột DB, chỉ các trường được phép sửa
const USER_FIELDS = {
  fullName: 'full_name',
  displayName: 'display_name',
  phoneNumber: 'phone_number',
};
const PROFILE_FIELDS = {
  bio: 'bio',
  gender: 'gender',
  birthYear: 'birth_year',
  occupation: 'occupation',
  schoolOrWorkplace: 'school_or_workplace',
  landmarkId: 'landmark_id',
  livingAreaText: 'living_area_text',
  livingProvinceCode: 'living_province_code',
  livingWardCode: 'living_ward_code',
  budgetMin: 'budget_min',
  budgetMax: 'budget_max',
  moveInDate: 'move_in_date',
  preferredGender: 'preferred_gender',
  onboardingGoal: 'onboarding_goal',
};

async function getMyProfile(userId) {
  const { rows } = await db.query(
    `SELECT u.user_id, u.email, u.phone_number, u.full_name, u.display_name, u.avatar_url,
            u.email_verified_at, u.created_at, u.password_hash IS NOT NULL AS has_password,
            up.bio, up.gender, up.birth_year, up.occupation, up.school_or_workplace, up.landmark_id,
            up.living_area_text, up.living_province_code, up.living_ward_code,
            up.budget_min, up.budget_max, to_char(up.move_in_date, 'YYYY-MM-DD') AS move_in_date,
            COALESCE(up.preferred_gender, 'any') AS preferred_gender, up.onboarding_goal, up.profile_completed_at,
            l.name AS landmark_name, l.short_name AS landmark_short_name,
            p.province_name AS living_province_name, w.ward_name AS living_ward_name
       FROM users u
       LEFT JOIN user_profiles up ON up.user_id = u.user_id
       LEFT JOIN landmarks l ON l.landmark_id = up.landmark_id
       LEFT JOIN provinces p ON p.province_code = up.living_province_code
       LEFT JOIN wards w ON w.ward_code = up.living_ward_code
      WHERE u.user_id = $1`, [userId]);
  const u = rows[0];
  if (!u) throw AppError.notFound('Không tìm thấy tài khoản');
  return {
    userId: u.user_id,
    email: u.email,
    phoneNumber: u.phone_number,
    fullName: u.full_name,
    displayName: u.display_name || u.full_name,
    avatarUrl: u.avatar_url,
    emailVerified: u.email_verified_at !== null,
    hasPassword: u.has_password,
    memberSince: u.created_at,
    bio: u.bio,
    gender: u.gender,
    birthYear: u.birth_year,
    occupation: u.occupation,
    schoolOrWorkplace: u.school_or_workplace,
    landmarkId: u.landmark_id,
    landmarkName: u.landmark_name,
    landmarkShortName: u.landmark_short_name,
    livingAreaText: u.living_area_text,
    // "Khu vực muốn tìm phòng": tỉnh bắt buộc khi đã chọn, phường có thể null (= cả tỉnh)
    livingProvinceCode: u.living_province_code,
    livingProvinceName: u.living_province_name,
    livingWardCode: u.living_ward_code,
    livingWardName: u.living_ward_name,
    budgetMin: u.budget_min,
    budgetMax: u.budget_max,
    moveInDate: u.move_in_date,
    preferredGender: u.preferred_gender,
    onboardingGoal: u.onboarding_goal,
    profileCompleted: u.profile_completed_at !== null,
  };
}

function buildSet(input, fields, startIndex) {
  const sets = [];
  const values = [];
  for (const [key, column] of Object.entries(fields)) {
    if (input[key] === undefined) continue;
    values.push(input[key]);
    sets.push(`${column} = $${startIndex + values.length}`);
  }
  return { sets, values };
}

// Khu vực muốn tìm phòng: phường phải thuộc tỉnh đã chọn; đổi tỉnh mà không gửi phường thì bỏ phường cũ.
// Đồng thời ghi living_area_text ("Phường Bàn Cờ, Thành phố Hồ Chí Minh") để chỗ khác hiển thị gọn.
async function normalizeArea(input, c) {
  if (input.livingProvinceCode === undefined && input.livingWardCode === undefined) return;
  if (input.livingWardCode) {
    const { rows } = await c.query(
      `SELECT w.ward_name, w.province_code, p.province_name
         FROM wards w JOIN provinces p ON p.province_code = w.province_code WHERE w.ward_code = $1`,
      [input.livingWardCode]);
    if (!rows[0]) throw AppError.validation([{ field: 'livingWardCode', message: 'Phường/xã không tồn tại' }]);
    if (input.livingProvinceCode && input.livingProvinceCode !== rows[0].province_code) {
      throw AppError.validation([{ field: 'livingWardCode', message: 'Phường/xã không thuộc tỉnh/thành đã chọn' }]);
    }
    input.livingProvinceCode = rows[0].province_code;
    input.livingAreaText = `${rows[0].ward_name}, ${rows[0].province_name}`;
  } else if (input.livingProvinceCode) {
    const { rows } = await c.query('SELECT province_name FROM provinces WHERE province_code = $1',
      [input.livingProvinceCode]);
    if (!rows[0]) throw AppError.validation([{ field: 'livingProvinceCode', message: 'Tỉnh/thành không tồn tại' }]);
    input.livingWardCode = null;
    input.livingAreaText = rows[0].province_name;
  } else {
    // Gửi livingProvinceCode rỗng: xóa khu vực
    input.livingProvinceCode = null;
    input.livingWardCode = null;
    input.livingAreaText = null;
  }
}

// Trường/nơi làm chọn từ gợi ý: lưu landmark_id, tên hiển thị lấy theo landmark nếu app không gửi
async function normalizeLandmark(input, c) {
  if (!input.landmarkId) return;
  const { rows } = await c.query('SELECT name FROM landmarks WHERE landmark_id = $1 AND is_active', [input.landmarkId]);
  if (!rows[0]) throw AppError.validation([{ field: 'landmarkId', message: 'Trường/địa điểm không tồn tại' }]);
  if (input.schoolOrWorkplace === undefined) input.schoolOrWorkplace = rows[0].name;
}

// Sửa một phần hồ sơ (ProfileSetupActivity, màn Chỉnh sửa hồ sơ).
// completeSetup = true: đánh dấu đã thiết lập hồ sơ lần đầu.
async function updateMyProfile(userId, input) {
  await db.withTransaction(async (c) => {
    await normalizeArea(input, c);
    await normalizeLandmark(input, c);
    const user = buildSet(input, USER_FIELDS, 1);
    if (user.sets.length) {
      await c.query(`UPDATE users SET ${user.sets.join(', ')} WHERE user_id = $1`, [userId, ...user.values]);
    }

    await c.query('INSERT INTO user_profiles (user_id) VALUES ($1) ON CONFLICT DO NOTHING', [userId]);
    const profile = buildSet(input, PROFILE_FIELDS, 1);
    if (input.completeSetup) profile.sets.push('profile_completed_at = COALESCE(profile_completed_at, NOW())');
    if (profile.sets.length) {
      await c.query(`UPDATE user_profiles SET ${profile.sets.join(', ')} WHERE user_id = $1`,
        [userId, ...profile.values]);
    }
  });
  return getMyProfile(userId);
}

// file: kết quả multer (memoryStorage) đã kiểm tra là ảnh
async function updateAvatar(userId, file) {
  const ext = { 'image/jpeg': '.jpg', 'image/png': '.png', 'image/webp': '.webp' }[file.mimetype];
  const dir = path.join(UPLOAD_ROOT, 'avatars');
  await fs.mkdir(dir, { recursive: true });
  const fileName = `${userId}-${Date.now()}${ext}`;
  await fs.writeFile(path.join(dir, fileName), file.buffer);

  const { rows } = await db.query(
    `UPDATE users u SET avatar_url = $2 FROM users old
      WHERE u.user_id = $1 AND old.user_id = u.user_id
      RETURNING old.avatar_url AS previous`, [userId, `/uploads/avatars/${fileName}`]);
  // Xóa ảnh cũ do người dùng tự tải lên (không đụng ảnh seed hay ảnh Google)
  const previous = rows[0] && rows[0].previous;
  if (previous && previous.startsWith('/uploads/avatars/')) {
    await fs.unlink(path.join(UPLOAD_ROOT, 'avatars', path.basename(previous))).catch(() => {});
  }
  return getMyProfile(userId);
}

// Đăng xuất mọi thiết bị, cấp token mới cho thiết bị đang đổi mật khẩu.
async function changePassword(userId, { currentPassword, newPassword, deviceLabel }) {
  const { rows } = await db.query('SELECT password_hash FROM users WHERE user_id = $1', [userId]);
  const hash = rows[0] && rows[0].password_hash;
  // Tài khoản Google chưa có mật khẩu thì được đặt mật khẩu lần đầu
  if (hash && !(await bcrypt.compare(currentPassword || '', hash))) {
    throw new AppError(400, 'WRONG_PASSWORD', 'Mật khẩu hiện tại không đúng');
  }
  if (hash && await bcrypt.compare(newPassword, hash)) {
    throw new AppError(400, 'SAME_PASSWORD', 'Mật khẩu mới phải khác mật khẩu hiện tại');
  }
  const newHash = await bcrypt.hash(newPassword, 10);
  return db.withTransaction(async (c) => {
    await c.query('UPDATE users SET password_hash = $2 WHERE user_id = $1', [userId, newHash]);
    await tokens.revokeAllForUser(userId, c);
    return (await tokens.issueTokens(userId, deviceLabel, c)).tokens;
  });
}

module.exports = { getMyProfile, updateMyProfile, updateAvatar, changePassword };
