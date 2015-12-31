package me.polymehr.polyCmd.util;

import me.polymehr.polyCmd.natives.NativeLoader;

public class LinuxUtil {
  
  static {
    NativeLoader.load("linux-native-utils", "lib", ".so");
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
