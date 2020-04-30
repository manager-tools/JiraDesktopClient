package com.almworks.items.gui.meta.export;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.Connection;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.schema.export.ExportPolicy;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Util;

import java.util.Collection;

class ItemExportImpl extends ItemExport {
  private final long myOwner;
  private final ExportPolicy myPolicy;
  private final GuiFeaturesManager myFeatures;
  private volatile String myDisplayName;

  public ItemExportImpl(String id, String displayName, long owner, ExportPolicy policy, GuiFeaturesManager features) {
    super(id);
    myDisplayName = displayName;
    myOwner = owner;
    myPolicy = policy;
    myFeatures = features;
  }

  @Override
  public String getDisplayName() {
    return myDisplayName;
  }

  @Override
  public Pair<String, ExportValueType> formatForExport(PropertyMap values, ExportContext context) {
    return myPolicy.export(values, context, myFeatures);
  }

  @Override
  public boolean isExportable(Collection<Connection> conns) {
    for (Connection connection : conns) {
      if (connection == null) continue;
      if (myOwner == connection.getConnectionItem()) return true;
      if (myOwner == connection.getProviderItem()) return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return "EXPORT[" + getId() + " owned:" + myOwner + "] (" + myPolicy + ")";
  }

  public boolean update(String id, String displayName, Long owner, ExportPolicy policy) {
    boolean idChanged = !Util.equals(id, getId());
    boolean ownerChanged = !Util.equals(owner, myOwner);
    boolean policyChanged = !Util.equals(policy, myPolicy);
    if (idChanged || ownerChanged || policyChanged) {
      LogHelper.error("Update not supported",
        idChanged ? "ID changed: " + getId() + " -> " + id : "ID same.",
        ownerChanged ? "Owner changed: " + myOwner + " -> " + owner : "Owner same.",
        policyChanged ? "Policy changed: " + myPolicy + " -> " + policy : "Policy same.");
        return false;
    }
    if (Util.equals(myDisplayName, displayName)) return false;
    myDisplayName = displayName;
    return true;
  }
}
