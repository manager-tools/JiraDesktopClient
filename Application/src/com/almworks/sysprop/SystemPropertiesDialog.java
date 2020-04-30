package com.almworks.sysprop;

import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.*;
import com.almworks.util.Env;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.etable.BaseCellEditor;
import com.almworks.util.components.etable.ColumnEditor;
import com.almworks.util.components.etable.EdiTableManager;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.i18n.Local;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.model.ValueModel;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.ScrollBarPolicy;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SystemPropertiesDialog {
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(SystemPropertiesDialog.class.getClassLoader(), "com/almworks/sysprop/message");
  static final ValueModel<Boolean> SHOWING = ValueModel.create(false);

  static void showDialog(ActionContext context) throws CantPerformException {
    new SystemPropertiesDialog().doShowDialog(context);
  }

  private ASortedTable<Property> myTable;
  private OrderListModel<Property> myModel;
  private EdiTableManager myManager;
  private KeyColumn myKeyColumn;
  private ValueColumn myValueColumn;

  private SystemPropertiesDialog() {
  }

  private void doShowDialog(ActionContext context) throws CantPerformException {
    myModel = createModel();
    myTable = createTable();

    final DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createMainBuilder("SystemPropertiesDialog");

    builder.setTitle("System Properties");
    builder.setEmptyCancelAction();
    builder.setOkAction(new SaveAction());
    builder.setBottomLineComponent(createBottomLine());
    builder.setInitialFocusOwner(myTable.getSwingComponent());
    builder.setIgnoreStoredSize(true);
    builder.setContent(createContent());

    if(Aqua.isAqua()) {
      builder.setBottomBevel(false);
      builder.setBorders(false);
    }

    SHOWING.setValue(true);
    builder.showWindow(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        SHOWING.setValue(false);
      }
    });
    myTable.getSelectionAccessor().setSelectedIndex(0);
  }

  private JComponent createBottomLine() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(new AddAction().getButton(), BorderLayout.WEST);
    return panel;
  }

  private OrderListModel<Property> createModel() {
    final List<Property> list = Collections15.arrayList();
    final Map<String, String> map = Env.getProperties();
    for(final Map.Entry<String, String> e : map.entrySet()) {
      list.add(new Property(e.getKey(), e.getValue()));
    }
    return OrderListModel.create(list);
  }

  private ASortedTable<Property> createTable() {
    assert myModel != null;

    final ASortedTable<Property> table = new ASortedTable<Property>();
    table.setGridHidden();
    table.setStriped(true);

    final JTable jTable = (JTable)table.getSwingComponent();
    jTable.getTableHeader().setReorderingAllowed(false);
    addClearShortcuts(table);
    addAddShortcuts(table);

    myKeyColumn = new KeyColumn();
    myValueColumn = new ValueColumn();

    final List<TableColumnAccessor<Property, ?>> columns = Collections15.arrayList();

    columns.add(TableColumnBuilder.<Property, Property>create("propKey", "Name")
      .setEditor(myKeyColumn)
      .setCanvasRenderer(myKeyColumn)
      .setConvertor(Convertor.<Property>identity())
      .setComparator(myKeyColumn)
      .setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(15))
      .createColumn());

    columns.add(TableColumnBuilder.<Property, Property>create("propValue", "Value")
      .setEditor(myValueColumn)
      .setCanvasRenderer(myValueColumn)
      .setConvertor(Convertor.<Property>identity())
      .setComparator(myValueColumn)
      .setSizePolicy(ColumnSizePolicy.Calculated.freeLetterMWidth(25))
      .createColumn());

    table.setColumnModel(FixedListModel.create(columns));
    table.setCollectionModel(myModel);

    final Map<ColumnEditor, Integer> ecMap = Collections15.hashMap();
    ecMap.put(myKeyColumn, 0);
    ecMap.put(myValueColumn, 1);
    myManager = new EdiTableManager(jTable, ecMap, myValueColumn, false);

    return table;
  }

  private void addClearShortcuts(final ASortedTable<Property> table) {
    final String clearProp = "clearProp";
    final JComponent jTable = table.getSwingComponent();
    final InputMap im = jTable.getInputMap(JComponent.WHEN_FOCUSED);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), clearProp);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), clearProp);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), clearProp);
    jTable.getActionMap().put(clearProp, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (final Property p : table.getSelectionAccessor().getSelectedItems()) {
          clearProperty(p);
        }
      }
    });
  }

  private void addAddShortcuts(final ASortedTable<Property> table) {
    final String addProp = "addProp";
    final JComponent jTable = table.getSwingComponent();
    final InputMap im = jTable.getInputMap(JComponent.WHEN_FOCUSED);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), addProp);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), addProp);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), addProp);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.SHIFT_DOWN_MASK), addProp);
    jTable.getActionMap().put(addProp, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addNewProperty();
      }
    });
  }

  private void clearProperty(Property p) {
    p.myVal = "";
    myModel.updateElement(p);
  }

  private void addNewProperty() {
    final Property p = new Property();
    myModel.addElement(p);
    myTable.getSelectionAccessor().setSelected(p);
    myTable.scrollSelectionToView();
    myManager.editSelectedRow(myKeyColumn);
  }

  private JComponent createContent() {
    assert myModel != null;
    assert myTable != null;

    final int prefRows = Util.bounded(10, myModel.getSize(), 20);
    myTable.adjustSize(55, prefRows, 20, 5);
    Aqua.cleanScrollPaneBorder(myTable);
    Aqua.addSouthBorder(myTable);
    ScrollBarPolicy.setDefaultWithHorizontal(myTable, ScrollBarPolicy.AS_NEEDED);

    final JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(myTable, BorderLayout.CENTER);
    Link link = new Link();
    link.setAnAction(new OpenBrowserAnAction("https://wiki.almworks.com/display/jc16/Command-line+Reference", true, I18N.getString("sysprop.window.seedoc.text")));
    link.setHorizontalAlignment(SwingConstants.LEADING);
    panel.add(link, BorderLayout.NORTH);
    return panel;
  }

  private boolean isAnythingChanged() {
    for(final Property p : myModel) {
      if(p.isChanged()) {
        return true;
      }
    }
    return false;
  }

  private void saveChanges() {
    TableCellEditor editor = myTable.getTable().getSwingCellEditor();
    if (editor != null) editor.stopCellEditing();
    final Map<String, String> diff = Collections15.hashMap();
    for(final Property p : myModel) {
      p.addToDiff(diff);
    }
    if(!diff.isEmpty()) {
      Env.changeProperties(diff);
    }
  }

  private class SaveAction extends SimpleAction {
    public SaveAction() {
      super("&Save");
      setDefaultPresentation(
        PresentationKey.SHORTCUT,
        Env.isMac()
          ? Shortcuts.ksMenu(KeyEvent.VK_S)
          : Shortcuts.ksMenu(KeyEvent.VK_ENTER));
      updateOnChange(myModel);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(isAnythingChanged());
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      saveChanges();
      final String app = Local.text(Terms.key_Deskzilla);
      final StatusBar sb = context.getSourceObject(MainWindowManager.ROLE).getStatusBar();
      final Detach[] hide = { null };
      hide[0] = sb.addComponent(StatusBar.Section.MESSAGES,
        StatusBarMessages.createWarning(sb,
          new Restarter(context.getSourceObject(ApplicationManager.ROLE), hide),
          "Restart required",
          "<html>You have changed system properties." +
          "<br>The new values will take effect after you restart " + app + "."));
    }
  }

  private class Restarter implements ActionListener {
    private final ApplicationManager myAppManager;
    private final Detach[] myCancelDetach;

    public Restarter(ApplicationManager appManager, Detach[] cancel) {
      myAppManager = appManager;
      myCancelDetach = cancel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if(askUser(e)) {
        myAppManager.requestExit();
      }
      if(myCancelDetach[0] != null) {
        myCancelDetach[0].detach();
      }
    }

    private boolean askUser(ActionEvent e) {
      final String app = Local.text(Terms.key_Deskzilla);
      final String quit = Env.isMac() ? "Quit " : "Exit ";

      final String msg =
        "<html>You have changed system properties." +
          "<br>The new values will take effect after you restart " + app + "." +
          "<br>" + quit + app + " now?";

      final int result = DialogsUtil.askUser(
        (Component)e.getSource(), msg, "Restart Required",
        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      return result == JOptionPane.YES_OPTION;
    }
  }

  private class AddAction extends SimpleAction {
    public AddAction() {
      super("&Add Property");
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(true);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      addNewProperty();
    }

    public JComponent getButton() {
      return new AActionButton(this);
    }
  }

  private static class Property {
    private final String myStartKey;
    private final String myStartVal;
    private String myKey;
    private String myVal;

    Property(String key, String value) {
      myKey = myStartKey = key;
      myVal = myStartVal = value;
    }

    Property() {
      this("", "");
    }

    boolean isChanged() {
      return isValueChanged() || isKeyChanged();
    }

    boolean isValueChanged() {
      return !myVal.equals(myStartVal);
    }

    boolean isKeyChanged() {
      return !myKey.equals(myStartKey);
    }

    void addToDiff(Map<String, String> diff) {
      if(isKeyChanged()) {
        if(!myStartKey.isEmpty()) {
          diff.put(myStartKey, null);
        }
        if(!myKey.isEmpty()) {
          diff.put(myKey, myVal.isEmpty() ? null : myVal);
        }
      } else if(isValueChanged()) {
        diff.put(myKey, myVal.isEmpty() ? null : myVal);
      }
    }
  }

  private abstract class BaseColumn extends BaseCellEditor<JTextField, Property>
    implements CanvasRenderer<Property>, Comparator<Property>
  {
    protected BaseColumn() {
      super(new JTextField());
    }

    @Override
    public void renderStateOn(CellState state, Canvas canvas, Property item) {
      if(item.isChanged()) {
        canvas.setFontStyle(Font.BOLD);
      }
      canvas.appendText(extractValue(item));
    }

    @Override
    protected void doSetValue(JTextField field, Property item) {
      field.setText(extractValue(item));
    }

    protected abstract String extractValue(Property item);

    @Override
    protected void doSaveEdit(JTextField field, Property item) {
      commitValue(item, field.getText());
      myModel.updateElement(item);
    }

    protected abstract void commitValue(Property item, String value);

    @Override
    public int compare(Property o1, Property o2) {
      return String.CASE_INSENSITIVE_ORDER.compare(extractValue(o1), extractValue(o2));
    }

    @Override
    protected Direction getTabDirection() {
      return Direction.NONE;
    }

    @Override
    protected Direction getEnterDirection() {
      return Direction.RIGHT;
    }
  }

  private class KeyColumn extends BaseColumn {
    @Override
    protected String extractValue(Property item) {
      return item.myKey;
    }

    @Override
    protected void commitValue(Property item, String value) {
      item.myKey = value;
    }
  }

  private class ValueColumn extends BaseColumn {
    @Override
    protected String extractValue(Property item) {
      return item.myVal;
    }

    @Override
    protected void commitValue(Property item, String value) {
      item.myVal = value;
    }
  }
}
