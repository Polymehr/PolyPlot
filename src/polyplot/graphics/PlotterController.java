package polyplot.graphics;


import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PlotterController extends JFrame {

    private static final long serialVersionUID = 6483735967730423588L;

    private FunctionPlotter plotter;

    public PlotterController() {
        super("PolyPlot");
        int px = 600;

        this.setSize(px, px);

        this.add(plotter = new FunctionPlotter());

        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.setLocationRelativeTo(null);

        this.setVisible(true);


        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                double rand = Math.random(); // A little easter egg...
                setTitle(rand <= 0.001 ? "Ame should really visit his Mom and not this window!"
                        : rand <= 0.1 ? "Dean's other other function plotter" : "PolyPlot");
            }
        });
    }

    /**
     * Returns the {@link FunctionPlotter} of this <code>PlotterController</code>.
     */
    public FunctionPlotter getFunctionPlotter() {
        return plotter;
    }

}
