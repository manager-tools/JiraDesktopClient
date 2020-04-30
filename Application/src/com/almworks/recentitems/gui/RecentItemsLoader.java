package com.almworks.recentitems.gui;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.explorer.ItemModelRegistry;
import com.almworks.explorer.loader.LoadedItemImpl;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DBLiveQuery;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.recentitems.RecentItemUtil;
import com.almworks.recentitems.RecentItemsService;
import com.almworks.recentitems.RecordType;
import com.almworks.util.Pair;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.properties.Role;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class RecentItemsLoader implements Startable, DBLiveQuery.Listener {
  public static final Role<RecentItemsLoader> ROLE =
    Role.role("RecentItemsLoader", RecentItemsLoader.class);

  private final Database myDatabase;
  private final ApplicationLoadStatus myAppStatus;
  private final ItemModelRegistry myRegistry;

  private final DetachComposite myLife = new DetachComposite();
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final OrderListModel<LoadedRecord> myLoadedModel = OrderListModel.create();

  private volatile boolean myProcessingEvent = false;
  private volatile boolean myFirstDbEvent = true;

  private final SortedListDecorator<LoadedRecord> mySortedModel = SortedListDecorator.create(
    myLife, myLoadedModel, new Comparator<LoadedRecord>() {
      @Override
      public int compare(LoadedRecord o1, LoadedRecord o2) {
        return o2.myTimestamp.compareTo(o1.myTimestamp);
      }
    }
  );

  private final FilteringListDecorator<LoadedRecord> myViewModel = FilteringListDecorator.create(
    myLife, mySortedModel,
    new Condition<LoadedRecord>() {
      @Override
      public boolean isAccepted(LoadedRecord value) {
        final LoadedItem item = value.myItem;
        return item != null && item.getConnection() != null && !item.services().isDeleted();
      }
    });

  public RecentItemsLoader(Database database, ApplicationLoadStatus appStatus, ItemModelRegistry registry) {
    myDatabase = database;
    myAppStatus = appStatus;
    myRegistry = registry;
  }

  @Override
  public void start() {
    myLife.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        for(final LoadedRecord rec : myLoadedModel) {
          if(rec != null && rec.myLife != null) {
            rec.myLife.detach();
          }
        }
      }
    });
    
    ModelUtils.whenTrue(
      myAppStatus.getApplicationLoadedModel(), ThreadGate.STRAIGHT,
      new Runnable() {
        @Override
        public void run() {
          myDatabase.liveQuery(myLife, RecentItemsService.EXPR_RECORDS, RecentItemsLoader.this);
          myViewModel.addAWTChangeListener(myLife, new ChangeListener() {
            @Override
            public void onChange() {
              if(!myProcessingEvent) {
                myModifiable.fireChanged();
              }
            }
          });
        }
      });
  }

  @Override
  public void stop() {
    myLife.detach();
  }

  @Override
  public void onICNPassed(long icn) {
  }

  @Override
  public void onDatabaseChanged(DBEvent event, DBReader reader) {
    final LongList addedLongs;
    if(myFirstDbEvent) {
      addedLongs = event.getAddedAndChangedSorted();
      myFirstDbEvent = false;
    } else {
      addedLongs = event.getAddedSorted();
    }

    final List<LoadedRecord> added = Collections15.linkedList();
    for(final LongIterator it = addedLongs.iterator(); it.hasNext();) {
      final LoadedRecord record = loadRecord(it.nextValue(), reader);
      if(record != null) {
        added.add(record);
      }
    }

    final LongList removed = event.getRemovedSorted();

    if(added.isEmpty() && removed.isEmpty()) {
      return;
    }

    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        updateModel(added, removed);
      }
    });
  }

  @Nullable
  private LoadedRecord loadRecord(long record, DBReader reader) {
    final long item = reader.getValue(record, RecentItemsService.ATTR_MASTER);
    if(!RecentItemUtil.checkItem(item, reader)) {
      return null;
    }

    final Pair<LoadedItemImpl, DetachComposite> pair = loadItem(item, reader);
    if (pair == null) return null;
    final LoadedItem loaded = pair.getFirst();
    final DetachComposite recordLife = pair.getSecond();
    if(loaded == null) return null;

    setupViewModelResync(loaded, recordLife);

    return new LoadedRecord(
      new Date(reader.getValue(record, RecentItemsService.ATTR_TIMESTAMP)),
      loaded, recordLife,
      RecordType.forId(reader.getValue(record, RecentItemsService.ATTR_REC_TYPE)),
      record);
  }

  @Nullable
  private Pair<LoadedItemImpl, DetachComposite> loadItem(long key, DBReader reader) {
    final DetachComposite life = new DetachComposite();
    LoadedItemImpl loadedItem = LoadedItemImpl.createLive(life, myRegistry, key, reader);
    return loadedItem != null ? Pair.create(loadedItem, life) : null;
  }

  private void setupViewModelResync(final LoadedItem item, DetachComposite recordLife) {
    final ChangeListener1<LoadedItem> listener = new ChangeListener1<LoadedItem>() {
      @Override
      public void onChange(LoadedItem object) {
        myViewModel.resynch();
      }
    };
    item.addAWTListener(listener);
    recordLife.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        item.removeAWTListener(listener);
      }
    });
  }

  private void updateModel(List<LoadedRecord> added, LongList removed) {
    myProcessingEvent = true;
    try {
      for(final LongIterator itr = removed.iterator(); itr.hasNext();) {
        final long record = itr.nextValue();
        for(final Iterator<LoadedRecord> itl = myLoadedModel.iterator(); itl.hasNext();) {
          if(itl.next().myKey == record) {
            itl.remove();
            break;
          }
        }
      }
      myLoadedModel.addAll(added);
    } finally {
      myProcessingEvent = false;
    }
    myModifiable.fireChanged();
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public AListModel<LoadedRecord> getLoadedModel() {
    return myViewModel;
  }
}
