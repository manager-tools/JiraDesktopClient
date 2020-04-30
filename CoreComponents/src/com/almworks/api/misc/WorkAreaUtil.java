package com.almworks.api.misc;

import com.almworks.util.BadFormatException;
import com.almworks.util.config.JDOMConfigurator;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author dyoma
 */
public class WorkAreaUtil {
  public static ReadonlyConfiguration loadEtcConfiguration(WorkArea workArea, String etcFile, String exceptionMessage) {
    File defaultWorkflow = workArea.getEtcFile(etcFile);
    if (defaultWorkflow != null && defaultWorkflow.isFile() && defaultWorkflow.canRead()) {
      FileInputStream stream = null;
      BufferedInputStream in = null;
      try {
        stream = new FileInputStream(defaultWorkflow);
        in = new BufferedInputStream(stream);
        return JDOMConfigurator.parse(in);
      } catch (IOException e) {
        Log.warn(exceptionMessage, e);
      } catch (BadFormatException e) {
        Log.warn(exceptionMessage, e);
      } finally {
        IOUtils.closeStreamIgnoreExceptions(in);
        IOUtils.closeStreamIgnoreExceptions(stream);
      }
    }
    return null;
  }
}
