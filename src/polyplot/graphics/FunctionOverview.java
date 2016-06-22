package polyplot.graphics;

import polyplot.math.CompilationContext;
import polyplot.math.Function;

import java.awt.*;
import java.awt.image.ColorModel;
import java.util.*;
import java.util.List;

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
    private static String EMPTY_LABEL    = "Nothing to show.";


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

        int margin = 12;
        final Graphics2D g2d = (Graphics2D) gc;

        final CompilationContext context = parent.getFunctionContext();

        Map<String, Color> functions = new LinkedHashMap<>();
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
            constants.add(c.getFullExpression() + String.format(Locale.ROOT, " \u2248 %.2f", c.getValue()));

        Collections.sort(constants, (c1, c2) -> {
                    String s1 = c1.substring(0, c1.indexOf(' '));
                    String s2 = c2.substring(0, c2.indexOf(' '));
                    if (s1.length() == s2.length()) return s1.compareTo(s2);
                    else return s1.length() - s2.length();
                });

        // Draw Box
        Rectangle bounds = getBounds(functions, constants, margin, gc, parent);

        if (parent.setBoundOffsetLeft(bounds.width))
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
                // Nothing to show
            if (functions.size() == 0 && constants.size() == 0) {
                yPox += fonHeight;
                if (yPox > maxHeight)
                    break outer;
                // http://stackoverflow.com/questions/4679715/is-there-a-way-to-tell-if-a-html-hex-colour-is-light-or-dark
                float b = Color.RGBtoHSB(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), null)[2];
                if (b > 0.5)
                    g2d.setColor(foreground.darker().darker().darker());
                else
                    g2d.setColor(foreground.brighter().brighter().brighter());
                g2d.setFont(g2d.getFont().deriveFont(Font.ITALIC));
                g2d.drawString(EMPTY_LABEL, xPos,
                        (int)(parent.getBounds().getHeight() - parent.getBounds().getY())/2 + fonHeight/2);
                g2d.setFont(origin);
                return;
            }
               // Draw Functions
            if (functions.size() > 0) {
                yPox += fonHeight;
                if (yPox > maxHeight)
                    break outer;
                g2d.setFont(g2d.getFont().deriveFont(UNDERLINED));
                g2d.drawString(FUNCTION_LABEL, xPos, yPox);
                g2d.setFont(origin);
                List<String> keys = new ArrayList<>(functions.keySet());
                Collections.sort(keys, (c1, c2) -> {
                    String s1 = c1.substring(0, c1.indexOf('('));
                    String s2 = c2.substring(0, c2.indexOf('('));
                    if (s1.length() == s2.length()) return s1.compareTo(s2);
                    else return s1.length() - s2.length();
                });
                for (String key : keys) {
                    yPox += fonHeight;
                    if (yPox > maxHeight)
                        break outer;
                    g2d.setColor(functions.get(key));
                    g2d.drawString(key, xPos, yPox);
                }
            }
                // Draw constants
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

        if (width == 0)
            width = gc.getFontMetrics().stringWidth(EMPTY_LABEL);

        width+= matgin;

        return new Rectangle(width, parent.getBounds().height);
    }
}
