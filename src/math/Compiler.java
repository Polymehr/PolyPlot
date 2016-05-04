package math;

import polyplot.MathEval;

import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 5hir0kur0
 */
public final class Compiler {
    private final static Pattern NUMBER = Pattern.compile("^(?:\\d*\\.\\d+|\\d+\\.?)");
    private final static Pattern SYMBOL = Pattern.compile("^\\w+");
    private final static Pattern OPERATOR;
    static { // initialize operator pattern
        StringBuilder pattern = new StringBuilder(32);
        pattern.append("^(?:");
        for (String operator : BinaryOperation.validBinaryOperators())
            pattern.append(Pattern.quote(operator)).append("|");
        for (String operator : UnaryOperation.validUnaryOperators())
            pattern.append(Pattern.quote(operator)).append("|");
        pattern.replace(pattern.length() - 1, pattern.length(), "");
        pattern.append(")");
        OPERATOR = Pattern.compile(pattern.toString());
    }
    private final static Pattern OPENING_BRACKET = Pattern.compile("^[(\\[{]");
    private final static Pattern CLOSING_BRACKET = Pattern.compile("^[)\\]}]");
    private final static Pattern EQUALS_OPERATOR = Pattern.compile("^=");
    private final static Pattern COMMA_OPERATOR = Pattern.compile("^,");
    private final static Pattern DEFINITION_SEPARATOR = Pattern.compile("\\s*;\\s*");

    private final CompilationContext context;

    private final boolean useFallbackParser;

    private List<String> arguments = Collections.emptyList();

    public Compiler(CompilationContext context, boolean useFallbackParser) {
        this.context = Objects.requireNonNull(context, "compilation context must not be null");
        this.useFallbackParser = useFallbackParser;
    }

    public void definition(String expression) {
        String[] definitions = expression.split(DEFINITION_SEPARATOR.pattern());
        for (String s : definitions) this.definition(tokenize(s), new MutableInteger());
    }

    public double constantExpression(String expression) {
        return this.constantValue(tokenize(expression), new MutableInteger());
    }

    private static List<Token> tokenize(String expression) {
        List<Token> result = new ArrayList<>(expression.length() / 21);

        for (int i = 0; i < expression.length();) { // i is incremented at the end of the loop
            String token = null;

            while (i < expression.length() && Character.isWhitespace(expression.charAt(i))) { ++i; } // skip whitespace

            if (i >= expression.length()) break;

            //NOTE: find(index) does not work for me here, because it does not set the position of '^' to <index>
            //therefore I have to work on a new substring every time :-/
            String substring = expression.substring(i);
            Matcher numberMatcher = NUMBER.matcher(substring);
            Matcher operatorMatcher = OPERATOR.matcher(substring);
            Matcher symbolMatcher = SYMBOL.matcher(substring);
            Matcher openingBracketMatcher = OPENING_BRACKET.matcher(substring);
            Matcher closingBracketMatcher = CLOSING_BRACKET.matcher(substring);
            Matcher equalsMatcher = EQUALS_OPERATOR.matcher(substring);
            Matcher commaMatcher = COMMA_OPERATOR.matcher(substring);

            if (numberMatcher.find()) {
                token = numberMatcher.group(0);
                result.add(Token.newNumberToken(token));
            }
            else if (operatorMatcher.find()) {
                token = operatorMatcher.group(0);
                Token previousToken = result.size() > 0 ? result.get(result.size() - 1) : null;
                final boolean isUnaryOperator = previousToken == null || !previousToken.isNumber()
                        && !previousToken.isSymbol() && !previousToken.isClosingBracket()
                        && UnaryOperation.isUnaryOperator(token);
                if (isUnaryOperator) result.add(Token.newUnaryOperatorToken(token));
                else result.add(Token.newBinaryOperatorToken(token));
            }
            else if (openingBracketMatcher.find()) {
                token = openingBracketMatcher.group(0);
                result.add(Token.newOpeningBracketToken(token));
            }
            else if (closingBracketMatcher.find()) {
                token = closingBracketMatcher.group(0);
                result.add(Token.newClosingBracketToken(token));
            }
            else if (equalsMatcher.find()) {
                token = equalsMatcher.group(0);
                result.add(Token.newEqualsOperatorToken(token));
            }
            else if (symbolMatcher.find()) {
                token = symbolMatcher.group(0);
                result.add(Token.newSymbolToken(token));
            }
            else if (commaMatcher.find()) {
                token = commaMatcher.group(0);
                result.add(Token.newCommaToken(token));
            }

            i += Objects.requireNonNull(token, "parsing error: illegal token (null)").length();
        }

        return result;
    }

