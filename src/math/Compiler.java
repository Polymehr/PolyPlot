package math;

import me.polymehr.polyPlot.MathEval;

import javax.swing.plaf.FontUIResource;
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

    private final CompilationContext context;

    public Compiler(CompilationContext context) {
        this.context = Objects.requireNonNull(context, "compilation context must not be null");
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

    public static void main(String[] args) {
        Compiler c = new Compiler(new CompilationContext(true));
        String testExpr = "2*3+a";
        List<CompiledToken> testExprRes = c.fallbackExpression(tokenize(testExpr), new MutableInteger(), 0, Arrays.asList("a"));
        System.out.println("TEST: " + testExprRes);
        PureFunction funnn = new PureFunction(testExprRes.toArray(new CompiledToken[testExprRes.size()]), "fun");
        System.out.println(funnn.of(1));
        if (true) return;
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

    }
}
