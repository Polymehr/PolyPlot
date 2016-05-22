package polyplot;

import polyplot.graphics.PlotterController;

import javax.swing.*;

public class PolyPlot {

    public static final String VERSION = "1.0";

    public static void main(String[] args) {

        SwingUtilities.invokeLater(PlotterController::new);

    }
}
