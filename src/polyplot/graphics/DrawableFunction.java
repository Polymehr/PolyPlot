package polyplot.graphics;

import polyplot.math.PureFunction;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static java.lang.Double.*;

/**
 * A component that represents a function that can be drawn
 * on the {@link FunctionPlotter}.<br>
 * The function can be drawn using 3 different render styles:
 * <ul>
 *     <li>{@link Path2D}</li>
 *     <li>points</li>
 *     <li>lines</li>
 * </ul>
 *
 * @author 5hir0kur0
 */
public class DrawableFunction extends DrawableComponent {

    private final PureFunction function;
    private final Path2D.Double path;

    private int lastWidth = -1;
    private int lastHeight = -1;
    private double lastXCorner = NaN;
    private double lastYCorner = NaN;
    private double xOffset = 0.0;
    private double yOffset = 0.0;
    private boolean grabbed = false;

    public enum DrawingMethod { PATH, POINTS, LINES }

    static DrawingMethod DRAWING_METHOD = DrawingMethod.PATH;

    private BufferedImage pixelBuffer;
    private final static Color TRANSPARENT = new Color(0, true);

    DrawableFunction(Color color, PureFunction function) {
        super(color);
        this.function = Objects.requireNonNull(function, "function for DrawableFunction must not be null");
        this.path = new Path2D.Double();
    }

    boolean intersectsWith(Point p, int radius, FunctionPlotter parent) {
        double yVal = this.function.of(parent.getValueOfXPixel(p.x) + xOffset);
        if (yVal != yVal || yVal == POSITIVE_INFINITY || yVal == NEGATIVE_INFINITY) return false;
        else return diff(parent.getPixelToYValue(yVal - yOffset), p.y) < radius;
    }

    public PureFunction getFunction() {
        return function;
    }

    public static void toggleDrawingMethod() {
        DRAWING_METHOD = DrawingMethod.values()[(DRAWING_METHOD.ordinal() + 1) % DrawingMethod.values().length];
    }

    @Override
    public void draw(Graphics g, FunctionPlotter parent) {
        if (this.hidden) return;
        final Graphics2D g2d = (Graphics2D) g;
        final int tmpWidth = parent.getWidth();
        final int tmpHeight = parent.getHeight();
        final double tmpXCorner = parent.getXCorner();
        final double tmpYCorner = parent.getYCorner();
        if (tmpWidth == this.lastWidth && tmpXCorner == this.lastXCorner && tmpYCorner == this.lastYCorner
                && tmpHeight == this.lastHeight && !this.grabbed && this.pixelBuffer != null) {
            g2d.drawImage(this.pixelBuffer, null, null);
        }
        this.lastWidth = tmpWidth;
        this.lastXCorner = tmpXCorner;
        this.lastYCorner = tmpYCorner;
        this.lastHeight = tmpHeight;
        this.grabbed = false;

        if (this.pixelBuffer != null && this.pixelBuffer.getWidth() == this.lastWidth
                && this.pixelBuffer.getHeight() == this.lastHeight) {
            final Graphics2D tmp = this.pixelBuffer.createGraphics();
            tmp.setBackground(TRANSPARENT);
            tmp.clearRect(0, 0, this.lastWidth, this.lastHeight);
        } else {
            this.pixelBuffer = new BufferedImage(this.lastWidth, this.lastHeight, BufferedImage.TYPE_INT_ARGB);
        }

        try {
            final Graphics2D tmpG2d = pixelBuffer.createGraphics();
            tmpG2d.setColor(this.foreground);
            tmpG2d.setRenderingHints(FunctionPlotter.RENDERING_HINTS);
            switch (DRAWING_METHOD) {
                case PATH: this.drawPath(tmpG2d, parent); break;
                case POINTS: this.drawPoints(tmpG2d, parent); break;
                case LINES: this.drawLines(tmpG2d, parent); break;
            }
        } catch (RuntimeException e) {
            g.drawString(e.toString(), 42, 42);
            System.err.println(e.toString());
        }
        g2d.drawImage(this.pixelBuffer, null, null);
    }


