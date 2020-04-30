package com.almworks.api.engine.util;

import com.almworks.api.engine.ProgressComponentWrapper;
import com.almworks.api.engine.SyncTask;
import com.almworks.util.English;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.SizeDelegate;
import com.almworks.util.components.SizeDelegating;
import com.almworks.util.components.SizeKey;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.progress.ProgressActivity;
import com.almworks.util.progress.ProgressData;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SynchronizationProgress extends SimpleModifiable implements ProgressComponentWrapper {
  private final ProgressSource myProgress;
  private final ScalarModel<SyncTask.State> myState;
  private final DetachComposite myDetach = new DetachComposite();

  private ProgressData myShownProgressInfo;

  private Box myWholePanel;
  private ALabel myProgressLabel;
  private JProgressBar myProgressBar;
  private static final int MAX_PROGRESS = 10000;
  private ALabel myCurrentStepLabel;

  private SyncTask.State myShownState = null;

  public SynchronizationProgress(ProgressSource progress, ScalarModel<SyncTask.State> state) {
    myProgress = progress;
    myState = state;
    progress.getModifiable().addChangeListener(myDetach, this);
    myDetach.add(listen(state));
    initComponent();
    updateComponent(progress.getProgressData(), state.getValue());
  }

  protected <T> Detach listen(ScalarModel<T> model) {
    return model.getEventSource().addAWTListener(new ScalarModel.Adapter<T>() {
      public void onScalarChanged(ScalarModelEvent<T> event) {
        fireChanged();
      }
    });
  }
  
  private void initComponent() {
    myCurrentStepLabel = new ALabel();
    myCurrentStepLabel.setAlignmentX(0);
    myCurrentStepLabel.setHorizontalAlignment(JLabel.LEFT);

    myProgressBar = new JProgressBar(0, MAX_PROGRESS);

    myProgressLabel = new ALabel("Synchronizing\u2026");
    UIUtil.adjustFont(myProgressLabel, 1.5F, -1, true);
    myProgressLabel.setBorder(new EmptyBorder(0, 0, 0, 9));

    // We have to keep this label width almost constant, otherwise progress bar will
    // inconveniently resize to fill center of borderlayout
    myProgressLabel.setSizeDelegate(new SizeDelegate.SingleFunction() {
      public Dimension getSize(JComponent component, Dimension componentSize) {
        Dimension byUI = ((SizeDelegating) component).getSuperSize(SizeKey.PREFERRED);
        Dimension maxSize = (Dimension) component.getClientProperty(this);
        if (maxSize == null) {
          maxSize = byUI;
          component.putClientProperty(this, maxSize);
        } else if (maxSize.width < byUI.width || maxSize.height < byUI.height) {
          maxSize = new Dimension(Math.max(maxSize.width, byUI.width), Math.max(maxSize.height, byUI.height));
          component.putClientProperty(this, maxSize);
        }
        return maxSize;
      }
    });

    Box progressPanel = new Box(BoxLayout.LINE_AXIS);
    progressPanel.add(myProgressLabel);
    Box progressBarWithSpring = Box.createVerticalBox();
    progressBarWithSpring.add(myProgressBar);
    progressBarWithSpring.add(Box.createHorizontalGlue());
    progressPanel.add(progressBarWithSpring);

    Box details = Box.createHorizontalBox();
    details.add(myCurrentStepLabel);
    details.add(Box.createHorizontalGlue());

    myWholePanel = new Box(BoxLayout.Y_AXIS) {
      // This method gets called by SyncForm.TaskRenderer.update()
      @Override
      public void setForeground(Color fg) {
        myProgressLabel.setForeground(fg);
      }
    };
    myWholePanel.add(progressPanel);
    myWholePanel.add(details);
    myWholePanel.add(Box.createVerticalGlue());
  }

  public void dispose() {
    myDetach.detach();
    super.dispose();
  }

  public JComponent getComponent() {
    Threads.assertAWTThread();
    ProgressData info = myProgress.getProgressData();
    SyncTask.State state = myState.getValue();
    if (info != myShownProgressInfo || state != myShownState) {
      updateComponent(info, state);
      myShownProgressInfo = info;
      myShownState = state;
    }
    return myWholePanel;
  }

  private void updateComponent(ProgressData info, SyncTask.State state) {
    if (state == null)
      state = SyncTask.State.NEVER_HAPPENED;
    setProgressLabel(state);
    if (info == null || info.isDone() || info.getProgress() == 0F) {
      setProgress(-1);
      myCurrentStepLabel.setText("");
    } else {
      setProgress((int)(info.getProgress() * MAX_PROGRESS));
      ProgressActivity activity = info.getActivity();
      myCurrentStepLabel.setText(activity == null ? "Working" : activity.toString());
    }
  }

  private void setProgressLabel(SyncTask.State state) {
    myProgressLabel.setText(English.humanizeEnumerable(state));
  }

  private void setProgress(int progress) {
    if (progress < 0) {
      myProgressBar.setVisible(false);
    } else {
      myProgressBar.setVisible(true);
      myProgressBar.setValue(progress);
    }
  }
}
