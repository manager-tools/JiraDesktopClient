package com.almworks.jira.provider3.links;

import com.almworks.engine.gui.Formlet;
import com.almworks.integers.LongList;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.SyncAttributes;
import com.almworks.itemsync.MergeOperationsManager;
import com.almworks.jira.provider3.links.upload.PrepareLinkUpload;
import com.almworks.jira.provider3.schema.Link;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;

import java.util.List;

public class JiraLinks {
  public static final ScalarSequence MK_DATA_LOADER = LoadedLinkLoader.SERIALIZABLE;
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(PrepareLinkUpload.class.getClassLoader(), "com/almworks/jira/provider3/links/message");

  public static void setupMerge(MergeOperationsManager manager) {
    manager.buildOperation(Link.DB_TYPE).addCustom(new ItemAutoMerge() {
      @Override
      public void preProcess(ModifiableDiff local) {
      }

      @Override
      public void resolve(AutoMergeData data) {
        if (data.getUnresolved().contains(SyncAttributes.INVISIBLE)) data.discardEdit(SyncAttributes.INVISIBLE);
      }
    }).finish();
  }

  public static void registerFeature(FeatureRegistry registry) {
    LoadedLinkLoader.registerFeature(registry);
  }

  public static Formlet createFormlet(GuiFeaturesManager features, Configuration links) {
    return LinksFormlet.create(features, links);
  }

  public static void deleteLinks(EditDrain drain, LongList links) {
    IsotropicTypes isotropic = IsotropicTypes.getInstance(drain);
    List<long[]> typeSourceTarget = Collections15.arrayList();
    for (ItemVersionCreator link : drain.changeItems(links)) {
      Long type = link.getValue(Link.LINK_TYPE);
      if (isotropic.isIsotropicType(type)) {
        long source = Link.SOURCE.getValue(link);
        long target = Link.TARGET.getValue(link);
        if (source > 0 && target > 0) typeSourceTarget.add(new long[] {type, target, source});
      }
      link.delete();
    }
    deleteLinks(drain, typeSourceTarget);
  }

  private static void deleteLinks(EditDrain drain, List<long[]> typeSourceTarget) {
    for (long[] linkInfo : typeSourceTarget) {
      long type = linkInfo[0];
      long source = linkInfo[1];
      long target = linkInfo[2];
      BoolExpr<DP> query = DPEquals.create(Link.LINK_TYPE, type).and(Link.SOURCE.queryEqual(source)).and(Link.TARGET.queryEqual(target));
      long linkItem = drain.getReader().query(query).getItem();
      if (linkItem > 0) drain.unsafeChange(linkItem).delete();
    }
  }
}
