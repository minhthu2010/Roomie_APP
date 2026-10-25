const AppError = require('./AppError');

// router.post('/x', validate({ body: schema }), handler)
function validate(schemas) {
  return (req, res, next) => {
    req.valid = {};
    for (const part of ['body', 'query', 'params']) {
      if (!schemas[part]) continue;
      const result = schemas[part].safeParse(req[part]);
      if (!result.success) {
        const details = result.error.issues.map((i) => ({ field: i.path.join('.'), message: i.message }));
        return next(AppError.validation(details));
      }
      req.valid[part] = result.data;
    }
    next();
  };
}

module.exports = validate;