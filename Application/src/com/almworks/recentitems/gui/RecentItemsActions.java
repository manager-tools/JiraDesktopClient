package com.almworks.recentitems.gui;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.gui.WindowController;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.Terms;
import com.almworks.util.collections.Convertor;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.dnd.TransferAction;
import org.almworks.util.Log;
import org.almworks.util.StringUtil;
import org.almworks.util.Util;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.util.List;

import static com.almworks.api.gui.MainMenu.*;

public class RecentItemsActions implements Startable {
  private final ActionRegistry myActionRegistry;

  public RecentItemsActions(ActionRegistry registry) {
    myActionRegistry = registry;
  }

  @Override
  public void start() {
    myActionRegistry.registerAction(RecentItems.SHOW_RECENT_ITEMS, new ShowAction());

    myActionRegistry.registerAction(
      RecentItems.COPY_ITEM, new ClosingProxy(new CopyAction()));
    myActionRegistry.registerAction(RecentItems.COPY_ID_SUMMARY, new ClosingProxy(Edit.COPY_ID_SUMMARY, Icons.ACTION_COPY));
    myActionRegistry.registerAction(RecentItems.CUSTOM_COPY, new ClosingProxy(Edit.CUSTOM_COPY, Icons.ACTION_COPY));
//    myActionRegistry.registerAction(
//      RecentArtifacts.PASTE_ITEM_KEY, new ClosingProxy(new PasteKeyAction()));

    myActionRegistry.registerAction(
      RecentItems.OPEN_IN_FRAME, new ClosingProxy(Search.OPEN_ITEM_IN_FRAME));
    myActionRegistry.registerAction(
      RecentItems.OPEN_IN_TAB, new ClosingProxy(Search.OPEN_ITEM_IN_TAB, Icons.DETAILS_PANEL));
    myActionRegistry.registerAction(
      RecentItems.OPEN_IN_BROWSER, new ClosingProxy(Search.OPEN_ITEM_IN_BROWSER));
    myActionRegistry.registerAction(
      RecentItems.EDIT_ITEM, new ClosingProxy(Edit.EDIT_ITEM));

    myActionRegistry.registerAction(
      RecentItems.START_WORK, new ClosingProxy(Tools.TIME_TRACKING_START_WORK_ON_ISSUE));
    myActionRegistry.registerAction(
      RecentItems.STOP_WORK, new ClosingProxy(Tools.TIME_TRACKING_STOP_WORK_ON_ISSUE));
  }

  @Override
  public void stop() {}

  private static class ShowAction extends SimpleAction {
    private ShowAction() {
      setDefaultPresentation(PresentationKey.NAME, "Show Recent " + Terms.ref_Artifacts);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(RecentItemsLoader.ROLE);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      RecentItemsDialog.showDialog(context.getComponent());
    }
  }

  public static class ClosingProxy extends AnActionProxy {
    private final Icon myIcon;

    public ClosingProxy(AnAction delegate, Icon icon) {
      super(delegate);
      myIcon = icon;
    }

    public ClosingProxy(String id, Icon icon) {
      this(new IdActionProxy(id), icon);
    }

    public ClosingProxy(String id) {
      this(id, null);
    }

    public ClosingProxy(AnAction delegate) {
      this(delegate, null);
    }

    @Override
    public void update(UpdateContext context) throws CantPerformException {
      try {
        super.update(context);
      } finally {
        if(myIcon != null) {
          context.putPresentationProperty(PresentationKey.SMALL_ICON, myIcon);
        }
      }
    }

    @Override
    public void perform(ActionContext context) throws CantPerformException {
      super.perform(context);
      WindowController.CLOSE_ACTION.perform(context);
    }
  }

  public static class CopyAction extends SimpleAction {
    public CopyAction() {
      watchRole(ItemWrapper.ITEM_WRAPPER);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      TransferAction.COPY.update(context);
      context.putPresentationProperty(PresentationKey.SMALL_ICON, Icons.ACTION_COPY_URL);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      TransferAction.COPY.perform(context);
    }
  }

  public static class PasteKeyAction extends SimpleAction {
    public PasteKeyAction() {
      super("Paste " + Local.text(Terms.key_Artifact_ID), Icons.ACTION_PASTE);
      watchRole(TextComponentTracker.TEXT_COMPONENT_TRACKER);
      watchRole(ItemWrapper.ITEM_WRAPPER);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(TimeTrackingCustomizer.ROLE);

      final TextComponentTracker tracker = context.getSourceObject(TextComponentTracker.TEXT_COMPONENT_TRACKER);
      context.updateOnChange(tracker.getModifiable());

      final List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);

      context.setEnabled(wrappers.size() > 0 && tracker.getTextComponent() != null
        && tracker.getTextComponent().getDocument() instanceof AbstractDocument);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      final TextComponentTracker tracker = context.getSourceObject(TextComponentTracker.TEXT_COMPONENT_TRACKER);
      final JTextComponent component = tracker.getTextComponent();
      if(component == null) {
        return;
      }

      final AbstractDocument document = Util.castNullable(AbstractDocument.class, component.getDocument());
      if(document == null) {
        return;
      }

      final List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
      if(wrappers.isEmpty()) {
        return;
      }

      final TimeTrackingCustomizer customizer = context.getSourceObject(TimeTrackingCustomizer.ROLE);
      final String string = StringUtil.implode(new KeyConvertor(customizer).collectList(wrappers), ", ");

      final int dot = component.getCaret().getDot();
      final int mark = component.getCaret().getMark();
      final int start = Math.min(dot, mark);
      final int end = Math.max(dot, mark);
      try {
        document.replace(start, end - start, string, null);
      } catch (BadLocationException e) {
        Log.warn(e);
      }
    }
  }

  private static class KeyConvertor extends Convertor<ItemWrapper, String> {
    private final TimeTrackingCustomizer myCustomizer;

    public KeyConvertor(TimeTrackingCustomizer customizer) {
      myCustomizer = customizer;
    }

    @Override
    public String convert(ItemWrapper value) {
      return myCustomizer.getItemKey(value);
    }
  }
}

