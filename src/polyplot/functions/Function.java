package polyplot.functions;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import polyplot.MathEval;

/**
 * Represents a mathematical function.<br>
 * <br>
 * Extending classed should at least implement two
 * constructors:
 * <ul>
 * <li>One with the function term as parameter.</li>
 * <li>One with (if given) all constants of the function.</li>
 * </ul>
 * <br>
 * <b>Note:</b> expressions like e.g. <pre>24x</pre> are invalid in String
 * function terms.<br>
 * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead!<br>
 */
public abstract class Function {
  
  /** Format of the function. */
  private Pattern FORMAT;
  
  /**
   * Constructs a function with its required format. 
   * @param format The required format as regular expression.
   */
  public Function(String format) {
    FORMAT = Pattern.compile(format);
  }
  
  /**
   * Returns a List, that contains all roots of this function. These can be exact or approximate.
   * @return the roots (zeros) of this function.
   */
  public abstract List<Double> calculateRoots();
  
  /**
   * Returns this Function's function term as a String.
   */
  public abstract String getFunctionTerm();
  
  /**
   * Calculates the y-value of the function with the given variables.
   * 
   * @param variables
   *    An array of values.<br>
   *    Each specified variable is assigned to a variable name (specified 
   *    by {@link #getVariables()}) in order of the variable name.
   *    If there are not enough values in <code>variables</code>, 
   *    <code>0.0</code> is assumed for every following variable name. 
   *    
   * @return The calculated y-value.
   */
  public double calculate(double ... variables) {
    List<String> names = getVariables();
    HashMap<String, Double> variableMap = new HashMap<String, Double>(names.size());
    for (int i = 0; i < names.size(); ++i)
      variableMap.put(names.get(i), i < variables.length ? variables[i] : 0.0);
    return calculate(variableMap);
  }
  
  /**
   * Calculates the y-value of the function with the given variables.
   * 
   * @param variables
   *    A HashMap containing the name and a assigned value.<br>
   *    The variables' names should match with the ones specified
   *    in {@link #getVariables()}.<br>
   *    If a variable name is unknown <code>0.0</code> is assumed. 
   *    
   * @return The calculated y-value.
   */
  public double calculate(HashMap<String, Double> variables) {
    MathEval math = new MathEval();
    for (String variable : getVariables())
      math.setVariable(variable, variables.getOrDefault(variable, 0.0));
    return math.evaluate(this.getFunctionTerm());
  }
  
  /**
   * Returns a HashMap containing all constant values and their names.<br>
   * The names should only consist of one character and already established names
   * shouldn't be used (e.g: 'e' because it's the common name for Eulers number.).
   */
  public abstract HashMap<String, Double> getConstants();
  
  /**
   * Returns a HashMap containing all constant values and their names.<br>
   * The names should only consist of one character and already established names
   * shouldn't be used (e.g: 'e' because it's the common name for Eulers number.).
   */
  public abstract List<String> getVariables();
  
  /**
   * Whether the format of of the given function matches the required one to be
   * a function of the <code>Function</code>'s type.<br>
   * The order and format of the variables and constants is important.<br>
   * The required format can be obtained via {@link #getFormat()}.<br>
   * <br>
   * <b>Note:</b> expressions like e.g. <pre>24x</pre> are invalid.<br>
   * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead!<br>
   * 
   * @param function The given function as String.
   */
  protected boolean isValid(String function) {
    return FORMAT.matcher(function).matches();
  }
  
  /**
   * Returns the specific name of this function.
   */
  public abstract String getName();
  
  /**
   * Returns the format of this function as a regular expression.
   */
  public String getFormat() {
    return FORMAT.toString();
  }
  
  /**
   * Extracts the constants from the function term to the function's corresponding 
   * members.
   */
  protected abstract void extractConstants();
  
  /**
   * Returns the Matcher of the formats Pattern (with the function
   * as argument).
   */
  protected Matcher getMatcher() {
    return FORMAT.matcher(getFunctionTerm());
  }
  
  /**
   * Returns <i>a copy</i> of this function's Format.
   */
  protected Pattern getPattern() {
    return Pattern.compile(FORMAT.pattern());
  }
  
  /** 
   * Returns this Function's function term.
   * @return The function term.
   * */
  @Override
  public String toString() {
    return getFunctionTerm();
  }
  
}
