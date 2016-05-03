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
    private final static Pattern DEFINITON_SEPERATOR = Pattern.compile("\\s*;\\s*");

    private final CompilationContext context;

    private final boolean useFallbackParser;

    private List<String> arguments = Collections.emptyList();

    public Compiler(CompilationContext context, boolean useFallbackParser) {
        this.context = Objects.requireNonNull(context, "compilation context must not be null");
        this.useFallbackParser = useFallbackParser;
    }

    public void definition(String expression) {
        String[] definitions = expression.split(DEFINITON_SEPERATOR.pattern());
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
                    List<CompiledToken> recursiveRes = fallbackExpression(tokens, startIndex.set(++i),
                            brackets + 1, args);
                    i = startIndex.get();
                    result.addAll(recursiveRes);
                    Function f = this.context.getFunction(token.getContent());
                    if (null == f)
                        throw new IllegalArgumentException("invalid function: "+token.getContent());
                    if (f instanceof DoubleUnaryOperator)
                        result.add(CompiledToken.newUnaryOperation((DoubleUnaryOperator) f));
                    else if (f instanceof  DoubleBinaryOperator)
                        result.add(CompiledToken.newBinaryOperation((DoubleBinaryOperator) f));
                    else if (f instanceof ImpureFunction)
                        result.add(CompiledToken.newFunction((ImpureFunction) f));
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
    // <number>        ::= [<digit>] | <function_call> | <symbol>
    // <power>         ::= <number> | <number> "^" <factor>
    // <factor>        ::= <power> | <unary_sign> <factor> | "(" <expression> ")"
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
        else throw new IllegalStateException("illegal end of expression");

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
            throw new IllegalStateException("expected closing bracket, but expression ended");

        if (!tokens.get(index.get()).isClosingBracket())
            throw new IllegalStateException("expected closing bracket, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size()) index.set(index.get() + 1);
        else throw new IllegalStateException("illegal end of expression");

        return new Node(this.context.getFunction(token.getContent()), args);
    }

    private List<Node> argumentList(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected argument list, but expression ended");

        final List<Node> arguments = new LinkedList<>();
        arguments.add(this.expression(tokens, index));
        if (index.get() >= tokens.size()) throw new IllegalStateException("illegal end of expression");

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
        } else if (token.isOpeningBracket()) {
            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("expected inner expression, but expression ended");

            Node result = this.expression(tokens, index);
            if (index.get() >= tokens.size())
                throw new IllegalStateException("expected closing bracket, but expression ended");

            if (!tokens.get(index.get()).isClosingBracket()) throw new IllegalStateException("missing closing bracket");

            if (index.get() < tokens.size()) index.set(index.get() + 1);
            else throw new IllegalStateException("illegal end of expression");

            return result;
        } else {
            return this.power(tokens, index);
        }
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
        else throw new IllegalStateException("illegal end of expression");

        if (!token.isSymbol()) throw new IllegalStateException("expected symbol, but got: " + token.getContent());

        return token.getContent();
    }

    private List<String> symbolList(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size()) throw new IllegalStateException("expected symbol list, but expression ended");

        final List<String> result = new LinkedList<>();
        result.add(this.symbol(tokens, index));

        if (index.get() >= tokens.size()) throw new IllegalStateException("illegal end of expression");

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
        if (index.get() >= tokens.size()) throw new IllegalStateException("illegal end of expression");

        if (!tokens.get(index.get()).isOpeningBracket())
            throw new IllegalStateException("expected opening bracket, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
        else throw new IllegalStateException("expected symbol list, but expression ended");

        final List<String> symbolList = this.symbolList(tokens, index);
        if (index.get() >= tokens.size()) throw new IllegalStateException("illegal end of expression");

        if (!tokens.get(index.get()).isClosingBracket())
            throw new IllegalStateException("expected closing bracket, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
        else throw new IllegalStateException("expected equals operator, but expression ended");

        if (!tokens.get(index.get()).isEqualsOperator())
            throw new IllegalStateException("expected equals operator, but got: "
                    + tokens.get(index.get()).getContent());

        if (index.get() < tokens.size()) index.set(index.get() + 1);
        else throw new IllegalStateException("illegal end of expression");

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

        Function f =  this.arguments.size() == 1 ?
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
        if (index.get() >= tokens.size()) throw new IllegalStateException("illegal end of expression");

        if (!tokens.get(index.get()).isEqualsOperator())
            throw new IllegalStateException("expected equals sign, but got: " + tokens.get(index.get()));
        index.set(index.get() + 1);
        if (index.get() >= tokens.size()) throw new IllegalStateException("illegal end of expression");

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
            throw new IllegalStateException("expected '=' or an opening bracket, but the expression ended");

        final Token tmpNext = tokens.get(index.get());
        index.set(oldIndex);
        if (tmpNext.isEqualsOperator()) this.constantDefinition(tokens, index);
        else if (tmpNext.isOpeningBracket()) this.functionDefinition(tokens, index);
        else throw new IllegalStateException("expected '=' or an opening bracket, but got: " + tmpNext);
    }


    // / recursive descent parser

    public static void main(String[] args) {
        Compiler c = new Compiler(new CompilationContext(true), false);
        String testExpr = "a + 2*3";
        List<CompiledToken> testExprRes = c.fallbackExpression(tokenize(testExpr), new MutableInteger(), 0, Collections.singletonList("a"));
        System.out.println("TEST: " + testExprRes);
        PureFunction funnn = new PureFunction(testExprRes.toArray(new CompiledToken[testExprRes.size()]), "fun");
        System.out.println(funnn.of(1));
        String[] exprs = {
                "42+(23^2-3*4/(3+2))%222/5*((3+2*2)/1)",
                "(42+(23^2-3*4/(3+2))%222/((3+2*2)/1))",
                "(42+(23^2-3*4/(3+2))%222/((3+2*2)/1))/(42+(23^2-3*4/(3+2))%222/((3+2*2)/1))+5",
                "(42+(23^2-3*4/(3+2))%222/((3+2*2)/1))",
                "((42+(23^2-3*4/(3+2))%222/((3+2*2)/1))/(42+(23^2-3*4/(3+2))%222/((3+2*2)/1)))+5"
        };
        for (int i = 0; i < exprs.length; ++i) {
            List<Token> tokenized = Collections.unmodifiableList(tokenize(exprs[i]));
            List<CompiledToken> result = c.fallbackExpression(tokenized, new MutableInteger(), 0, Collections.emptyList());
            System.out.println(result);
            PureFunction f = new PureFunction(result.toArray(new CompiledToken[result.size()]), "sömefünctiön");
            System.out.println(exprs[i] + "    ==    " + f.of(42.0));
        }
        String expression = "-1 + 2 * 3 / ( 3 - 4 )\t";
        List<Token> tokenized = Collections.unmodifiableList(tokenize(expression));
        List<CompiledToken> result = c.fallbackExpression(tokenized, new MutableInteger(), 0, Collections.emptyList());
        PureFunction f = new PureFunction(result.toArray(new CompiledToken[result.size()]), "sömefünctiön");
        System.out.println(f.of(42.0));
        System.err.println(
                (42+(Math.pow(23, 2)-3*4/(3+2))%222/((3+2*2)/1))/(42+(Math.pow(23, 2)-3*4/(3+2))%222/((3+2*2)/1))+5
        );

        //time check
        long ownTime = 0;
        long otherTime = 0;
        int runs = 42;
        long before, after;
        double resulttt;
        String expr = "++++++++++++++++++++++++++++++------------------------------------------------------1*sin(2^2^3*sin(sin(cos(tan(42.44442))))/(1+2+3+4+5+6+7+8+9*5*6*9*8/4^2^3^1^1^1^1)------2222)/1/1";
        List<Token> tokenizedcheck = Collections.unmodifiableList(tokenize(expr));
        List<CompiledToken> resultcheck = c.fallbackExpression(tokenizedcheck, new MutableInteger(), 0, Collections.emptyList());
        System.out.println(resultcheck);
        PureFunction fun = new PureFunction(resultcheck.toArray(new CompiledToken[result.size()]), "sömefünctiön");
        MathEval m = new MathEval();
        while( runs --> 0 ) {
            before = System.nanoTime();
            resulttt = fun.of(Double.NaN);
            after = System.nanoTime();
            ownTime += after - before;
            System.out.println("MY RESULT: "+resulttt);
            before = System.nanoTime();
            resulttt = m.evaluate(expr);
            after = System.nanoTime();
            otherTime += after - before;
            System.out.println("OTHER RESULT: "+resulttt);
        }

        System.out.println("----------------------------------------------");

        double ownAvg = ownTime / 42.0;
        double otherAvg = otherTime / 42.0;

        System.out.println("MY AVERAGE: "+ownAvg);
        System.out.println("OTHER AVERAGE: "+otherAvg);


        List<Token> tokens = tokenize("function(x,y) = 4*x^2^(3-1)+33333+sin(y)+sin(1.234)");
        c.functionDefinition(tokens, new MutableInteger());

        System.out.println(c.constantExpression("((42+(23^2-3*4/(3+2))%222/((3+2*2)/1))/(42+(23^2-3*4/(3+2))%222/((3+2*2)/1)))+5"));

        final String term = "sin(atan2(arg1, arg2*2^3 + arg1 / ( tan(1 + arg2) % 42 )) - "
                        + "++1++1++1++1++1++1++1++1++1++1++1+++++++---------+++++++++arg3/arg4) /arg5^"
                        + "(arg1 * log(PI % atan(arg3))^arg2 / 1 / 1 / 1 / 1 / 1 * 2 / (1 + 1 - 1 + 1 - 1 + (1*1*1*1*(2^2^3)))*"
                        + "sqrt(42) / sqrt(3*arg2^arg3*arg1))";
        final String theUltimateTestExpression = "leFunction(arg1, arg2, arg3, arg4, arg5) = " + term;
        System.out.println(theUltimateTestExpression);
        List<CompiledToken> fallbackResult = c.fallbackExpression(tokenize(term), new MutableInteger(), 0,
                Arrays.asList("arg1", "arg2", "arg3", "arg4", "arg5"));
        System.out.println("lefunction(1,2,3,4,5[FALLBACK] = " + new ImpureFunction(5, fallbackResult.toArray(new CompiledToken[fallbackResult.size()]), "ledäschd").of(1, 2, 3, 4, 5));

        c.definition("x = y = z =((42+(23^2-3*4/(3+2))%222/((3+2*2)/1))/(42+(23^2-3*4/(3+2))%222/((3+2*2)/1)))+5;"
                + theUltimateTestExpression);

        System.err.println("recursively optimized: " + c.context.getFunction("lefunction"));

        System.err.println("lefunction(1,2,3,4,5) = " + c.context.getFunction("lefunction").of(1, 2, 3, 4, 5));

        System.out.println("______________________________________________________________________________");

        c.definition("f(x) = lefunction(1,2,3,4,x)");
        System.out.println(c.context.getFunction("f"));

        c.definition("test(a,b,c,d) = d - c - b - a");
        System.out.println(c.context.getFunction("test"));
        System.out.println(c.constantExpression("test(1,2,3,4)"));
        System.out.println(c.fallbackExpression(tokenize("test(1,2,3,4)"), new MutableInteger(), 0, Collections.emptyList()));
        System.out.println(c.fallbackExpression(tokenize("1+2+3+4"), new MutableInteger(), 0, Collections.emptyList()));
        c.definition("asdf(x) = 1/2/3/4");
        System.out.println(c.context.getFunction("asdf"));

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("fallback param order test: " + c.fallbackExpression(tokenize("lefunction(a,b,c,d,e)"), new MutableInteger(), 0, Arrays.asList("a", "b", "c", "d", "e")));
        c.arguments = Arrays.asList("a", "b", "c", "d", "e");
        System.out.println("recursive param order test: " + c.expression(tokenize("lefunction(a,b,c,d,e)"), new MutableInteger()));
        c.arguments = Collections.emptyList();
        System.out.println("fallback param order test: " + c.fallbackExpression(tokenize("sin(b)/a/c-d-e"), new MutableInteger(), 0, Arrays.asList("a", "b", "c", "d", "e")));
        c.arguments = Arrays.asList("a", "b", "c", "d", "e");
        System.out.println("recursive param order test: " + c.expression(tokenize("sin(b)/a/c-d-e"), new MutableInteger()));
        c.arguments = Collections.emptyList();

        System.out.println("2^3^4 = " + c.fallbackExpression(tokenize("2^3^4"), new MutableInteger(), 0, Collections.emptyList()));
    }
}
