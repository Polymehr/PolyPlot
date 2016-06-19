package polyplot;

import polyplot.graphics.Options;
import polyplot.graphics.PlotterController;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolyPlot {

    public static final String VERSION = "1.0";

    private static int pxX = 800, pxY = 800;

    public static void main(String[] args) {

        Options.INSTANCE.load();

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
            }  else if (arg.startsWith("-o") || arg.equals("--options")) {
                if (arg.startsWith("-o") && arg.length() > 2)
                    System.err.println("Trailing characters after '-a' aren't supported.");

                List<String> valuePairs = new LinkedList<>();
                for (int j = i+1; j < args.length && !args[j].startsWith("-"); ++j, ++i)
                    valuePairs.add(args[j]);

                if (valuePairs.isEmpty())
                    System.err.println("Expected at least one value pair!");
                else {
                    Options o = Options.INSTANCE;
                    for (String valPair : valuePairs) {
                        String[] split = valPair.split("=");
                        if (split.length != 2)
                            System.err.println("Expected a key value pair in the format KEY=VALUE. Found '" + valPair + "'");
                        else
                            o.set(split[0], split[1]);
                    }
                    o.reload();
                }

            } else if (arg.startsWith("-h") || arg.equals("--help")) {
                System.out.println("Help:");
                System.out.println("   -s, --size    X:Y         Set the size of the window. X and Y must be positive.");
                System.out.println("   -o, --options [OPTION]... Set an option. Options in KEY=VALUE format expected.");
                System.out.println("                              Set options will overwrite ones set in the options ");
                System.out.println("                              file.");
                System.out.println("   -h, --help                Display help and exit.");
                System.exit(0);
            } else if (arg.startsWith("-")) {
                System.err.println("Unknown option: " + arg);
            } else
                System.err.println("Expected switch: " + arg);
        }
    }
}
