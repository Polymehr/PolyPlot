package me.polymehr.polyCmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.print.attribute.standard.Chromaticity;

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
  
  protected List<Integer> internal;
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
        updateDrawnLine();
        return setCaretOnScreen(caret-1);
      case CARET_RIGHT:
        updateDrawnLine();
        return setCaretOnScreen(caret+1);
      case AUTOCOMPLETE:
        break;
      case CARET_END:
        return setCaretOnScreen(internal.size());
      case CARET_START:
        return setCaretOnScreen(0);
      case CARET_WORD_NEXT:
        break;
      case CARET_WORD_PREV:
        break;
      case CHAR_INPUT:
        internal.add(caret++, lastChar);
        updateAll();
        return setCaretOnScreen(caret);
      case CLEAR:
        break;
      case DELETE_BACK:
        if (caret > 0) {
          internal.remove(--caret);
          updateAll();
          return setCaretOnScreen(caret);
        } else
          return false;
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
    
    
    return false;
  }
  
  public void setLine(String line) {
    if (line != null) {
      internal.clear();
      line.codePoints().forEach((cp) -> internal.add(cp));
      this.line = new StringBuffer(line);
    }
  }
  
  protected void updateAll() {
    updateLineString();
    updateDrawnLine();
  }
  
  protected void updateLineString() {
    line.setLength(0);
    for (int cp : internal)
      line.appendCodePoint(cp);
  }
  
  protected void updateLine(String line, int caretPosition) {
   setLine(line);
   updateDrawnLine();
   setCaretOnScreen(caretPosition);
  }
  
  protected void updateLine(String line) {
    setLine(line);
    updateDrawnLine();
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
  
  protected abstract void updateDrawnLine();
  public abstract boolean setCaretOnScreen(int position);
  
  @Override
  public void run() {
    init();
    try (BufferedReader bf = new BufferedReader(new InputStreamReader(System.in))) {
      inputStream = bf;
      line = new StringBuffer();
      internal = new ArrayList<>(line.capacity());
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
