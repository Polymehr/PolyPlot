package math;

import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * Stores the functions and constants produced by a {@link Compiler} and some defaults like "sin()" or "e".
 * @author 5hir0kur0
 * @see Compiler
 */
public class CompilationContext {
    private final Map<String, Double> constants;
    private final Map<String, Function> functions;

    private final static class PureFunctionAdapter implements Function, DoubleUnaryOperator {

        private final DoubleUnaryOperator operation;
        private final String name;

        PureFunctionAdapter(DoubleUnaryOperator operation, String name) {
            this.operation = Objects.requireNonNull(operation, "pure function adapter operation must not be null");
            this.name = Objects.requireNonNull(name, "function name must not be null");
            if (this.name.trim().isEmpty()) throw new IllegalArgumentException("function name must not be empty");
        }

        @Override
        public double of(double... args) {
            if (args.length != 1) throw new IllegalArgumentException("pure functions take exactly one argument");
            return this.operation.applyAsDouble(args[0]);
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
        public double applyAsDouble(double operand) {
            return this.operation.applyAsDouble(operand);
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    private final static class BiFunctionAdapter implements DoubleBinaryOperator, Function {

        private final DoubleBinaryOperator operation;
        private final String name;

        BiFunctionAdapter(String name, DoubleBinaryOperator operation) {
            this.operation = Objects.requireNonNull(operation, "binary function operation must not be null");
            this.name = Objects.requireNonNull(name, "function name must not be null");
            if (this.name.trim().isEmpty()) throw new IllegalArgumentException("function name must not be empty");
        }

        @Override
        public double applyAsDouble(double left, double right) {
            return this.operation.applyAsDouble(left, right);
        }

        @Override
        public double of(double... args) {
            if (args.length != 2) throw new IllegalStateException("illegal number of arguments");
            return this.applyAsDouble(args[0], args[1]);
        }

        @Override
        public int getNumberOfArguments() {
            return 2;
        }

        @Override
        public String toString() {
            return this.name + "[bi]()";
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    /**
     * Creates a new {@code CompilationContext}.
     * @param addDefaultFunctionsAndConstants if this option is {@code true}, add some default functions like "sin()"
     *                                        and some default constants like "pi"
     */
    public CompilationContext(boolean addDefaultFunctionsAndConstants) {
        this.constants = new HashMap<>();
        this.functions = new HashMap<>();
        if (!addDefaultFunctionsAndConstants) return;
        addConstant("e", Math.E);
        addConstant("pi", Math.PI);
        addConstant("π", Math.PI); // in case someone wants to use the unicode-character

        addPureFunction("abs", Math::abs);
        addPureFunction("acos", Math::acos);
        addPureFunction("asin", Math::asin);
        addPureFunction("atan", Math::atan);
        addBiFunction("atan2", Math::atan2);
        addBiFunction("IEEEremainder", Math::IEEEremainder);
        addBiFunction("max", Math::max);
        addBiFunction("min", Math::min);
        addPureFunction("cbrt", Math::cbrt);
        addPureFunction("ceil", Math::ceil);
        addPureFunction("cos", Math::cos);
        addPureFunction("cosh", Math::cosh);
        addPureFunction("exp", Math::exp);
        addPureFunction("expm1", Math::expm1);
        addPureFunction("floor", Math::floor);
        addPureFunction("log", Math::log);
        addPureFunction("log10", Math::log10);
        addPureFunction("log1p", Math::log1p);
        addPureFunction("round", Math::round);
        addPureFunction("sin", Math::sin);
        addPureFunction("sinh", Math::sinh);
        addPureFunction("sqrt", Math::sqrt);
        addPureFunction("√", Math::sqrt);
        addPureFunction("tan", Math::tan);
        addPureFunction("toDegrees", Math::toDegrees);
        addPureFunction("toRadians", Math::toRadians);
        addPureFunction("ulp", Math::ulp);
    }

    /**
     * Adds a new {@link Function} to the context.
     * @param name the {@link Function}'s name; must not be {@code null} or empty
     * @param function the {@link Function} to be added; must not be {@code null}
     */
    public final void addFunction(String name, Function function) {
        if (Objects.requireNonNull(name, "function name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("function name must not be empty");
        if (this.functions.containsKey(name.toLowerCase()))
            throw new IllegalArgumentException("functions cannot be redefined, because the expression compiler works"
                    + " under the assumption that they are constant");
        this.functions.put(name.toLowerCase(), Objects.requireNonNull(function, "function must not be null"));
    }

    private void addPureFunction(String name, DoubleUnaryOperator function) {
        this.addFunction(name, new PureFunctionAdapter(function, name));
    }

    private void addBiFunction(String name, DoubleBinaryOperator function) {
        this.addFunction(name, new BiFunctionAdapter(name, function));
    }

    /**
     * Adds a constant ({@code double}) to the context.
     * @param name the constant's name; must not be {@code null} or empty
     * @param constant the constant's value; must not be {@code NaN} or positive or negative {@code Infinity}
     */
    public final void addConstant(String name, Double constant) {
        if (Objects.requireNonNull(name, "constant name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("constant name must not be empty");
        if (null == constant || Double.isInfinite(constant) || Double.isNaN(constant))
            throw new IllegalArgumentException("constant must not be null, NaN or infinite");
        if (this.constants.containsKey(name.toLowerCase()))
            throw new IllegalArgumentException("constants cannot be redefined, because the expression compiler works "
                    + "under the assumption that they are constant");
        this.constants.put(name.toLowerCase(), constant);
    }

    /**
     * Returns the {@link Function} with the given name.
     * @param name the name of the {@link Function} to be returned; must not be {@code null}
     * @return the {@link Function} if it was found or {@code null} otherwise
     */
    public Function getFunction(String name) {
        return functions.get(name.toLowerCase());
    }

    /**
     * Returns the constant with the given name.
     * @param name the name of the constant to be returned; must not be {@code null}
     * @return the {@link Double} if it was found or {@code null} otherwise
     */
    public Double getConstant(String name) {
        return constants.get(name.toLowerCase());
    }

    /**
     * Checks whether a {@code Function} exists.
     * @param name the {@code Function}'s name; must not be {@code null}
     * @return {@code true} if the {@link Function} exists or {@code false} otherwise
     */
    public boolean hasFunction(String name) {
        return this.functions.containsKey(name.toLowerCase());
    }

    /**
     * Checks whether a constant exists.
     * @param name the constant's name; must not be {@code null}
     * @return {@code true} if the constant exists or {@code false} otherwise
     */
    public boolean hasConstant(String name) {
        return this.constants.containsKey(name.toLowerCase());
    }

    /**
     * Gets all the context's {@link Function}s.
     * @return a {@link Collection} of {@link Function}s
     */
    public Collection<Function> getFunctions() {
        return this.functions.values();
    }

    /**
     * Gets all the context's constants.
     * @return a {@link Collection} of {@link java.util.Map.Entry}s (the first element is the name and the second the
     *         value of the constant)
     */
    public Set<Map.Entry<String, Double>> getConstants() {
        return this.constants.entrySet();
    }
}
