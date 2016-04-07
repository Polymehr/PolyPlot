package polyplot.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

public class Scale implements DrawableComponent {
  
  private Color color;
  
  private int horizontalBar = 0;
  private int verticalBar = 0;
  
  private int pow = 0;

  public Scale(Color color) {
      this.color = color;
  }

  @Override
  public void update(FunctionPlotter parent) {
      int zeroX = parent.getPixelToXValue(0);
      int zeroY = parent.getPixelToYValue(0);

      verticalBar = (zeroY < 0) ? 0 : (zeroY > parent.getHeight()-1) ? parent.getHeight()-1 : zeroY;
      horizontalBar   = (zeroX < 0) ? 0 : (zeroX > parent.getWidth()-1) ? parent.getWidth()-1 : zeroX;

      
      final double span = parent.getSpan()/2;
      
      if (span > 1)
        for (int i = 0; span >= Math.pow(10, i); ++i) {
          pow =  i-1;
        }
      else
        for (int i = 0; span <= Math.pow(10, i); --i) {
          pow = i-1;
        }
      
  }

  @Override
  public void draw(Graphics gc, FunctionPlotter parent) {
    gc.setColor(color);
    gc.drawLine(0, verticalBar, parent.getWidth(), verticalBar);
    gc.drawLine(horizontalBar, 0, horizontalBar, parent.getHeight());
    
    BigDecimal startX = BigDecimal.valueOf(parent.getXCorner()).setScale(-pow, RoundingMode.FLOOR);
    BigDecimal startY = BigDecimal.valueOf(parent.getYCorner()).setScale(-pow, RoundingMode.FLOOR);
    
    final BigDecimal offset  = BigDecimal.ONE.movePointRight(pow);
    { 
      final int markers = BigDecimal.valueOf(parent.spanX).divide(offset).setScale(0, RoundingMode.CEILING).intValue();
      final BigDecimal compare = BigDecimal.valueOf(parent.getXCorner() + parent.getSpanX()).add(offset);
        
      for (BigDecimal start = startX; start.compareTo(compare) == -1; start = start.add(offset)) {
        drawXMarker(start, new Point(parent.getPixelToXValue(start.doubleValue()), verticalBar), 2, parent, gc);
      }
    }
      
      
    {
      final int markers = BigDecimal.valueOf(parent.spanX).divide(offset).setScale(0, RoundingMode.CEILING).intValue();
      final BigDecimal compare = BigDecimal.valueOf(parent.getYCorner() + parent.getSpanY()).add(offset);
        
      int maxLength = 0;
      List<BigDecimal> cache = new LinkedList<>();
        
      for (BigDecimal start = startY; start.compareTo(compare) == -1; start = start.add(offset)) {
        cache.add(start);
        
        int len = gc.getFontMetrics().stringWidth((start.precision() < 4 ? start.toPlainString() : start.toEngineeringString()));
        if (len > maxLength)
          maxLength = len;
      }
        
      for (BigDecimal bd : cache)
        drawYMarker(bd, new Point(horizontalBar, parent.getPixelToYValue(bd.doubleValue())), 2, maxLength, parent, gc);
    }
    
  }
  
  private void drawXMarker(BigDecimal value, Point p, int lineLength, FunctionPlotter parent, Graphics gc) {
    if (value.stripTrailingZeros().equals(BigDecimal.ZERO))
      return;
    final String text = (value == null ? "" : (value.precision() < 4 ? value.toPlainString() : value.toEngineeringString()));
    
    final int height = value == null ? 0 :  gc.getFontMetrics().getHeight();
    final int width  = gc.getFontMetrics().stringWidth(text);
    
    gc.drawLine(p.x, p.y-lineLength, p.x, p.y+lineLength);
    
    if (p.y+ lineLength + 2 + height < parent.getHeight())
      gc.drawString(text, p.x - width/2, p.y + lineLength + height - 2);
    else
      gc.drawString(text, p.x - width/2 , p.y - lineLength - 3);
  }
  
  private void drawYMarker(BigDecimal value, Point p, int lineLength, int maxWidth, FunctionPlotter parent, Graphics gc) {
    if (value.stripTrailingZeros().equals(BigDecimal.ZERO))
      return;
    final String text = (value == null ? "" : (value.precision() < 4 ? value.toPlainString() : value.toEngineeringString()));
    
    final int height = value == null ? 0 :  gc.getFontMetrics().getHeight();
    final int width  = gc.getFontMetrics().stringWidth(text);
    
    gc.drawLine(p.x - lineLength, p.y, p.x + lineLength, p.y);
    
    if (p.x - maxWidth - lineLength - 5 > 0)
      gc.drawString(text, p.x - width - lineLength - 2, p.y - 3 + height/2);
    else
      gc.drawString(text, p.x + lineLength + 2, p.y - 3 + height/2);
    
  }

  @Override
  public void setForegroundColor(Color color) {
    this.color = color;
    
  }

  @Override
  public Color getForegroundColor() {
    return this.color;
  }

}
