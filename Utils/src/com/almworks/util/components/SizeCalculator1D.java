package com.almworks.util.components;

import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public abstract class SizeCalculator1D {
  public abstract int getPrefLength(@NotNull JComponent context);

  public static SizeCalculator1D fixedPixels(final int pixels) {
    return new FixedPixels(pixels);
  }

  public static SizeCalculator1D text(final String text) {
    return new Text(text);
  }

  public static SizeCalculator1D letterMWidth(final int count) {
    return new LetterM(count);
  }

  public static SizeCalculator1D sum(final int gap, final SizeCalculator1D ... calculators) {
    return new SizeCalculator1D() {
      public int getPrefLength(@NotNull JComponent context) {
        int sum = 0;
        for (SizeCalculator1D calculator : calculators) {
          int length = calculator.getPrefLength(context);
          if (length > 0 && sum > 0)
            sum += gap;
          sum += length;
        }
        return sum;
      }
    };
  }

  public static SizeCalculator1D textLines(final int number) {
    return new SizeCalculator1D() {
      @Override
      public int getPrefLength(@NotNull JComponent context) {
        FontMetrics metrics = context.getFontMetrics(context.getFont());
        return metrics.getHeight() * number;
      }
    };
  }

  protected FontMetrics getFontMetrics(@NotNull JComponent context) {
    return context.getFontMetrics(context.getFont());
  }

  private static class FixedPixels extends SizeCalculator1D {
    private final int myPixels;

    public FixedPixels(int pixels) {
      myPixels = pixels;
    }

    public int getPrefLength(@NotNull JComponent context) {
      return myPixels;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      FixedPixels other = Util.castNullable(FixedPixels.class, obj);
      return other != null && myPixels == other.myPixels;
    }

    @Override
    public int hashCode() {
      return myPixels ^ FixedPixels.class.hashCode();
    }
  }


  private static class Text extends SizeCalculator1D {
    private final String myText;

    public Text(String text) {
      myText = text;
    }

    public int getPrefLength(@NotNull JComponent context) {
      return getFontMetrics(context).stringWidth(myText);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      Text other = Util.castNullable(Text.class, obj);
      return other != null && Util.equals(myText, other.myText);
    }

    @Override
    public int hashCode() {
      return Util.hashCode(myText) ^ Text.class.hashCode();
    }
  }


  private static class LetterM extends SizeCalculator1D {
    private final int myCount;

    public LetterM(int count) {
      myCount = count;
    }

    public int getPrefLength(@NotNull JComponent context) {
      return getFontMetrics(context).charWidth('M') * myCount;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      LetterM other = Util.castNullable(LetterM.class, obj);
      return other != null && myCount == other.myCount;
    }

    @Override
    public int hashCode() {
      return myCount ^ LetterM.class.hashCode();
    }
  }
}
