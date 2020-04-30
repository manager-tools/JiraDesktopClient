package com.almworks.tools.tagexporter;

import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPIntersects;
import com.almworks.items.impl.SQLiteDatabase;
import com.almworks.util.NoObfuscation;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.commons.LongObjFunction2;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.progress.Progress;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.almworks.util.Collections15.arrayList;

//import com.almworks.tags.TagsComponent;

/** Implements NoObfuscation to be called through reflection */
public class TagExporter3 extends TagExporterEnv implements NoObfuscation {
  private static final String DB_FILE_NAME = "items.db";
  private static final String TAG_TYPE_ID = "com.almworks.engine.tags:t:tag";
  private static final String TAG_ICON_PATH_ATTR_ID = "com.almworks.engine.tags:a:tag.iconPath";
  private static final String PRIMARY_ITEM_TAGS_ATTR_ID = "com.almworks.engine.tags:a:tags";
  private static final String ISSUE_KEY_ATTR_ID = "jira:a:issue.key";
  private static final String BUG_ID_ATTR_ID = "com.almworks.bugzilla:a:bug.id";
  private static final String CONNECTION_ATTR_ID = "com.almworks.items.api.sync:a:connection";
  private static final String CONNECTION_ID_ATTR_ID = "com.almworks.items.api.sync:a:connectionID";
  private static final String BUGZILLA_CONNECTION_ID_ATTR_ID = "com.almworks.bugzilla:a:connection.id";

  public static void main(String[] args) {
    TagExporterLauncher.launch(new TagExporter3(), args);
  }

  /** Do not remove -- called via reflection from applications 
  * @param reportError (key, message); key defines error message group */
  @SuppressWarnings("UnusedDeclaration")
  public static void exportTags(File workspace, @NotNull File outFile, Progress progress, Procedure2<String, String> reportError) {
    TagExporter.exportTags(new TagExporter3(), workspace, outFile, progress, reportError);
  }
 
  
  @Override
  public String getFullName() {
    return "3.0 Tag Exporter";
  }

  @Override
  protected boolean isWorkspace(WorkspaceStructure workArea) {
    try {
      File db = getDbFile(workArea).getCanonicalFile();
      return db.isFile();
    } catch (Exception ex) {
      return false;
    }
  }

  private static File getDbFile(WorkspaceStructure workArea) throws IOException {
    return new File(workArea.getRootDir(), DB_FILE_NAME).getCanonicalFile();
  }

  @NotNull
  @Override
  public List<TagInfo> readTags(WorkspaceStructure workArea, Progress progress) throws Exception {
    TagReader tagReader = new TagReader(workArea, progress.createDelegate(0.6));
    progress.setProgress(0.05, "Opening the database");
    tagReader.start();
    progress.setProgress(0.4);
    try {
      return Util.NN(tagReader.readTags(), Collections.EMPTY_LIST);
    } finally {
      try {
        tagReader.stop();
      } finally {
        progress.setDone();
      }
    }
  }


  private static class TagReader implements ReadTransaction<List<TagInfo>> {
    private final WorkspaceStructure myWorkArea;
    private final Progress myProgress;
    private SQLiteDatabase myDb;

    public TagReader(WorkspaceStructure workArea, Progress progress) {
      myWorkArea = workArea;
      myProgress = progress;
    }

    public void start() throws IOException {
      File dbFile = getDbFile(myWorkArea);
      File tempDir = new File(System.getProperty("java.io.tmpdir"));
      if (!tempDir.canWrite()) tempDir = myWorkArea.getTempDir();
      myDb = new SQLiteDatabase(dbFile, tempDir);
      myDb.start();
    }
    
    public void stop() {
      myDb.stop();
    }
    
    public List<TagInfo> readTags() {
      return myDb.readForeground(this).waitForCompletion();
    }

    @Override
    public List<TagInfo> transaction(final DBReader reader) throws DBOperationCancelledException {
      myProgress.setProgress(0.1);

      Long typeTag = getTagType(reader);
      DBAttribute<String> attrTagIconPath = requireAttrById(reader, TAG_ICON_PATH_ATTR_ID, String.class);
      DBAttribute<? extends Collection<Long>> attrPrimaryItemsTags = requireCollectionAttrById(reader, PRIMARY_ITEM_TAGS_ATTR_ID, Long.class);
      
      DBAttribute<String> attrIssueKey = getAttrById(reader, ISSUE_KEY_ATTR_ID, String.class);
      DBAttribute<Integer> attrBugId = getAttrById(reader, BUG_ID_ATTR_ID, Integer.class);
      if (attrIssueKey == null && attrBugId == null) {
        myProgress.addError("Database format has changed: no key attribute found");
        throw new DBOperationCancelledException();
      }
      boolean isJira = attrIssueKey != null;
      DBAttribute<?> attrKey = isJira ? attrIssueKey : attrBugId;
      DBAttribute<Long> attrConnection = getAttrConnection(reader);
      DBAttribute<String> attrConnectionId = requireAttrById(reader, CONNECTION_ID_ATTR_ID, String.class);
      
      myProgress.setProgress(0.2);
      return reader.query(DPEquals.create(DBAttribute.TYPE, typeTag))
        .fold(new ArrayList<TagInfo>(), new TagInfoCreator(reader, attrTagIconPath, attrPrimaryItemsTags, isJira, attrKey, attrConnection, attrConnectionId));
    }

