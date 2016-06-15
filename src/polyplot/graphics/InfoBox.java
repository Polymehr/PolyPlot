package polyplot.graphics;

import polyplot.math.PureFunction;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

/**
 * A component that represents a box that shows
 * the value of the drawn functions at the current mouse
 * location and the x- and y-values at the current mouse
 * location.<br>
 * It can be displayed in the corner or at the mouse cursor.
 *
 * @author Jannik
 */
public class InfoBox extends DrawableComponent {

    private boolean docked;
    private boolean showPixel;
    private int functionRadius;

    InfoBox(final Color foreground, final Color background,
                   boolean docked, boolean showPixel, boolean hidden, int functionRadius) {
        super(foreground, background, hidden);
        this.foreground = foreground;
        this.background = background;
        this.docked = docked;
        this.showPixel = showPixel;
        this.functionRadius = functionRadius;
    }


    @Override
    public void draw(Graphics gc, FunctionPlotter parent) {

        if (super.hidden)
            return;

        Point mouse = parent.getMousePos();
        if (mouse == null || mouse.x < 0 || mouse.y < 0)
            return;

        BigDecimal xV = BigDecimal.valueOf(parent.getValueOfXPixel(mouse.x)).setScale(-parent.getPower() + 3, RoundingMode.HALF_UP);
        BigDecimal yV = BigDecimal.valueOf(parent.getValueOfYPixel(mouse.y)).setScale(-parent.getPower() + 3, RoundingMode.HALF_UP);

        String xString = "X: " + xV.toEngineeringString() + (showPixel ? " (" + mouse.x + ")" : "");
        String yString = "Y: " + yV.toEngineeringString() + (showPixel ? " (" + mouse.y + ")" : "");


        int height = gc.getFontMetrics().getHeight();
        int width = 0;


        List<String> components = new LinkedList<>();

        components.add(xString);
        components.add(yString);
        switch (functionRadius) {
            case 0:
                break;
            case -1:
                for (DrawableFunction f : parent.getFunctions())
                    components.add(getFunctionString(f.getFunction(), mouse.x, parent));
                break;
            default:
                for (DrawableFunction f : parent.getFunctions())
                    if (f.intersectsWith(mouse, functionRadius, parent))
                        components.add(getFunctionString(f.getFunction(), mouse.x, parent));
        }


        for (String s : components) {
            int wid = gc.getFontMetrics().stringWidth(s);
            if (wid > width)
                width = wid;
        }
        width += 4;

        Rectangle bounds = new Rectangle(width, height * components.size() + 4);

        Point pos = calculatePos(bounds, parent);

        gc.setColor(super.background);
        gc.fillRect(pos.x, pos.y, bounds.width, bounds.height);

        gc.setColor(super.foreground);
        gc.drawRect(pos.x, pos.y, bounds.width, bounds.height);

        gc.drawString(xString, pos.x + 3, pos.y - 1 + height);
        gc.drawString(yString, pos.x + 3, pos.y - 1 + height * 2);


        switch (functionRadius) {
            case 0:
                break;
            case -1: {
                int yPos = pos.y - 1 + height * 2;
                for (DrawableFunction f : parent.getFunctions()) {
                    gc.setColor(f.getForegroundColor());
                    gc.drawString(getFunctionString(f.getFunction(), mouse.x, parent), pos.x + 3, yPos += height);
                }
            }
            break;

            default:
                int yPos = pos.y - 1 + height * 2;
                for (DrawableFunction f : parent.getFunctions())
                    if (f.intersectsWith(mouse, functionRadius, parent)) {
                        gc.setColor(f.getForegroundColor());
                        gc.drawString(getFunctionString(f.getFunction(), mouse.x, parent), pos.x + 3, yPos += height);
                    }
        }

    }

    private Point calculatePos(Rectangle bounds, FunctionPlotter parent) {
        int xPos, yPos;
        final Rectangle parentBounds = parent.getBounds();

        if (docked) {
            final int xBar = parent.getPixelToXValue(0);
            final int yBar = parent.getPixelToYValue(0);

            xPos = (xBar > parentBounds.width - 11 - bounds.width) ? parentBounds.x + 5 : parentBounds.width - 6 - bounds.width;
            yPos = (yBar > parentBounds.height - 11 - bounds.height) ? parentBounds.y + 5 : parentBounds.height - 6 - bounds.height;
        } else {
            Point mouse = parent.getMousePos();

            xPos = (mouse.x > parentBounds.width - 15 - 6 - bounds.width) ? mouse.x - 8 - bounds.width : mouse.x + 15;
            yPos = (mouse.y > parentBounds.height - 6 - bounds.height) ? parentBounds.height - 6 - bounds.height : mouse.y - 2;

        }

        return new Point(xPos, yPos);
    }

    private String getFunctionString(PureFunction f, int xPos, FunctionPlotter parent) {
        double val = f.of(parent.getValueOfXPixel(xPos));
        String calc =
                // Special cases
                Double.isNaN(val) ? "Undefined" :
                        Double.POSITIVE_INFINITY == val ? "∞" : Double.NEGATIVE_INFINITY == val ? "-∞" :
                                // else use rounded value
                                BigDecimal.valueOf(val).setScale(-parent.getPower() + 3, RoundingMode.HALF_UP).toEngineeringString();
        BigDecimal xVal = BigDecimal.valueOf(parent.getValueOfXPixel(xPos))
                .setScale(-parent.getPower() + 3, RoundingMode.HALF_UP);
        return f.getName() + "(" + xVal + ") = " + calc;
    }

    @Override
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public void toggleHidden() {
        hidden = !hidden;
    }
}
