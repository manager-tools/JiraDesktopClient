package com.almworks.spellcheck.dictionary;

import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Const;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class FixedDictionary implements AlmSuggester.AlmDictionary {
  private final String[] myWords;

  private FixedDictionary(String[] words) {
    myWords = words;
  }

  public static FixedDictionary loadUTF8File(File file) throws IOException {
    String[] array;
    array = loadWords(file);
    return new FixedDictionary(array);
  }

  public static String[] loadWords(File file) throws IOException {
    if (!file.exists()) return Const.EMPTY_STRINGS;
    String[] array;FileInputStream stream = new FileInputStream(file);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
      String line;
      ArrayList<String> words = Collections15.arrayList();
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty()) words.add(line);
      }
      array = words.toArray(new String[words.size()]);
    } finally {
      stream.close();
    }
    Arrays.sort(array);
    int newLength = ArrayUtil.removeSubsequentDuplicates(array, 0, array.length);
    if (newLength < array.length) {
      String[] newArray = new String[newLength];
      System.arraycopy(array, 0, newArray, 0, newLength);
      array = newArray;
    }
    return array;
  }

  @Override
  public boolean hasWord(String lowCaseWord) {
    return Arrays.binarySearch(myWords, lowCaseWord) >= 0;
  }
}
