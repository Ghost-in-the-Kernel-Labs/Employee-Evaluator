const { sumOfPrimes } = require("./examples");
const path = require("path");
require("./view");

function total(n) {
  return n > 0 ? sumOfPrimes(n) : 0;
}

module.exports = { total };
