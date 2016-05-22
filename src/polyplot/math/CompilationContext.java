package polyplot.math;

import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

/**
 * Stores the functions and constants produced by a {@link Compiler} and some defaults like "sin()" or "e".
 * NOTE: The observers will be notified whenever a function or constant is added. You can just check with
 *       {@code instanceof} whether it was a {@link Function} or {@link polyplot.math.CompilationContext.Constant}.
 *       Observers are not notified when a function/constant is removed, because removing only occurs while
 *       recompiling (and thus the respective (updated) item will be added again later again anyway)
 * @author Gordian
 * @see Compiler
 */
public class CompilationContext extends Observable {
    private final Map<String, Constant> constants;
    private final Map<String, Function> functions;

    private boolean functionCacheInvalid = true;
    private boolean constantCacheInvalid = true;

    private List<Constant> constantCache = null;
    private List<Function> functionCache = null;

    /**
     * Used to represent a constant. This class is needed because a constant needs to store whether or not it was
     * defined by the user.
     */
    public final static class Constant {
        final boolean userDefined;
        final double value;
        final String fullExpression;
        final String name;

        Constant(double value, boolean userDefined, String name, String fullExpression) {
            if (Objects.requireNonNull(name, "constant name must not be null").trim().isEmpty())
                throw new IllegalArgumentException("empty constant name");
            this.value = value;
            this.userDefined = userDefined;
            this.fullExpression = Objects.requireNonNull(fullExpression, "constant expression must not be null");
            this.name = Objects.requireNonNull(name, "constant name must not be null");
        }

        /**
         * @return {@code true} if the constant was defined by the user or {@code false} otherwise
         */
        public boolean isUserDefined() {
            return this.userDefined;
        }

        /**
         * @return the constant's value
         */
        public double getValue() {
            return this.value;
        }

        /**
         * @return the full expression used to define the constant; might be {@code null}
         */
        public String getFullExpression() {
            return this.fullExpression;
        }

        /**
         * @return the constant's name; never {@code null}
         */
        public String getName() {
            return this.name;
        }
    }

    private final static class PureFunctionAdapter extends Function implements DoubleUnaryOperator {

        private final DoubleUnaryOperator operation;

        PureFunctionAdapter(DoubleUnaryOperator operation, String name) {
            super(name, "[native function]", 1, null, false);
            this.operation = Objects.requireNonNull(operation, "pure function adapter operation must not be null");
        }

        @Override
        public double of(double... args) {
            if (args.length != 1) throw new IllegalArgumentException("pure functions take exactly one argument");
            return this.operation.applyAsDouble(args[0]);
        }

        @Override
        public String toString() {
            return this.name + "[pure]()";
        }

        @Override
        public double applyAsDouble(double operand) {
            return this.operation.applyAsDouble(operand);
        }

    }

    private final static class BiFunctionAdapter extends Function implements DoubleBinaryOperator {

        private final DoubleBinaryOperator operation;

