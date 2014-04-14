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
 * <h2>12.2 Primary Expression</h2><br>
 * <h3>12.2.4 Array Initializer</h3>
 * <ul>
 * <li>12.2.4.2 Array Comprehension
 * </ul>
 */
public final class LegacyComprehension extends Comprehension implements ScopedNode {
    private final BlockScope scope;

    public LegacyComprehension(BlockScope scope, List<ComprehensionQualifier> list,
            Expression expression) {
        super(list, expression);
        this.scope = scope;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
