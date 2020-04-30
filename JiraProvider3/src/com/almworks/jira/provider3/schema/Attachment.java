package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.util.DBNamespace;
import com.almworks.jira.provider3.sync.schema.ServerAttachment;
import com.almworks.jira.provider3.sync.schema.ServerJira;

import java.util.Date;

public class Attachment {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerAttachment.TYPE);
  public static final DBAttribute<Long> ISSUE = ServerJira.toLinkAttribute(ServerAttachment.ISSUE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerAttachment.ID);
  public static final DBAttribute<String> FILE_URL = ServerJira.toScalarAttribute(ServerAttachment.FILE_URL);
  public static final DBAttribute<String> DATE_STRING = ServerJira.toScalarAttribute(ServerAttachment.DATE_STRING);
  public static final DBAttribute<Date> DATE = ServerJira.toScalarAttribute(ServerAttachment.DATE);
  public static final DBAttribute<String> MIME_TYPE = ServerJira.toScalarAttribute(ServerAttachment.MIME_TYPE);
  public static final DBAttribute<String> SIZE_STRING = ServerJira.toScalarAttribute(ServerAttachment.SIZE_STRING);
  public static final DBAttribute<String> ATTACHMENT_NAME = ServerJira.toScalarAttribute(ServerAttachment.FILE_NAME);
  public static final DBAttribute<Long> AUTHOR = ServerJira.toLinkAttribute(ServerAttachment.AUTHOR);
  private static final DBNamespace NS = ServerJira.NS.subNs("attachments");
  public static final DBAttribute<String> LOCAL_FILE = NS.string("localFile", "Local File", true);
}
