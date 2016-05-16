package polyplot.graphics;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Objects;

import math.PureFunction;

public class DrawableFunction extends DrawableComponent {

    private double xOffset;
    private double yOffset;

    private final PureFunction function;
    private final Path2D.Double path;

    private int lastWidth = -1;
    private double lastXCorner = Double.NaN;
    private double lastYCorner = Double.NaN;

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

    @Override
    public void draw(Graphics g, FunctionPlotter parent) {
        g.setColor(this.foreground);
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
            if (!Double.isNaN(y))
                this.path.lineTo(i, parent.getPixelToYValue(y));
        }
        ((Graphics2D) g).draw(this.path);
    }
}
