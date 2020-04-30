package com.almworks.util.model;

import com.almworks.util.exec.SeparateEventQueueGate;
import org.almworks.util.detach.Lifespan;
import util.concurrent.SynchronizedBoolean;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ThreadingSquareCollectionTests extends SquareCollectionTests {
  private SeparateEventQueueGate myConvertingGate;
  private SeparateEventQueueGate myBuildingGate;

  protected void setUp() throws Exception {
    super.setUp();
    myConvertingGate = new SeparateEventQueueGate() {
      public void gate(final Runnable runnable) {
        super.gate(new Runnable() {
          public void run() {
            // race condition facilitator
            try {
              Thread.sleep(300);
            } catch (InterruptedException e) {
            }
            runnable.run();
          }
        });
      }
    };
    myConvertingGate.start();
    myBuildingGate = new SeparateEventQueueGate();
    myBuildingGate.start();
  }

  protected void tearDown() throws Exception {
    myConvertingGate.stop();
    myConvertingGate = null;
    myBuildingGate.stop();
    myBuildingGate = null;
    super.tearDown();
  }

  public void testEventOrder() throws InterruptedException {
    final SynchronizedBoolean contentKnown = new SynchronizedBoolean(false);
    final SynchronizedBoolean cellsSet = new SynchronizedBoolean(false);

    SquareCollectionModel.Adapter<String, String, String> listener = new SquareCollectionModel.Adapter<String, String, String>() {
      public void onContentKnown(SquareCollectionModelEvent<String, String, String> event) {
        System.out.println(event);
        assertTrue(contentKnown.commit(false, true));
      }

      public void onRowsAdded(SquareCollectionModelEvent<String, String, String> event) {
        System.out.println(event);
        assertFalse(contentKnown.get());
      }

      public void onColumnsAdded(SquareCollectionModelEvent<String, String, String> event) {
        System.out.println(event);
        assertFalse(contentKnown.get());
      }

      public void onCellsSet(SquareCollectionModelEvent<String, String, String> event) {
        try {
          System.out.println(event);
          assertFalse(contentKnown.get());
        } finally {
          cellsSet.commit(false, true);
        }
      }
    };

    myRows = ArrayListCollectionModel.create(true, false);
    myColumns = ArrayListCollectionModel.create(true, false);
    myModel = ConvertingSquareCollectionModel.create(myRows, myColumns, CONCAT, myConvertingGate, myBuildingGate);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);

    myRows.getWritableCollection().add("x");
    myRows.getWritableCollection().add("y");

    myColumns.getWritableCollection().add("1");
    myColumns.getWritableCollection().add("2");

    myRows.setContentKnown();
    myColumns.setContentKnown();

    contentKnown.waitForValue(true);
    cellsSet.waitForValue(true);
    Thread.sleep(50);
  }
}
