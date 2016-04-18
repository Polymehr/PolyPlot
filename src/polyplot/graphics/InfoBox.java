package polyplot.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

public class InfoBox implements DrawableComponent {
  
  private Color foreground;
  private Color background;
  
  private boolean docked;
  private boolean showPixel;
  private int functionRadius;
  
  public InfoBox(final Color foreground, final Color background, 
                        boolean docked, boolean showPixel, int functionRadius) {
    this.foreground = foreground;
    this.background = background;
    this.docked = docked;
    this.showPixel = showPixel;
    this.functionRadius = functionRadius;
  }


  @Override
  public void draw(Graphics gc, FunctionPlotter parent) {
    
    Point mouse = parent.getMousePos();
    if (mouse == null || mouse.x < 0 || mouse.y < 0)
      return;
    
    BigDecimal xV = BigDecimal.valueOf(parent.getValueOfXPixel(mouse.x)).setScale(-parent.getPower()+3, RoundingMode.HALF_UP);
    BigDecimal yV = BigDecimal.valueOf(parent.getValueOfYPixel(mouse.y)).setScale(-parent.getPower()+3, RoundingMode.HALF_UP);
    
    String xString = "X: " + xV.toEngineeringString()+ (showPixel ? " (" + mouse.x + ")" : "");
    String yString = "Y: " + yV.toEngineeringString() + (showPixel ? " (" + mouse.y + ")" : "");
    
    
    int height = gc.getFontMetrics().getHeight();
    int width  = 0;
    
    
    
    List<String> components = new LinkedList<>();
    
    components.add(xString);
    components.add(yString);
    
    for (String s : components) {
      int wid = gc.getFontMetrics().stringWidth(s);
      if (wid > width)
        width = wid;
    }
    width += 4;
    
    Rectangle bounds = new Rectangle(width, height*components.size()+4);
    
    Point pos = calculatePos(bounds, parent);
    
    gc.setColor(background);
    gc.fillRect(pos.x, pos.y, bounds.width, bounds.height);
    
    gc.setColor(foreground);
    gc.drawRect(pos.x, pos.y, bounds.width, bounds.height);
    
    gc.drawString(xString, pos.x+3, pos.y-1+height);
    gc.drawString(yString, pos.x+3, pos.y-1+height*2);
    
  }
  
  private Point calculatePos(Rectangle bounds ,FunctionPlotter parent) {
    int xPos = 0, yPos = 0; 
    
    if (docked) {
      final int xBar = parent.getPixelToXValue(0);
      final int yBar = parent.getPixelToYValue(0);
      
      xPos = (xBar > parent.getWidth()-11-bounds.width) ? 5 : parent.getWidth()-6-bounds.width;
      yPos = (yBar > parent.getHeight()-11-bounds.height) ? 5 : parent.getHeight()-6-bounds.height;
    } else {
      Point mouse = parent.getMousePos();
      
      xPos = (mouse.x > parent.getWidth()-15-6-bounds.width) ? mouse.x-8-bounds.width : mouse.x+15;
      yPos = (mouse.y > parent.getHeight()-6-bounds.height) ? parent.getHeight()-6-bounds.height : mouse.y-2;
      
    }
    
    return new Point(xPos, yPos);
  }

  @Override
  public void setForegroundColor(Color c) {
    this.foreground = c;
    
  }

  @Override
  public Color getForegroundColor() {
    return foreground;
  }
  
  @Override
  public void setBackgroundColor(Color c) {
    this.background = c;
  }
  
  @Override
  public Color getBackgroundColor() {
    return background;
  }

}
