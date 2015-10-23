package me.polymehr.polyPlot.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

/**
 * A simple quadratic function with the formula:
 * <pre>
 *      f(x) = a*x^2+b*x+c
 * </pre>
 * 
 * Variables:<br> 
 * &#8195-<code>x</code>: point at the x-axis.<br>
 * Constants:<br>
 * &#8195-<code>a</code>: describes stretch and orientation of the function.<br>
 * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
 * &#8195-<code>b</code>: describes the slope of the function with the y-axis.<br>
 * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
 * &#8195-<code>c</code>: describes the shift on the y-axis.<br>
 * &#8195&#8195Optional; default value is <code>0.0</code>.<br>
 * <br>
 * <b>Note:</b> expressions like e.g. <pre>24x</pre> are invalid.<br>
 * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead! 
 */
public class ParabolaFunction extends Function {
  
  /** Format of the Parabola. */
  private static final String FORMAT_PARABOLA = 
      "(("+FunctionUtil.CONSTANT+"\\*)?([\\+\\-])?x\\^2)(("+FunctionUtil.CONSTANT_FORCED_SIGN+"\\*)?([\\+\\-])?x)?"+FunctionUtil.CONSTANT_FORCED_SIGN+"?";
  
  /** Function as String.*/
  private String function;
  
  /** Constant; describes stretch and orientation of the function.*/
  private double a;
  /** Constant; describes the slope of the function with the y-axis.*/
  private double b;
  /** Constant; describes the shift on the y-axis.*/
  private double c;
  
  /**
   * Constructs a new ParabolaFunction from a function in a String.<br>
   * The function has to have the format:
   * <pre>
   *      f(x) = a*x^2+b*x+c
   * </pre>
   * 
   * Variables:<br> 
   * &#8195-<code>x</code>: point at the x-axis.<br>
   * Constants:<br>
   * &#8195-<code>a</code>: describes stretch and orientation of the function.<br>
   * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
   * &#8195-<code>b</code>: describes the slope of the function with the y-axis.<br>
   * &#8195&#8195Optional; default value is <code>1.0</code>.<br>
   * &#8195-<code>c</code>: describes the shift on the y-axis.<br>
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
  public ParabolaFunction(String function) {
    
    super(FORMAT_PARABOLA);
    
    if (function != null && isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid or null: " + function);
    
    extractConstants();
  }
  
  /**
   * Constructs a new ParabolaFunction from the given <code>a</code>, <code>b</code>
   * and <code>c</code> values.
   * 
   * @param a The <code>a</code> value.
   * @param b The <code>b</code> value.
   * @param c The <code>c</code> value.
   * 
   * @throws IllegalArgumentException 
   *    if <code>a</code>, <code>b</code>
   *    or <code>c</code> are either <code>NaN</code> or 
   *    <code>positive/negative infinity</code>.
   */
  public ParabolaFunction(double a, double b, double c) {
    super(FORMAT_PARABOLA);
    
    for (double d : new double[]{a,b,c})
      if (d == Double.NaN || d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY)
        throw new IllegalArgumentException("A value cannot be NaN or positive/negative infinity!");
    
    String function = a + "*x^2" + (b>=0?"+"+b:b) + "*x" + (c>=0?"+"+c:c);
    
    if (isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid:" + function);
    
    this.a = a;
    this.b = b;
    this.c = c;
  }
  

  @Override
  public List<Double> calculateRoots() {
    List<Double> roots = new ArrayList<Double>(2);
    
    double discriminant = (b*b) - (4*a*c);
    System.out.println(discriminant);
    
    if (discriminant < 0 || a == 0)
      return roots;
    else {
      roots.add(((-b)-Math.sqrt(discriminant))/(2*a));
      if (discriminant > 0)
        roots.add(((-b)+Math.sqrt(discriminant))/(2*a));
    }
    return roots;
  }

  @Override
  public String getFunctionTerm() {
    return function;
  }

  @Override
  public HashMap<String, Double> getConstants() {
    HashMap<String, Double> constants = new HashMap<String, Double>(3);
    constants.put("a", a);
    constants.put("b", b);
    constants.put("c", c);
    return constants;
  }
  
  @Override
  public void extractConstants() {
    
    Matcher f = getMatcher();
    f.matches();
    
    // Initialising values.
    a = (f.group(4) != null && f.group(4).equals("-") ? -1.0 : 1.0) *
        (f.group(3) == null ? 1.0 : Double.parseDouble(f.group(3)));
    b = (f.group(6) == null ? 0.0 : 
         f.group(8) != null && f.group(8).equals("-") ? -1.0 : 1.0) *
        (f.group(7) == null ? 1.0 : Double.parseDouble(f.group(7)));
    c =  f.group(9) == null ? 0.0 : Double.parseDouble(f.group(9));
    
  }

  @Override
  public List<String> getVariables() {
    ArrayList<String> variable = new ArrayList<String>(1);
    variable.add("x");
    return variable;
  }

  @Override
  public String getName() {
    return "Parabola (quadratic equation)";
  }

}
