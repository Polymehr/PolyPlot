package polyplot.graphics;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Polymehr
 */
public class CheatSheet extends DrawableComponent {

    public CheatSheet(Color foreground, Color background, boolean hidden) {
        super(foreground, background, hidden);
    }

    @Override
    public void draw(Graphics gc, FunctionPlotter parent) {
        if (hidden)
            return;
        // Hardcoded help
        final String title = "Cheat Sheet:", delimiter = " - ";
        final int margin = 10;
        final String[] movement = {
            "Movement:",
                "(Shift+) ←, h", "Move left 10% of width (or 1px)",
                "(Shift+) ↑, j", "Move up 10% of height (or 1px)",
                "(Shift+) ↓, k", "Move down 10% of height (or 1px)",
                "(Shift+) →, l", "Move right 10% of width (or 1px)",
                "", "",
                "<Mouse Dragging>", "Move coordinate system",
                "(Alt+) Scroll Up/Down", "Move along x-axis in positive/negative direction (1px)",
                "Shift+(Alt)+ Scroll Up/Down", "Move along y-axis in positive/negative direction (1px)",
                "", "",
                "0", "Center the y-axis in the middle of the window",
                "g", "Center the x-axis in the middle of the window",
                "o", "Center the ordinate origin in the middle of the window",
        };
        final String[] zoom = {
            "Zoom:",
                "Ctrl+(Shift)+ plus", "Zoom in (10x faster)",
                "Ctrl+(Shift)+ minus", "Zoom out (10x faster)",
                "Ctrl+0", "Reset Zoom",
                "", "",
                "Ctrl+(Shift)+ Scroll Up", "Zoom in (10x faster)",
                "Ctrl+(Shift)+ Scroll Down", "Zoom out (10x faster)",
        };
        final String[] control = {
            "Control:",
                "(Shift+) a", "Add a function (and keep prompt open)",
                "ESC", "Close input field",
        };
        final String[] view = {
            "View:",
                "F1", "Toggle the Cheat Sheet",
                "i", "Toggle the info box",
        };

        final String[][] data = {movement, zoom, control, view};

        // Used fonts
        Font origin = gc.getFont();
        Font titles = origin.deriveFont(DrawableComponent.UNDERLINED);
        Font hl = new Font(Font.MONOSPACED, Font.PLAIN, origin.getSize());

        // Define bounds
        int height = 0, maxWidthKeys = 0, maxWidthDescription = 0;
        final int fontHeight = gc.getFontMetrics().getHeight();

        for (String[] subset : data) {
            height += fontHeight * (subset.length / 2 + 1);
            for (int i = 1; i < subset.length; ++i) {
                if (i % 2 == 1) {
                    gc.setFont(hl);
                    final int width = gc.getFontMetrics().stringWidth(subset[i]);
                    if (width > maxWidthKeys)
                        maxWidthKeys = width;
                } else {
                    gc.setFont(origin);
                    final int width = gc.getFontMetrics().stringWidth(subset[i]);
                    if (width > maxWidthDescription)
                        maxWidthDescription = width;
                }
            }
        }

        gc.setFont(origin);
        int width = maxWidthKeys + gc.getFontMetrics().stringWidth(delimiter) + maxWidthDescription + margin;
        height += fontHeight * (data.length) + margin;

        int xBound = parent.getWidth() / 2 - width / 2, yBound = parent.getHeight() / 2 - height / 2;


        // Drawing
        gc.setColor(super.background);
        gc.fillRect(xBound, yBound, width, height);
        gc.setColor(super.foreground);
        gc.drawRect(xBound, yBound, width, height);

        int yPos = margin / 2 + fontHeight + yBound;
        // Mind margin and that 'drawString()'s coordinates are in the lower left corner (margin+width)
        final int xPos = margin / 2 + xBound;
        gc.setFont(origin.deriveFont(Font.BOLD).deriveFont(UNDERLINED));

        gc.drawString(title, xBound + width / 2 - gc.getFontMetrics().stringWidth(title) / 2, yPos);

        for (String[] subset : data) {
            gc.setFont(titles);
            gc.drawString(subset[0], xPos, yPos += fontHeight);
            for (int i = 1; i < subset.length; ++i)
                if (i % 2 == 1) {
                    gc.setFont(hl);
                    gc.drawString(subset[i], xPos, yPos += fontHeight);
                } else {
                    gc.setFont(origin);
                    if (!subset[i].isEmpty())
                        gc.drawString(delimiter + subset[i], xPos + maxWidthKeys, yPos);
                }
            yPos += fontHeight;
        }
    }
}
