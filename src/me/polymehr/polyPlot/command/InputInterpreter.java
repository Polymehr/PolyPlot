package me.polymehr.polyPlot.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public abstract class InputInterpreter extends Thread implements AutoCloseable {
  
  protected enum Function {
    AUTOCOMPLETE, HISTORY_UP, HISTORY_DOWN, 
    CARET_LEFT, CARET_RIGHT, CARET_START, CARET_END,
    CARET_WORD_NEXT, CARET_WORD_PREV,
    DELETE_FRONT, DELETE_BACK,
    DELETE_WORD_NEXT, DELETE_WORD_PREV,
    EXIT, CLEAR, SEND,
    CHAR_INPUT, NO_OPERATION;
  }
  
  protected CommandInterface ci;
  
  protected String PS1 = ">>";
  
  protected BufferedReader inputStream;
  
  protected List<String> history;
  protected int historyPointer;
  
  protected StringBuffer intput;
  protected int lastChar = 20;
  protected int inputPointer = 0;
  
  public InputInterpreter(CommandInterface ci, Path historyLocation, int preLoadedHistoryLines) {
    super("Input");
    this.setDaemon(true);
    this.ci = Objects.requireNonNull(ci, "Command Interface cannot be 'null'!");
  }
  
  abstract boolean init();
  
  /**
   * Sets the leading character String for the prompt.<br>
   * E.g.: <code>">> foo bar1 bar2"</code>
   */
  public void set$PS1(String ps1) {
    if (ps1 != null)
      PS1 = ps1;
  }
  
  
  abstract Function readFunction(BufferedReader input) throws IOException;
  
  abstract boolean doFunction(Function f);
  
  abstract void setLine(String line) throws IOException;
  
  @Override
  public void run() {
    init();
    try (BufferedReader bf = new BufferedReader(new InputStreamReader(System.in))) {
      inputStream = bf;
      intput = new StringBuffer();
      setLine("");
      while (true) {
        Function o = this.readFunction(bf);

        if (o == Function.EXIT) {
          close();
          setLine("\n");
          System.out.println("Exiting...");
          break;
        } else {
          setLine(o.toString());
        }
      }
      
    } catch (IOException e) {
      e.printStackTrace();
      try {
        close();
      } catch (Exception e1) {
        System.err.print("Could not exit properly!");
        e1.printStackTrace();
      }
    } catch (Exception e) {
      try {
        close();
      } catch (Exception e1) {
        System.err.println("Could not exit properly!");
        e1.printStackTrace();
      }
    }
  }

}
