
package com.almworks.platform;

import com.almworks.util.files.ExtensionFileFilter;
import org.almworks.util.Log;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * :todoc:
 *
 * @author sereda
 */
class ComponentConfiguration {
  private final File myComponentJarDir;
  private final File myComponentCacheXmlDir;

  private Set<File> myUsedJarFiles = new HashSet<File>();
  private Set<String> myComponentClassNames = new HashSet<String>();

  private ComponentConfiguration(File jarDir, File xmlDir) {
    myComponentJarDir = jarDir;
    myComponentCacheXmlDir = xmlDir;
  }

  public Set<File> getUsedJarFiles() {
    return Collections.unmodifiableSet(myUsedJarFiles);
  }

  public Set<String> getComponentClassNames() {
    return Collections.unmodifiableSet(myComponentClassNames);
  }

  private void build() {
    File[] files = myComponentJarDir.listFiles(new ExtensionFileFilter("jar", false));
    if (files == null) {
      Log.warn("no jars in component directory (" + myComponentJarDir + ")");
      return;
    }
    for (File file : files) {
      LibraryJar libJar = new LibraryJar(file, myComponentCacheXmlDir);
      if (!libJar.checkJar())
        continue;

      assert Log.debug("configuring " + file);
      Set<String> components = libJar.getComponents();
      if (components.size() == 0) {
        Log.warn("jar (" + file.getName() + ") doesn't contain components");
        continue;
      }

      myUsedJarFiles.add(file);
      myComponentClassNames.addAll(components);
    }
  }

  public static ComponentConfiguration buildConfiguration(File jarDir, File xmlDir) {
    ComponentConfiguration configuration = new ComponentConfiguration(jarDir, xmlDir);
    configuration.build();
    return configuration;
  }

  public void addDebugComponents(InputStream stream) throws IOException, JDOMException {
    if (stream == null)
      return;
    Set<String> classNames = XmlHintFiles.readHintStream(stream);
    assert Log.debug("adding " + classNames.size() + " debug components");
    myComponentClassNames.addAll(classNames);
  }
}
