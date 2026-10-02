const main = require('../../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/spin/update';
  return main(req, res);
};
