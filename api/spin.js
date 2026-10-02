const main = require('./index.js');
module.exports = (req, res) => {
  req.targetRoute = '/spin';
  return main(req, res);
};
