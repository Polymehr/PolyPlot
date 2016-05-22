package polyplot.graphics;

import polyplot.math.*;
import polyplot.math.Compiler;
import polyplot.PolyPlot;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class FunctionPlotter extends JPanel implements Observer {

    private static final long serialVersionUID = 1L;

    private long renderTime;

    private List<DrawableComponent> toDraw;
    private List<DrawableComponent> overlayComponents;

    private Insets boundOffset;

    Options o;


    private double xCorner;
    private double yCorner;

    private final double spanBase;
    private double span, spanX, spanY;

    private Point mouse;

    private int pow;

    private double zoomBase;
    private int zoom;


    private final JPanel overlay;
    private final InfoBox info;
    private final DebugGUI debug;
    private final InputField inputField;
    private final CheatSheet help;
    private final FunctionOverview functionInfo;

    private final Compiler compiler;
    private List<DrawableFunction> functions;
    private final Map<String, Color> functionColors;

    public FunctionPlotter(double span) {
        if (span <= 0)
            throw new IllegalArgumentException("Span cannot be negative or zero!");
        this.setLayout(new BorderLayout());
        super.setFocusable(true);
        super.requestFocusInWindow();


        overlay = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics gc) {
                for (DrawableComponent dc : overlayComponents)
                    dc.draw(gc, FunctionPlotter.this);
            }
        };
        overlay.setBackground(new Color(0x00, true));
        overlay.setOpaque(false);

        this.span = spanBase = span;
        yCorner = -(span / 2);
        xCorner = -(span / 2);

        o = new Options();
        boundOffset = new Insets(0, 0, 0, 0);

        registerKeyBindings();
        registerMouseListener();

        toDraw = new LinkedList<>();
        overlayComponents = new LinkedList<>();
        toDraw.add(new Scale(o.scaleColor));
        info = new InfoBox(o.scaleColor, new Color(0x50_000000 | o.backgroundColor.getRGB(), true), true, true, true, -1);
        overlayComponents.add(info);
        functionInfo = new FunctionOverview(o.scaleColor, new Color(0xA0_FFFFFF, true), true, true, false);
        overlayComponents.add(functionInfo);
        help = new CheatSheet(o.scaleColor, new Color(0xB8_EFFFFF, true), true);
        overlayComponents.add(help);
        inputField = new InputField(o.scaleColor, new Color(0x7F_FFFFFF, true), true, this);
        overlayComponents.add(inputField);
        debug = this.new DebugGUI(new Color(0, 0, 0, 0xE0), new Color(0xFF, 0xFF, 0xFF, 0xE0), true);
        overlayComponents.add(debug);


        //jlp.add(this, 0);
        //jlp.add(overlay, 1);
        this.add(overlay, BorderLayout.CENTER);

        mouse = null;

        zoom = 0;
        zoomBase = 1.05;

        compiler = new Compiler(new CompilationContext(true));
        functions = new ArrayList<>(10);
        compiler.getContext().addObserver(this);
        functionColors = new HashMap<>();

        updateSpans();
        updatePow();
    }

    public double getValueXPerPixel() {
        return spanX / getWidth();
    }

    public double getValueYPerPixel() {
        return spanY / getHeight();
    }

    public double getValueOfXPixel(int pixel) {
        return xCorner + pixel * getValueXPerPixel();
    }

    public double getValueOfYPixel(int pixel) {
        return (yCorner + (getHeight() - pixel - 1) * getValueYPerPixel());
    }

    public int getPixelToYValue(double value) {
        if (value == Double.NaN)
            throw new IllegalArgumentException("Value has to be a real number! " + value);
        return (int) (getHeight() - (value / getValueYPerPixel() - yCorner / getValueYPerPixel()) - 1);
    }

    public int getPixelToXValue(double value) {
        if (value == Double.NaN)
            throw new IllegalArgumentException("Value has to be a real number! " + value);
        return (int) ((value / getValueXPerPixel() - xCorner / getValueXPerPixel()) - 1);
    }

    public double getYSpan() {
        return spanY;
    }

    public double getXCorner() {
        return xCorner;
    }

    public double getYCorner() {
        return yCorner;
    }

    public double getSpan() {
        return span;
    }

    public double getXSpan() {
        return spanX;
    }

    public Point getMousePos() {
        return mouse;
    }

    public int getPower() {
        return pow;
    }

    List<DrawableFunction> getFunctions() {
        return functions;
    }

    CompilationContext getFunctionContext() {
        return compiler.getContext();
    }

    /**
     * @return the bounds of the coordinate system. These can change if
     * some components are drawn that need a part of the screen space.
     */
    public Rectangle getBounds() {
        Rectangle actual = super.getBounds();
        return new Rectangle(actual.x + boundOffset.left, actual.y + boundOffset.top,
                actual.width - boundOffset.right, actual.height - boundOffset.bottom);
    }

    boolean setBoundOffsetBottom(int value) {
        boolean repaint = boundOffset.bottom != value;
        boundOffset.bottom = value;
        if(repaint)
            repaint();
        return repaint;
    }

    boolean setBoundOffsetLeft(int value) {
        boolean repaint = boundOffset.left != value;
        boundOffset.left = value;
        if(repaint)
            repaint();
        return repaint;
    }

    boolean setBoundOffsetRight(int value) {
        boolean repaint = boundOffset.right != value;
        boundOffset.right = value;
        if(repaint)
            repaint();
        return repaint;
    }

    boolean setBoundOffsetTop(int value) {
        boolean repaint = boundOffset.top != value;
        boundOffset.top = value;
        if(repaint)
            repaint();
        return repaint;
    }

    JPanel getOverlay() {
        return overlay;
    }

    boolean isDebugActive() {
        return !debug.isHidden();
    }

    @Override
    public void paintComponent(Graphics g) {
        final Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        updateSpans();
//    xCorner+=5;
//    yCorner+=5;
        g.setColor(o.backgroundColor);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (DrawableComponent dc : toDraw) {
            dc.draw(g, this);
        }

        drawFunctions(g);
    }

    private void paintOverlay(Graphics gc, boolean useCache) {
        if (useCache) {
            //TODO Draw Cache
            overlay.repaint();
        } else {
            repaint();
        }

    }


    /**
     * Draws all functions that are registered in <code>functions</code>, in their color.
     *
     * @param g
     */
    private void drawFunctions(Graphics g) {
        for (DrawableFunction f : functions) f.draw(g, this);
    }


    private void move(Point a, Point b) {

        double ya = getValueOfYPixel(a.y);
        double xa = getValueOfXPixel(a.x);

        double yb = getValueOfYPixel(b.y);
        double xb = getValueOfXPixel(b.x);


        double yd = yb - ya;
        double xd = xb - xa;

        yCorner -= yd;
        xCorner -= xd;
    }


    public void zoom(Point center, int factor) {

        int y0 = center.y, x0 = center.x;

        double vy = getValueOfYPixel(y0), vx = getValueOfXPixel(x0);

        span = spanBase * Math.pow(zoomBase, zoom += factor);
        updateSpans();
        int x1 = getPixelToXValue(vx), y1 = getPixelToYValue(vy);

        move(new Point(x1, y1), center);
    }

    public void setZoom(Point center, int newValue) {
        int y0 = center.y, x0 = center.x;

        double vy = getValueOfYPixel(y0), vx = getValueOfXPixel(x0);

        span = spanBase * Math.pow(zoomBase, zoom = newValue);
        updateSpans();
        int x1 = getPixelToXValue(vx), y1 = getPixelToYValue(vy);

        move(new Point(x1, y1), center);
    }

    private void updateSpans() {
        if (o.stretch || getHeight() == getWidth()) {
            spanY = span;
            spanX = span;
        } else if (getHeight() < getWidth()) {
            spanY = span;
            spanX = getValueYPerPixel() * (getWidth() - 1);
        } else {
            spanX = span;
            spanY = getValueXPerPixel() * (getHeight() - 1);
        }

        updatePow();
    }

    private void updatePow() {
        final double span = this.span / 2;
        if (span > 1)
            for (int i = 0; span >= Math.pow(10, i); ++i) {
                pow = i;
            }
        else
            for (int i = 0; span <= Math.pow(10, i); --i) {
                pow = i - 1;
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
                zoom(new Point(getWidth() / 2, getHeight() / 2), -1);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        action.put("zoomOut", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                zoom(new Point(getWidth() / 2, getHeight() / 2), 1);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomInFast");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomInFast");
        action.put("zoomInFast", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                zoom(new Point(getWidth() / 2, getHeight() / 2), -10);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomOutFast");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomOutFast");
        action.put("zoomOutFast", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                zoom(new Point(getWidth() / 2, getHeight() / 2), 10);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "zoomReset");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK), "zoomReset");
        action.put("zoomReset", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setZoom(new Point(getWidth() / 2, getHeight() / 2), 0);
                repaint();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "toggleInfo");
        action.put("toggleInfo", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                info.toggleHidden();
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "toggleHelp");
        action.put("toggleHelp", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                help.toggleHidden();
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "toggleDrawPoints");
        action.put("toggleDrawPoints", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                DrawableFunction.DRAW_POINTS = !DrawableFunction.DRAW_POINTS;
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "toggleFuncInfo");
        action.put("toggleFuncInfo", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                functionInfo.toggleHidden();
                repaint();
            }
        });
        Consumer<String> addFunction = s -> {
            try {
                compiler.definition(s);
            } catch (IllegalArgumentException|IllegalStateException ex) {
                inputField.outputException(ex);
            }
        };
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "addFunction");
        action.put("addFunction", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                inputField.read("Add function or constant", false, true, addFunction, FunctionPlotter.this);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK), "addFunctionKeep");
        action.put("addFunctionKeep", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                inputField.read("Add function or constant", true, true, addFunction, FunctionPlotter.this);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "evaluate");
        action.put("evaluate", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                inputField.read("Evaluate", false, false, s -> {
                    try {
                        inputField.outputLine(s + " = " + compiler.constantExpression(s));
                    } catch (Exception ex) {
                        inputField.outputException(ex);
                    }
                }, FunctionPlotter.this);
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
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "toggleDebug");
        action.put("toggleDebug", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                debug.toggleHidden();
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.CTRL_DOWN_MASK), "debugConsole");
        action.put("debugConsole", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                ScriptEngineManager sem = new ScriptEngineManager();
                ScriptEngine js = sem.getEngineByName("JavaScript");
                StringWriter sw = new StringWriter();
                js.getContext().setWriter(sw);
                js.put("fp", FunctionPlotter.this);

                try {
                    js.eval("function hack(obj, field) {" +
                            "var field = obj.getClass().getDeclaredField(field);" +
                            "field.setAccessible(true);" +
                            "return field;" +
                            "}");
                } catch (ScriptException e1) {
                    e1.printStackTrace();
                }
                final int[] outputLineCount = {0};
                inputField.read("Console", true, false, x -> {
                    try {
                        Object o = js.eval(x);
                        inputField.outputOutput("<< " + Objects.toString(o, "undefined"));
                        try (Scanner s = new Scanner(sw.toString())) {
                            for (int l = 0; s.hasNextLine(); ++l) {
                                if (l >= outputLineCount[0]) {
                                    ++outputLineCount[0];
                                    inputField.outputLine(s.nextLine());
                                } else
                                    s.nextLine();
                            }
                        }
                    } catch (ScriptException se) {
                        inputField.outputException(se);
                    }
                }, FunctionPlotter.this);
                repaint();
            }
        });


        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "left");
        action.put("left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point((int) +(getWidth() * 0.1), 0));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK), "leftSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.SHIFT_DOWN_MASK), "leftSlow");
        action.put("leftSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(+1, 0));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "right");
        action.put("right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point((int) -(getWidth() * 0.1), 0));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK), "rightSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.SHIFT_DOWN_MASK), "rightSlow");
        action.put("rightSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(-1, 0));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "up");
        action.put("up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, (int) +(getHeight() * 0.1)));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK), "upSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.SHIFT_DOWN_MASK), "upSlow");
        action.put("upSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, +1));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0), "down");
        action.put("down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, (int) -(getHeight() * 0.1)));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK), "downSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.SHIFT_DOWN_MASK), "downSlow");
        action.put("downSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, -1));
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, 0), "centerYAxis");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, 0), "centerYAxis");
        action.put("centerYAxis", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                xCorner = -(spanX / 2);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0), "centerXAxis");
        action.put("centerXAxis", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                yCorner = -(spanY / 2);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0), "centerToOrigin");
        action.put("centerToOrigin", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                yCorner = -(spanY / 2);
                xCorner = -(spanX / 2);
                repaint();
            }
        });
    }

    void enableKeyBindings(final boolean enable) {
        ActionMap map = this.getActionMap();
        for (Object o : map.allKeys())
            map.get(o).setEnabled(enable);
    }

    private void registerMouseListener() {
        this.addMouseWheelListener(e -> {

            if (e.isControlDown())
                if (e.getWheelRotation() < 0)
                    for (int i = 0; i > e.getWheelRotation(); --i)
                        zoom(e.getPoint(), -1);
                else
                    for (int i = 0; i < e.getWheelRotation(); ++i)
                        zoom(e.getPoint(), 1);

            else if (e.isShiftDown())
                if (e.getWheelRotation() < 0)
                    for (int i = 0; i > e.getWheelRotation(); --i)
                        move(e.getPoint(), new Point(e.getPoint().x, e.getPoint().y + (int) ((e.isAltDown() ? 1 :
                                getHeight() * 0.1))));
                else
                    for (int i = 0; i < e.getWheelRotation(); ++i)
                        move(e.getPoint(), new Point(e.getPoint().x, e.getPoint().y - (int) ((e.isAltDown() ? 1 :
                                getHeight() * 0.1))));
            else if (e.getWheelRotation() < 0)
                for (int i = 0; i > e.getWheelRotation(); --i)
                    move(e.getPoint(), new Point(e.getPoint().x - (int) ((e.isAltDown() ? 1 :
                            getHeight() * 0.1)), e.getPoint().y));
            else
                for (int i = 0; i < e.getWheelRotation(); ++i)
                    move(e.getPoint(), new Point(e.getPoint().x + (int) ((e.isAltDown() ? 1 :
                            getHeight() * 0.1)), e.getPoint().y));

            repaint();
        });


        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouse = e.getPoint();
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint();
                if (!info.isHidden())
                    repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                move(mouse, e.getPoint());
                mouse = e.getPoint();
                repaint();
            }

        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouse = e.getPoint();
                if (!info.isHidden())
                    repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouse = e.getPoint();
                if (!info.isHidden())
                    repaint();
            }
        });
    }

    @Override
    public void repaint() {
        if (debug == null || debug.isHidden())
            super.repaint();
        else {
            renderTime = System.nanoTime();
            super.repaint();
            renderTime = System.nanoTime() - renderTime;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        functions.clear();
        compiler.getContext().getFunctions(true).forEach(f -> {
            if (f instanceof PureFunction) {
                final Color tmp;
                if (functionColors.containsKey(f.getName())) tmp = functionColors.get(f.getName());
                else tmp = new Color(this.o.functionColors[functionColors.size() % this.o.functionColors.length]);
                functionColors.put(f.getName(), tmp);
                functions.add(new DrawableFunction(tmp, (PureFunction) f));
            }
        });
        repaint();
    }

    private class DebugGUI extends DrawableComponent {

        public DebugGUI(Color foreground, Color background, boolean hidden) {
            super(foreground, background, hidden);
        }

        @Override
        public void draw(Graphics gc, FunctionPlotter parent) {
            if (hidden)
                return;

            String[] info = {
                    "DEBUG INFO:",
                    "program_version   = " + PolyPlot.VERSION,
                    "span_base         = " + spanBase,
                    "span              = " + span,
                    "span_x            = " + spanX,
                    "span_y            = " + spanY,
                    "x_corner          = " + xCorner,
                    "y_corner          = " + yCorner,
                    "value_per_x_pixel = " + getValueXPerPixel(),
                    "value_per_y_pixel = " + getValueYPerPixel(),
                    "zoom              = " + zoom,
                    "zoom_base         = " + zoomBase,
                    "power             = " + pow,
                    "resolution        = " + getWidth()+"x"+getHeight(),
                    "render_time       = " + renderTime + "ns",
            };
            Font f = gc.getFont();
            gc.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            int i = 0;
            for (String s : info) {
                Rectangle2D rec = gc.getFontMetrics().getStringBounds(s, gc);
                int width = gc.getFontMetrics().stringWidth(s);
                int height = gc.getFontMetrics().getHeight();
                int x = 0;
                int y = (i + 1) * height - 5;

                gc.setColor(background);
                gc.fillRect(x, y - height + 3, width, height);
                gc.setColor(foreground);
                if (i == 0) {
                    Font f0 = gc.getFont();
                    gc.setFont(f0.deriveFont(Font.BOLD));
                    gc.drawString(s, 0, y);
                    gc.setFont(f0);
                } else
                    gc.drawString(s, 0, y);

                ++i;
            }

            gc.setFont(f);
        }
    }
}
