package com.almworks.util.components;

import com.almworks.util.TODO;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.SingleChildLayout;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

import static com.almworks.util.ui.SingleChildLayout.CONTAINER;
import static com.almworks.util.ui.SingleChildLayout.PREFERRED;

/**
 * @author dyoma
 */
public class ComponentsListController<T> {
  private final JPanel myPanel = new JPanel(new GridLayout(0, 1, 0, 0));
  private final JPanel myWholePanel;
  private final Map<T, AbstractButton> myButtons = Collections15.hashMap();
  private final ListAccessor<T> myAccessor;
  private CanvasRenderer<T> myRenderer = Renderers.canvasToString("");

  public ComponentsListController(ListAccessor<T> accessor) {
    myAccessor = accessor;
    myWholePanel = SingleChildLayout.envelop(myPanel, PREFERRED, PREFERRED, CONTAINER, PREFERRED, 0F, 0F);
    myPanel.setOpaque(false);
  }

  public JPanel getPanel() {
    return myWholePanel;
  }

  @Nullable
  public AbstractButton findButton(T item) {
    return myButtons.get(item);
  }

  protected AbstractButton createButton(final T item) {
    final AbstractButton button = myAccessor.doCreateButton(item);
    if (!myWholePanel.isOpaque())
      button.setOpaque(false);
    myButtons.put(item, button);
    updateButton(button, item);
    return button;
  }

  protected void forgetButtons(List<T> items) {
    for (T item : items) {
      AbstractButton button = myButtons.remove(item);
      myAccessor.onButtonRemoved(button);
    }
  }

  protected void updateButton(int index) {
    AbstractButton button = (AbstractButton) myPanel.getComponent(index);
    T item = myAccessor.getItemAt(index);
    updateButton(button, item);
  }

  private void updateButton(AbstractButton button, T item) {
    PlainTextCanvas canvas = new PlainTextCanvas();
    myRenderer.renderStateOn(CellState.LABEL, canvas, item);
    NameMnemonic.parseString(canvas.getText()).setToButton(button);
    myAccessor.onButtonUpdated(button, item);
  }

  public void setRenderer(CanvasRenderer<T> renderer) {
    myRenderer = renderer;
    for (int i = 0; i < myPanel.getComponentCount(); i++)
      updateButton(i);
  }

  public void insertComponents(int index, int length) {
    GridLayout layout = (GridLayout) myPanel.getLayout();
    layout.setRows(layout.getRows() + length);
    for (int i = index; i < index + length; i++) {
      T item = myAccessor.getItemAt(i);
      AbstractButton button = createButton(item);
      myPanel.add(button, i);
    }
  }

  public void removeComponents(AListModel.RemovedEvent event) {
    for (int i = event.getFirstIndex(); i <= event.getLastIndex(); i++)
      myPanel.remove(i);
    forgetButtons(event.getAllRemoved());
    GridLayout layout = (GridLayout) myPanel.getLayout();
    layout.setRows(layout.getRows() - event.getLength());
  }

  public void updateComponents(AListModel.UpdateEvent event) {
    for (int i = event.getLowAffectedIndex(); i <= event.getHighAffectedIndex(); i++)
      if (event.isUpdated(i))
        updateButton(i);
  }

  public void rearrangeComponents(AListModel.AListEvent event) {
    throw TODO.notImplementedYet();
  }

  public int getComponentCount() {
    return myPanel.getComponentCount();
  }

  public AbstractButton getComponent(int index) {
    return (AbstractButton) myPanel.getComponent(index);
  }

  public void setOpaque(boolean opaque) {
    myWholePanel.setOpaque(opaque);
  }

  public interface ListAccessor<T> {
    AbstractButton doCreateButton(T item);

    void onButtonRemoved(AbstractButton button);

    T getItemAt(int index);

    void onButtonUpdated(AbstractButton button, T item);
  }
}