    private Long getTagType(DBReader reader) {
      Long tagType = reader.findMaterialized(new DBItemType(TAG_TYPE_ID));
      if (tagType == null) {
        myProgress.addError("Incompatible database format: no tag type (" + TAG_TYPE_ID + ')');
        throw new DBOperationCancelledException();
      }
      return tagType;
    }

    private DBAttribute<Long> getAttrConnection(DBReader reader) {
      DBAttribute<Long> attrConnectionInit = getAttrById(reader, CONNECTION_ATTR_ID, Long.class);
      if (attrConnectionInit == null) attrConnectionInit = getAttrById(reader, BUGZILLA_CONNECTION_ID_ATTR_ID, Long.class);
      if (attrConnectionInit == null) {
        myProgress.addError("Database format has changed");
        throw new DBOperationCancelledException();
      }
      return attrConnectionInit;
    }

    private <T> DBAttribute<T> getAttrById(DBReader reader, String id, Class<T> scalarClass) {
      return getAttrById(reader, id, false, scalarClass, singletonList(DBAttribute.ScalarComposition.SCALAR));
    }
    
    private <T> DBAttribute<T> requireAttrById(DBReader reader, String id, Class<T> scalarClass) {
      return getAttrById(reader, id, true, scalarClass, singletonList(DBAttribute.ScalarComposition.SCALAR));
    }
    
    private <T> DBAttribute<? extends Collection<T>> requireCollectionAttrById(DBReader reader, String id, Class<T> scalarClass) {
      return (DBAttribute)getAttrById(reader, id, true, scalarClass, arrayList(DBAttribute.ScalarComposition.LIST, DBAttribute.ScalarComposition.SET));
    }
    
    private <T> DBAttribute<T> getAttrById(DBReader reader, String id, boolean require, Class<T> scalarClass, List<DBAttribute.ScalarComposition> scalarCompositions) {
      DBAttribute attribute = reader.getAttribute(id);
      if (attribute == null && !require) return null;
      checkAttr(attribute != null, "cannot find mandatory attribute '" + id + '\'');
      checkAttr(attribute.getScalarClass() == scalarClass, "attribute '" + id + "' has changed its type from '" + scalarClass + "' to '" + attribute.getScalarClass() + '\'');
      checkAttr(scalarCompositions.contains(attribute.getComposition()), "attribute '" + id + "' has changed its composition from '" + scalarCompositions + "' to '" + attribute.getComposition() + '\'');
      return (DBAttribute<T>)attribute;
    }
    
    private void checkAttr(boolean check, String errorMessage) {
      if (!check) {
        myProgress.addError("Incompatible database format: " + errorMessage);
        throw new DBOperationCancelledException();
      }
    }

    private class TagInfoCreator implements LongObjFunction2<ArrayList<TagInfo>> {
      private boolean warnedNotImported;
      private final DBReader myReader;
      private final DBAttribute<String> myAttrTagIconPath;
      private final DBAttribute<? extends Collection<Long>> myAttrPrimaryItemTags;
      private final boolean myJira;
      private final DBAttribute<?> myAttrKey;
      private final DBAttribute<Long> myAttrConnection;
      private final DBAttribute<String> myAttrConnectionId;

      public TagInfoCreator(DBReader reader, DBAttribute<String> attrTagIconPath, DBAttribute<? extends Collection<Long>> attrPrimaryItemTags,
        boolean isJira, DBAttribute<?> attrKey, DBAttribute<Long> attrConnection, DBAttribute<String> attrConnectionId)
      {
        myReader = reader;
        myAttrTagIconPath = attrTagIconPath;
        myAttrPrimaryItemTags = attrPrimaryItemTags;
        myJira = isJira;
        myAttrKey = attrKey;
        myAttrConnection = attrConnection;
        myAttrConnectionId = attrConnectionId;
      }

      @Override
      public ArrayList<TagInfo> invoke(long tag, ArrayList<TagInfo> tagInfos) {
        tagInfos.add(new TagInfo(
          Util.NN(DBAttribute.NAME.getValue(tag, myReader), "Tag " + tag),
          myAttrTagIconPath.getValue(tag, myReader),
          myReader.query(DPIntersects.create(myAttrPrimaryItemTags, Collections.singleton(tag)))
            .fold(new MultiMap<String, String>(), new LongObjFunction2<MultiMap<String, String>>() { @Override public MultiMap<String, String> invoke(long primaryItem, MultiMap<String, String> items)
            {
              Long connection = getValue(primaryItem, myAttrConnection);
              String connectionId = connection == null ? null : getValue(connection, myAttrConnectionId);
              Object key = getValue(primaryItem, myAttrKey);
              if (connectionId != null && key != null)
                items.add(connectionId, key.toString());
              return items;
            }}),
          myJira
        ));
        return tagInfos;
      }

      @Nullable
      private <T> T getValue(long item, DBAttribute<T> attr) {
        T value = myReader.getValue(item, attr);
        if (!warnedNotImported && value == null) {
          warnedNotImported = true;
          Log.error("Some of the items were not imported");
        }
        return value;
      }
    }
  }
}
