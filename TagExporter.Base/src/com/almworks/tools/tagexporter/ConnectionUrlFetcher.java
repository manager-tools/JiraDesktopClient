package com.almworks.tools.tagexporter;

import com.almworks.util.collections.Functional;
import com.almworks.util.config.JDOMConfigurator;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

public class ConnectionUrlFetcher {
  /**
   * @return map from connection (U)ID to connection base URL setting present in the specified configuration file. <br>
   * Note that in some versions of Deskzilla/JIRA Client, it contains verbatim user-entered data which may be not equal to the true baseURL (representing a function of it).
   * */
  @NotNull
  public static Map<String, String> fetchConnectionUrls(WorkspaceStructure workArea) throws IOException, JDOMException {
    IOException ioe = null;
    JDOMException jdome = null;
    try {
      return fetchConnectionUrls(workArea.getConfigFile());
    } catch (IOException ex) {
      Log.warn(ex.getMessage());
      ioe = ex;
    } catch (JDOMException ex) {
      Log.warn(ex.getMessage());
      jdome = ex;
    }
    for (File configFile : Arrays.asList(getTempFile(workArea), Functional.first(getBackupFiles(workArea)))) {
      try {
        Map<String, String> urls = fetchConnectionUrls(configFile);
        Log.warn("Using config file " + configFile);
        return urls;
      } catch (Exception ignored) {
        Log.warn(ignored.getMessage());
      }
    }
    if (ioe != null) throw ioe;
    throw jdome;
  }

  /** Logic copied from {@link  com.almworks.util.config.JDOMConfigurator#JDOMConfigurator(File, long, File)} */
  private static File getTempFile(WorkspaceStructure workArea) {
    File configFile = workArea.getConfigFile();
    return new File(configFile.getPath().concat(".tmp"));
  }

  private static List<File> getBackupFiles(WorkspaceStructure workArea) {
    List<File> files = Arrays.asList(workArea.getConfigBackupDir().listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith(JDOMConfigurator.BACKUP_FILE_PREFIX);
      }
    }));
    Collections.sort(files, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
        return -Util.compareLongs(o1.lastModified(), o2.lastModified());
      }
    });
    return files;
  }

  private static Map<String, String> fetchConnectionUrls(File configFile) throws IOException, JDOMException {
    SAXBuilder builder = JDOMUtils.createBuilder();
    return fetchUrls(builder.build(configFile));
  }

  private static Map<String, String> fetchUrls(Document document) {
    final Map<String, String> map = Collections15.hashMap();
    for (Element connection : JDOMUtils.queryPath(document.getRootElement(), "engine", "provider")) {
      String id = connection.getChildText("id");
      String baseUrl = connection.getChildText("baseURL");
      map.put(id, baseUrl);
    }
    return map;
  }
}
