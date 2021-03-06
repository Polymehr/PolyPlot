package polyplot.math;

/**
 * Stores an integer that can be changed.
 * @author 5hir0kur0
 */
final class MutableInteger {
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
