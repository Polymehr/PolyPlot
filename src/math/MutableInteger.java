package math;

/**
 * @author Gordian
 */
public class MutableInteger {
    private int i = 0;

    public MutableInteger set(int i) {
        this.i = i;
        return this;
    }

    public int get() {
        return this.i;
    }

    @Override
    public String toString() {
        return Integer.toString(this.i);
    }
}
