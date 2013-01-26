/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * <h1>12 Statements and Declarations</h1>
 * <ul>
 * <li>12.11 The switch Statement
 * </ul>
 */
public class SwitchStatement extends BreakableStatement {
    private EnumSet<Abrupt> abrupt;
    private Set<String> labelSet;
    private Expression expression;
    private List<SwitchClause> clauses;

    public SwitchStatement(EnumSet<Abrupt> abrupt, Set<String> labelSet, Expression expression,
            List<SwitchClause> clauses) {
        this.abrupt = abrupt;
        this.labelSet = labelSet;
        this.expression = expression;
        this.clauses = clauses;
    }

    @Override
    public EnumSet<Abrupt> getAbrupt() {
        return abrupt;
    }

    @Override
    public Set<String> getLabelSet() {
        return labelSet;
    }

    public Expression getExpression() {
        return expression;
    }

    public List<SwitchClause> getClauses() {
        return clauses;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
