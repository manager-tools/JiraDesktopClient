package com.almworks.jira.provider3.links;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.schema.LinkType;
import com.almworks.util.collections.LongSet;
import org.almworks.util.TypedKey;

import java.util.Map;

public class IsotropicTypes {
  private static final TypedKey<IsotropicTypes> CACHE_KEY = TypedKey.create("isotropicTypes");
  
  private final LongSet myIsotropic = new LongSet();
  private final LongSet myNotIsotropic = new LongSet();
  private final VersionSource mySource;

  private IsotropicTypes(VersionSource source) {
    mySource = source;
  }

  public static IsotropicTypes getInstance(VersionSource source) {
    return getInstance(source.getReader());
  }

  public static IsotropicTypes getInstance(DBReader reader) {
    Map map = reader.getTransactionCache();
    IsotropicTypes instance = CACHE_KEY.getFrom(map);
    if (instance == null) {
      instance = new IsotropicTypes(BranchSource.trunk(reader));
      CACHE_KEY.putTo(map, instance);
    }
    return instance;
  }
  
  public boolean isIsotropicType(Long type) {
    if (type == null) return false;
    if (myNotIsotropic.contains(type)) return false;
    if (!myIsotropic.contains(type)) {
      if (isIsotropic(mySource.forItem(type))) myIsotropic.add(type);
      else {
        myNotIsotropic.add(type);
        return false;
      }
    }
    return true;
  }

  private boolean isIsotropic(ItemVersion linkType) {
    String inward = linkType.getValue(LinkType.INWARD_DESCRIPTION);
    String outward = linkType.getValue(LinkType.OUTWARD_DESCRIPTION);
    return inward != null && outward != null && inward.equals(outward);
  }
}
