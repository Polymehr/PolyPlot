package math;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Gordian
 */
final class Token {

    private enum Type {
        NUMBER, SYMBOL, BINARY_OPERATOR, UNARY_OPERATOR, OPENING_BRACKET, CLOSING_BRACKET, EQUALS_OPERATOR, COMMA
    }

    private final Type TYPE;
    private final String CONTENT;



    private Token(Type type, String content) {
        this.TYPE = Objects.requireNonNull(type, "token TYPE must not be null");
        if (Objects.requireNonNull(content, "token content must not be null").trim().isEmpty())
            throw new IllegalArgumentException("token content must not be empty");
        this.CONTENT = content;
    }

    public boolean isNumber() {
        return this.TYPE == Type.NUMBER;
    }

    public boolean isSymbol() {
        return this.TYPE == Type.SYMBOL;
    }

    public boolean isBinaryOperator() {
        return this.TYPE == Type.BINARY_OPERATOR;
    }

    public boolean isUnaryOperator() {
        return this.TYPE == Type.UNARY_OPERATOR;
    }

    public boolean isOpeningBracket() {
        return this.TYPE == Type.OPENING_BRACKET;
    }

    public boolean isClosingBracket() {
        return this.TYPE == Type.CLOSING_BRACKET;
    }

    public boolean isComma() {
        return this.TYPE == Type.COMMA;
    }

    public String getContent() {
        return this.CONTENT;
    }

    public Type getType() {
        return this.TYPE;
    }

    @Override
    public String toString() {
        return "Token[TYPE="+this.TYPE+"; CONTENT="+this.CONTENT+"]";
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
