package polyplot.graphics;

import polyplot.PolyPlot;
import polyplot.math.CompilationContext;
import polyplot.math.Compiler;
import polyplot.math.Function;
import polyplot.math.PureFunction;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A {@link JPanel} that contains a ordinate system that can display
 * {@link DrawableComponent}s. Each pixel of the <code>JPanel</code>
 * has a numerical value and it is possible to zoom in or out.
 *
 * @author Jannik
 */
public class FunctionPlotter extends JPanel implements Observer {

    static final RenderingHints RENDERING_HINTS;
    static {
        RENDERING_HINTS = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RENDERING_HINTS.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        RENDERING_HINTS.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        RENDERING_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);
        RENDERING_HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private long renderTime;

    private List<DrawableComponent> underlayComponents;
    private List<DrawableComponent> overlayComponents;

    private Insets boundOffset;

    Options o;


    private double xCorner;
    private double yCorner;

    private final double spanBase;
    private double span, spanX, spanY;
    private int lastWidth = 0, lastHeight = 0;

    private Point mouse;

    private int pow;

    private double zoomBase;
    private int zoom;


    private final JPanel overlay;
    private final Scale scale;
    private final InfoBox info;
    private final DebugGUI debug;
    private final InputField inputField;
    private final CheatSheet help;
    private final FunctionOverview functionInfo;

    private final Compiler compiler;
    private List<DrawableFunction> functions;
    private DrawableFunction grabbedFunction;
    private final Map<String, Color> functionColors;


    private Mode mode;

    FunctionPlotter() {
        o = Options.INSTANCE;
        mode = Mode.NORMAL;

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

        this.span = spanBase = o.span;
        yCorner = -(span / 2);
        xCorner = -(span / 2);

        boundOffset = new Insets(0, 0, 0, 0);

        registerKeyBindings();
        registerMouseListener();

        DrawableFunction.DRAWING_METHOD = o.functionsPointRendering;

        underlayComponents = new LinkedList<>();
        overlayComponents = new LinkedList<>();
        scale = new Scale(o.scaleColor);
        underlayComponents.add(scale);
        info = new InfoBox(o.infoBoxForeground, o.infoBoxBackground, o.infoBoxDocked, o.infoBoxShowPixels,
                o.infoBoxHidden, o.infoBoxFunctionRadius);
        overlayComponents.add(info);
        functionInfo = new FunctionOverview(o.functionOverviewForeground, o.functionOverviewBackground,
                o.functionOverviewHidden, true, o.functionOverviewShowHidden);
        overlayComponents.add(functionInfo);
        help = new CheatSheet(o.cheatSheetForeground, o.cheatSheetBackground, true);
        overlayComponents.add(help);
        inputField = new InputField(o.inputFieldForeground, o.inputFieldBackground, true,
                o.inputFieldOutputDefault, o.inputFieldOutputInput, o.inputFieldOutputOutput, o.inputFieldOutputError,
                this);
        overlayComponents.add(inputField);
        debug = this.new DebugGUI(o.debugForeground, o.debugBackground, true);
        overlayComponents.add(debug);


        this.add(overlay, BorderLayout.CENTER);

        mouse = null;

        zoom = 0;
        zoomBase = o.zoomBase;

        compiler = new Compiler(new CompilationContext(true));
        functions = new ArrayList<>(10);
        grabbedFunction = null;
        compiler.getContext().addObserver(this);
        functionColors = new HashMap<>();

        updateSpans();
        updatePow();
        debug.updateFuncConstCount();
    }

    double getValueXPerPixel() {
        return spanX / getWidth();
    }

    double getValueYPerPixel() {
        return spanY / getHeight();
    }

    double getValueOfXPixel(int pixel) {
        return xCorner + pixel * getValueXPerPixel();
    }

    double getValueOfYPixel(int pixel) {
        return (yCorner + (getHeight() - pixel - 1) * getValueYPerPixel());
    }

