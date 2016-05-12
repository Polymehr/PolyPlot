package math;

import java.util.Objects;

/**
 * Represents an arithmetic function with an arbitrary number of arguments.
 * @author 5hir0kur0
 * @see Function
 */
final class ImpureFunction extends Function {

    final double[] args;

    ImpureFunction(String name, String fullExpression, int numberOfArguments, CompiledToken[] postfix) {
        super(name, fullExpression, numberOfArguments,
                Objects.requireNonNull(postfix, "compiled postfix expression must not be null"));
        if (numberOfArguments == 1)
            throw new IllegalArgumentException("PureFunction should be used if there is only one argument.");
        this.args = new double[this.numberOfArguments];
    }

    @Override
    public double of(double... args) {
        if (args.length != this.numberOfArguments)
            throw new IllegalArgumentException("illegal number of arguments: "+args.length);
        System.arraycopy(args, 0, this.args, 0, this.numberOfArguments);
        return this.ofStoredArgs();
    }

    double ofStoredArgs() {
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
        if (this.stack.size() != 1) throw new IllegalStateException("stack not one at the end of calculation");
        return this.stack.pop();
    }

    @Override
    public String toString() {
        return this.name + "[" + this.numberOfArguments + "]()";
    }
}
