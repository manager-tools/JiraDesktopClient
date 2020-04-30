package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.table.TableColumnModel;

/**
 *
 */
public abstract class ADecoratedTableModel<T> extends ATableModel<T> {
  //private final FireEventSupport<L> myListeners = createListener();

  private Detach myDataDetach = Detach.NOTHING;

  public ADecoratedTableModel(TableColumnModel columnModel) {
    super(columnModel);
  }

  public Detach setDataModel(AListModel<? extends T> dataModel) {
    myDataDetach.detach();
    DetachComposite life = new DetachComposite();
    AListModel<T> decorator = createDecorator(dataModel, life);
    myDataDetach = life;
    return super.setDataModel(decorator);
  }

  public abstract AListModel<T> createDecorator(AListModel<? extends T> dataModel, DetachComposite life);


}
