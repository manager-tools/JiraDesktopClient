package com.almworks.export;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.util.ExportDescription;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.TableController;
import com.almworks.engine.gui.attachments.Attachment;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

public class ExportedData {
  @NotNull private final List<ArtifactRecord> myRecords;
  @NotNull private final LinkedHashSet<ItemExport> myKeys;
  @NotNull private final List<String> mySelectedColumnsNames;
  @Nullable private final String myCollectionName;
  @Nullable private final GenericNode myNode;
  @NotNull private final Date myDateCollected = new Date();
  private final ItemExport myKeyExport;
  private final ItemExport mySummaryExport;
  private final ModelKey<List<Comment>> myComments;
  private final ModelKey<List<Attachment>> myAttachments;

  private ExportedData(List<ArtifactRecord> records, LinkedHashSet<ItemExport> keys, List<String> selectedColumnsNames,
    String collectionName, GenericNode node, ItemExport keyExport, ItemExport summaryExport, ModelKey<List<Comment>> comments, ModelKey<List<Attachment>> attachments)
  {
    myRecords = records;
    myKeys = keys;
    mySelectedColumnsNames = selectedColumnsNames;
    myCollectionName = collectionName;
    myNode = node;
    myKeyExport = keyExport;
    mySummaryExport = summaryExport;
    myComments = comments;
    myAttachments = attachments;
  }

  public static ExportedData create(TableController controller) throws CantPerformException {
    AListModel<? extends LoadedItem> model = controller.getCollectionModel();
    int size = model.getSize();
    if (size == 0)
      throw new CantPerformException();

    List<ArtifactRecord> allRecords = Collections15.arrayList();

    List<LoadedItem> items = Collections15.arrayList(Collections15.linkedHashSet(model.toList()));
    LinkedHashSet<ItemExport> exports = new LinkedHashSet<ItemExport>();
    ItemExport keyExport = null;
    ItemExport summaryExport = null;
    ModelKey<List<Comment>> comments = null;
    ModelKey<List<Attachment>> attachments = null;
    for (LoadedItem item : items) {
      Connection connection = item.getConnection();
      PropertyMap map = new PropertyMap(item.getValues());
      ExportDescription description = item.getMetaInfo().getExportDescription();
      exports.addAll(description.getExports());
      keyExport = areSame(keyExport, description.getItemKeyExport());
      summaryExport = areSame(summaryExport, description.getItemSummaryExport());
      comments = areSame(comments, description.getComments());
      attachments = areSame(attachments, description.getAttachments());
      ArtifactRecord record = new ArtifactRecord(map, connection, description.getTypeDisplayName());
      allRecords.add(record);
    }

    @Nullable String collectionName = controller.getCollectionShortName();
    @Nullable GenericNode node = controller.getCollectionNode();

    List<String> selectedColumnsNames = Collections15.arrayList();
    List<TableColumnAccessor<LoadedItem, ?>> columns = controller.getSelectedColumns();
    for (TableColumnAccessor<LoadedItem, ?> column : columns) selectedColumnsNames.add(column.getId());
    return new ExportedData(allRecords, exports, selectedColumnsNames, collectionName, node, keyExport, summaryExport, comments, attachments);
  }

  private static <T> T areSame(T prev, T next) {
    if (prev == null || next == null) return prev != null ? prev : next;
    if (Util.equals(prev, next)) return prev;
    LogHelper.error("Different", prev, next);
    return prev;
  }

  public List<ArtifactRecord> getRecords() {
    return myRecords;
  }

  public LinkedHashSet<ItemExport> getKeys() {
    return myKeys;
  }

  public List<String> getSelectedColumnsNames() {
    return mySelectedColumnsNames;
  }

  public String getCollectionName() {
    return myCollectionName;
  }

  @Nullable
  public GenericNode  getNode() {
    return myNode;
  }

  public Date getDateCollected() {
    return myDateCollected;
  }

  @Nullable
  public ItemExport getKeyExport() {
    return myKeyExport;
  }

  @Nullable
  public ItemExport getSummaryExport() {
    return mySummaryExport;
  }

  @Nullable
  public ModelKey<List<Comment>> getComments() {
    return myComments;
  }

  @Nullable
  public ModelKey<List<Attachment>> getAttachments() {
    return myAttachments;
  }

  public Collection<Connection> getConnections() {
    final Collection<Connection> conns = Collections15.hashSet();
    for(final ArtifactRecord rec : myRecords) {
      conns.add(rec.getConnection());
    }
    return conns;
  }

  public static class ArtifactRecord {
    @NotNull private final PropertyMap myValues;
    @NotNull private final Connection myConnection;
    @NotNull private final String myDisplayableType;

    public ArtifactRecord(PropertyMap values, Connection connection, String displayableType) {
      myValues = values;
      myConnection = connection;
      myDisplayableType = displayableType;
    }

    public PropertyMap getValues() {
      return myValues;
    }

    public Connection getConnection() {
      return myConnection;
    }

    public String getDisplayableType() {
      return myDisplayableType;
    }
  }
}
