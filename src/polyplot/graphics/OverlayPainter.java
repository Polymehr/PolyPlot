package polyplot.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.HashMap;

import polyplot.PolyPlot;
import polyplot.functions.Function;

/**
 * Draws the overlay of a {@link FunctionPlotter}.<br>
 * Used to extract drawing methods.
 */
class OverlayPainter {
  
  private FunctionPlotter fp;
  
  private Point infoMouse;
  
  
  OverlayPainter(FunctionPlotter fp) {
    this.fp         = fp;

    registerOverlayEvents();
  }
  
  
  
  
  

  private void drawVerticalScale(Point p, double value, boolean drawValue, int shortenLine, boolean expodential, Graphics g) {
    int x = p.x, y = p.y;
    String number = drawValue ? formatNumber(value, fp.o.getRoundScale(), expodential) : "";

    int width = g.getFontMetrics().stringWidth(number);
    int height = (drawValue) ? g.getFontMetrics().getHeight() : 0;
    
    if (y!=0&&y!=fp.getHeight()-1) {
      g.drawLine(x-3+shortenLine, y, x+3-shortenLine, y);
      
      if (!(y-height/2-1<0 || x+height/2+1>fp.getHeight()-1)) {
        
        int xStr = (x-width-4 < 0) ? x+7 : x-width-5;
        g.drawString(number, xStr, (int)(y+(height*0.5625)/2));
      }
    }
  }

  private void drawHorizontialScale(Point p, double value, boolean drawValue, int shortenLine, boolean expodential, Graphics g) {
    int x = p.x, y = p.y;
    String number = drawValue ? formatNumber(value, fp.o.getRoundScale(), expodential) : "";
    
    int width = g.getFontMetrics().stringWidth(number);
    int height = (drawValue) ? g.getFontMetrics().getHeight() : 0;
    
    if (x!=0&&x!=fp.getWidth()-1) {
      g.drawLine(x, y-3+shortenLine, x, y+3-shortenLine);
      
      if (!(x-width/2-1<0 || x+width/2+1>fp.getWidth()-1)) {
        
        int yStr = (y+height+4 < fp.getHeight()) ? y+height : y-6;
        g.drawString(number, x-width/2, yStr);
      }
    }
  }
  
