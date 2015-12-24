package me.polymehr.polyCmd;

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
  
  protected StringBuffer line;
  protected int lastChar = 20;
  protected int caret = 0;
  
  public InputInterpreter(CommandInterface ci, Path historyLocation, int preLoadedHistoryLines) {
    super("Input");
    this.setDaemon(true);
    this.ci = Objects.requireNonNull(ci, "Command Interface cannot be 'null'!");
  }
  
  protected abstract boolean init();
  
  /**
   * Sets the leading character String for the prompt.<br>
   * E.g.: <code>">> foo bar1 bar2"</code>
   */
  public void set$PS1(String ps1) {
    if (ps1 != null)
      PS1 = ps1;
  }
  
  
  protected abstract Function readFunction(BufferedReader input) throws IOException;
  
  protected boolean doFunction(Function f) throws Exception {
    switch (f) {
      case CARET_LEFT:
        updateLine();
        return setCaret(caret-1);
      case CARET_RIGHT:
        updateLine();
        return setCaret(caret+1);
      case AUTOCOMPLETE:
        break;
      case CARET_END:
        break;
      case CARET_START:
        break;
      case CARET_WORD_NEXT:
        break;
      case CARET_WORD_PREV:
        break;
      case CHAR_INPUT:
        String s = new String(Character.toChars(lastChar));
        this.line.insert(caret, s);
        updateLine();
        return setCaret(caret+1);
      case CLEAR:
        break;
      case DELETE_BACK:
        break;
      case DELETE_FRONT:
        break;
      case DELETE_WORD_NEXT:
        break;
      case DELETE_WORD_PREV:
        break;
      case EXIT:
        System.exit(0);
        return true;
      case HISTORY_DOWN:
        break;
      case HISTORY_UP:
        break;
      case NO_OPERATION:
        break;
      case SEND:
        break;
      default:
        break;
    }
    
    
    updateLine("");
    
    return false;
  }
  
  public void setLine(String line) {
    if (line != null)
      this.line = new StringBuffer(line);
  }
  
  protected void updateLine(String line, int caretPosition) {
   setLine(line);
   updateLine();
   setCaret(caretPosition);
  }
  
  private void updateLine(String line) {
    setLine(line);
    updateLine();
  }
  
  
  protected boolean exit() {
    return exit(null);
  }
  protected boolean exit(Exception e) {
    try {
      if (e == null)
      updateLine("Exiting...");
      else {
        updateLine("An exception occured! Exiting...");
        e.printStackTrace();
      }
      close();
      
      return true;
    } catch (Exception e1) {
      System.err.println("Could not exit properly!");
      e.printStackTrace();
      return false;
    }
    
  }
  
  protected abstract void updateLine();
  public abstract boolean setCaret(int position);
  
  @Override
  public void run() {
    init();
    try (BufferedReader bf = new BufferedReader(new InputStreamReader(System.in))) {
      inputStream = bf;
      line = new StringBuffer();
      updateLine("Super duper muper testy Test.");
      while (true) {
        Function f = this.readFunction(bf);

        doFunction(f);
      }
      
    } catch (IOException e) {
      exit();
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
