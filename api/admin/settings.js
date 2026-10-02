const main = require('../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/settings';
  return main(req, res);
};
