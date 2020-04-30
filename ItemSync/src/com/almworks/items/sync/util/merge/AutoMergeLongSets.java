package com.almworks.items.sync.util.merge;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.ItemDiff;
import com.almworks.util.collections.LongSet;
import org.almworks.util.ArrayUtil;

import java.util.Collection;

public class AutoMergeLongSets extends SimpleAutoMerge {
  private final DBAttribute<? extends Collection<? extends Long>>[] myAttribute;

  public AutoMergeLongSets(DBAttribute<? extends Collection<? extends Long>> ... attributes) {
    myAttribute = ArrayUtil.arrayCopy(attributes);
  }

  @Override
  public void resolve(AutoMergeData data) {
    for (DBAttribute<? extends Collection<? extends Long>> attribute : myAttribute) {
      mergeSets(data, attribute);
    }
  }

  public static void mergeSets(AutoMergeData data, DBAttribute<? extends Collection<? extends Long>> attribute) {
    ItemDiff local = data.getLocal();
    LongList trunk = local.getNewerVersion().getLongSet(attribute);
    LongList base = local.getElderVersion().getLongSet(attribute);
    LongSet added = LongSet.setDifference(trunk, base);
    LongSet removed = LongSet.setDifference(base, trunk);
    LongSet resolution = LongSet.copy(data.getServer().getNewerVersion().getLongSet(attribute));
    resolution.addAll(added);
    resolution.removeAll(removed);
    data.setCompositeResolution(attribute, resolution);
  }
}
