package polyplot.math;

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * Represents an arithmetic function with exactly one argument.
 * @author 5hir0kur0
 * @see Function
 */
public final class PureFunction extends Function implements DoubleUnaryOperator {

    private double xOffset = 0.0;
    private double yOffset = 0.0;

    PureFunction(String name, String fullExpression, CompiledToken[] postfix) {
        super(name, fullExpression, 1, Objects.requireNonNull(postfix, "compiled postfix expression must not be null"));
        this.of(0.0); // needed because the stack needs to be the correct size for fastOf(...) to work
    }

    public double of(double x) {
        x += xOffset;
        for (CompiledToken token : this.postfix) {
            switch (token.type) {
                case NUMBER: this.stack.push(token.number); break;
                case ARGUMENT: this.stack.push(x); break;
                case UNARY_OPERATION: this.stack.push(token.unaryOperator.applyAsDouble(this.stack.pop())); break;
                case BINARY_OPERATION: {
                    final double arg1 = this.stack.pop();
                    final double arg0 = this.stack.pop();
                    this.stack.push(token.binaryOperator.applyAsDouble(arg0, arg1));
                } break;
                case FUNCTION: {
                    ImpureFunction f = token.function;
                    for (int i = 0, stop = f.getNumberOfArguments(); i < stop; ++i)
                        f.args[i] = this.stack.pop();
                    this.stack.push(f.ofStoredArgs());
                } break;
            }
        }
        if (this.stack.size() != 1) throw new IllegalStateException("stack not one at the end of calculation");
        return this.stack.pop() + yOffset;
    }

    public void setXOffset(double xOffset) {
        if (Double.isNaN(xOffset) || Double.isInfinite(xOffset))
            throw new IllegalArgumentException("illegal x offset: " + xOffset);
        this.xOffset = xOffset;
    }

    public void setYOffset(double yOffset) {
        if (Double.isNaN(yOffset) || Double.isInfinite(yOffset))
            throw new IllegalArgumentException("illegal y offset: " + yOffset);
        this.yOffset = yOffset;
    }

    public double getXOffset() {
        return this.xOffset;
    }

    public double getYOffset() {
        return this.yOffset;
    }

    /**
     * This method saves some function calls by directly accessing the stack and using ugly if-statements for operators.
     * @param x the argument of the function stored by this class
     * @return the value of the function at x
     * @see #of(double)
     */
    public double fastOf(double x) {
        x += xOffset;
        for (CompiledToken token : this.postfix) {
            switch (token.type) {
                case NUMBER: this.stack.stack[++this.stack.top] = token.number; break;
                case ARGUMENT: this.stack.stack[++this.stack.top] = x; break;
                case UNARY_OPERATION:
                    final double arg = this.stack.stack[this.stack.top--];
                    if (token.unaryOperator == UnaryOperation.MINUS.operation)
                        this.stack.stack[++this.stack.top] = -arg;
                    else if (token.unaryOperator == UnaryOperation.PLUS.operation) break;
                    else
                        this.stack.stack[++this.stack.top] = token.unaryOperator.applyAsDouble(arg);
                    break;
                case BINARY_OPERATION:
                    final double arg1 = this.stack.stack[this.stack.top--];
                    final double arg0 = this.stack.stack[this.stack.top--];
                    if (token.binaryOperator == BinaryOperation.MULTIPLICATION.operation)
                        this.stack.stack[++this.stack.top] = arg0 * arg1;
                    else if (token.binaryOperator == BinaryOperation.DIVISION.operation)
                        this.stack.stack[++this.stack.top] = arg0 / arg1;
                    else if (token.binaryOperator == BinaryOperation.PLUS.operation)
                        this.stack.stack[++this.stack.top] = arg0 + arg1;
                    else if (token.binaryOperator == BinaryOperation.MINUS.operation)
                        this.stack.stack[++this.stack.top] = arg0 - arg1;
                    else if (token.binaryOperator == BinaryOperation.EXPONENTIATION.operation)
                        this.stack.stack[++this.stack.top] = Math.pow(arg0, arg1);
                    else if (token.binaryOperator == BinaryOperation.MODULUS.operation)
                        this.stack.stack[++this.stack.top] = arg0 % arg1;
                    else
                        this.stack.stack[++this.stack.top] = token.binaryOperator.applyAsDouble(arg0, arg1);
                    break;
                case FUNCTION:
                    ImpureFunction f = token.function;
                    for (int i = 0, stop = f.getNumberOfArguments(); i < stop; ++i)
                        f.args[i] = this.stack.stack[this.stack.top--];
                    this.stack.stack[++this.stack.top] = f.ofStoredArgs();
                    break;
            }
        }
        if (this.stack.top != 0) throw new IllegalStateException("stack not one at the end of calculation");
        return this.stack.stack[this.stack.top--] + yOffset;
    }

    @Override
    public double applyAsDouble(double operand) {
        return this.of(operand);
    }

    @Override
    public double of(double... args) {
        if (args.length != 1)
            throw new IllegalArgumentException("pure function must have one argument ("+args.length+" given)");
        return of(args[0]);
    }

    @Override
    public int getNumberOfArguments() {
        return 1;
    }

    @Override
    public String toString() {
        return this.name + "[pure]()";
    }

    @Override
    public String getName() {
        return this.name;
    }
}