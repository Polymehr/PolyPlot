package me.polymehr.polyPlot.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;

public class UnixInputInterpreter extends InputInterpreter {

  public UnixInputInterpreter(CommandInterface ci, Path historyLocation,
      int preLoadedHistoryLines) {
    super(ci, historyLocation, preLoadedHistoryLines);
  }

  @Override
  public void close() throws Exception {
    if (!setTerminalMode("sane")) {
      System.err.println("An error occurred while resetting your terminal to \"sane\" mode.\n"
          + "As a result your terminal could misbehave. Try resetting it manualy with the 'stty' command.");
    }
  }

  @Override
  boolean init() {
    return setTerminalMode("raw");
  }

  @Override
  Function readFunction(BufferedReader input) throws IOException {
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
      case 0x7F: // Backspace
        return Function.DELETE_BACK;
      case 0x03: // Ctrl+C / End of Text
      case 0x04: // Ctrl+D / End of Transmission
        return Function.EXIT;
    
      default :
        return Function.CHAR_INPUT;
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
  boolean doFunction(Function f) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  void setLine(String line) throws IOException {
    System.out.print("\r" + PS1 + " " + line + "\u001B[K");
    
  }

}
