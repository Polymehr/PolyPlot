package me.polymehr.polyPlot;

import me.polymehr.polyPlot.command.CommandInterface;
import me.polymehr.polyPlot.command.InputInterpreter;
import me.polymehr.polyPlot.command.UnixInputInterpreter;


public class PolyPlot  {
  
  public static final String VERSION = "0.6.0";
  
  private static PlotterController main;
  
  private static InputInterpreter in;
  
  public static void main(String[] args) {
    
    
    main = new PlotterController();
    
    in = new UnixInputInterpreter(new CommandInterface(), null, 0);
    
    in.start();

  }
  
  public static PlotterController getMainPlotterController() {
    return main;
  }

}
