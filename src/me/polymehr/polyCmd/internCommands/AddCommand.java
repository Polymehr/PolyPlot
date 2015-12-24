package me.polymehr.polyCmd.internCommands;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.polymehr.polyPlot.PolyPlot;
import me.polymehr.polyPlot.functions.Function;
import me.polymehr.polyPlot.functions.FunctionUtil;
import me.polymehr.polyPlot.graphics.plotter.FunctionPlotter;


/**
 * A command that adds a function with an optional specified color.
 */
public class AddCommand extends Command {
  
  public AddCommand() {
    super("addfunction", "Usage: 'addfunction <function term> [color]'", "Adds a function graph to the function plotter.", 
          "Use <funnction term> to add a graph to the plotter.\n"
        + " Note: expressions like '42x' are invalid. Use '42*x' instead!\n"
        + "The optional [color] of the function must be specfied in hexadecimal notation with optional leading '#' or '0x'.",
        new String[]{"addgraph", "add"});
  }

  @Override
  public boolean perform(String[] args) {
    if (args.length == 1 || args.length == 2) {
      FunctionPlotter fp = PolyPlot.getMainPlotterController().getFunctionPlotter();
      
      Function f;
      
      try {
        f = FunctionUtil.getFunctionByTerm(args[0]);
      } catch (IllegalArgumentException e) {
        System.err.println("Invalid function term! ("+args[0]+")");
        return true;
      }
      
      if (args.length != 2)
        try {
          fp.addFunction(f);
        } catch (UnsupportedOperationException e) {
          System.err.println(e.getMessage());
          return true;
        }
      else {
        int color = 0xFF0000;
        
        Matcher m = Pattern.compile("((#)|(0x))?([0-9a-f]{6})").matcher(args[1].toLowerCase());
        
        if (m.matches())
          try {
            color = Integer.parseInt(m.group(4), 16);
            fp.addFunction(f, new Color(color));
          } catch (NumberFormatException e) {
            System.err.println("Illegal hex format.");
            return true;
          } catch (UnsupportedOperationException e) {
            System.err.println(e.getMessage());
            return true;
          }
      }
      
      System.out.println("Added graph 'f(x)="+f.getFunctionTerm()+"' to the plotter.");
      System.out.println("Recognised function type: " + f.getName());
      
      fp.repaint();
      return true;
      
    }
    return false;
  }

}
