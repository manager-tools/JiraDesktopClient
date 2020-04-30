package com.almworks.util.text.parser;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author : Dyoma
 */
public abstract class FormulaWriter {
  private int myIndex;

  private FormulaWriter(int index) {
    myIndex = index;
  }

  public FormulaWriter createChild() {
    addRaw("(");
    final int childIndex = myIndex;
    addRaw(")");
    return new FormulaWriter(childIndex){
      protected void insertSymbolAt(int index, String symbol) {
        FormulaWriter.this.insertSymbol(index, symbol);
      }

      public String getWholeText() {
        return FormulaWriter.this.getWholeText();
      }
    };
  }

  public abstract String getWholeText();

  public void addToken(String text) {
    addRaw(quoteIfNeeded(text));
  }

  public static String quoteIfNeeded(String text) {
    if (text == null)
      text = "";
    StringBuffer buffer = new StringBuffer();
    boolean needsQuotes = text.length() == 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (Character.isLetterOrDigit(c))
        buffer.append(c);
      else {
        needsQuotes = true;
        if (c == '"' || c == '\\')
          buffer.append('\\');
        buffer.append(c);
      }
    }
    if (needsQuotes) {
      buffer.insert(0, '"');
      buffer.append('"');
    }
    String quoted = buffer.toString();
    return quoted;
  }

  public void addRaw(String symbol) {
    insertSymbol(myIndex, symbol);
  }

  private void insertSymbol(int index, String symbol) {
    insertSymbolAt(index, symbol);
    myIndex += symbol.length();
  }

  protected abstract void insertSymbolAt(int index, String symbol);

  public static FormulaWriter create() {
    final StringBuffer buffer = new StringBuffer();
    return new FormulaWriter(0) {
      protected void insertSymbolAt(int index, String symbol) {
        buffer.insert(index, symbol);
      }

      public String getWholeText() {
        return buffer.substring(0, buffer.length());
      }
    };
  }

  public static String write(WritableFormula writable) {
    FormulaWriter result = create();
    writable.writeFormula(result);
    return result.getWholeText();
  }

  public void writeSeparated(@NotNull List<? extends WritableFormula> writables, @NotNull String separator) {
    int size = writables.size();
    if (size == 0)
      return;
    if (size == 1) {
      writables.get(0).writeFormula(this);
      return;
    }
    String sep = "";
    for (WritableFormula writable : writables) {
      addRaw(sep);
      addRaw("(");
      writable.writeFormula(this);
      addRaw(")");
      sep = separator;
    }
  }

}
