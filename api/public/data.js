const main = require('../index.js');
module.exports = (req, res) => {
  req.targetRoute = '/public/data';
  return main(req, res);
};
