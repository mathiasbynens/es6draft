/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.1 Primary Expressions</h2><br>
 * <h3>11.1.4 Array Initialiser</h3>
 * <ul>
 * <li>11.1.4.1 Array Literal
 * </ul>
 */
public class ArrayLiteral extends ArrayInitialiser {
    private List<Expression> elements;

    public ArrayLiteral(List<Expression> elements) {
        this.elements = elements;
    }

    public List<Expression> getElements() {
        return elements;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
