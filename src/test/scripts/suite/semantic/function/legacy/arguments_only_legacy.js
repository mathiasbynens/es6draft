/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Similar to "arguments.js", with the only exception of adding a named parameter "arguments".

// basic test
{
  function f(a, arguments) {
    return [a, arguments[0], f.arguments[0]];
  }
  let [arg0, arg1, arg2] = f(0, [2]);
  assertSame(0, arg0);
  assertSame(2, arg1);
  assertSame(0, arg2);
}

// parameter map - assignment (1)
{
  function f(a, arguments) {
    a = 1;
    return [a, arguments[0], f.arguments[0]];
  }
  let [arg0, arg1, arg2] = f(0, [2]);
  assertSame(1, arg0);
  assertSame(2, arg1);
  assertSame(0, arg2);
}

// parameter map - assignment (2)
{
  function f(a, arguments) {
    arguments[0] = 1;
    return [a, arguments[0], f.arguments[0]];
  }
  let [arg0, arg1, arg2] = f(0, [2]);
  assertSame(0, arg0);
  assertSame(1, arg1);
  assertSame(0, arg2);
}

// parameter map - assignment (3)
{
  function f(a, arguments) {
    f.arguments[0] = 1;
    return [a, arguments[0], f.arguments[0]];
  }
  let [arg0, arg1, arg2] = f(0, [2]);
  assertSame(0, arg0);
  assertSame(2, arg1);
  assertSame(0, arg2);
}

// parameter map - delete (1)
{
  function f(a, arguments) {
    delete arguments[0];
    a = 1;
    return [a, arguments[0], f.arguments[0]];
  }
  let [arg0, arg1, arg2] = f(0, [2]);
  assertSame(1, arg0);
  assertSame(void 0, arg1);
  assertSame(0, arg2);
}

// parameter map - delete (2)
{
  function f(a, arguments) {
    delete arguments[0];
    arguments[0] = 1;
    return [a, arguments[0], f.arguments[0]];
  }
  let [arg0, arg1, arg2] = f(0, [2]);
  assertSame(0, arg0);
  assertSame(1, arg1);
  assertSame(0, arg2);
}

// parameter map - delete (3)
{
  function f(a, arguments) {
    delete arguments[0];
    f.arguments[0] = 1;
    return [a, arguments[0], f.arguments[0]];
  }
  let [arg0, arg1, arg2] = f(0, [2]);
  assertSame(0, arg0);
  assertSame(void 0, arg1);
  assertSame(0, arg2);
}
