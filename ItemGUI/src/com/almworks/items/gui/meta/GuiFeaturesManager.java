package com.almworks.items.gui.meta;

import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.ConnectionIconsManager;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBResult;
import com.almworks.items.cache.DBImage;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.export.ExportsCollector;
import com.almworks.items.gui.meta.schema.applicability.Applicabilities;
import com.almworks.items.gui.meta.schema.columns.Columns;
import com.almworks.items.gui.meta.schema.columns.ConditionFeatures;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.items.gui.meta.schema.dnd.DnDChange;
import com.almworks.items.gui.meta.schema.dnd.DnDChangeCollector;
import com.almworks.items.gui.meta.schema.enums.EnumType;
import com.almworks.items.gui.meta.schema.export.Exports;
import com.almworks.items.gui.meta.schema.gui.ViewerField;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.gui.meta.schema.renderers.ItemRenderers;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.model.ValueModel;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.util.List;

public class GuiFeaturesManager implements Startable {
  public static final Role<GuiFeaturesManager> ROLE = Role.role(GuiFeaturesManager.class);
  private final DBImage myImage;
  private final ConnectionIconsManager myConnectionIconsManager;
  private final SyncManager mySyncManager;
  private final List<Provider> myProviders;
  private final List<DBInit> myDBInits;
  private final DetachComposite myLife = new DetachComposite();
  private final EnumTypesCollector myEnumTypes;
  private final DescriptorsCollector myDescriptors;
  private final ModelKeyCollector myModelKeys;
  private final ColumnsCollector myColumns;
  private final FeatureRegistry myFeatureRegistry;
  private final ViewerFieldsCollector myViewerFields;
  private final ItemsTreeLayoutCollector myItemsTreeLayoutCollector;
  private final DnDChangeCollector myDndChanges;
  private final ExportsCollector myExports;
  private final OrdersCollector myReorders;

  public GuiFeaturesManager(DBImage image, ConnectionIconsManager connectionIconsManager, Provider[] providers, DBInit[] dbInits, SyncManager syncManager) {
    myImage = image;
    myConnectionIconsManager = connectionIconsManager;
    mySyncManager = syncManager;
    myFeatureRegistry = new FeatureRegistry();
    myViewerFields = ViewerFieldsCollector.create(image);
    myProviders = Collections15.unmodifiableListCopy(providers);
    myDBInits = Collections15.unmodifiableListCopy(dbInits);
    myEnumTypes = EnumTypesCollector.create(image);
    myDescriptors = DescriptorsCollector.create(image, myEnumTypes);
    myModelKeys = ModelKeyCollector.create(myImage);
    myColumns = ColumnsCollector.create(myImage);
    myItemsTreeLayoutCollector = new ItemsTreeLayoutCollector(myImage);
    myDndChanges = new DnDChangeCollector(myImage);
    myExports = new ExportsCollector(myImage, this);
    myReorders = new OrdersCollector(myImage, this);
  }

  public static GuiFeaturesManager getInstance(DBReader reader) {
    return reader.getDatabaseUserData().getUserData(ROLE);
  }

  public static GuiFeaturesManager getInstance(DBImage image) {
    return image.getDatabase().getUserData().getUserData(ROLE);
  }

  @Override
  public void start() {
    if (!myImage.getDatabase().getUserData().putIfAbsent(ROLE, this))
      LogHelper.error("Another features manager exists");
    myImage.getDatabase().getUserData().putIfAbsent(ConnectionIconsManager.ROLE, myConnectionIconsManager);

    ModelKeys.registerFeatures(myFeatureRegistry);
    Columns.registerFeatures(myFeatureRegistry);
    ItemRenderers.registerFeatures(myFeatureRegistry);
    EnumType.registerFeatures(myFeatureRegistry);
    ViewerField.registerFeatures(myFeatureRegistry);
    Descriptors.registerFeatures(myFeatureRegistry);
    ConditionFeatures.registerFeatures(myFeatureRegistry);
    DnDChange.registerFeatures(myFeatureRegistry);
    Exports.registerFeatures(myFeatureRegistry);
    Applicabilities.registerFeatures(myFeatureRegistry);
    for (Provider provider : myProviders) provider.registerFeatures(myFeatureRegistry);
    DBInitializer.init(mySyncManager, myDBInits);


    myEnumTypes.start(myLife);
    myViewerFields.start(myLife);
    myDescriptors.start(myLife);
    myModelKeys.start(myLife);
    myColumns.start(myLife);
    myItemsTreeLayoutCollector.start(myLife);
    myDndChanges.start(myLife);
    myExports.start(myLife);
    myReorders.start(myLife);
  }

