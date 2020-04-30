package com.almworks.items.sync.edit;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.tests.TimeGuard;
import com.almworks.util.ui.actions.CantPerformException;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectIterator;
import junit.framework.Assert;
import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestEditFactory implements EditorFactory {
  private static final CollectionsCompare CHECK = new CollectionsCompare();
  private final List<MyEditor> myNewEditors = Collections15.arrayList();
  // Guarded by myNewEditors
  private final List<MyEditor> myAliveEditors = Collections15.arrayList();
  private final SyncManager myManager;

  TestEditFactory(SyncManager manager) {
    myManager = manager;
  }

  public MyEditor edit(long item) throws InterruptedException {
    startEdit(item);
    return waitForEditor();
  }

  public EditControl startEdit(long item) {
    EditControl control = myManager.prepareEdit(item);
    Assert.assertNotNull(control);
    control.start(this);
    return control;
  }

  @Override
  public ItemEditor prepareEdit(DBReader reader, EditPrepare prepare) {
    Assert.assertEquals(1, prepare.getItems().size());
    long item = prepare.getItems().get(0);
    AttributeMap shadowables = SyncUtils.readTrunk(reader, item).getAllShadowableMap();
    MyEditor editor = new MyEditor(this, item, prepare.getControl(), shadowables);
    synchronized (myNewEditors) {
      myNewEditors.add(editor);
      myAliveEditors.add(editor);
      myNewEditors.notifyAll();
    }
    return editor;
  }

  @Override
  public void editCancelled() {
    LogHelper.error("Not implemented yet");
  }

  public MyEditor waitForEditor() throws InterruptedException {
    MyEditor editor = TimeGuard.waitFor(new Procedure<TimeGuard<MyEditor>>() {
      @Override
      public void invoke(TimeGuard<MyEditor> arg) {
        synchronized (myNewEditors) {
          if (myNewEditors.size() > 0) {
            arg.setResult(myNewEditors.remove(0));
            Assert.assertEquals(0, myNewEditors.size());
          }
        }
      }
    });
    GUITestCase.flushAWTQueue();
    return editor;
  }

  private void editorReleased(MyEditor editor) {
    synchronized (myNewEditors) {
      CHECK.contains(editor, myAliveEditors);
      myAliveEditors.remove(editor);
      myNewEditors.remove(editor);
      myNewEditors.notifyAll();
    }
  }

  public static class MyEditor implements ItemEditor {
    private final TestEditFactory myFactory;
    private final EditControl myControl;
    private final long myItem;
    private final AttributeMap myInitialValues;
    private final TLongObjectHashMap<ItemValues> myConcurrentChanges = new TLongObjectHashMap<>();
    private boolean myAlive = true;
    private boolean myShown = false;
    private boolean myReleased = false;
    private Boolean mySuccessful = null;

    private MyEditor(TestEditFactory factory, long item, EditControl control, AttributeMap initialValues) {
      myFactory = factory;
      myItem = item;
      myControl = control;
      myInitialValues = initialValues;
    }

    @Override
    public synchronized boolean isAlive() {
      Assert.assertFalse(myReleased);
      Assert.assertTrue(myShown);
      return myAlive;
    }

    @Override
    public synchronized void showEditor() throws CantPerformException {
      Assert.assertFalse(myShown);
      myShown = true;
      notifyAll();
    }

    @Override
    public void onItemsChanged(TLongObjectHashMap<ItemValues> newValues) {
      synchronized (myConcurrentChanges) {
        for (TLongObjectIterator<ItemValues> it = newValues.iterator(); it.hasNext();) {
          it.advance();
          long item = it.key();
          ItemValues values = it.value();
          if (myConcurrentChanges.containsKey(item)) Assert.fail("Already changed " + item);
          else myConcurrentChanges.put(item, values);
        }
      }
    }

    public void checkChanges(long item, AttributeMap expectedChange) {
      synchronized (myConcurrentChanges) {
        ItemValues map = myConcurrentChanges.get(item);
        if (map == null) Assert.assertNull(expectedChange);
        else Assert.assertTrue(map.equalValues(expectedChange));
        myConcurrentChanges.remove(item);
      }
    }

    public <T> void checkChange(long item, DBAttribute<T> attribute, T expectedNewValue) {
      checkChanges(item, AttributeMap.singleton(attribute, expectedNewValue));
    }

    public void checkNoMoreChanges() {
      synchronized (myConcurrentChanges) {
        Assert.assertTrue(myConcurrentChanges.isEmpty());
      }
    }

    public MyEditor commit(final AttributeMap values) {
      final AttributeMap map = new AttributeMap();
      map.putAll(values);
      return commit(Collections.singletonMap(myItem, map));
    }

    public MyEditor commit(Map<Long, AttributeMap> itemValues) {
      GUITestCase.flushAWTQueue();
      synchronized (this) {
        Assert.assertTrue(myShown);
        Assert.assertFalse(myReleased);
      }
      final List<CommitItemEdit> commits = Collections15.arrayList();
      LongList locked = myControl.getItems();
      for (Map.Entry<Long, AttributeMap> entry : itemValues.entrySet()) {
        Long item = entry.getKey();
        if (item > 0) Assert.assertTrue(locked.contains(item));
        commits.add(new CommitItemEdit(entry.getValue(), myControl, item));
      }

      myControl.commit(new EditCommit() {
        @Override
        public void performCommit(EditDrain drain) throws DBOperationCancelledException {
          for (CommitItemEdit commit : commits) commit.performCommit(drain);
        }

        @Override
        public void onCommitFinished(boolean success) {
          Assert.assertTrue(success);
          synchronized (this) {
            Assert.assertNull(mySuccessful);
            mySuccessful = success;
          }
          for (CommitItemEdit commit : commits) commit.onCommitFinished(success);
        }
      });
      return this;
    }

    @Override
    public void activate() throws CantPerformException {
      assert false;
    }

    @Override
    public void onEditReleased() {
      synchronized (this) {
        Assert.assertTrue(myShown);
        myReleased = true;
        notifyAll();
      }
      myFactory.editorReleased(this);
    }

    public <T> MyEditor commit(DBAttribute<T> attribute, T value) {
      AttributeMap map = new AttributeMap();
      map.put(attribute, value);
      return commit(map);
    }

    public void waitReleased() throws InterruptedException {
      waitFor(null, true, null);
    }

    private void waitFor(final Boolean shown, final Boolean released, final Boolean success) throws InterruptedException {
      if (shown == null && released == null) return;
      TimeGuard.waitFor(new Procedure<TimeGuard<Object>>() {
        @Override
        public void invoke(TimeGuard<Object> arg) {
          if (shown != null && shown != myShown) return;
          if (released != null && released != myReleased) return;
          if (success != null && !success.equals(mySuccessful)) return;
          arg.setResult(null);
        }
      });
    }

    public void cancel() {
      myControl.release();
    }

    public EditorLock getLock() {
      return myControl;
    }

    public <T> T getInitialValue(DBAttribute<T> attribute) {
      return myInitialValues.get(attribute);
    }

    public <T> void checkInitialValue(DBAttribute<T> attribute, T expected) {
      Assert.assertEquals(expected, getInitialValue(attribute));
    }
  }
}
