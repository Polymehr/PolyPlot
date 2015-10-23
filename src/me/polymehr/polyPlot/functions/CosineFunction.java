package me.polymehr.polyPlot.functions;

import java.util.ArrayList;
import java.util.List;

/**
 * A cosine function with the formula:
 * <pre>
 *      f(x) = a*cos(b*x+c)+d
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
public class CosineFunction extends TrigonometricFunction {

  /**
   * Constructs a new CosineFunction from a function in a String.<br>
   * The function has to have the format:
   * <pre>
   *      f(x) = a*cos(b*x+c)+d
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
  public CosineFunction(String function) {
    super(function, "cos");
  }

  /**
   * Constructs a new CosineFunction from the given <code>a</code>, <code>b</code>,
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
  public CosineFunction(double a, double b, double c, double d) {
    super("cos", a, b, c, d);
  }

  @Override
  public List<Double> calculateRoots() {

    final double lowerBound = FunctionUtil.getLowerBound(),
                 upperBound = FunctionUtil.getUpperBound();
    
    ArrayList<Double> roots = new ArrayList<Double>(4);

    int n=0;
    
    if (getRoot0(0)<getRoot1(0)) {
      n = (int) ((lowerBound-getRoot0(0))/(getRoot0(1)-getRoot0(0))-(getRoot0(0)>=lowerBound?1:0));

      for (; getRoot0(n) <= upperBound; ++n) {
        if (getRoot0(n)>=lowerBound && getRoot0(n)<=upperBound && !roots.contains(getRoot0(n)))
          roots.add(getRoot0(n));
        if (getRoot1(n)>=lowerBound && getRoot1(n)<=upperBound && !roots.contains(getRoot1(n)))
          roots.add(getRoot1(n));
      }
    } else {
      n = (int) (-(lowerBound-getRoot1(0))/(getRoot1(0)-getRoot1(1))+(getRoot1(0)>=lowerBound?1:0));
      
      for (; getRoot1(n) <= upperBound; --n) {
        if (getRoot1(n)>=lowerBound && getRoot1(n)<=upperBound && !roots.contains(getRoot1(n)))
          roots.add(getRoot1(n));
        if (getRoot0(n)>=lowerBound && getRoot0(n)<=upperBound && !roots.contains(getRoot0(n)))
          roots.add(getRoot0(n));
      }
    }
    return roots;
  }
  
  /**
   * Returns the solution of the first root formula.
   * @param n The integer n.
   * @see <a href="http://www.wolframalpha.com/">Source of formula</a>
   */
  private double getRoot0(int n) {
    return 1/b*(2*Math.PI*n-c-Math.acos(-d/a));
  }
  
  /**
   * Returns the solution of the first root formula.
   * @param n The integer n.
   * @see <a href="http://www.wolframalpha.com/">Source of formula</a>
   */
  private double getRoot1(int n) {
    return 1/b*(2*Math.PI*n-c+Math.acos(-d/a));
  }
  
  @Override
  public String getName() {
    return "Cosine Function";
  }

}
