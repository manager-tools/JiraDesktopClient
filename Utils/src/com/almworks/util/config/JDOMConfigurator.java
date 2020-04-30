package com.almworks.util.config;

import com.almworks.util.BadFormatException;
import com.almworks.util.CantGetHereException;
import com.almworks.util.Pair;
import com.almworks.util.exec.SeparateEventQueueGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.io.IOUtils;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import util.concurrent.SynchronizedBoolean;
import util.concurrent.SynchronizedLong;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


/**
 * @author : Dyoma
 */
public class JDOMConfigurator implements MediumWatcher {
  public static final String BACKUP_FILE_PREFIX = "config_backup_";
  private static final int MAXIMUM_BACKUP_FILES = 10;
  private static final long CHANGES_TO_BACKUP = 100000;

  private static final long DEFAULT_SAVE_PAUSE = 2500;
  private static final int SAVE_ATTEMPTS = 3;

  private final File myConfigFile;
  private final File myConfigFileTmp;
  private final File myBackupDir;
  private final SynchronizedBoolean myDirty = new SynchronizedBoolean(false);
  private final Configuration myConfiguration;
  private final MapMedium myRootMedium;

  private final SynchronizedBoolean myStopped = new SynchronizedBoolean(false);
  private final SeparateEventQueueGate myGate = new SeparateEventQueueGate(null, "autosave", true);
  private final Bottleneck mySaveBottleneck;
  private final SynchronizedLong myChangeCount = new SynchronizedLong(Long.MAX_VALUE >> 1);

  public JDOMConfigurator(File configFile) throws IOException, BadFormatException {
    this(configFile, null);
  }

  public JDOMConfigurator(File configFile, long savePause) throws IOException, BadFormatException {
    this(configFile, savePause, null);
  }

  public JDOMConfigurator(File configFile, File backupDir) throws IOException, BadFormatException {
    this(configFile, DEFAULT_SAVE_PAUSE, backupDir);
  }

  JDOMConfigurator(File configFile, long savePause, File backupDir) throws IOException, BadFormatException {
    mySaveBottleneck = new Bottleneck(savePause, myGate, new Runnable() {
      public void run() {
        try {
          save();
        } catch (IOException e) {
          Log.debug("cannot save config", e);
        }
      }
    });
    myConfigFile = configFile;
    myConfigFileTmp = new File(configFile.getPath().concat(".tmp"));
    myBackupDir = backupDir;

    Element rootElement = readConfig().getRootElement();
    myRootMedium = new MapMedium(null, rootElement.getName());
    myConfiguration = SynchronizedConfiguration.createSynchonized(myRootMedium, this);
    ConfigurationUtil.copyTo(new ReadonlyConfiguration(new JDOMMedium(rootElement)), myConfiguration);
  }

  public static ReadonlyConfiguration parse(ClassLoader classLoader, String resource) {
    try {
      final InputStream stream = classLoader.getResourceAsStream(resource);
      if (stream == null)
        throw new Failure("Resource not found: " + resource);
      try {
        return parse(stream);
      } finally {
        IOUtils.closeStreamIgnoreExceptions(stream);
      }
    } catch (Exception e) {
      throw new Failure(e);
    }
  }

  public static ReadonlyConfiguration parse(InputStream stream) throws IOException, BadFormatException {
    try {
      return new ReadonlyConfiguration(JDOMReadonlyMedium.createReadonly(stream));
    } catch (JDOMException e) {
      throw new BadFormatException("cannot parse", e);
    }
  }

  public static ReadonlyConfiguration parse(Reader reader) throws IOException, BadFormatException {
    try {
      return new ReadonlyConfiguration(JDOMReadonlyMedium.createReadonly(reader));
    } catch (JDOMException e) {
      throw new BadFormatException("cannot parse", e);
    }
  }

  public static ReadonlyConfiguration parse(String xml) throws IOException, BadFormatException {
    return parse(new StringReader(xml));
  }

  public static void storeDocument(Document documentCopy, OutputStream stream) throws IOException {
    XMLOutputter xmlOutputter = createOutputter();
    xmlOutputter.output(documentCopy, stream);
  }

  public static String storeDocumentToString(Document document) {
    return createOutputter().outputString(document);
  }

  private static XMLOutputter createOutputter() {
    Format format = Format.getPrettyFormat();
    format.setIndent("  ");
    format.setOmitDeclaration(true);
    XMLOutputter xmlOutputter = new XMLOutputter(format);
    return xmlOutputter;
  }

  public static String writeConfiguration(ReadonlyConfiguration config) {
    Document document = new Document(new Element(config.getName()));
    Configuration copy = new Configuration(new JDOMMedium(document.getRootElement()), null);
    ConfigurationUtil.copyTo(config, copy);
    return storeDocumentToString(document);
  }

  public void onMediumUpdated() {
    setDirty();
  }

  private void setDirty() {
    if (myStopped.get()) {
      Log.warn("dirty config after stop");
      return;
    }
    myChangeCount.increment();
    myDirty.set(true);
    myGate.ensureStarted();
    mySaveBottleneck.requestDelayed();
  }

  public Configuration getConfiguration() {
    return myConfiguration;
  }

  public synchronized void save() throws IOException {
    if (!myDirty.get())
      return;
    if (!checkDiskSpace())
      return;
    saveConfigFile();
    backupConfigFile();
    myDirty.set(false);
  }

