package com.almworks.explorer.loader.meta;

import com.almworks.api.application.MetaInfo;
import com.almworks.items.api.DBReader;
import org.almworks.util.Log;

/**
 * @author dyoma
 */
public class IntrospectionSupport {
  public MetaInfo getMetaInfo(long artifact, DBReader reader) {
    MetaInfo metaInfo = MetaInfo.REGISTRY.getMetaInfo(artifact, reader);
    if (metaInfo != null)
      return metaInfo;

    assert false : artifact;
    Log.error("?" + artifact);
    return null;
  }
}
