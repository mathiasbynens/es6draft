/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, fail
} = Assert;

// stub for Assert.assertSyntaxError
function assertSyntaxError(source) {
  return assertThrows(() => parseModule(source), SyntaxError);
}

function assertNoSyntaxError(source) {
  try {
    parseModule(source);
  } catch (e) {
    fail `Expected no syntax error, but got ${e}`
  }
}

// Test NoLineTerminator restriction
assertNoSyntaxError(`
  module
  let a;
`);