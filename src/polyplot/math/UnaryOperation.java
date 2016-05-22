package polyplot.math;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * Represents an unary arithmetic operation, such as unary plus or minus.
 * @author Gordian
 */
enum UnaryOperation implements ComparableOperator {
    MINUS("-", operand -> -operand, 84),
    PLUS("+", operand -> +operand, 84);

    private final String sign;
    final DoubleUnaryOperator operation; // accessed by fastOf(...) in PureFunction
    private final int weight;

    public DoubleUnaryOperator getOperation() {
        return this.operation;
    }

    UnaryOperation(String sign, DoubleUnaryOperator operation, int weight) {
        if (Objects.requireNonNull(sign, "operator sign must not be null").trim().isEmpty())
            throw new IllegalArgumentException("operator sign must not be empty");
        this.sign = sign;
        this.operation = Objects.requireNonNull(operation, "operator operation must not be null");
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    private static String[] validOperators = null;

    public static String[] validUnaryOperators() {
        if (null == validOperators) {
            validOperators = new String[UnaryOperation.values().length];
            for (int i = 0; i < validOperators.length; ++i) {
                validOperators[i] = UnaryOperation.values()[i].sign;
            }
        }
        return validOperators;
    }

    public static boolean isUnaryOperator(String operator) {
        for (String s : UnaryOperation.validUnaryOperators()) if (s.equals(operator)) return true;
        return false;
    }

    /**
     * Get the unary operator representing the given sign.
     * @param sign the operator's sign; must either be {@code "+"} or {@code "-"}
     * @return the {@code UnaryOperation} object if the parameter was one of the ones mentioned above
     * @throws IllegalArgumentException if the parameter wasn't one of the ones mentioned above
     */
    public static UnaryOperation ofSign(String sign) throws IllegalArgumentException {
        for (UnaryOperation uo : UnaryOperation.values())
            if (uo.sign.equals(sign)) return uo;
        throw new IllegalArgumentException("\""+sign+"\" is not a valid operator");
    }

    @Override
    public String toString() {
        return "`"+this.sign +"`";
    }
}
