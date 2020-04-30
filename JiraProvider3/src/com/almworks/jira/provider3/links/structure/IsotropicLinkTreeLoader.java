package com.almworks.jira.provider3.links.structure;

import com.almworks.api.application.LoadedItem;
import com.almworks.explorer.TableTreeStructure;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.links.LoadedLink2;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class IsotropicLinkTreeLoader implements SerializableFeature<TableTreeStructure> {
  public static IsotropicLinkTreeLoader INSTANCE = new IsotropicLinkTreeLoader();

  private IsotropicLinkTreeLoader() {}

  @Override
  public TableTreeStructure restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
    long linkType = stream.nextLong();
    if (linkType <= 0 || !stream.isSuccessfullyAtEnd()) return null;
    return new Structure(linkType);
  }

  @Override
  public Class<TableTreeStructure> getValueClass() {
    return TableTreeStructure.class;
  }

  public static ScalarSequence createSequence(long linkType) {
    return new ScalarSequence.Builder().append(IssuesLinkTreeLayout.FEATURE_ISOTROPIC_LOADER).appendLong(linkType).create();
  }

  private static class Structure extends TableTreeStructure {
    private final long myLinkType;

    public Structure(long linkType) {
      myLinkType = linkType;
    }

    @Override
    public Set<Long> getNodeParentKeys(LoadedItem element) {
      List<LoadedLink2> loadedLinks = IssuesLinkTreeLayout.getLinks(element);
      long thisIssue = element.getItem();
      HashSet<Long> result = Collections15.hashSet();
      for (LoadedLink2 link : loadedLinks) {
        if (myLinkType != link.getType()) continue;
        long opposite = link.getOppositeIssue();
        if (opposite <= thisIssue) continue;
        result.add(opposite);
      }
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      Structure other = Util.castNullable(Structure.class, obj);
      return other != null && myLinkType == other.myLinkType;
    }

    @Override
    public int hashCode() {
      return (int) myLinkType ^ Structure.class.hashCode();
    }
  }
}
