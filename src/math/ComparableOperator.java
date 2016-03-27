package math;

/**
 * @author Gordian
 */
interface ComparableOperator {
    /**
     * Compare this operator to another operator.
     * @param other the other operator; must not be {@code null}
     * @return the difference of the operator weights
     */
    default int compareOperator(ComparableOperator other) {
        return getWeight() - other.getWeight();
    }

    int getWeight();

    default boolean isUnary() {
        return this instanceof UnaryOperation;
    }

    default boolean isBinary() {
        return this instanceof BinaryOperation;
    }

    default boolean isBinaryAndLeftAssociative() {
        return isBinary() && ((BinaryOperation)this).isLeftAssociative();
    }
}
