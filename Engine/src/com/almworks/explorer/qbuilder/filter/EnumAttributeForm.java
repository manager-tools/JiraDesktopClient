package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.ItemKey;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.ACheckboxList;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

public class EnumAttributeForm {
  private ACheckboxList myEnum;
  private JButton myNone;
  private JButton myInvert;
  private JButton myAll;
  private JPanel myWholePanel;
  private JCheckBox myShowRelevant;
  private AListModel<ItemKey> myAllValues;
  private BasicScalarModel<AListModel<ItemKey>> myRelevantValues;
  private Boolean myShowingRelevant = null;

  public EnumAttributeForm(CanvasRenderer<ItemKey> variantsRenderer, boolean searchSubstring) {
    myEnum.setCanvasRenderer(variantsRenderer);
    ListSpeedSearch.install(myEnum).setSearchSubstring(searchSubstring);
    myAll.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myEnum.getCheckedAccessor().selectAll();
        myEnum.requestFocusInWindow();
      }
    });
    myNone.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myEnum.getCheckedAccessor().clearSelection();
        myEnum.requestFocusInWindow();
      }
    });
    myInvert.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myEnum.getCheckedAccessor().invertSelection();
      }
    });
    clearModels();
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public SelectionAccessor<ItemKey> getSelectionAccessor() {
    return myEnum.getCheckedAccessor();
  }

  public void scrollSelectionToView() {
    myEnum.scrollSelectionToView();
  }

  public void clearModels() {
    myShowRelevant.setSelected(true);
    myShowRelevant.setVisible(false);
    myAllValues = null;
    myRelevantValues = null;
    myShowingRelevant = null;
    myEnum.setCollectionModel(null);
  }

  public void setModels(final Lifespan lifespan, @NotNull AListModel<ItemKey> allValues,
    BasicScalarModel<AListModel<ItemKey>> relevantValues)
  {
    myAllValues = allValues;
    myRelevantValues = relevantValues;
    myRelevantValues.getEventSource().addAWTListener(lifespan, new ScalarModel.Adapter<AListModel<ItemKey>>() {
      public void onScalarChanged(ScalarModelEvent<AListModel<ItemKey>> event) {
        if (lifespan.isEnded())
          return;
        AListModel<ItemKey> model = event.getNewValue();
        boolean relevantDiffers = model != null && !equalModels(model, EnumAttributeForm.this.myAllValues);
        if (model == null)
          model = myAllValues;
        if (myShowRelevant.isSelected()) {
          myEnum.setCollectionModel(model, true);
          myEnum.getSelectionAccessor().ensureSelectionExists();
        }
        myShowRelevant.setVisible(relevantDiffers);
      }
    });
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        switchModel(myShowRelevant.isSelected());
      }
    };
    listener.actionPerformed(null);
    lifespan.add(UIUtil.addActionListener(myShowRelevant, listener));
    lifespan.add(myEnum.getClearModelDetach());
  }

  private boolean equalModels(AListModel<ItemKey> model1, AListModel<ItemKey> model2) {
    if (model1.getSize() != model2.getSize())
      return false;
    Set<ItemKey> set = Collections15.hashSet(model1.toList());
    set.removeAll(model2.toList());
    return set.size() == 0;
  }

  private void switchModel(boolean relevant) {
    if (myShowingRelevant == null || myShowingRelevant != relevant) {
      myShowingRelevant = relevant;
      AListModel<ItemKey> model = myShowingRelevant ? myRelevantValues.getValue() : myAllValues;
      if (model == null)
        model = myAllValues;
      myEnum.setCollectionModel(model, true);
      myEnum.getSelectionAccessor().ensureSelectionExists();
    }
  }

  public void setReadOnly() {
    myAll.setVisible(false);
    myInvert.setVisible(false);
    myNone.setVisible(false);
    myEnum.getScrollable().setEnabled(false);
  }
}