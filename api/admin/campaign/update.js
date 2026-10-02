const main = require('../../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/campaign/update';
  return main(req, res);
};
