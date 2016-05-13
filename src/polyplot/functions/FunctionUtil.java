package polyplot.functions;

import java.util.List;

import polyplot.MathEval;

/**
 * A utility class for Function containing some 
 * useful constants and methods.<br>
 * Some functionality can only be used from extending classes.
 */
public  class FunctionUtil {
  /** Accuracy while approximately computing roots of a function.*/ 
  private static double accuracy = 0.000000001;
  /** Lower bound while approximately computing roots of a function.*/
  private static double lowerBound = -300;
  /** Upper bound while approximately computing roots of a function.*/
  private static double upperBound = +300;
  /** The step width for the root searcher.*/
  private static double stepWidth = 0.2;
  
  /** 
   * Regex String capturing a constant with optional sign (in the format <code>"<i>d</i>"</code>, 
   * <code>"<i>d</i>.<i>d</i>"</code> and <code>".<i>d</i>"</code>.) with a group to get the 
   * number itself. 
   */
  static final String CONSTANT             = "([+-]?\\d*?\\.?\\d+?)";
  /** 
   * Regex String capturing a constant with a leading sign (in the format <code>"<i>d</i>"</code>, 
   * <code>"<i>d</i>.<i>d</i>"</code> and <code>".<i>d</i>"</code>.) with a group to get the 
   * number itself. 
   */
  static final String CONSTANT_FORCED_SIGN = "([+-]\\d*?\\.?\\d+?)";
  /** Regex String capturing every operator ("+ - * / ^ (") <i>before</i> a variable or constant.*/ 
  static final String OPERATOR_BEFORE      = "[\\+\\-\\*/\\^\\(]+?";
  /** Regex String capturing every operator ("+ - * / ^ )") <i>after</i> a variable or constant.*/
  static final String OPERATOR_AFTER       = "[\\+\\-\\*/\\^\\)]+?";
 
  
  
  /** Returns accuracy while approximately computing roots of a function.*/
  public static double getAccuracy() {
    return accuracy;
  }
  /** 
   * Sets accuracy while approximately computing roots of a function.
   * @param accuracy Accuracy to be set. Has to be <code>>0</code>.
   */
  public static void setAccuracy(double accuracy) {
    if (accuracy > 0)
      FunctionUtil.accuracy = accuracy;
  }
  /** Returns the step width for the root searcher.*/
  public static double getStepWidth() {
    return stepWidth;
  }
  /** 
   * Sets the step width for the root searcher.
   * @param stepWidth Step width to be set. Has to be <code>>0</code>.
   */
  public static void setStepWidth(double stepWidth) {
    if (stepWidth > 0)
      FunctionUtil.stepWidth = stepWidth;
  }
  /**
   * Returns the lower bound while approximately computing roots of a function.
   */
  public static double getLowerBound() {
    return lowerBound;
  }
  /**
   * Sets the lower bound while approximately computing roots of a function.
   * @param lowerBound 
   *  The lower bound to set. The lower bound cannot be
   *  bigger or equal to the <code>upper bound</code>.
   */
  public static void setLowerBound(double lowerBound) {
    if (lowerBound < upperBound)
      FunctionUtil.lowerBound = lowerBound;
  }
  /**
   * Returns the upper bound while approximately computing roots of a function.
   */
  public static double getUpperBound() {
    return upperBound;
  }
  /**
   * Sets the upper bound while approximately computing roots of a function.
   * @param upperBound 
   *  The upper bound to set. The lower bound cannot be
   *  smaller or equal to the <code>lower bound</code>.
   */
  public static void setUpperBound(double upperBound) {
    if (upperBound > lowerBound)
    FunctionUtil.upperBound = upperBound;
  }
  
  /**
   * Adds a value to a specified list if the value does not lies within the
   * accuracy span of the existing values.
   * @param value The value to be added.
   * @param toList The list the value will be added to.
   */
  static void add(double value, List<Double> toList) {
    for (double d : toList)
      if (d+FunctionUtil.getAccuracy()>= value && d-FunctionUtil.getAccuracy()<=value)
        return;
    toList.add(value);
  }
  
  /**
   * Gets a specific Function based on the function term.<br>
   * If no matching Function is found, a <code>CommonFunction</code>
   * will be returned.<br>
   * <br>
   * <b>Note:</b> The recognition algorithm is order and format sensitive
   * for variables and constants. More information should be given in the 
   * class description of the desired function.<br>
   * <b>Also:</b> expressions like e.g. <pre>24x</pre> are invalid.<br>
   * Expressions like e.g. <pre>42<b>*</b>x</pre>must be used instead!<br>
   * 
   * @param functionTerm 
   *  The function term. This term must be evaluable with 
   *  {@link MathEval#evaluate(String)}. 
   * @return 
   *  A Function, that fits best to the function term.<br>
   *  Returns a new <code>CommonFunction</code> if no other function fits.
   * 
   * @throws IllegalArgumentException
   *  if the function term is in a invalid format. 
   */
  public static Function getFunctionByTerm(String functionTerm) {
    if (functionTerm == null || functionTerm.isEmpty())
      throw new IllegalArgumentException("'functionTerm' cannot be empty or null!");
    
    return new CommonFunction(functionTerm);
  }
  
  /** Static class. */
  private FunctionUtil() {}
}
