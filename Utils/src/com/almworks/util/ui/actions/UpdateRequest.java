package com.almworks.util.ui.actions;

import com.almworks.util.DECL;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.CollectionModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

/**
 * @author dyoma
 */
public class UpdateRequest {
  private final MyChangeListener myListener;
  private final Updatable myUpdatable;
  private final ActionContext myContext;

  public UpdateRequest(Updatable updatable, ActionContext context) {
    myUpdatable = updatable;
    myContext = context;
    myListener = new MyChangeListener(myUpdatable);
  }

  @NotNull
  public Lifespan getLifespan() {
    return myUpdatable.getLifespan();
  }

  public UpdateService getUpdateService() {
    return myUpdatable;
  }

  public Updatable getUpdatable() {
    return myUpdatable;
  }

  public void watchRole(@NotNull final TypedKey<?> role) {
    final Lifespan life = getLifespan();
    myContext.iterateDataProviders(new ElementVisitor<DataProvider>() {
      public boolean visit(DataProvider provider) {
        if (provider.hasRole(role))
          provider.addRoleListener(life, role, myListener);
        return true;
      }
    });
  }

  public ActionContext getContext() {
    return myContext;
  }

  public void updateOnChange(@NotNull Modifiable modifiable) {
    modifiable.addAWTChangeListener(getLifespan(), myListener);
  }

  public void updateOnChange(@NotNull Document document) {
    DocumentUtil.addListener(getLifespan(), document, new MyDocumentListener(myUpdatable));
  }

  public <T> void updateOnChange(@NotNull CollectionModel<T> model) {
    model.getEventSource().addAWTListener(getLifespan(), new MyCollectionModelListener<T>(myUpdatable));
  }

  public <T> void updateOnChange(@NotNull SelectionInListModel<T> model) {
    model.addAWTChangeListener(getLifespan(), myListener);
    model.addSelectionChangeListener(getLifespan(), myListener);
  }

  public <T> void updateOnChange(@NotNull ScalarModel<T> model) {
    BasicScalarModel.invokeOnChange(model, getLifespan(), ThreadGate.AWT, myListener);
  }

  public void updateOnPropertyChange(JComponent component, String propertyName) {
    UIUtil.addSwingPropertyChangeListener(getLifespan(), component, propertyName, myListener);
  }

  public ChangeListener getChangeListener() {
    return myListener;
  }

  @Nullable
  public <T> T getSourceObjectOrNull(TypedKey<? extends T> role) {
    try {
      return myContext.getSourceObject(role);
    } catch (CantPerformException e) {
      return null;
    }
  }

  @Nullable
  public <T> Collection<T> getSourceCollectionOrNull(TypedKey<? extends T> role) {
    try {
      return myContext.getSourceCollection(role);
    } catch (CantPerformException e) {
      return null;
    }
  }

  public void watchModifiableRole(TypedKey<? extends Modifiable> role) {
    watchRole(role);
    try {
      updateOnChange(myContext.getSourceObject(role));
    } catch (CantPerformException e) {
      DECL.ignoreException();
    }
  }

  private static class MyDocumentListener extends DocumentAdapter {
    private final Updatable myUpdatable;

    public MyDocumentListener(Updatable updatable) {
      myUpdatable = updatable;
    }

    protected void documentChanged(DocumentEvent e) {
      myUpdatable.requestUpdate();
    }
  }

  private static class MyCollectionModelListener<T> extends CollectionModel.Adapter<T> {
    private final Updatable myUpdatable;

    public MyCollectionModelListener(Updatable updatable) {
      myUpdatable = updatable;
    }

    protected void onChange() {
      myUpdatable.requestUpdate();
    }
  }

  private static class MyChangeListener implements ChangeListener, PropertyChangeListener {
    private final Updatable myUpdatable;

    public MyChangeListener(Updatable updatable) {
      myUpdatable = updatable;
    }

    public void onChange() {
      myUpdatable.requestUpdate();
    }

    public String toString() {
      return "L:" + myUpdatable;
    }

    public void propertyChange(PropertyChangeEvent evt) {
      myUpdatable.requestUpdate();
    }
  }
}
