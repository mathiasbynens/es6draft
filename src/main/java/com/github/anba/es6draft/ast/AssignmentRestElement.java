/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.13 Assignment Operators</h2>
 * <ul>
 * <li>
 * <li>11.13.1 Destructuring Assignment
 * </ul>
 */
public class AssignmentRestElement extends AstNode implements AssignmentElementItem {
    private LeftHandSideExpression target;

    public AssignmentRestElement(LeftHandSideExpression target) {
        this.target = target;
    }

    public LeftHandSideExpression getTarget() {
        return target;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
