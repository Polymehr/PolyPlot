package polyplot.functions.properties;

import java.util.List;

/**
 * Specifies that a Function is able to have poles.<br>
 * Poles are points in the function where the y-value tends
 * to positive or negative infinity, usually if a division 
 * by <code>0</code> occurs.
 */
public interface Poles {
  
  /**
   * Returns the poles of the function (the points where the y-value is undefined; 
   * usually if a division by <code>0</code> occurs).<br>
   * Normally returns the pre-calculated pole-<code>List</code>.
   */
  public List<Double> getPoles();
  
  /**
   * Returns whether a pole of the function lies between the two x-values.<br>
   * Normally the value will be compared with the values previously calculated
   * with {@link #recalculatePoles()}.
   * @param x0 The lower bound of the span.
   * @param x1 The upper bound of the span.
   * @throws IllegalArgumentException
   *    if <code>x0</code> is bigger or equal to <code>x1</code>.
   */
  public boolean isPole(double x0, double x1) throws IllegalArgumentException;
  
  /**
   * Calculates the poles of this function.<br>
   * Normally the poles will be stored in a  <code>List</code>
   * after this calculation.
   */
  public void calculatePoles();
}
