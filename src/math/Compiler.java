package math;

import polyplot.MathEval;

import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gordian
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

    private final CompilationContext context;

    private final boolean useFallbackParser;

    private List<String> arguments = Collections.emptyList();

    public Compiler(CompilationContext context, boolean useFallbackParser) {
        this.context = Objects.requireNonNull(context, "compilation context must not be null");
        this.useFallbackParser = useFallbackParser;
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
                    result.add(CompiledToken.newUnaryOperationToken(((UnaryOperation)top).getOperation()));
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
                result.add(CompiledToken.newNumberToken(Double.parseDouble(token.getContent())));
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
                        result.add(CompiledToken.newUnaryOperationToken(((DoubleUnaryOperator)f)));
                    else
                        result.add(CompiledToken.newFunction(f));
                } else if (args.contains(token.getContent())){ // token is an argument
                    result.add(CompiledToken.newArgumentToken(args.indexOf(token.getContent())));
                } else { // token is a constant
                    Double constant = this.context.getConstant(token.getContent());
                    if (null == constant) throw new IllegalArgumentException("invalid constant: "+token.getContent());
                    result.add(CompiledToken.newNumberToken(constant));
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
            else throw new IllegalArgumentException("invalid token: "+token);
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
            switch (token.TYPE) {
                case NUMBER:
                case ARGUMENT:
                    stack.push(token);
                    break;
                case UNARY_OPERATION:
                    if (!stack.isEmpty() && stack.peek().TYPE == CompiledToken.Type.NUMBER)
                        stack.push(CompiledToken.newNumberToken(
                                ((DoubleUnaryOperator) token.CONTENT).applyAsDouble(stack.pop().NUMBER)
                        ));
                    else stack.push(token);
                    break;
                case BINARY_OPERATION:
                    if (!stack.isEmpty() && stack.peek().TYPE == CompiledToken.Type.NUMBER) {
                        final CompiledToken arg1Token = stack.pop();
                        if (!stack.isEmpty() && stack.peek().TYPE == CompiledToken.Type.NUMBER) {
                            final CompiledToken arg0Token = stack.pop();
                            final double arg0 = arg0Token.NUMBER;
                            final double arg1 = arg1Token.NUMBER;
                            stack.push(CompiledToken.newNumberToken(
                                    ((DoubleBinaryOperator) token.CONTENT).applyAsDouble(arg0, arg1)
                            ));
                        } else {
                            stack.push(arg1Token);
                            stack.push(token);
                        }
                    } else stack.push(token);
                    break;
                case FUNCTION: {
                    Function f = (Function)token.CONTENT;
                    double[] args = new double[f.getNumberOfArguments()];
                    int i = 0;
                    for (; i < f.getNumberOfArguments(); ++i) {
                        if (!stack.isEmpty() && stack.peek().TYPE == CompiledToken.Type.NUMBER)
                            args[i] = stack.pop().NUMBER;
                        else break;
                    }
                    if (i == f.getNumberOfArguments()) {
                        stack.push(CompiledToken.newNumberToken(f.of(args)));
                    } else { // if not all args were constant, push them back onto the stack
                        while (--i >= 0) stack.push(CompiledToken.newNumberToken(args[i]));
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
    // <digit>         ::= "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
    // <unary_sign>    ::= "-" | "+"
    // <number>        ::= [<digit>] | <function_call> | <symbol>
    // <power>         ::= <number> | <number> "^" <factor>
    // <factor>        ::= <power> | <unary_sign> <factor> | "(" <expression> ")"
    // <product>       ::= <factor> | <factor> ("*" | "/" | "%") <product>
    // <expression>    ::= <product> | <product> ("+" | "-")  <expression>
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
            if (this.isConstant()) result.add(CompiledToken.newNumberToken(this.constantValue()));
            else {
                switch (this.type) {
                    case CONSTANT: throw new UnsupportedOperationException("constant detection not working correctly");
                    case ARGUMENT:
                        result.add(CompiledToken.newArgumentToken(this.argumentIndex));
                        break;
                    case UNARY_OPERATION:
                        if (!this.hasRight())
                            throw new IllegalStateException("unary operation without operand");
                        result.addAll(this.right.compile());
                        result.add(CompiledToken.newUnaryOperationToken(this.unaryOperation));
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
                            result.add(CompiledToken.newUnaryOperationToken((DoubleUnaryOperator) this.function));
                        }
                        else {
                            if (this.arguments == null
                                    || this.arguments.size() != this.function.getNumberOfArguments())
                                throw new IllegalStateException("illegal number of arguments for function: "
                                        + this.function);
                            Collections.reverse(this.arguments);
                            for (Node arg : this.arguments) result.addAll(arg.compile());
                            result.add(CompiledToken.newFunction(this.function));
                        }
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
        final Node factor = this.factor(tokens, index);

        if (index.get() >= tokens.size()) return factor;
        final Token next = tokens.get(index.get());

        if ("/".equals(next.getContent()) || "*".equals(next.getContent()) || "%".equals(next.getContent())) {
            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("expected factor or product, but expression ended");

            return new Node(BinaryOperation.ofSign(next.getContent()).getOperation(),
                    factor, // left
                    this.product(tokens, index)); // right
        } else return factor;
    }

    private Node expression(List<Token> tokens, MutableInteger index) {
        if (index.get() >= tokens.size())
            throw new IllegalStateException("expected inner expression, but expression ended");
        final Node product = this.product(tokens, index);

        if (index.get() >= tokens.size()) return product;

        final Token next = tokens.get(index.get());
        if ("+".equals(next.getContent()) || "-".equals(next.getContent())) {
            if (index.get() < tokens.size() - 1) index.set(index.get() + 1);
            else throw new IllegalStateException("expected sum or factor, but expression ended");

            return new Node(BinaryOperation.ofSign(next.getContent()).getOperation(),
                    product, // left
                    this.expression(tokens, index) ); // right
        } else return product;
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

        if (this.useFallbackParser) {
            //TODO
        } else {
            this.arguments = symbolList;
            final Node expression = this.expression(tokens, index);
            this.arguments = Collections.emptyList();
            System.err.println("NAME: " + name);
            System.err.println("ARGUMENTS: " + symbolList);
            System.err.println(expression.toString());
        }
        return null;
    }


    // / recursive descent parser

    public static void main(String[] args) {
        Compiler c = new Compiler(new CompilationContext(true), false);
        String testExpr = "2*3+a";
        List<CompiledToken> testExprRes = c.fallbackExpression(tokenize(testExpr), new MutableInteger(), 0, Arrays.asList("a"));
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
    }
}
