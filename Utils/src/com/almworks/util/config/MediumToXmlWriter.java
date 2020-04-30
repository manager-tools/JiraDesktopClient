package com.almworks.util.config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author dyoma
 */
public final class MediumToXmlWriter {
  private static final String EMPTY_SETTING_ATTR = JDOMReadonlyMedium.EMPTY_SETTING + "=\"\"";
  private final XMLWriter myWriter;
  private final byte[] myEmptySetting;
//  private final StringBuffer myBuffer = new StringBuffer(1024);

  private MediumToXmlWriter(OutputStream stream) {
    myWriter = new XMLWriter(stream);
    myEmptySetting = myWriter.encodeString(EMPTY_SETTING_ATTR);
  }

  private void write(ReadonlyMedium<? extends ReadonlyMedium> medium) throws IOException {
    myWriter.openTag(medium.getName());
    myWriter.newLine();
    writeSettings(medium.getSettings());
    List<? extends ReadonlyMedium> subsets = medium.getSubsets().getAll(null);
    for (int i = 0; i < subsets.size(); i++) {
      ReadonlyMedium child = subsets.get(i);
      write(child);
    }
    myWriter.closeTag();
  }

  public static void writeMedium(ReadonlyMedium<? extends ReadonlyMedium> medium, OutputStream stream)
    throws IOException
  {
    new MediumToXmlWriter(stream).write(medium);
  }

  private void writeSettings(SubMedium<String> settings) throws IOException {
    Collection<String> settingNames = settings.getAllNames();
    for (Iterator<String> iterator = settingNames.iterator(); iterator.hasNext();) {
      String name = iterator.next();
      List<String> values = settings.getAll(name);
      assert !values.isEmpty() : name;
      if (values.isEmpty())
        myWriter.writeTag(name, myEmptySetting);
      else {
        for (int i = 0; i < values.size(); i++) {
          String value = values.get(i);
          if (value.length() == 0)
            myWriter.writeTag(name, myEmptySetting);
          else {
            myWriter.openTag(name);
            myWriter.appendText(value);
            myWriter.closeTag();
          }
        }
      }
    }
  }
}
