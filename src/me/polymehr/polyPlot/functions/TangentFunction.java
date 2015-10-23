package me.polymehr.polyPlot.functions;

import java.util.ArrayList;
import java.util.List;

import me.polymehr.polyPlot.functions.properties.Poles;

/**
 * A tangent function with the formula:
 * <pre>
 *      f(x) = a*tan(b*x+c)+d
 * </pre>
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
public class TangentFunction extends TrigonometricFunction implements Poles {
  
  /** The poles of this function. */
  private ArrayList<Double> poles;

  /**
   * Constructs a new TangentFunction from a function in a String.<br>
   * The function has to have the format:
   * <pre>
   *      f(x) = a*tan(b*x+c)+d
   * </pre>
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
   * @param function The function as a String.
   * 
   * @throws IllegalArgumentException 
   *    if <code>function</code> is invalid or 
   *    <code>null</code>.
   */
  public TangentFunction(String function) {
    super(function, "tan");
  }

  /**
   * Constructs a new TangentFunction from the given <code>a</code>, <code>b</code>,
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
   *    <code>positive/negative infinity</code>.
   */
  public TangentFunction(double a, double b, double c, double d) {
    super("tan", a, b, c, d);
  }

  @Override
  public List<Double> calculateRoots() {
    
    final double lowerBound = FunctionUtil.getLowerBound(),
                 upperBound = FunctionUtil.getUpperBound();

    ArrayList<Double> roots = new ArrayList<Double>(4);
    
    int n = 0;
    
    if (getRoot(0)<getRoot(1)) {
      n = (int) ((lowerBound-getRoot(0))/(getRoot(1)-getRoot(0))-(getRoot(0)>=lowerBound?1:0));
      
      for (; getRoot(n) <= upperBound; ++n)
        if (getRoot(n)>=lowerBound && getRoot(n)<=upperBound)
          roots.add(getRoot(n));
      
    } else {
      n = (int) (-(lowerBound-getRoot(1))/(getRoot(0)-getRoot(1))+(getRoot(0)>=lowerBound?2:1));
      
      for (; getRoot(n) <= upperBound; --n)
        if (getRoot(n)>=lowerBound && getRoot(n)<=upperBound)
          roots.add(getRoot(n));
    }

    return roots;
  }
  
  /**
   * Returns the solution of the root formula.
   * @param n The integer n.
   * @see <a href="http://www.wolframalpha.com/">Source of formula</a>
   */
  private double getRoot(int n) {
    return 1/b*(Math.PI*n-c-Math.atan(d/a));
  }

  @Override
  public void calculatePoles() {

    final double lowerBound = FunctionUtil.getLowerBound(),
                 upperBound = FunctionUtil.getUpperBound();
    
    if (poles == null)
      poles = new ArrayList<Double>(4);
    else
      poles.clear();

    int n = 0;

    if (getPole(0)<getPole(1)) {
      n = (int) ((lowerBound-getPole(0))/(getPole(1)-getPole(0))-(getPole(0)>=lowerBound?1:0));
      
      for (; getPole(n) <= upperBound; ++n)
        if (getPole(n)>=lowerBound && getPole(n)<=upperBound)
          poles.add(getPole(n));

    } else {
      n = (int) (-(lowerBound-getPole(1))/(getPole(0)-getPole(1))+(getPole(0)>=lowerBound?2:1));
      
      for (; getPole(n) <= upperBound; --n)
        if (getPole(n)>=lowerBound && getPole(n)<=upperBound)
          poles.add(getPole(n));
    }

  }
  
  /**
   * Returns the solution of the pole formula.
   * @param n The integer n.
   * @see <a href="http://www.wolframalpha.com/">Source of formula</a>
   */
  private double getPole(int n) {
    return -c/b+1/b*(Math.PI/2+n*Math.PI);
  }

  @Override
  public boolean isPole(double x0, double x1) {
    if (poles == null)
      calculatePoles();
    
    for (double p : poles)
      if (x1 >= p && x0 <= p)
        return true;
    return false;
  }
  
  @Override
  public double getPeriod() {
    return Math.PI/Math.abs(b);
  }

  @Override
  public String getName() {
    return "Tangent Function";
  }

  @Override
  public List<Double> getPoles() {
    if (poles == null)
      calculatePoles();
    
    return poles;
  }

}
