const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const { notFound, errorHandler } = require('./middleware/errorHandler');

const app = express();

app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use(morgan('dev'));
app.use('/uploads', express.static(path.join(__dirname, '..', 'uploads')));

app.use('/api/v1/health', require('./modules/health/health.routes'));
app.use('/api/v1', require('./modules/accounts'));   // A
app.use('/api/v1', require('./modules/posts'));      // B
app.use('/api/v1', require('./modules/discovery'));  // C

app.use(notFound);
app.use(errorHandler);

module.exports = app;