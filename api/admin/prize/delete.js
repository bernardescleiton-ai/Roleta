const main = require('../../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/prize/delete';
  return main(req, res);
};
