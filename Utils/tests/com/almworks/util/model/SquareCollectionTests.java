package com.almworks.util.model;

import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.Lifespan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SquareCollectionTests extends BaseTestCase {
  protected ConvertingSquareCollectionModel<String, String, String> myModel;
  protected ArrayListCollectionModel<String> myRows;
  protected ArrayListCollectionModel<String> myColumns;

  protected void tearDown() throws Exception {
    myModel = null;
    myRows = null;
    myColumns = null;
  }

  protected static final Convertor<Pair<String, String>, String> CONCAT = new Convertor<Pair<String, String>, String>() {
    public String convert(Pair<String, String> pair) {
      return pair.getFirst().concat(pair.getSecond());
    }
  };

  public void testBasic() {
    ArrayListCollectionModel rows = ArrayListCollectionModel.create(true, Arrays.asList(new String[]{"x", "y"}));
    ArrayListCollectionModel columns = ArrayListCollectionModel.create(true, Arrays.asList(new String[]{"1", "2"}));
    myModel = ConvertingSquareCollectionModel.createStraight(rows, columns, CONCAT);

    final List<String> nrows = new ArrayList<String>();
    final List<String> ncols = new ArrayList<String>();
    final List<String> ncells = new ArrayList<String>();

    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new SquareCollectionModel.Consumer<String, String, String>() {
      public void onRowsAdded(SquareCollectionModelEvent<String, String, String> event) {
        Object[] rows = event.getRows();
        for (int i = 0; i < rows.length; i++)
          nrows.add((String) rows[i]);
      }

      public void onColumnsAdded(SquareCollectionModelEvent<String, String, String> event) {
        Object[] columns = event.getColumns();
        for (int i = 0; i < columns.length; i++)
          ncols.add((String) columns[i]);
      }

      public void onCellsSet(SquareCollectionModelEvent<String, String, String> event) {
        Object[] columns = event.getColumns();
        Object[] rows = event.getRows();
        Object[][] data = event.getValues();
        for (int i = 0; i < rows.length; i++)
          for (int j = 0; j < columns.length; j++)
            ncells.add((String) data[i][j]);
      }

      public void onContentKnown(SquareCollectionModelEvent<String, String, String> event) {
      }
    });

    check(nrows, new String[]{"x", "y"});
    check(ncols, new String[]{"1", "2"});
    check(ncells, new String[]{"x1", "x2", "y1", "y2"});

    rows.getWritableCollection().add("z");
    check(nrows, new String[]{"z"});
    check(ncols, new String[]{});
    check(ncells, new String[]{"z1", "z2"});

    columns.getWritableCollection().add("3");
    check(nrows, new String[]{});
    check(ncols, new String[]{"3"});
    check(ncells, new String[]{"x3", "y3", "z3"});
  }

  public void testContentKnown() {
    final boolean[] notified = {false};
    SquareCollectionModel.Adapter<String, String, String> listener = new SquareCollectionModel.Adapter<String, String, String>() {
      public void onContentKnown(SquareCollectionModelEvent<String, String, String> event) {
        assertEquals(myModel, event.getSource());
        assertFalse(Thread.holdsLock(myModel.getLock()));
        notified[0] = true;
      }
    };

    myRows = ArrayListCollectionModel.create(true, true);
    myColumns = ArrayListCollectionModel.create(true, true);
    myModel = ConvertingSquareCollectionModel.createStraight(myRows, myColumns, CONCAT);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    assertTrue(notified[0]);
    notified[0] = false;

    myRows = ArrayListCollectionModel.create(true, false);
    myColumns = ArrayListCollectionModel.create(true, true);
    myModel = ConvertingSquareCollectionModel.createStraight(myRows, myColumns, CONCAT);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    assertFalse(notified[0]);
    myRows.getWritableCollection().add("haba");
    myRows.setContentKnown();
    assertTrue(notified[0]);
    notified[0] = false;

    myRows = ArrayListCollectionModel.create(true, true);
    myColumns = ArrayListCollectionModel.create(true, false);
    myModel = ConvertingSquareCollectionModel.createStraight(myRows, myColumns, CONCAT);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    assertFalse(notified[0]);
    myColumns.getWritableCollection().add("haba");
    myColumns.setContentKnown();
    assertTrue(notified[0]);
    notified[0] = false;

    myRows = ArrayListCollectionModel.create(true, false);
    myColumns = ArrayListCollectionModel.create(true, false);
    myModel = ConvertingSquareCollectionModel.createStraight(myRows, myColumns, CONCAT);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    assertFalse(notified[0]);
    myColumns.getWritableCollection().add("haba");
    myColumns.setContentKnown();
    assertFalse(notified[0]);
    myRows.getWritableCollection().add("daba");
    myRows.setContentKnown();
    assertTrue(notified[0]);
    notified[0] = false;
  }

  private void check(List<String> list, String[] sample) {
    String[] actual = list.toArray(new String[list.size()]);
    Arrays.sort(actual);
    Arrays.sort(sample);
    assertTrue(Arrays.equals(actual, sample));
    list.clear();
  }
}
