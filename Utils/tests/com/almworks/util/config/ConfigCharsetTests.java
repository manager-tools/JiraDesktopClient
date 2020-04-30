package com.almworks.util.config;

import com.almworks.util.BadFormatException;
import com.almworks.util.files.FileUtil;
import com.almworks.util.tests.BaseTestCase;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;

public class ConfigCharsetTests extends BaseTestCase {
  private static final String ARIGATO = "Valueclick\u306e\u5e83\u544a\u30d0\u30ca\u30fc\u304c\u898b\u3048\u306a\u3044";

  public void testKeepMultibyteChars() throws BadFormatException, IOException {
    Configuration c = new Configuration(new JDOMMedium(new Element("root")), MediumWatcher.BLIND);
    c.setSetting("test", ARIGATO);
    String serialized = JDOMConfigurator.writeConfiguration(c);
    ReadonlyConfiguration restoredConfig = JDOMConfigurator.parse(serialized);
    String setting = restoredConfig.getSetting("test", null);
    assertEquals(ARIGATO, setting);
  }

  public void testKeepMultibyteCharsInFile() throws IOException, BadFormatException {
    File configFile = createFileName();
    FileUtil.writeFile(configFile, "<config></config>");
    JDOMConfigurator configurator = new JDOMConfigurator(configFile);
    Configuration config = configurator.getConfiguration();
    config.setSetting("test", ARIGATO);
    configurator.save();

    configurator = new JDOMConfigurator(configFile);
    config = configurator.getConfiguration();
    assertEquals(ARIGATO, config.getSetting("test", null));
  }
}