  private void backupConfigFile() {
    if (myBackupDir == null)
      return;
    boolean backupNeeded;
    synchronized (myChangeCount.getLock()) {
      long changes = myChangeCount.get();
      backupNeeded = changes > CHANGES_TO_BACKUP;
      if (backupNeeded)
        myChangeCount.set(0);
    }
    if (!backupNeeded)
      return;
    if (!myBackupDir.isDirectory()) {
      Log.warn("cannot backup config file to " + myBackupDir);
    }

    boolean success = writeBackupConfigFile();
    if (success) {
      try {
        removeOlderBackupFiles();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }

  private void removeOlderBackupFiles() throws InterruptedException {
    File[] files = myBackupDir.listFiles(new FileFilter() {
      public boolean accept(File pathname) {
        if (!pathname.isFile())
          return false;
        return pathname.getName().startsWith(BACKUP_FILE_PREFIX);
      }
    });
    if (files != null && files.length > MAXIMUM_BACKUP_FILES) {
      List<Pair<File, Long>> sorted = Collections15.arrayList();
      for (File file : files) {
        sorted.add(Pair.create(file, file.lastModified()));
      }
      Collections.sort(sorted, new Comparator<Pair<File, Long>>() {
        public int compare(Pair<File, Long> o1, Pair<File, Long> o2) {
          long time1 = o1.getSecond();
          long time2 = o2.getSecond();
          if (time1 < time2)
            return -1;
          else if (time1 > time2)
            return 1;
          else
            return 0;
        }
      });

      int toDelete = sorted.size() - MAXIMUM_BACKUP_FILES;
      assert toDelete > 0 && toDelete < sorted.size();
      for (int i = 0; i < toDelete; i++) {
        FileUtil.deleteFile(sorted.get(i).getFirst(), true);
      }
    }
  }

  private boolean writeBackupConfigFile() {
    String baseSuffix = new SimpleDateFormat("yyMMdd_HHmm").format(new Date());
    File backupFile = null;
    for (int i = 0; i < 100; i++) {
      String suffix;
      suffix = i == 0 ? baseSuffix : (i < 10 ? baseSuffix + "_0" + i : baseSuffix + "_" + i);
      File f = new File(myBackupDir, BACKUP_FILE_PREFIX + suffix + ".xml");
      if (!f.exists()) {
        backupFile = f;
        break;
      }
    }

    if (backupFile == null) {
      Log.warn("cannot write backup file");
      return false;
    }

    Log.debug("writing backup config file " + backupFile.getName());
    try {
      FileUtil.copyFile(myConfigFile, backupFile);
    } catch (IOException e) {
      Log.warn("cannot write backup file " + backupFile, e);
      return false;
    }

    return true;
  }

  private void saveConfigFile() throws IOException {
    int attempt = 0;
    while (true) {
      try {
        writeConfigTempFile();
        renameTempFileToConfigFile();

        // success!
        break;
      } catch (IOException e) {
        if (++attempt >= SAVE_ATTEMPTS)
          throw e;
        Log.debug("first attempt to write config failed", e);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }

  private void renameTempFileToConfigFile() throws IOException, InterruptedException {
    if (myConfigFile.exists()) {
      FileUtil.deleteFile(myConfigFile, false);
    }
    FileUtil.renameTo(myConfigFileTmp, myConfigFile, 5);
  }

  private void writeConfigTempFile() throws IOException {
    OutputStream out = null;
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(myConfigFileTmp);
      out = new BufferedOutputStream(fos);
      MediumToXmlWriter.writeMedium(myRootMedium, out);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(out);
      if (fos != null) {
        try {
          FileDescriptor fd = fos.getFD();
          fd.sync();
        } catch (IOException e) {
          // ignore
        }
      }
      IOUtils.closeStreamIgnoreExceptions(fos);
    }
  }

  private boolean checkDiskSpace() {
    // todo will check in Mustang
    return true;
  }

  public void start() {
    setDirty();
  }

  public void stop() {
    if (myStopped.commit(false, true))
      myGate.stop();
  }

  private synchronized Document readConfig() throws IOException, BadFormatException {
    Exception mainFileException = null;
    try {
      return readConfigFromFile(myConfigFile);
    } catch (Exception e) {
      mainFileException = e;
    }
    if (!myConfigFileTmp.isFile())
      rethrow(mainFileException);
    Log.debug("trying to restore configuration from a temporary file");
    try {
      Document document = readConfigFromFile(myConfigFileTmp);
      return document;
    } catch (Exception e) {
      Log.debug("cannot read temporary file", e);
    }
    rethrow(mainFileException);
    throw new CantGetHereException();
  }

  private void rethrow(Exception e) throws IOException, BadFormatException {
    if (e != null) {
      if (e instanceof IOException)
        throw (IOException) e;
      if (e instanceof BadFormatException)
        throw (BadFormatException) e;
    }
    throw new CantGetHereException("e=" + e);
  }

  private Document readConfigFromFile(File file) throws IOException, BadFormatException {
    FileInputStream fis = null;
    BufferedInputStream in = null;
    try {
      fis = new FileInputStream(file);
      in = new BufferedInputStream(fis);
      SAXBuilder builder = JDOMUtils.createBuilder();
      try {
        return builder.build(in);
      } catch (JDOMException e) {
        throw badFormatException(file, e);
      }
    } finally {
      IOUtils.closeStreamIgnoreExceptions(in);
      IOUtils.closeStreamIgnoreExceptions(fis);
    }
  }

  private static BadFormatException badFormatException(File configFile, JDOMException e) {
    return new BadFormatException("cannot parse " + configFile, e);
  }
}
