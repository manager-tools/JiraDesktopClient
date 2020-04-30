package com.almworks.jira.provider3.links.actions;

import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.util.SyncAttributes;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.jira.provider3.links.IsotropicTypes;
import com.almworks.jira.provider3.schema.Link;
import com.almworks.jira.provider3.schema.LinkType;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.ImageBasedDecorator2;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DirectionalLinkType {
  public static final CanvasRenderer<DirectionalLinkType> RENDERER = new CanvasRenderer<DirectionalLinkType>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, DirectionalLinkType item) {
      if (item == null) return;
      canvas.appendText(item.myDescription);
    }
  };
  private final long myLinkType;
  private final boolean myOutward;
  private final String myDescription;

  public DirectionalLinkType(long linkType, boolean outward, String description) {
    myLinkType = linkType;
    myOutward = outward;
    myDescription = description;
  }

  public String toString() {
    return myDescription;
  }

  public static AListModel<DirectionalLinkType> createModel(Lifespan life, GuiFeaturesManager manager, long connection) {
    EnumTypesCollector.Loaded linkTypes = manager.getEnumTypes().getType(LinkType.ENUM_TYPE);
    if (linkTypes == null) {
      LogHelper.error("Missing link types");
      return AListModel.EMPTY;
    }
    ItemHypercubeImpl cube = new ItemHypercubeImpl();
    ItemHypercubeUtils.adjustForConnection(cube, connection);
    AListModel<LoadedItemKey> model = linkTypes.getValueModel(life, cube);
    ImageBasedDecorator2<LoadedItemKey, DirectionalLinkType> decorator =
      new ImageBasedDecorator2<LoadedItemKey, DirectionalLinkType>(model) {
        @Override
        protected List<? extends DirectionalLinkType> createImage(LoadedItemKey linkType, int sourceIndex,
          boolean update)
        {
          Integer id = linkType.getValue(LinkType.ID);
          long typeItem = linkType.getItem();
          if (id == null || typeItem <= 0) return Collections15.emptyList();
          String outName = linkType.getValue(LinkType.OUTWARD_DESCRIPTION);
          String inName = linkType.getValue(LinkType.INWARD_DESCRIPTION);
          List<DirectionalLinkType> result = Collections15.arrayList(2);
          if (outName != null)
            result.add(new DirectionalLinkType(typeItem, true, outName));
          if (inName != null && !inName.equals(outName))
            result.add(new DirectionalLinkType(typeItem, false, inName));
          return result;
        }
      };
    decorator.attach(life);
    return decorator.getDecoratedImage();
  }

  @Nullable
  public static DirectionalLinkType load(ItemVersion linkType, boolean outward) {
    if (linkType == null) return null;
    String name = Util.NN(linkType.getValue(outward ? LinkType.OUTWARD_DESCRIPTION : LinkType.INWARD_DESCRIPTION)).trim();
    if (name.isEmpty()) return null;
    return new DirectionalLinkType(linkType.getItem(), outward, name);
  }

  @Override
  public int hashCode() {
    return (int)myLinkType + (myOutward ? 0 : 1);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    DirectionalLinkType other = Util.castNullable(DirectionalLinkType.class, obj);
    return other != null && myLinkType == other.myLinkType && myOutward == other.myOutward;
  }

  public String getDescription() {
    return myDescription;
  }

  public static LongList queryLinks(ItemVersion issue, ItemAttribute master, long linkType) {
    return DatabaseUnwrapper.query(issue.getReader(),
      master.queryEqual(issue.getItem()).and(DPEquals.create(Link.LINK_TYPE, linkType)))
      .copyItemsSorted();
  }

  public static LongList mapOpposites(DBReader reader, LongList links, ItemAttribute oppositeMaster) {
    if (links == null || links.isEmpty()) return LongList.EMPTY;
    LongArray result = new LongArray(links.size());
    for (int i = 0; i < links.size(); i++) {
      Long oppositeIssue = oppositeMaster.getValue(reader, links.get(i));
      result.add(oppositeIssue != null && oppositeIssue > 0 ? oppositeIssue : 0);
    }
    return result;
  }

  private static ItemAttribute getThisMaster(boolean outward) {
    return outward ? Link.SOURCE : Link.TARGET;
  }

  private static ItemAttribute getOppositeMaster(boolean outward) {
    return outward ? Link.TARGET : Link.SOURCE;
  }

  public void createLinks(CommitContext issueContext, LongList oppositeIssues) {
    createLinks(issueContext, myOutward, myLinkType, oppositeIssues);
  }

  public long getType() {
    return myLinkType;
  }

  public boolean getOutward() {
    return myOutward;
  }

  public static void createLinks(CommitContext issueContext, boolean outward, long linkType, LongList oppositeIssues) {
    ItemVersion issue = issueContext.readTrunk();
    Long connection = issue.getValue(SyncAttributes.CONNECTION);
    if (connection == null || connection <= 0) {
      LogHelper.error("Missing connection", issueContext);
      return;
    }
    DBReader reader = issueContext.getReader();
    LongArray links = LongArray.copy(queryLinks(issue, getThisMaster(outward), linkType));
    links.sortUnique();
    LongArray existingOpposites = LongArray.copy(mapOpposites(reader, links, getOppositeMaster(outward)));
    if (IsotropicTypes.getInstance(reader).isIsotropicType(linkType)) {
      LongArray isoLinks = LongArray.copy(queryLinks(issue, getOppositeMaster(outward), linkType));
      isoLinks.sortUnique();
      LongList isoOpposites = mapOpposites(reader, isoLinks, getThisMaster(outward));
      for (int i = 0; i < isoLinks.size(); i++) {
        long link = isoLinks.get(i);
        if (links.contains(link))
          continue;
        links.add(link);
        existingOpposites.add(isoOpposites.get(i));
      }
    }
    for (int i = 0; i < oppositeIssues.size(); i++) {
      long opposite = oppositeIssues.get(i);
      if (opposite <= 0 || issueContext.getItem() == opposite) continue;
      int index = findExisting(issueContext.getDrain().readItems(links), existingOpposites, opposite);
      if (index >= 0) {
        ItemVersionCreator link = issueContext.getDrain().changeItem(links.get(index));
        if (!link.isAlive()) link.setAlive();
      } else {
        ItemVersionCreator link = issueContext.getDrain().createLocalItem();
        link.setValue(SyncSchema.INVISIBLE, false);
        link.setValue(DBAttribute.TYPE, Link.DB_TYPE);
        link.setValue(SyncAttributes.CONNECTION, connection);
        getThisMaster(outward).setValue(link, issueContext.getItem());
        getOppositeMaster(outward).setValue(link, opposite);
        link.setValue(Link.LINK_TYPE, linkType);
        links.add(link.getItem());
        existingOpposites.add(opposite);
      }
    }
  }

  private static int findExisting(List<ItemVersion> links, LongArray existingOpposites, long opposite) {
    int index = -1;
    for (int i = 0; i < existingOpposites.size(); i++) {
      long item = existingOpposites.get(i);
      if (item != opposite) continue;
      ItemVersion link = links.get(i);
      ItemVersion candidate = index >= 0 ? links.get(index) : null;
      if (candidate == null) index = i;
      else if (!candidate.isAlive() && link.isAlive()) index = i;
      else if (candidate.isAlive() == link.isAlive()) index = candidate.getItem() <= link.getItem() ? index : i;
    }
    return index;
  }
}
