package polyplot.graphics;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Objects;
import static java.lang.Double.NaN;

import math.PureFunction;

public class DrawableFunction extends DrawableComponent {

    private final PureFunction function;
    private final Path2D.Double path;

    private int lastWidth = -1;
    private double lastXCorner = NaN;
    private double lastYCorner = NaN;

    private final static BasicStroke STROKE = new BasicStroke(2);

    public DrawableFunction(Color color, PureFunction function) {
        super(color);
        this.function = Objects.requireNonNull(function, "function for DrawableFunction must not be null");
        this.path = new Path2D.Double();
    }

    public boolean intersectsWith(Point p, int radius, FunctionPlotter parent) {
        return diff(parent.getPixelToYValue(this.function.of(p.x)), p.y) < radius;
    }

    public double valueAt(final double x) {
        return function.fastOf(x);
    }

    @Override
    public void draw(Graphics g, FunctionPlotter parent) {
        if (this.hidden) return;
        g.setColor(this.foreground);
        ((Graphics2D)g).setStroke(STROKE);
        final int tmpWidth = parent.getWidth();
        final double tmpXCorner = parent.getXCorner();
        final double tmpYCorner = parent.getYCorner();
        if (tmpWidth == this.lastWidth && tmpXCorner == lastXCorner && tmpYCorner == lastYCorner) {
            ((Graphics2D) g).draw(this.path);
            return;
        }
        this.lastWidth = tmpWidth;
        this.lastXCorner = tmpXCorner;
        this.lastYCorner = tmpYCorner;

        this.path.reset();
        this.path.moveTo(0, 0);
        boolean lastWasNaN = false;
        for (int i = -1, width = parent.getWidth(); i < width; ++i) {
            final double y = this.function.fastOf(parent.getValueOfXPixel(i));
            if (y == y) { // when y = NaN this is false
                if (lastWasNaN) this.path.moveTo(i, parent.getPixelToYValue(y));
                else this.path.lineTo(i, parent.getPixelToYValue(y));
                lastWasNaN = false;
            } else lastWasNaN = true;
        }
        ((Graphics2D) g).draw(this.path);
    }

    private int diff(int i1, int i2) {
        if (i1 < i2) return i2 - i1;
        else return i1 - i2;
    }

    public void drawLines(Graphics g, FunctionPlotter parent) {
        if (this.hidden) return;
        g.setColor(this.foreground);
        ((Graphics2D)g).setStroke(STROKE);
        double y, lastY = this.function.fastOf(parent.getValueOfXPixel(-1));
        int lastXPixel = -1;
        for (int i = 0; i < parent.getWidth(); ++i) {
            y = this.function.fastOf(parent.getValueOfXPixel(i));

            if (y == y && lastY == lastY)
                g.drawLine(lastXPixel, parent.getPixelToYValue(lastY),
                        i, parent.getPixelToYValue(y));

            if (lastXPixel != i) lastY = y;
            lastXPixel = i;
        }
    }
}
