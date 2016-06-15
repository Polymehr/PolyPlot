package polyplot.graphics;

import polyplot.math.CompilationContext;
import polyplot.math.Function;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A component that represents a side view that shows all
 * functions and constants defined in the given
 * {@link FunctionPlotter}.
 *
 * @author Jannik
 */
public class FunctionOverview extends DrawableComponent {

    private boolean showOnlyUserDefined;

    private boolean showHidden;

    private static String FUNCTION_LABEL = "Functions:";
    private static String CONSTANT_LABEL = "Constants:";


    FunctionOverview(Color foreground, Color background, boolean hidden,
                            boolean showOnlyUserDefined, boolean showHidden) {
        super(foreground, background, hidden);
        this.showOnlyUserDefined = showOnlyUserDefined;
        this.showHidden = showHidden;
    }

    @Override
    public void draw(Graphics gc, FunctionPlotter parent) {
        if (super.hidden) {
            parent.setBoundOffsetLeft(0);
            return;
        }

        int margin = 6;
        final Graphics2D g2d = (Graphics2D) gc;

        final CompilationContext context = parent.getFunctionContext();

        Map<String, Color> functions = new HashMap<>();
        List<String> constants = new LinkedList<>();
        List<DrawableFunction> drawableFunctions = parent.getFunctions();

        // Add Functions and Constants
        outer:
        for (Function f : context.getFunctions(showOnlyUserDefined)) {
            for (DrawableFunction df : drawableFunctions)
                if (df.getFunction().equals(f)) {
                    if (!df.isHidden() || showHidden)
                        functions.put(f.getFullExpression(), df.getForegroundColor());
                    continue outer;
                }
            functions.put(f.getFullExpression(), super.foreground);
        }

        for (CompilationContext.Constant c : context.getConstants(showOnlyUserDefined))
            constants.add(c.getFullExpression());

        // Draw Box
        Rectangle bounds = getBounds(functions, constants, margin, gc, parent);

        if (bounds.width == 0) {
            parent.setBoundOffsetLeft(0);
            return;
        } else if (parent.setBoundOffsetLeft(bounds.width))
            return;

        g2d.setColor(background);
        g2d.fill(bounds);

        g2d.setColor(foreground);
        g2d.drawLine(bounds.x+bounds.width, bounds.y, bounds.x+bounds.width, bounds.y+bounds.height);

        Font origin = g2d.getFont();
        outer: {
            // Draw Info
            final int xPos = margin / 2, fonHeight = gc.getFontMetrics().getHeight(),
                    maxHeight = parent.getBounds().height;
            int yPox = margin / 2;

            if (functions.size() > 0) {
                yPox += fonHeight;
                if (yPox > maxHeight)
                    break outer;
                g2d.setFont(g2d.getFont().deriveFont(UNDERLINED));
                g2d.drawString(FUNCTION_LABEL, xPos, yPox);
                g2d.setFont(origin);
                for (Map.Entry e : functions.entrySet()) {
                    yPox += fonHeight;
                    if (yPox > maxHeight)
                        break outer;
                    g2d.setColor((Color) e.getValue());
                    g2d.drawString((String) e.getKey(), xPos, yPox);
                }
            }

            if (constants.size() > 0) {
                // A Little Gap
                if (functions.size() > 0) {
                    yPox += fonHeight;
                    if (yPox > maxHeight)
                        break outer;
                }

                yPox += fonHeight;
                if (yPox > maxHeight)
                    break outer;
                g2d.setColor(foreground);
                g2d.setFont(g2d.getFont().deriveFont(UNDERLINED));
                g2d.drawString(CONSTANT_LABEL, xPos, yPox);
                g2d.setFont(origin);
                for (String s : constants) {
                    yPox += fonHeight;
                    if (yPox > maxHeight)
                        break outer;
                    g2d.drawString(s, xPos, yPox);
                }
            }
        }
    }

    private Rectangle getBounds(Map<String, Color> functions, List<String> constants, int matgin,
                                Graphics gc, FunctionPlotter parent) {

        int width = 0;

        for (String s : functions.keySet()) {
            int tmpWidth = gc.getFontMetrics().stringWidth(s);
            if (width <= tmpWidth)
                width = tmpWidth;
        }
        for (String s : constants) {
            int tmpWidth = gc.getFontMetrics().stringWidth(s);
            if (width <= tmpWidth)
                width = tmpWidth;
        }

        if (!functions.isEmpty()) {
            int tmpWidth = gc.getFontMetrics().stringWidth(FUNCTION_LABEL);
            if (width <= tmpWidth)
                width = tmpWidth;
        }

        if (!constants.isEmpty()) {
            int tmpWidth = gc.getFontMetrics().stringWidth(CONSTANT_LABEL);
            if (width <= tmpWidth)
                width = tmpWidth;
        }

        if (width > 0)
            width+= matgin;

        return new Rectangle(width, parent.getBounds().height);
    }
}
