package me.polymehr.polyPlot.functions;

import java.util.List;

import me.polymehr.polyPlot.functions.FunctionUtil;

/**
 * Provides to find roots in functions.
 */
class RootUtil {
  
  /**
   * Tries to find roots in a specified span.<br>
   * Is not very efficient.
   * 
   * @param f The target function.
   * @param roots New roots will be added to this list.
   * @param lowerBound The lower bound of the search span.
   * @param upperBound The upper bound of the search span.
   * @param step 
   *    The step width. Should be very small to be efficient.
   *    
   * @author <a href="http://rosettacode.org/wiki/Roots_of_a_function#Java">RosettaCode</a>
   */
  static void calcMultibleRootsRC(Function f, List<Double> roots, double lowerBound,
        double upperBound, double step) {

    double x = lowerBound, ox = x;
    double y = f.calculate(x), oy = y;
    int s = sign(y), os = s;

    for (; x <= upperBound ; x += step) {
      s = sign(y = f.calculate(x));
      
      if (s == 0 || isZero(y)) {
        FunctionUtil.add(x, roots);
      } else if (s != os) {
        double dx = x - ox;
        double dy = y - oy;
        double cx = x - dx * (y / dy);

        FunctionUtil.add(cx, roots);
      }
      ox = x; oy = y; os = s;
    }
  }
  
  /**
   * Recreates Brant's method to get a root within a scope.<br>
   * The y- values to a and b must have different signs.<br>
   * <br>
   * <b>Note:</b> This method is a recreation is a recreation of
   * a algorithm for Matlab, so I don't know if it works 100% of
   * the time.
   * @param f The target function.
   * @param a A point of the scope.
   * @param b Another point of the scope.
   * @param maxCalcs the maximum number of calculations.
   * @return a approximate root between a and b.
   */
  static double calcBrent(Function f, double a, double b, double maxCalcs) {
    
    double calcCounter = 0;
    
    double fa = f.calculate(a), fb = f.calculate(b);
    
    if (fa*fb>0)
      throw new IllegalArgumentException("a and b must have different signs.");
    
    double c = a, fc = fa;
    double d = b-a, e = d;
    
    while (calcCounter <= maxCalcs) {
      calcCounter++;
      
      if (fb*fc>0) {
        c=a; fc=fa; d=b-a; e=d; 
      }
      if (abs(fc)<abs(fb)) {
        a=b; b=c; c=a;
        fa=fb; fb=fc; fc=fa; 
      }
      
      double tol=2*eps()*abs(b)+FunctionUtil.getAccuracy(), m=(c-b)/2;
      
      if (abs(m)>tol && abs(fb)>0) {
        if (abs(e)<tol||abs(fa)<=abs(fb)) {
          d=m; e =m;
        } else {
          double s = fb/fa, p, q, r;
          if (a==c) {
            p=2*m*s; q=1-s;
          } else {
            q=fa/fc; r=fb/fc;
            p=s*(2*m*q*(q-r)-(b-a)*(r-1));
            q=(q-1)*(r-1)*(s-1);
          }
          if (p>0)
            q=-q;
          else
            p=-p;
          s=e; e=d;
          if (2*p<3*m*q-abs(tol*q) && p<abs(s*q/2))
            d=p/q;
          else {
            d=m; e=m;
          }
        }
      
        a=b; fa=fb;
        
        if (abs(d)>tol)
          b=b+d;
        else
          if (m>0)
            b=b+tol;
          else
            b=b-tol;
      } else
        break;
      fb=f.calculate(b);
      
    }
    return b;
  }
  
  /**
   * Uses the bisection method to get a root within a scope.<br>
   * The y- values to a and b must have different signs.<br>
   * 
   * @param f The target function.
   * @param a A point of the scope.
   * @param b Another point of the scope.
   * @param maxCalcs the maximum number of calculations.
   * @return a approximate root between a and b.
   */
  static double calcBisection(Function f, double a, double b) {
    double m, y_m, y_a;
   
    if (f.calculate(a)*f.calculate(b)>0)
      throw new IllegalArgumentException("a and b must have different signs.");
   
    while ((b-a) > FunctionUtil.getAccuracy()) {
       m = (a+b)/2;
   
       y_m = f.calculate(m);
       y_a = f.calculate(a);
   
       if ((y_m*y_a<0))
          b = m;
       else
          a = m;
    }
   
    return (a+b)/2;
  }
  
  /**
   * Tries to calculate the roots using recursion.
   * @deprecated It is <b>very</b> stack intensive. It will properly crash.
   * @param f The target function.
   * @param roots New roots will be added to this list.
   * @param lowerBound The lower bound of the search span.
   * @param upperBound The upper bound of the search span.
   * @throws IllegalArgumentException
   *    if the lower bound is bigger that the upper bound. 
   */
  static void calcMultibleRootsRecursive(Function f, List<Double> roots, final double lowerBound, final double upperBound)
      throws IllegalArgumentException 
  {
    if (lowerBound > upperBound)
      throw new IllegalArgumentException("'lowerBound' cannot be bigger than 'upperBound'.");
    
    
    double span, y0, y1, x0, x1;

    span = Math.abs(upperBound - lowerBound);
    x0 = lowerBound;
    x1 = upperBound;
    
    y0 = f.calculate(lowerBound);
    y1 = f.calculate(upperBound); 
    
    
    if (Math.abs(y0) < Math.abs(y1) && isZero(y0))
      FunctionUtil.add(x0, roots);
    else if (Math.abs(y0) > Math.abs(y1) && isZero(y1))
      FunctionUtil.add(x1, roots);
    else if (Math.abs(y0) == Math.abs(y1) && isZero(y0))
      FunctionUtil.add(x0, roots);
    else if ((span < FunctionUtil.getAccuracy() && y0 + y1 > FunctionUtil.getAccuracy()*10) || span < FunctionUtil.getAccuracy() / 10 || lowerBound == upperBound)
      return;
    
    calcMultibleRootsRecursive(f, roots, lowerBound, lowerBound+(span/2));
    calcMultibleRootsRecursive(f, roots, lowerBound+(span/2), upperBound);

  }
  

  /** 
   * Returns <code>Math.pow(2, -52)</code>.
   * Used in 
   */
  private static double eps() {
    return Math.pow(2, -52);
  }

  /**
   * Short form of {@link Math#abs(double)}.
   */
  private static double abs(double value) {
    return Math.abs(value);
  }

  /**
   * Returns <code>-1</code> if <code>x</code> is negative, <code>1</code> if it's positive
   * and <code>0</code> if it's <code>0</code>.
   * @param x The double that sign should be determined.
   * 
   * @author <a href="http://rosettacode.org/wiki/Roots_of_a_function#Java">RosettaCode</a>
   */
  private static int sign(double x) {
    return (x < 0.0) ? -1 : (x > 0.0) ? 1 : 0;
  }

  /**
   * Whether the x-value can be treaded as a zero point.
   * @param xValue The x-value.
   */
  static boolean isZero(double xValue) {
    return Math.abs(xValue) <= FunctionUtil.getAccuracy();
  }
  
  /** Static class. */
  private RootUtil() {}
}
