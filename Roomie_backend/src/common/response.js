function ok(res, data, status = 200) {
  return res.status(status).json({ data });
}

// ?page=1&limit=20  ->  { limit, offset, page }
function getPaging(query, maxLimit = 50) {
  const page = Math.max(1, parseInt(query.page, 10) || 1);
  const limit = Math.min(maxLimit, Math.max(1, parseInt(query.limit, 10) || 20));
  return { page, limit, offset: (page - 1) * limit };
}

function paged(res, items, { page, limit }, total) {
  return res.json({ data: items, meta: { page, limit, total } });
}

module.exports = { ok, getPaging, paged };