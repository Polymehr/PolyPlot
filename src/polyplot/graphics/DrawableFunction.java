package polyplot.graphics;

import polyplot.math.PureFunction;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static java.lang.Double.NaN;

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

    static boolean DRAW_POINTS = false;

    private BufferedImage pixelBuffer;

    DrawableFunction(Color color, PureFunction function) {
        super(color);
        this.function = Objects.requireNonNull(function, "function for DrawableFunction must not be null");
        this.path = new Path2D.Double();
    }

    boolean intersectsWith(Point p, int radius, FunctionPlotter parent) {
        return diff(parent.getPixelToYValue(this.function.of(p.x)), p.y) < radius;
    }

    public PureFunction getFunction() {
        return function;
    }

    @Override
    public void draw(Graphics g, FunctionPlotter parent) {
        if (this.hidden) return;
        g.setColor(this.foreground);
        //((Graphics2D)g).setStroke(STROKE);
        final int tmpWidth = parent.getWidth();
        final int tmpHeight = parent.getHeight();
        final double tmpXCorner = parent.getXCorner();
        final double tmpYCorner = parent.getYCorner();
        if (tmpWidth == this.lastWidth && tmpXCorner == this.lastXCorner && tmpYCorner == this.lastYCorner
                && tmpHeight == this.lastHeight) {
            if (DRAW_POINTS) {
                this.drawPoints(g, parent, true);
                return;
            }
            else if (this.pixelBuffer == null) {
                ((Graphics2D) g).draw(this.path);
                return;
            }
        }
        this.lastWidth = tmpWidth;
        this.lastXCorner = tmpXCorner;
        this.lastYCorner = tmpYCorner;
        this.lastHeight = tmpHeight;

        if (DRAW_POINTS) {
            this.drawPoints(g, parent, false);
            return;
        }

        this.pixelBuffer = null;

        this.path.reset();
        boolean lastWasNaN = true;
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

    void drawPoints(Graphics g, FunctionPlotter parent, boolean drawBuffer) {
        final Graphics2D g2d = (Graphics2D)g;

        if (drawBuffer && this.pixelBuffer != null) {
            g2d.drawImage(this.pixelBuffer, null, null);
            return;
        }

        this.pixelBuffer = new BufferedImage(this.lastWidth, this.lastHeight, BufferedImage.TYPE_INT_ARGB);

        final int rgb = this.foreground.getRGB();
        final Graphics2D tmpGraphics = this.pixelBuffer.createGraphics();
        tmpGraphics.setColor(this.foreground);

        for (int i = 0; i < this.lastWidth; ++i) {
            final double y = this.function.fastOf(parent.getValueOfXPixel(i));
            final int yPixel = parent.getPixelToYValue(y);

            if (y == y && yPixel >= 0 && yPixel < this.lastHeight) {
                //this.pixelBuffer.setRGB(i, yPixel, rgb);
                tmpGraphics.drawRect(i, yPixel, 1, 1);
            }
        }

        g2d.drawImage(this.pixelBuffer, null, null);
    }

    void drawLines(Graphics g, FunctionPlotter parent) {
        if (this.hidden) return;
        g.setColor(this.foreground);
        //((Graphics2D)g).setStroke(STROKE);
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
