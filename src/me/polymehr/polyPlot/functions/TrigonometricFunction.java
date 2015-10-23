package me.polymehr.polyPlot.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import me.polymehr.polyPlot.functions.properties.Periodic;

/**
 * A trigonometric function with the formula:
 * <pre>
 *      f(x) = a*<i>type</i>(b*x+c)+d
 * </pre>
 * 
 * Where <i>type</i> can either be <code>sin</code>, <code>cos</code>, 
 * <code>tan</code>, <code>asin</code>, <code>acos</code> or <code>atan</code>.<br>
 * <br>
 * 
 * Variables:<br> 
 * &#8195-<code>x</code>: point at the x-axis.<br>
 * Constants:<br>
 * &#8195-<code>a</code>: describes stretch and orientation of the function in 
 * y-direction.<br>
 * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
 * &#8195-<code>b</code>: describes stretch and orientation of the function in 
 * x-direction.<br>
 * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
 * &#8195-<code>c</code>: describes the shift on the x-axis.<br>
 * &#8195&#8195Optional; default value is <code>0.0</code>.<br>
 * &#8195-<code>d</code>: describes the shift on the y-axis.<br>
 * &#8195&#8195Optional; default value is <code>0.0</code>.<br>
 * <br>
 * <b>Note:</b> expressions like e.g. <pre>24x</pre> are invalid.<br>
 * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead!
 */
public abstract class TrigonometricFunction extends Function implements Periodic {
  
  /** 
   * Format of the trigonometric function. The <code>%s</code> has to 
   * be replaced with the function type.
   */
  private final static String FORMAT_TRIGONOMETRIC_FUNCTION = 
            "(("+FunctionUtil.CONSTANT+"\\*)?([\\+\\-])?%s\\(("+FunctionUtil.CONSTANT+"\\*)?"
          + "([\\+\\-])?x"+FunctionUtil.CONSTANT_FORCED_SIGN+"?"+"\\))"+FunctionUtil.CONSTANT_FORCED_SIGN+"?";
  
  /** Function as String. */
  protected String function;
  
  /** Constant; describes stretch and orientation of the function in y-direction */
  protected double a;
  /** Constant; describes stretch and orientation of the function in x-direction. */
  protected double b;
  /** Constant; describes the shift on the x-axis. */
  protected double c;
  /** Constant; describes the shift on the y-axis. */
  protected double d;

  /**
   * Constructs a new TrigonometricFunction from a function in a String.<br>
   * The function has to have the format:
   * <pre>
   *      f(x) = a*<i>type</i>(b*x+c)+d
   * </pre>
   * 
   * Where <i>type</i> can either be <code>sin</code>, <code>cos</code>, 
   * <code>tan</code>, <code>asin</code>, <code>acos</code> or <code>atan</code>.<br>
   * <br>
   * 
   * Variables:<br> 
   * &#8195-<code>x</code>: point at the x-axis.<br>
   * Constants:<br>
   * &#8195-<code>a</code>: describes stretch and orientation of the function in 
   * y-direction.<br>
   * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
   * &#8195-<code>b</code>: describes stretch and orientation of the function in 
   * x-direction.<br>
   * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
   * &#8195-<code>c</code>: describes the shift on the x-axis.<br>
   * &#8195&#8195Optional; default value is <code>0.0</code>.<br>
   * &#8195-<code>d</code>: describes the shift on the y-axis.<br>
   * &#8195&#8195Optional; default value is <code>0.0</code>.<br>
   * <br>
   * <b>Note:</b> expressions like e.g. <pre>24x</pre> are invalid.<br>
   * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead!
   * 
   * @param type 
   *    The type of the trigonometric function. Must be either
   *    <code>sin</code>, <code>cos</code>, <code>tan</code>, 
   *    <code>asin</code>, <code>acos</code> or <code>atan</code>
   * @param function The function as a String.
   * 
   * @throws IllegalArgumentException 
   *    if <code>function</code> is invalid or 
   *    <code>null</code> or if <code>type</code> 
   *    is unknown.
   */
  public TrigonometricFunction(String function, String type) {
    
    super(String.format(FORMAT_TRIGONOMETRIC_FUNCTION, type));

    if (!(type.equals("sin") || type.equals("cos") || type.equals("tan") || 
        type.equals("asin") || type.equals("acos") || type.equals("atan")))
      throw new IllegalArgumentException("Type of trigonometric '"+type+
          "' function unknown or unsuppported.");

    if (function != null && isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid or null: " + function);

    extractConstants();

  }
  
  /**
   * Constructs a new TrigonometricFunction from the given <code>a</code>, <code>b</code>,
   * <code>c</code> and <code>d</code> values.
   * 
   * @param a The <code>a</code> value.
   * @param b The <code>b</code> value.
   * @param c The <code>c</code> value.
   * @param c The <code>c</code> value.
   * 
   * @throws IllegalArgumentException 
   *    if <code>a</code>, <code>b</code>, <code>c</code>
   *    or <code>d</code> are either <code>NaN</code> or 
   *    <code>positive/negative infinity</code> or if 
   *    <code>type</code> is unknown.
   */
  public TrigonometricFunction(String type, double a, double b, double c, double d) {
    
    super(String.format(FORMAT_TRIGONOMETRIC_FUNCTION, type));
    
    if (!(type.equals("sin") || type.equals("cos") || type.equals("tan") || 
        type.equals("asin") || type.equals("acos") || type.equals("atan")))
      throw new IllegalArgumentException("Name of trigonometric '"+type+"' function unknown or unsuppported.");
    
    for (double e : new double[]{a,b,c,d})
      if (e == Double.NaN || e == Double.POSITIVE_INFINITY || e == Double.NEGATIVE_INFINITY)
        throw new IllegalArgumentException("A value cannot be NaN or positive/negative infinity!");
    
    String function = a + "*" + type + "(" + b + "*x" + (c>=0?"+"+c:c) + ")" + (d>=0?"+"+d:d);
    
    if (isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid:" + function);
    
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
  }

  @Override
  public String getFunctionTerm() {
    return function;
  }
  
  @Override
  public HashMap<String, Double> getConstants() {
    HashMap<String, Double> constants = new HashMap<String, Double>(4);
    constants.put("a", a);
    constants.put("b", b);
    constants.put("c", c);
    constants.put("d", d);
    return constants;
  }

  @Override
  public List<String> getVariables() {
    ArrayList<String> variables = new ArrayList<String>(1);
    variables.add("x");
    return variables;
  }

  @Override
  protected void extractConstants() {
    Matcher f = getMatcher();
    f.matches();
    
    // Initialising values.
    a = (f.group(4) != null && f.group(4).equals("-") ? -1.0 : 1.0) *
        (f.group(3) == null ? 1.0 : Double.parseDouble(f.group(3)));
    b = (f.group(7) != null && f.group(7).equals("-") ? -1.0 : 1.0) *
        (f.group(6) == null ? 1.0 : Double.parseDouble(f.group(6)));
    c =  f.group(8) == null ? 0.0 : Double.parseDouble(f.group(8));
    d =  f.group(9) == null ? 0.0 : Double.parseDouble(f.group(9));

  }
  
  @Override
  public double getPeriod() {
    return (2*Math.PI)/Math.abs(b);
  }

}
