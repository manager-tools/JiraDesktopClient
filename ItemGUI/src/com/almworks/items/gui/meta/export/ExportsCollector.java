package com.almworks.items.gui.meta.export;

import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.ItemImageCollector;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.export.ExportPolicy;
import com.almworks.items.gui.meta.schema.export.Exports;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.LogHelper;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.detach.Lifespan;

import java.util.List;

public class ExportsCollector implements ItemImageCollector.ImageFactory<ItemExportImpl> {
  private static final DataLoader<String> ID = AttributeLoader.create(Exports.ID);
  private static final SerializedObjectAttribute<ExportPolicy> POLICY = SerializedObjectAttribute.create(ExportPolicy.class, Exports.POLICY);
  private final ItemImageCollector<ItemExportImpl> myExports;
  private final ItemImageCollector.GetUpToDate<ItemExportImpl> myUpToDate;
  private final GuiFeaturesManager myFeatures;

  public ExportsCollector(DBImage image, GuiFeaturesManager features) {
    myFeatures = features;
    ImageSlice slice = image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, Exports.DB_TYPE));
    myExports = ItemImageCollector.create(slice, this, false);
    myUpToDate = ItemImageCollector.GetUpToDate.create(myExports);
  }

  public void start(Lifespan life) {
    ImageSlice slice = myExports.getSlice();
    slice.addData(DBCommons.OWNER, ID, DBCommons.DISPLAY_NAME, POLICY, DataLoader.IDENTITY_LOADER);
    myExports.start(life);
  }

  @ThreadSafe
  public List<? extends ItemExport> getAll() {
    return myUpToDate.get();
  }

  @Override
  public boolean update(ItemExportImpl export, long item) {
    ImageSlice slice = myExports.getSlice();
    ExportPolicy policy = slice.getValue(item, POLICY);
    Long owner = slice.getValue(item, DBCommons.OWNER);
    String id = getExportId(item, slice);
    String displayName = getExportDisplayName(item, slice);
    if (id == null || policy == null || owner == null) {
      LogHelper.error("Missing update export value", id, policy, owner);
      return false;
    }
    return export.update(id, displayName, owner, policy);
  }

  @Override
  public void onRemoved(ItemExportImpl image) {
  }

  @Override
  public ItemExportImpl create(long item) {
    ImageSlice slice = myExports.getSlice();
    ExportPolicy policy = slice.getValue(item, POLICY);
    Long owner = slice.getValue(item, DBCommons.OWNER);
    String id = getExportId(item, slice);
    String displayName = getExportDisplayName(item, slice);
    if (id == null || policy == null || owner == null) return null;
    return new ItemExportImpl(id, displayName, owner, policy, myFeatures);
  }

  private String getExportDisplayName(long item, ImageSlice slice) {
    return slice.getNNValue(item, DBCommons.DISPLAY_NAME, "").trim();
  }

  private String getExportId(long item, ImageSlice slice) {
    String id = slice.getNNValue(item, ID, "").trim();
    return id.isEmpty() ? null : id;
  }

  public ItemExport findExport(DBStaticObject exportId) {
    if (exportId == null) return null;
    ItemExport export = myExports.findImageByValue(DataLoader.IDENTITY_LOADER, exportId.getIdentity());
    LogHelper.assertError(export != null, "Export not found", exportId);
    return export;
  }
}

