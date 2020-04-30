package com.almworks.items.gui.edit.helper;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.*;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface EditDescriptor {
  public static final DataRole<DescriptionStrings> DESCRIPTION_STRINGS = DataRole.createRole(DescriptionStrings.class, "DescriptionStrings");
  /**
   * Typical action for edit window.
   * @see Impl#addCommonActions(java.util.List)
   */
  List<String> COMMON_ACTIONS = Collections15.unmodifiableListCopy(MainMenu.NewItem.COMMIT, MainMenu.NewItem.SAVE_DRAFT, MainMenu.NewItem.DISCARD);

  /**
   * Used for editor which does not lock any item. Such editor can be open several times. If the editor should have single instance (in some scope-context) provide this key.<br>
   * if this key is not null edit system does not open second window with same editor in same context - it focuses currently open window instead.
   * @return edit context to avoid multiple windows with same editor. Null means that multiple editors are allowed.
   */
  Object getContextKey();

  @Nullable
  BasicWindowBuilder<UIComponentWrapper> buildWindow(ComponentContainer container);

  void update(UpdateContext context);

  boolean isEnabled();

  DescriptionStrings getDescriptionStrings();

  void setDescriptionStrings(String title, String message, String save, String upload);

  JComponent createToolbarPanel(DefaultEditModel.Root model);

  class Impl implements EditDescriptor {
    private final WindowType myType;
    private final String myWindowId;
    private String myActionScope;
    private String myWindowTitle;
    private final Map<PresentationKey<?>, Object> myValues = Collections15.hashMap();
    /**
     * Used by {@link com.almworks.util.ui.WindowUtil#setupWindow(org.almworks.util.detach.Lifespan, java.awt.Window, com.almworks.util.config.Configuration, boolean, java.awt.Dimension, boolean, java.awt.GraphicsConfiguration, com.almworks.util.ui.WindowUtil.WindowPositioner)}
     */
    @Nullable
    private final Dimension myWindowPrefsize;

    private final List<Pair<String, Map<String, PresentationMapping<?>>>> myCommonActionIds = Collections15.arrayList();
    private final List<Pair<String, Map<String, PresentationMapping<?>>>> myRightActionIds = Collections15.arrayList();
    private Object myContextKey;
    private DescriptionStrings myDescriptionStrings;

    public Impl(WindowType type, String windowId, String windowTitle, @Nullable Dimension windowPrefsize) {
      myType = type;
      myWindowId = windowId;
      myWindowTitle = windowTitle;
      myWindowPrefsize = windowPrefsize;
    }

    public static Impl frame(String windowId, String windowTitle, @Nullable Dimension windowPrefsize) {
      return new Impl(WindowType.FRAME, windowId, appendAppName(windowTitle), windowPrefsize);
    }

    public static Impl notModal(String windowId, String windowTitle, @Nullable Dimension windowPrefsize) {
      return new Impl(WindowType.DIALOG, windowId, appendAppName(windowTitle), windowPrefsize);
    }

    private static String appendAppName(String windowTitle) {
      StringBuilder title = new StringBuilder(windowTitle);
      if (title.length() > 0) title.append(" - ");
      title.append(Local.parse(Terms.ref_Deskzilla));
      return title.toString();
    }

    /**
     * @see #addActions(java.util.List, java.util.List, java.util.Map)
     */
    public void addCommonActions(String ... ids) {
      addCommonActions(Arrays.asList(ids));
    }

    /**
     * @see #addActions(java.util.List, java.util.List, java.util.Map)
     */
    public void addCommonActions(List<String> ids) {
      addCommonActions(ids, null);
    }

    /**
     * @see #addActions(java.util.List, java.util.List, java.util.Map)
     */
    public void addCommonActions(List<String> ids, @Nullable Map<String, PresentationMapping<?>> presentation) {
      addActions(ids, myCommonActionIds, presentation);
    }

    /**
     * Adds action ids. Allows to override presentation. Use empty id for separator.
     * @param ids action ids or empty for separator
     * @param presentation override default toolbar presentation
     */
    private void addActions(List<String> ids, List<Pair<String, Map<String, PresentationMapping<?>>>> target,
      @Nullable Map<String, PresentationMapping<?>> presentation) {
      for (String id : ids)  if (id != null) target.add(Pair.create(id, presentation));
    }

    /**
     * @see #addActions(java.util.List, java.util.List, java.util.Map)
     */
    public void addRightActions(List<String> ids) {
      addActions(ids, myRightActionIds, null);
    }

    public void setActionScope(String actionScope) {
      myActionScope = actionScope;
    }

    public void setContextKey(Object contextKey) {
      myContextKey = contextKey;
    }

    public <T> void putPresentationProperty(PresentationKey<T> key, T value) {
      if (value instanceof String) {
        value = (T) Local.parse((String) value);
      }
      myValues.put(key, value);
    }

    /**
     * @param notUploadedTitle title of a corresponding NotUploadedMessage
     * @param notUploadedMessage content of a corresponding NotUploadedMessage
     * @param saveDescription description of a Save Draft button
     * @param uploadDescription description of an Upload button
     */
    @Override
    public void setDescriptionStrings(String notUploadedTitle, String notUploadedMessage, String saveDescription, String uploadDescription) {
      myDescriptionStrings = new DescriptionStrings(notUploadedTitle, notUploadedMessage, saveDescription, uploadDescription);
    }

    @Override
    public DescriptionStrings getDescriptionStrings() {
      return myDescriptionStrings;
    }

    @Override
    public boolean isEnabled() {
      EnableState enabled = Util.castNullable(EnableState.class, myValues.get(PresentationKey.ENABLE));
      return enabled == null || enabled == EnableState.ENABLED;
    }

    @SuppressWarnings( {"unchecked"})
    @Override
    public void update(UpdateContext context) {
      for (Map.Entry<PresentationKey<?>, Object> entry : myValues.entrySet()) {
        context.putPresentationProperty((PresentationKey<Object>) entry.getKey(), entry.getValue());
      }
    }

    @Override
    public Object getContextKey() {
      return myContextKey;
    }

    @Override
    @Nullable
    public BasicWindowBuilder<UIComponentWrapper> buildWindow(ComponentContainer container) {
      BasicWindowBuilder<UIComponentWrapper> builder;
      switch (myType) {
      case FRAME:
        builder = container.requireActor(WindowManager.ROLE).createFrame(myWindowId);
        break;
      case DIALOG:
        builder = createDialog(container).setModal(false);
        break;
      case MODAL:
        builder = createDialog(container).setModal(true);
        break;
      default:
        LogHelper.error("Unknown type", myType);
        return null;
      }
      builder.setActionScope(myActionScope);
      builder.setTitle(myWindowTitle);
      builder.setPreferredSize(myWindowPrefsize);
      return builder;
    }

    private DialogBuilder createDialog(ComponentContainer container) {
      DialogBuilder dialog = container.requireActor(DialogManager.ROLE).createBuilder(myWindowId);
      dialog.setBottomLineShown(false);
      dialog.setBorders(false);
      return dialog;
    }

    @Override
    public JComponent createToolbarPanel(DefaultEditModel.Root model) {
      JPanel leftToolbar = createLeftToolbar();
      JPanel rightToolbar = createRightToolbar();
      if (leftToolbar == null || rightToolbar == null) return leftToolbar != null ? leftToolbar : rightToolbar;
      JPanel headerPanel = new JPanel(new BorderLayout());
      headerPanel.setOpaque(false);
      headerPanel.add(leftToolbar, BorderLayout.CENTER);
      headerPanel.add(rightToolbar, BorderLayout.EAST);
      return headerPanel;
    }

    @Nullable
    public JPanel createRightToolbar() {
      return createToolbar(myRightActionIds);
    }

    @Nullable
    public JPanel createLeftToolbar() {
      return createToolbar(myCommonActionIds);
    }

    @Nullable
    private static JPanel createToolbar(List<Pair<String, Map<String, PresentationMapping<?>>>> actions) {
      if (actions == null || actions.isEmpty()) return null;
      ToolbarBuilder builder = ToolbarBuilder.buttonsWithText();
      for (Pair<String, Map<String, PresentationMapping<?>>> pair : actions) {
        String id = pair.getFirst();
        if (id.length() == 0) builder.addSeparator();
        else builder.addAction(new IdActionProxy(id), null, pair.getSecond());
      }
      JPanel panel = builder.createHorizontalPanel();
      panel.setOpaque(false);
      return panel;
    }

    public void setWindowTitle(String windowTitle) {
      myWindowTitle = windowTitle;
    }
  }

  public abstract class Wrapper implements EditDescriptor {
    @Override
    public Object getContextKey() {
      return getDelegate().getContextKey();
    }

    @Override
    @Nullable
    public BasicWindowBuilder<UIComponentWrapper> buildWindow(ComponentContainer container) {
      return getDelegate().buildWindow(container);
    }

    @Override
    public void update(UpdateContext context) {
      getDelegate().update(context);
    }

    @Override
    public boolean isEnabled() {
      return getDelegate().isEnabled();
    }

    @Override
    public DescriptionStrings getDescriptionStrings() {
      return getDelegate().getDescriptionStrings();
    }

    @Override
    public void setDescriptionStrings(String title, String message, String save, String upload) {
      getDelegate().setDescriptionStrings(title, message, save, upload);
    }

    @Override
    public JComponent createToolbarPanel(DefaultEditModel.Root model) {
      return getDelegate().createToolbarPanel(model);
    }

    @NotNull
    protected abstract EditDescriptor getDelegate();
  }

  public class DescriptionStrings {
    private final String myNotUploadedTitle;
    private final String myNotUploadedMessage;
    private final String mySaveDescription;
    private final String myUploadDescription;

    public DescriptionStrings(String notUploadedTitle, String notUploadedMessage, String saveDescription, String uploadDescription) {
      myNotUploadedTitle = notUploadedTitle;
      myNotUploadedMessage = notUploadedMessage;
      mySaveDescription = saveDescription;
      myUploadDescription = uploadDescription;
    }

    public String getNotUploadedTitle() {
      return myNotUploadedTitle;
    }

    public String getNotUploadedMessage() {
      return myNotUploadedMessage;
    }

    public String getSaveDescription() {
      return mySaveDescription;
    }

    public String getUploadDescription() {
      return myUploadDescription;
    }
  }

  enum WindowType {
    MODAL,
    DIALOG,
    FRAME
  }
}
