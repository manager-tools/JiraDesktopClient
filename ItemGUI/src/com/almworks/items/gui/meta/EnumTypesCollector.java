package com.almworks.items.gui.meta;

import com.almworks.api.application.*;
import com.almworks.api.application.qb.EnumGrouping;
import com.almworks.api.explorer.util.ItemKeys;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.explorer.qbuilder.filter.EnumConstraintKind;
import com.almworks.explorer.qbuilder.filter.ItemKeyModelCollector;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.AttributeReference;
import com.almworks.items.cache.util.ItemImageCollector;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.enums.EnumType;
import com.almworks.items.gui.meta.schema.enums.IconLoader;
import com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower;
import com.almworks.items.gui.meta.schema.enums.OrderKind;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.PerItemTransactionCache;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnumTypesCollector implements ItemImageCollector.ImageFactory<EnumTypesCollector.Loaded> {
  private static final DataLoader<ItemKeyDisplayName> RENDERER_KIND = SerializedObjectAttribute.create(
    ItemKeyDisplayName.class, EnumType.ItemKeys.RENDERER);
  private static final SerializedObjectAttribute<LoadedEnumNarrower> NARROWER = SerializedObjectAttribute.create(
    LoadedEnumNarrower.class, EnumType.NARROWER);
  private static final AttributeReference<?> UNIQUE_KEY = AttributeReference.create(EnumType.ItemKeys.UNIQUE_KEY);
  private static final SerializedObjectAttribute<IconLoader> ICON_LOADER = SerializedObjectAttribute.create(IconLoader.class, EnumType. ItemKeys.ICON_LOADER);
  private static final DataLoader<OrderKind> ORDER = SerializedObjectAttribute.create(OrderKind.class,
    EnumType.ItemKeys.ORDER);
  private static final DataLoader<List<ItemKeySubloader>> SUBLOADERS = SerializableFeature.SequenceDeserializer.create(ItemKeySubloader.class, EnumType.ItemKeys.SUBLOADERS);

  private final ItemImageCollector<Loaded> myTypes;

  private EnumTypesCollector(ImageSlice slice) {
    myTypes = ItemImageCollector.create(slice, this, true);
  }

  public static EnumTypesCollector create(DBImage image) {
    QueryImageSlice slice = image.createQuerySlice(DPNotNull.create(EnumType.ENUM_TYPE));
    return new EnumTypesCollector(slice);
  }

  public void start(Lifespan life) {
    myTypes.addDataLoaders(UNIQUE_KEY, ICON_LOADER, RENDERER_KIND, ORDER, SUBLOADERS);
    myTypes.addDataLoaders(NARROWER, DataLoader.IDENTITY_LOADER);
    myTypes.addDataLoaders(AttributeLoader.createArray(EnumType.ENUM_TYPE, EnumType.SEARCH_SUBSTRING));
    myTypes.start(life);
  }

  @Nullable
  public Loaded getType(long item) {
    return myTypes.getImage(item);
  }

  public Loaded getType(DBStaticObject object) {
    if (object == null) return null;
    return myTypes.findImageByValue(DataLoader.IDENTITY_LOADER, object.getIdentity());
  }

  public DBImage getImage() {
    return getSlice().getImage();
  }

  private ImageSlice getSlice() {
    return myTypes.getSlice();
  }

  private Database getDatabase() {
    return getImage().getDatabase();
  }

  @Override
  public boolean update(Loaded image, long item) {
    return image.update(myTypes.getSlice());
  }

  @Override
  public void onRemoved(Loaded image) {
    image.stop();
  }

  @Override
  public Loaded create(long item) {
    Loaded loaded = new Loaded(item, this);
    update(loaded, item);
    return loaded;
  }

  public static EnumTypesCollector getInstance(DBReader reader) {
    return GuiFeaturesManager.getInstance(reader).getEnumTypes();
  }

  public static LoadedItemKey getResolvedItem(GuiFeaturesManager manager, DBStaticObject enumType, long item) {
    EnumTypesCollector.Loaded linkTypes = manager.getEnumTypes().getType(enumType);
    if (linkTypes == null) {
      LogHelper.error("Missing enum type", enumType);
      return null;
    }
    return linkTypes.getResolvedItem(item);
  }


  private static final PerItemTransactionCache<LoadedEnumNarrower> NARROWERS = PerItemTransactionCache.create("enumType/narrowers");
  @Nullable
  public static LoadedEnumNarrower getNarrower(DBReader reader, long enumType) {
    LoadedEnumNarrower narrower = NARROWERS.get(reader, enumType);
    if (narrower == null) {
      narrower = NARROWER.getValue(reader, enumType);
      NARROWERS.put(reader, enumType, narrower);
    }
    return narrower;
  }

  public static class Loaded {
    private final EnumTypesCollector myMaster;
    private final Lifecycle myLife = new Lifecycle();
    private final long myItem;
    private ItemKeyModelCollector<LoadedItemKey> myModelCollector = null;
    private boolean mySearchSubstring = false;
    // Guarded by this
    private ItemKeyDescriptor myDescriptor;
    private Long myEnumType;
    private LoadedEnumNarrower myNarrower = LoadedEnumNarrower.DEFAULT;
    private ResolvedFactory<LoadedItemKey> myFactory;
    private final SimpleModifiable myModifiable = new SimpleModifiable();

    public Loaded(long item, EnumTypesCollector master) {
      myItem = item;
      myMaster = master;
    }

    @NotNull
    public LoadedEnumNarrower getNarrower() {
      return myNarrower;
    }

    public boolean update(ImageSlice slice) {
      Threads.assertAWTThread();
      Long enumType = slice.getValue(myItem, EnumType.ENUM_TYPE);
      ItemKeyDescriptor descriptor = getItemKeyDescriptor(slice);
      LoadedEnumNarrower narrower = slice.getValue(myItem, NARROWER);
      ItemKeyDescriptor prevDescriptor;
      Long prevEnumType;
      boolean searchSubstring = Boolean.TRUE.equals(slice.getValue(myItem, EnumType.SEARCH_SUBSTRING));
      LoadedEnumNarrower prevNarrower;
      synchronized (this) {
        prevDescriptor = myDescriptor;
        prevEnumType = myEnumType;
        prevNarrower = myNarrower;
        mySearchSubstring = searchSubstring;
      }
      if (!Util.equals(enumType, prevEnumType)
        || !Util.equals(descriptor, prevDescriptor)
        || !Util.equals(prevNarrower, narrower))
      {
        LogHelper.assertError(prevEnumType == null && prevDescriptor == null && prevNarrower == LoadedEnumNarrower.DEFAULT , "Updating enum type", enumType, descriptor);
        myLife.cycle();
        if (myLife.isDisposed()) return false;
        ItemKeyModelCollector<LoadedItemKey> collector = null;
        Collection<DBAttribute<?>> additional = collectAttributes(narrower);
        ResolvedFactory<LoadedItemKey> factory = descriptor != null ? descriptor.createFactory(additional) : null;
        synchronized (this) {
          myNarrower = narrower;
          myDescriptor = descriptor;
          myEnumType = enumType;
          myFactory = factory;
          if (myFactory != null && myEnumType != null) {
            collector = new ItemKeyModelCollector<LoadedItemKey>(
              myFactory, String.valueOf(myEnumType),
              DPEquals.create(DBAttribute.TYPE, myEnumType), null);
            myModelCollector = collector;
          }
        }
        if (collector != null) {
          collector.start(myLife.lifespan(), myMaster.getDatabase(), myModifiable);
        }
      }
      return true;
    }

    private ItemKeyDescriptor getItemKeyDescriptor(ImageSlice slice) {
      DBAttribute<?> uniqueKey = slice.getValue(myItem, UNIQUE_KEY);
      IconLoader iconLoader = slice.getValue(myItem, ICON_LOADER);
      ItemKeyDisplayName rendererKind = slice.getValue(myItem, RENDERER_KIND);
      OrderKind orderKind = slice.getValue(myItem, ORDER);
      List<ItemKeySubloader> subloaders = slice.getValue(myItem, SUBLOADERS);
      return new ItemKeyDescriptor(uniqueKey, iconLoader, rendererKind, orderKind, subloaders);
    }

    private Collection<DBAttribute<?>> collectAttributes(LoadedEnumNarrower narrower) {
      HashSet<DBAttribute<?>> attributes = Collections15.hashSet();
      narrower.collectValueAttributes(attributes);
      attributes.remove(SyncAttributes.CONNECTION);
      return attributes;
    }

    public void stop() {
      myLife.dispose();
      synchronized (this) {
        myDescriptor = null;
        myEnumType = null;
        myNarrower = null;
        myModelCollector = null;
      }
    }

    @Nullable
    public LoadedItemKey getResolvedItem(long item) {
      ItemKeyModelCollector<LoadedItemKey> collector;
      synchronized (this) {
        collector = myModelCollector;
      }
      return collector != null ? collector.findForItem(item) : null;
    }

    @NotNull
    public List<LoadedItemKey> getResolvedItems(Collection<? extends Long> items) {
      if (items == null) return Collections.emptyList();
      ArrayList<LoadedItemKey> result = Collections15.arrayList();
      for (Long item : items) {
        LoadedItemKey key = getResolvedItem(item);
        if (key != null) result.add(key);
      }
      return result;
    }

    @Nullable
    public ResolvedItem getResolvedItem(ItemKeyCache keyCache, DBReader reader, long enumItem) {
      if (enumItem <= 0) return null;
      List<ResolvedItem> result = Collections15.arrayList(1);
      collectResolvedItems(keyCache, reader, LongArray.create(enumItem), result);
      return result.isEmpty() ? null : result.get(0);
    }

    @Nullable
    public ResolvedItem getResolvedItem(ItemKeyCache keyCache, ItemVersion version, DBAttribute<Long> reference) {
      long item = version.getNNValue(reference, 0l);
      if (item <= 0) return null;
      return getResolvedItem(keyCache, version.getReader(), item);
    }

    public void collectResolvedItems(ItemKeyCache keyCache, DBReader reader, LongList enumItems, List<? super ResolvedItem> target) {
      if (target == null) {
        LogHelper.error("No-null target expected");
        return;
      }
      ResolvedFactory<LoadedItemKey> factory;
      synchronized (this) {
        factory = myFactory;
      }
      if (factory == null) return;
      for (int i = 0; i < enumItems.size(); i++) {
        try {
          LoadedItemKey itemKey = keyCache.getItemKey(enumItems.get(i), reader, factory);
          target.add(itemKey);
        } catch (BadItemException e) {
          Log.error(e);
        }
      }
    }

    public void collectResolvedItems(ItemKeyCache keyCache, ItemVersion version, DBAttribute<? extends Collection<? extends Long>> references,
      List<? super ResolvedItem> target) {
      collectResolvedItems(keyCache, version.getReader(), version.getLongSet(references), target);
    }

    public void resolveItemId(String itemId, ItemHypercubeImpl cube, List<ResolvedItem> target) {
      ItemKeyModelCollector<LoadedItemKey> collector;
      LoadedEnumNarrower narrower;
      synchronized (this) {
        collector = myModelCollector;
        narrower = myNarrower;
      }
      if (collector == null || narrower == null) return;
      ItemKeys.resolveItemId(itemId, cube, target, narrower, collector);
    }

    public BaseEnumConstraintDescriptor createEnumDescriptor(DBAttribute<?> attribute, String displayName,
      @Nullable ItemKey notSet, EnumConstraintKind kind, List<EnumGrouping> grouping,
      Convertor<ItemKey, String> filterConvertor, String id)
    {
      LoadedEnumNarrower narrower;
      boolean searchSubstring;
      ItemKeyModelCollector<LoadedItemKey> collector;
      synchronized (this) {
        narrower = myNarrower;
        searchSubstring = mySearchSubstring;
        collector = myModelCollector;
      }
      return BaseEnumConstraintDescriptor.create(attribute, narrower, displayName, notSet, kind, grouping,
        filterConvertor, null, null, searchSubstring, collector, id);
    }

    public AListModel<LoadedItemKey> getValueModel(Lifespan life, ItemHypercube cube) {
      LoadedEnumNarrower narrower;
      ItemKeyModelCollector<LoadedItemKey> collector;
      synchronized (this) {
        narrower = myNarrower;
        collector = myModelCollector;
      }
      return narrower.narrowModel(life, collector.getModel(), cube);
    }

    @ThreadSafe
    @NotNull
    public List<LoadedItemKey> getEnumValues(ItemHypercube cube) {
      LoadedEnumNarrower narrower;
      ItemKeyModelCollector<LoadedItemKey> collector;
      synchronized (this) {
        narrower = myNarrower;
        collector = myModelCollector;
      }
      if (narrower == null || collector == null) return Collections.emptyList();
      return narrower.narrowList(collector.getAllItemKeys(), cube);
    }

    public Modifiable getModifiable() {
      return myModifiable;
    }

    public EnumTypesCollector getEnumTypes() {
      return myMaster;
    }

    public long getItem() {
      return myItem;
    }
  }
}
