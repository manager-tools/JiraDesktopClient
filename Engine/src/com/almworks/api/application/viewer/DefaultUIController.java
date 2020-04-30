package com.almworks.api.application.viewer;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.spi.provider.AbstractConnection;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.recent.CBRecentSynchronizer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
public abstract class DefaultUIController<C extends JComponent> implements UIController<C> {
  private static final ComponentProperty<ModelKey<?>> ATTACHED_MODEL_KEY = ComponentProperty.createProperty("AMK");
  public static final ComponentProperty<String> RECENT_CONFIG = ComponentProperty.createProperty("recentConfig");

  private static <C extends JComponent> UIController<? super C> chooseDefault(C c) {
    if (c instanceof JTextComponent) {
      if (c instanceof JTextPane)
        return (UIController) STYLED_TEXT;
      else
        return (UIController) TEXT;
    }
    else if (c instanceof AList)
      return (UIController) LIST;
    else if (c instanceof AComboBox)
      return (UIController) COMBO_BOX;

    return c.getComponentCount() > 0 ? ROOT : NUMB;
  }

  public void connectUI(Lifespan lifespan, ModelMap model, C component) {
    ModelKey key = getKey(component, model);
    if (key != null) {
      connectUI(lifespan, model, component, key);
    }
  }

  @Nullable
  protected ModelKey getKey(C component, ModelMap model) {
    ModelKey<?> amk = ATTACHED_MODEL_KEY.getClientValue(component);
    if (amk != null) {
      return amk;
    }
    String name = component.getName();
    if (name == null)
      return null;
    ModelKey key = model.getMetaInfo().findKey(name);
    if (key == null) {
      assert false : name;
      return null;
    }
    return key;
  }

  public static void connectChildren(JComponent component, Lifespan lifespan, ModelMap model) {
    for (int i = 0; i < component.getComponentCount(); i++) {
      Component child = component.getComponent(i);
      if (!(child instanceof JComponent))
        continue;
      connectComponent(lifespan, model, (JComponent) child);
    }
  }

  protected abstract void connectUI(Lifespan lifespan, ModelMap model, C component, ModelKey key);

  public static final UIController<JTextComponent> TEXT = new TextDefaultUIController();

  public static final UIController<JTextPane> STYLED_TEXT = new StyledDefaultUIController();

  public static final UIController<AList> LIST = new ListDefaultUIController();

  public static final UIController<AComboBox> COMBO_BOX = new ComboBoxDefaultUIController();

  public static final Root ROOT = new Root();

  public static void connectComponent(Lifespan lifespan, ModelMap model, JComponent component) {
    UIController controller = CONTROLLER.getClientValue(component);
    if (controller == null)
      controller = chooseDefault(component);    
    controller.connectUI(lifespan, model, component);
  }

  public static CollectionRenderer<ItemKey> createItemKeyRenderer() {
    return Renderers.createRenderer(ITEM_KEY_RENDERER);
  }

  public static final ItemKeyRenderer ITEM_KEY_RENDERER = new ItemKeyRenderer();

  // catching a bug: type param removed
  private static class ItemKeyRenderer implements CanvasRenderer {

    public void renderStateOn(CellState state, Canvas canvas, Object item) {
      if (item != null) {
        if (item instanceof ItemKey) {
          ((ItemKey) item).renderOn(canvas, state);
        } else {
          String stringValue = String.valueOf(item);
          Log.warn("invalid value for IKR (" + item.getClass() + ": " + stringValue + ")");
          canvas.appendText(stringValue);
        }
      }
    }
  }

//  private static class ItemKeyRenderer implements CanvasRenderer<ItemKey> {
//    private static final ItemKeyRenderer INSTANCE = new ItemKeyRenderer();
//
//    public void renderStateOn(CellState state, Canvas canvas, ItemKey item) {
//      if (item != null) {
//        item.renderOn(canvas, state);
//      }
//    }
//  }
//
  private static class TextDefaultUIController extends DefaultUIController<JTextComponent> {
    protected void connectUI(Lifespan lifespan, ModelMap model, final JTextComponent component, ModelKey key) {
      Document swingModel = (Document) key.getModel(lifespan, model, Document.class);
      component.setDocument(swingModel);
      lifespan.add(new Detach() {
        protected void doDetach() {
          component.setDocument(new PlainDocument());
        }
      });
    }
  }

  private static class StyledDefaultUIController extends DefaultUIController<JTextPane> {
    protected void connectUI(Lifespan lifespan, ModelMap model, final JTextPane component, ModelKey key) {
      StyledDocument swingModel = (StyledDocument) key.getModel(lifespan, model, StyledDocument.class);


      component.setDocument(swingModel);
      lifespan.add(new Detach() {
        protected void doDetach() {

          component.setDocument(new DefaultStyledDocument());
        }
      });
    }
  }

  private static class ListDefaultUIController extends DefaultUIController<AList> {
    protected void connectUI(Lifespan lifespan, ModelMap model, final AList component, ModelKey key) {
      AListModel swingModel = (AListModel) key.getModel(lifespan, model, AListModel.class);
      component.setCollectionModel(swingModel);
      lifespan.add(new Detach() {
        protected void doDetach() {
          component.setCollectionModel(AListModel.EMPTY);
        }
      });
    }
  }

  private static class ComboBoxDefaultUIController extends DefaultUIController<AComboBox> {

    protected void connectUI(Lifespan lifespan, ModelMap model, AComboBox component, ModelKey key) {
      final AComboboxModel cbModel = (AComboboxModel) key.getModel(lifespan, model, AComboboxModel.class);
      String path = RECENT_CONFIG.getClientValue(component);
      if (path != null && path.length() > 0) {
        LoadedItemServices lis = LoadedItemServices.VALUE_KEY.getValue(model);
        if (lis != null) {
          AbstractConnection connection = lis.getConnection(AbstractConnection.class);
          if (connection != null) {
            Configuration config = connection.getConnectionConfig(AbstractConnection.RECENTS, path);
            CBRecentSynchronizer.setupComboBox(lifespan, component, cbModel, config, ItemKey.GET_ID, ITEM_KEY_RENDERER);
            return;
          }
        }
      }
      component.setCanvasRenderer(ITEM_KEY_RENDERER);
      component.setModel(cbModel);
    }
  }


  public static class Root implements UIController<JComponent> {
    public void connectUI(@NotNull Lifespan lifespan, @NotNull ModelMap model, @NotNull JComponent component) {
      connectChildren(component, lifespan, model);
    }
  }
}
