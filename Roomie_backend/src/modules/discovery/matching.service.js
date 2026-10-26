const db = require('../../db');
const AppError = require('../../common/AppError');

// Thuật toán tương thích (xem ghi chú D trong migrations/001_init.sql):
//   Mỗi câu:   similarity = 1 - |điểm_A - điểm_B| / (điểm_max - điểm_min)
//   Mỗi nhóm:  trung bình có trọng số (lifestyle_questions.weight) của các câu trong nhóm
//   Tổng:      trung bình có trọng số của tất cả các câu mà CẢ HAI bên đã trả lời
// Chỉ dùng bộ câu hỏi đang phát hành: question_id của version khác sẽ không khớp.

const LEVELS = [
  [85, 'Rất phù hợp', 'Hai bên có nhịp sống rất gần nhau và dễ thống nhất quy tắc chung.'],
  [70, 'Khá phù hợp', 'Phần lớn thói quen giống nhau, chỉ cần trao đổi thêm vài điểm.'],
  [50, 'Phù hợp một phần', 'Có một số khác biệt, nên nói rõ quy tắc trước khi ở chung.'],
  [0, 'Ít phù hợp', 'Nhiều thói quen khác nhau, cần cân nhắc kỹ trước khi ở chung.'],
];

function levelOf(percent) {
  if (percent == null) return { level: null, tagline: 'Chưa đủ dữ liệu để so sánh.' };
  const [, level, tagline] = LEVELS.find(([min]) => percent >= min);
  return { level, tagline };
}

// similarity -> trạng thái hiển thị từng câu
function statusOf(similarity) {
  if (similarity == null) return 'unknown';   // một bên chưa trả lời
  if (similarity >= 1) return 'match';        // giống hệt
  if (similarity >= 0.5) return 'close';      // lệch 1 mức
  return 'discuss';                           // khác nhiều, cần trao đổi
}

function similarity(a, b, min, max) {
  if (a == null || b == null) return null;
  return 1 - Math.abs(a - b) / (max - min || 1);
}

// rows: mỗi câu có seeker_score + <side>_score; side = 'room' | 'host'
function compare(rows, side) {
  const groups = new Map();
  let sum = 0;
  let weightSum = 0;
  const criteria = rows.map((r) => {
    const sim = similarity(r.seeker_score, r[`${side}_score`], r.min_score, r.max_score);
    const g = groups.get(r.criterion_key)
      || { key: r.criterion_key, name: r.criterion_name, order: r.criterion_order, sum: 0, weight: 0 };
    if (sim != null) {
      g.sum += sim * r.weight;
      g.weight += r.weight;
      sum += sim * r.weight;
      weightSum += r.weight;
    }
    groups.set(r.criterion_key, g);
    return {
      questionKey: r.question_key,
      criterionKey: r.criterion_key,
      text: side === 'room' ? (r.room_question_text || r.question_text) : r.question_text,
      status: statusOf(sim),
      similarity: sim == null ? null : Math.round(sim * 100) / 100,
      weight: r.weight,
      seekerAnswer: r.seeker_label,
      otherAnswer: r[`${side}_label`],
    };
  });

  const overall = weightSum ? Math.round((sum / weightSum) * 100) : null;
  // Điểm chung nổi bật: các câu giống hệt, ưu tiên câu có trọng số cao
  const highlights = criteria
    .filter((c) => c.status === 'match')
    .sort((a, b) => b.weight - a.weight)
    .slice(0, 4)
    .map((c) => c.seekerAnswer);

  return {
    overall,
    ...levelOf(overall),
    answeredBoth: criteria.filter((c) => c.status !== 'unknown').length,
    groups: [...groups.values()]
      .sort((a, b) => a.order - b.order)
      .map((g) => ({ key: g.key, name: g.name, percent: g.weight ? Math.round((g.sum / g.weight) * 100) : null })),
    criteria,
    highlights,
  };
}

const DETAIL_SQL = `
WITH v AS (SELECT questionnaire_version_id FROM questionnaire_versions WHERE status = 'published'),
q AS (
  SELECT lq.question_id, lq.question_key, lq.question_text, lq.room_question_text, lq.weight,
         lq.display_order, c.criterion_key, c.criterion_name, c.display_order AS criterion_order,
         MIN(o.score) AS min_score, MAX(o.score) AS max_score
    FROM lifestyle_questions lq
    JOIN v USING (questionnaire_version_id)
    JOIN lifestyle_criteria c ON c.criterion_id = lq.criterion_id
    JOIN lifestyle_question_options o ON o.question_id = lq.question_id AND o.is_active
   WHERE lq.is_active
   GROUP BY lq.question_id, c.criterion_id)
SELECT q.*,
       so.score AS seeker_score, so.option_label AS seeker_label,
       ro.score AS room_score,   COALESCE(ro.room_option_label, ro.option_label) AS room_label,
       ho.score AS host_score,   ho.option_label AS host_label
  FROM q
  LEFT JOIN user_lifestyle_answers sa ON sa.question_id = q.question_id AND sa.user_id = $1
  LEFT JOIN lifestyle_question_options so ON so.question_id = q.question_id AND so.option_key = sa.answer_value->>'option_key'
  LEFT JOIN post_lifestyle_answers pa ON pa.question_id = q.question_id AND pa.post_id = $2
  LEFT JOIN lifestyle_question_options ro ON ro.question_id = q.question_id AND ro.option_key = pa.answer_value->>'option_key'
  LEFT JOIN user_lifestyle_answers ha ON ha.question_id = q.question_id AND ha.user_id = $3
  LEFT JOIN lifestyle_question_options ho ON ho.question_id = q.question_id AND ho.option_key = ha.answer_value->>'option_key'
 ORDER BY q.display_order`;

