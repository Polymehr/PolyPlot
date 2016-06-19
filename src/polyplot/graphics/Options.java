package polyplot.graphics;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Options of the function plotter.<br>
 * Variables should be self-explaining.
 */
public enum Options {

    INSTANCE;

    private Properties options;
    private Properties defaults;

    private static final String FILE_NAME = "options.conf";

    private static final String BG = "graphics.color.background";
    private static final String BG_DEFAULT = "FFFFFFFF";
    Color backgroundColor;
    private static final String SCALE_COLOR = "graphics.scale.color";
    private static final String SCALE_COLOR_DEFAULT = "000000FF";
    Color scaleColor;

    private static final String FUNCTION_COLORS = "graphics.functions.colors";
    private static final String FUNCTION_COLORS_DEFAULT = "[FF0000, 00FF00, 0000FF, FFC800, 00FFFF, FF00FF]";
    Integer[] functionColors;

    private static final String BOX_FG = "graphics.info-box.foreground";
    private static final String BOX_FG_DEFAULT = "FF";
    private static final String BOX_BG = "graphics.info-box.background";
    private static final String BOX_BG_DEFAULT = "50";
    Color   infoBoxForeground;
    Color   infoBoxBackground;
    private static final String OV_FG = "graphics.function-overview.foreground";
    private static final String OV_FG_DEFAULT = "FF";
    private static final String OV_BG = "graphics.function-overview.background";
    private static final String OV_BG_DEFAULT = "A0";
    Color   functionOverviewForeground;
    Color   functionOverviewBackground;
    private static final String HELP_FG = "graphics.cheat-sheet.foreground";
    private static final String HELP_FG_DEFAULT = "FF";
    private static final String HELP_BG = "graphics.cheat-sheet.background";
    private static final String HELP_BG_DEFAULT = "B8";
    Color   cheatSheetForeground;
    Color   cheatSheetBackground;
    private static final String INPUT_FG = "graphics.input.foreground";
    private static final String INPUT_FG_DEFAULT = "FF";
    private static final String INPUT_BG = "graphics.input.background";
    private static final String INPUT_BG_DEFAULT = "7F";
    private static final String INPUT_OUT_DEF = "graphics.input.output-color.default";
    private static final String INPUT_OUT_DEF_DEFAULT = "FF";
    private static final String INPUT_OUT_IN = "graphics.input.output-color.input";
    private static final String INPUT_OUT_IN_DEFAULT = "3F3F3F";
    private static final String INPUT_OUT_OUT = "graphics.input.output-color.output";
    private static final String INPUT_OUT_OUT_DEFAULT = "4F4F4F";
    private static final String INPUT_OUT_ERR = "graphics.input.output-color.error";
    private static final String INPUT_OUT_ERR_DEFAULT = "990000";
    Color   inputFieldForeground;
    Color   inputFieldBackground;
    Color   inputFieldOutputDefault;
    Color   inputFieldOutputInput;
    Color   inputFieldOutputOutput;
    Color   inputFieldOutputError;
    private static final String DEBUG_FG = "graphics.debug.foreground";
    private static final String DEBUG_FG_DEFAULT = "E0";
    private static final String DEBUG_BG = "graphics.debug.background";
    private static final String DEBUG_BG_DEFAULT = "E0";
    Color   debugForeground;
    Color   debugBackground;

    private static final String BOX_HIDE = "graphics.info-box.hide";
    private static final String BOX_HIDE_DEFAULT = "true";
    boolean infoBoxHidden;
    private static final String BOX_RADIUS = "graphics.info-box.function-radius";
    private static final String BOX_RADIUS_DEFAULT = "20";
    int     infoBoxFunctionRadius;
    private static final String BOX_PIXELS = "graphics.info-box.show-pixels";
    private static final String BOX_PIXELS_DEFAULT = "false";
    boolean infoBoxShowPixels;
    private static final String BOX_DOCKED = "graphics.info-box.docked";
    private static final String BOX_DOCKED_DEFAULT = "true";
    boolean infoBoxDocked;
    private static final String OV_HIDE = "graphics.function-overview.hide";
    private static final String OV_HIDE_DEFAULT = "true";
    boolean functionOverviewHidden;
    private static final String OV_SHOW_HIDDEN = "graphics.function-overview.show-hidden";
    private static final String OV_SHOW_HIDDEN_DEFAULT = "false";
    boolean functionOverviewShowHidden;

