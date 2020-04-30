package com.almworks.jira.provider3.gui.viewer;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.application.viewer.CommentRenderingHelper;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.SyncState;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.util.LogHelper;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Date;
import java.util.List;

public class CommentImpl implements Comment {
  public static final DataRole<CommentImpl> DATA_ROLE = DataRole.createRole(CommentImpl.class);
  public static final CommentRenderingHelper<CommentImpl> COMMENT_RENDERING_HELPER = new CommentRenderingHelper<CommentImpl>() {
    public Color getForeground(CommentImpl comment) {
      return null;
    }

    @Override
    public String getHeaderPrefix(CommentImpl comment) {
      return null;
    }

    @Override
    public String getHeaderSuffix(CommentImpl comment) {
      StringBuilder ret = new StringBuilder();
      String visibleTo = comment.getVisibleForText();
      if (visibleTo != null && visibleTo.length() > 0) {
        ret = new StringBuilder().append("visible to ").append(visibleTo);
      }
      ret.append(" ");
      switch (comment.mySyncState) {
      case SYNC: break;
      case NEW: break;
      case EDITED: ret.append("<edited>"); break;
      case LOCAL_DELETE: ret.append("<locally deleted>"); break;
      case DELETE_MODIFIED: ret.append("<conflict: locally deleted but modified on server>"); break;
      case MODIFIED_CORPSE: ret.append("<conflict: locally modified but deleted on server>"); break;
      case CONFLICT: ret.append("<conflict: concurrently edited locally and on server>"); break;
      default: LogHelper.error("Unknown syncState", comment.mySyncState);
      }
      return ret.toString().trim();
    }
  };

  private final long myItem;
  private final Date myCreated;
  private final long myAuthor;
  private final String myText;
  private final Date myUpdated;
  private final long myUpdateAuthor;
  private final long myVisibility;
  private final SyncState mySyncState;
  private final GuiFeaturesManager myManager;

  CommentImpl(GuiFeaturesManager manager, long item, Date created, long author, String text, Date updated, long updateAuthor,
    long visibility, SyncState syncState) 
  {
    myManager = manager;
    myCreated = created;
    myAuthor = author;
    myText = Util.NN(text);
    myUpdated = updated;
    myUpdateAuthor = updateAuthor;
    mySyncState = syncState;
    myVisibility = visibility;
    myItem = item;
  }

  @Override
  public String getText() {
    return myText;
  }

  @Override
  public String getWhenText() {
    if (myCreated != null) {
      return DateUtil.toLocalDateTime(myCreated);
    }
    if (isLocalNew()) return "<new>";
    return "";
  }

  @Override
  public Date getWhen() {
    return myCreated;
  }

  @Override
  public String getWhoText() {
    return getUserDisplayName(myAuthor, "Anon");
  }
  
  public long getAuthor() {
    return myAuthor;
  }

  @Override
  public String getHeaderTooltipHtml() {
    if (myUpdated != null) {
      String updateAuthor = getUserDisplayName(myUpdateAuthor, "anon");
      String updated = DateUtil.toLocalDateTime(myUpdated);
      return "Edited by " + updateAuthor + " \u2014 " + updated;
    }
    return null;
  }

  public long getItem() {
    return myItem;
  }

  public boolean isLocalNew() {
    return myCreated == null;
  }

  @Nullable
  public String getVisibleForText() {
    return LoadedIssueUtil.getVisibilityText(myManager, myVisibility);
  }

  public long getVisibleForItem() {
    return myVisibility;
  }

  private String getUserDisplayName(long user, String defaultResult) {
    String name = LoadedIssueUtil.getUserDisplayName(myManager, user);
    return name != null ? name : defaultResult;
  }

  public static ModelKey<List<CommentImpl>> getModelKey(GuiFeaturesManager features) {
    return features.findListModelKey(MetaSchema.KEY_COMMENTS_LIST, CommentImpl.class);
  }
}
