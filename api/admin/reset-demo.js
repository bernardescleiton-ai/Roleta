const main = require('../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/reset-demo';
  return main(req, res);
};