// GET /room-posts/:postId/compatibility
async function getCompatibility(seekerId, postId) {
  const { rows: posts } = await db.query(
    `SELECT rp.post_id, rp.owner_id, rp.title, rp.address_text, rp.moderation_status,
            COALESCE(u.display_name, u.full_name) AS owner_name, u.avatar_url AS owner_avatar,
            (SELECT pi.image_url FROM post_images pi WHERE pi.post_id = rp.post_id
              ORDER BY pi.is_cover DESC, pi.display_order LIMIT 1) AS cover_image_url
       FROM room_posts rp
       JOIN users u ON u.user_id = rp.owner_id
      WHERE rp.post_id = $1 AND rp.deleted_at IS NULL`, [postId]);
  const post = posts[0];
  if (!post || (post.moderation_status !== 'approved' && post.owner_id !== seekerId)) {
    throw AppError.notFound('Không tìm thấy bài đăng');
  }

  const { rows } = await db.query(DETAIL_SQL, [seekerId, postId, post.owner_id]);
  const seekerAnswered = rows.filter((r) => r.seeker_score != null).length;

  return {
    postId,
    post: { title: post.title, addressText: post.address_text, coverImageUrl: post.cover_image_url },
    owner: { userId: post.owner_id, displayName: post.owner_name, avatarUrl: post.owner_avatar },
    isOwnPost: post.owner_id === seekerId,
    totalQuestions: rows.length,
    seekerAnsweredCount: seekerAnswered,
    room: compare(rows, 'room'),
    host: compare(rows, 'host'),
  };
}

// Điểm % cho nhiều phòng một lúc (thẻ phòng ở Home / Tìm kiếm / Bản đồ / Đã lưu).
// Trả về Map<postId, { room, host, match }>; match = điểm với phòng, chưa có thì lấy điểm với chủ phòng.
async function scorePosts(seekerId, postIds, client = db) {
  const result = new Map();
  if (!seekerId || !postIds.length) return result;
  const { rows } = await client.query(`
    WITH v AS (SELECT questionnaire_version_id FROM questionnaire_versions WHERE status = 'published'),
    q AS (
      SELECT lq.question_id, lq.weight, MIN(o.score) AS mn, MAX(o.score) AS mx
        FROM lifestyle_questions lq
        JOIN v USING (questionnaire_version_id)
        JOIN lifestyle_question_options o ON o.question_id = lq.question_id AND o.is_active
       WHERE lq.is_active
       GROUP BY lq.question_id),
    s AS (
      SELECT q.*, so.score AS s_score
        FROM q
        JOIN user_lifestyle_answers sa ON sa.question_id = q.question_id AND sa.user_id = $1
        JOIN lifestyle_question_options so ON so.question_id = q.question_id
                                          AND so.option_key = sa.answer_value->>'option_key'),
    room AS (
      SELECT pa.post_id,
             SUM((1 - ABS(s.s_score - po.score)::numeric / GREATEST(s.mx - s.mn, 1)) * s.weight) / SUM(s.weight) AS sim
        FROM s
        JOIN post_lifestyle_answers pa ON pa.question_id = s.question_id AND pa.post_id = ANY($2::uuid[])
        JOIN lifestyle_question_options po ON po.question_id = s.question_id
                                          AND po.option_key = pa.answer_value->>'option_key'
       GROUP BY pa.post_id),
    host AS (
      SELECT rp.post_id,
             SUM((1 - ABS(s.s_score - ho.score)::numeric / GREATEST(s.mx - s.mn, 1)) * s.weight) / SUM(s.weight) AS sim
        FROM room_posts rp
        JOIN user_lifestyle_answers ha ON ha.user_id = rp.owner_id
        JOIN s ON s.question_id = ha.question_id
        JOIN lifestyle_question_options ho ON ho.question_id = s.question_id
                                          AND ho.option_key = ha.answer_value->>'option_key'
       WHERE rp.post_id = ANY($2::uuid[])
       GROUP BY rp.post_id)
    SELECT p.id AS post_id, ROUND(room.sim * 100) AS room, ROUND(host.sim * 100) AS host
      FROM unnest($2::uuid[]) AS p(id)
      LEFT JOIN room ON room.post_id = p.id
      LEFT JOIN host ON host.post_id = p.id`, [seekerId, postIds]);
  for (const r of rows) {
    result.set(r.post_id, { room: r.room, host: r.host, match: r.room ?? r.host ?? null });
  }
  return result;
}

module.exports = { getCompatibility, scorePosts };
