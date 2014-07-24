/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.Iterator;
import java.util.List;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.1 Function Definitions
 * </ul>
 */
public final class FormalParameterList extends AstNode implements Iterable<FormalParameter> {
    private final List<FormalParameter> formals;

    public FormalParameterList(long beginPosition, long endPosition, List<FormalParameter> formals) {
        super(beginPosition, endPosition);
        this.formals = formals;
    }

    public List<FormalParameter> getFormals() {
        return formals;
    }

    @Override
    public Iterator<FormalParameter> iterator() {
        return formals.iterator();
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
