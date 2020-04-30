package com.almworks.jira.provider3.gui.viewer;

import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.ReferrerLoader;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.jira.provider3.schema.Comment;
import com.almworks.jira.provider3.schema.Jira;

import java.util.Date;

public class CommentImplLoader extends ReferrerLoader<CommentImpl> {
  private static final CommentImplLoader INSTANCE = new CommentImplLoader();
  private static final ReferrerLoader.Descriptor DESCRIPTOR = ReferrerLoader.Descriptor.create(Jira.NS_FEATURE, "commentsList", INSTANCE);
  public static final ScalarSequence SERIALIZABLE = DESCRIPTOR.getSerializable();

  private CommentImplLoader() {
    super(Comment.ISSUE, CommentImpl.class);
  }

  public static void registerFeature(FeatureRegistry registry) {
    DESCRIPTOR.registerFeature(registry);
  }

  @Override
  public CommentImpl extractValue(ItemVersion slaveVersion, ReferrerLoader.LoadContext context) {
    if (slaveVersion.isInvisible()) return null;
    Date created = slaveVersion.getValue(Comment.CREATED);
    long author = slaveVersion.getNNValue(Comment.AUTHOR, 0l);
    long updateAuthor = slaveVersion.getNNValue(Comment.EDITOR, 0l);
    String text = Comment.loadHumanText(slaveVersion);
    long security = slaveVersion.getNNValue(Comment.LEVEL, 0l);
    long item = slaveVersion.getItem();
    Date updated = slaveVersion.getValue(Comment.UPDATED);
    SyncState syncState = slaveVersion.getSyncState();
    return new CommentImpl(GuiFeaturesManager.getInstance(slaveVersion.getReader()), item, created, author, text, updated, updateAuthor, security, syncState);
  }
}
