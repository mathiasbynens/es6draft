/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 Statements and Declarations</h1><br>
 * <h2>12.2 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>12.2.4 Destructuring Binding Patterns
 * </ul>
 */
public class ObjectBindingPattern extends BindingPattern {
    private List<BindingProperty> properties;

    public ObjectBindingPattern(List<BindingProperty> properties) {
        this.properties = properties;
    }

    public List<BindingProperty> getProperties() {
        return properties;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
