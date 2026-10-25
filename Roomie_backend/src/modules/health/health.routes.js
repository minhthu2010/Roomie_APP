const router = require('express').Router();
const db = require('../../db');
const { ok } = require('../../common/response');

router.get('/', async (req, res) => {
  const { rows } = await db.query('SELECT NOW() AS db_time');
  ok(res, { status: 'ok', dbTime: rows[0].db_time });
});

module.exports = router;