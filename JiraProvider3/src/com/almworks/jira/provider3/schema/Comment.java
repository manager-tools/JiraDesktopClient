package com.almworks.jira.provider3.schema;

import com.almworks.engine.gui.TextController;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.sync.schema.ServerComment;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class Comment {

  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerComment.TYPE);
  public static final DBAttribute<Long> ISSUE = ServerJira.toLinkAttribute(ServerComment.ISSUE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerComment.ID);
  public static final DBAttribute<String> TEXT = ServerJira.toScalarAttribute(ServerComment.TEXT);
  public static final DBAttribute<Long> AUTHOR = ServerJira.toLinkAttribute(ServerComment.AUTHOR);
  public static final DBAttribute<Date> CREATED = ServerJira.toScalarAttribute(ServerComment.CREATED);
  public static final DBAttribute<Long> EDITOR = ServerJira.toLinkAttribute(ServerComment.EDITOR);
  public static final DBAttribute<Date> UPDATED = ServerJira.toScalarAttribute(ServerComment.UPDATED);
  public static final DBAttribute<Long> LEVEL = ServerJira.toLinkAttribute(ServerComment.SECURITY);

  @NotNull
  public static String loadHumanText(ItemVersion comment) {
    if (comment == null) return "";
    DBAttribute<String> attribute = Comment.TEXT;
    return loadHumanText(comment, attribute);
  }

  @NotNull
  public static String loadHumanText(ItemVersion item, DBAttribute<String> attribute) {
    return TextController.toHumanText(item.getValue(attribute));
  }
}
