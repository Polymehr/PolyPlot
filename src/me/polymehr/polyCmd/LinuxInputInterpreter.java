package me.polymehr.polyCmd;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import me.polymehr.polyCmd.util.LinuxUtil;

public class LinuxInputInterpreter extends InputInterpreter {
  
  private State lastState = null;
  
  private JTextArea j = new JTextArea();
  
  public LinuxInputInterpreter(CommandInterface ci, Path historyLocation,
      int preLoadedHistoryLines) {
    super(ci, historyLocation, preLoadedHistoryLines);
    
    JFrame j = new JFrame("Test");
    j.setSize(300, 300);
    JScrollPane sp = new JScrollPane(this.j);
    this.j.setBackground(Color.BLACK);
    this.j.setForeground(Color.GREEN);
    j.add(sp);
    j.setVisible(true);
  }

  @Override
  public void close() throws Exception {
    if (!setTerminalMode("sane") | !setTerminalMode("echo")) {
      System.err.println("An error occurred while resetting your terminal to \"sane\" mode.\n"
          + "As a result your terminal could misbehave. Try resetting it manualy with the 'stty' command.");
    }
  }

  @Override
  protected boolean init() {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        exit();
      }
    }));
    
    return setTerminalMode("raw") & setTerminalMode("-echo");
    
  }

  @Override
  protected Function readFunction(BufferedReader input) throws IOException {
    this.lastChar = input.read();
    
    
    switch (lastChar) {
      // Handle CSI escape codes
      case 0x1B: { // read char == escape char 
    
      int sec = input.read(), thri = lastChar = input.read();
      if (sec == 0x5B)
        switch (thri) {
          case 0x41:
            return Function.HISTORY_UP; //ARROW UP
          case 0x42:
            return Function.HISTORY_DOWN; //ARROW DOWN
          case 0x43:
            return Function.CARET_RIGHT; //ARROW RIGHT
          case 0x44:
            return Function.CARET_LEFT; //ARROW LEFT
          default:
            break;
        }
      else if (sec == 0x4F)
        switch (thri) {
          case 0x48:
            return Function.CARET_START; //HOME
          case 0x46:
            return Function.CARET_END;//END
        }
      return Function.NO_OPERATION;
    }
      case 0x09: // Tab
        return Function.AUTOCOMPLETE;
      case 0x0D: // New Line / Carriage Return 
        return Function.SEND;
      case 0x0C:
        return Function.CLEAR;
      case 0x7F: // Backspace
        return Function.DELETE_BACK;
      case 0x03: // Ctrl+C / End of Text
      case 0x04: // Ctrl+D / End of Transmission
      case -1  :
        return Function.EXIT;
    
      default :
        return lastChar < 0x20 ? 
            Function.NO_OPERATION : Function.CHAR_INPUT;
    }
  }
  
  private boolean setTerminalMode(String mode) {
    try {
      String[] cmd = {"/bin/sh", "-c", "stty " + mode + " </dev/tty"};
      return Runtime.getRuntime().exec(cmd).waitFor() == 0;
    } catch (IOException | InterruptedException e) {
      return false;
    }
  }


  @Override
  protected void updateDrawnLine() {
    setCaretOnScreen(0, false);
    
    System.out.print("\r" + PS1 + " " + line + " \u001B[K");
//    System.out.print("["+internal.size()+","+caretLine+":"+caret+"]");
  }
  
  private void drawLine(int number) {
    
  }
  
  private void setLine(int line) {
    System.out.print("");
  }
  
  String last = "";

  public boolean setCaretOnScreen(int position, boolean updateCaret) {
    final int size = internal.size(), termWidth = LinuxUtil.getTerminalColumns();
    if (position < 0)
      position = 0;
    else if (position > size)
      position = size;
    
    int caretLine = (caret+PS1.length()+1)/termWidth;
    int targtLine = (position+PS1.length()+1)/termWidth;
    int lineOffset = targtLine - caretLine;
    
    String current = "Caret: " + caret + ";\n pos:"+position+";\n caretline:" + caretLine + 
        ";\n posline: " +targtLine + ";\n lineoffset: "+lineOffset +";\n term: "+ termWidth;
    
    if (termWidth >= (PS1.length()+1+size + (caret == size ? 1: 0))) {
      System.out.print("\u001B["+(PS1.length()+2+position)+"G");
      current += ";\n of: false\n\n";
    } else {
      current += ";\n of: true\n\n";
      if (lineOffset != 0)
        System.out.print("\u001B["+Math.abs(lineOffset)+(lineOffset < 0?"A":"B"));
      System.out.print("\r\u001B["+((PS1.length()+position-1)%termWidth)+"C");
      
//      try {
//        Thread.currentThread().sleep(1000);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
    }
    
    j.setText(last + current);
    last = current;
    
    if (updateCaret)
      caret = position;
    
    return position >= 0 && position <= size;
    
  }

  @Override
  public boolean setCaretOnScreen(int position) {
    return setCaretOnScreen(position, true);
  }
  
  private static class State {
    List<Integer> internal = null;
    int           caret = 0;
    int           termsize = 0;
    
    void update(LinuxInputInterpreter lIn) {
      internal = lIn.internal;
      caret    = lIn.caret;
      termsize = LinuxUtil.getTerminalColumns();
    }
    
  }

}
