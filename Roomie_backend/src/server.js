const env = require('./config/env');
const app = require('./app');

app.listen(env.port, () => {
  console.log(`Roomie API chạy tại http://localhost:${env.port}/api/v1`);
});