  /**
   * Draws the info box static or as tool tip.
   * @param xPx The x value of the scale.
   * @param yPx The x value of the scale.
   */
  private void drawInfoBox(int xPx, int yPx, Graphics g) {
    Color bg = fp.o.backgroundColor;
    Color fg = fp.o.scaleColor;
    HashMap<Function, Color> allowedFunctions = new HashMap<Function, Color>(fp.functions.size());
    if (infoMouse!=null && fp.o.infobox) {
      int width=0, height = 0, 
          xM = infoMouse.x, yM = infoMouse.y;
      
      String y = formatNumber(fp.getValueOfYPixel(yM), fp.o.getRoundInfoBox(), false);
      String x = formatNumber(fp.getValueOfXPixel(xM), fp.o.getRoundInfoBox(), false);
      
      String[] phrases = {
          "X: "+x+ (fp.o.pixelsInfobox ? " ("+xM+")":""),
          "Y: "+y + (fp.o.pixelsInfobox ? " ("+yM+")":"")
      };
      
      for (String s : phrases) {
        Rectangle2D rec = g.getFontMetrics().getStringBounds(s, g);
        if (width<rec.getWidth())
          width=(int) rec.getWidth();
        height+= rec.getHeight();
      }
      
      if (fp.o.getFunctionsInInfobox() %3 == 2)
        for (Function f : fp.functions.keySet()) {
          Rectangle2D rec = g.getFontMetrics().getStringBounds(
              "f(x)="+f.getFunctionTerm()+"="+formatNumber(f.calculate(fp.getValueOfXPixel(xM)), fp.o.getRoundInfoBox(), false),g);
          if (width<rec.getWidth())
            width=(int) rec.getWidth();
          height+= rec.getHeight();
          allowedFunctions.put(f, fp.functions.get(f));
        }
      else if (fp.o.getFunctionsInInfobox() %3 == 1) {
        int radius = fp.o.getFunctionSearchRadius();
        if (fp.o.debug) g.drawOval(xM-radius, yM-radius, radius*2+1, radius*2+1);
        
        for (Function f : fp.functions.keySet()) {
          
          for (int xFunc = xM-radius; xFunc < xM+radius; ++xFunc) {
            Line2D.Double line = new Line2D.Double(
                xFunc, 
                fp.getPixelToYValue(f.calculate(fp.getValueOfXPixel(xFunc))), 
                xFunc+1, 
                fp.getPixelToYValue(f.calculate(fp.getValueOfXPixel(xFunc+1))));
            
            if (fp.o.debug) {
              Color c = g.getColor();
              g.setColor(new Color(0, 0, 0, 64));
              g.drawLine((int)line.x1,(int)line.y1,(int)line.x2,(int)line.y2);
              g.setColor(c);
            }
            
            if (line.ptLineDist(infoMouse)<=radius) {
              Rectangle2D rec = g.getFontMetrics().getStringBounds(
                  "f(x)="+f.getFunctionTerm()+"="+formatNumber(f.calculate(fp.getValueOfXPixel(xM)), fp.o.getRoundInfoBox(), false),g);
              if (width<rec.getWidth())
                width=(int) rec.getWidth();
              height+= rec.getHeight();
              allowedFunctions.put(f, fp.functions.get(f));
              break;
            }
          }
        }
      }
      
      
      int xBox, yBox;
      
      if (fp.o.dockInfobox) {
        xBox = (fp.getWidth()-12-width > xPx+4) ? fp.getWidth()-12-width : 5;
        yBox = (fp.getHeight()-8-height > yPx+4) ? fp.getHeight()-8-height : 5;
        
      } else {
        xBox = (xM+15+width>fp.getWidth() -12) ? xM-15-width : xM+15;
        yBox = (yM + height>fp.getHeight()-12) ? yM - height-6 : yM;
      }
      
      g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), fp.o.infoboxAlpha));
      
      g.fillRect(xBox+1, yBox+1, width+5, height+1);
      
      g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), fp.o.infoboxAlpha));
      
      g.drawRect(xBox, yBox, width+6, height+2);
      int elementHeight = height/(2+allowedFunctions.size());
      g.drawString(phrases[0], xBox+3, yBox-2+elementHeight);
      g.drawString(phrases[1], xBox+3, yBox-2+elementHeight*2);
      
      int num = 3;
      for (Function f : allowedFunctions.keySet()) {
        Color fc = allowedFunctions.get(f);
        g.setColor(new Color(fc.getRed(), fc.getGreen(), fc.getBlue(), fp.o.infoboxAlpha));
        g.drawString("f(x)="+f.getFunctionTerm()+"="+formatNumber(f.calculate(fp.getValueOfXPixel(xM)), fp.o.getRoundInfoBox(), false), 
            xBox+3, yBox-2+elementHeight*num);
        num++;
      }
      
    }
  }
  
  
  private void drawDebug(Graphics g) {
    String[] info = {
        "DEBUG INFO:",
        "ProgramVersion   = \""+PolyPlot.VERSION+"\"",
        "Span             = \""+fp.span+"\"",
        "SpanX            = \""+fp.spanX+"\"",
        "SpanY            = \""+fp.spanY+"\"",
        "xCorner          = \""+fp.xCorner+"\"",
        "yCorner          = \""+fp.yCorner+"\"",
        "ValuePerXPixel   = \""+fp.getValueXPerPixel()+"\"",
        "ValuePerYPixel   = \""+fp.getValueYPerPixel()+"\"",
        "Power            = \""+fp.getPower()+"\"",
        "RenderTime       = \""+fp.renderTime+"ns\"",
    };
    Font f = g.getFont();
    g.setFont(new Font("Courier New", Font.PLAIN, 12));
    
    int  i = 0;
    for (String s : info) {
      Rectangle2D rec = g.getFontMetrics().getStringBounds(s, g);
      g.setColor(new Color(255,255,255,0xCC));
      g.fillRect((int)rec.getX(), (int)(rec.getY()*i), (int)rec.getWidth(), (int)rec.getHeight()-2);
      g.setColor(new Color(0,0,0,0xCC));
      if (i == 0) {
        Font f0 = g.getFont();
        g.setFont(f0.deriveFont(Font.BOLD));
        g.drawString(s, 0, (int)(rec.getY()*(i-1)));
        g.setFont(f0);
      } else
        g.drawString(s, 0, (int)(rec.getY()*(i-1)));
      
      i--;
    }
    
    g.setFont(f);
  }

  void drawOverlay(Graphics g) {

  //    System.out.println(fp.getValueXPerPixel());
      
      g.setColor(fp.o.scaleColor);
  //    System.out.println(fp.getPixelToYValue(0));
      
      
      int y = fp.getPixelToYValue(0);
      int x = fp.getPixelToXValue(0);
      
      
      int yPx = y < 0 ? 0 : y > fp.getHeight()-1 ? fp.getHeight()-1 : (int) y;
      int xPx = x < 0 ? 0 : x > fp.getWidth()-1 ? fp.getWidth()-1 : (int) x;
      
//      for (double i = fp.xCorner; i <= fp.xCorner+fp.spanX; i+=0.5)
//        drawHorizontialScale(new Point(fp.getPixelToXValue(i), yPx), i, i!=0,0,false, g);
//      
//      for (double i = fp.yCorner; i >= fp.yCorner-fp.spanY; i-=0.5) {
//        drawVerticalScale(new Point(xPx, fp.getPixelToYValue(i)), i, i!=0, 0, false, g);
//      }
      g.drawLine(0, yPx, fp.getWidth(), yPx);
      g.drawLine(xPx, 0, xPx, fp.getWidth());
      
      //drawScale(xPx, yPx, g);
      
      //drawInfoBox(xPx, yPx, g);
      
//      if (fp.o.debug)
//        drawDebug(g);
      
  }
  
  
  private String formatNumber(double number, int round,  boolean expodential) {
    String digits = "0.0";
//    if (round > -1)
//      for (int i = 0; i < round; ++i)
//        digits+='#';
//    else
//      digits = "0";
//    
//    digits = "0."+digits;
    
    if (expodential)
      digits+="E0";
    
    DecimalFormat f = new DecimalFormat(digits);
    f.setDecimalSeparatorAlwaysShown(false); // Needed?
    
    if (round > -1)
      f.setMaximumFractionDigits(round);
    else
      f.setMaximumFractionDigits(42);
      
    
    
    
//    Number n = (round>-1) ? Math.round(number.doubleValue()*Math.pow(10, round))/Math.pow(10, round)
//    : number.doubleValue();
//    
//    if (n.doubleValue()%1==0)
//      n = n.intValue();
//    
//    NumberFormat f = new DecimalFormat("");
    
    
    return f.format(number); //n.toString();
  }
  
  private void drawScale(int scaleX, int scaleY, Graphics g) {
    int pow = 42;
    
    for (int i = pow; fp.spanX/2<Math.pow(10, i); --i)
      pow = i;
    --pow;
    //TODO Finish him!
    double startX = fp.xCorner-((fp.xCorner<0 ? Math.pow(10, pow)+fp.xCorner%Math.pow(10, pow) : fp.xCorner%Math.pow(10, pow)));
    double startY = fp.yCorner-((fp.yCorner<0 ? Math.pow(10, pow)+fp.yCorner%Math.pow(10, pow) : fp.yCorner%Math.pow(10, pow)));
//    System.out.println(startX);
//    for (int i )
    
//    System.out.println(startX+ ";; "+Math.pow(10, pow-1) + "; " + (int)fp.spanX/Math.pow(10, pow-1));

    for (double x = startX; x < fp.xCorner+fp.spanX; x+=Math.pow(10, pow-1))
      drawHorizontialScale(
          new Point(fp.getPixelToXValue(x), scaleY), 
          x, 
          x%Math.pow(10, pow) == 0 || x%(5*Math.pow(10, pow-1))==0, 
          (x%Math.pow(10, pow) == 0) ? 0 : (x%(5*Math.pow(10, pow-1))==0) ? 1 : 2, 
          pow>5, 
          g);
    
    for (double y = startY; y < fp.yCorner+fp.spanY; y+=Math.pow(10, pow-1))
      drawVerticalScale(
          new Point(scaleX, fp.getPixelToYValue(y)), 
          y, 
          y%Math.pow(10, pow) == 0 || y%(5*Math.pow(10, pow-1))==0, 
          (y%Math.pow(10, pow) == 0) ? 0 : (y%(5*Math.pow(10, pow-1))==0) ? 1 : 2, 
          pow>5, 
          g);
    
  }
  
  
  
  
  
  private void registerOverlayEvents() {
    fp.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        infoMouse = e.getPoint();
        fp.repaint();
      }
      @Override
      public void mouseExited(MouseEvent e) {
        infoMouse = null;
        fp.repaint();
      }
    });
    fp.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        infoMouse = e.getPoint();
        fp.repaint();
      }
      @Override
      public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
      }
      
    });
  }
}
