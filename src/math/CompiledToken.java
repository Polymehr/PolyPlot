package math;

import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * @author 5hir0kur0
 */
final class CompiledToken {
    enum Type {
        NUMBER, ARGUMENT, UNARY_OPERATION, BINARY_OPERATION, FUNCTION
    }

    final Object CONTENT;
    final double NUMBER; // avoid autoboxing by using a primitive field
    final Type TYPE;

    static CompiledToken newNumberToken(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number))
            throw new IllegalArgumentException("number token must not be infinite or NaN");
        return new CompiledToken(number);
    }

    static CompiledToken newArgumentToken(int index) {
        if (index < 0) throw new IllegalArgumentException("argument token index must not be smaller than 0");
        return new CompiledToken(index, Type.ARGUMENT);
    }

    static CompiledToken newUnaryOperationToken(DoubleUnaryOperator operation) {
        return new CompiledToken(Objects.requireNonNull(operation, "unary operation token operation must not be null"),
                Type.UNARY_OPERATION);
    }

    static CompiledToken newBinaryOperation(DoubleBinaryOperator operation) {
        return new CompiledToken(Objects.requireNonNull(operation, "binary operation token operation must not be null"),
                Type.BINARY_OPERATION);
    }

    static CompiledToken newFunction(Function function) {
        return new CompiledToken(Objects.requireNonNull(function, "impure function token function must not be null"),
                Type.FUNCTION);
    }

    private CompiledToken(double content) {
        this.NUMBER = content;
        this.CONTENT = null;
        this.TYPE = Type.NUMBER;
    }

    private CompiledToken(Object content, Type type) {
        this.NUMBER = Double.NaN;
        this.CONTENT = Objects.requireNonNull(content, "compiled token must not be null");
        this.TYPE = Objects.requireNonNull(type, "compiled token type must not be null");
    }

    @Override
    public String toString() {
        switch (this.TYPE) {
            case ARGUMENT: return "{"+this.CONTENT.toString()+"}";
            case BINARY_OPERATION:
                for (BinaryOperation bo : BinaryOperation.values())
                    if (bo.getOperation() == this.CONTENT) return bo.toString();
                return "{{INVALID BINARY OPERATION: " + this.CONTENT + "}}";
            case UNARY_OPERATION:
                for (UnaryOperation uo : UnaryOperation.values())
                    if (uo.getOperation() == this.CONTENT) return uo.toString();
                return "{{PURE FUNCTION: " + this.CONTENT + "}}}";
            default:
                return this.CONTENT == null ? Double.toString(this.NUMBER) : this.CONTENT.toString();
        }
    }
}
