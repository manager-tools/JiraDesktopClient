package com.almworks.tools.modpip;

import com.almworks.util.files.FileUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ModPip {
  private static final String BUILD_DATE_FORMAT = "yyyy/MM/dd HH:mm z";

  private static String fileContents;

  public static void main(String[] args) throws IOException {
    boolean success = false;
    try {
      File file = new File(args[0]);
      fileContents = FileUtil.readFile(file);
      fileContents = replace(fileContents, "buildDate", new SimpleDateFormat(BUILD_DATE_FORMAT).format(new Date()));
      for (int i = 1; i < args.length; ) {
        String cmd = args[i++];
        String value = args[i++];
        fileContents = replace(fileContents, cmd, value);
      }
      FileUtil.writeFile(file, fileContents);
      success = true;
    } finally {
      if (!success) {
        System.err.println("--unsuccessful ModPip--");
        System.err.println(Arrays.asList(args));
        if (fileContents != null)
          System.err.println(fileContents);
      }
    }
  }

  private static String replace(String java, String constName, String constValue) {
    String prefix = "private final String " + constName + " = \"";
    int k = java.indexOf(prefix);
    if (k < 0)
      throw new IllegalArgumentException("cannot find constant " + constName);
    int p = java.indexOf('"', k + prefix.length());
    if (p < 0 || java.substring(k, p).indexOf('\n') >= 0)
      throw new IllegalArgumentException("bad code for constant " + constName);
    String value = constValue.replaceAll("\"", "\\\"").replaceAll("\\\\", "\\\\");
    return java.substring(0, k + prefix.length()) + value + java.substring(p);
  }
}
