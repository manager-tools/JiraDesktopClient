package com.almworks.items.sync.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.util.AttributeMap;
import org.almworks.util.Log;

/**
 * @deprecated should be replaced with {@link com.almworks.items.sync.VersionSource} implementations.
 */
@Deprecated
public enum Branch {
  TRUNK,
  SERVER;

  public boolean isServer() {
    switch (this) {
    case TRUNK: return false;
    case SERVER: return true;
    default: Log.error("Wrong branch " + this); return false;
    }
  }

  public DBAttribute<AttributeMap> getShadow() {
    switch (this) {
    case TRUNK: return null;
    case SERVER: return SyncSchema.DOWNLOAD;
    default: Log.error("Wrong branch " + this); return null;
    }
  }
}
