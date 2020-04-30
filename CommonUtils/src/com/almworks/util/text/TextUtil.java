package com.almworks.util.text;

import com.almworks.util.collections.ArrayIterator;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Convertors;
import com.almworks.util.text.html.HtmlTextIterator;
import org.almworks.util.Const;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Dyoma
 */
public class TextUtil {
  private static final String ESLASH = "\\\\";
  private static final String ESLASH2 = ESLASH + ESLASH;
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final ThreadLocal<DecimalFormat> DEFAULT_NUMBER_FORMAT = new ThreadLocal<DecimalFormat>();

  private static final Pattern CLOSE_TAG_PATTERN =
    Pattern.compile("<\\s*([^<>/ ]*)([^<>/]*)/>", Pattern.CASE_INSENSITIVE);
  private static final Pattern REMOVE_IMG = Pattern.compile("<\\s*img.*/>", Pattern.CASE_INSENSITIVE);
  private static final String DEFAULT_STYLESHEET =
    "<style>\n" + "body { font-family: Tahoma; font-size: 11pt; }\n" + "</style>";
  public static final Pattern GET_NUMERIC_PATTERN = Pattern.compile("\\d+");
  public final static Pattern KEYWORD_SPLIT_PATTERN = Pattern.compile("[\\s\\,]+");
  public final static Pattern LABEL_SPLIT_PATTERN = Pattern.compile("\\s+");

  public static final Convertor<String, String> SOFTER_STRING = new Convertor<String, String>() {
    @Override
    public String convert(String value) {
      return getSoftStringForComparison(value);
    }
  };


  /**
   *
   * @param input
   * @param defaultValue
   * @return defaultValue if no integer (>0) found
   */
  public static final int findFirstNonNegativeInt(String input, int defaultValue) {
    Matcher matcher = GET_NUMERIC_PATTERN.matcher(input);
    if (matcher.find()) {
      return Util.toInt(input.substring(matcher.start(), matcher.end()), defaultValue);
    }
    return defaultValue;
  }

  public static int countLines(String text) {
    LineTokenizer tokenizer = new LineTokenizer(text);
    int counter = 0;
    while (tokenizer.hasMoreLines()) {
      tokenizer.nextLine();
      counter++;
    }
    return counter;
  }

  public static String formatFilename(File file) {
    StringBuffer result = new StringBuffer();
    result.append(file.getName());
    File parentFile = file.getParentFile();
    if (parentFile != null) {
      result.append(" (");
      result.append(parentFile.getPath());
      result.append(")");
    }
    return result.toString();
  }

  public static String escapeChar(String string, char c) {
    return string.replaceAll(ESLASH, ESLASH2).replaceAll(String.valueOf(c), ESLASH + c);
  }

  public static String unescapeChar(String string, char c) {
    return string.replaceAll(ESLASH + c, String.valueOf(c)).replaceAll(ESLASH2, ESLASH);
  }

  public static String unwrapLines(String text) {
    return unwrapLines(text, 0);
  }

  public static String unwrapLines(String text, int minLength) {
    List<String> lines = LineTokenizer.getLines(text);
    if (lines.size() == 0)
      return "";
    StringBuffer result = new StringBuffer();
    int maxLength = maxLength(lines, minLength);
    for (int i = 0; i < lines.size() - 1; i++) {
      String line = lines.get(i);
      if (line.trim().length() == 0) {
        result.append("\n");
        continue;
      }
      result.append(line);
      String nextLine = lines.get(i + 1);
      String nextLineTrimmed = nextLine.trim();
      if (nextLineTrimmed.length() == 0) {
        result.append("\n");
        continue;
      }
      if (Character.isWhitespace(nextLine.charAt(0))) {
        result.append("\n");
        continue;
      }
      String word = firstWord(nextLineTrimmed);
      if (line.length() + 1 + word.length() < maxLength)
        result.append("\n");
      else
        result.append(" ");
    }
    result.append(lines.get(lines.size() - 1));
    return result.toString();
  }

  private static String firstWord(String line) {
    return line.split("\\W", 2)[0];
  }

