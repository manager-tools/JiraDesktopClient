package com.almworks.items.sync.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class SyncSchema {
  public static final DBNamespace NS = DBNamespace.moduleNs("com.almworks.itemSync");
  /**
   * Base shadow. The server side values which are the past of last user changes<br>
   * Exist iff the item is locally modified (locally created new items are locally modified).
   */
  public static final DBAttribute<AttributeMap> BASE = SyncAttributes.BASE_SHADOW;
  /**
   * Conflict shadow. The server side values which are conflicts with last local changes. {@link #BASE} is past of this shadow.<br>
   * Exists iff the item is locally modified and concurrent edit on server conflicts with local changes. The item is in
   * conflict state.
   */
  public static final DBAttribute<AttributeMap> CONFLICT = SyncAttributes.CONFLICT_SHADOW;
  /**
   * Download shadow. The last known server side values which aren't processed yet. This shadow exists until automerge copy it
   * to some other ({@link #BASE} or {@link #CONFLICT} shadow or to TRUNK.
   */
  public static final DBAttribute<AttributeMap> DOWNLOAD = NS.attributeMap("download", "Download Shadow");
  /**
   * Upload task shadow. The local values which are requested for upload.<br>
   * This shadow exists since upload is started and until it is completely done (including automerge). This shadow is past of TRUNK
   */
  public static final DBAttribute<AttributeMap> UPLOAD_TASK = NS.attributeMap("uploadTask", "Upload-Task Shadow");
  /**
   * Actual successful upload. The local values which are actually uploaded to server.<br>
   * The {@link #BASE} is past of this shadow and {@link #UPLOAD_TASK} is the future. <br>
   * If this shadow is equal to BASE this means that upload completely failed (nothing is uploaded)<br>
   * If this shadow is equal to UPLOAD_TASK this means that all requested changes are successfully uploaded<br>
   * Used together with {@link #DONE_UPLOAD_HISTORY} and is part of done-upload data.
   * <b>Avoid direct usage of the attribute</b>
   * @see #DONE_UPLOAD_HISTORY
   */
  public static final DBAttribute<AttributeMap> DONE_UPLOAD = NS.attributeMap("doneUpload", "Done-Upload Shadow");
  /**
   * Actual successfully uploaded history steps.<br>
   * Should be in range [0, currentHistory] (where currentHistory is trunk history length. 0 means no history step is uploaded,
   * currentHistory means all history is uploaded.<br>
   * Used together with {@link #DONE_UPLOAD} and is part of whole done-upload data.<br>
   * <b>Avoid direct usage of the attribute</b>
   * @see #DONE_UPLOAD
   */
  public static final DBAttribute<Integer> DONE_UPLOAD_HISTORY = NS.integer("doneUploadHistory", "Done-Upload History steps", false);
  public static final BoolExpr<DP> HAS_DONE_UPLOAD = DPNotNull.create(DONE_UPLOAD).or(DPNotNull.create(DONE_UPLOAD_HISTORY));
  public static final DBAttribute<Boolean> IS_SHADOWABLE = SyncAttributes.SHADOWABLE;
  public static final DBAttribute<Boolean> INVISIBLE = SyncAttributes.INVISIBLE;
  /**
   * Upload mark. This mark is set by client code via {@link com.almworks.items.sync.ItemUploader.UploadPrepare#setUploadAttempt(long, byte[])} and
   * cleared by ItemSync core when the item reaches synchronized state (thus no not-uploaded data left).<br>
   * Client code may store any data serialized to bytes.
   */
  public static final DBAttribute<byte[]> UPLOAD_ATTEMPT = NS.bytes("uploadAttempt", "Upload Attempt Mark");

  public static final DBAttribute<Integer> DECIMAL_SCALE = NS.integer("decimalScale", "Decimal Scale", false);

  /**
   * @return true if an item can has shadowable value for the attribute. false means that the attribute is not shadowable or
   * has not ever been used, so no item may has a value for it
   */
  public static boolean hasShadowableValue(DBReader reader, DBAttribute<?> attribute) {
    return AttributeInfo.instance(reader).isShadowable(attribute);
  }

  @NotNull
  public static AttributeMap filterShadowable(DBReader reader, AttributeMap map) {
    AttributeMap result = new AttributeMap();
    for (DBAttribute attribute : map.keySet()) {
      if (hasShadowableValue(reader, attribute)) result.put(attribute, map.get(attribute));
    }
    return result;
  }

  public static void markShadowable(DBAttribute<?> attribute) {
    attribute.initialize(IS_SHADOWABLE, true);
  }

  public static AttributeMap getInvisible() {
    AttributeMap map = new AttributeMap();
    map.put(INVISIBLE, true);
    return map;
  }

  public static boolean isInvisible(AttributeMap map) {
    return map != null && Boolean.TRUE.equals(map.get(SyncSchema.INVISIBLE));
  }

  public static void discardSingle(DBWriter writer, long item) {
    HolderCache holders = HolderCache.instance(writer);
    VersionHolder serverHolder = holders.getServerHolder(item);
    if (serverHolder instanceof VersionHolder.WriteTrunk) {
      VersionHolder.WriteTrunk trunk = (VersionHolder.WriteTrunk) serverHolder;
      boolean aNew = trunk.isNew();
      AttributeMap download = writer.getValue(item, SyncSchema.DOWNLOAD);
      AttributeMap conflict = writer.getValue(item, SyncSchema.CONFLICT);
      AttributeMap base = writer.getValue(item, SyncSchema.BASE);
      if (aNew && download == null && conflict == null && base == null) serverHolder = null;
      else Log.error("not empty shadows " + aNew + " " + download + " " + conflict + " " + base);
    }
    AttributeMap server = serverHolder != null ? serverHolder.getAllShadowableMap() : null;
    if (server != null) {
      AttributeMap map = writer.getAttributeMap(item);
      for (DBAttribute<?> attribute : filterShadowable(writer, map).keySet()) {
        if (!server.containsKey(attribute))
          writer.setValue(item, attribute, null);
      }
      for (DBAttribute<?> a : server.keySet()) {
        DBAttribute<Object> attribute = (DBAttribute<Object>) a;
        writer.setValue(item, attribute, server.get(attribute));
      }
    }
    holders.setBase(item, null);
    holders.setConflict(item, null);
    holders.setDownload(item, null);
    holders.setUploadTask(item, null);
    holders.setDoneUpload(item, null, null);
  }

  public static void setDecimalScale(int scale, DBAttribute<BigDecimal> ... attributes) {
    for (DBAttribute<BigDecimal> attribute : attributes) attribute.initialize(DECIMAL_SCALE, scale);
  }

  public static boolean hasDoneUpload(DBReader reader, long item) {
    return reader.getValue(item, SyncSchema.DONE_UPLOAD) != null ||
      reader.getValue(item, SyncSchema.DONE_UPLOAD_HISTORY) != null;
  }
}
