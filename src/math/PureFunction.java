package math;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * @author Gordian
 */
public final class PureFunction implements DoubleUnaryOperator, Function {
    private final CompiledToken[] postfix;
    private final DoubleStack stack;
    private final String name;

    public PureFunction(CompiledToken[] postfix, String name) {
        if (Objects.requireNonNull(name, "function name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("function name must not be empty");
        this.postfix = Objects.requireNonNull(postfix, "compiled postfix expression must not be null");
        this.stack = new DoubleStack(postfix.length / 2 == 0 ? 1 : postfix.length / 2);
        this.name = name;
    }

    public double of(double x) {
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
        //TODO: maybe remove this check later for better performance
        if (this.stack.size() != 1) throw new IllegalStateException("stack not one at the end of calculation");
        return this.stack.pop();
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
        return this.name + "[pure]()" + "=" + Arrays.toString(this.postfix); //TODO remove postfix
    }

    @Override
    public String getName() {
        return this.name;
    }
}