  @NotNull
  public static String[] getKeywords(@Nullable String text) {
    return split(text, KEYWORD_SPLIT_PATTERN);
  }

  @NotNull
  public static String[] getLabels(@Nullable String text) {
    return split(text, LABEL_SPLIT_PATTERN);
  }

  @NotNull
  private static String[] split(@Nullable String text, Pattern delimiter) {
    if (text == null) {
      return Const.EMPTY_STRINGS;
    }
    text = text.trim();
    //noinspection ConstantConditions
    if (text.length() == 0) {
      return Const.EMPTY_STRINGS;
    }
    return delimiter.split(text);
  }

  public static int maxLength(List<String> lines, int min) {
    int length = min;
    for (String line : lines) {
      length = Math.max(line.length(), length);
    }
    return length;
  }

  public static String truncate(String text, FontMetrics fontMetrics, int maxWidth) {
    int originalWidth = fontMetrics.stringWidth(text);
    if (originalWidth < maxWidth)
      return text;
    float ratio = ((float) maxWidth) / originalWidth;
    int originalLength = text.length();
    int length = (int) (originalLength * ratio);
    if (length == 0)
      return "";
    String truncated = text.substring(length);
    int cutted = fontMetrics.stringWidth(truncated);
    int toCut = originalWidth - maxWidth;
    if (cutted != toCut) {
      if (cutted > toCut) {
        while (length < originalLength) {
          cutted -= fontMetrics.charWidth(text.charAt(length - 1));
          if (cutted <= toCut)
            break;
          length++;
        }
      } else {
        while (length > 0) {
          length--;
          cutted += fontMetrics.charWidth(text.charAt(length));
          if (cutted >= toCut)
            break;
        }
      }
    }

    return text.substring(0, length);
  }

  public static String truncateChars(String text, int maxLength) {
    if (text == null || text.length() < maxLength) return text;
    return text.substring(0, maxLength) + "…";
  }

  @Nullable
  public static String hardWrap(String text, int columns) {
    if (text == null)
      return null;
    assert columns > 1 : columns;

    int length = text.length();
    if (length < columns)
      return text;

    StringBuffer result = new StringBuffer(length + length / 80 + 1);
    int charsOnLine = 0;
    // 0 is invalid since there's no point in inserting \n on the zeroeth index.
    int indexForCR = 0;
    for (int i = 0; i < length; i++) {
      char c = text.charAt(i);
      result.append(c);
      if (c == '\n') {
        charsOnLine = 0;
        indexForCR = 0;
      } else {
        charsOnLine++;
        if (charsOnLine > columns && indexForCR > 0) {
          result.insert(indexForCR, '\n');
          charsOnLine = result.length() - indexForCR - 1;
          indexForCR = 0;
          assert charsOnLine >= 0 : charsOnLine;
        }
        if (Character.isWhitespace(c))
          indexForCR = result.length();
      }
    }

    return result.toString();
  }

  public static String separate(String[] strings, String separator) {
    return separate(Arrays.asList(strings), separator);
  }

  public static String concatenate(Iterable<? extends String> strings) {
    return separate(strings, "");
  }

  public static <T> String separate(T[] strings, String separator, Convertor<T, String> convertor) {
    return separate(ArrayIterator.readonly(strings), separator, convertor);
  }

  public static String separate(Iterable<? extends String> strings, String separator) {
    return separate(strings, separator, Convertors.<String>identity());
  }

  public static <T> String separate(Iterable<? extends T> strings, String separator, Convertor<T, String> convertor) {
    return separate(strings.iterator(), separator, convertor);
  }

  public static <T> String separate(Iterator<? extends T> it, String separator, Convertor<T, String> convertor) {
    StringBuilder buffer = new StringBuilder();
    separate(it, separator, convertor::convert, buffer);
    return buffer.toString();
  }

  public static <T> void separate(Iterator<? extends T> it, String separator, Function<T, String> toString, StringBuilder buffer) {
    String prevSeparator = "";
    while (it.hasNext()) {
      T element = it.next();
      String string = toString.apply(element);
      if (string != null && string.length() > 0) {
        buffer.append(prevSeparator);
        buffer.append(string);
        prevSeparator = separator;
      }
    }
  }

