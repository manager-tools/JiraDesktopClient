package com.almworks.items.gui.edit.helper;

import com.almworks.api.container.ComponentContainer;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.sync.EditControl;
import com.almworks.items.sync.EditorLock;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.L;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import com.almworks.util.ui.actions.CantPerformExceptionSilently;
import org.almworks.util.Collections15;

import java.util.Map;

public class EditItemHelper {
  public static final Role<EditItemHelper> ROLE = Role.role(EditItemHelper.class);
  private final Map<Pair<EditFeature, Object>, NoLockController> myNewWindows = Collections15.hashMap();
  private final ComponentContainer myContainer;

  EditItemHelper(ComponentContainer container) {
    myContainer = container;
  }

  public void registerWindow(NoLockController controller, EditFeature feature, Object contextKey) {
    LogHelper.assertError(contextKey != null, "Null context key", feature);
    Pair<EditFeature, Object> key = Pair.create(feature, contextKey);
    if (myNewWindows.containsKey(key)) {
      LogHelper.error("Already registered", feature, contextKey);
      return;
    }
    myNewWindows.put(key, controller);
  }

  public void startCreateNewItem(ActionContext context, EditFeature editor, EditDescriptor descriptor) throws CantPerformException {
    Threads.assertAWTThread();
    Object contextKey = descriptor.getContextKey();
    if (contextKey != null) {
      NoLockController window = myNewWindows.get(Pair.create(editor, contextKey));
      if (window != null) {
        window.activate();
        throw new CantPerformExceptionSilently("Window activated");
      }
    }
    LongArray itemsToLock = new LongArray();
    DefaultEditModel.Root model = editor.setupModel(context, itemsToLock);
    LogHelper.assertError(itemsToLock.isEmpty(), itemsToLock, editor, contextKey, descriptor);
    NoLockController.start(this, editor, model, descriptor, contextKey);

  }

  public void startEdit(LongList toLock, EditFeature editor, EditDescriptor descriptor, DefaultEditModel.Root model) throws CantPerformException {
    EditorLock lockingEditor = null;
    for (LongListIterator i = toLock.iterator(); i.hasNext();) {
      EditorLock lock = getSyncManager().findLock(i.nextValue());
      if (lockingEditor != null && lock != lockingEditor) {
        throw new CantPerformExceptionExplained(
          L.content(Local.parse("Some " + Terms.ref_artifacts + " are already being edited")));
      }
      lockingEditor = lock;
    }
    if (lockingEditor != null) {
      lockingEditor.activateEditor();
      throw new CantPerformExceptionSilently("focused another editor");
    }
    EditControl editControl = CantPerformException.ensureNotNull(getSyncManager().prepareEdit(toLock));
    LockedController.start(this, editor, model, descriptor, editControl);
  }

  public void unregisterWindow(EditFeature feature, Object contextKey) {
    NoLockController removed = myNewWindows.remove(Pair.create(feature, contextKey));
    LogHelper.assertError(removed != null, "Not registered", feature, contextKey);
  }

  public SyncManager getSyncManager() {
    return myContainer.getActor(SyncManager.ROLE);
  }

  public ComponentContainer getContainer() {
    return myContainer;
  }
}