  @Override
  public void stop() {
    myLife.detach();
  }

  @NotNull
  public EnumTypesCollector getEnumTypes() {
    return myEnumTypes;
  }

  public FeatureRegistry getFeatureRegistry() {
    return myFeatureRegistry;
  }

  public ColumnsCollector getColumns() {
    return myColumns;
  }

  public LoadedModelKey<?> findModelKey(DBStaticObject key) {
    return myModelKeys.findKey(key);
  }

  public TableColumnAccessor<LoadedItem, ?> getColumn(DBStaticObject column) {
    return myColumns.get(column);
  }

  public AListModel<ConstraintDescriptor> getDescriptorsModel(Lifespan life, ValueModel<? extends ScopeFilter> scope) {
    return myDescriptors.filterModel(life, scope);
  }

  public AListModel<? extends TableColumnAccessor<LoadedItem, ?>> getColumnsModel(Lifespan life,
    ValueModel<? extends ScopeFilter> scope)
  {
    return myColumns.filterModel(life, scope);
  }

  public <T> LoadedModelKey<T> findScalarKey(DBStaticObject identity, Class<T> valueClass) {
    LoadedModelKey<?> modelKey = findModelKey(identity);
    if (modelKey == null) return null;
    LoadedModelKey<T> result = modelKey.castScalar(valueClass);
    LogHelper.assertError(result != null, "Wrong key value class", valueClass, modelKey);
    return result;
  }

  public <T> LoadedModelKey<List<T>> findListModelKey(DBStaticObject identity, Class<T> elementClass) {
    LoadedModelKey<?> modelKey = findModelKey(identity);
    if (modelKey == null) return null;
    LoadedModelKey<List<T>> listKey = modelKey.castList(elementClass);
    LogHelper.assertError(listKey != null, "Wrong key value class", elementClass, modelKey);
    return listKey;
  }

  public AListModel<ItemsTreeLayout> getTreeLayouts() {
    return myItemsTreeLayoutCollector.getModel();
  }

  public ModelKeyCollector getModelKeyCollector() {
    return myModelKeys;
  }

  public DBImage getImage() {
    return myImage;
  }

  public ViewerFieldsCollector getViewerFields() {
    return myViewerFields;
  }

  public DescriptorsCollector getDescriptors() {
    return myDescriptors;
  }

  @Nullable
  public DnDChange getDnDChange(@Nullable DBAttribute<?> attribute) {
    return myDndChanges.getDnDChange(attribute);
  }

  public List<? extends ItemExport> getExports() {
    return myExports.getAll();
  }

  @Nullable
  public ItemExport findExport(DBStaticObject export) {
    return myExports.findExport(export);
  }

  public AListModel<OrdersCollector.LoadedReorder> getReorders() {
    return myReorders.getModel();
  }


  public interface Provider {
    void registerFeatures(FeatureRegistry featureRegistry);
  }

  public interface DBInit {
    void initialize(DBDrain drain);

    void onFinished(DBResult<?> result);
  }


  private static class DBInitializer implements DownloadProcedure<DBDrain> {
    private final List<DBInit> myDbInits;

    public DBInitializer(List<DBInit> dbInits) {
      myDbInits = dbInits;
    }

    @Override
    public void write(DBDrain drain) throws DBOperationCancelledException {
      for (DBInit dbInit : myDbInits) dbInit.initialize(drain);
    }

    @Override
    public void onFinished(DBResult<?> result) {
      LogHelper.assertError(result.isSuccessful(), "Failed to run DB init");
      for (DBInit dbInit : myDbInits) dbInit.onFinished(result);
    }

    public static void init(SyncManager syncManager, List<DBInit> dbInits) {
      if (dbInits.isEmpty()) return;
      syncManager.writeDownloaded(new DBInitializer(dbInits)).waitForCompletion();
    }
  }
}
