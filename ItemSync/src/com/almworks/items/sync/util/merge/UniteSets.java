package com.almworks.items.sync.util.merge;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.ItemAutoMerge;
import com.almworks.util.collections.LongSet;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.List;

public class UniteSets extends SimpleAutoMerge {
  private final DBAttribute<? extends Collection<? extends Long>> myAttribute;

  public UniteSets(DBAttribute<? extends Collection<? extends Long>> attribute) {
    myAttribute = attribute;
  }

  @Override
  public void resolve(AutoMergeData data) {
    LongList newLocal = data.getLocal().getNewerVersion().getLongSet(myAttribute);
    LongList newServer = data.getServer().getNewerVersion().getLongSet(myAttribute);
    LongList resolution;
    if (newLocal.isEmpty() || newServer.isEmpty()) resolution = newLocal.isEmpty() ? newServer : newLocal;
    else {
      LongSet set = new LongSet();
      set.addAll(newLocal);
      set.addAll(newServer);
      resolution = set;
    }
    data.setCompositeResolution(myAttribute, resolution);
  }

  public static ItemAutoMerge create(DBAttribute<? extends Collection<? extends Long>> ... attributes) {
    if (attributes.length == 1) return new UniteSets(attributes[0]);
    else {
      List<ItemAutoMerge> list = Collections15.arrayList();
      for (DBAttribute<? extends Collection<? extends Long>> attribute : attributes) list.add(new UniteSets(attribute));
      return new CompositeMerge(list);
    }
  }
}
