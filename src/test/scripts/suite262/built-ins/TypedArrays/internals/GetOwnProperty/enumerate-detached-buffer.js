/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-integer-indexed-exotic-objects-getownproperty-p
info: Test for-in enumeration with detached buffer.
description: >
  9.4.5.1 [[GetOwnProperty]] ( P )
    ...
    3. If Type(P) is String, then
      a. Let numericIndex be ! CanonicalNumericIndexString(P).
      b. If numericIndex is not undefined, then
        i. Let value be ? IntegerIndexedElementGet(O, numericIndex).
    ...

  9.4.5.8 IntegerIndexedElementGet ( O, index )
    ...
    3. Let buffer be O.[[ViewedArrayBuffer]].
    4. If IsDetachedBuffer(buffer) is true, throw a TypeError exception.
    ...

  13.7.5.15 EnumerateObjectProperties (O)
    ...
    EnumerateObjectProperties must obtain the own property keys of the
    target object by calling its [[OwnPropertyKeys]] internal method.
    Property attributes of the target object must be obtained by
    calling its [[GetOwnProperty]] internal method.

includes: [testTypedArray.js, detachArrayBuffer.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var sample = new TA(42);
  $DETACHBUFFER(sample.buffer);

  assert.throws(TypeError, function() {
    for (var key in sample) {
      throw new Test262Error();
    }
  });
});
