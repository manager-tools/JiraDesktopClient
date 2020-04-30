package com.almworks.jira.provider3.links;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.UiItem;
import com.almworks.api.engine.Engine;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.*;
import com.almworks.util.collections.LongSet;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LoadedLink2 implements LoadedLink, UiItem {
  public static final DataRole<LoadedLink2> DB_LINK = DataRole.createRole(LoadedLink2.class);

  private final long myConnection;
  private final long myLinkItem;
  private final long myType;
  private final boolean myOutward;
  private final long myOppositeIssue;
  private final String myOppositeKey;
  private final String myOppositeSummary;
  private final long myOppositeStatus;
  private final long myOppositeIssueType;
  private final long myOppositePriority;

  private LoadedLink2(long linkItem, long connection, long type, boolean outward, long oppositeIssue, String oppositeKey, String oppositeSummary, long oppositeIssueType,
      long oppositeStatus, long oppositePriority)
  {
    myType = type;
    myOutward = outward;
    myOppositeKey = oppositeKey;
    myOppositeSummary = oppositeSummary;
    myOppositeIssueType = oppositeIssueType;
    myOppositeStatus = oppositeStatus;
    myOppositePriority = oppositePriority;
    myLinkItem = linkItem;
    myOppositeIssue = oppositeIssue;
    myConnection = connection;
  }

  public static LoadedLink2 load(ItemVersion link, long issue) {
    long source = Link.SOURCE.getValue(link);
    long target =  Link.TARGET.getValue(link);
    if (source == 0 || target == 0) return null;
    boolean outward;
    long opposite;
    if (source == issue) {
      outward = true;
      opposite = target;
    } else if (target == issue) {
      outward = false;
      opposite = source;
    } else return null;
    return load(link, outward, opposite);
  }

  private static LoadedLink2 load(ItemVersion link, boolean outward, long oppositeItem) {
    long linkType = link.getNNValue(Link.LINK_TYPE, 0l);
    long connection = link.getNNValue(SyncAttributes.CONNECTION, 0l);
    ItemVersion opposite = link.forItem(oppositeItem);
    return new LoadedLink2(link.getItem(), connection, linkType, outward, oppositeItem, opposite.getValue(Issue.KEY),
      opposite.getValue(Issue.SUMMARY), opposite.getNNValue(Issue.ISSUE_TYPE, 0l), opposite.getNNValue(Issue.STATUS, 0l),
      opposite.getNNValue(Issue.PRIORITY, 0l));
  }

  @Override
  public String getOppositeString(@NotNull TypedKey<String> key) {
    if (KEY.equals(key)) return myOppositeKey;
    else if (SUMMARY.equals(key)) return myOppositeSummary;
    return null;
  }

  @Override
  public ItemKey getOppositeEnum(GuiFeaturesManager manager, @NotNull TypedKey<ItemKey> key) {
    if (STATUS.equals(key)) return getItemKey(manager, Status.ENUM_TYPE, myOppositeStatus);
    else if (ISSUE_TYPE.equals(key)) return getItemKey(manager, IssueType.ENUM_TYPE, myOppositeIssueType);
    else if (PRIORITY.equals(key)) return getItemKey(manager, Priority.ENUM_TYPE, myOppositePriority);
    return null;
  }

  private ItemKey getItemKey(GuiFeaturesManager manager, DBStaticObject enumType, long item) {
    EnumTypesCollector.Loaded type = manager.getEnumTypes().getType(enumType);
    return type == null ? null : type.getResolvedItem(item);
  }

  public static LoadedModelKey<List<LoadedLink2>> getLinksKey(GuiFeaturesManager features) {
    return features.findListModelKey(MetaSchema.KEY_LINKS_LIST, LoadedLink2.class);
  }

  @Override
  public boolean getOutward() {
    return myOutward;
  }

  @Override
  public long getType() {
    return myType;
  }

  public String getDescription(GuiFeaturesManager manager) {
    LoadedItemKey type = EnumTypesCollector.getResolvedItem(manager, LinkType.ENUM_TYPE, myType);
    if (type == null) return null;
    return type.getValue(myOutward ? LinkType.OUTWARD_DESCRIPTION : LinkType.INWARD_DESCRIPTION);
  }

  @Override
  public long getItem() {
    return myLinkItem;
  }

  public long getOppositeIssue() {
    return myOppositeIssue;
  }

  public long getConnection() {
    return myConnection;
  }

  @Nullable
  public JiraConnection3 getConnection(Engine engine) {
    if (myConnection <= 0) return null;
    return Util.castNullable(JiraConnection3.class, engine.getConnectionManager().findByItem(myConnection));
  }

  public static void filterIsotropic(VersionSource issue, List<LoadedLink2> links) {
    IsotropicTypes isotropic = IsotropicTypes.getInstance(issue);
    Map<Long, LongSet> knownIsotropic = Collections15.hashMap();
    for (Iterator<LoadedLink2> it = links.iterator(); it.hasNext(); ) {
      LoadedLink2 element = it.next();
      long type = element.getType();
      if (!isotropic.isIsotropicType(type))
        continue;
      LongSet opposites = knownIsotropic.get(type);
      if (opposites == null) {
        opposites = new LongSet();
        knownIsotropic.put(type, opposites);
      }
      long opposite = element.getOppositeIssue();
      if (opposites.contains(opposite))
        it.remove();
      else
        opposites.add(opposite);
    }
  }

  public static boolean isIsotropicType(GuiFeaturesManager manager, long type) {
    LoadedItemKey typeKey = EnumTypesCollector.getResolvedItem(manager, LinkType.ENUM_TYPE, type);
    if (typeKey == null) return false;
    String outward = typeKey.getValue(LinkType.OUTWARD_DESCRIPTION);
    String inward = typeKey.getValue(LinkType.INWARD_DESCRIPTION);
    return outward != null && inward != null && outward.equals(inward);
  }
}
