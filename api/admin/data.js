const main = require('../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/data';
  return main(req, res);
};