  public static String separateToString(Iterable<?> list, String separator) {
    return separate(list, separator, Convertors.getToString());
  }

  public static String prepend(Collection<String> strings, String prefix, String sep) {
    StringBuffer buffer = new StringBuffer();
    String separator = "";
    for (String s : strings) {
      buffer.append(separator);
      separator = sep;
      buffer.append(prefix);
      buffer.append(s);
    }
    return buffer.toString();
  }

  public static String preprocessHtml(String html) {
    html = CLOSE_TAG_PATTERN.matcher(html).replaceAll("<$1$2></$1>");
    html = REMOVE_IMG.matcher(html).replaceAll("");
    Font font = UIManager.getFont("Label.font");
    String family = font != null ? font.getFamily() : "Tahoma";
    int size = font != null ? font.getSize() : 11;
    return "<html><style>body { font-family: " + family + "; font-size: " + size + ";}</style><body>" + html +
      "</body></html>";
  }

  @NotNull
  public static BigDecimal parseBigDecimal(@NotNull String input) throws NumberFormatException {
    return new BigDecimal(input.trim());
  }

  public static String bigDecimalToString(BigDecimal number) {
    if (number == null)
      return null;
    DecimalFormat format = DEFAULT_NUMBER_FORMAT.get();
    if (format == null) {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
      symbols.setDecimalSeparator('.');
      format = new DecimalFormat("0.###", symbols);
      format.setGroupingUsed(false);
      DEFAULT_NUMBER_FORMAT.set(format);
    }
    String value = format.format(number);
    return value;
  }

  /**
   * Now method unused. Needed when collapsed comments will be paited using JTextArea
   * @param text
   * @param pattern
   * @param width
   * @param maxLines
   * @param metrics
   * @param htmlContent
   * @return
   */
  public static String getFirstLinesFromFirstMatch(String text, Pattern pattern, int width, int maxLines, FontMetrics metrics,
    boolean htmlContent) {

    if (pattern != null && pattern.pattern() != "") {
      text = getStartWithMatch(text, pattern);
    }
    String[] lines = getPlainTextLines(text, width, maxLines, metrics, htmlContent, false);

    return lines[0];
  }

  public static String getStartWithMatch(String text, Pattern pattern) {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      int start = matcher.start(0);
      if (start > 10) {
        text = "\u2026" + text.substring(start - 10);
      }
    }
    return text;
  }

  public static String[] getPlainTextLines(String text, int width, int maxLines, FontMetrics metrics,
    boolean htmlContent, boolean split)
  {
    CharIterator it = htmlContent ? new HtmlTextIterator(text) : new StringCharIterator(text);
    String[] result = new String[split ? maxLines : 1];
    StringBuilder globalBuffer = new StringBuilder();
    String word = nextWord(it);
    for (int i = 0; i < maxLines; i++) {
      StringBuilder buffer = new StringBuilder();
      String suffix = i < maxLines - 1 ? "" : " \u2026";
      int maxLineWidth = suffix == "" ? width : width - metrics.stringWidth(suffix);

      buffer.append(word);
      word = nextWord(it);
      while (it.hasNext()) {
        if (word.length() > 0) {
          boolean appended = appendWord(buffer, word, metrics, maxLineWidth);
          if (!appended) {
            break;
          }
        }
        word = nextWord(it);
      }
      boolean needsSuffix = it.hasNext();
      if (!needsSuffix && word.length() > 0) {
        boolean appended = appendWord(buffer, word, metrics, maxLineWidth);
        if (!appended) {
          needsSuffix = true;
        } else {
          word = nextWord(it);
          assert word.length() == 0;
        }
      }
      if (needsSuffix && suffix.length() > 0) {
        buffer.append(suffix);
      }

      if (split)
        result[i] = buffer.toString();
      else
        globalBuffer.append(buffer).append(' ');
    }

    if (!split) {
      result[0] = globalBuffer.toString();
    }
    return result;
  }

  private static boolean appendWord(StringBuilder buffer, String word, FontMetrics metrics, int maxLineWidth) {
    int lastLength = buffer.length();
    if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) != ' ') {
      buffer.append(' ');
    }
    buffer.append(word);
    int lineWidth = metrics.stringWidth(buffer.toString());
    if (lineWidth > maxLineWidth) {
      buffer.delete(lastLength, buffer.length());
      return false;
    }
    return true;
  }

  public static String nextWord(CharIterator it) {
    StringBuilder buffer = new StringBuilder(16);
    while (it.hasNext()) {
      char c = it.nextChar();
      if (!HtmlTextIterator.isSpace(c))
        buffer.append(c);
      else
        break;
    }
    return buffer.toString();
  }

  //  @Nullable(documentation = "if s is null")
  public static StringBuilder append(@Nullable StringBuilder builder, @Nullable CharSequence s) {
    if (s != null) {
      if (builder == null) {
        builder = new StringBuilder(s);
      } else {
        builder.append(s);
      }
    }
    return builder;
  }

