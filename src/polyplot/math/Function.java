package polyplot.math;

import java.util.Objects;

/**
 * Represents a mathematical function.
 * @author Gordian
 */
public abstract class Function {
    protected final int numberOfArguments;
    protected final String name;
    protected final String expression;
    protected final CompiledToken[] postfix;
    protected final DoubleStack stack;
    protected final boolean userDefined;

    protected Function(String name, String fullExpression, int numberOfArguments, CompiledToken[] postfix,
                       boolean userDefined) {
        if (numberOfArguments < 1)
            throw new IllegalArgumentException("a function cannot have less than one argument");
        if (Objects.requireNonNull(name, "function name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("function name must not be empty");
        if (Objects.requireNonNull(fullExpression, "function expression must not be null").trim().isEmpty())
            throw new IllegalArgumentException("function expression must not be empty");
        this.numberOfArguments = numberOfArguments;
        this.postfix = postfix; // may be null
        this.stack = new DoubleStack(2);
        this.name = name;
        this.expression = fullExpression;
        this.userDefined = userDefined;
    }

    protected Function(String name, String fullExpression, int numberOfArguments, CompiledToken[] postfix) {
        this(name, fullExpression, numberOfArguments, postfix, true);
    }

    /**
     * Calculate a value of the function.
     * @param args the functions arguments; there must be {@code getNumberOfArguments()} arguments
     * @return the calculated value
     */
    public abstract double of(double... args);

    /**
     * @return the number of arguments this function takes
     */
    public int getNumberOfArguments() {
        return this.numberOfArguments;
    }

    /**
     * @return the function's name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the full expression used to define the function (e.g. {@code "f(x) = x ^ 2"})
     */
    public String getFullExpression() {
        return this.expression;
    }

    /**
     * @return {@code true} if the function was defined by the user or {@code false} otherwise
     */
    public boolean isUserDefined() {
        return this.userDefined;
    }
}
