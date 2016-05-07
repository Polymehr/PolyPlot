package polyplot.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public abstract class DrawableComponent {

  protected Color foreground;
  protected Color background;
  protected boolean hidden;

  protected static final Map<TextAttribute, Object> UNDERLINED;
  static {
    UNDERLINED = new HashMap<>();
    UNDERLINED.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

  }

  public DrawableComponent(Color foreground, Color background, boolean hidden) {
    this.foreground = Objects.requireNonNull(foreground);
    this.background = Objects.requireNonNull(background);
    this.hidden = hidden;
  }

  public DrawableComponent(Color foreground, boolean hidden) {
    this(foreground, new Color(0, true), hidden);
  }

  public DrawableComponent(Color foreground, Color background) {
    this(foreground, background, false);
  }

  public DrawableComponent(Color foreground) {
    this(foreground, new Color(0, true), false);
  }

  public abstract void draw(Graphics gc, FunctionPlotter parent);
  
  public void setForegroundColor(Color c) {
    this.foreground = Objects.requireNonNull(c);
  }
  
  public Color getForegroundColor() {
    return foreground;
  }
  
  public void setBackgroundColor(Color c) {
    this.background = Objects.requireNonNull(c);
  }
  
  public Color getBackgroundColor() {
    return background;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void toggleHidden() {
    hidden = !hidden;
  }
}