    private static final String SCALE_STRETCH = "graphics.scale.stretch";
    private static final String SCALE_STRETCH_DEFAULT = "false";
    boolean scaleStretch;

    private static final String FUNCTION_RENDERING = "graphics.functions.rendering-method";
    private static final String FUNCTION_RENDERING_DEFAULT = "LINES";
    DrawableFunction.DrawingMethod functionsPointRendering;

    private static final String ZOOM_BASE = "graphics.zoom-base";
    private static final String ZOOM_BASE_DEFAULT = "1.05";
    double zoomBase;

    private static final String SPAN = "graphics.span";
    private static final String SPAN_DEFAULT = "20";
    double span;

    private static final String THEME = "graphics.theme";
    private static final String THEME_DEFAULT = "default";
    String theme;

    Options() {
        defaults = new Properties();
        options = new Properties(defaults);

        defaults.put(BG, BG_DEFAULT);

        defaults.put(SCALE_COLOR, SCALE_COLOR_DEFAULT);
        defaults.put(SCALE_STRETCH, SCALE_STRETCH_DEFAULT);

        defaults.put(FUNCTION_COLORS, FUNCTION_COLORS_DEFAULT);
        defaults.put(FUNCTION_RENDERING, FUNCTION_RENDERING_DEFAULT);

        defaults.put(BOX_BG, BOX_BG_DEFAULT);
        defaults.put(BOX_FG, BOX_FG_DEFAULT);
        defaults.put(BOX_DOCKED, BOX_DOCKED_DEFAULT);
        defaults.put(BOX_PIXELS, BOX_PIXELS_DEFAULT);
        defaults.put(BOX_RADIUS, BOX_RADIUS_DEFAULT);
        defaults.put(BOX_HIDE, BOX_HIDE_DEFAULT);

        defaults.put(OV_BG, OV_BG_DEFAULT);
        defaults.put(OV_FG, OV_FG_DEFAULT);
        defaults.put(OV_SHOW_HIDDEN, OV_SHOW_HIDDEN_DEFAULT);
        defaults.put(OV_HIDE, OV_HIDE_DEFAULT);

        defaults.put(HELP_BG, HELP_BG_DEFAULT);
        defaults.put(HELP_FG, HELP_FG_DEFAULT);

        defaults.put(INPUT_BG, INPUT_BG_DEFAULT);
        defaults.put(INPUT_FG, INPUT_FG_DEFAULT);
        defaults.put(INPUT_OUT_DEF, INPUT_OUT_DEF_DEFAULT);
        defaults.put(INPUT_OUT_IN, INPUT_OUT_IN_DEFAULT);
        defaults.put(INPUT_OUT_OUT, INPUT_OUT_OUT_DEFAULT);
        defaults.put(INPUT_OUT_ERR, INPUT_OUT_ERR_DEFAULT);

        defaults.put(DEBUG_BG, DEBUG_BG_DEFAULT);
        defaults.put(DEBUG_FG, DEBUG_FG_DEFAULT);

        defaults.put(ZOOM_BASE, ZOOM_BASE_DEFAULT);
        defaults.put(SPAN, SPAN_DEFAULT);

        defaults.put(THEME, THEME_DEFAULT);
    }

    private double getDoubleValue(String key, DoublePredicate isValid) {
        String value = options.getProperty(key).trim();
        double result;
        try {
            result = Double.parseDouble(value);
            if (isValid.test(result))
                return result;
            else {
                System.err.println("[ERROR/Options]: Decimal number is invalid (" + key + "): " + value);
                return Double.parseDouble(defaults.getProperty(key));
            }
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("[ERROR/Options]: Illegal decimal number format (" + key + "): " + value);
            return Double.NaN;
        }

    }

    private int getIntValue(String key, IntPredicate isValid) {
        String value = options.getProperty(key).trim();
        int result;
        try {
            result = Integer.parseInt(value);
            if (isValid.test(result))
                return result;
            else
                return Integer.parseInt(defaults.getProperty(key));
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }

    }

    private boolean getBoolValue(String key) {
        if (!key.trim().isEmpty())
            return Boolean.parseBoolean(options.getProperty(key).trim());
        else
            return Boolean.parseBoolean(defaults.getProperty(key).trim());
    }

