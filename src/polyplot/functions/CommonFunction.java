package polyplot.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import polyplot.MathEval;
import polyplot.functions.properties.Approximate;
import polyplot.functions.properties.Poles;

/**
 * Represents a common function.<br>
 * This type is returned by {@link FunctionUtil#getFunctionByTerm(String)} 
 * if the function is unknown.<br>
 * If accepts any type of function as long as it's valid and can be
 * evaluated with {@link MathEval}.
 */
public class CommonFunction extends Function implements Poles, Approximate {
  
  /** Format of the CommonFunction.*/
  private static final String FORMAT_COMMON_FUNCTION = "[0-9a-zA-z\\.\\+\\-\\*/\\(\\)]+";
  
  /** Function as String.*/
  private String function;
  
  /** The constants of the function. There are extracted once, to save resources. */ 
  private HashMap<String, Double> constants;
  /** The divisions in the function (used for pole-finding). There are extracted once, to save resources. */
  private ArrayList<String>       divisions;
  
  
  public CommonFunction(String function) {
    super(FORMAT_COMMON_FUNCTION);
    
    if (function != null && isValid(function))
      this.function = function;
    else
      throw new IllegalArgumentException("Function term invalid or null: " + function);
    
    extractConstants();
    extractDivisions();
  }
  
  @Override
  protected boolean isValid(String function) {
    boolean valid = false;
    try {
      MathEval math = new MathEval();
      math.setConstant("x", 0.0);
      math.evaluate(function);
      
      valid = true;
    } catch (Exception e) {}
    
    return valid && super.isValid(function);
  }

  @Override
  public List<Double> calculateRoots() {
    ArrayList<Double> roots = new ArrayList<Double>(10);
    
    final double 
        lowerBound = FunctionUtil.getLowerBound(), 
        upperBound = FunctionUtil.getUpperBound(),
        stepWidth  = FunctionUtil.getStepWidth();
    
    for (double i = lowerBound, lastI = lowerBound ; i <= upperBound; i += stepWidth) {
      
      double y = calculate(i);
      
      if (RootUtil.isZero(y))
        FunctionUtil.add(i, roots);
      else if (!(y*calculate(lastI)>0))
        FunctionUtil.add(RootUtil.calcBrent(this, lastI, i, 1000), roots);
      
      lastI = i;
    }
    return roots;
  }
  
  @Override
  public String getFunctionTerm() {
    return function;
  }

  /**
   * Returns a HashMap containing all constant values and their names.<br>
   * All constants are named <pre>"c" + constant number</pre> with 
   * <code>constant number</code> starting by <code>0</code>.
   */
  @Override
  public HashMap<String, Double> getConstants() {
    return constants;
  }

  @Override
  public List<String> getVariables() {
    ArrayList<String> variables = new ArrayList<String>(1);
    variables.add("x");
    return variables;
  }

  @Override
  public String getName() {
    return "Common funcion (not specified)";
  }

  @Override
  protected void extractConstants() {
    if (constants == null)
      constants = new HashMap<String, Double>(4);
    
    final String bf = FunctionUtil.OPERATOR_BEFORE, 
        af = FunctionUtil.OPERATOR_AFTER, co = FunctionUtil.CONSTANT; 
    
    Matcher f = Pattern.compile(
        "(^\\(?"+co+""+af+")|"+  // constant at start of term
        "("+bf+""+co+""+af+")|"+ // constant between operators
        "("+bf+""+co+"\\)?$)|"+  // constant at end of term
        "(^\\(?"+co+"\\)?$)")    // term is a single constant
        .matcher(function);
    int constNum = 0, start = 0;
    
    while (f.find(start)) {
      for (int i = 2; i <= f.groupCount(); i+=2)
        if (f.group(i) != null) 
          constants.put("c" + constNum++, 
            Double.parseDouble(f.group(i)));
      if (f.end()-1!=start)
        start = f.end()-1;
      else
        break;
    }
    
  }
  
  /**
   * Extracts the divisions of this function.<br>
   * The divisions are used for pole-finding.
   * If the value is close to <code>0</code>, the
   * value is a potential pole.
   */
  private void extractDivisions() {
    if (divisions == null)
      divisions = new ArrayList<String>();
    
    Pattern p = Pattern.compile("(/(x))|"
        + "(/(\\([0-9a-zA-z\\.\\+\\-\\*/\\(\\)]+\\)))|");
    
    getDivisions(function, p, divisions);
  }

  @Override
  public List<Double> getPoles() {
    return new ArrayList<Double>(0);
  }

  @Override
  public boolean isPole(double x0, double x1) {

    if (divisions.contains("x") && 
        x1 >= 0 && x0 <= 0)
      return true; // division by 0 at origin of ordinates
    
    double stepWidth = (x1-x0)/100;
    
    MathEval math = new MathEval();
    for (double x = x0; x <= x1; x+= stepWidth)
      for (String divTerm : divisions) {
        try  {
          if (divTerm.contains("x"))
            math.setVariable("x", x);
          double sol = math.evaluate(divTerm);

          if (RootUtil.isZero(sol) || 
              sol == Double.POSITIVE_INFINITY ||
              sol == Double.NEGATIVE_INFINITY)
            return true;

        } catch (ArithmeticException | NumberFormatException e) {e.printStackTrace();}
      }
    
    
    return false;
  }
  
  /**
   * Extracts the divisions from a function term and adds it to a
   * given list.<br>
   * Recursive.
   * @param function The given function term.
   * @param p The target pattern.
   * @param list The list, the term will be added to.
   */
  private void getDivisions(String function, Pattern p, List<String> list) {
    Matcher m = p.matcher(function);

    while (m.find()) {
      for (int i = 1; i <= m.groupCount(); i+=2)
        if (m.group(i) != null) {
          if (!list.contains(m.group(i+1)))
            list.add(m.group(i+1));
          getDivisions(m.group(i+1), p, list);
        }
    }
    
  }

  @Override
  public void calculatePoles() {} // Only #isPole() possible at the moment.
}
