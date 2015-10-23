package me.polymehr.polyPlot.graphics.plotter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import me.polymehr.polyPlot.functions.ConstantFunction;
import me.polymehr.polyPlot.functions.Function;
import me.polymehr.polyPlot.functions.FunctionUtil;
import me.polymehr.polyPlot.functions.properties.Periodic;
import me.polymehr.polyPlot.functions.properties.Poles;

public class FunctionPlotter extends JPanel {

  private static final long serialVersionUID = 1L;
  long renderTime;
  
  HashMap<Function, Color> functions;
  
  Options o;
  private OverlayPainter op;
  
  
  double xCorner;
  
  double yCorner;
  
  double span, spanX, spanY;
  
  
  
  public FunctionPlotter(double span) {
    if (span <= 0)
      throw new IllegalArgumentException("Span cannot be negative or zero!");
    
    this.span = span;
    
    
    yCorner = -(span/2);
    xCorner = -(span/2);
    
    op = new OverlayPainter(this);
    o = new Options();
    
    registerKeyBindings();
    registerMouseListener();
    
    functions = new HashMap<Function, Color>();
    
  }
  
  
  
  
  
  
  
  public void addFunction(Function f) {
    addFunction(f, o.graphColor);
    
    o.graphColor = new Color(o.functionColors[functions.size()%o.functionColors.length]);
    
  }

  public void addFunction(Function f, Color c) {
    if (functions.size() >= o.getMaxFunctions())
      throw new UnsupportedOperationException("No more functions can be added. Function limit ("+o.getMaxFunctions()+") reached!");
    
    for (Function dup : functions.keySet())
      if (dup.getFunctionTerm().equals(f.getFunctionTerm()))
        return;
    
    functions.put(f, c);
  }
  
  public void removeFunction(String functionTerm) {
    for (Function key : functions.keySet())
      if (key.getFunctionTerm().equals(functionTerm)) {
        functions.remove(key);
        return;
      }
    
    throw new IllegalArgumentException("Function with function term '" + functionTerm + "' not found.");
  }







  double getValueXPerPixel() {
    return spanX/getWidth();
  }
  double getValueYPerPixel() {
    return spanY/getHeight();
  }
  
  double getValueOfXPixel(int pixel) {
    return xCorner+pixel*getValueXPerPixel();
  }
  
  double getValueOfYPixel(int pixel) {
    return (yCorner+(getHeight()-pixel-1)*getValueYPerPixel());
  }
  
  int getPixelToYValue(double value) {
    if (value == Double.NaN)
      throw new IllegalArgumentException("Value has to be a real number! "+value);
    return (int) (getHeight()-(value/getValueYPerPixel()-yCorner/getValueYPerPixel())-1);
  }
  int getPixelToXValue(double value) {
    if (value == Double.NaN)
      throw new IllegalArgumentException("Value has to be a real number! "+value);
    return (int) ((value/getValueXPerPixel()-xCorner/getValueXPerPixel())-1);
  }
  
  
  
  
  
  
  
  
  
  @Override
  public void paint(Graphics g) {
    reCalculateSpans();
//    xCorner+=5;
//    yCorner+=5;
    g.setColor(o.backgroundColor);
    g.fillRect(0, 0, getWidth(), getHeight());
    
    drawFunctions(g);
    op.drawOverlay(g);
    
    
  }
  


