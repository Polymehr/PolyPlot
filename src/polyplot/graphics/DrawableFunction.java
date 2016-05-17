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

    public boolean intersectsWith(final Point p, int radius, FunctionPlotter parent) {
        return this.path.intersects(p.x, p.y, radius, radius); // TODO maybe improve this (?)
    }

    public double valueAt(final double x) {
        return function.fastOf(x);
    }

    public void oldDraw(Graphics g, FunctionPlotter parent) {
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
        this.path.moveTo(-1, -1);
        for (int i = -1, width = parent.getWidth(); i < width; ++i) {
            final double y = this.function.fastOf(parent.getValueOfXPixel(i));
            if (y == y) // when y = NaN this is false
                this.path.lineTo(i, parent.getPixelToYValue(y));
        }
        ((Graphics2D) g).draw(this.path);
    }

    private double diff(double d1, double d2) {
        if (d1 < d2) return d2 - d1;
        else return d1 - d2;
    }

    @Override
    public void draw(Graphics g, FunctionPlotter parent) {
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
