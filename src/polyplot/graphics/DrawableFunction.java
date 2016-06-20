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
 * @author Gordian
 */
public class DrawableFunction extends DrawableComponent {

    private final PureFunction function;
    private final Path2D.Double path;

    private int lastWidth = -1;
    private int lastHeight = -1;
    private double lastXCorner = NaN;
    private double lastYCorner = NaN;

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
        double yVal = this.function.of(parent.getValueOfXPixel(p.x));
        if (yVal != yVal || yVal == POSITIVE_INFINITY || yVal == NEGATIVE_INFINITY) return false;
        else return diff(parent.getPixelToYValue(yVal), p.y) < radius;
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
                && tmpHeight == this.lastHeight && this.pixelBuffer != null) {
            g2d.drawImage(this.pixelBuffer, null, null);
        }
        this.lastWidth = tmpWidth;
        this.lastXCorner = tmpXCorner;
        this.lastYCorner = tmpYCorner;
        this.lastHeight = tmpHeight;

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

    public void drawPath(Graphics2D g, FunctionPlotter parent) {
        this.path.reset();
        boolean lastWasNaN = true;
        for (int i = -1, width = parent.getWidth(); i < width; ++i) {
            final double y = this.function.fastOf(parent.getValueOfXPixel(i));
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
                    yPixel = parent.getPixelToYValue(y);
                }
                if (lastWasNaN) this.path.moveTo(xPixel, yPixel);
                else this.path.lineTo(xPixel, yPixel);
                lastWasNaN = false;
            } else lastWasNaN = true;
        }
        g.draw(this.path);
    }

    void move(Point source, Point destination, FunctionPlotter parent) {
        // TODO: Implement
    }

    private int diff(int i1, int i2) {
        if (i1 < i2) return i2 - i1;
        else return i1 - i2;
    }

    void drawPoints(Graphics2D g, FunctionPlotter parent) {
        for (int i = 0; i < this.lastWidth; ++i) {
            final double y = this.function.fastOf(parent.getValueOfXPixel(i));
            if (y != y) continue;
            final int yPixel = parent.getPixelToYValue(y);

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
            final double tmpY = this.function.fastOf(parent.getValueOfXPixel(-1));
            if (tmpY == tmpY) lastYPixel = parent.getPixelToYValue(tmpY);
            else lastYPixel = Integer.MAX_VALUE;
        }
        for (int i = 0; i < parent.getWidth(); ++i) {
            final double y = this.function.fastOf(parent.getValueOfXPixel(i));
            int yPixel = Integer.MAX_VALUE;
            if (y == y) {
                yPixel = parent.getPixelToYValue(y);
                // of one of the points is visible
                if ((yPixel > NEG_OFFSET && yPixel < OFFSET) || (lastYPixel > NEG_OFFSET && lastYPixel < OFFSET)) {
                    g.drawLine(i - 1, lastYPixel, i, yPixel);
                }
            }
            lastYPixel = yPixel;
        }
    }
}
