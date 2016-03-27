package math;

import java.util.ArrayList;
import java.util.Objects;

/**
 * @author 5hir0kur0
 */
public class ImpureFunction implements Function {
    private final int numberOfArguments;
    private final String name;
    private final CompiledToken[] postfix;
    private final ArrayList<Double> stack;

    public ImpureFunction(int numberOfArguments, CompiledToken[] postfix, String name) {
        if (numberOfArguments < 1)
            throw new IllegalArgumentException("a function cannot have less than one argument");
        if (Objects.requireNonNull(name, "function name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("function name must not be empty");
        this.numberOfArguments = numberOfArguments;
        this.postfix = Objects.requireNonNull(postfix, "compiled postfix expression must not be null");
        this.stack = new ArrayList<>(42);
        this.name = name;
    }

    @Override
    public double of(double... args) {
        if (args.length != this.numberOfArguments)
            throw new IllegalArgumentException("illegal NUMBER of arguments: "+args.length);
        return 0.0/0;
    }

    @Override
    public int getNumberOfArguments() {
        return this.numberOfArguments;
    }

    @Override
    public String toString() {
        return this.name + "[" + this.numberOfArguments + "]";
    }
}