    private Color getColor(String key, boolean alphaAllowed) {
        String value = options.getProperty(key).trim().toLowerCase();

        Pattern colorPattern = Pattern.compile("#?([0-9a-f]{1,8})");
        Matcher m = colorPattern.matcher(value);
        if (!m.matches()) {
            System.err.println("[ERROR/Options]: Illegal color format (" + key + "): " + value);
            value  = defaults.getProperty(key).trim().toLowerCase();
            m = colorPattern.matcher(value);
            if (!m.matches())
                throw new IllegalStateException("Illegal format of default value!: " + key);
        }
        final String hex = m.group(1);

        int rgb;

        if (hex.length() <= 6)
            rgb = Integer.parseInt(hex, 16);
        else
            rgb = Integer.parseInt(hex.substring(0, 6), 16);

        if (!alphaAllowed || hex.length() <= 6)
            return new Color(rgb);
        else {
            int alpha = Integer.parseInt(hex.substring(6), 16) & 0xFF;
            return new Color(alpha << 24 | rgb, true);
        }
    }

    private Color getFromContext(String key, boolean foreground) {
        String value = options.getProperty(key).trim().toLowerCase();

        Pattern colorPattern = Pattern.compile("#?([0-9a-f]{1,8})");
        Matcher m = colorPattern.matcher(value);
        if (!m.matches()) {
            System.err.println("[ERROR/Options]: Illegal color format (" + key + "): " + value);
            value = defaults.getProperty(key).trim().toLowerCase();
            m = colorPattern.matcher(value);
            if (!m.matches())
                throw new IllegalStateException("Illegal format of default value!: " + key);
        }
        final String hex = m.group(1);

        String colorValue;
        String context = String.format("%06x%02x", (foreground ? scaleColor : backgroundColor).getRGB() & 0xFFFFFF,
                                                   (foreground ? scaleColor : backgroundColor).getAlpha());

        if (hex.length() == 0)
            colorValue = "FF" + context.substring(0, 6);
        else if (hex.length() <= 2)
            colorValue = hex + context.substring(0, 6);
        else if (hex.length() <= 6)
            colorValue = "FF" + hex;
        else
            colorValue = hex.substring(6) + hex.substring(0, 6);

        return new Color(Integer.parseUnsignedInt(colorValue, 16), true);

    }

    private Integer[] getColorArray(String key) {
        String value = options.getProperty(key).trim().toLowerCase();

        Pattern expression = Pattern.compile("\\s*\\[\\s*(#?[0-9a-f]{1,6}\\s*(?:\\s*,\\s*#?[0-9a-f]{1,6}\\s*)*)]\\s*");
        Pattern colorPattern = Pattern.compile("#?([0-9a-f]{1,6})");
        Matcher m = expression.matcher(value);
        if (!m.matches()) {
            System.err.println("[ERROR/Options]: Illegal color list format (" + key + "): " + value);
            value = defaults.getProperty(key).trim().toLowerCase();
            m = colorPattern.matcher(value);
            if (m.matches())
                throw new IllegalStateException("Illegal format of default value!: " + key);
        }

        m = colorPattern.matcher(value);
        List<Integer> result = new ArrayList<>(6);


        while (m.find()) {
            try {
                result.add(Integer.parseInt(m.group(1), 16));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                result.add(0xFF0000);
            }
        }

        return result.toArray(new Integer[result.size()]);

    }

    private Path getTheme() {
        theme = options.getProperty(THEME);
        String extension = ".theme";
        if (theme.isEmpty())
            theme = defaults.getProperty(THEME_DEFAULT);
        else if (theme.lastIndexOf('.') != -1) {
            int index = theme.lastIndexOf('.');
            extension = theme.substring(index);
            theme = theme.substring(0, index);
        }

        try {
            Path file = Paths.get("themes/" + theme + extension);
            if (Files.exists(file))
                return file;
            else {
                System.err.println("[ERROR/Options]: No theme with name '" + theme + "' found (at '" + file + "')");
                theme = "<internal>";
                return null;
            }
        } catch (InvalidPathException e) {
            System.err.println("[ERROR/Options]: Invalid theme path: " + e.getMessage());
            theme = defaults.getProperty(THEME_DEFAULT);
            Path file = Paths.get("themes/" + theme + ".theme");
            if (Files.exists(file))
                return file;
            else {
                theme = "<internal>";
                return null;
            }
        }
    }

    /**
     * Loads the options from the option file.
     */
    public void load() {
        load(Paths.get(FILE_NAME), false);
    }

