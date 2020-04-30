package com.almworks.items.gui.meta.schema.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.explorer.qbuilder.filter.EnumNarrower;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

abstract class SingleControlling extends EnumNarrower.Filtering<Pair<LongList, LongList>> implements LoadedEnumNarrower {
  public static final SerializableFeature<LoadedEnumNarrower> FEATURE =
    new SerializableFeature<LoadedEnumNarrower>() {
      @Override
      public LoadedEnumNarrower restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        long issueAttrItem = stream.nextLong();
        long setAttrItem = stream.nextLong();
        if (!stream.isSuccessfullyAtEnd()) return null;
        DBAttribute<Long> issueAttribute = BadUtil.getScalarAttribute(reader, issueAttrItem, Long.class);
        DBAttribute<?> itemAttribute = BadUtil.getAttribute(reader, setAttrItem);
        if (issueAttribute == null || itemAttribute == null || !Long.class.equals(itemAttribute.getScalarClass())) return null;
        switch (itemAttribute.getComposition()) {
        case SCALAR:
          DBAttribute<Long> refAttribute = BadUtil.castScalar(Long.class, itemAttribute);
          if (refAttribute != null) return new SingleReference(issueAttribute, refAttribute);
          break;
        case SET:
        case LIST:
          DBAttribute<? extends Collection<Long>> refsAttribute = BadUtil.castCollectionAttribute(Long.class, itemAttribute);
          if (refsAttribute != null) return new SingleSet(issueAttribute, refsAttribute);
        default:
          LogHelper.error("Unknown composition", itemAttribute.getComposition(), issueAttribute, itemAttribute);
          return null;
        }
        LogHelper.error("Wrong attribute", issueAttribute, itemAttribute);
        return null;
      }

      @Override
      public Class<LoadedEnumNarrower> getValueClass() {
        return LoadedEnumNarrower.class;
      }
    };
  @NotNull
  private final DBAttribute<Long> myIssueAttribute;

  public SingleControlling(@NotNull DBAttribute<Long> issueAttribute) {
    myIssueAttribute = issueAttribute;
  }

  @Override
  protected Pair<LongList, LongList> getNarrowDownData(ItemHypercube cube) {
    Collection<Long> connectionsItems = ItemHypercubeUtils.getIncludedConnections(cube);
    LongList connections = toLongList(connectionsItems);
    LongList controlling = toLongList(cube.getIncludedValues(myIssueAttribute));
    return Pair.create(connections, controlling);
  }

  @Override
  protected boolean isAccepted(ItemKey artifact, @NotNull Pair<LongList, LongList> data, ItemHypercube cube) {
    LongList connections = data.getFirst();
    LongList controlling = data.getSecond();
    if (!checkConnection(artifact, connections)) return false;
    if (controlling == null) return true;
    LoadedItemKey item = Util.castNullable(LoadedItemKey.class, artifact);
    if (item == null) return artifact != null;
    Collection<Long> allowed = getAllowed(item);
    if (allowed == null || allowed.isEmpty()) return true;
    for (Long enumItem : allowed) if (controlling.contains(enumItem)) return true;
    return false;
  }

  @Override
  public boolean isAccepted(ItemHypercube cube, LoadedItemKey value) {
    LongArray included = LongArray.create(Util.NN(cube.getIncludedValues(myIssueAttribute), Collections.<Long>emptySet()));
    LongArray excluded = LongArray.create(Util.NN(cube.getExcludedValues(myIssueAttribute), Collections.<Long>emptySet()));
    Collection<Long> allowed = getAllowed(value);
    return allowed == null || allowed.isEmpty() || ItemHypercubeUtils.matches(allowed, included, excluded);
  }

  @Override
  public boolean isAllowedValue(ItemVersion issue, ItemVersion value) {
    if (!LoadedEnumNarrower.DEFAULT.isAllowedValue(issue, value)) return false;
    Long issueValue = issue.getValue(myIssueAttribute);
    if (issueValue == null || issueValue <= 0) return true;
    LongList allowed = getAllowed(value);
    return allowed.isEmpty() || allowed.contains(issueValue);
  }

  @Override
  public void collectIssueAttributes(Collection<? super DBAttribute<Long>> target) {
    target.add(SyncAttributes.CONNECTION);
    target.add(myIssueAttribute);
  }

  protected abstract Collection<Long> getAllowed(LoadedItemKey item);

  @NotNull
  protected abstract LongList getAllowed(ItemVersion valueItem);

  private boolean checkConnection(ItemKey item, LongList connections) {
    ResolvedItem resolved = Util.castNullable(ResolvedItem.class, item);
    if (connections == null) return true;
    if (resolved == null || connections == null || connections.isEmpty()) return false;
    long connection = resolved.getConnectionItem();
    return connection <= 0 || connections.contains(connection);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    SingleControlling other = Util.castNullable(SingleControlling.class, obj);
    return other != null && Util.equals(myIssueAttribute, other.myIssueAttribute);
  }

  @Override
  public int hashCode() {
    return SingleControlling.class.hashCode() ^ myIssueAttribute.hashCode();
  }

  private static LongList toLongList(Collection<Long> items) {
    if (items == null || items.isEmpty()) return null;
    LongArray result = LongArray.create(items);
    result.sortUnique();
    return result.isEmpty() ? null : result;
  }

  public static class SingleReference extends SingleControlling {
    private final DBAttribute<Long> myRefAttribute;

    public SingleReference(DBAttribute<Long> issueAttribute, DBAttribute<Long> referenceAttribute) {
      super(issueAttribute);
      myRefAttribute = referenceAttribute;
    }

    @Override
    public void collectValueAttributes(Collection<? super DBAttribute<?>> target) {
      target.add(myRefAttribute);
    }

    protected Collection<Long> getAllowed(LoadedItemKey item) {
      Long value = item.getValue(myRefAttribute);
      return value != null && value > 0 ? Collections.singleton(value) : Collections15.<Long>emptySet();
    }

    @NotNull
    @Override
    protected LongList getAllowed(ItemVersion valueItem) {
      Long value = valueItem.getValue(myRefAttribute);
      return value != null && value > 0 ? LongArray.create(value) : LongList.EMPTY;
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this || (super.equals(obj) && (Util.equals(((SingleReference) obj).myRefAttribute, myRefAttribute)));
    }

    @Override
    public int hashCode() {
      return super.hashCode() ^ myRefAttribute.hashCode();
    }
  }

  public static class SingleSet extends SingleControlling {
    private final DBAttribute<? extends Collection<Long>> myReferencesAttribute;

    public SingleSet(DBAttribute<Long> issueAttribute, DBAttribute<? extends Collection<Long>> setAttribute) {
      super(issueAttribute);
      myReferencesAttribute = setAttribute;
    }

    @Override
    public void collectValueAttributes(Collection<? super DBAttribute<?>> target) {
      target.add(myReferencesAttribute);
    }

    protected Collection<Long> getAllowed(LoadedItemKey item) {
      return item.getValue(myReferencesAttribute);
    }

    @NotNull
    @Override
    protected LongList getAllowed(ItemVersion valueItem) {
      return valueItem.getLongSet(myReferencesAttribute);
    }

    @Override
    public boolean equals(Object obj) {
      return obj == this || (super.equals(obj) && (Util.equals(((SingleSet) obj).myReferencesAttribute, myReferencesAttribute)));
    }

    @Override
    public int hashCode() {
      return super.hashCode() ^ myReferencesAttribute.hashCode();
    }
  }
}
