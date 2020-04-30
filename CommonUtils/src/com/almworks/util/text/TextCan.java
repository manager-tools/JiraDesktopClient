package com.almworks.util.text;

import com.almworks.util.collections.Convertor;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public abstract class TextCan {
  protected static final int SIZE_OF = 8 + 4 + 4;
  private static final int SIZE_OF_STRING = 28;
  private static final int DEFAULT_MIN_BYTES = 50;
  private static final float DEFAULT_MIN_RATIO = 0.6f;
  private final int myLength;
  private int myHashCode;
  public static final Convertor<TextCan, String> TO_STRING = new Convertor<TextCan, String>() {
    @Override
    public String convert(TextCan value) {
      return value != null ? value.getText() : null;
    }
  };
  public static final Convertor<String, TextCan> FROM_STRING = new Convertor<String, TextCan>() {
    @Override
    public TextCan convert(String value) {
      return wrapNN(value);
    }
  };

  protected TextCan(int length) {
    myLength = length;
  }

  @NotNull
  public abstract String getText();

  public final int getTextLength() {
    return myLength;
  }

  protected abstract int calcHashCode();

  public final boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null) return false;
    if (!(obj instanceof TextCan)) return false;
    TextCan other = (TextCan) obj;
    if (other.getTextLength() != getTextLength() || other.hashCode() != hashCode()) return false;
    String text = getText();
    String otherText = other.getText();
    return Util.equals(text, otherText);
  }

  public final int hashCode() {
    if (myHashCode != 0) return myHashCode;
    int hash = calcHashCode();
    if (hash == 0) hash = 1;
    myHashCode = hash;
    return hash;
  }

  public static TextCan wrap(String text, int minBytes, float minRatio) {
    if (text == null) return null;
    if (text.length() == 0) return EMPTY_TEXT;
    int textSize = sizeOfString(text);
    int rawSize = textSize + RawText.SIZE_OF;
    int minCompressed = ZipText.SIZE_OF + text.length() / 10;
    if (!needsCompression(minBytes, minRatio, rawSize, minCompressed)) return new RawText(text);
    byte[] coded = ZipUtil.encode(text);
    if (coded == null) return new RawText(text);
    int compressed = ZipText.SIZE_OF + coded.length;
    if (!needsCompression(minBytes, minRatio, rawSize, compressed)) return new RawText(text);
    return new ZipText(text.length(), coded);
  }

  public static TextCan wrap(String text) {
    return wrap(text, DEFAULT_MIN_BYTES, DEFAULT_MIN_RATIO);
  }

  @NotNull
  public static TextCan wrapNN(String text) {
    TextCan can = wrap(text);
    return can != null ? can : EMPTY_TEXT;
  }

  private static boolean needsCompression(int minBytes, float minRatio, int rawSize, int compressed) {
    return rawSize - compressed > minBytes && (float )compressed / rawSize < minRatio;
  }

  static int sizeOfString(String text) {
    return SIZE_OF_STRING + text.length() * 2;
  }

  public static final TextCan EMPTY_TEXT = new TextCan(0) {
    @Override
    public String getText() {
      return "";
    }

    @Override
    protected int calcHashCode() {
      return getText().hashCode();
    }
  };

  static class RawText extends TextCan {
    static final int SIZE_OF = TextCan.SIZE_OF + 4;
    private final String myText;

    protected RawText(@NotNull String text) {
      super(text.length());
      myText = text;
    }

    @NotNull
    @Override
    public String getText() {
      return myText;
    }

    @Override
    protected int calcHashCode() {
      return myText.hashCode();
    }
  }

  static class ZipText extends TextCan {
    static final int SIZE_OF = TextCan.SIZE_OF + 4 + 4 + 12;
    private final byte[] myZipped;
    private volatile WeakReference<String> myDecoded = null;

    private ZipText(int length, byte[] zipped) {
      super(length);
      myZipped = zipped;
    }

    @NotNull
    @Override
    public String getText() {
      WeakReference<String> ref = myDecoded;
      if (ref != null) {
        String text = ref.get();
        if (text != null) return text;
      }
      String text = ZipUtil.decode(myZipped);
      if (text == null) text = TextUtil.timesRepeat(' ', getTextLength());
      myDecoded = new WeakReference<String>(text);
      return text;
    }

    @Override
    protected int calcHashCode() {
      return getText().hashCode();
    }
  }
}
