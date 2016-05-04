package math;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Gordian
 */
public final class ImpureFunction implements Function {
    private final int numberOfArguments;
    private final String name;
    private final CompiledToken[] postfix;
    private final DoubleStack stack;

    public final double[] args;

    public ImpureFunction(int numberOfArguments, CompiledToken[] postfix, String name) {
        if (numberOfArguments < 1)
            throw new IllegalArgumentException("a function cannot have less than one argument");
        if (Objects.requireNonNull(name, "function name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("function name must not be empty");
        this.numberOfArguments = numberOfArguments;
        this.postfix = Objects.requireNonNull(postfix, "compiled postfix expression must not be null");
        this.stack = new DoubleStack(42);
        this.name = name;
        this.args = new double[this.numberOfArguments];
    }

    @Override
    public double of(double... args) {
        if (args.length != this.numberOfArguments)
            throw new IllegalArgumentException("illegal number of arguments: "+args.length);
        System.arraycopy(args, 0, this.args, 0, this.numberOfArguments);
        return this.ofStoredArgs();
    }

    public double ofStoredArgs() {
        for (CompiledToken token : this.postfix) {
            switch (token.type) {
                case NUMBER: this.stack.push(token.number); break;
                case ARGUMENT:
                    this.stack.push(this.args[token.index]);
                    break;
                case UNARY_OPERATION:
                    this.stack.push(token.unaryOperator.applyAsDouble(this.stack.pop()));
                    break;
                case BINARY_OPERATION: {
                    Double arg1 = this.stack.pop();
                    Double arg0 = this.stack.pop();
                    this.stack.push(token.binaryOperator.applyAsDouble(arg0, arg1));
                } break;
                case FUNCTION: {
                    final ImpureFunction f = token.function;
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
    public int getNumberOfArguments() {
        return this.numberOfArguments;
    }

    @Override
    public String toString() {
        return this.name + "[" + this.numberOfArguments + "]()" + "[[[" + Arrays.toString(this.postfix); //TODO: REMOVE LATER
    }

    @Override
    public String getName() {
        return this.name;
    }
}
