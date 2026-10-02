const main = require('../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/admin/export-csv';
  return main(req, res);
};