    int getPixelToYValue(double value) {
        if (value != value)
            throw new IllegalArgumentException("Value has to be a real number! " + value);
        return (int) (getHeight() - (value / getValueYPerPixel() - yCorner / getValueYPerPixel()) - 1);
    }

    int getPixelToXValue(double value) {
        if (value == Double.NaN)
            throw new IllegalArgumentException("Value has to be a real number! " + value);
        return (int) ((value / getValueXPerPixel() - xCorner / getValueXPerPixel()) - 1);
    }

    double getYSpan() {
        return spanY;
    }

    double getXCorner() {
        return xCorner;
    }

    double getYCorner() {
        return yCorner;
    }

    double getSpan() {
        return span;
    }

    double getXSpan() {
        return spanX;
    }

    Point getMousePos() {
        return mouse;
    }

    int getPower() {
        return pow;
    }

    Mode getMode() {
        return mode;
    }

    void setMode(Mode mode) {
        this.mode = mode;
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
        g2d.setRenderingHints(RENDERING_HINTS);
        if (this.getWidth() != lastWidth && this.getHeight() != lastHeight) {
            lastWidth  = this.getWidth();
            lastHeight = this.getHeight();

            updateSpans();
        }

        g.setColor(o.backgroundColor);
        g.fillRect(0, 0, getWidth(), getHeight());

        for (DrawableComponent dc : underlayComponents) {
            dc.draw(g, this);
        }

        drawFunctions(g);
    }

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

    private void move(Point a, Point b, boolean function) {
        if (function && mode == Mode.MOVE &&
            this.grabbedFunction != null) {
            this.grabbedFunction.move(b, a, this);
            this.compiler.updateConstants();
        } else if (mode != Mode.INPUT)
            this.move(a, b);
        super.repaint();
    }

    private void center(boolean x, boolean y, boolean function) {
        if (mode != Mode.INPUT) {
            if (x)
                yCorner = -(spanY / 2);
            if (y)
                xCorner = -(spanX / 2);
            if (function && mode == Mode.MOVE && grabbedFunction != null) {
                double xOffset = y ? 0 : grabbedFunction.getXOffset();
                double yOffset = x ? 0 : grabbedFunction.getYOffset();

                grabbedFunction.setOffset(xOffset, yOffset);
            }
        }
        repaint();
    }


    void zoom(Point center, int factor) {

        int y0 = center.y, x0 = center.x;

        double vy = getValueOfYPixel(y0), vx = getValueOfXPixel(x0);

        span = spanBase * Math.pow(zoomBase, zoom += factor);
        updateSpans();
        int x1 = getPixelToXValue(vx), y1 = getPixelToYValue(vy);

        move(new Point(x1, y1), center);
    }

    void setZoom(Point center, int newValue) {
        int y0 = center.y, x0 = center.x;

        double vy = getValueOfYPixel(y0), vx = getValueOfXPixel(x0);

        span = spanBase * Math.pow(zoomBase, zoom = newValue);
        updateSpans();
        int x1 = getPixelToXValue(vx), y1 = getPixelToYValue(vy);

        move(new Point(x1, y1), center);
    }