    void move(Point source, Point destination, FunctionPlotter parent) {
        final double xOffset = parent.getValueOfXPixel(destination.x) - parent.getValueOfXPixel(source.x);
        final double yOffset = parent.getValueOfYPixel(destination.y) - parent.getValueOfYPixel(source.y);
        try {
            this.xOffset -= xOffset; // minus magic
            this.yOffset += yOffset;
        } catch (IllegalArgumentException ignored) {
            System.err.println(ignored.toString());
        }
        this.grabbed = true;
    }

    boolean isMoved() {
        return this.xOffset != 0.0 || this.yOffset != 0.0;
    }

    void setOffset(double xOffset, double yOffset) {
        if (Double.isInfinite(xOffset) || Double.isInfinite(yOffset) || Double.isNaN(xOffset) || Double.isNaN(yOffset))
            throw new IllegalArgumentException("illegal offset: " + xOffset + ", " + yOffset);
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.grabbed = true;
    }

    double getXOffset() {
        return this.xOffset;
    }

    double getYOffset() {
        return this.yOffset;
    }

    private int diff(int i1, int i2) {
        if (i1 < i2) return i2 - i1;
        else return i1 - i2;
    }

    private double calculateXValue(int pixel, FunctionPlotter parent) {
        return parent.getValueOfXPixel(pixel) + this.xOffset;
    }

    private int calculateYPixel(double yValue, FunctionPlotter parent) {
        return parent.getPixelToYValue(yValue + this.yOffset);
    }

    public void drawPath(Graphics2D g, FunctionPlotter parent) {
        this.path.reset();
        boolean lastWasNaN = true;
        for (int i = -1; i < this.lastWidth; ++i) {
            final double y = this.function.fastOf(this.calculateXValue(i, parent));
            if (y == y) { // when y = NaN this is false
                final int yPixel, xPixel;
                if (y == Double.POSITIVE_INFINITY) {
                    xPixel = i - 1;
                    yPixel = -1;
                } else if (y == Double.NEGATIVE_INFINITY) {
                    xPixel = i - 1;
                    yPixel = this.lastHeight;
                } else {
                    xPixel = i;
                    yPixel = this.calculateYPixel(y, parent);
                }
                if (lastWasNaN) this.path.moveTo(xPixel, yPixel);
                else this.path.lineTo(xPixel, yPixel);
                lastWasNaN = false;
            } else lastWasNaN = true;
        }
        g.draw(this.path);
    }

    void drawPoints(Graphics2D g, FunctionPlotter parent) {
        for (int i = 0; i < this.lastWidth; ++i) {
            final double y = this.function.fastOf(this.calculateXValue(i, parent));
            if (y != y) continue;
            final int yPixel = this.calculateYPixel(y, parent);

            if (yPixel >= 0 && yPixel < this.lastHeight) {
                //this.pixelBuffer.setRGB(i, yPixel, rgb);
                g.drawRect(i, yPixel, 1, 1);
            }
        }
    }

    void drawLines(Graphics2D g, FunctionPlotter parent) {
        if (this.hidden) return;
        g.setColor(this.foreground);
        final int OFFSET = this.lastHeight + 42_000;
        final int NEG_OFFSET = -42_000;
        //((Graphics2D)g).setStroke(STROKE);
        int lastYPixel;
        {
            final double tmpY = this.function.fastOf(this.calculateXValue(-1, parent));
            if (tmpY == tmpY) lastYPixel = this.calculateYPixel(tmpY, parent);
            else lastYPixel = Integer.MAX_VALUE;
        }
        for (int i = 0; i < parent.getWidth(); ++i) {
            final double y = this.function.fastOf(this.calculateXValue(i, parent));
            int yPixel = Integer.MAX_VALUE;
            if (y == y) {
                yPixel = this.calculateYPixel(y, parent);
                // of one of the points is visible
                if ((yPixel > NEG_OFFSET && yPixel < OFFSET) || (lastYPixel > NEG_OFFSET && lastYPixel < OFFSET)) {
                    g.drawLine(i - 1, lastYPixel, i, yPixel);
                }
            }
            lastYPixel = yPixel;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DrawableFunction)) return false;
        final DrawableFunction df = (DrawableFunction) other;
        return df.function.getName().equalsIgnoreCase(this.function.getName());
    }
}
