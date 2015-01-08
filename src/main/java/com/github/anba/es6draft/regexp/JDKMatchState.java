/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.BitSet;
import java.util.Iterator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link MatchState} implementation for standard JDK {@link Pattern} regular expressions
 */
final class JDKMatchState implements MatchState, IterableMatchResult, RawMatchResult {
    private final Matcher matcher;
    private final BitSet negativeLAGroups;

    public JDKMatchState(Matcher matcher, BitSet negativeLAGroups) {
        this.matcher = matcher;
        this.negativeLAGroups = negativeLAGroups;
    }

    @Override
    public String toString() {
        return String.format("%s: [matcher=%s]", getClass().getSimpleName(), matcher);
    }

    @Override
    public Iterator<String> iterator() {
        return new GroupIterator(this, negativeLAGroups);
    }

    @Override
    public MatchResult toMatchResult() {
        MatchResult matchResult = matcher.toMatchResult();
        assert matchResult instanceof Matcher;
        return new JDKMatchState((Matcher) matchResult, negativeLAGroups);
    }

    @Override
    public RawMatchResult toRawMatchResult() {
        MatchResult matchResult = matcher.toMatchResult();
        assert matchResult instanceof Matcher;
        return new JDKMatchState((Matcher) matchResult, negativeLAGroups);
    }

    @Override
    public boolean find() {
        return matcher.find();
    }

    @Override
    public boolean find(int start) {
        return matcher.find(start);
    }

    @Override
    public boolean matches(int start) {
        return matcher.region(start, matcher.regionEnd()).lookingAt();
    }

    @Override
    public int start() {
        return matcher.start();
    }

    @Override
    public int start(int group) {
        return matcher.start(group);
    }

    @Override
    public int end() {
        return matcher.end();
    }

    @Override
    public int end(int group) {
        return matcher.end(group);
    }

    @Override
    public String group() {
        return matcher.group();
    }

    @Override
    public String group(int group) {
        return matcher.group(group);
    }

    @Override
    public int groupCount() {
        return matcher.groupCount();
    }

    @Override
    public Object rawGroup() {
        return group();
    }

    @Override
    public Object rawGroup(int group) {
        return group(group);
    }
}
