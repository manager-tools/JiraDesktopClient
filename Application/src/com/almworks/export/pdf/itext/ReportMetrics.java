package com.almworks.export.pdf.itext;

import com.almworks.util.Env;
import com.almworks.util.collections.FlattenIterator;
import com.almworks.util.io.IOUtils;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.BaseFont;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import sun.font.CompositeFont;
import sun.font.Font2D;
import sun.font.PhysicalFont;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class ReportMetrics {

  public final float BASE_TEXT_SIZE = 10.0f;

  public final float BASIC_TEXT_LEADING = BASE_TEXT_SIZE * 1.2f;

  public final float SPACING = 10.0f;

  public final float BEETWEEN_COMMENT_SPACING = 8.0f;

  public final float PAGE_BREAK_SPACING = 15f;

  public final BaseColor GRAY = new BaseColor(0x66, 0x66, 0x66);
  private static final BaseColor BLACK = BaseColor.BLACK;

  private final Font sansF;
  private final Font sansBoldF;
  private final Font serifF;
  private final Font monoF;

  public Font headerFont() {
    return getFont(sansF, 14, BLACK);
  }

  public Font headerFontBold() {
    return getFont(sansBoldF, 14, BLACK);
  }

  public Font keyReportFont() {
    return getFont(sansBoldF, 12, BLACK);
  }

  public Font whenWhoFont() {
    return getFont(sansBoldF, 8.0f, GRAY);
  }

  public final Font attachFont() {
    return getFont(monoF, BASE_TEXT_SIZE, BLACK);
  }

  public final Font commentDescriptionFont() {
    return attachFont();
  }

  public Font attribureNameFont(boolean compact) {
    final int size = compact ? 8 : 11;
    return getFont(serifF, size, BLACK);
  }

  public final Font attribureValueFont(boolean compact) {
    final int size = compact ? 8 : 11;
    return getFont(sansF, size, BLACK);
  }

  public ReportMetrics(File fontConfig) throws DocumentException, IOException {
    FontConfig config = new FontConfig(fontConfig).load();
    int fontSum = 0;
    if (config.hasFontDirectory()) {
      for (String fontDir : config.myFontDirs) {
        fontSum += FontFactory.registerDirectory(fontDir);
      }
    }
    fontSum = 1;
    if (fontSum == 0 && !Env.isMac()) {
      Iterator<String> ii = FlattenIterator.create(config.mySansFamilies, config.mySansBoldFamilies,
        config.myMonoFamilies, config.mySerifFamilies);
      while (ii.hasNext()) {
        String fontName = ii.next();
        java.awt.Font font = java.awt.Font.decode(fontName);
        assert font != null : fontName;
        String fileName = getFileName(font);
        if (fileName != null) {
          FontFactory.register(fileName);
        }
      }
    }

    sansF = setupFont(config.mySansFamilies, FontFactory.HELVETICA);
    sansBoldF = setupFont(config.mySansBoldFamilies, FontFactory.HELVETICA_BOLD);
    monoF = setupFont(config.myMonoFamilies, FontFactory.COURIER);
    serifF = setupFont(config.mySerifFamilies, FontFactory.TIMES);
  }

  private Font getFont(Font font, float size, BaseColor color) {
    font.setSize(size);
    font.setColor(color);
    return font;
  }

  private Font setupFont(List<String> families, String defaultFont) {
    for (String sansFamily : families) {
      Font font = FontFactory.getFont(sansFamily, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
      if (font.getBaseFont() != null) {
        return font;
      }
    }
    return FontFactory.getFont(defaultFont, FontFactory.defaultEncoding, FontFactory.defaultEmbedding);
  }

  private boolean hasFontDirectory(FontConfig config) {
    return false;
  }

  public static String getFileName(java.awt.Font f) {
    try {
      final Method declaredMethod = java.awt.Font.class.getDeclaredMethod("getFont2D");
      declaredMethod.setAccessible(true);
      Font2D font2d = (Font2D) declaredMethod.invoke(f);
      if (font2d instanceof CompositeFont) {
        final Field field = CompositeFont.class.getDeclaredField("components");
        field.setAccessible(true);
        PhysicalFont[] physicalFonts = (PhysicalFont[]) field.get(font2d);

        if (physicalFonts != null && physicalFonts.length > 0 && physicalFonts[0] != null) {
          return getFromPhysicalFont(physicalFonts[0]);
        }
      } else if (font2d instanceof PhysicalFont) {
        return getFromPhysicalFont((PhysicalFont) font2d);
      }
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        Log.warn(e);
      }
      //ignore
    }
    return null;
  }

  private static String getFromPhysicalFont(PhysicalFont font2d) {
    try {
      final Field field = PhysicalFont.class.getDeclaredField("platName");
      field.setAccessible(true);
      final Object o = field.get(font2d);
      return o != null ? (String) o : null;
    } catch (NoSuchFieldException e) {
      // ignore
    } catch (IllegalAccessException e) {
      // ignore
    }
    return null;
  }

  private static void appendIfRequested(List<java.awt.Font> fontList, java.awt.Font f, List<String> required,
    boolean bold, boolean italic)
  {
    final String fontName = Util.lower(f.getFontName());
    for (String s : required) {
      if (fontName.equalsIgnoreCase(s) && (fontName.contains("bold") == bold) &&
        (fontName.contains("italic") == italic))
      {
        fontList.add(f);
        return;
      }
    }
  }

  private static class FontConfig {
    public final List<String> myFontDirs = Collections15.arrayList();
    public final List<String> mySerifFamilies = Collections15.arrayList();
    public final List<String> myMonoFamilies = Collections15.arrayList();
    public final List<String> mySansFamilies = Collections15.arrayList();
    public final List<String> mySansBoldFamilies = Collections15.arrayList();

    private final File mySettingsFile;

    public FontConfig(File settingsFile) {
      mySettingsFile = settingsFile;
    }

    public FontConfig load() {
      if (mySettingsFile != null && mySettingsFile.isFile() && mySettingsFile.canRead()) {
        loadFromFile(mySettingsFile);
      }
      loadDefaults();
      return this;
    }

    private void loadFromFile(File file) {
      Properties p = new Properties();
      FileInputStream in = null;
      try {
        in = new FileInputStream(file);
        p.load(in);
        parsePropertyList(p, "font.directories", myFontDirs);
        parsePropertyList(p, "font.sans", mySansFamilies);
        parsePropertyList(p, "font.mono", myMonoFamilies);
        parsePropertyList(p, "font.serif", mySerifFamilies);
        parsePropertyList(p, "font.bold", mySansBoldFamilies);
      } catch (IOException e) {
        Log.warn("cannon load " + file, e);
      } finally {
        IOUtils.closeStreamIgnoreExceptions(in);
      }
    }

    private void loadDefaults() {
      if (mySerifFamilies.isEmpty())
        mySerifFamilies.add("times new roman");
      if (mySansBoldFamilies.isEmpty())
        mySansBoldFamilies.add("arial black");
      if (mySansFamilies.isEmpty())
        mySansFamilies.add("arial");
      if (myMonoFamilies.isEmpty())
        myMonoFamilies.add("courier new");
    }

    private void parsePropertyList(Properties properties, String property, List<String> target) {
      String p = properties.getProperty(property);
      if (p == null || p.length() == 0)
        return;
      StringTokenizer st = new StringTokenizer(p, ";");
      while (st.hasMoreElements()) {
        String elem = st.nextToken().trim();
        if (elem.length() > 0) {
          target.add(elem);
        }
      }
    }

    public boolean hasFontDirectory() {
      return myFontDirs.size() > 0;
    }
  }
}