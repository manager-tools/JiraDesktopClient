package com.almworks.jira.provider3.attachments;

import com.almworks.api.application.StateIcon;
import com.almworks.api.application.StateIconHelper;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.ReferrerLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.schema.Attachment;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.Terms;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class AttachmentImplLoader extends ReferrerLoader<AttachmentImpl> {
  private static final AttachmentImplLoader INSTANCE = new AttachmentImplLoader();
  static final ReferrerLoader.Descriptor DESCRIPTOR = ReferrerLoader.Descriptor.create(Jira.NS_FEATURE, "attachmentsList", INSTANCE);
  public static final StateIcon ATTACHMENT_STATE_ICON = new StateIcon(Icons.ATTACHMENT, 0, Terms.ref_Artifact + " has attachments");

  private AttachmentImplLoader() {
    super(Attachment.ISSUE, AttachmentImpl.class);
  }

  @Override
  public AttachmentImpl extractValue(ItemVersion slaveVersion, ReferrerLoader.LoadContext context) {
    if (slaveVersion.isInvisible()) return null;
    GuiFeaturesManager features = context.getActor(GuiFeaturesManager.ROLE);
    ItemVersion issue = slaveVersion.readValue(Attachment.ISSUE);
    JiraConnection3 connection = null;
    if (issue != null) {
      Long cItem = issue.getNNValue(SyncAttributes.CONNECTION, 0l);
      Connection byItem = context.getActor(Engine.ROLE).getConnectionManager().findByItem(cItem);
      connection = Util.castNullable(JiraConnection3.class, byItem);
    }

    return AttachmentImpl.load(slaveVersion, features, connection);
  }

  @Override
  protected void afterElementsExtracted(ItemVersion issue, @NotNull PropertyMap values, @NotNull List<AttachmentImpl> elements) {
    if (elements.isEmpty()) return;
    StateIconHelper.addStateIcon(values, ATTACHMENT_STATE_ICON);
  }
}
