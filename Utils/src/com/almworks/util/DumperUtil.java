package com.almworks.util;

import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.io.PrintStream;
import java.util.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class DumperUtil {

  public static String getBytesRepresentation(byte[] bytes) {
    if (bytes.length == 0)
      return "{}";

    StringBuffer buffer = new StringBuffer();
    int rows = (bytes.length + 15) / 16;
    for (int i = 0; i < rows; i++) {
      if (i > 0)
        buffer.append('\n');
      StringBuffer textual = new StringBuffer();
      int j;
      for (j = 0; j < 16; j++) {
        int k = i * 16 + j;
        if (k >= bytes.length)
          break;
        if (j > 0)
          buffer.append(' ');
        if (j == 8)
          buffer.append(' ');
        String hex = Util.upper(Integer.toHexString(((int) bytes[k]) & 0xFF));
        if (hex.length() == 1)
          buffer.append('0');
        buffer.append(hex);
        char c = (char) bytes[k];
        if (Character.isISOControl(c))
          c = '.';
        textual.append(c);
      }
      for (; j < 16; j++) {
        buffer.append("   ");
        if (j == 8)
          buffer.append(' ');
        textual.append(' ');
      }
      buffer.append("   ");
      buffer.append(textual.toString());
    }
    return buffer.toString();
  }

  public static void printMap(PrintStream out, List<Pair<String, String>> map, int indent) {
    int maxKeyLength = 0;
    for (Iterator<Pair<String, String>> ii = map.iterator(); ii.hasNext();) {
      Pair<String, String> pair = ii.next();
      int l = pair.getFirst().length();
      if (l > maxKeyLength)
        maxKeyLength = l;
    }
    for (Iterator<Pair<String, String>> jj = map.iterator(); jj.hasNext();) {
      Pair<String, String> pair = jj.next();
      StringBuffer buffer = new StringBuffer();
      for (int i = 0; i < indent; i++)
        buffer.append(' ');
      int keyLength = pair.getFirst().length();
      while (keyLength < maxKeyLength) {
        buffer.append(' ');
        keyLength++;
      }
      buffer.append(pair.getFirst());
      buffer.append(" => ");
      int offset = buffer.length();
      String value = pair.getSecond();
      if (value.indexOf('\n') == -1) {
        buffer.append(value);
      } else {
        String[] lines = value.split("\n");
        buffer.append(lines[0]);
        for (int i = 1; i < lines.length; i++) {
          buffer.append('\n');
          for (int k = 0; k < offset; k++)
            buffer.append(' ');
          buffer.append(lines[i]);
        }
      }
      out.println(buffer.toString());
    }
  }

  public static void printMap(PrintStream out, Map<?, ?> map, int indent) {
    List<Pair<String, String>> list = Collections15.arrayList();
    for (Map.Entry<?, ?> e : map.entrySet()) {
      list.add(Pair.create(String.valueOf(e.getKey()), String.valueOf(e.getValue())));
    }
    Collections.sort(list, new Comparator<Pair<String, String>>() {
      public int compare(Pair<String, String> o1, Pair<String, String> o2) {
        return o1.getFirst().compareToIgnoreCase(o2.getFirst());
      }
    });
    printMap(out, list, indent);
  }
}
