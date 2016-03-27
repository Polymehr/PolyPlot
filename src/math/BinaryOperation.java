package math;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;

/**
 * @author Gordian
 */
enum BinaryOperation implements ComparableOperator {
    // simple operators
    EXPONENTIATION("^", Math::pow, 42, false),
    MULTIPLICATION("*", (left, right) -> left * right, 41, true),
    DIVISION("/", (left, right) -> left / right, 41, true),
    MODULUS("%", (left, right) -> left % right, 41, true),
    PLUS("+", (left, right) -> left + right, 39, true),
    MINUS("-", (left, right) -> left - right, 39, true);

    private final String SIGN;
    private final DoubleBinaryOperator OPERATION;
    private final boolean LEFT_ASSOCIATIVE;
    private final int WEIGHT;

    BinaryOperation(String sign, DoubleBinaryOperator operation, int weight, boolean leftAssociative) {
        if (Objects.requireNonNull(sign, "operator SIGN must not be null").trim().isEmpty())
            throw new IllegalArgumentException("operator SIGN must not be empty");
        this.SIGN = sign;
        this.OPERATION = Objects.requireNonNull(operation, "operator OPERATION must not be null");
        this.LEFT_ASSOCIATIVE = leftAssociative;
        this.WEIGHT = weight;
    }

    public DoubleBinaryOperator getOperation() {
        return this.OPERATION;
    }

    @Override
    public int getWeight() {
        return this.WEIGHT;
    }

    /**
     * Is the operator left associative?
     * NOTE: Most operators are left associative, i.e. something like "1 - 2 - 3 - 4" is the same as
     * "(((1 - 2) - 3) - 4)". However, there are exceptions, like e.g. exponentiation where "2 ^ 3 ^ 4" is equal to
     * "(2 ^ (3 ^ 4))".
     * @return {@code true} if the operator is left associative or {@code false} otherwise
     */
    public boolean isLeftAssociative() {
        return this.LEFT_ASSOCIATIVE;
    }

    /**
     * Get the binary operator representing the given sign.
     * @param sign the operator's sign; must be one of {@code "^"}, {@code "*"}, {@code "/"}, {@code "%"}, {@code "+"},
     *             or {@code "-"}
     * @return the {@code BinaryOperation} object if the parameter was one of the ones mentioned above
     * @throws IllegalArgumentException if the parameter wasn't one of the ones mentioned above
     */
    public static BinaryOperation ofSign(String sign) throws IllegalArgumentException {
        for (BinaryOperation bo : BinaryOperation.values())
            if (bo.SIGN.equals(sign)) return bo;
        throw new IllegalArgumentException("\""+sign+"\" is not a valid operator");
    }

    private static String[] validOperators = null;

    public static String[] validBinaryOperators() {
        if (null == validOperators) {
            validOperators = new String[BinaryOperation.values().length];
            for (int i = 0; i < validOperators.length; ++i) {
                validOperators[i] = BinaryOperation.values()[i].SIGN;
            }
        }
        return validOperators;
    }

    @Override
    public String toString() {
        return this.SIGN;
    }
}