    private void updateSpans() {
        if (o.scaleStretch || getHeight() == getWidth()) {
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
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                zoom(new Point(getWidth() / 2, getHeight() / 2), -1);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        action.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                zoom(new Point(getWidth() / 2, getHeight() / 2), 1);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomInFast");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomInFast");
        action.put("zoomInFast", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                zoom(new Point(getWidth() / 2, getHeight() / 2), -10);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomOutFast");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,
                InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "zoomOutFast");
        action.put("zoomOutFast", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                zoom(new Point(getWidth() / 2, getHeight() / 2), 10);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "zoomReset");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK), "zoomReset");
        action.put("zoomReset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                setZoom(new Point(getWidth() / 2, getHeight() / 2), 0);
                repaint();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "toggleInfo");
        action.put("toggleInfo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                info.toggleHidden();
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "toggleHelp");
        action.put("toggleHelp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                help.toggleHidden();
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "toggleDrawingMethod");
        action.put("toggleDrawingMethod", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                DrawableFunction.toggleDrawingMethod();
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "toggleFuncInfo");
        action.put("toggleFuncInfo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                functionInfo.toggleHidden();
                repaint();
            }
        });
        Consumer<String> addFunctionConstant = s -> {
            try {
                compiler.definition(s);
            } catch (IllegalArgumentException|IllegalStateException|NullPointerException ex) {
                inputField.postException(ex);
            }
        };
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "addFunctionConstant");
        action.put("addFunctionConstant", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                inputField.read("Add function or constant", false, true, addFunctionConstant, FunctionPlotter.this);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK), "addFunctionConstantKeep");
        action.put("addFunctionConstantKeep", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                inputField.read("Add function or constant", true, true, addFunctionConstant, FunctionPlotter.this);
                repaint();
            }
        });

        Consumer<String> addFunction = s -> {
            String[] functions = s.split(";");
            StringBuilder result = new StringBuilder(s.length());
            final Pattern fp = Pattern.compile("^\\s*(\\w+)\\s*\\(\\s*\\w+\\s*\\)\\s*=.*?$");

            List<String> functionNames = new LinkedList<>();

            for (String func : functions) {
                Matcher m = fp.matcher(func);
                if (m.matches())
                    functionNames.add(m.group(1));
            }

            CompilationContext context = compiler.getContext();
            final char[] alphabet =
                    {
                            'a', 'b', 'c', 'd', 'e', 'f',
                            'g', 'h', 'i', 'j', 'k', 'l',
                            'm', 'n', 'o', 'p', 'q', 'r',
                            's', 't', 'u', 'v', 'w', 'x',
                            'y', 'z'
                    };
            StringBuilder name = new StringBuilder("e");
            for (String func : functions)
                if (func.contains("="))
                    result.append(" ").append(func).append(";");
                else {
                    do {
                        // Generate next name
                        for (int i = name.length()-1;;) {
                            int next = (name.charAt(i) - 0x60) % 26;
                            name.setCharAt(i, alphabet[next]);
                            if (next == 0)
                                if (i-- > 0)
                                    continue;
                                else
                                    name.append('a');
                            break;
                        }
                    } while (context.hasFunction(name.toString()) ||
                             functionNames.contains(name.toString()));
                    // Found a name.
                    result.append(" ").append(name).append("(x) = ").append(func).append(";");
                }
            try {
                compiler.definition(result.toString());
            } catch (IllegalArgumentException|IllegalStateException|NullPointerException ex) {
                inputField.postException(ex);
            }
        };
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "addFunction");
        action.put("addFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                inputField.read("Add function", false, true, addFunction, FunctionPlotter.this);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK), "addFunctionKeep");
        action.put("addFunctionKeep", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                inputField.read("Add function", true, true, addFunction, FunctionPlotter.this);
                repaint();
            }
        });
        Consumer<String> addConstant = s -> {
            String[] constants = s.split(";");
            StringBuilder result = new StringBuilder(s.length());
            final Pattern fp = Pattern.compile("^\\s*(\\w+)\\s*=.*?$");

            List<String> constantNames = new LinkedList<>();

            for (String con : constants) {
                Matcher m = fp.matcher(con);
                if (m.matches())
                    constantNames.add(m.group(1));
            }

            CompilationContext context = compiler.getContext();
            final char[] alphabet =
                    {
                            'a', 'b', 'c', 'd', 'e', 'f',
                            'g', 'h', 'i', 'j', 'k', 'l',
                            'm', 'n', 'o', 'p', 'q', 'r',
                            's', 't', 'u', 'v', 'w', 'x',
                            'y', 'z'
                    };
            StringBuilder name = new StringBuilder("b");
            for (String con : constants)
                if (con.contains("="))
                    result.append(" ").append(con).append(";");
                else {
                    do {
                        // Generate next name
                        for (int i = name.length()-1;;) {
                            int next = (name.charAt(i) - 0x60) % 26;
                            name.setCharAt(i, alphabet[next]);
                            if (next == 0)
                                if (i-- > 0)
                                    continue;
                                else
                                    name.append('a');
                            break;
                        }
                    } while (context.hasConstant(name.toString()) ||
                            constantNames.contains(name.toString()));
                    // Found a name.
                    result.append(" ").append(name).append(" = ").append(con).append(";");
                }
            try {
                compiler.definition(result.toString());
            } catch (IllegalArgumentException|IllegalStateException|NullPointerException ex) {
                inputField.postException(ex);
            }
        };
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "addConstant");
        action.put("addConstant", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                inputField.read("Add constant", false, true, addConstant, FunctionPlotter.this);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK), "addConstantKeep");
        action.put("addConstantKeep", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                inputField.read("Add constant", true, true, addConstant, FunctionPlotter.this);
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "evaluate");
        action.put("evaluate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT) return;
                inputField.read("Evaluate", false, false, s -> {
                    try {
                        inputField.postLine(s + " = " + compiler.constantExpression(s));
                    } catch (Exception ex) {
                        inputField.postException(ex);
                    }
                }, FunctionPlotter.this);
                repaint();
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "reRender");
        action.put("reRender", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "toggleDebug");
        action.put("toggleDebug", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                debug.toggleHidden();
                repaint();
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.CTRL_DOWN_MASK), "debugConsole");
        action.put("debugConsole", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ScriptEngineManager sem = new ScriptEngineManager();
                ScriptEngine js = sem.getEngineByName("JavaScript");
                StringWriter sw = new StringWriter();
                js.getContext().setWriter(sw);
                js.put("__internal_fp", FunctionPlotter.this);

                try {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(
                            FunctionPlotter.class.getResourceAsStream("scripts/debug.js")))) {
                        StringBuilder result = new StringBuilder(420);
                        String input;
                        while ((input = br.readLine()) != null)
                            result.append(input).append('\n');

                        js.eval(result.toString());

                        js.eval("var fp = hack(__internal_fp)");

                    } catch (IOException ex) {
                        inputField.postError("Could not read debug script!");
                        inputField.postException(ex);
                    }

                } catch (ScriptException e1) {
                    e1.printStackTrace();
                }
                final int[] outputLineCount = {0};
                inputField.read("Console", true, false, x -> {
                    try {
                        Object o = js.eval(x);
                        inputField.postInput(">> " + x);
                        try (Scanner s = new Scanner(sw.toString())) {
                            for (int l = 0; s.hasNextLine(); ++l) {
                                if (l >= outputLineCount[0]) {
                                    ++outputLineCount[0];
                                    inputField.postLine(s.nextLine());
                                } else
                                    s.nextLine();
                            }
                        }
                        inputField.postOutput("<< " + Objects.toString(o, "undefined"));
                    } catch (Exception ex) {
                        inputField.postException(ex);
                    }
                }, FunctionPlotter.this);
                repaint();
            }
        });
        // Move coordinate system
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), "left");
        action.put("left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point((int) +(getWidth() * 0.1), 0), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "leftSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK), "leftSlow");
        action.put("leftSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(+1, 0), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "right");
        action.put("right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point((int) -(getWidth() * 0.1), 0), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "rightSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK), "rightSlow");
        action.put("rightSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(-1, 0), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "up");
        action.put("up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, (int) +(getHeight() * 0.1)), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "upSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_DOWN_MASK), "upSlow");
        action.put("upSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, +1), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0), "down");
        action.put("down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, (int) -(getHeight() * 0.1)), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "downSlow");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK), "downSlow");
        action.put("downSlow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, -1), false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, 0), "centerYAxis");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, 0), "centerYAxis");
        action.put("centerYAxis", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                center(false, true, false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0), "centerXAxis");
        action.put("centerXAxis", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                center(true, false, false);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, 0), "centerToOrigin");
        action.put("centerToOrigin", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                center(true, true, false);
            }
        });
        // Move grabbed function
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK), "leftFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.SHIFT_DOWN_MASK), "leftFunction");
        action.put("leftFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point((int) +(getWidth() * 0.1), 0), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "leftSlowFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "leftSlowFunction");
        action.put("leftSlowFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(+1, 0), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK), "rightFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.SHIFT_DOWN_MASK), "rightFunction");
        action.put("rightFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point((int) -(getWidth() * 0.1), 0), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "rightSlowFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "rightSlowFunction");
        action.put("rightSlowFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(-1, 0), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK), "upFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.SHIFT_DOWN_MASK), "upFunction");
        action.put("upFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, (int) +(getHeight() * 0.1)), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "upSlowFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "upSlowFunction");
        action.put("upSlowFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, +1), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK), "downFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.SHIFT_DOWN_MASK), "downFunction");
        action.put("downFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, (int) -(getHeight() * 0.1)), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "downSlowFunction");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK), "downSlowFunction");
        action.put("downSlowFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                move(new Point(), new Point(0, -1), true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "centerYAxis");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.SHIFT_DOWN_MASK), "centerYAxisFunction");
        action.put("centerYAxisFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                center(false, true, true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.SHIFT_DOWN_MASK), "centerXAxisFunction");
        action.put("centerXAxisFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                center(true, false, true);
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_DOWN_MASK), "centerToOriginFunction");
        action.put("centerToOriginFunction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                center(true, true, true);
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "grabFunctionSelect");
        action.put("grabFunctionSelect", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT || mode == Mode.MOVE) return;
                if (!functions.isEmpty()) {

                    if (functions.size() == 1) {
                        grabbedFunction = functions.get(0);
                        mode = Mode.MOVE;
                        return;
                    } else
                    if (mouse != null)
                        for (int i = functions.size()-1; i >= 0; --i)
                            if (functions.get(i).intersectsWith(mouse, o.mouseGrabRadius, FunctionPlotter.this)) {
                                grabbedFunction = functions.get(i);
                                mode = Mode.MOVE;
                                return;
                            }

                    inputField.read("Move function by name", false, true, (String f) -> {
                        f = f.trim().toLowerCase();
                        for (DrawableFunction df : functions)
                            if (df.getFunction().getName().equals(f)) {
                                grabbedFunction = df;
                                mode = Mode.MOVE;
                                return;
                            }
                        inputField.postError("No function with name '" + f + "' found!");
                    }, FunctionPlotter.this);
                    repaint();
                }
            }
        });
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.SHIFT_DOWN_MASK), "grabFunctionComplete");
        action.put("grabFunctionComplete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.INPUT || mode == Mode.MOVE) return;
                if (!functions.isEmpty()) {
                    String autoInput = null;

                    if (functions.size() == 1)
                        autoInput = functions.get(0).getFunction().getName().toLowerCase();
                    else
                        if (mouse != null)
                            for (int i = functions.size()-1; i >= 0; --i)
                                if (functions.get(i).intersectsWith(mouse, o.mouseGrabRadius, FunctionPlotter.this)) {
                                    autoInput = functions.get(i).getFunction().getName();
                                    break;
                                }

                    inputField.read("Move function by name", autoInput, false, true, (String f) -> {
                        f = f.trim().toLowerCase();
                        for (DrawableFunction df : functions)
                            if (df.getFunction().getName().equals(f)) {
                                grabbedFunction = df;
                                mode = Mode.MOVE;
                                return;
                            }
                        inputField.postError("No function with name '" + f + "' found!");
                    }, FunctionPlotter.this);
                    repaint();
                }
            }
        });

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitMode");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "exitMode");
        action.put("exitMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode == Mode.NORMAL)
                    return;
                else if (mode == Mode.INPUT)
                    inputField.cancel();
                mode = Mode.NORMAL;
                repaint();
            }
        });

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
                if (mode == Mode.MOVE && e.getButton() == MouseEvent.BUTTON1 &&
                        FunctionPlotter.this.grabbedFunction != null) {
                    FunctionPlotter.this.grabbedFunction = null;
                    FunctionPlotter.this.mode = Mode.NORMAL;
                }
                mouse = e.getPoint();
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (mode == Mode.MOVE && FunctionPlotter.this.grabbedFunction != null) {
                    FunctionPlotter.this.grabbedFunction.move(mouse, e.getPoint(), FunctionPlotter.this);
                    FunctionPlotter.this.compiler.updateConstants();
                }

                mouse = e.getPoint();
                if (FunctionPlotter.this.grabbedFunction != null || !info.isHidden())
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
        List<DrawableFunction> moved = functions.stream()
                .filter(DrawableFunction::isMoved)
                .collect(Collectors.toList());
        functions.clear();
        debug.updateFuncConstCount();
        compiler.getContext().getFunctions(true).forEach(f -> {
            if (f instanceof PureFunction) {
                final Color tmp;
                if (functionColors.containsKey(f.getName().toLowerCase()))
                    tmp = functionColors.get(f.getName().toLowerCase());
                else
                    tmp = new Color(this.o.functionColors[functionColors.size() % this.o.functionColors.length]);
                functionColors.put(f.getName().toLowerCase(), tmp);
                final DrawableFunction tmpDrawable = new DrawableFunction(tmp, (PureFunction) f);
                final int index = moved.indexOf(tmpDrawable);
                if (index >= 0) tmpDrawable.setOffset(moved.get(index).getXOffset(), moved.get(index).getYOffset());
                functions.add(tmpDrawable);
            }
        });
        repaint();
    }

    enum Mode {
        NORMAL, MOVE, INPUT;
    }

    private class DebugGUI extends DrawableComponent {

        private int functions;
        private int functionsUser;

        private int constants;
        private int constantsUser;

        DebugGUI(Color foreground, Color background, boolean hidden) {
            super(foreground, background, hidden);
            this.functions     = 0;
            this.functionsUser = 0;
            this.constants     = 0;
            this.constantsUser = 0;
        }

        @Override
        public void draw(Graphics gc, FunctionPlotter parent) {
            if (hidden)
                return;

            String[] info = {
                    "DEBUG INFO:",
                    "program_version        = " + PolyPlot.VERSION,
                    "mode                   = " + mode,
                    "span_base              = " + spanBase,
                    "span                   = " + span,
                    "span_x                 = " + spanX,
                    "span_y                 = " + spanY,
                    "x_corner               = " + xCorner,
                    "y_corner               = " + yCorner,
                    "value_per_x_pixel      = " + getValueXPerPixel(),
                    "value_per_y_pixel      = " + getValueYPerPixel(),
                    "function_render_method = " + DrawableFunction.DRAWING_METHOD,
                    "defined_functions      = " + functions + " (" + functionsUser + ")",
                    "defined_constants      = " + constants + " (" + constantsUser + ")",
                    "grabbed_function       = " + (grabbedFunction == null ? "" :
                            grabbedFunction.getFunction().getName() +
                            " (" + grabbedFunction.getXOffset() + ", " + grabbedFunction.getYOffset() + ")"),
                    "zoom                   = " + zoom,
                    "zoom_base              = " + zoomBase,
                    "power                  = " + pow,
                    "resolution             = " + getWidth()+"x"+getHeight(),
                    "theme                  = " + o.theme,
                    "render_time            = " + renderTime + "ns",
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

        void updateFuncConstCount() {
            CompilationContext context = compiler.getContext();

            functions     = context.getFunctions(false).size();
            functionsUser = context.getFunctions(true).size();
            constants     = context.getConstants(false).size();
            constantsUser = context.getConstants(true).size();
        }
    }
}
