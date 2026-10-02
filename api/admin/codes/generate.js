const main = require('../../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/codes/generate';
  return main(req, res);
};
