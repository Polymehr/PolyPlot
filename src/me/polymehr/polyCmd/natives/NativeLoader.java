package me.polymehr.polyCmd.natives;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;

public class NativeLoader {

  /**
   * Loads a native library from jar.
   * 
   * @param name Name of the library without pre- and suffixes.
   * @param prefix the prefix of the library.
   * @param suffix the suffix of the library.
   * 
   * @return if the loading was possible.
   */
  public static boolean load(String name, String prefix, String suffix) {
    String fullName = prefix + name + suffix;
    
    try {
      File dir = new File(System.getProperty("java.io.tmpdir") + File.separator + "polycmd/");
      addLibraryPath(dir.getPath());
      
      try {
        System.loadLibrary(name);
        return true;
      } catch (UnsatisfiedLinkError e) {
        if (!dir.exists())
          dir.mkdir();

        File f = new File(dir.getAbsoluteFile() + File.separator + fullName);

        if (!f.exists())
          f.createNewFile();

        InputStream br = null;
        OutputStream bw = null;

        try {
          br = NativeLoader.class.getResourceAsStream(fullName);
          bw = new FileOutputStream(f);

          int readBytes;
          byte[] buffer = new byte[4096];

          while ((readBytes = br.read(buffer)) > 0)
            bw.write(buffer, 0, readBytes);
          
          System.loadLibrary(name);
          return true;
          
        } catch (IOException e1) {
          e1.printStackTrace();
        } catch (UnsatisfiedLinkError e1) {
          System.err.println("Could not load native libary!");
          e.printStackTrace();
        } finally {
          if (br != null)
            br.close();
          if (br != null)
            bw.close();
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
  
  /**
   * <code>Source: http://stackoverflow.com/questions/15409223/adding-new-paths-for-native-libraries-at-runtime-in-java/15409446#15409446</code> 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */
  private static void addLibraryPath(String pathToAdd) throws NoSuchFieldException, SecurityException, 
                  IllegalArgumentException, IllegalAccessException {
    final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
    usrPathsField.setAccessible(true);

    //get array of paths
    final String[] paths = (String[])usrPathsField.get(null);

    //check if the path to add is already present
    for(String path : paths) {
        if(path.equals(pathToAdd)) {
            return;
        }
    }

    //add the new path
    final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
    newPaths[newPaths.length-1] = pathToAdd;
    usrPathsField.set(null, newPaths);
  }
}
