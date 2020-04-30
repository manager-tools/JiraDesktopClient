package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.ModelKeyUtils;
import com.almworks.api.application.viewer.UIController;
import com.almworks.api.connector.http.HtmlUtils;
import com.almworks.util.Env;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Convertors;
import com.almworks.util.xml.JDOMUtils;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author dyoma
 */
public class TextController<T> extends BaseTextController<T> {
  private static final String COMMENT_HTML = "jiraclient.comment.html";
  @NotNull
  private final Convertor<T, String> myToTextConvertor;

  @Nullable
  private final Convertor<String, T> myFromTextConvertor;

  @Nullable
  private final T myEmptyValue;

  public TextController(ModelKey<T> key, @NotNull Convertor<T, String> toTextConvertor,
    @Nullable Convertor<String, T> fromText, @Nullable T emptyValue, boolean directSet)
  {
    super(key, directSet);
    myToTextConvertor = toTextConvertor;
    myFromTextConvertor = fromText;
    myEmptyValue = emptyValue;
  }

  @NotNull
  public static String toHumanText(String rawText) {
    if (rawText == null) return "";
    String humanText;
    if (Env.getBoolean(COMMENT_HTML)) {
      try {
        Document document = HtmlUtils.buildHtmlDocument(new InputSource(new StringReader(rawText)));
        humanText = JDOMUtils.getText(document.getRootElement());
      } catch (SAXException e) {
        humanText = rawText;
      } catch (IOException e) {
        humanText = rawText;
      }
    } else humanText = rawText;
    return humanText.trim();
  }

  protected T getEmptyStringValue() {
    return myEmptyValue;
  }

  protected T toValue(String text) {
    Convertor<String, T> textConvertor = myFromTextConvertor;
    assert textConvertor != null;
    return textConvertor == null ? null : textConvertor.convert(text);
  }

  protected boolean isEditable() {
    return myFromTextConvertor != null;
  }

  protected String toText(T value) {
    return myToTextConvertor.convert(value);
  }

  public static TextController<String> installTextViewer(JTextComponent component, ModelKey<String> key, boolean directSet) {
    return installController(component, textViewer(key, directSet));
  }

  public static <C extends UIController> C installController(JComponent component, C controller) {
    CONTROLLER.putClientValue(component, controller);
    return controller;
  }

  public static TextController<String> textViewer(ModelKey<String> key, boolean directSet) {
    return new TextController<String>(key, Convertors.<String>identity(), null, null, directSet);
  }

  public static TextController<String> humanTextViewer(ModelKey<String> key) {
    return new TextController<>(key, new Convertor<String, String>() {
      @Override
      public String convert(String value) {
        return toHumanText(value);
      }
    }, null, null, true);
  }

  public static TextController<Object> anyTextViewer(ModelKey<?> key, boolean directSet) {
    return new TextController<Object>((ModelKey<Object>) key, ModelKeyUtils.ANY_TO_STRING, null, null, directSet);
  }

  public static TextController<Integer> intEditor(ModelKey<Integer> key) {
    return new TextController<Integer>(key, Convertors.<Integer>getToString(), new Convertor<String, Integer>() {
      public Integer convert(String s) {
        try {
          return Integer.parseInt(s);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }, null, false);
  }
}
