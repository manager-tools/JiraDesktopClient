package com.almworks.jira.provider3.links;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.ReferrerLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.jira.provider3.schema.Link;
import com.almworks.util.properties.PropertyMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class LoadedLinkLoader extends ReferrerLoader<LoadedLink2> {
  private static final LoadedLinkLoader INSTANCE = new LoadedLinkLoader();
  private static final ReferrerLoader.Descriptor DESCRIPTOR = ReferrerLoader.Descriptor.create(Jira.NS_FEATURE, "links.list", INSTANCE);
  public static final ScalarSequence SERIALIZABLE = DESCRIPTOR.getSerializable();

  private LoadedLinkLoader() {
    //noinspection unchecked
    super(new DBAttribute[] {Link.SOURCE.getAttribute(), Link.TARGET.getAttribute()}, LoadedLink2.class);
  }

  public static void registerFeature(FeatureRegistry registry) {
    DESCRIPTOR.registerFeature(registry);
  }

  @Override
  public LoadedLink2 extractValue(ItemVersion slaveVersion, LoadContext context) {
    if (slaveVersion.isInvisible()) return null;
    long issue = context.getItemServices().getItem();
    return LoadedLink2.load(slaveVersion, issue);
  }

  @Override
  protected void afterElementsExtracted(ItemVersion issue, @NotNull PropertyMap values, @NotNull List<LoadedLink2> elements) {
    LoadedLink2.filterIsotropic(issue, elements);
  }
}
