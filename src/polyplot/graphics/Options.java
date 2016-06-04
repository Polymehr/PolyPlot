package polyplot.graphics;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import javax.xml.bind.annotation.*;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Options of the function plotter.<br>
 * Variables should be self-explaining.
 */
enum Options {

    INSTANCE;

    private Properties options;
    private Properties defaults;

    private static final String FILE_NAME = "options.pp";

    private static final String BG = "graphics.color.background";
    private static final String BG_DEFAULT = "#FFFFFFFF";
    Color backgroundColor;
    private static final String SCALE_COLOR = "graphics.scale.color";
    private static final String SCALE_COLOR_DEFAULT = "#000000FF";
    Color scaleColor;

    private static final String FKT_COLORS = "graphics.functions.colors";
    private static final String FKT_COLORS_DEFAULT = "[#FF0000, #00FF00, #0000FF, #FFC800, #00FFFF, #FF00FF]";
    Integer[] functionColors;

    private static final String BOX_FG = "graphics.info-box.foreground";
    private static final String BOX_FG_DEFAULT = "#FF";
    private static final String BOX_BG = "graphics.info-box.background";
    private static final String BOX_BG_DEFAULT = "#50";
    Color   infoBoxForeground;
    Color   infoBoxBackground;
    private static final String OV_FG = "graphics.function-overview.foreground";
    private static final String OV_FG_DEFAULT = "#FF";
    private static final String OV_BG = "graphics.function-overview.background";
    private static final String OV_BG_DEFAULT = "#A0";
    Color   functionOverviewForeground;
    Color   functionOverviewBackground;
    private static final String HELP_FG = "graphics.cheat-sheet.foreground";
    private static final String HELP_FG_DEFAULT = "#FF";
    private static final String HELP_BG = "graphics.cheat-sheet.background";
    private static final String HELP_BG_DEFAULT = "#B8";
    Color   cheatSheetForeground;
    Color   cheatSheetBackground;
    private static final String INPUT_FG = "graphics.input.foreground";
    private static final String INPUT_FG_DEFAULT = "#FF";
    private static final String INPUT_BG = "graphics.input.background";
    private static final String INPUT_BG_DEFAULT = "#7F";
    Color   inputFieldForeground;
    Color   inputFieldBackground;
    private static final String DEBUG_FG = "graphics.debug.foreground";
    private static final String DEBUG_FG_DEFAULT = "#E0";
    private static final String DEBUG_BG = "graphics.debug.background";
    private static final String DEBUG_BG_DEFAULT = "#E0";
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
    private static final String OV_SHOW_USER = "graphics.function-overview.only-show-user-defined";
    private static final String OV_SHOW_USER_DEFAULT = "true";
    boolean functionOverviewShowOnlyUserDefined;

    private static final String SCALE_STRETCH = "graphics.scale.stretch";
    private static final String SCALE_STRETCH_DEFAULT = "false";
    boolean scaleStretch;

    private static final String FKT_POINT = "graphics.functions.point-rendering";
    private static final String FKT_POINT_DEFAULT = "false";
    boolean functionsPointRendering;

    private static final String ZOOM_BASE = "graphics.zoom-base";
    private static final String ZOOM_BASE_DEFAULT = "1.05";
    double zoomBase;

    private static final String SPAN = "graphics.span";
    private static final String SPAN_DEFAULT = "20";
    double span;

