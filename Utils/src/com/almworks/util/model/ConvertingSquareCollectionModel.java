package com.almworks.util.model;

import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import util.concurrent.SynchronizedBoolean;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ConvertingSquareCollectionModel <R, C, V> extends AbstractContentModel<
  SquareCollectionModelEvent<R, C, V>,
  SquareCollectionModel.Consumer<R, C, V>,
  SquareCollectionModel<R, C, V>,
  SquareCollectionModelSetter<R, C, V>
  >
  implements SquareCollectionModel<R, C, V>, SquareCollectionModelSetter<R, C, V> {

  // todo make fast and less consuming implementation
  private final Map<R, Map<C, V>> myValues = Collections15.hashMap();
  private final Set<C> myColumns = Collections15.hashSet();

  private final CollectionModel<R> myRowsModel;
  private final CollectionModel<C> myColumnsModel;
  private final Convertor<Pair<R, C>, V> myConvertor;
  private final ThreadGate myConvertingGate;
  private final ThreadGate myBuildingGate;

  private boolean myValuesRequested = false;
  private final SynchronizedBoolean myColumnsKnown = new SynchronizedBoolean(false, myLock);
  private final SynchronizedBoolean myRowsKnown = new SynchronizedBoolean(false, myLock);

  private ConvertingSquareCollectionModel(CollectionModel<R> rowsModel, CollectionModel<C> columnsModel,
    Convertor<Pair<R, C>, V> convertor, ThreadGate convertingGate, ThreadGate buildingGate) {

    super(rowsModel.isContentKnown() && columnsModel.isContentKnown(),
      rowsModel.isContentChangeable() && columnsModel.isContentChangeable(),
      (Class) SquareCollectionModel.Consumer.class);

    assert convertor != null;
    assert convertingGate != null;

    myRowsModel = rowsModel;
    myColumnsModel = columnsModel;
    myConvertor = convertor;
    myConvertingGate = convertingGate;
    myBuildingGate = buildingGate;
    requestContent();
  }

  public void requestContent() {
    super.requestContent();
    if (myValuesRequested)
      return;
    myValuesRequested = true;

    myColumnsModel.getEventSource().addListener(Lifespan.FOREVER, myBuildingGate, new CollectionModel.Adapter<C>() {
      public void onScalarsAdded(CollectionModelEvent<C> event) {
        columnsAdded(event.getScalars());
      }

      public void onScalarsRemoved(CollectionModelEvent<C> event) {
        throw new UnsupportedOperationException();
      }

      public void onContentKnown(CollectionModelEvent<C> event) {
        checkFullyKnown(myColumnsKnown);
      }
    });

    myRowsModel.getEventSource().addListener(Lifespan.FOREVER, myBuildingGate, new CollectionModel.Adapter<R>() {
      public void onScalarsAdded(CollectionModelEvent<R> event) {
        rowsAdded(event.getScalars());
      }

      public void onScalarsRemoved(CollectionModelEvent<R> event) {
        throw new UnsupportedOperationException();
      }

      public void onContentKnown(CollectionModelEvent<R> event) {
        checkFullyKnown(myRowsKnown);
      }
    });

    myColumnsModel.requestContent();
    myRowsModel.requestContent();
  }

  private void checkFullyKnown(final SynchronizedBoolean rowsOrColumns) {
    myConvertingGate.execute(new Runnable() {
      public void run() {
        Consumer<R, C, V> dispatcher = null;
        synchronized (myLock) {
          if (!rowsOrColumns.commit(false, true))
            return;
          if (myColumnsKnown.get() && myRowsKnown.get())
            dispatcher = myEventSupport.getDispatcherSnapshot();
        }
        if (dispatcher != null)
          dispatcher.onContentKnown(createDefaultEvent());
      }
    });
  }

  private void rowsAdded(Object[] rows) {
    if (rows.length == 0)
      return;
    Object[] rowsToProcess = null;
    Object[] columnsToProcess = null;
    SquareCollectionModel.Consumer<R, C, V> dispatcher = null;
    synchronized (myLock) {
      List<R> addedRows = Collections15.arrayList(rows.length);
      for (int i = 0; i < rows.length; i++) {
        R row = (R) rows[i];
        Map<C, V> rowData = myValues.get(row);
        if (rowData == null) {
          rowData = Collections15.hashMap();
          myValues.put(row, rowData);
          addedRows.add(row);
        }
      }
      rowsToProcess = addedRows.toArray();
      dispatcher = myEventSupport.getDispatcherSnapshot();
      columnsToProcess = myColumns.toArray();
    }
    if (dispatcher != null && rowsToProcess.length > 0)
      dispatcher.onRowsAdded(SquareCollectionModelEvent.createRows(this, rowsToProcess));
    setTableData(rowsToProcess, columnsToProcess);
  }

  private void columnsAdded(Object[] columns) {
    if (columns.length == 0)
      return;
    Object[] columnsToProcess = null;
    Object[] rowsToProcess = null;
    SquareCollectionModel.Consumer<R, C, V> dispatcher = null;
    synchronized (myLock) {
      List<C> addedColumns = Collections15.arrayList(columns.length);
      for (int i = 0; i < columns.length; i++) {
        C column = (C) columns[i];
        if (myColumns.add(column))
          addedColumns.add(column);
      }
      columnsToProcess = addedColumns.toArray();
      dispatcher = myEventSupport.getDispatcherSnapshot();
      rowsToProcess = myValues.keySet().toArray();
    }
    if (dispatcher != null && columnsToProcess.length > 0)
      dispatcher.onColumnsAdded(SquareCollectionModelEvent.createColumns(this, columnsToProcess));
    setTableData(rowsToProcess, columnsToProcess);
  }

  private void setTableData(final Object[] finalRows, final Object[] finalColumns) {
    if (finalRows.length > 0 && finalColumns.length > 0) {
      myConvertingGate.execute(new Runnable() {
        public void run() {
          Object[][] data = new Object[finalRows.length][];
          Consumer<R, C, V> dispatcher = null;
          synchronized (myLock) {
            for (int i = 0; i < finalRows.length; i++) {
              R row = (R) finalRows[i];
              data[i] = new Object[finalColumns.length];
              Map<C, V> rowData = myValues.get(row);
              for (int j = 0; j < finalColumns.length; j++) {
                C column = (C) finalColumns[j];
                V value = myConvertor.convert(Pair.create(row, column));
                rowData.put(column, value);
                data[i][j] = value;
              }
            }
            dispatcher = myEventSupport.getDispatcherSnapshot();
          }
          if (dispatcher != null)
            dispatcher.onCellsSet(SquareCollectionModelEvent.create(ConvertingSquareCollectionModel.this,
              finalRows, finalColumns, data));
        }
      });
    }
  }


  public boolean isContentKnown() {
    synchronized (myLock) {
      return myRowsKnown.get() && myColumnsKnown.get();
    }
  }

  public boolean isChangeable() {
    return myRowsModel.isContentChangeable() || myColumnsModel.isContentChangeable();
  }

  public Object afterAddListenerWithLock(ThreadGate threadGate, Consumer<R, C, V> consumer, Object passThrough) {

    SquareCollectionModelEvent[] events = new SquareCollectionModelEvent[4];
    Object[] rows = myValues.keySet().toArray();
    Object[] columns = myColumns.toArray();
    Object[][] data = new Object[rows.length][];
    for (int i = 0; i < rows.length; i++) {
      data[i] = new Object[columns.length];
      Map rowData = myValues.get(rows[i]);
      for (int j = 0; j < columns.length; j++)
        data[i][j] = rowData.get(columns[j]);
    }
    events[0] = SquareCollectionModelEvent.createColumns(this, columns);
    events[1] = SquareCollectionModelEvent.createRows(this, rows);
    events[2] = SquareCollectionModelEvent.create(this, rows, columns, data);
    events[3] = isContentKnown() ? createDefaultEvent() : null;
    return events;
  }

  public void afterAddListenerWithoutLock(ThreadGate threadGate, final Consumer<R, C, V> consumer, Object passThrough) {

    final SquareCollectionModelEvent[] events = (SquareCollectionModelEvent[]) passThrough;
    threadGate.execute(new Runnable() {
      public void run() {
        boolean columns = events[0].getColumns().length > 0;
        boolean rows = events[1].getRows().length > 0;
        if (columns)
          consumer.onColumnsAdded(events[0]);
        if (rows)
          consumer.onRowsAdded(events[1]);
        if (columns && rows)
          consumer.onCellsSet(events[2]);
        if (events[3] != null)
          consumer.onContentKnown(events[3]);
      }
    });
  }

  public void setContentKnown() {
    throw new UnsupportedOperationException();
  }

  public SquareCollectionModelEvent<R, C, V> createDefaultEvent() {
    return new SquareCollectionModelEvent<R, C, V>(this, null, null, null);
  }

  public static <R, C, V> ConvertingSquareCollectionModel<R, C, V> create(CollectionModel<R> rowsModel,
    CollectionModel<C> columnsModel, Convertor<Pair<R, C>, V> convertor, ThreadGate convertingGate,
    ThreadGate buildingGate) {

    return new ConvertingSquareCollectionModel<R, C, V>(rowsModel, columnsModel, convertor, convertingGate,
      buildingGate);
  }

  public static <R, C, V> ConvertingSquareCollectionModel<R, C, V> createStraight(CollectionModel<R> rowsModel,
    CollectionModel<C> columnsModel, Convertor<Pair<R, C>, V> convertor) {

    return create(rowsModel, columnsModel, convertor, ThreadGate.STRAIGHT, ThreadGate.STRAIGHT);
  }

  public static <R, C, V> ConvertingSquareCollectionModel<R, C, V> createLong(CollectionModel<R> rowsModel,
    CollectionModel<C> columnsModel, Convertor<Pair<R, C>, V> convertor, Object sequenceKey) {

    ThreadGate gate = ThreadGate.LONG(sequenceKey);
    return create(rowsModel, columnsModel, convertor, gate, gate);
  }
}
