package me.polymehr.polyCmd.util;

public class Unix {
  
  static {
    System.loadLibrary("linux-native-utils");
  }
  
  /**
   * Returns the width of the terminal.
   */
  public static native int getTerminalColumns();
  
  /**
   * Returns the height of the terminal.
   */
  public static native int getTerminalLines();
}
