package me.polymehr.polyCmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;

import me.polymehr.polyCmd.util.Unix;

public class LinuxInputInterpreter extends InputInterpreter {

  public LinuxInputInterpreter(CommandInterface ci, Path historyLocation,
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
  protected boolean init() {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        exit();
      }
    }));
    
    return setTerminalMode("raw");
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
  
//  private int getColumns() {
//    try {
//      Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", 
//          "\"echo $COLUMNS\" < /dev/tty"});
//      System.out.println("Waiting.");
////      p.waitFor();
//      BufferedReader br = new BufferedReader(new InputStreamReader(
//          p.getInputStream()));
//      
//      return Integer.parseInt(br.readLine());
//    } catch (IOException /*| InterruptedException*/ e) {
//      e.printStackTrace();
//      return 0;
//    }
//  }

  @Override
  protected void updateLine() {
    System.out.print("Drawing.");
    System.out.print("\r" + PS1 + " " + line + " \u001B[K");
  }

  @Override
  public boolean setCaret(int position) {
    if (position < 0)
      caret = 0;
    else if (position > line.length())
      caret = line.length();
    else 
      caret = position;
    
    
    StringBuffer buf = new StringBuffer();
    
    for (int len = caret-line.length(); len <= 0; ++len)
      buf.append('\u0008');
    
    System.out.print(buf);
    
//    System.out.print("\u001B["+(PS1.length()+2+caret)+"G");
    
    return caret == position;
    
  }

}
