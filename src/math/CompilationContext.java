package math;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * @author 5hir0kur0
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

    public CompilationContext(boolean addDefaultFunctionsAndConstants) {
        this.constants = new HashMap<>();
        this.functions = new HashMap<>();
        if (!addDefaultFunctionsAndConstants) return;
        addConstant("e", Math.E);
        addConstant("pi", Math.PI);

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

    public final void addFunction(String name, Function function) {
        if (Objects.requireNonNull(name, "function name must not be null").isEmpty())
            throw new IllegalArgumentException("function name must not be empty");
        if (this.functions.containsKey(name.toLowerCase()))
            throw new IllegalArgumentException("functions cannot be redefined, because the expression compiler works"
                    + " under the assumption that they are constant");
        this.functions.put(name.toLowerCase(), Objects.requireNonNull(function, "function must not be null"));
    }

    public final void addPureFunction(String name, DoubleUnaryOperator function) {
        this.addFunction(name, new PureFunctionAdapter(function, name));
    }

    public final void addBiFunction(String name, DoubleBinaryOperator function) {
        this.addFunction(name, new BiFunctionAdapter(name, function));
    }

    public final void addConstant(String name, Double constant) {
        if (Objects.requireNonNull(name, "constant name must not be null").isEmpty())
            throw new IllegalArgumentException("constant name must not be empty");
        if (null == constant || Double.isInfinite(constant) || Double.isNaN(constant))
            throw new IllegalArgumentException("constant must not be null, NaN or infinite");
        if (this.constants.containsKey(name.toLowerCase()))
            throw new IllegalArgumentException("constants cannot be redefined, because the expression compiler works "
                    + "under the assumption that they are constant");
        this.constants.put(name.toLowerCase(), constant);
    }

    public Function getFunction(String name) {
        return functions.get(name.toLowerCase());
    }

    public Double getConstant(String name) {
        return constants.get(name.toLowerCase());
    }

    public boolean hasFunction(String name) {
        return this.functions.containsKey(name.toLowerCase());
    }

    public boolean hasConstant(String name) {
        return this.constants.containsKey(name.toLowerCase());
    }
}
