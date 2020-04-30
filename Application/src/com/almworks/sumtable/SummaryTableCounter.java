package com.almworks.sumtable;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.items.api.*;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.DetachableValue;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.DoubleBottleneck;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class SummaryTableCounter {
  private final AListModel<STFilter> myColumnModel;
  private final AListModel<STFilter> myRowModel;
  private final AListModel<STFilter> myCounterModel;
  private final DetachableValue<GenericNode> myNode = DetachableValue.create();
  private final BasicScalarModel<CountingSummaryTableData> myDataModel = BasicScalarModel.createWithValue(null, true);

  private CountingSummaryTableData myCountingData = null;

  private final Procedure<CountingSummaryTableData> myDataFinish = new Procedure<CountingSummaryTableData>() {
    public void invoke(CountingSummaryTableData data) {
      if (data == myCountingData) {
        myDataModel.setValue(data);
      }
    }
  };

  private final DoubleBottleneck myRecountBottleneck = new DoubleBottleneck(50, 500, ThreadGate.AWT, new Runnable() {
    public void run() {
      recount();
    }
  });

  private final Bottleneck myDelayedRecounter = new Bottleneck(500, ThreadGate.STRAIGHT, myRecountBottleneck);

  public SummaryTableCounter(AListModel<STFilter> columnModel, AListModel<STFilter> rowModel, AListModel<STFilter> counterModel) {
    myColumnModel = columnModel;
    myRowModel = rowModel;
    myCounterModel = counterModel;
  }

  public ScalarModel<CountingSummaryTableData> getDataModel() {
    return myDataModel;
  }

  @ThreadAWT
  public void attach(Lifespan lifespan, Database db, @NotNull GenericNode node) {
    if (!node.isNode())
      return;
    myNode.set(lifespan, node);
    myColumnModel.addChangeListener(lifespan, ThreadGate.STRAIGHT, myRecountBottleneck);
    myRowModel.addChangeListener(lifespan, ThreadGate.STRAIGHT, myRecountBottleneck);
    myCounterModel.addChangeListener(lifespan, ThreadGate.STRAIGHT, myRecountBottleneck);
    node.getQueryResult().addChangeListener(lifespan, ThreadGate.STRAIGHT, myRecountBottleneck);

    db.addListener(lifespan, new DBListener() {
      public void onDatabaseChanged(DBEvent event, DBReader reader) {
        myRecountBottleneck.requestDelayed();
      }
    });
    lifespan.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        if (myCountingData != null) myCountingData.cancelCounting();
      }
    });

    recount();
  }

  @ThreadAWT
  private void recount() {
    GenericNode node = myNode.get();
    if (node == null || !node.isNode())
      return;
    CountingSummaryTableData data = myCountingData;
    if (data != null) {
      data.cancelCounting();
    }
    List<STFilter> columns = myColumnModel.toList();
    List<STFilter> rows = myRowModel.toList();
    List<STFilter> counters = myCounterModel.toList();

    final DBFilter filter = node.getQueryResult().getDbFilter();
    if (filter == null) {
      myDelayedRecounter.requestDelayed();
      return;
    }
    myCountingData = new CountingSummaryTableData(columns, rows, counters, filter, myDelayedRecounter, myDataFinish);
    myCountingData.startCounting();
  }
}
