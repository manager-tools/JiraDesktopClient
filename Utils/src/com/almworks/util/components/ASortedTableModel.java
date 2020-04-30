/*package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.events.FireEventSupport;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.table.TableColumnModel;


class ASortedTableModel<T> extends ATableModel<T> {
  private final FireEventSupport<SortingListener> myListeners =
    FireEventSupport.createSynchronized(SortingListener.class);
  private Detach myDataDetach = Detach.NOTHING;

  ASortedTableModel(TableColumnModel columnModel) {
    super(columnModel);
  }

  public Detach setDataModel(AListModel<? extends T> dataModel) {
    myDataDetach.detach();
    DetachComposite life = new DetachComposite();
    SortedListDecorator<T> decorator = SortedListDecorator.create(life, dataModel, null);
    myDataDetach = life;
    return super.setDataModel(decorator);
  }

  static class SortedTableModelAdapter extends TableModelAdapter {
    public ATableModel createTableModel(TableColumnModel columnModel) {
      return new ASortedTableModel(columnModel);
    }
  }
}
*/
package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import org.almworks.util.detach.DetachComposite;

import javax.swing.table.TableColumnModel;



public class ASortedTableModel<T> extends ADecoratedTableModel<T> {

  ASortedTableModel(TableColumnModel columnModel) {
    super(columnModel);
  }


  public AListModel<T> createDecorator(AListModel<? extends T> dataModel, DetachComposite life) {
    return SortedListDecorator.create(life, dataModel, null);
  }

  /*protected FireEventSupport<SortingListener> createListener() {
    return FireEventSupport.createSynchronized(SortingListener.class);
  }*/

  public static class SortedTableModelAdapter extends TableModelAdapter {
    public ATableModel createTableModel(TableColumnModel columnModel) {
      return new ASortedTableModel(columnModel);
    }
  }
}

