package math;

import java.util.Objects;
import java.util.function.DoubleBinaryOperator;

/**
 * Represents a binary arithmetic operation, such as e.g. plus or minus.
 * @author 5hir0kur0
 */
enum BinaryOperation implements ComparableOperator {
    // simple operators
    EXPONENTIATION("^", Math::pow, 42, false),
    MULTIPLICATION("*", (left, right) -> left * right, 41, true),
    DIVISION("/", (left, right) -> left / right, 41, true),
    MODULUS("%", (left, right) -> left % right, 41, true),
    PLUS("+", (left, right) -> left + right, 39, true),
    MINUS("-", (left, right) -> left - right, 39, true);

    private final String sign;
    final DoubleBinaryOperator operation; // accessed by fastOf(...) in PureFunction
    private final boolean leftAssociative;
    private final int weight;

    BinaryOperation(String sign, DoubleBinaryOperator operation, int weight, boolean leftAssociative) {
        if (Objects.requireNonNull(sign, "operator sign must not be null").trim().isEmpty())
            throw new IllegalArgumentException("operator sign must not be empty");
        this.sign = sign;
        this.operation = Objects.requireNonNull(operation, "operator operation must not be null");
        this.leftAssociative = leftAssociative;
        this.weight = weight;
    }

    public DoubleBinaryOperator getOperation() {
        return this.operation;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    public String getSign() {
        return this.sign;
    }

    /**
     * Is the operator left associative?
     * NOTE: Most operators are left associative, i.e. something like "1 - 2 - 3 - 4" is the same as
     * "(((1 - 2) - 3) - 4)". However, there are exceptions, like e.g. exponentiation where "2 ^ 3 ^ 4" is equal to
     * "(2 ^ (3 ^ 4))".
     * @return {@code true} if the operator is left associative or {@code false} otherwise
     */
    public boolean isLeftAssociative() {
        return this.leftAssociative;
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
            if (bo.sign.equals(sign)) return bo;
        throw new IllegalArgumentException("\""+sign+"\" is not a valid operator");
    }

    private static String[] validOperators = null;

    public static String[] validBinaryOperators() {
        if (null == validOperators) {
            validOperators = new String[BinaryOperation.values().length];
            for (int i = 0; i < validOperators.length; ++i) {
                validOperators[i] = BinaryOperation.values()[i].sign;
            }
        }
        return validOperators;
    }

    @Override
    public String toString() {
        return this.sign;
    }
}
