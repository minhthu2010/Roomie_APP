const db = require('../../db');
const AppError = require('../../common/AppError');

// Hiện chỉ hỗ trợ câu hỏi single_choice: answer_value = {"option_key": "..."}

async function getPublishedVersion(client = db) {
  const { rows } = await client.query(
    `SELECT questionnaire_version_id, version_key, version_name
       FROM questionnaire_versions WHERE status = 'published'`);
  if (!rows[0]) throw AppError.notFound('Chưa có bộ câu hỏi nào được phát hành');
  return rows[0];
}

// mode = 'personal' (bài test cá nhân) | 'room' (bài test của phòng, bước 6 đăng bài)
async function getQuestionnaire(mode = 'personal', client = db) {
  const version = await getPublishedVersion(client);
  const versionId = version.questionnaire_version_id;

  const { rows: criteria } = await client.query(
    `SELECT DISTINCT c.criterion_key, c.criterion_name, c.description, c.display_order
       FROM lifestyle_criteria c
       JOIN lifestyle_questions q ON q.criterion_id = c.criterion_id
      WHERE q.questionnaire_version_id = $1 AND q.is_active
      ORDER BY c.display_order`, [versionId]);

  const { rows: questions } = await client.query(
    `SELECT q.question_id, q.question_key, q.question_text, q.room_question_text, q.description,
            q.answer_type, q.is_required, q.display_order, c.criterion_key,
            COALESCE(json_agg(json_build_object(
                'optionKey', o.option_key,
                'label', o.option_label,
                'roomLabel', o.room_option_label,
                'score', o.score) ORDER BY o.display_order)
              FILTER (WHERE o.option_id IS NOT NULL), '[]') AS options
       FROM lifestyle_questions q
       LEFT JOIN lifestyle_criteria c ON c.criterion_id = q.criterion_id
       LEFT JOIN lifestyle_question_options o ON o.question_id = q.question_id AND o.is_active
      WHERE q.questionnaire_version_id = $1 AND q.is_active
      GROUP BY q.question_id, c.criterion_key
      ORDER BY q.display_order`, [versionId]);

  const isRoom = mode === 'room';
  return {
    versionId,
    versionKey: version.version_key,
    versionName: version.version_name,
    mode,
    totalQuestions: questions.length,
    criteria: criteria.map((c) => ({
      key: c.criterion_key, name: c.criterion_name, description: c.description,
    })),
    questions: questions.map((q) => ({
      questionId: q.question_id,
      questionKey: q.question_key,
      criterionKey: q.criterion_key,
      text: (isRoom && q.room_question_text) || q.question_text,
      description: q.description,
      answerType: q.answer_type,
      isRequired: q.is_required,
      displayOrder: q.display_order,
      options: q.options.map((o) => ({
        optionKey: o.optionKey,
        label: (isRoom && o.roomLabel) || o.label,
        score: o.score,
      })),
    })),
  };
}

// Kiểm tra từng câu trả lời thuộc bộ câu hỏi đang phát hành và optionKey hợp lệ.
async function checkAnswers(versionId, answers, client) {
  const ids = answers.map((a) => a.questionId);
  if (new Set(ids).size !== ids.length) {
    throw AppError.badRequest('Mỗi câu hỏi chỉ được trả lời một lần trong một lần gửi');
  }
  const { rows } = await client.query(
    `SELECT q.question_id, array_agg(o.option_key) AS option_keys
       FROM lifestyle_questions q
       JOIN lifestyle_question_options o ON o.question_id = q.question_id AND o.is_active
      WHERE q.questionnaire_version_id = $1 AND q.is_active AND q.question_id = ANY($2::bigint[])
      GROUP BY q.question_id`, [versionId, ids]);
  const validKeys = new Map(rows.map((r) => [r.question_id, r.option_keys]));

  const details = [];
  answers.forEach((a, i) => {
    const keys = validKeys.get(a.questionId);
    if (!keys) details.push({ field: `answers.${i}.questionId`, message: 'Câu hỏi không thuộc bộ câu hỏi hiện tại' });
    else if (!keys.includes(a.optionKey)) details.push({ field: `answers.${i}.optionKey`, message: 'Lựa chọn không hợp lệ' });
  });
  if (details.length) throw AppError.validation(details);
}

// Số câu bắt buộc chưa trả lời; tableName/ownerColumn chọn bảng của user hoặc của phòng.
async function countMissingRequired(versionId, tableName, ownerColumn, ownerId, client) {
  const { rows } = await client.query(
    `SELECT COUNT(*) AS missing
       FROM lifestyle_questions q
      WHERE q.questionnaire_version_id = $1 AND q.is_active AND q.is_required
        AND NOT EXISTS (SELECT 1 FROM ${tableName} a
                         WHERE a.questionnaire_version_id = q.questionnaire_version_id
                           AND a.question_id = q.question_id AND a.${ownerColumn} = $2)`,
    [versionId, ownerId]);
  return rows[0].missing;
}

async function listAnswers(versionId, tableName, ownerColumn, ownerId, client = db) {
  const { rows } = await client.query(
    `SELECT a.question_id, q.question_key, a.answer_value->>'option_key' AS option_key, a.updated_at
       FROM ${tableName} a
       JOIN lifestyle_questions q ON q.question_id = a.question_id
      WHERE a.questionnaire_version_id = $1 AND a.${ownerColumn} = $2
      ORDER BY q.display_order`, [versionId, ownerId]);
  return rows.map((r) => ({
    questionId: r.question_id, questionKey: r.question_key, optionKey: r.option_key, updatedAt: r.updated_at,
  }));
}