    Options() {
        defaults = new Properties();
        options = new Properties(defaults);

        defaults.put(BG, BG_DEFAULT);

        defaults.put(SCALE_COLOR, SCALE_COLOR_DEFAULT);
        defaults.put(SCALE_STRETCH, SCALE_STRETCH_DEFAULT);

        defaults.put(FKT_COLORS, FKT_COLORS_DEFAULT);
        defaults.put(FKT_POINT, FKT_POINT_DEFAULT);

        defaults.put(BOX_BG, BOX_BG_DEFAULT);
        defaults.put(BOX_FG, BOX_FG_DEFAULT);
        defaults.put(BOX_DOCKED, BOX_DOCKED_DEFAULT);
        defaults.put(BOX_PIXELS, BOX_PIXELS_DEFAULT);
        defaults.put(BOX_RADIUS, BOX_RADIUS_DEFAULT);
        defaults.put(BOX_HIDE, BOX_HIDE_DEFAULT);

        defaults.put(OV_BG, OV_BG_DEFAULT);
        defaults.put(OV_FG, OV_FG_DEFAULT);
        defaults.put(OV_SHOW_HIDDEN, OV_SHOW_HIDDEN_DEFAULT);
        defaults.put(OV_SHOW_USER, OV_SHOW_USER_DEFAULT);
        defaults.put(OV_HIDE, OV_HIDE_DEFAULT);

        defaults.put(HELP_BG, HELP_BG_DEFAULT);
        defaults.put(HELP_FG, HELP_FG_DEFAULT);

        defaults.put(INPUT_BG, INPUT_BG_DEFAULT);
        defaults.put(INPUT_FG, INPUT_FG_DEFAULT);

        defaults.put(DEBUG_BG, DEBUG_BG_DEFAULT);
        defaults.put(DEBUG_FG, DEBUG_FG_DEFAULT);

        defaults.put(ZOOM_BASE, ZOOM_BASE_DEFAULT);
        defaults.put(SPAN, SPAN_DEFAULT);
    }

    private double getDoubleValue(String key, DoublePredicate isValid) {
        String value = options.getProperty(key).trim();
        double result;
        try {
            result = Double.parseDouble(value);
            if (isValid.test(result))
                return result;
            else
                return Double.parseDouble(defaults.getProperty(key));
            } catch (NumberFormatException | NullPointerException e) {
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

        Pattern colorPattern = Pattern.compile("#([0-9a-f]{1,8})");
        Matcher m = colorPattern.matcher(value);
        if (!m.matches()) {
            System.err.println("Settings: Illegal color format: " + value);
            m = colorPattern.matcher(value = defaults.getProperty(key).trim());
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

        Pattern colorPattern = Pattern.compile("#([0-9a-f]{1,8})");
        Matcher m = colorPattern.matcher(value);
        if (!m.matches()) {
            System.err.println("Settings: Illegal color format: " + value);
            m = colorPattern.matcher(value = defaults.getProperty(key).trim());
            if (!m.matches())
                throw new IllegalStateException("Illegal format of default value!: " + key);
        }
        final String hex = m.group(1);

        String colorValue;
        String context = (foreground ? SCALE_COLOR_DEFAULT : BG_DEFAULT).substring(1);

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

        Pattern expression = Pattern.compile("\\s*\\[\\s*(#[0-9a-f]{1,6}\\s*(?:\\s*,\\s*#[0-9a-f]{1,6}\\s*)*)]\\s*");
        Pattern colorPattern = Pattern.compile("#([0-9a-f]{1,6})");
        Matcher m = expression.matcher(value);
        if (!m.matches()) {
            value = defaults.getProperty(key);
            if ((m = expression.matcher(value)).matches())
                throw new IllegalStateException("Illegal format of default value!: " + key);
        }

        value = m.group(1); // Remove Brackets

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



    public void load() {
        try {
            options.load(Files.newBufferedReader(Paths.get(FILE_NAME)));
        } catch (NoSuchFileException e) {
            System.out.println("[INFO] No Options file (\"" + FILE_NAME +"\") found.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.zoomBase = getDoubleValue(ZOOM_BASE, d -> d == d && d != 0 &&
                d != Double.NEGATIVE_INFINITY && d != Double.POSITIVE_INFINITY);
        this.span = getDoubleValue(SPAN, d -> d == d && d > 0 && d != Double.POSITIVE_INFINITY);
        this.scaleStretch = getBoolValue(SCALE_STRETCH);

        this.infoBoxFunctionRadius = getIntValue(BOX_RADIUS, i -> i >= -1);
        this.infoBoxDocked = getBoolValue(BOX_DOCKED);
        this.infoBoxShowPixels = getBoolValue(BOX_PIXELS);
        this.infoBoxHidden = getBoolValue(BOX_HIDE);

        this.functionOverviewShowHidden = getBoolValue(OV_SHOW_HIDDEN);
        this.functionOverviewShowOnlyUserDefined = getBoolValue(OV_SHOW_USER);
        this.functionOverviewHidden = getBoolValue(OV_HIDE);

        this.functionColors = getColorArray(FKT_COLORS);
        this.functionsPointRendering = getBoolValue(FKT_POINT);

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
    }

}
