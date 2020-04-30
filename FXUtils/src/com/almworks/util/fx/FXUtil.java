package com.almworks.util.fx;

import com.almworks.util.LogHelper;
import com.almworks.util.components.plaf.patches.FontSizeSettingPatch;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class FXUtil {
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(FXUtil.class.getClassLoader(), "com/almworks/util/fx/defaultMessages");
  public static final String PROPERTIES_EXT = ".properties";

  public static void loadFxml(Object controller, String fxmlName, @Nullable String resourceName) {
    try {
      loadFxmlWithException(controller, fxmlName, resourceName);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void loadFxmlWithException(Object controller, String fxmlName, @Nullable String resourceName) throws IOException {
    Class<?> clazz = controller.getClass();
    URL fxmlUrl = clazz.getResource(fxmlName);
    ResourceBundle i18n = null;
    try {
      ClassLoader classLoader = clazz.getClassLoader();
      if (resourceName != null) {
        String resourcePath;
        if (resourceName.startsWith("/")) resourcePath = resourceName.substring(1);
        else {
          String path = Util.getClassPath(clazz);
          resourcePath = path + resourceName;
        }
        if (resourcePath.endsWith(PROPERTIES_EXT))
          resourcePath = resourcePath.substring(0, resourcePath.length() - PROPERTIES_EXT.length());
        i18n = ResourceBundle.getBundle(resourcePath, Locale.getDefault(), classLoader);
      }
      FXMLLoader loader = new FXMLLoader(fxmlUrl, i18n, new JavaFXBuilderFactory(classLoader));
      loader.setClassLoader(classLoader);
      loader.setController(controller);
      loader.load();
      fixCssStylesheetsReferences(controller);
    } catch (IOException | RuntimeException e) {
      LogHelper.warning("Failed to load FXML:", fxmlName, resourceName, fxmlUrl, i18n, controller);
      throw e;
    }
  }

  private static void fixCssStylesheetsReferences(Object controller) {
    if (controller == null) return;
    List<Field> fields = new ArrayList<>();
    Class<?> clazz = controller.getClass();
    while (clazz != null) {
      fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
      clazz = clazz.getSuperclass();
    }
    for (Field field : fields) {
      if (field.getAnnotation(FXML.class) == null) return;
      field.setAccessible(true);
      Object val;
      try {
        val = field.get(controller);
      } catch (IllegalAccessException e) {
        continue;
      }
      Parent parent = Util.castNullable(Parent.class, val);
      if (parent == null) continue;
      for (int i = 0; i < parent.getStylesheets().size(); i++) {
        ObservableList<String> stylesheets = parent.getStylesheets();
        String cssRef = stylesheets.get(i);
        stylesheets.set(i, fixCssReference(cssRef));
      }
    }
  }

  public static String loadCssRef(Object controller, String path) {
    if (controller == null) return null;
    URL uri = controller.getClass().getResource(path);
    if (uri == null) return null;
    return fixCssReference(uri.toExternalForm());
  }

  /**
   * Use this method to add stylesheets to {@link Parent#getStylesheets()}.
   * Replaces whitespace character with "%20". Otherwise the stylesheet reference won't loaded.<br>
   * See: http://stackoverflow.com/questions/32757086/load-javafx-css-with-whitespaces-within-path
   * @param cssUri css URI (resource or file)
   * @return fixed css URI (with whitespace characters)
   *
   */
  private static String fixCssReference(String cssUri) {
    if (cssUri == null) return null;
    return cssUri.replaceAll(" ", "%20").replace('\\', '/');
  }

  public static Window findWindow(@Nullable Node node) {
    if (node == null) return null;
    Scene scene = node.getScene();
    return scene == null ? null : scene.getWindow();
  }

  public static URI getDocumentUri(Document document) {
    URI uri;
    try {
      if (document == null) return null;
      String documentURI = document.getDocumentURI();
      uri = documentURI != null ? new URI(documentURI) : null;
    } catch (URISyntaxException e) {
      LogHelper.warning("DocumentURI:", document.getDocumentURI(), e);
      uri = null;
    }
    return uri;
  }

  public static void addFontSizeStyle(Node node) {
    int fontSize = FontSizeSettingPatch.getOverrideFontSize();
    if (fontSize <= 0) return;
    if (node.styleProperty().isBound()) {
      LogHelper.warning("Style is bound, cant update font style", node);
      return;
    }
    String style = node.getStyle();
    String fontStyle = "-fx-font-size: " + fontSize;
    node.setStyle(style == null || style.isEmpty() ? fontStyle : style + ";" + fontStyle);
  }
}
