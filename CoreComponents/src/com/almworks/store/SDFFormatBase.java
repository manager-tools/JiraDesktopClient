package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.io.BufferedDataInput;

import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
abstract class SDFFormatBase implements SDFFormat {
  public int hashCode() {
    return getClass().hashCode() * 23;
  }

  public boolean equals(Object o) {
    if (o == null)
      return false;
    return getClass().equals(o.getClass());
  }

  public int readFormatVersion(BufferedDataInput input) throws IOException, FileFormatException {
    return getFormatVersion(); // no version in file
  }

  protected void checkFeatures(StoreFeature[] requestedFeatures, StoreFeature[] supportedFeatures) {
    for (int i = 0; i < requestedFeatures.length; i++) {
      StoreFeature requestedFeature = requestedFeatures[i];
      int j = 0;
      for (; j < supportedFeatures.length; j++) {
        StoreFeature supportedFeature = supportedFeatures[j];
        if (supportedFeature == requestedFeature)
          break;
      }
      if (j == supportedFeatures.length)
        throw new UnsupportedOperationException("feature " + requestedFeature + " is not supported");
    }
  }
}
