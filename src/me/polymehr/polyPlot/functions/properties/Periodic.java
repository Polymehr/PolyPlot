package me.polymehr.polyPlot.functions.properties;

/**
 * Specifies a function that is periodic.<br>
 * A periodic function is a function that periodically
 * repeats itself in a given interval.<br>
 * The potential roots and poles of a periodic function
 * can only be calculated in a given scope.
 */
public interface Periodic {
  
  /**
   * Returns the period of a function.
   */
  public double getPeriod();

}
