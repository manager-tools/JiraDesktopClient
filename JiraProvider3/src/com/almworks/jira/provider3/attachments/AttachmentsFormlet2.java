package com.almworks.jira.provider3.attachments;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.DefaultUIController;
import com.almworks.api.application.viewer.UIController;
import com.almworks.api.config.MiscConfig;
import com.almworks.api.download.DownloadManager;
import com.almworks.engine.gui.AbstractFormlet;
import com.almworks.engine.gui.attachments.*;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.ToolbarEntry;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class AttachmentsFormlet2<T extends Attachment> extends AbstractFormlet implements Highlightable {
  private final ModelKey<? extends Collection<T>> myKey;
  private final AttachmentsPanel<T> myView;

  private List<ToolbarEntry> myToolbarActions;

  private Comparator<? super T> myOrder;

  private int myLastAttachmentCount;
  private boolean myVisible;

  private boolean myInitialized;

  public AttachmentsFormlet2(Configuration configuration, ModelKey<? extends Collection<T>> key, MiscConfig globalConfig, DataRole<T> attachmentDataRole) {
    super(configuration);
    myKey = key;
    MenuBuilder popupMenu = AttachmentUtils.createAttachmentPopupMenu(JiraActions.DELETE_ATTACHMENT, JiraActions.RENAME_ATTACHMENT/*, JiraActions.EDIT_IMAGE_ATTACHMENT*/);
    myView = new AttachmentsPanel<T>(popupMenu, attachmentDataRole, globalConfig.getConfig("attachmentsPanel"), null);
  }

  public AttachmentsFormlet2<T> setOrder(Comparator<? super T> order) {
    if (myInitialized) {
      assert false;
      return this;
    }
    myOrder = order;
    return this;
  }

  public AttachmentsFormlet2<T> addProperties(AttachmentProperty<? super T, ?>... properties) {
    if (myInitialized) {
      assert false;
      return this;
    }
    for (AttachmentProperty<? super T, ?> property : properties) {
      myView.addProperty(property);
    }
    return this;
  }

  public AttachmentsFormlet2<T> setLabelProperty(AttachmentProperty<? super T, ?> property) {
    if (myInitialized) {
      assert false;
      return this;
    }
    myView.setLabelProperty(property);
    return this;
  }

  public AttachmentsFormlet2<T> setTooltipProvider(AttachmentTooltipProvider<? super T> provider) {
    if (myInitialized) {
      assert false;
      return this;
    }
    myView.setTooltipProvider(provider);
    return this;
  }

  private void initialize() {
    myView.initialize();
    UIController.CONTROLLER.putClientValue(myView.getComponent(), new MyPanelController());
    myToolbarActions = Collections15.arrayList(myView.createToolbar());
  }

  private void updateFormlet(Collection<T> attachments) {
    ensureInitialized();
    myVisible = attachments != null && attachments.size() > 0;
    myLastAttachmentCount = myVisible ? attachments.size() : 0;
    fireFormletChanged();
  }

  public String getCaption() {
    ensureInitialized();
    return isCollapsed() ? String.valueOf(myLastAttachmentCount) : null;
  }

  public List<? extends ToolbarEntry> getActions() {
    ensureInitialized();
    return isCollapsed() ? null : myToolbarActions;
  }

  @NotNull
  public WidthDrivenComponent getContent() {
    ensureInitialized();
    return myView;
  }

  public boolean isVisible() {
    return myVisible;
  }

  private void ensureInitialized() {
    if (myInitialized)
      return;
    initialize();
    myInitialized = true;
  }

  public void setHighlightPattern(Pattern pattern) {
    myView.setHighlightPattern(pattern);
  }

  private class MyPanelController implements UIController {
    public void connectUI(@NotNull Lifespan lifespan, @NotNull final ModelMap modelMap, @NotNull JComponent component) {
      ChangeListener listener = new ChangeListener() {
        public void onChange() {
          updateFormlet(myKey.getValue(modelMap));
        }
      };
      listener.onChange();
      modelMap.addAWTChangeListener(lifespan, listener);
      LoadedItemServices itemServices = LoadedItemServices.VALUE_KEY.getValue(modelMap);
      if (itemServices != null) {
        DownloadManager downloadManager = itemServices.getActor(DownloadManager.ROLE);
        if (downloadManager != null) {
          AListModel<T> model = getModel(lifespan, modelMap);
          if (myOrder != null) {
            model = SortedListDecorator.create(lifespan, model, myOrder);
          }
          lifespan.add(myView.show(model, downloadManager));
          DefaultUIController.ROOT.connectUI(lifespan, modelMap, component);
        }
      }
    }

    private AListModel<T> getModel(Lifespan life, final ModelMap modelMap) {
      final OrderListModel<T> model = OrderListModel.create();
      ChangeListener listener = new ChangeListener() {
        public void onChange() {
          Collection<T> elements = myKey.getValue(modelMap);
          if (elements == null) elements = Collections15.emptyCollection();
          model.replaceElementsSet(elements);
        }
      };
      modelMap.addAWTChangeListener(life, listener);
      listener.onChange();
      return model;
    }
  }
}
