package polyplot.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Represents a component that can be drawn by the {@link FunctionPlotter}.
 * It has a fore and background color and can be hidden.
 *
 * @author Jannik
 */
public abstract class DrawableComponent {

    protected Color foreground;
    protected Color background;
    protected boolean hidden;

    protected static final Map<TextAttribute, Object> UNDERLINED;

    static {
        UNDERLINED = new HashMap<>();
        UNDERLINED.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

    }

    protected DrawableComponent(Color foreground, Color background, boolean hidden) {
        this.foreground = Objects.requireNonNull(foreground);
        this.background = Objects.requireNonNull(background);
        this.hidden = hidden;
    }

    protected DrawableComponent(Color foreground, boolean hidden) {
        this(foreground, new Color(0, true), hidden);
    }

    protected DrawableComponent(Color foreground, Color background) {
        this(foreground, background, false);
    }

    protected DrawableComponent(Color foreground) {
        this(foreground, new Color(0, true), false);
    }

    /**
     * Draws the component on the given {@link Graphics}.
     * If the component is hidden, this method should not draw anything.
     *
     * @param gc
     *      the <code>Graphics</code> of the <code>FunctionPlotter</code>.
     * @param parent
     *      the <code>FunctionPlotter</code> this component is drawn on.
     */
    public abstract void draw(Graphics gc, FunctionPlotter parent);

    void setForegroundColor(Color c) {
        this.foreground = Objects.requireNonNull(c);
    }

    Color getForegroundColor() {
        return foreground;
    }

    void setBackgroundColor(Color c) {
        this.background = Objects.requireNonNull(c);
    }

    Color getBackgroundColor() {
        return background;
    }

    void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    boolean isHidden() {
        return hidden;
    }

    void toggleHidden() {
        hidden = !hidden;
    }
}
