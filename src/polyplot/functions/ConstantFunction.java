package polyplot.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import polyplot.MathEval;

/**
 * Represents a function, that has a constant value like
 * <code>41+1</code>.
 */
public class ConstantFunction extends Function {
  
  /** Format of the ConstantFunction.*/
  private static final String FORMAT_CONSTANT = "[0-9a-zA-z\\.\\+\\-\\*/\\(\\)]+";
  
  /** Function as String.*/
  private String function;
  
  /** The constant.*/
  private double constant;
  
  /**
   * Constructs a function, that has a constant value like
   * <code>41+1</code>.
   * @param function the function term.
   */
  public ConstantFunction(String function) {
    super(FORMAT_CONSTANT);
    
    if (function != null && isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid or null: " + function);
    extractConstants();
  }
  
  /**
   * Constructs a function, that has a constant value like
   * <code>41+1</code>.
   * @param constant the constant.
   */
  public ConstantFunction(double constant) {
    super(FORMAT_CONSTANT);
    
    if (constant == Double.NaN || constant == Double.POSITIVE_INFINITY || constant == Double.NEGATIVE_INFINITY)
      throw new IllegalArgumentException("A value cannot be NaN or positive/negative infinity!");
    
    String function = Double.toString(constant);
    
    if (isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid:" + function);
    
    this.constant = constant;

  }

  @Override
  public List<Double> calculateRoots() {
    ArrayList<Double> root = new ArrayList<Double>(0);
    if (constant == 0)
      root.add(Double.POSITIVE_INFINITY);
    return root;
  }

  @Override
  public String getFunctionTerm() {
    return function;
  }

  @Override
  public HashMap<String, Double> getConstants() {
    HashMap<String, Double> constant = new HashMap<String, Double>(1);
    constant.put("c", this.constant);
    return constant;
  }

  @Override
  public List<String> getVariables() {
    return new ArrayList<String>(0);
  }

  @Override
  public String getName() {
    return "Constant";
  }

  @Override
  protected void extractConstants() {
    constant = new MathEval().evaluate(function);
  }
  
  @Override
  protected boolean isValid(String function) {
    boolean valid = false;
    try {
      MathEval math = new MathEval();
      math.evaluate(function);
      
      valid = math.previousExpressionConstant();
    } catch (Exception e) {}
    
    return valid && super.isValid(function);
  }
  
  @Override
  public double calculate(HashMap<String, Double> variables) {
    return constant;
  }

}
