package com.almworks.util.config;

import com.almworks.util.BadFormatException;
import com.almworks.util.files.FileUtil;
import com.almworks.util.tests.BaseTestCase;

import java.io.File;
import java.io.IOException;

public class JDOMSaveDurabilityTests extends BaseTestCase {
  private JDOMConfigurator myConfigurator;
  private File myConfigFile;
  private File myConfigFileTmp;
  private static final String CONFIG = "<r><s1 empty=\"\"/><s2><v>x</v></s2></r>";

  protected void setUp() throws Exception {
    super.setUp();
    myConfigFile = createFileName();
    myConfigFileTmp = new File(myConfigFile.getPath().concat(".tmp"));
  }

  protected void tearDown() throws Exception {
    if (myConfigurator != null) {
      myConfigurator.stop();
      myConfigurator = null;
    }
    Thread.sleep(100);
    if (myConfigFile != null) {
      myConfigFile.delete();
      myConfigFile = null;
    }
    if (myConfigFileTmp != null) {
      myConfigFileTmp.delete();
      myConfigFileTmp = null;
    }
    super.tearDown();
  }

  public void testLoadingFromBackup() throws Exception {
    FileUtil.writeFile(myConfigFileTmp, CONFIG);
    FileUtil.writeFile(myConfigFile, CONFIG.substring(0, 10));
    myConfigurator = new JDOMConfigurator(myConfigFile, 200);
    testConfig(myConfigurator.getConfiguration());
  }

  public void testLoadingFromBackupFails() throws Exception {
    FileUtil.writeFile(myConfigFileTmp, CONFIG.substring(0, 10));
    assertTrue(myConfigFile.createNewFile());
    try {
      myConfigurator = new JDOMConfigurator(myConfigFile, 200);
      fail("created config from a broken temp file");
    } catch (BadFormatException e) {
      // normal
    }
  }

  public void testLoadingMainFileWhenBadBackupExists() throws Exception {
    FileUtil.writeFile(myConfigFile, CONFIG);
    FileUtil.writeFile(myConfigFileTmp, CONFIG.substring(0, 10));
    myConfigurator = new JDOMConfigurator(myConfigFile, 200);
    testConfig(myConfigurator.getConfiguration());
  }

  public void testFailedSaveDurability() throws IOException, BadFormatException, InterruptedException {
    FileUtil.writeFile(myConfigFile, CONFIG);
    myConfigurator = new JDOMConfigurator(myConfigFile, 200);
    myConfigurator.start();

    // todo - lock that works on Unix
    //FileChannel c = new FileOutputStream(myConfigFile).getChannel();
    //FileLock lock = c.lock();
    //new FileInputStream(myConfigFile);

    myConfigurator.getConfiguration().setSetting("haba", "haba");

    // let it attempt to save
    Thread.sleep(300);

    // emulate exit
    try {
      myConfigurator.save();
      //fail("successfully saved when there's file lock");
    } catch (IOException e) {
      // normal
    }

    myConfigurator.stop();

    //lock.release();
    //c.close();

    myConfigurator = new JDOMConfigurator(myConfigFile, 200);
    myConfigurator.start();

    Configuration configuration = myConfigurator.getConfiguration();
    testConfig(configuration);
    assertEquals("haba", configuration.getSetting("haba", null));
  }

  public void testRestoreMainFile() throws IOException, BadFormatException, InterruptedException {
    FileUtil.writeFile(myConfigFileTmp, CONFIG);
    FileUtil.writeFile(myConfigFile, CONFIG.substring(0, 10));
    myConfigurator = new JDOMConfigurator(myConfigFile, 200);
    testConfig(myConfigurator.getConfiguration());
    myConfigurator.start();
    Thread.sleep(800);
    assertTrue(myConfigFile.isFile());
    String content = FileUtil.readFile(myConfigFile);
    content = content.replaceAll("\\s", "");
    assertEquals(CONFIG.replaceAll("\\s", ""), content);
    assertFalse(myConfigFileTmp.exists());
    myConfigurator.stop();
    Thread.sleep(400);
  }

  private void testConfig(Configuration configuration) {
    assertNotNull(configuration);
    assertNotNull(configuration.getSubset("s1"));
    assertNotNull(configuration.getSubset("s2"));
  }
}
