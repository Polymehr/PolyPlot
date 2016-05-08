package math;

/**
 * Represents a mathematical function.
 * @author 5hir0kur0
 */
public interface Function {
    /**
     * Calculate a value of the function.
     * @param args the functions arguments; there must be {@code getNumberOfArguments()} arguments
     * @return the calculated value
     */
    double of(double... args);

    /**
     * @return the number of arguments this function takes
     */
    int getNumberOfArguments();

    /**
     * @return the function's name
     */
    String getName();
}