  /**
   * Draws all functions that are registered in <code>functions</code>, in their color.
   * @param g
   */
  private void drawFunctions(Graphics g) {
    for (Function f : functions.keySet()) {
      g.setColor(functions.get(f));
      if (f instanceof ConstantFunction) {
        
        int y = getPixelToYValue(f.calculate(0));
        
        g.drawLine(-1, y, getWidth(), y);
        
      } else if (o.poleRecognition && f instanceof Poles) {
        Poles p = (Poles) f;
        
        FunctionUtil.setLowerBound(getValueOfXPixel(0));
        FunctionUtil.setUpperBound(getValueOfXPixel(getWidth()-1));
        
        p.calculatePoles();


        for (int i = -1; i < getWidth(); ++i) {
          
          double xLst = getValueOfXPixel(i-1); // TODO: Better implementation
          double x0    = getValueOfXPixel(i);
          double x1    = getValueOfXPixel(i+1);
          double xNxt  = getValueOfXPixel(i+2);
          
          double yLst = f.calculate(xLst);
          double y0   = f.calculate(x0);
          double y1   = f.calculate(x1);
          double yNxt = f.calculate(xNxt);
          
          
          final double NaN = Double.NaN, 
              NEG_INF = Double.NEGATIVE_INFINITY, POS_INF = Double.POSITIVE_INFINITY;

          
          if (y0!=NaN&&y1!=NaN)
            if (p.isPole(x1, xNxt)) {
              g.drawLine(
                  i, getPixelToYValue(y0),
                  i+1, getPixelToYValue(y0<yLst? NEG_INF : POS_INF));
            }else if (p.isPole(xLst, x0))
              g.drawLine(
                  i, getPixelToYValue(y1<yNxt? NEG_INF : POS_INF),
                  i+1, getPixelToYValue(y1));
            else if (!p.isPole(x0, x1))
              g.drawLine(
                  i, getPixelToYValue(y0),
                  i+1, getPixelToYValue(y1));
        }
        
      } else {
        
        for (int i = -1; i < getWidth(); ++i) {

          double x0   = getValueOfXPixel(i);
          double x1   = getValueOfXPixel(i+1);

          double y0   = f.calculate(x0);
          double y1   = f.calculate(x1);

          
          final double NaN = Double.NaN;

          if (y0!=NaN&&y1!=NaN)
            g.drawLine(
                i, getPixelToYValue(y0),
                i+1, getPixelToYValue(y1));
        }
        
      }
         
    }
  }
  
  
  
  private void move(Point a, Point b) {
    
    double ya = getValueOfYPixel(a.y);
    double xa = getValueOfXPixel(a.x);
    
    double yb = getValueOfYPixel(b.y);
    double xb = getValueOfXPixel(b.x);
    
    
    double yd = yb-ya;
    double xd = xb-xa;
    
    yCorner-=yd;
    xCorner-=xd;
  }
  
  
  private void zoomIn(Point center, double percentage) {
    if (percentage <= 0 || percentage >= 1)
      throw new IllegalArgumentException("Illegal zoom value: " + percentage*100+"%");
    
    int y0 = center.y, x0 = center.x;
    
    if (y0<0 || x0<0  || y0>getHeight()-1 || x0>getWidth()-1)
      throw new IllegalArgumentException("Zoom center out of bounds: " + x0 + ","+y0);
    
    double vy = getValueOfYPixel(y0), vx = getValueOfXPixel(x0);
    
    span -= span*percentage;
    reCalculateSpans();
    int x1 = getPixelToXValue(vx), y1 = getPixelToYValue(vy);
    
    move(new Point(x1, y1), center);
    
  }
  
  private void zoomOut(Point center, double percentage) {
    if (percentage <= 0)
      throw new IllegalArgumentException("Illegal zoom value: " + percentage*100+"%");
    
    int y0 = center.y, x0 = center.x;
    
    if (y0<0 || x0<0  || y0>getHeight()-1 || x0>getWidth()-1)
      throw new IllegalArgumentException("Zoom center out of bounds: " + x0 + ","+y0);
    
    double vy = getValueOfYPixel(y0), vx = getValueOfXPixel(x0);
    
    span += span*percentage;
    reCalculateSpans();
    int x1 = getPixelToXValue(vx), y1 = getPixelToYValue(vy);
    
    move(new Point(x1, y1), center);
  }
  
  private void reCalculateSpans() {
      if (o.stretch || getHeight()==getWidth()) {
        spanY = span;
        spanX = span;
      } else if (getHeight()<getWidth()) {
        spanY = span;
        spanX = getValueYPerPixel()*(getWidth()-1);
      } else {
        spanX=span;
        spanY=getValueXPerPixel()*(getHeight()-1);
      }    
  }

  
  
  
  
  
  
