/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * Enumeration of lexer tokens
 */
public enum Token {
    // Keyword -> 7.6.1.1
    BREAK("break"), CASE("case"), CATCH("catch"), CLASS("class"), CONTINUE("continue"), CONST(
            "const"), DEBUGGER("debugger"), DEFAULT("default"), DELETE("delete"), DO("do"), ELSE(
            "else"), EXPORT("export"), FINALLY("finally"), FOR("for"), FUNCTION("function"), IF(
            "if"),
    IMPORT("import"),
    IN("in"),
    INSTANCEOF("instanceof"),
    LET("let"),
    NEW("new"),
    RETURN("return"),
    SUPER("super"),
    SWITCH("switch"),
    THIS("this"),
    THROW("throw"),
    TRY("try"),
    TYPEOF("typeof"),
    VAR("var"),
    VOID("void"),
    WHILE("while"),
    WITH("with"),
    // FutureReservedWord -> 7.6.1.2
    ENUM("enum"),
    EXTENDS("extends"),
    // FutureReservedWord (strict) -> 7.6.1.2
    IMPLEMENTS("implements"),
    INTERFACE("interface"),
    PACKAGE("package"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    STATIC("static"),
    YIELD("yield"),
    // Identifier
    NAME("<name>"),
    // Literal
    NULL("null"),
    TRUE("true"),
    FALSE("false"),
    NUMBER("<number>"),
    STRING("<string>"),
    REGEXP("<regexp>"),
    // Template
    TEMPLATE("`"),
    // Punctuator -> 7.7
    LC("{"), RC("}"), LP("("), RP(")"), LB("["), RB("]"), DOT("."), SEMI(";"), COMMA(","), LT("<"),
    GT(">"), LE("<="), GE(">="), EQ("=="), NE("!="), SHEQ("==="), SHNE("!=="), ADD("+"), SUB("-"),
    MUL("*"), MOD("%"), INC("++"), DEC("--"), SHL("<<"), SHR(">>"), USHR(">>>"), BITAND("&"),
    BITOR("|"), BITXOR("^"), NOT("!"), BITNOT("~"), AND("&&"), OR("||"), HOOK("?"), COLON(":"),
    ASSIGN("="), ASSIGN_ADD("+="), ASSIGN_SUB("-="), ASSIGN_MUL("*="), ASSIGN_MOD("%="),
    ASSIGN_SHL("<<="), ASSIGN_SHR(">>="), ASSIGN_USHR(">>>="), ASSIGN_BITAND("&="), ASSIGN_BITOR(
            "|="), ASSIGN_BITXOR("^="), ARROW("=>"),
    // missing punctuator
    TRIPLE_DOT("..."),
    // DivPunctuator -> 7.7
    DIV("/"), ASSIGN_DIV("/="),
    // Comment
    COMMENT("<comment>"),
    // EOF, Error
    EOF("<eof>"), ERROR("<error>");

    private final String name;

    private Token(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
