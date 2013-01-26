/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Base interface for all abstract-syntax-tree nodes
 */
public interface Node {
    int getLine();

    void setLine(int line);

    <R, V> R accept(NodeVisitor<R, V> visitor, V value);
}
