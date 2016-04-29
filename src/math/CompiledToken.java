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

    final Object content;
    final double number; // avoid autoboxing by using a primitive field
    final int index; // same here
    final Type type;

    static CompiledToken newNumberToken(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number))
            throw new IllegalArgumentException("number token must not be infinite or NaN");
        return new CompiledToken(number);
    }

    static CompiledToken newArgumentToken(int index) {
        if (index < 0) throw new IllegalArgumentException("argument token index must not be smaller than 0");
        return new CompiledToken(index);
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
        this.number = content;
        this.content = null;
        this.type = Type.NUMBER;
        this.index = -1;
    }

    private CompiledToken(Object content, Type type) {
        this.number = Double.NaN;
        this.content = Objects.requireNonNull(content, "compiled token must not be null");
        this.type = Objects.requireNonNull(type, "compiled token type must not be null");
        this.index = -1;
    }

    private CompiledToken(int index) {
        if (index < 0) throw new IllegalArgumentException("index must be bigger than null");
        this.index = index;
        this.type = Type.ARGUMENT;
        this.number = Double.NaN;
        this.content = null;
    }

    @Override
    public String toString() {
        switch (this.type) {
            case ARGUMENT: return "{"+this.content.toString()+"}";
            case BINARY_OPERATION:
                for (BinaryOperation bo : BinaryOperation.values())
                    if (bo.getOperation() == this.content) return bo.toString();
                return "{{INVALID BINARY OPERATION: " + this.content + "}}";
            case UNARY_OPERATION:
                for (UnaryOperation uo : UnaryOperation.values())
                    if (uo.getOperation() == this.content) return uo.toString();
                return "{{PURE FUNCTION: " + this.content + "}}}";
            default:
                return this.content == null ? Double.toString(this.number) : this.content.toString();
        }
    }
}
