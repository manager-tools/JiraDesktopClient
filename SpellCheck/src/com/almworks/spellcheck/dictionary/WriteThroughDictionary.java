package com.almworks.spellcheck.dictionary;

import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.InterruptableRunnable;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WriteThroughDictionary implements AlmSuggester.AlmDictionary  {
  private final List<String> myWords = Collections15.arrayList();
  private final List<String> myNewWords = Collections15.arrayList();
  private final File myFile;
  private final Procedure<IOException> myExceptionNotifier;
  private InterruptableRunnable myWriteRequest = new InterruptableRunnable() {
    @Override
    public void run() throws InterruptedException {
      doWrite();
    }
  };

  private WriteThroughDictionary(File file, Procedure<IOException> exceptionNotifier) {
    myFile = file;
    myExceptionNotifier = exceptionNotifier;
  }

  @Override
  public boolean hasWord(String lowCaseWord) {
    synchronized (myWords) {
      return Collections.binarySearch(myWords, lowCaseWord) >= 0;
    }
  }

  @NotNull
  public static WriteThroughDictionary create(File file, Procedure<IOException> exceptionNotifier) throws IOException {
    String[] words = FixedDictionary.loadWords(file);
    WriteThroughDictionary dictionary = new WriteThroughDictionary(file, exceptionNotifier);
    dictionary.myWords.addAll(Arrays.asList(words));
    return dictionary;
  }

  /**
   * @return true if dictionary word set is changed
   */
  public boolean addWord(String word) {
    word = word.trim();
    word = Util.lower(word);
    if (word.isEmpty()) return false;
    synchronized (myWords) {
      int index = Collections.binarySearch(myWords, word);
      if (index >= 0) return false;
      myWords.add(-index - 1, word);
    }
    boolean requestWrite;
    synchronized (myNewWords) {
      if (myNewWords.contains(word)) return true;
      myNewWords.add(word);
      requestWrite = myNewWords.size() == 1;
    }
    if (requestWrite) ThreadGate.executeLong(myWriteRequest);
    return true;
  }

  private void doWrite() {
    String[] words;
    synchronized (myNewWords) {
      int size = myNewWords.size();
      if (size == 0) return;
      words = new String[size];
      myNewWords.toArray(words);
      myNewWords.clear();
    }
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(buffer);
    try {
      for (String word : words) writer.append(word).append("\n");
      writer.close();
    } catch (IOException e) {
      LogHelper.error(e); // should not happen
      return;
    }
    try {
      FileOutputStream fileStream = new FileOutputStream(myFile, true);
      try {
        fileStream.write(buffer.toByteArray());
      } finally {
        fileStream.close();
      }
    } catch (IOException e) {
      myExceptionNotifier.invoke(e);
    }
  }

  public File getFile() {
    return myFile;
  }
}
