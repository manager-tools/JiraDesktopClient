package com.almworks.actions.console.actionsource;

import com.almworks.actions.console.CompletionFieldController;
import com.almworks.actions.console.CompletionTextField;
import com.almworks.actions.console.PopupController;
import com.almworks.actions.console.VariantModelController;
import com.almworks.util.commons.Function;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.almworks.util.ui.widgets.util.CanvasWidget;
import com.almworks.util.ui.widgets.util.list.ColumnListWidget;
import com.almworks.util.ui.widgets.util.list.ListSelectionProcessor;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

class OperationConsoleAction extends SimpleAction {
  // Uncomment to investigate focus problems
//  static {
//    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
//    manager.addPropertyChangeListener(new PropertyChangeListener() {
//      @Override
//      public void propertyChange(PropertyChangeEvent evt) {
//        System.out.println(evt.getPropertyName() + ": " + evt.getNewValue() + " <- " + evt.getOldValue());
//      }
//    });
//  }
  public OperationConsoleAction() {
    super("Operations Console\u2026");
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final Component contextComponent = context.getComponent();
    Window owner = SwingTreeUtil.getOwningFrameDialog(SwingTreeUtil.findAncestorOfType(contextComponent, Window.class));
    CompletionTextField<ActionEntry> field = createField(collectModel(context));
    JComponent fieldDecoration = createDecoration(field);
    final CompletionFieldController<ActionEntry> controller = field.getController();
    final PopupController mainPopup = new PopupController(fieldDecoration);
    controller.setConsumeEsc(false);
    field.setAction(new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ActionEntry selected = controller.getSelected();
        if (selected == null) return;
        mainPopup.hide();
        AnAction action = selected.getAction();
        JComponent jcomponent = SwingTreeUtil.findAncestorOfType(contextComponent, JComponent.class);
        if (jcomponent != null && action != null) ActionUtil.performAction(action, jcomponent);
      }
    });
    mainPopup.setHideOnEscape(true);
    mainPopup.setListenFocus(true);
    mainPopup.showAt(owner, getFieldBounds(contextComponent, fieldDecoration, field));
  }

  private JComponent createDecoration(JComponent content) {
    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(content, BorderLayout.SOUTH);
    JLabel header = new JLabel("Actions");
    Font font = header.getFont();
    header.setFont(font.deriveFont(font.getSize() * 1.5f).deriveFont(Font.PLAIN));
    header.setHorizontalTextPosition(SwingConstants.CENTER);
    header.setHorizontalAlignment(SwingConstants.CENTER);
    panel.add(header, BorderLayout.NORTH);
    panel.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
    return panel;
  }

  private Function<Lifespan, VariantModelController<ActionEntry>> collectModel(ActionContext context) throws CantPerformException {
    List<UpdatedAction.Group> groups = context.getSourceObject(ConsoleActionsComponent.ROLE).getEnabledGroups(context.getComponent());
    return new ActionsModelFactory(ActionEntry.collectGroups(groups));
  }

  /**
   * @return size of the result is content preferred size. Bounds are located to make center of the content be located at the center of the source component
   */
  private Rectangle getFieldBounds(Component source, JComponent content, CompletionTextField<ActionEntry> field) {
    // Calculate center of source source
    Rectangle visible = SwingTreeUtil.getVisibleScreenBounds(source);
    Point location = visible.getLocation();
    Dimension size = visible.getSize();
    location.x += size.width / 2;
    location.y += size.height / 2;
    final Rectangle screen = UIUtil.getScreenUserSize(UIUtil.getGraphicsConfigurationForPoint(location));
    // Place center of the field to the center of ther source
    Dimension contentSize = content.getPreferredSize();
    location.x -= contentSize.width / 2;
    location.y -= contentSize.height / 2;
    if (!content.isDisplayable()) {
      content.setBounds(new Rectangle(new Point(0, 0), content.getPreferredSize()));
      content.doLayout();
    }
    int height = field.getY() + field.getHeight();
    height += field.getController().getMaxListHeight();
    Dimension contentWithPopup = new Dimension(contentSize);
    contentWithPopup.height = Math.max(contentWithPopup.height, height);
    Rectangle bounds = new Rectangle(location, contentWithPopup);
    UIUtil.fitScreen(screen, bounds);
    return new Rectangle(bounds.getLocation(), contentSize);
  }

  private CompletionTextField<ActionEntry> createField(Function<Lifespan, VariantModelController<ActionEntry>> modelFactory) {
    CompletionTextField<ActionEntry> field = new CompletionTextField<ActionEntry>();
    CompletionFieldController<ActionEntry> controller = field.getController();
    field.setColumns(50);
    controller.getListSelection().setSelectable(ActionEntry.IS_ACTION);
    controller.setModelFactory(modelFactory);
    controller.getPopup().setHideOnEscape(true);
    CanvasWidget<ActionEntry> widget = new CanvasWidget<ActionEntry>();
    widget.setProtoType(ActionEntry.PROTOTYPE);
    widget.setRenderer(ActionEntry.RENDERER);
    ColumnListWidget<ActionEntry> listWidget = controller.getListWidget();
    widget.setStateFactory(new ListSelectionProcessor.StateFactory(listWidget));
    listWidget.addWidget(widget);
    listWidget.setColumnPolicy(widget, 1, 0, 10);
    return field;
  }
}
