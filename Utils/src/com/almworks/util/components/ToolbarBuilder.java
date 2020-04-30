package com.almworks.util.components;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.ui.AActionComponent;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class ToolbarBuilder {
  private final List<ToolbarEntry> myActions = Collections15.arrayList();
  private final Map<String, PresentationMapping<?>> myCommonPresentation = Collections15.hashMap();
  private JComponent myContextComponent = null;

  public ToolbarBuilder() {
  }

  public void addAction(final AnAction action) {
    addAction(action, null, null);
  }

  public void addAction(String actionId) {
    addAction(new IdActionProxy(actionId));
  }

  /**
   * Each action may ovveride common toolbar {@link #myContextComponent} and common toolbar {@link #myCommonPresentation}
   * When overriding {@link #myCommonPresentation} action specific values overrides {@link #myCommonPresentation}, but
   * not ovveriden {@link #myCommonPresentation} values are still applied.
   */
  public void addAction(@NotNull AnAction action, @Nullable JComponent context,
    @Nullable Map<String, PresentationMapping<?>> overrideDefaults)
  {
    Map<String, PresentationMapping<?>> notNullDefaults;
    notNullDefaults = overrideDefaults != null ? overrideDefaults : Collections15.<String, PresentationMapping<?>>emptyMap();
    myActions.add(new ActionReference(action, context, notNullDefaults));
  }

  public void addAction(AnAction action, JComponent context) {
    addAction(action, context, null);
  }

  public ToolbarBuilder addAllActions(List<? extends AnAction> actions) {
    for (AnAction action : actions)
      addAction(action);
    return this;
  }

  public void addActions(AnAction... actions) {
    for (AnAction action : actions)
      addAction(action);
  }

  public void addAllActions(List<AnAction> actions, JComponent context) {
    for (AnAction action : actions)
      addAction(action, context);
  }

  public void addAllActionIds(Collection<String> actionIds) {
    for (String actionId : actionIds) addAction(actionId);
  }

  public void addSeparator() {
    myActions.add(SeparatorToolbarEntry.INSTANCE);
  }

  public void addSelectionAction(final AListModel<? extends AnAction> actions, final String unselectedName,
    final boolean autoHide, @Nullable final AnAction selection)
  {
    myActions.add(new ToolbarEntry() {
      public void addToToolbar(AToolbar toolbar) {
        DropDownButton button = toolbar.addDropDownButton(myContextComponent, unselectedName);
        setupButton(button, actions, autoHide);
      }

      public void addToPanel(JPanel panel, JComponent contextComponent) {
        DropDownButton button = AToolbar.createDropDownButton(contextComponent, unselectedName);
        setupButton(button, actions, autoHide);
        panel.add(button);
      }

      private void setupButton(DropDownButton button, AListModel<? extends AnAction> actions, boolean autoHide) {
        if (selection == null) button.setActions(actions);
        else button.setActions(actions, selection);
        if (autoHide)
          button.setAutoHideShow(true);
      }
    });
  }

  public AToolbar createHorizontalToolbar() {
    return createToolbar(InlineLayout.HORISONTAL);
  }

  public AToolbar createVerticalToolbar() {
    return createToolbar(InlineLayout.VERTICAL);
  }

  public AToolbar createToolbar(InlineLayout.Orientation orientation) {
    return addAllToToolbar(new AToolbar(myContextComponent, orientation));
  }

  public JPanel createHorizontalPanel() {
    return createPanel(InlineLayout.HORISONTAL);
  }

  private JPanel createPanel(InlineLayout.Orientation orientation) {
    JPanel panel = new JPanel(new InlineLayout(orientation, 0, false));
    for (ToolbarEntry action : myActions) {
      action.addToPanel(panel, myContextComponent);
    }
    return panel;
  }

  public AToolbar createFlowToolbar() {
    return addAllToToolbar(
      new AToolbar(myContextComponent, new FlowLayoutVFixed(FlowLayout.LEADING, 0, 0), JToolBar.HORIZONTAL));
  }

  public void setContextComponent(JComponent contextComponent) {
    myContextComponent = contextComponent;
  }

  public void setCommonPresentation(Map<String, PresentationMapping<?>> presentation) {
    myCommonPresentation.clear();
    myCommonPresentation.putAll(presentation);
  }

  public void addCommonPresentation(String swingKey, PresentationMapping<?> mapping) {
    myCommonPresentation.put(swingKey, mapping);
  }

  public static ToolbarBuilder smallVisibleButtons() {
    ToolbarBuilder builder = new ToolbarBuilder();
    builder.setCommonPresentation(PresentationMapping.VISIBLE_NONAME);
    return builder;
  }

  public static ToolbarBuilder smallEnabledButtons() {
    ToolbarBuilder builder = new ToolbarBuilder();
    builder.setCommonPresentation(PresentationMapping.ENABLED_NONAME_PLUS_DESCRIPTION);
    return builder;
  }

  public static ToolbarBuilder buttonsWithText() {
    return new ToolbarBuilder();
  }

  public void addLabel(final String label) {
    myActions.add(new ToolbarEntry() {
      public void addToToolbar(AToolbar toolbar) {
        toolbar.add(new JLabel(label));
      }

      public void addToPanel(JPanel panel, JComponent contextComponent) {
        panel.add(new JLabel(label));
      }
    });
  }

  public void addComponent(final Component component) {
    myActions.add(new ToolbarEntry() {
      public void addToToolbar(AToolbar toolbar) {
        toolbar.add(component);
      }

      public void addToPanel(JPanel panel, JComponent contextComponent) {
        panel.add(component);
      }
    });
  }

  public List<ToolbarEntry> getEntries() {
    return Collections.unmodifiableList(myActions);
  }

  public void addAll(Collection<? extends ToolbarEntry> entries) {
    myActions.addAll(entries);
  }

  public void add(ToolbarEntry entry) {
    myActions.add(entry);
  }

  public AToolbar addAllToToolbar(AToolbar toolbar) {
    for(final ToolbarEntry entry : myActions) {
      entry.addToToolbar(toolbar);
    }
    return toolbar;
  }

  private class ActionReference implements ToolbarEntry {
    @NotNull
    private final AnAction myAction;
    @Nullable
    private final JComponent myOverridenContext;
    @Nullable
    private final Map<String, PresentationMapping<?>> myOverridenPresentation;

    protected ActionReference(@NotNull AnAction action, @Nullable JComponent contextComponent,
      @Nullable Map<String, PresentationMapping<?>> overridePresentation)
    {
      myAction = action;
      myOverridenContext = contextComponent;
      myOverridenPresentation = overridePresentation;
    }

    public void addToToolbar(AToolbar toolbar) {
      AActionComponent<?> button = toolbar.addAction(myAction, chooseContextComponent(myContextComponent));
      setupButton(button);
    }

    private JComponent chooseContextComponent(JComponent contextComponent) {
      return myOverridenContext != null ? myOverridenContext : contextComponent;
    }

    public void addToPanel(JPanel panel, JComponent contextComponent) {
      AToolbarButton button = AToolbar.createActionButton(myAction, chooseContextComponent(contextComponent));
      setupButton(button);
      panel.add(button);
    }

    private void setupButton(AActionComponent<?> button) {
      for (Map.Entry<String, PresentationMapping<?>> entry : myCommonPresentation.entrySet())
        button.setPresentationMapping(entry.getKey(), entry.getValue());
      Map<String, PresentationMapping<?>> overridenPresentation = myOverridenPresentation;
      if (overridenPresentation != null)
        for (Map.Entry<String, PresentationMapping<?>> entry : overridenPresentation.entrySet())
          button.setPresentationMapping(entry.getKey(), entry.getValue());
    }
  }
}
