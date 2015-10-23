package me.polymehr.polyPlot.command.internCommands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.polymehr.polyPlot.command.Input;

/**
 * A command that provides help.
 */
public class HelpCommand extends Command {
  
  Input cm;
  
  public HelpCommand(Input cm) {
    super("help", "Usage: 'help [command]'", "Provides help for all commands or a specific one.", 
        "Can be used to display  help for all commands or a specific one.",
        new String[]{"h","?"});
    this.cm = cm;
  }

  @Override
  public boolean perform(String[] args) {
    List<Command> cmds = cm.getRegisteredCommands();
    
    Collections.sort(cmds);
    
    if (args.length == 0) {
      for (Command c : cmds)
        printHelp(c);
      return true;
    } else if (args.length == 1) {
      for (Command c : cmds)
        if (c.getName().equalsIgnoreCase(args[0])) {
          printHelp(c);
          return true;
        } else {
          for (String alias : c.aliases)
            if (alias.equalsIgnoreCase(args[0])) {
              printHelp(c);
              return true;
            }
        }
      System.err.println("Command '"+args[0]+"' not found.");
      return true;
    }
    
    return false;
  }
  
  /**
   * Prints the help for a specified command.
   * @param c a command.
   */
  private void printHelp(Command c) {
    System.out.println(c.getName() + " (" + c.usage +  ")" + (c.aliases.length != 0 ? "  " + Arrays.toString(c.aliases) : "") + "\n");
    formatString(c.description, "Description");
    formatString(c.help, "Help");
    System.out.println("\n");
     
  }
  
  private void formatString(String s, String type) {
    if (s.contains("\n")) {
      int idx = 0;
      while (s.indexOf('\n', idx) != -1) {
        System.out.println("  " + (idx == 0 ? type + ":\n   " : " ") + s.substring(idx, s.indexOf('\n', idx)));

        idx = s.indexOf('\n', idx)+1;
        if (idx >= s.length())
          return;
      }
      System.out.println("   " + s.substring(idx));
    } else
      System.out.println("  " + type + ":\n   " + s);
  }

}
