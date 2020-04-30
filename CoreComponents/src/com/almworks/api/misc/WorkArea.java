package com.almworks.api.misc;

import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

public interface WorkArea {
  Role<WorkArea> APPLICATION_WORK_AREA = Role.role("application.workArea", WorkArea.class);

  // etc files
  String ETC_WELCOME_HTML = "welcome.html";
  String ETC_WORKFLOW_XML = "workflow.xml";                             
  String ETC_HTML_EXPORT_CSS = "bugzilla-bugs.css";
  String ETC_DUPLICATION_PUBLIC_BUG_SETTINGS = "duppub.properties";
  String ETC_EXPORT_PROPERTIES = "export.properties";
  String ETC_AUTO_ASSIGN_XML = "autoAssign.xml";

  File getRootDir();

  File getHomeDir();

  File getConfigFile();

  File getDatabaseDir();

  File getStorerDir();

  File getDatabaseBackupDir();

  File getDownloadDir();

  File getUploadDir();

  File getConfigBackupDir();


  /**
   * Unlocks work area. Should be called right before application exits.
   */
  void shutdown();

  @Nullable
  File getLockFile();

  void announceApiPort(int port);

  File getLogDir();

  File getEtcDir();

  @Nullable
  File getEtcFile(String file);

  File getComponentHintsDir();

  /**
   * Searches file in "etc" folder under installation dir and workspace dir.
   */
  @Nullable
  File getEtcCollectionFile(String collectionFolder, String fileName);

  @Nullable
  Collection<File> getEtcCollectionFiles(String collectionFolder);

  File getInstallationEtcDir();

  /** @return Directory for temporary storage or null if it could not be created. */
  @Nullable
  File getTempDir();

  /**
   * Returns path to workspace's auxiliary directory denoted by the relative path.
   * If the directory does not exists - it is created.
   * @param relativePath path relative to root auxiliary directory. If relative path contains several directories, they must be separated with '/'
   * @return path to requested auxiliary directory
   */
  @NotNull
  File getAuxDir(String relativePath);
}
