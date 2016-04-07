package polyplot.graphics;

import java.awt.Color;
import java.awt.Graphics;


public interface DrawableComponent {
  
  void update(FunctionPlotter parent);
  
  void draw(Graphics gc, FunctionPlotter parent);
  
  void setForegroundColor(Color c);
  
  Color getForegroundColor();
  
  default void setBackgroundColor(Color c) {}
  
  default Color getBackgroundColor() { return new Color(0x00, true);
    
  }

}