        BiFunctionAdapter(String name, DoubleBinaryOperator operation) {
            super(name, "[native function]", 2, null, false);
            this.operation = Objects.requireNonNull(operation, "binary function operation must not be null");
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
        public String toString() {
            return this.name + "[bi]()";
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
        addConstant("e", "[native constant; value = " + Math.E + "]", Math.E, false);
        addConstant("pi", "[native constant; value = " + Math.PI + "]", Math.PI, false);
        // in case someone wants to use the unicode-character
        addConstant("π", "[native constant; value = " + Math.PI + "]", Math.PI, false);

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
    public final void addFunction(String name, Function function) throws IllegalArgumentException {
        if (Objects.requireNonNull(name, "function name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("function name must not be empty");
        if (this.functions.containsKey(name.toLowerCase()))
            throw new IllegalArgumentException("functions cannot be redefined, because the expression compiler works"
                    + " under the assumption that they are constant");
        this.functions.put(name.toLowerCase(), Objects.requireNonNull(function, "function must not be null"));
        this.functionCacheInvalid = true;
        if (function.isUserDefined()) {
            super.setChanged();
            super.notifyObservers(function);
        }
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
     * @param fullExpression the full expression used to define this constant (e.g. {@code "c = 23*PI"});
     *                       may be {@code null}; if it is {@code null} the value of the constant won't be reevaluated
     *                       on recompile
     * @param constant the constant's value; must not be {@code NaN} or positive or negative {@code Infinity}
     * @param userDefined {@code true} if the constant was defined by the user or {@code false} if it is predefined
     */
    public final void addConstant(String name, String fullExpression, Double constant, boolean userDefined) throws IllegalArgumentException {
        if (Objects.requireNonNull(name, "constant name must not be null").trim().isEmpty())
            throw new IllegalArgumentException("constant name must not be empty");
        if (null == constant || Double.isInfinite(constant) || Double.isNaN(constant))
            throw new IllegalArgumentException("constant must not be null, NaN or infinite");
        if (this.constants.containsKey(name.toLowerCase()))
            throw new IllegalArgumentException("constants cannot be redefined, because the expression compiler works "
                    + "under the assumption that they are constant");
        Constant tmp = new Constant(constant, userDefined, name, fullExpression);
        this.constants.put(name.toLowerCase(), tmp);
        this.constantCacheInvalid = true;
        if (userDefined) {
            super.setChanged();
            super.notifyObservers(tmp);
        }
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
        Constant tmp =  constants.get(name.toLowerCase());
        if (null == tmp) return null;
        return tmp.value;
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
     * @param onlyUserDefined if {@code true}, only return user-defined functions
     * @return a {@link Collection} of {@link Function}s
     */
    public List<Function> getFunctions(boolean onlyUserDefined) {
        if (onlyUserDefined && !this.functionCacheInvalid && this.functionCache != null) return this.functionCache;
        List<Function> result = this.functions.values().stream()
                .filter(f -> !onlyUserDefined || f.isUserDefined())
                .collect(Collectors.toList());
        if (onlyUserDefined) {
            this.functionCache = result;
            this.functionCacheInvalid = false;
        }
        return result;
    }

    /**
     * Gets all the context's constants.
     * @param onlyUserDefined if {@code true}, only return user-defined constants
     * @return a {@link List} of {@link polyplot.math.CompilationContext.Constant}s
     */
    public List<Constant> getConstants(boolean onlyUserDefined) {
        if (onlyUserDefined && !this.constantCacheInvalid && this.constantCache != null) return this.constantCache;
        List<Constant> result =  this.constants.values().stream()
                .filter(constant -> !onlyUserDefined || constant.userDefined)
                .collect(Collectors.toList());
        if (onlyUserDefined) {
            this.constantCache = result;
            this.constantCacheInvalid = false;
        }
        return result;
    }

    /**
     * NOTE: This method should only be used by the {@link Compiler} class, as calling it requires recompiling all user
     *       defined functions. Some functions might not be able to properly execute if they are not recompiled after
     *       this method was invoked.
     * @param name the function's name
     */
    void removeFunctionIfPresent(String name) {
        Function tmp = this.functions.remove(name.toLowerCase());
        if (!tmp.isUserDefined()) throw new IllegalStateException("trying to remove non-user-defined function");
        this.functionCacheInvalid = true;
        // no need to notify observers, because this method is only called if the compiler is doing a recompile
        // (and thus all the functions will be added again later again (possibly with a different implementation))
        // which will notify the observers
    }

    /**
     * NOTE: This method should only be used by the {@link Compiler} class, as calling it requires recompiling all user
     *       defined functions. Some functions might not be able to properly execute if they are not recompiled after
     *       this method was invoked.
     * @param name the constant's name
     */
    void removeConstantIfPresent(String name) {
        this.constants.remove(name.toLowerCase());
        this.constantCacheInvalid = true;
        // no need to notify observers, because this method is only called if the compiler is doing a recompile
        // (and thus all the constants will be added again later again (possibly with a different value))
        // which will notify the observers
    }
}
