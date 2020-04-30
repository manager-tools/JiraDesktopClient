package com.almworks.items.sync.util.merge;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.ItemDiff;
import com.almworks.items.sync.ItemVersion;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Set;

public class AutoMergeStringSets extends SimpleAutoMerge {
  private final DBAttribute<? extends Collection<? extends String>> myAttribute;

  public AutoMergeStringSets(DBAttribute<? extends Collection<? extends String>> attribute) {
    myAttribute = attribute;
  }

  @Override
  public void resolve(AutoMergeData data) {
    ItemDiff local = data.getLocal();
    Collection<? extends String> trunk = getValue(local.getNewerVersion());
    Collection<? extends String> base = getValue(local.getElderVersion());
    Set<String> added = Collections15.hashSet(trunk);
    added.removeAll(base);
    Set<String> removed = Collections15.hashSet(base);
    removed.removeAll(trunk);
    Set<String> resolution = Collections15.hashSet(getValue(data.getServer().getNewerVersion()));
    resolution.addAll(added);
    resolution.removeAll(removed);
    data.setCompositeResolution(myAttribute, resolution);
  }

  private Collection<? extends String> getValue(ItemVersion version) {
    Collection<? extends String> trunk = version.getValue(myAttribute);
    if (trunk == null) trunk = Collections15.emptyCollection();
    return trunk;
  }
}
