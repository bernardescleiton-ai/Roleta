const main = require('../../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/prize/save';
  return main(req, res);
};