    /**
     * Sets an options. The correctness of the
     * value will not be checked.
     *
     * @param key
     *      the key of the option.
     * @param value
     *      the value of the option.
     */
    public void set(String key, String value) {
        if (defaults.containsKey(key))
            options.setProperty(key, value);
        else
            System.err.println("[ERROR/Options]: The specified key (" + key + ") does not exist!");
    }

    /**
     * Reloads the internal fields of the options.
     */
    public void reload() {
        load(null, false);
    }

    private void load(Path file, boolean theme) {
        if (file != null)
            try {
                options.load(Files.newBufferedReader(file));
            } catch (NoSuchFileException e) {
                System.out.println("[INFO/Options]: No " + (theme ? "Theme" : "Options") + " file (\"" + file +"\") found" +
                        ".");
                if (theme) return;
            } catch (IOException e) {
                e.printStackTrace();
                if (theme) return;
            }

        if (!theme) {

            Path themeFile = getTheme();
            if (themeFile != null && file != null)
                load(themeFile, true);

            this.zoomBase = getDoubleValue(ZOOM_BASE, d -> d == d && d > 0 && d != Double.POSITIVE_INFINITY);
            this.span = getDoubleValue(SPAN, d -> d == d && d > 0 && d != Double.POSITIVE_INFINITY);
            this.scaleStretch = getBoolValue(SCALE_STRETCH);

            this.infoBoxFunctionRadius = getIntValue(BOX_RADIUS, i -> i >= -1);
            this.infoBoxDocked = getBoolValue(BOX_DOCKED);
            this.infoBoxShowPixels = getBoolValue(BOX_PIXELS);
            this.infoBoxHidden = getBoolValue(BOX_HIDE);

            this.functionOverviewShowHidden = getBoolValue(OV_SHOW_HIDDEN);
            this.functionOverviewHidden = getBoolValue(OV_HIDE);

            try {
                this.functionsPointRendering = DrawableFunction.DrawingMethod.valueOf(
                        this.options.getProperty(FUNCTION_RENDERING));
            } catch (IllegalArgumentException e) {
                this.functionsPointRendering = DrawableFunction.DrawingMethod.valueOf(FUNCTION_RENDERING_DEFAULT);
            }

        } else {
            // Prevent the loading of non-theme options set in theme files on reload.
            // I know, it's cheaty.
            options.put(SCALE_STRETCH, SCALE_STRETCH_DEFAULT);
            options.put(FUNCTION_RENDERING, FUNCTION_RENDERING_DEFAULT);
            options.put(BOX_DOCKED, BOX_DOCKED_DEFAULT);
            options.put(BOX_PIXELS, BOX_PIXELS_DEFAULT);
            options.put(BOX_RADIUS, BOX_RADIUS_DEFAULT);
            options.put(BOX_HIDE, BOX_HIDE_DEFAULT);
            options.put(OV_SHOW_HIDDEN, OV_SHOW_HIDDEN_DEFAULT);
            options.put(OV_HIDE, OV_HIDE_DEFAULT);
            options.put(ZOOM_BASE, ZOOM_BASE_DEFAULT);
            options.put(SPAN, SPAN_DEFAULT);
            options.put(THEME, THEME_DEFAULT);
        }

        this.functionColors = getColorArray(FUNCTION_COLORS);

        this.backgroundColor = getColor(BG, false);
        this.scaleColor = getColor(SCALE_COLOR, true);

        this.infoBoxForeground = getFromContext(BOX_FG, true);
        this.infoBoxBackground = getFromContext(BOX_BG, false);

        this.functionOverviewForeground = getFromContext(OV_FG, true);
        this.functionOverviewBackground = getFromContext(OV_BG, false);

        this.cheatSheetForeground = getFromContext(HELP_FG, true);
        this.cheatSheetBackground = getFromContext(HELP_BG, false);

        this.debugForeground = getFromContext(DEBUG_FG, true);
        this.debugBackground = getFromContext(DEBUG_BG, false);

        this.inputFieldForeground = getFromContext(INPUT_FG, true);
        this.inputFieldBackground = getFromContext(INPUT_BG, false);
        this.inputFieldOutputDefault = getFromContext(INPUT_OUT_DEF, true);
        this.inputFieldOutputInput = getFromContext(INPUT_OUT_IN, true);
        this.inputFieldOutputOutput = getFromContext(INPUT_OUT_OUT, true);
        this.inputFieldOutputError = getFromContext(INPUT_OUT_ERR, true);
    }

}
