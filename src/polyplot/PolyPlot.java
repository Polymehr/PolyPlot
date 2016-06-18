package polyplot;

import polyplot.graphics.PlotterController;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolyPlot {

    public static final String VERSION = "1.0";

    private static int pxX = 800, pxY = 800;

    public static void main(String[] args) {

        processArgs(args);

        SwingUtilities.invokeLater(()-> new PlotterController(pxX, pxY));

    }

    private static void processArgs(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.startsWith("-s") || arg.equals("--size")) {
                String dimensions;
                if (arg.startsWith("-s") && arg.length() > 2) // must directly after
                    dimensions = arg.substring(2);
                else if (i + 1 >= args.length ||
                        args[i + 1].startsWith("-") || (i + 2 < args.length && args[i + 2].startsWith("-"))) {
                    System.err.println("Expected one argument.");
                    System.exit(1);
                    return;
                } else
                    dimensions = args[++i];

                Matcher m = Pattern.compile("(\\d+):(\\d+)").matcher(dimensions);
                if (m.matches()) {
                    pxX = Integer.parseInt(m.group(1));
                    pxY = Integer.parseInt(m.group(2));
                } else {
                    System.err.println("Malformed dimensions. Expected X:Y Found: " + dimensions);
                }
            } else if (arg.startsWith("-h") || arg.equals("--help")) {
                System.out.println("Help:");
                System.out.println("   -s, --size X:Y set the size of the window.");
                System.out.println("                  X and Y must be positive");
                System.out.println("   -h, --help     display help and exit");
                System.exit(0);
            } else if (arg.startsWith("-")) {
                System.err.println("Unknown option: " + arg);
            } else
                System.err.println("Expected switch: " + arg);
        }
    }
}
