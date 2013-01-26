/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import com.github.anba.es6draft.ast.*;

/**
 *
 */
class FunctionName extends DefaultNodeVisitor<String, String> {
    static final NodeVisitor<String, String> INSTANCE = new FunctionName();

    @Override
    protected String visit(Node node, String defaultValue) {
        throw new IllegalStateException();
    }

    @Override
    public String visit(ArrowFunction node, String defaultValue) {
        return defaultValue;
    }

    @Override
    public String visit(FunctionDeclaration node, String defaultValue) {
        BindingIdentifier identifier = node.getIdentifier();
        assert identifier != null;
        return identifier.getName();
    }

    @Override
    public String visit(FunctionExpression node, String defaultValue) {
        BindingIdentifier identifier = node.getIdentifier();
        if (identifier != null) {
            return identifier.getName();
        } else {
            return defaultValue;
        }
    }

    @Override
    public String visit(GeneratorDeclaration node, String defaultValue) {
        BindingIdentifier identifier = node.getIdentifier();
        assert identifier != null;
        return identifier.getName();
    }

    @Override
    public String visit(GeneratorExpression node, String defaultValue) {
        BindingIdentifier identifier = node.getIdentifier();
        if (identifier != null) {
            return identifier.getName();
        } else {
            return defaultValue;
        }
    }

    @Override
    public String visit(MethodDefinition node, String defaultValue) {
        String propName = PropName(node);
        assert propName != null;
        return propName;
    }
}
