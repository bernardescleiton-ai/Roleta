const main = require('../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/login';
  return main(req, res);
};
