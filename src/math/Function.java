package math;

/**
 * @author Gordian
 */
public interface Function {
    double of(double... args);
    int getNumberOfArguments();
    String getName();
}
