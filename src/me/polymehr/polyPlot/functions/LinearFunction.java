package me.polymehr.polyPlot.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

/**
 * A simple linear function with the formula:
 * <pre>
 *      f(x) = m*x+n
 * </pre>
 * 
 * Variables:<br> 
 * &#8195-<code>x</code>: point at the x-axis.<br>
 * Constants:<br>
 * &#8195-<code>m</code>: describes the slope of the function.<br>
 * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
 * &#8195-<code>n</code>: describes the point intersection with the y-axis.<br>
 * &#8195&#8195Optional; default value is <code>0.0</code>.<br>
 * <br>
 * <b>Note:</b> expressions like e.g. <pre>24x</pre> are invalid.<br>
 * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead! 
 */
public class LinearFunction extends Function {
  
  /** Format of the Linear. */
  private static final String FORMAT_LINEAR = 
      "(("+FunctionUtil.CONSTANT+"\\*)?([\\+\\-])?x)"+FunctionUtil.CONSTANT_FORCED_SIGN+"?";
  
  /** Function as String.*/
  private String function;
  
  /** Constant; describes the slope of the function.*/
  private double m;
  /** Constant; describes the point intersection with the y-axis.*/
  private double n;
  
  /**
   * Constructs a new LinearFunction from a function in a String.<br>
   * The function has to have the format:
   * <pre>
   *      f(x) = m*x+n
   * </pre>
   * 
   * Variables:<br> 
   * &#8195-<code>x</code>: point at the x-axis.<br>
   * Constants:<br>
   * &#8195-<code>m</code>: describes the slope of the function.<br>
   * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
   * &#8195-<code>n</code>: describes the point intersection with the y-axis.<br>
   * &#8195&#8195Optional; default value is <code>0.0</code>.<br>
   * <br>
   * <b>Note:</b> expressions like e.g. <pre>24x</pre> are invalid.<br>
   * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead! 
   * 
   * @param function The function as a String.
   * 
   * @throws IllegalArgumentException 
   *    if <code>function</code> is invalid or 
   *    <code>null</code>.
   */
  public LinearFunction(String function) {
    
    super(FORMAT_LINEAR);
    
    if (function != null && isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid or null: " + function);
    
    extractConstants();
  }
  
  /**
   * Constructs a new LinearFunction from the given <code>m</code> and <code>n</code> values.
   * 
   * @param m The <code>m</code> value.
   * @param n The <code>n</code> value.
   * 
   * @throws IllegalArgumentException 
   *    if <code>m</code> or <code>n</code> are either <code>NaN</code> or 
   *    <code>positive/negative infinity</code>.
   */
  public LinearFunction(double m, double n) {
    super(FORMAT_LINEAR);
    
    for (double d : new double[]{m,n})
      if (d == Double.NaN || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY)
        throw new IllegalArgumentException("A value cannot be NaN or positive/negative infinity!");
    
    String function = m + "*x" + (n>=0?"+"+n:n);
    
    if (isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid:" + function);
    
    this.m = m;
    this.n = n;
  }

  @Override
  public List<Double> calculateRoots() {
    ArrayList<Double> root = new ArrayList<Double>(1);
    root.add(-n/m);
    return root;
  }

  @Override
  public String getFunctionTerm() {
    return function;
  }
  
  @Override
  public HashMap<String, Double> getConstants() {
    HashMap<String, Double> constants = new HashMap<String, Double>(2);
    constants.put("n", n);
    constants.put("m", m);
    return constants;
  }

  @Override
  protected void extractConstants() {

    Matcher f = getMatcher();
    f.matches();
    
    // Initialising values.
    m = (f.group(4) != null && f.group(4).equals("-") ? -1.0 : 1.0) *
        (f.group(3) == null ? 1.0 : Double.parseDouble(f.group(3)));
    n =  f.group(5) == null ? 0.0 : Double.parseDouble(f.group(5)); 
      
  }

  @Override
  public List<String> getVariables() {
    ArrayList<String> variables = new ArrayList<String>(1);
    variables.add("x");
    return variables;
  }

  @Override
  public String getName() {
    return "Linear Function";
  }

}
