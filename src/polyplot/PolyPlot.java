package polyplot;

import polyplot.graphics.PlotterController;

public class PolyPlot  {
  
  public static final String VERSION = "0.7.2";
  
  private static PlotterController main;
  
  // private static InputInterpreter in;
  
  public static void main(String[] args) {
    
    
    main = new PlotterController();
    
    // in = new LinuxInputInterpreter(new CommandInterface(), null, 0);
    
    // in.start();

  }
  
  public static PlotterController getMainPlotterController() {
    return main;
  }

}
