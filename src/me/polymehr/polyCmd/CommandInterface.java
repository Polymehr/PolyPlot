package me.polymehr.polyCmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import me.polymehr.polyCmd.internCommands.AddCommand;
import me.polymehr.polyCmd.internCommands.Command;
import me.polymehr.polyCmd.internCommands.HelpCommand;
import me.polymehr.polyCmd.internCommands.RemoveCommand;

/**
 * An class that provides basic command and command line parsing
 * functionality.<br>
 * This class runs as its own thread and reads from the standard
 * input. 
 */
public class CommandInterface extends Thread {

  /** A List of the registered commands. */
  private List<Command> cmds = new ArrayList<Command>();

  public CommandInterface() {
    this(new ArrayList<Command>(0));
    registerDefaults();
  }
  
  public CommandInterface(List<Command> commands) {
    super("Input");
    this.setDaemon(true);
    for (Command c : commands)
      registerCommand(c);
  }

  /**
   * Converts an given Message to a Command and tries to perform it.<br>
   * If the given Command does not exists <code>"Unknown Command."</code> will be printed to the
   * user.
   * 
   * @param command the Command that will be performed.<br>
   *    If <code>null</code> or completely white-space, nothing will be done.
   */
  public boolean runCommand(String command) {
    
    if (command == null)
      return false;
    if (command.trim().isEmpty())
      return false;
    
    if (command.charAt(0) == '/')
      command = command.substring(1);
    
    final String input = command;

    String label;
    
    if (command.indexOf(' ') != -1)
      label = command.substring(0, command.indexOf(' ')).toLowerCase();
    else
      label = command.toLowerCase();
    
    Command target = null;

    for (Command cmd : cmds)
      if (cmd.getName().equalsIgnoreCase(label))
        target = cmd;
      else
        for (String alias : cmd.getAliases())
          if (alias.equals(label))
            target = cmd;
      

    if (target == null) {
      System.err.println("Unknown Command: \""+label+"\"");
      return false;
    }
    
    boolean firstArgEmpty = command.length()==label.length()+1;
    
    if (command.indexOf(' ') == -1 || firstArgEmpty)
      command = "";
    else
      command = command.substring(command.indexOf(' ')+1);
    
    ArrayList<String> args = new ArrayList<String>();
    
    StringBuffer arg = new StringBuffer();
    
    boolean nextEscaped   = false;
    boolean singleEscaped = false;
    boolean doubleEscaped = false;
    int     lastEscaped   = 0;
    
    int     i;
    for (i = 0; i < command.length(); ++i) {
      char ch = command.charAt(i);
      switch (ch) {
        case '\\':
          if (nextEscaped) {
            arg.append(ch);
            nextEscaped = false;
          } else
            nextEscaped = true;
          break;
        case ' ':
          if (singleEscaped || doubleEscaped || nextEscaped) {
            arg.append(ch);
            if (nextEscaped)
              nextEscaped = false;
          } else {
            args.add(arg.toString());
            arg.delete(0, arg.length());
          }
          break;
        case '\'':
          if (doubleEscaped || nextEscaped) {
            arg.append(ch);
            if (nextEscaped)
              nextEscaped = false;
          } else {
            singleEscaped = !singleEscaped;
            lastEscaped = i;
          }
          break;
        case '"':
          if (singleEscaped || nextEscaped) {
            arg.append(ch);
            if (nextEscaped)
              nextEscaped = false;
          } else {
            doubleEscaped = !doubleEscaped;
            lastEscaped = i;
          } 
          break;
        default:
          if (nextEscaped) {
            StringBuffer pointer = new StringBuffer(i+1);
            int k = label.length()+i+1;
            for (int j = 0; j < k; ++j)
              pointer.append(' ');
            pointer.append('^');
              
            System.err.println("Illegal '\\'-escape at index "+k+". If you want to write '\\' as literal use '\\\\' instead!");
            System.err.println(input);
            System.err.println(pointer);
            return false;
          } else
            arg.append(ch);
      }
    }
    if (doubleEscaped || singleEscaped || nextEscaped) {
      StringBuffer pointer = new StringBuffer(i+1);
      int k = label.length()+(nextEscaped?i:lastEscaped+1);
      for (int j = 0; j < k; ++j)
        pointer.append(' ');
      pointer.append('^');
      System.err.println("Unclosed '"+(nextEscaped?"\\' escape":((doubleEscaped?'"':"'")+"' quotation"))+" at index "+k+"!");
      System.err.println(input);
      System.err.println(pointer);
      return false;
    } 
    else if (!command.isEmpty() || firstArgEmpty)
      args.add(arg.toString());
    
    for (String s : args)
      System.out.println("\""+s+"\"");
    
    boolean success = target.perform(args.toArray(new String[args.size()]));

    if (!success)
      System.err.println(target.getUsage());

    return success;
  }

  /**
   * Adds a Command to the existing ones.<br>
   * The name and aliases of the Command have to be unique.
   * 
   * @param c The Command to be added.
   */
  public void registerCommand(Command c) {

    for (Command cmd : cmds)
      if (cmd.getName().equalsIgnoreCase(c.getName()))
        throw new IllegalArgumentException(
                "Cannot add Command '" + c.getName() + "' (name conflict with '"
                    + cmd.getClass().getSimpleName() + "').");
      else
        for (String alias : cmd.getAliases())
          for (String alias2 : c.getAliases())
            if (alias.equals(alias2))
              throw new IllegalArgumentException(
                      "Cannot add Command '" + c.getName() + "' (alias name conflict with '"
                          + cmd.getClass().getSimpleName() + "').");
              

    cmds.add(c);
  }
  
  /**
   * Registers the default commands.
   */
  private void registerDefaults() {
    registerCommand(new HelpCommand(this));
    registerCommand(new AddCommand());
    registerCommand(new RemoveCommand());
  }
  
  /**
   * Gets all a copy of all registered commands.
   */
  public List<Command> getRegisteredCommands() {
    return new ArrayList<Command>(cmds);
  }
  
//  @Override
//  public void run() {
//    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
////    System.out.flush();
//    System.out.print("> ");
//    while (true) {
//      try {
//        while (true) {
////          System.err.flush();
////          System.out.flush();
//          runCommand(in.readLine());
////          System.out.flush();
//          System.out.print("> ");
//        } 
//
//      } catch (Exception e) {
//        System.err.flush();
//        e.printStackTrace();
////        System.out.flush();
//        System.out.print("> ");
//      }
//    }
//
//  }
}
