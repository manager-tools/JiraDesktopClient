package com.almworks.jira.provider3.gui.timetrack.edit;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEnumEditor;
import com.almworks.items.gui.edit.editors.scalar.SliderController;
import com.almworks.items.gui.edit.editors.scalar.TimeSpentEditor;
import com.almworks.items.gui.edit.editors.text.ScalarFieldEditor;
import com.almworks.jira.provider3.gui.edit.editors.VisibilityEditor;
import com.almworks.jira.provider3.gui.timetrack.RemainEstimateEditor;
import com.almworks.jira.provider3.schema.Group;
import com.almworks.jira.provider3.schema.ProjectRole;
import com.almworks.jira.provider3.schema.Worklog;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.speedsearch.TextSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Const;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

class WorklogForm {
  static final TimeSpentEditor TIME_SPENT = new TimeSpentEditor(NameMnemonic.parseString("&Time Spent"), Worklog.TIME_SECONDS, EditWorklogsFeature.DEFAULT_UNIT);
  static final StartTimeEditor START = new StartTimeEditor(NameMnemonic.parseString("&Started"), Worklog.STARTED, TIME_SPENT, Const.SECOND);
  static final ScalarFieldEditor<String> COMMENT = ScalarFieldEditor.textPane(NameMnemonic.parseString("Co&mment"), Worklog.COMMENT);
  static final AdjustmentEditor ADJUSTMENT = AdjustmentEditor.INSTANCE;
  static final DropdownEnumEditor VISIBILITY = VisibilityEditor.create(Worklog.SECURITY);

  private static final String SELECTED_SECURITY_GROUP_SETTING = "selectedSecurityGroup";
  private static final String LAST_COMMENT = "lastWorklog";
  private static final String LAST_TIMESPENT = "hoursSpent";
  private static final String DEFAULT_TIME_SPENT = "1h";

  private JPanel myWholePanel;
  private JTextField mySpentField;
  private JSlider mySliderSlider;
  private JRadioButton myAutoAdjust;
  private JRadioButton myDontAdjust;
  private JRadioButton mySetRemain;
  private JTextField myRemain;
  private JTextArea myWorkLog;
  private ADateField myStartTime;
  private AComboBox<ItemKey> myViewableBy;
  private JLabel myWorkDescriptionLabel;
  private JLabel myTimeSpentLabel;
  private JLabel myStartedLabel;
  private ButtonGroup myAdjustGroup;

  public WorklogForm(final Configuration config) {
    myWholePanel.setBorder(UIUtil.BORDER_5);
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
    SliderController.setupSlider(mySliderSlider);
    TextSpeedSearch.installCtrlF(myWorkLog);
  }

  public JComponent getComponent() {
    return myWholePanel;
  }


  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        LAFUtil.initializeLookAndFeel();
        WorklogForm form = new WorklogForm(Configuration.EMPTY_CONFIGURATION);
        DebugFrame.show(form.myWholePanel);
      }
    });
  }

  private void createUIComponents() {
    // windows slider hack :/
    mySliderSlider = new JSlider() {
      @Override
      public Dimension getPreferredSize() {
        Dimension ps = super.getPreferredSize();
        return new Dimension(ps.width, ps.height + 18);
      }
    };

    myStartTime = new ADateField(ADateField.Precision.DATE_TIME);
  }

  public void attachComment(Lifespan life, EditItemModel model, ScalarFieldEditor<String> editor,
    @Nullable Configuration config) {
    myWorkDescriptionLabel.setEnabled(true);
    myWorkLog.setEnabled(true);
    if (config != null)
      editor.syncValue(life, model, config, WorklogForm.LAST_COMMENT, "");
    editor.attachComponent(life, model, myWorkLog);
  }

  public void attachVisibility(Lifespan life, EditItemModel model, DropdownEnumEditor editor, @Nullable Configuration config) {
    myViewableBy.setEnabled(true);
    if (config != null)
      editor.syncValue(life, model, config, WorklogForm.SELECTED_SECURITY_GROUP_SETTING,  null, ProjectRole.ENUM_TYPE, Group.ENUM_TYPE);
    editor.attachCombo(life, model, myViewableBy);
  }

  public void attachStart(Lifespan life, EditItemModel model, StartTimeEditor editor) {
    myStartedLabel.setEnabled(true);
    myStartTime.setEnabled(true);
    editor.attachField(life, model, myStartTime);
  }

  public void attachTimeSpent(Lifespan life, EditItemModel model, TimeSpentEditor editor, @Nullable Configuration config) {
    myTimeSpentLabel.setEnabled(true);
    mySpentField.setEnabled(true);
    mySliderSlider.setEnabled(true);
    if (config != null)
      editor.syncValue(life, model, config, WorklogForm.LAST_TIMESPENT, WorklogForm.DEFAULT_TIME_SPENT);
    editor.attach(life, model, mySpentField, mySliderSlider);
  }

  public void attachAdjustment(Lifespan life, EditItemModel model, RemainEstimateEditor estimate,
    RemainingAdjustmentEditor adjustment)
  {
    estimate.attachComponent(life, model, myRemain);
    adjustment.attach(life, model, myRemain, myAdjustGroup, myAutoAdjust, myDontAdjust, mySetRemain);
  }
}