  /**
   * Registers following key bindings:<br>
   * <ul>
   * <li>Zoom in (center): <code>'CTRL'+'+'</code></li>
   * <li>Zoom out (center): <code>'CTRL'+'-'</code></li>
   * <li>Refresh: <code>'F5'</code></li>
   * <li>Toggle info box: <code>'I'</code></li>
   * <li>Toggle rounding: <code>'F5'</code></li>
   * <li>Toggle debug info: <code>'CTRL'+'F3'</code></li>
   * </ul>
   */
  private void registerKeyBindings() {
    InputMap input = this.getInputMap(WHEN_IN_FOCUSED_WINDOW);
    ActionMap action = this.getActionMap();
    
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn");
    action.put("zoomIn", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      @Override
      public void actionPerformed(ActionEvent e) {
        zoomIn(new Point(getWidth()/2, getHeight()/2), 0.05);
        repaint();
      }
    });
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");
    action.put("zoomOut", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      @Override
      public void actionPerformed(ActionEvent e) {
        zoomOut(new Point(getWidth()/2, getHeight()/2), 0.0526315789473684);
        repaint();
      }
    });
    
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0), "toggleInfo");
    action.put("toggleInfo", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      @Override
      public void actionPerformed(ActionEvent e) {
        o.infobox = !o.infobox;
        repaint();
      }
    });
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "reRender");
    action.put("reRender", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      @Override
      public void actionPerformed(ActionEvent e) {
        repaint();
      }
    });
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "toggleRounding");
    action.put("toggleRounding", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      @Override
      public void actionPerformed(ActionEvent e) {
        if (o.getRoundInfoBox() == 0)
          o.setRoundInfoBox(Integer.MIN_VALUE);
        else if (o.getRoundInfoBox() == Integer.MIN_VALUE)
          o.setRoundInfoBox(0);
        else
          o.setRoundInfoBox(-o.getRoundScale());
        
        if (o.getRoundScale() == 0)
          o.setRoundScale(Integer.MIN_VALUE);
        else if (o.getRoundScale() == Integer.MIN_VALUE)
          o.setRoundScale(0);
        else
          o.setRoundScale(-o.getRoundScale());
        
        repaint();
      }
    });
    input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.CTRL_DOWN_MASK), "toggleDebug");
    action.put("toggleDebug", new AbstractAction() {
      private static final long serialVersionUID = 1L;
      @Override
      public void actionPerformed(ActionEvent e) {
        o.debug = !o.debug;
        repaint();
      }
    });
  }
  
  private Point mouse = null;
  
  private void registerMouseListener() {
    this.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        
        if (e.isControlDown())
          if (e.getWheelRotation()<0)
            for (int i = 0; i > e.getWheelRotation(); --i)
              zoomIn(e.getPoint(), 0.05);
          else
            for (int i = 0; i < e.getWheelRotation(); ++i)
              zoomOut(e.getPoint(), 0.0526315789473684);
        
        else if (e.isShiftDown())
          if (e.getWheelRotation()<0)
            for (int i = 0; i > e.getWheelRotation(); --i)
              move(e.getPoint(), new Point(e.getPoint().x, e.getPoint().y+(int)((e.isAltDown()?1:getHeight()*0.1))));
          else
            for (int i = 0; i < e.getWheelRotation(); ++i)
              move(e.getPoint(), new Point(e.getPoint().x, e.getPoint().y-(int)((e.isAltDown()?1:getHeight()*0.1))));
        else
          if (e.getWheelRotation()<0)
            for (int i = 0; i > e.getWheelRotation(); --i)
              move(e.getPoint(), new Point(e.getPoint().x-(int)((e.isAltDown()?1:getHeight()*0.1)), e.getPoint().y));
          else
            for (int i = 0; i < e.getWheelRotation(); ++i)
              move(e.getPoint(), new Point(e.getPoint().x+(int)((e.isAltDown()?1:getHeight()*0.1)), e.getPoint().y));
        
        repaint();
      } 
    });


    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        mouse = e.getPoint();
      }
    });
    this.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        move(mouse, e.getPoint());
        repaint();
        mouse = e.getPoint();
      }
      
    });
  }
  private void registerWindowListeners() {
    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (!o.autoCenter)
          reCalculateSpans();
        else {
          reCalculateSpans();
        }
      }
      @Override
      public void componentHidden(ComponentEvent e) {
       System.out.println("Hi.");
      }
    });
  }
  @Override
  public void repaint() {
    if  (o != null && !o.debug)
      super.repaint();
    else {
      renderTime = System.nanoTime();
      super.repaint();
      renderTime = System.nanoTime()-renderTime;
    }
  }
}
