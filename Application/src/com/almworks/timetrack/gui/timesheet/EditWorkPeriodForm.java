package com.almworks.timetrack.gui.timesheet;

import com.almworks.api.application.LoadedItem;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.English;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.exec.Context;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Date;
import java.util.List;

public class EditWorkPeriodForm {
  private JPanel myWholePanel;
  private JTextArea myComments;
  private ADateField myFinished;
  private ADateField myStarted;
  private AComboBox<LoadedItem> myIssue;
  private JLabel myIssueLabel;
  private JLabel myCurrentLabel;

  private final ValueModel<Date> myStartedModel = ValueModel.create();
  private final ValueModel<Date> myFinishedModel = ValueModel.create();
  private final SelectionInListModel<LoadedItem> myIssueModel = SelectionInListModel.create();
  private List<WorkPeriod> myOtherWork;

  public EditWorkPeriodForm() {
    myStarted.setDateModel(myStartedModel);
    myFinished.setDateModel(myFinishedModel);
    myIssue.setCanvasRenderer(new CanvasRenderer<LoadedItem>() {
      final TimeTrackingCustomizer myCustomizer = Context.require(TimeTrackingCustomizer.ROLE);

      public void renderStateOn(CellState state, Canvas canvas, LoadedItem item) {
        if (item == null) {
          canvas.setFontStyle(Font.ITALIC);
          canvas.appendText("please select " + English.a(Local.parse(Terms.ref_artifact)));
          return;
        }

        final Pair<String, String> kns = myCustomizer.getItemKeyAndSummary(item);
        final String key = kns.getFirst();
        final String summary = kns.getSecond();

        if (key.length() > 0)
          canvas.appendText(key);
        if (key.length() > 0 && summary.length() > 0)
          canvas.appendText(" ");
        if (summary.length() > 0)
          canvas.appendText(summary);
      }
    });
    myIssue.setModel(myIssueModel);
    myIssueLabel.setText(Local.parse(Terms.ref_Artifact + ":"));
    myCurrentLabel.setForeground(GlobalColors.ERROR_COLOR);
    myCurrentLabel.setBorder(new EmptyBorder(5, 5, 10, 5));
    myCurrentLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    myCurrentLabel.setVisible(false);
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
  }

  private void createUIComponents() {
    myFinished = new ADateField(ADateField.Precision.DATE_TIME);
    myStarted = new ADateField(ADateField.Precision.DATE_TIME);
  }

  public void setArtifacts(AListModel<LoadedItem> model, LoadedItem selected) {
    myIssueModel.setData(Lifespan.FOREVER, model);
    myIssueModel.setSelectedItem(selected == null && model.getSize() > 0 ? model.getAt(0) : selected);
  }

  public void setDates(long from, long to) {
    long now = System.currentTimeMillis();
    myStartedModel.setValue(new Date(from == 0 ? now : from));
    myFinishedModel.setValue(new Date(to == 0 ? now : to));
  }

  public void setOtherWork(List<WorkPeriod> work) {
    myOtherWork = work;
  }

  public LoadedItem getSelectedArtifact() {
    return myIssueModel.getSelectedItem();
  }

  public long getFrom() {
    Date value = myStartedModel.getValue();
    return value == null ? 0 : value.getTime();
  }

  public long getTo() {
    Date value = myFinishedModel.getValue();
    return value == null ? 0 : value.getTime();
  }

  public String getComments() {
    return myComments.getText();
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void setComments(String comments) {
    myComments.setText(Util.NN(comments));
  }

  public void setCurrent(boolean current) {
    myFinished.setEnabled(!current);
    myIssue.setEnabled(!current);
    myCurrentLabel.setVisible(current);
  }
}
