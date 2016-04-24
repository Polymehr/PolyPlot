package math;

import java.util.Objects;

/**
 * @author Gordian
 */
final class Token {

    private enum Type {
        NUMBER, SYMBOL, BINARY_OPERATOR, UNARY_OPERATOR, OPENING_BRACKET, CLOSING_BRACKET, EQUALS_OPERATOR, COMMA
    }

    private final Type type;
    private final String content;



    private Token(Type type, String content) {
        this.type = Objects.requireNonNull(type, "token type must not be null");
        if (Objects.requireNonNull(content, "token content must not be null").trim().isEmpty())
            throw new IllegalArgumentException("token content must not be empty");
        this.content = content;
    }

    public boolean isNumber() {
        return this.type == Type.NUMBER;
    }

    public boolean isSymbol() {
        return this.type == Type.SYMBOL;
    }

    public boolean isBinaryOperator() {
        return this.type == Type.BINARY_OPERATOR;
    }

    public boolean isUnaryOperator() {
        return this.type == Type.UNARY_OPERATOR;
    }

    public boolean isOpeningBracket() {
        return this.type == Type.OPENING_BRACKET;
    }

    public boolean isClosingBracket() {
        return this.type == Type.CLOSING_BRACKET;
    }

    public boolean isComma() {
        return this.type == Type.COMMA;
    }

    public boolean isEqualsOperator() {
        return this.type == Type.EQUALS_OPERATOR;
    }

    public String getContent() {
        return this.content;
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return "Token[type="+this.type +"; content="+this.content +"]";
    }

    public static Token newNumberToken(String content) {
        return new Token(Type.NUMBER, content);
    }

    public static Token newSymbolToken(String content) {
        return new Token(Type.SYMBOL, content);
    }

    public static Token newBinaryOperatorToken(String content) {
        return new Token(Type.BINARY_OPERATOR, content);
    }

    public static Token newUnaryOperatorToken(String content) {
        return new Token(Type.UNARY_OPERATOR, content);
    }

    public static Token newOpeningBracketToken(String content) {
        return new Token(Type.OPENING_BRACKET, content);
    }

    public static Token newClosingBracketToken(String content) {
        return new Token(Type.CLOSING_BRACKET, content);
    }

    public static Token newEqualsOperatorToken(String content) {
        return new Token(Type.EQUALS_OPERATOR, content);
    }

    public static Token newCommaToken(String content) {
        return new Token(Type.COMMA, content);
    }
}
