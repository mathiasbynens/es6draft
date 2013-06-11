/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.Identifier;
import com.github.anba.es6draft.ast.NodeVisitor;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.1 Primary Expressions</h2>
 * <ul>
 * <li>11.1.2 Identifier Reference
 * </ul>
 */
public class IdentifierValue extends Identifier {
    public IdentifierValue(String name) {
        super(name);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}