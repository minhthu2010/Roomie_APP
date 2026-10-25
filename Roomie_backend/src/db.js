const { Pool, types } = require('pg');
const env = require('./config/env');

// pg mặc định trả BIGINT (20) và NUMERIC (1700) về dạng chuỗi.
// Tiền VND và id nhỏ hơn 2^53 nên đổi sang Number là an toàn.
types.setTypeParser(20, (v) => Number(v));
types.setTypeParser(1700, (v) => parseFloat(v));

const pool = new Pool({ connectionString: env.databaseUrl, max: 10 });

// Chạy nhiều câu trong một transaction:
// await withTransaction(async (client) => { await client.query(...); });
async function withTransaction(fn) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

module.exports = { pool, query: (text, params) => pool.query(text, params), withTransaction };