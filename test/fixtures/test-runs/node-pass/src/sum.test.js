const test = require("node:test");
const assert = require("node:assert");
const { sum } = require("./sum");

test("sum adds", () => assert.equal(sum(1, 2), 3));
test("sum adds negatives", () => assert.equal(sum(-1, -2), -3));
