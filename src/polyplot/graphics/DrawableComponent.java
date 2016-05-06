package polyplot.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Objects;


public abstract class DrawableComponent {

  protected Color foreground;
  protected Color background;

  public DrawableComponent(Color foreground, Color background) {
    this.foreground = Objects.requireNonNull(foreground);
    this.background = Objects.requireNonNull(background);
  }

  public DrawableComponent(Color foreground) {
    this(foreground, new Color(0, true));
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

}
