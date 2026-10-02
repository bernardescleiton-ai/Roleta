const main = require('./index.js');
module.exports = (req, res) => {
  req.targetRoute = '/validate-code';
  return main(req, res);
};