async function totalQuestions(versionId, client = db) {
  const { rows } = await client.query(
    `SELECT COUNT(*) AS total FROM lifestyle_questions
      WHERE questionnaire_version_id = $1 AND is_active`, [versionId]);
  return rows[0].total;
}

// ---------- Bài test cá nhân ----------

async function getMyTest(userId) {
  const version = await getPublishedVersion();
  const versionId = version.questionnaire_version_id;
  const [answers, total, progress] = await Promise.all([
    listAnswers(versionId, 'user_lifestyle_answers', 'user_id', userId),
    totalQuestions(versionId),
    db.query(
      `SELECT current_step, is_completed, started_at, completed_at, updated_at
         FROM user_lifestyle_test_progress
        WHERE user_id = $1 AND questionnaire_version_id = $2`, [userId, versionId]),
  ]);
  const p = progress.rows[0];
  return {
    versionId,
    totalQuestions: total,
    answeredCount: answers.length,
    currentStep: p ? p.current_step : 1,
    isStarted: Boolean(p),
    isCompleted: p ? p.is_completed : false,
    startedAt: p ? p.started_at : null,
    completedAt: p ? p.completed_at : null,
    updatedAt: p ? p.updated_at : null,
    answers,
  };
}

// Lưu theo từng câu (app gọi mỗi lần bấm "Tiếp") hoặc lưu cả bài một lần.
// completed = true chỉ được chấp nhận khi đã trả lời đủ câu bắt buộc.
async function saveMyAnswers(userId, { answers, currentStep, completed }) {
  await db.withTransaction(async (client) => {
    const version = await getPublishedVersion(client);
    const versionId = version.questionnaire_version_id;
    await checkAnswers(versionId, answers, client);

    for (const a of answers) {
      await client.query(
        `INSERT INTO user_lifestyle_answers (user_id, questionnaire_version_id, question_id, answer_value)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (user_id, questionnaire_version_id, question_id)
         DO UPDATE SET answer_value = EXCLUDED.answer_value`,
        [userId, versionId, a.questionId, { option_key: a.optionKey }]);
    }

    const total = await totalQuestions(versionId, client);
    // Không gửi currentStep (VD sửa riêng 1 câu) thì giữ nguyên bước đang làm.
    const step = currentStep == null ? null : Math.min(Math.max(currentStep, 1), Math.max(total, 1));

    let isCompleted = false;
    if (completed) {
      const missing = await countMissingRequired(versionId, 'user_lifestyle_answers', 'user_id', userId, client);
      if (missing > 0) throw AppError.validation([{ field: 'answers', message: `Còn ${missing} câu bắt buộc chưa trả lời` }]);
      isCompleted = true;
    }

    // Đã hoàn thành rồi thì sửa lại câu trả lời vẫn giữ trạng thái hoàn thành.
    await client.query(
      `INSERT INTO user_lifestyle_test_progress
            (user_id, questionnaire_version_id, current_step, is_completed, completed_at)
       VALUES ($1, $2, COALESCE($3::int, 1), $4, CASE WHEN $4 THEN NOW() END)
       ON CONFLICT (user_id, questionnaire_version_id) DO UPDATE SET
            current_step = COALESCE($3::int, user_lifestyle_test_progress.current_step),
            is_completed = user_lifestyle_test_progress.is_completed OR EXCLUDED.is_completed,
            completed_at = CASE
                WHEN EXCLUDED.is_completed THEN NOW()
                ELSE user_lifestyle_test_progress.completed_at END`,
      [userId, versionId, step, isCompleted]);
  });
  return getMyTest(userId);
}

// ---------- Bài test của phòng (bước 6 đăng bài) ----------

async function assertPostOwner(postId, userId, client = db) {
  const { rows } = await client.query(
    'SELECT owner_id FROM room_posts WHERE post_id = $1 AND deleted_at IS NULL', [postId]);
  if (!rows[0]) throw AppError.notFound('Không tìm thấy bài đăng');
  if (rows[0].owner_id !== userId) throw AppError.forbidden();
}

async function getPostTest(postId, client = db) {
  const version = await getPublishedVersion(client);
  const versionId = version.questionnaire_version_id;
  const [answers, total, missing] = await Promise.all([
    listAnswers(versionId, 'post_lifestyle_answers', 'post_id', postId, client),
    totalQuestions(versionId, client),
    countMissingRequired(versionId, 'post_lifestyle_answers', 'post_id', postId, client),
  ]);
  return { postId, versionId, totalQuestions: total, answeredCount: answers.length, isCompleted: missing === 0, answers };
}

async function savePostAnswers(postId, userId, { answers }) {
  await db.withTransaction(async (client) => {
    await assertPostOwner(postId, userId, client);
    const version = await getPublishedVersion(client);
    const versionId = version.questionnaire_version_id;
    await checkAnswers(versionId, answers, client);

    for (const a of answers) {
      await client.query(
        `INSERT INTO post_lifestyle_answers (post_id, questionnaire_version_id, question_id, answer_value)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (post_id, questionnaire_version_id, question_id)
         DO UPDATE SET answer_value = EXCLUDED.answer_value`,
        [postId, versionId, a.questionId, { option_key: a.optionKey }]);
    }
  });
  return getPostTest(postId);
}

// B gọi hàm này trước khi gửi duyệt bài: true nếu phòng đã làm đủ bài test.
async function isPostTestComplete(postId, client = db) {
  const version = await getPublishedVersion(client);
  const missing = await countMissingRequired(
    version.questionnaire_version_id, 'post_lifestyle_answers', 'post_id', postId, client);
  return missing === 0;
}

module.exports = {
  getQuestionnaire,
  getMyTest,
  saveMyAnswers,
  assertPostOwner,
  getPostTest,
  savePostAnswers,
  isPostTestComplete,
};
