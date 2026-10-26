const router = require('express').Router();
const { z } = require('zod');
const validate = require('../../common/validate');
const { ok } = require('../../common/response');
const { requireAuth } = require('../../middleware/auth');
const quiz = require('./quiz.service');

const answersSchema = z.array(z.object({
  questionId: z.coerce.number().int().positive(),
  optionKey: z.string().min(1).max(80),
})).max(100);

const postParams = z.object({ postId: z.guid() });

// Bộ câu hỏi đang phát hành (QuizIntro, HabitQuiz). mode=room dùng cách hỏi cho phòng.
router.get('/questionnaire/current',
  validate({ query: z.object({ mode: z.enum(['personal', 'room']).default('personal') }) }),
  async (req, res) => {
    ok(res, await quiz.getQuestionnaire(req.valid.query.mode));
  });

// Tiến độ + câu trả lời bài test của tôi
router.get('/me/lifestyle-test', requireAuth, async (req, res) => {
  ok(res, await quiz.getMyTest(req.user.userId));
});

router.put('/me/lifestyle-answers', requireAuth,
  validate({
    body: z.object({
      answers: answersSchema.default([]),
      currentStep: z.coerce.number().int().min(1).optional(),
      completed: z.boolean().default(false),
    }),
  }),
  async (req, res) => {
    ok(res, await quiz.saveMyAnswers(req.user.userId, req.valid.body));
  });

// Bài test của phòng (bước 6 đăng bài) - chỉ chủ bài đăng
router.get('/room-posts/:postId/lifestyle-answers', requireAuth,
  validate({ params: postParams }),
  async (req, res) => {
    const { postId } = req.valid.params;
    await quiz.assertPostOwner(postId, req.user.userId);
    ok(res, await quiz.getPostTest(postId));
  });

router.put('/room-posts/:postId/lifestyle-answers', requireAuth,
  validate({ params: postParams, body: z.object({ answers: answersSchema.min(1) }) }),
  async (req, res) => {
    ok(res, await quiz.savePostAnswers(req.valid.params.postId, req.user.userId, req.valid.body));
  });

module.exports = router;
