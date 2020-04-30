package com.almworks.jira.provider3.links.structure;

import com.almworks.api.application.LoadedItem;
import com.almworks.explorer.TableTreeStructure;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.links.LoadedLink2;
import com.almworks.jira.provider3.links.structure.ui.AnisotropicLinkStructure;
import com.almworks.util.Break;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AnisotropicLinkStructureLoader implements SerializableFeature<AnisotropicLinkStructureLoader.Structure> {
  public static final AnisotropicLinkStructureLoader INSTANCE = new AnisotropicLinkStructureLoader();

  private AnisotropicLinkStructureLoader() {}

  @Override
  public Structure restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
    try {
      return doRestore(stream);
    } catch (Break error) {
      String what = error.getMessage();
      if (!what.isEmpty()) {
        LogHelper.error("No", what);
      }
      return null;
    }
  }

  private static Structure doRestore(ByteArray.Stream stream) throws Break {
    long linkType = stream.nextLong();
    Break.breakIf(linkType <= 0, "link type");
    boolean outward = stream.nextBoolean();
    Break.breakIf(!stream.isSuccessfullyAtEnd(), "");
    return createStructure(linkType, outward);
  }

  public static Structure createStructure(long linkType, boolean outward) {
    return new Structure(linkType, outward);
  }

  public static ScalarSequence createSequence(long linkType, boolean outward) {
    return new ScalarSequence.Builder()
      .append(IssuesLinkTreeLayout.FEATURE_ANISOTROPIC_LOADER)
      .appendLong(linkType)
      .append(outward)
    .create();
  }

  @Override
  public Class<Structure> getValueClass() {
    return Structure.class;
  }

  static class Structure extends TableTreeStructure implements AnisotropicLinkStructure {
    private final long myLinkTypeItem;
    private final boolean myOutward;

    public Structure(long linkTypeItem, boolean outward) {
      if (linkTypeItem <= 0L) throw new IllegalArgumentException();
      myLinkTypeItem = linkTypeItem;
      myOutward = outward;
    }

    @Override
    public long getLinkTypeItem() {
      return myLinkTypeItem;
    }

    @Override
    public boolean isOutward() {
      return myOutward;
    }

    @Override
    public Set<Long> getNodeParentKeys(LoadedItem element) {
      List<LoadedLink2> loadedLinks = IssuesLinkTreeLayout.getLinks(element);
      long thisIssue = element.getItem();
      HashSet<Long> result = Collections15.hashSet();
      for (LoadedLink2 link : loadedLinks) {
        if (link.getType() != myLinkTypeItem) continue;
        if (link.getOutward() == myOutward) continue;
        long opposite = link.getOppositeIssue();
        if (opposite == thisIssue) continue;
        result.add(opposite);
      }
      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      Structure structure = (Structure) o;

      if (myLinkTypeItem != structure.myLinkTypeItem)
        return false;
      if (myOutward != structure.myOutward)
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = (int) (myLinkTypeItem ^ (myLinkTypeItem >>> 32));
      result = 31 * result + (myOutward ? 1 : 0);
      return result;
    }
  }
}
