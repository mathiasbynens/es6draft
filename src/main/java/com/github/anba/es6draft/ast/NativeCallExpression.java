/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>Extension: Native call expression
 * </ul>
 */
public final class NativeCallExpression extends Expression {
    private final Identifier base;
    private final List<Expression> arguments;

    public NativeCallExpression(long beginPosition, long endPosition, Identifier base,
            List<Expression> arguments) {
        super(beginPosition, endPosition);
        this.base = base;
        this.arguments = arguments;
    }

    public Identifier getBase() {
        return base;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}