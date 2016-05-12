package math;

import java.util.Arrays;

/**
 * @author 5hir0kur0
 */
final class DoubleStack {
    double[] stack;
    int top;

    public DoubleStack(int initialSize) {
        if (initialSize <= 0) throw new IllegalArgumentException("stack size must not be <= 0");
        this.stack = new double[initialSize];
        this.top = -1;
    }

    public void push(double d) {
        if (++this.top >= this.stack.length)
            this.stack = Arrays.copyOf(this.stack, this.stack.length * 2);
        this.stack[this.top] = d;
    }

    public double pop() {
        if (this.top < 0) throw new IllegalStateException("trying to pop empty stack");
        return this.stack[this.top--];
    }

    public int size() {
        return this.top + 1;
    }
}