    private int numArgs = 0;  // used by fallbackExpression to count arguments
    private List<CompiledToken> fallbackExpression(List<Token> tokens,
                                                   MutableInteger startIndex,
                                                   int brackets,
                                                   List<String> args) {
        final List<CompiledToken> result = new LinkedList<>();
        final Stack<ComparableOperator> stack = new Stack<>();
        final Stack<List<CompiledToken>> commaResultStack = new Stack<>();
        final java.util.function.Function<BooleanSupplier, Void> addRemainingElementsToResult = condition -> {
            while (condition.getAsBoolean()) {
                ComparableOperator top = stack.pop();
                if (top.isBinary())
                    result.add(CompiledToken.newBinaryOperation(((BinaryOperation) top).getOperation()));
                else if (top.isUnary())
                    result.add(CompiledToken.newUnaryOperation(((UnaryOperation) top).getOperation()));
                else
                    throw new IllegalStateException("token neither unary nor binary operation");
            }
            return null;
        };

        if (new HashSet<>(args).size() != args.size()) throw new IllegalArgumentException("duplicate arguments");
        if (brackets < 0) throw new IllegalArgumentException("invalid use of brackets");
        if (startIndex.get() >= tokens.size()) throw new IllegalStateException("startIndex > tokens.size()");

        for (int i = startIndex.get(); i < tokens.size(); ++i, startIndex.set(i)) {
            Token token = tokens.get(i);
            if      (token.isNumber()) {
                result.add(CompiledToken.newNumber(Double.parseDouble(token.getContent())));
            }
            else if (token.isSymbol()) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).isOpeningBracket()) { // token is a function
                    ++i;
                    final int oldArgs = this.numArgs;
                    this.numArgs = 1;
                    List<CompiledToken> recursiveRes = fallbackExpression(tokens, startIndex.set(i + 1),
                            brackets + 1, args);
                    final int newArgs = this.numArgs;
                    this.numArgs = oldArgs;
                    i = startIndex.get();
                    result.addAll(recursiveRes);
                    Function f = this.context.getFunction(token.getContent());
                    if (null == f)
                        throw new IllegalArgumentException("invalid function: "+token.getContent());
                    if (newArgs != f.getNumberOfArguments())
                        throw new IllegalStateException("invalid number of arguments for function " + f.getName()
                                + ": " + this.numArgs);
                    if (f instanceof DoubleUnaryOperator)
                        result.add(CompiledToken.newUnaryOperation((DoubleUnaryOperator) f));
                    // can't be supported, because the argument order would have to be different
                    //else if (f instanceof  DoubleBinaryOperator)
                    //    result.add(CompiledToken.newBinaryOperation((DoubleBinaryOperator) f));
                    else if (f instanceof ImpureFunction)
                        result.add(CompiledToken.newFunction((ImpureFunction) f));
                    else if (f instanceof DoubleBinaryOperator)
                        throw new IllegalStateException("binary operator functions are currently not supported by "
                                + "the fallback parser (remove function: " + f.getName() + ")");
                    else throw new IllegalStateException("illegal function class: " + f.getClass().getName());
                } else if (args.contains(token.getContent())){ // token is an argument
                    result.add(CompiledToken.newArgument(args.indexOf(token.getContent())));
                } else { // token is a constant
                    Double constant = this.context.getConstant(token.getContent());
                    if (null == constant) throw new IllegalArgumentException("invalid constant: "+token.getContent());
                    result.add(CompiledToken.newNumber(constant));
                }
            }
            else if (token.isOpeningBracket()) {
                List<CompiledToken> recursiveRes = fallbackExpression(tokens, startIndex.set(++i), brackets + 1, args);
                i = startIndex.get();
                result.addAll(recursiveRes);
            }
            else if (token.isClosingBracket()) {
                if (--brackets < 0) throw new IllegalArgumentException("illegal use of brackets");
                addRemainingElementsToResult.apply(() -> !stack.isEmpty());
                while (!commaResultStack.isEmpty()) {
                    result.addAll(commaResultStack.pop());
                }
                //TODO: remove later
                System.err.println(stack);
                System.err.println(result);
                return result;
            }
            else if (token.isUnaryOperator()) {
                stack.push(UnaryOperation.ofSign(token.getContent()));
            }
            else if (token.isBinaryOperator()) {
                BinaryOperation current = BinaryOperation.ofSign(token.getContent());
                addRemainingElementsToResult.apply(() -> !stack.isEmpty() && stack.peek().compareOperator(current) > 0
                                                   || !stack.isEmpty() && stack.peek().isBinaryAndLeftAssociative()
                                                   && stack.peek().compareOperator(current) >= 0);
                stack.push(BinaryOperation.ofSign(token.getContent()));
            }
            else if (token.isComma()) {
                addRemainingElementsToResult.apply(() -> !stack.isEmpty());
                commaResultStack.push(new ArrayList<>(result));
                result.clear();
                ++this.numArgs;
            }
            else throw new IllegalArgumentException("invalid token: " + token);
        }

        addRemainingElementsToResult.apply(() -> !stack.isEmpty());

        if (brackets != 0) throw new IllegalArgumentException("invalid use of brackets");

        if (!commaResultStack.isEmpty()) throw new IllegalArgumentException("comma operator appeared outside of ()");

        //TODO: remove later
        System.err.println(stack);
        System.err.println(result);

        optimize(result);

        //TODO: remove later
        System.err.println("____OPTIMIZED RESULT: "+result);

        return result;
    }

    private static void optimize(List<CompiledToken> tokens) {
        final Stack<CompiledToken> stack = new Stack<>();
        for (CompiledToken token : tokens) {
            switch (token.type) {
                case NUMBER:
                case ARGUMENT:
                    stack.push(token);
                    break;
                case UNARY_OPERATION:
                    if (!stack.isEmpty() && stack.peek().type == CompiledToken.Type.NUMBER)
                        stack.push(CompiledToken.newNumber(token.unaryOperator.applyAsDouble(stack.pop().number)));
                    else stack.push(token);
                    break;
                case BINARY_OPERATION:
                    if (!stack.isEmpty() && stack.peek().type == CompiledToken.Type.NUMBER) {
                        final CompiledToken arg1Token = stack.pop();
                        if (!stack.isEmpty() && stack.peek().type == CompiledToken.Type.NUMBER) {
                            final CompiledToken arg0Token = stack.pop();
                            final double arg0 = arg0Token.number;
                            final double arg1 = arg1Token.number;
                            stack.push(CompiledToken.newNumber(token.binaryOperator.applyAsDouble(arg0, arg1)));
                        } else {
                            stack.push(arg1Token);
                            stack.push(token);
                        }
                    } else stack.push(token);
                    break;
                case FUNCTION: {
                    final ImpureFunction f = token.function;
                    int i = 0;
                    for (; i < f.getNumberOfArguments(); ++i) {
                        if (!stack.isEmpty() && stack.peek().type == CompiledToken.Type.NUMBER)
                            f.args[i] = stack.pop().number;
                        else break;
                    }
                    if (i == f.getNumberOfArguments()) {
                        stack.push(CompiledToken.newNumber(f.ofStoredArgs()));
                    } else { // if not all args were constant, push them back onto the stack
                        while (--i >= 0) {
                            stack.push(CompiledToken.newNumber(f.args[i]));
                            f.args[i] = Double.NaN;
                        }
                        stack.push(token);
                    }
                }
            }
        }
        tokens.clear();
        tokens.addAll(stack);
    }

    // recursive descent parser
    // -> parses the following grammar into a syntax tree
    // // [<...>] => one or more times
    // // {<...>} => zero or more times
    // <digit>         ::= "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
    // <unary_sign>    ::= "-" | "+"
    // <number>        ::= [<digit>] | <function_call> | <symbol> | "(" <expression> ")"
    // <power>         ::= <number> | <number> "^" <factor>
    // <factor>        ::= <power> | <unary_sign> <factor>
    // <product>       ::= <factor> {("*" | "/" | "%") <factor>}
    // <expression>    ::= <product> {("+" | "-")  <product>}
    // <function_call> ::= <symbol> "(" <argument_list> ")"
    // <argument_list> ::= <expression> {"," <expression>}
    // <symbol>        ::= [<ascii_letter>]
    // <function_def>  ::= <symbol> "(" <symbol_list> ")" "=" <expression>
    // <symbol_list>   ::= <symbol> | <symbol> "," <symbol_list>
    // # expression has to be constant
    // <constant_def>  ::= <symbol> "=" <expression> | <symbol> "=" <constant_def>
    // <definition>    ::= <function_def> | <constant_def>

    private static class Node {
        enum Type { CONSTANT, ARGUMENT, UNARY_OPERATION, BINARY_OPERATION, FUNCTION }

        final Type type;
        final double constant;
        final DoubleUnaryOperator unaryOperation;
        final DoubleBinaryOperator binaryOperation;
        final Function function;

        final Node left;
        final Node right;

        final List<Node> arguments;

        final int argumentIndex;

        private Node(Type type, double constant, DoubleUnaryOperator unaryOperation,
                     DoubleBinaryOperator binaryOperation, Function function, Node left, Node right, List<Node> args,
                     int argumentIndex) {
            this.type = type;
            this.constant = constant;
            this.unaryOperation = unaryOperation;
            this.binaryOperation = binaryOperation;
            this.function = function;
            this.left = left;
            this.right = right;
            this.arguments = args;
            this.argumentIndex = argumentIndex;
        }

        Node(double constant) {
            this(Type.CONSTANT, constant, null, null, null, null, null, null, -1);
        }

        Node(DoubleUnaryOperator unaryOperation, Node right) {
            this(Type.UNARY_OPERATION, Double.NaN, unaryOperation, null, null, null, right, null, -1);
            if (null == unaryOperation || null == right)
                throw new IllegalStateException("trying to create node with null-operation or null-branches");
        }

        Node(DoubleBinaryOperator binaryOperation, Node left, Node right) {
            this(Type.BINARY_OPERATION, Double.NaN, null, binaryOperation, null, left, right, null, -1);
            if (null == binaryOperation || null == left || null == right)
                throw new IllegalStateException("trying to create node with null-operation or null-branches");
        }

        Node(Function function, List<Node> args) {
            this(Type.FUNCTION, Double.NaN, null, null, Objects.requireNonNull(function), null, null, args, -1);
            if (args.isEmpty()) throw new IllegalStateException("trying to create node with zero-arg function");
        }

        Node(int argumentIndex) {
            this(Type.ARGUMENT, Double.NaN, null, null, null, null, null, null, argumentIndex);
            if (!(this.argumentIndex >= 0))
                throw new IllegalStateException("argument index must be positive or zero");
        }

        boolean hasLeft() {
            return this.left != null;
        }

        boolean hasRight() {
            return this.right != null;
        }

        boolean hasLeftAndRight() {
            return this.hasLeft() && this.hasRight();
        }

        boolean isConstant() {
            switch (this.type) {
                case CONSTANT: return true;
                case UNARY_OPERATION: return this.right.isConstant();
                case BINARY_OPERATION: return this.left.isConstant() && this.right.isConstant();
                case FUNCTION:
                    boolean result = true;
                    for (Node n : this.arguments) if (!n.isConstant()) {
                        result = false;
                        break;
                    }
                    return result;
                case ARGUMENT:
                    return false;
                default:
                    throw new IllegalStateException("invalid node type");
            }
        }

        double constantValue() {
            if (!this.isConstant())
                throw new IllegalStateException("trying to compute non-constant tree at compilation time");
            switch (this.type) {
                case CONSTANT: return this.constant;
                case UNARY_OPERATION: return this.unaryOperation.applyAsDouble(this.right.constantValue());
                case BINARY_OPERATION:
                    return this.binaryOperation.applyAsDouble(this.left.constantValue(), this.right.constantValue());
                case FUNCTION: {
                    double[] args = new double[this.arguments.size()];
                    int i = 0;
                    for (Node arg : this.arguments) args[i++] = arg.constantValue();
                    return this.function.of(args);
                }
                default: throw new IllegalStateException("invalid node type");
            }
        }

        List<CompiledToken> compile() {
            List<CompiledToken> result = new LinkedList<>();
            if (this.isConstant()) result.add(CompiledToken.newNumber(this.constantValue()));
            else {
                switch (this.type) {
                    case CONSTANT: throw new UnsupportedOperationException("constant detection not working correctly");
                    case ARGUMENT:
                        result.add(CompiledToken.newArgument(this.argumentIndex));
                        break;
                    case UNARY_OPERATION:
                        if (!this.hasRight())
                            throw new IllegalStateException("unary operation without operand");
                        result.addAll(this.right.compile());
                        result.add(CompiledToken.newUnaryOperation(this.unaryOperation));
                        break;
                    case BINARY_OPERATION:
                        if (!this.hasLeftAndRight())
                            throw new IllegalStateException("binary operation with missing operands");
                        result.addAll(this.left.compile());
                        result.addAll(this.right.compile());
                        result.add(CompiledToken.newBinaryOperation(this.binaryOperation));
                        break;
                    case FUNCTION:
                        if (this.function instanceof DoubleUnaryOperator) {
                            if (this.arguments.size() != 1)
                                throw new IllegalStateException("argument list of unary function not equal to one: "
                                        + this.function);
                            result.addAll(this.arguments.get(0).compile());
                            result.add(CompiledToken.newUnaryOperation((DoubleUnaryOperator) this.function));
                        } else if (this.function instanceof  DoubleBinaryOperator) {
                            if (this.arguments.size() != 2)
                                throw new IllegalStateException("argument list of binary function not equal to two: "
                                        + this.function);
                            result.addAll(this.arguments.get(0).compile());
                            result.addAll(this.arguments.get(1).compile());
                            result.add(CompiledToken.newBinaryOperation((DoubleBinaryOperator) this.function));
                        }
                        else if (this.function instanceof ImpureFunction){
                            if (this.arguments == null
                                    || this.arguments.size() != this.function.getNumberOfArguments())
                                throw new IllegalStateException("illegal number of arguments for function: "
                                        + this.function);
                            Collections.reverse(this.arguments);
                            for (Node arg : this.arguments) result.addAll(arg.compile());
                            result.add(CompiledToken.newFunction((ImpureFunction) this.function));
                        } else throw new IllegalStateException("illegal function class: "
                                    + this.function.getClass().getName());
                        break;
                } // end switch
            } // end else
            return result;
        } // end compile()

        @Override
        public String toString() {
            return this.compile().toString();
        }
    }

    private Node number(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size()) throw new IllegalStateException("expected number, but expression ended");

        final Token token = tokens.get(index.get());

        if (index.get() < tokens.size()) index.set(index.get() + 1);
        else throw new IllegalStateException("expected number, but the expression ended (near " + token + ")");

        if (token.isNumber()) return new Node(Double.parseDouble(token.getContent()));
        else if (token.isSymbol()) {
            final Token next = index.get() >= tokens.size() ? null : tokens.get(index.get());
            if (next != null && next.isOpeningBracket()) { // function call
                index.set(index.get() - 1);
                return functionCall(tokens, index);
            } else { // constant or argument
                if (this.arguments.contains(token.getContent()))
                    return new Node(this.arguments.indexOf(token.getContent()));
                if (!this.context.hasConstant(token.getContent()))
                    throw new IllegalStateException("non-existent constant: " + token.getContent());

                return new Node(this.context.getConstant(token.getContent()));
            }
        } else if (token.isOpeningBracket()) {
            //if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            //else throw new IllegalStateException("expected inner expression, but expression ended");

            Node result = this.expression(tokens, index);
            if (index.get() >= tokens.size())
                throw new IllegalStateException("expected closing bracket, but expression ended");

            if (!tokens.get(index.get()).isClosingBracket())
                throw new IllegalStateException("expected closing bracket, but got '"
                        + tokens.get(index.get()).getContent() + "'");

            if (index.get() < tokens.size()) index.set(index.get() + 1);

            return result;
        } else {
            throw new IllegalStateException("invalid number: "+token.getContent());
        }
    }

    private Node functionCall(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected function call, but expression ended");

        final Token token = tokens.get(index.get());
        if (!token.isSymbol() || !this.context.hasFunction(token.getContent()))
            throw new IllegalStateException("non-existent function: " + token.getContent());

        if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
        else throw new IllegalStateException("expected opening bracket but expression ended");

        final Token next = tokens.get(index.get());
        if (!next.isOpeningBracket()) throw new IllegalStateException("illegal function call: " + token.getContent());

        if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
        else throw new IllegalStateException("expected argument list but expression ended");

        final List<Node> args = this.argumentList(tokens, index);
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected closing bracket after argument list for " + token.getContent()
                    + ", but expression ended");

        if (!tokens.get(index.get()).isClosingBracket())
            throw new IllegalStateException("expected closing bracket, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size()) index.set(index.get() + 1);

        return new Node(this.context.getFunction(token.getContent()), args);
    }

    private List<Node> argumentList(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected argument list, but expression ended");

        final List<Node> arguments = new LinkedList<>();
        arguments.add(this.expression(tokens, index));
        if (index.get() >= tokens.size()) return arguments;
        //throw new IllegalStateException("illegal end of expression after argument list");

        if (tokens.get(index.get()).isComma()) {
            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("expected argument, comma or closing bracket but expression ended");
            arguments.addAll(this.argumentList(tokens, index));
        }
        return arguments;
    }

    private Node factor(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size()) throw new IllegalStateException("expected factor, but expression ended");

        Token token = tokens.get(index.get());
        if (token.isUnaryOperator()) {
            UnaryOperation unaryOperator = UnaryOperation.ofSign(token.getContent());

            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("expected factor but expression ended");

            return new Node(unaryOperator.getOperation(), this.factor(tokens, index));
        } else
            return this.power(tokens, index);
    }

    private Node power(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected exponential expression, but expression ended");
        Node number = this.number(tokens, index);

        if (index.get() >= tokens.size()
                || !tokens.get(index.get()).getContent().equals(BinaryOperation.EXPONENTIATION.getSign()))
            return number;

        if (index.get() < tokens.size() - 1) index.set(index.get() + 1); // skip "^" and go to start of factor
        else throw new IllegalStateException("expected factor, but expression ended");

        Node factor = this.factor(tokens, index);

        return new Node(BinaryOperation.EXPONENTIATION.getOperation(), number, factor);
    }

    private Node product(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size()) throw new IllegalStateException("expected product, but expression ended");
        Node result = this.factor(tokens, index);

        if (index.get() >= tokens.size()) return result;

        Token next = tokens.get(index.get());
        while (BinaryOperation.DIVISION.getSign().equals(next.getContent()) ||
               BinaryOperation.MULTIPLICATION.getSign().equals(next.getContent()) ||
               BinaryOperation.MODULUS.getSign().equals(next.getContent())) {
            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("expected factor, but expression ended");

            result = new Node(BinaryOperation.ofSign(next.getContent()).getOperation(),
                    result,
                    this.factor(tokens, index));

            if (index.get() < tokens.size()) next = tokens.get(index.get());
            else break;
        }
        return result;
    }

    private Node expression(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected inner expression, but expression ended");
        Node result = this.product(tokens, index);

        if (index.get() >= tokens.size()) return result;

        Token next = tokens.get(index.get());
        while (BinaryOperation.PLUS.getSign().equals(next.getContent()) ||
               BinaryOperation.MINUS.getSign().equals(next.getContent())) {
            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("expected product, but expression ended");

            result = new Node(BinaryOperation.ofSign(next.getContent()).getOperation(),
                    result,
                    this.product(tokens, index));

            if (index.get() < tokens.size()) next = tokens.get(index.get());
            else break;
        }
        return result;
    }

    private String symbol(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size()) throw new IllegalStateException("expected symbol, but expression ended");
        final Token token = tokens.get(index.get());

        if (index.get() < tokens.size()) index.set(index.get() + 1);

        if (!token.isSymbol()) throw new IllegalStateException("expected symbol, but got: " + token.getContent());

        return token.getContent();
    }

    private List<String> symbolList(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size()) throw new IllegalStateException("expected symbol list, but expression ended");

        final List<String> result = new LinkedList<>();
        result.add(this.symbol(tokens, index));

        if (index.get() >= tokens.size())
            throw new IllegalStateException("illegal end of expression after symbol list");

        if (tokens.get(index.get()).isComma()) {
            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("excepted argument, but expression ended");
            result.addAll(this.symbolList(tokens, index));
        }

        return result;
    }

    private Function functionDefinition(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected function definition, but expression ended");

        final String name = this.symbol(tokens, index);
        if (index.get() >= tokens.size())
            throw new IllegalStateException("illegal end of expression after function declaration of " + name);

        if (!tokens.get(index.get()).isOpeningBracket())
            throw new IllegalStateException("expected opening bracket, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
        else throw new IllegalStateException("expected symbol list, but expression ended");

        final List<String> symbolList = this.symbolList(tokens, index);
        if (index.get() >= tokens.size())
            throw new IllegalStateException("illegal end of expression after argument list of " + name);

        if (!tokens.get(index.get()).isClosingBracket())
            throw new IllegalStateException("expected closing bracket, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
        else throw new IllegalStateException("expected equals operator, but expression ended");

        if (!tokens.get(index.get()).isEqualsOperator())
            throw new IllegalStateException("expected equals operator, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size()) index.set(index.get() + 1);
        else throw new IllegalStateException("expected function body of " + name + ", but the expression ended");

        List<CompiledToken> compiled;
        if (this.useFallbackParser) {
            compiled = this.fallbackExpression(tokens, index, 0, symbolList);
        } else {
            this.arguments = symbolList;
            final Node expression = this.expression(tokens, index);
            this.arguments = Collections.emptyList();
            compiled = expression.compile();
        }

        CompiledToken[] compiledTokens = compiled.toArray(new CompiledToken[compiled.size()]);

        Function f =  symbolList.size() == 1 ?
                new PureFunction(compiledTokens, name) :
                new ImpureFunction(symbolList.size(), compiledTokens, name);

        this.context.addFunction(name, f);

        return f;
    }

    private double constantValue(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected constant definition, but expression ended");

        final List<CompiledToken> compiled;
        if (this.useFallbackParser)
            compiled = this.fallbackExpression(tokens, index, 0, Collections.emptyList());
        else {
            this.arguments = Collections.emptyList();
            Node expression = expression(tokens, index);
            compiled = expression.compile();
        }

        if (compiled.size() != 1) throw new IllegalStateException("expected constant expression");

        CompiledToken token = compiled.get(0);
        if (token.type != CompiledToken.Type.NUMBER)
            throw new IllegalStateException("expected number, but got: " + token);

        return token.number;
    }

    private double constantDefinition(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected constant definition, but expression ended");

        final String name = this.symbol(tokens, index);
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected definition of constant " + name + ", but the expression ended"
                    + " (expected '=')");

        if (!tokens.get(index.get()).isEqualsOperator())
            throw new IllegalStateException("expected equals sign, but got: " + tokens.get(index.get()));
        index.set(index.get() + 1);
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected value or expression for constant " + name
                    + ", but the expression ended");

        final double value;
        final Token tmpNext = tokens.get(index.get());
        if (tmpNext.isSymbol() && !this.context.hasConstant(tmpNext.getContent()))
            value = this.constantDefinition(tokens, index);
        else value = constantValue(tokens, index);

        this.context.addConstant(name, value);

        return value;
    }

    private void definition(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected definition, but expression ended");

        final int oldIndex = index.get();
        final String tmpSymbol = this.symbol(tokens, index);
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected '=' or an opening bracket after '" + tmpSymbol
                    + "', but the expression ended");

        final Token tmpNext = tokens.get(index.get());
        index.set(oldIndex);
        if (tmpNext.isEqualsOperator()) this.constantDefinition(tokens, index);
        else if (tmpNext.isOpeningBracket()) this.functionDefinition(tokens, index);
        else throw new IllegalStateException("expected '=' or an opening bracket, but got: " + tmpNext);

        if (index.get() < tokens.size())
            throw new IllegalStateException("not the whole expression could be parsed (stopped at '"
                    + tokens.get(index.get()).getContent() + "')");
    }


    // / recursive descent parser

    public static void main(String[] args) {
        Compiler compiler = new Compiler(new CompilationContext(true), false);

        String[] functions = {
                "2 * sin(5*(x - 42)) + 44454",
                "x*x*x*x*x*x*x*x*x*x*x*x*x*x/x/x/x/x/x/x/x/x/x+x+x+x+x+x",
                "x^2^3/x^2",
                "sqrt(x) + 43895708923.234",
                "sqrt(x^2 - 42^2)",
                "sin(x)/cos(4*(x-(2^3)^2)) + x^-24"
        };
        PureFunction[] compiledFunctions = new PureFunction[functions.length];
        final long compileStart = System.nanoTime();
        for (int i = 0; i < compiledFunctions.length; ++i) {
            compiler.definition("f" + i + "(x) = " + functions[i]);
            compiledFunctions[i] = (PureFunction) compiler.context.getFunction("f" + i);
        }
        final long compileEnd = System.nanoTime();
        final long compileTime = compileEnd - compileStart;
        System.out.println("Compile-Time (in ns): " + compileTime + " [ = ~" + compileTime / 1_000 + "µs = ~"
                + compileTime / 1_000_000 + "ms ]");

        MathEval me = new MathEval();

        final int NUM_TESTS = 42_000;

        for (int function = 0; function < functions.length; ++function) {
            System.out.println("Testing function #" + function + "...");
            System.out.println("f(42) = " + compiledFunctions[function].of(42));
            double runAverage = 0;
            double runAverageMathEval = 0;
            for (int i = NUM_TESTS; i --> 0; --i) {
                final PureFunction f = compiledFunctions[function];
                final double arg = (double)i;
                final long runStart = System.nanoTime();
                f.of(arg);
                final long runEnd = System.nanoTime();

                try {
                    String expr = functions[function].replaceAll("x", Double.toString(arg));
                    final long evalRunStart = System.nanoTime();
                    me.evaluate(expr);
                    final long evalRunEnd = System.nanoTime();

                    double evalRunTime = evalRunEnd - evalRunStart;
                    runAverageMathEval -= runAverageMathEval / NUM_TESTS;
                    runAverageMathEval += evalRunTime / NUM_TESTS;
                } catch (Exception ignored) { }

                // average calculation from: https://stackoverflow.com/questions/12636613/
                // how-to-calculate-moving-average-without-keeping-the-count-and-data-total
                double runTime = runEnd - runStart;
                runAverage -= runAverage / NUM_TESTS;
                runAverage += runTime / NUM_TESTS;
            }

            System.out.println("Run-Time (in ns) of function #" + function +  ": " + runAverage + " [ = "
                    + runAverage / 1_000 + "µs = " + runAverage / 1_000_000 + "ms ]");
            System.out.println("Run-Time (in ns) of MathEval" + function +  ": " + runAverage + " [ = "
                    + runAverage / 1_000 + "µs = " + runAverage / 1_000_000 + "ms ]");
            System.out.println("Custom implementation was " + runAverageMathEval / runAverage + " faster.");
        }

    }
}
