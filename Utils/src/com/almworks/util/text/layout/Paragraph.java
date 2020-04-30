package com.almworks.util.text.layout;

import com.almworks.util.collections.Convertor;

import java.util.List;

/**
 * @author : Dyoma
 */
public interface Paragraph {
  public static Convertor<Paragraph, List<String>> TEXT_LINES = new Convertor<Paragraph, List<String>>() {
    public List<String> convert(Paragraph t) {
      return t.getLines();
    }
  };

  double getPixelWidth();
  List<String> getLines();
}