//  @Nullable(documentation ="if sb is null")
  public static String toString(@Nullable StringBuilder sb) {
    return sb != null ? sb.toString() : null;
  }

  /**
   * Returns a string that has only letters and digits from the parameter, with latin characters in lower case.
   * <p>
   * This method is used to soft-compare strings that might differ in white space or special characters.
   */
  @NotNull
  public static String getSoftStringForComparison(@Nullable String text) {
    if (text == null || text.length() == 0)
      return "";
    int len = text.length();
    StringBuilder b = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        b.append(c < 256 ? Character.toLowerCase(c) : c);
      }
    }
    return b.toString();
  }

  public static String separate(byte[] array, int offset, int length, String separator) {
    if (array == null || array.length == 0) return "";
    StringBuilder builder = new StringBuilder();
    String sep = "";
    for (byte n : array) {
      builder.append(sep);
      sep = separator;
      builder.append(n);
    }
    return builder.toString();
  }

  public static String separate(int[] array, int offset, int length, String separator) {
    if (array == null || array.length == 0) return "";
    StringBuilder builder = new StringBuilder();
    String sep = "";
    for (int n : array) {
      builder.append(sep);
      sep = separator;
      builder.append(n);
    }
    return builder.toString();
  }

  public static String separate(long[] array, int offset, int length, String separator) {
    if (array == null || array.length == 0) return "";
    StringBuilder builder = new StringBuilder();
    String sep = "";
    for (long n : array) {
      builder.append(sep);
      sep = separator;
      builder.append(n);
    }
    return builder.toString();
  }

  public static String separateTimes(String input, String separator, int times) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < times; i++) {
      if (i != 0)
        builder.append(separator);
      builder.append(input);
    }
    return builder.toString();
  }

  public static String timesRepeat(char c, int count) {
    char[] chars = new char[count];
    Arrays.fill(chars, c);
    return new String(chars);
  }

  public static <T> Convertor<T, String> prefixToString(final String prefix) {
    return new Convertor<T, String>() {
      @Override
      public String convert(T value) {
        return value != null ? prefix + value : "";
      }
    };
  }

  public static void setTextAndPreserveCaret(JTextComponent cmp, String newContent) {
    int len = cmp.getDocument().getLength();
    float p = (len == 0) ? 0 : (float)cmp.getCaretPosition() / len;
    cmp.setText(newContent);
    len = cmp.getDocument().getLength();
    cmp.setCaretPosition((int) (p *len));
  }

  public static String trimLines(String text) {
    List<String> lines = LineTokenizer.getLines(text);
    for (int i = 0; i < lines.size(); i++) lines.set(i, lines.get(i).trim());
    StringBuilder builder = new StringBuilder();
    String sep = "";
    for (String line : lines) { // Do not replace with TextUtil.separate(). Should keep empty lines.
      builder.append(sep);
      builder.append(line);
      sep = "\n";
    }
    return builder.toString();
  }
}