package polyplot.math;

import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * @author Gordian
 */
final class CompiledToken {
    enum Type {
        NUMBER, ARGUMENT, UNARY_OPERATION, BINARY_OPERATION, FUNCTION
    }

    final ImpureFunction function;
    final double number; // avoid autoboxing by using a primitive field
    final int index; // same here
    final DoubleUnaryOperator unaryOperator;
    final DoubleBinaryOperator binaryOperator;
    final Type type;

    static CompiledToken newNumber(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number))
            throw new IllegalArgumentException("number token must not be infinite or NaN");
        return new CompiledToken(null, number, -1, null, null, Type.NUMBER);
    }

    static CompiledToken newArgument(int index) {
        if (index < 0) throw new IllegalArgumentException("argument token index must not be smaller than 0");
        return new CompiledToken(null, Double.NaN, index, null, null, Type.ARGUMENT);
    }

    static CompiledToken newUnaryOperation(DoubleUnaryOperator operation) {
        return new CompiledToken(null, Double.NaN, -1,
                Objects.requireNonNull(operation, "unary operation token operation must not be null"), null,
                Type.UNARY_OPERATION);
    }

    static CompiledToken newBinaryOperation(DoubleBinaryOperator operation) {
        return new CompiledToken(null, Double.NaN, -1, null,
                Objects.requireNonNull(operation, "binary operation token operation must not be null"),
                Type.BINARY_OPERATION);
    }

    static CompiledToken newFunction(ImpureFunction function) {
        return new CompiledToken(Objects.requireNonNull(function, "impure function token function must not be null"),
                Double.NaN, -1, null, null, Type.FUNCTION);
    }

    private CompiledToken(ImpureFunction function, double number, int index, DoubleUnaryOperator unaryOperator,
                          DoubleBinaryOperator binaryOperator, Type type) {
        this.function = function;
        this.index = index;
        this.number = number;
        this.unaryOperator = unaryOperator;
        this.binaryOperator = binaryOperator;
        this.type = type;
    }

    @Override
    public String toString() {
        switch (this.type) {
            case ARGUMENT: return "{" + this.index + "}";
            case BINARY_OPERATION:
                for (BinaryOperation bo : BinaryOperation.values())
                    if (bo.getOperation() == this.binaryOperator) return bo.toString();
                return this.binaryOperator.toString();
            case UNARY_OPERATION:
                for (UnaryOperation uo : UnaryOperation.values())
                    if (uo.getOperation() == this.unaryOperator) return uo.toString();
                return this.unaryOperator.toString();
            case NUMBER: return Double.toString(this.number);
            case FUNCTION: return this.function.toString();
            default: return "{{INVALID COMPILE-TIME TOKEN}}";
        }
    }
}
