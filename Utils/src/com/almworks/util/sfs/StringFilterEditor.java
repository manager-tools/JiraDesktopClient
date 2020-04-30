package com.almworks.util.sfs;

import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.RadioButtonGroup;
import com.almworks.util.components.plaf.macosx.combobox.MacPrettyComboBox;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.threads.BottleneckJobs;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.swing.DocumentUtil;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Arrays;
import java.util.regex.Pattern;

import static com.almworks.util.sfs.StringFilter.MatchType;

public class StringFilterEditor {
  private static final String LAST_TYPE = "lastType";
  private static final String LAST_CASE_SENSITIVE = "lastCS";
  private static final int HISTORY_LENGTH = 10;
  private static final String LAST_FILTERS = "lastFilter";

  private final JPanel myPanel = new JPanel();
  private final JComboBox myFilter = new MacPrettyComboBox();
  private final JCheckBox myCaseSensitive = new JCheckBox();
  private final RadioButtonGroup<MatchType> myRadios = new RadioButtonGroup();
  private final JLabel myErrorLabel = new JLabel();

  private static final BottleneckJobs<StringFilterEditor> ERROR_CHECKER =
    new BottleneckJobs<StringFilterEditor>(250, ThreadGate.AWT) {
      protected void execute(StringFilterEditor job) {
        job.checkError();
      }
    };


  public StringFilterEditor() {
    initCombo();
    initRadios();
    initCheckbox();
    initErrorLabel();
    initForm();
    listenForError();
  }

  private void initCheckbox() {
    NameMnemonic.parseString("&Case sensitive").setToButton(myCaseSensitive);
  }

  private void listenForError() {
    myRadios.getModel().addSelectionChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        updateError();
      }
    });
    ComboBoxEditor editor = myFilter.getEditor();
    if (editor != null) {
      Component c = editor.getEditorComponent();
      if (c instanceof JTextComponent) {
        JTextComponent textComponent = (JTextComponent) c;
        DocumentUtil.addListener(Lifespan.FOREVER, textComponent.getDocument(), new DocumentAdapter() {
          protected void documentChanged(DocumentEvent e) {
            updateError();
          }
        });
      }
    }
    clearError();
  }

  private void updateError() {
    clearError();
    ERROR_CHECKER.addJobDelayed(this);
  }

  private void clearError() {
    myErrorLabel.setText(" ");
  }

  private void checkError() {
    if (myRadios.getModel().getSelectedItem() == MatchType.REGEXP) {
      String text = getFilterText();
      if (text.length() > 0) {
        try {
          Pattern.compile(text);
        } catch (Exception e) {
          myErrorLabel.setText("Bad regexp: " + e.getMessage());
          return;
        }
      }
    }
    clearError();
  }

  private void initErrorLabel() {
    myErrorLabel.setForeground(GlobalColors.ERROR_COLOR);
  }

  private void initRadios() {
    SelectionInListModel<MatchType> model = SelectionInListModel.create(Arrays.asList(MatchType.CONTAINS, MatchType.STARTS_WITH,
      MatchType.ENDS_WITH, MatchType.REGEXP, MatchType.EXACT), MatchType.CONTAINS);
    model.setSelectedItem(MatchType.EXACT);
    myRadios.setModel(model);
    myRadios.setRenderer(new CanvasRenderer<MatchType>() {
      public void renderStateOn(CellState state, com.almworks.util.components.Canvas canvas, MatchType item) {
        canvas.appendText(item.getLongDescriptionWithMnemonic());
      }
    });
  }

  private void initCombo() {
    myFilter.setEditable(true);
    myFilter.setSelectedItem("");
    myFilter.setMaximumRowCount(HISTORY_LENGTH);
  }

  private void initForm() {
    FormLayout layout = new FormLayout("d, 4dlu, d:g", "d, 4dlu, d, 4dlu, d, 4dlu, d");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout, myPanel);
    builder.append("&Filter:", myFilter);
    builder.nextLine(2);
    builder.nextColumn(2);
    builder.append(myRadios.getPanel());
    builder.nextLine(2);
    builder.append(myCaseSensitive, 3);
    builder.nextLine(2);
    builder.append(myErrorLabel, 3);
  }

  public StringFilter showNewFilter(Function<JPanel, Boolean> dialogShower, Configuration config, Component owner) {
    prepare(config);
    Boolean r = dialogShower.invoke(myPanel);
    if (r != null && r) {
      String filterText = getFilterText();
      if (filterText != null && filterText.length() > 0) {
        storeHistory(config, filterText);
        return createFilter(filterText);
      }
    }
    return null;
  }

  private void storeHistory(Configuration config, String filterText) {
    MatchType item = myRadios.getModel().getSelectedItem();
    if (item != null && item != MatchType.INVALID)
      config.setSetting(LAST_TYPE, item.getId());
    config.setSetting(LAST_CASE_SENSITIVE, myCaseSensitive.isSelected());

    java.util.List<String> lastFilters = Collections15.arrayList(config.getAllSettings(LAST_FILTERS));
    if (!lastFilters.contains(filterText)) {
      lastFilters.add(0, filterText);
      while (lastFilters.size() > HISTORY_LENGTH) {
        lastFilters.remove(lastFilters.size() - 1);
      }
      config.removeSettings(LAST_FILTERS);
      config.setSettings(LAST_FILTERS, lastFilters);
    }
  }

  private void prepare(Configuration config) {
    MatchType matchType = MatchType.fromInt(config.getIntegerSetting(LAST_TYPE, -1));
    if (matchType == null || matchType == MatchType.INVALID) {
      matchType = MatchType.CONTAINS;
    }
    myRadios.getModel().setSelectedItem(matchType);
    myCaseSensitive.setSelected(config.getBooleanSetting(LAST_CASE_SENSITIVE, false));
    java.util.List<String> lastFilters = config.getAllSettings(LAST_FILTERS);
    DefaultComboBoxModel model = new DefaultComboBoxModel(lastFilters.toArray());
    myFilter.setModel(model);

    ComboBoxEditor editor = myFilter.getEditor();
    if (editor != null) {
      editor.setItem("");
    }
  }

  private StringFilter createFilter(String filterText) {
    MatchType matchType = myRadios.getModel().getSelectedItem();
    if (matchType == null || matchType == MatchType.INVALID)
      return null;
    return new StringFilter(matchType, filterText, !myCaseSensitive.isSelected());
  }

  private String getFilterText() {
    ComboBoxEditor editor = myFilter.getEditor();
    String filter;
    if (editor == null) {
      filter = String.valueOf(myFilter.getSelectedItem());
    } else {
      filter = String.valueOf(editor.getItem());
    }
    return filter.trim();
  }
}
