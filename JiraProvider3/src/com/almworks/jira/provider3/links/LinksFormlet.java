package com.almworks.jira.provider3.links;

import com.almworks.engine.gui.AbstractFormlet;
import com.almworks.engine.gui.Formlet;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.util.Pair;
import com.almworks.util.components.ATable;
import com.almworks.util.components.TreeModelBridge;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.components.layout.WidthDrivenComponentAdapter;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

class LinksFormlet extends AbstractFormlet {
  private final ATable<Pair<Object, TreeModelBridge<?>>> myLinks;
  private final WidthDrivenComponentAdapter myAdapter;
  private final List<ToolbarEntry> myActions = Collections15.arrayList();
  private String myLastBrief;
  private boolean myVisible;

  private LinksFormlet(ATable<Pair<Object, TreeModelBridge<?>>> links, Configuration settings) {
    super(settings);
    myLinks = links;
    myAdapter = new WidthDrivenComponentAdapter(enclose(myLinks));
  }

  public static Formlet create(GuiFeaturesManager features, Configuration settings) {
    ATable<Pair<Object, TreeModelBridge<?>>> table = new ATable<Pair<Object, TreeModelBridge<?>>>();
    LinksController controller = LinksController.install(features, table, settings.getOrCreateSubset("linksCollapse"));
    final LinksFormlet formlet = new LinksFormlet(table, settings);
    controller.update(new LinksController.Updater() {
      @SuppressWarnings({"RawUseOfParameterizedType"})
      public void update(LinksTree tree) {
        formlet.myVisible = tree.getRoot().getChildCount() > 0;
        formlet.myLastBrief = tree.getSummaryString();
        formlet.fireFormletChanged();
      }
    });
    AnAction defaultAction = new IdActionProxy(JiraActions.VIEW_LINKED_ISSUES) ;
    controller.addDefaultPopupAction(defaultAction);
    formlet.addAction(controller, new IdActionProxy(JiraActions.ADD_LINKS), true);
    formlet.addAction(controller, new IdActionProxy(JiraActions.REMOVE_LINKS), true);
    formlet.addAction(controller, new IdActionProxy(JiraActions.VIEW_ALL_LINKED_ISSUES), false);
    return formlet;
  }

  private void addAction(LinksController controller, AnAction action, boolean addToPopup) {
    if (addToPopup)
      controller.addPopupAction(action);
    myActions.add(new ActionToolbarEntry(action, myLinks.getSwingComponent(), PresentationMapping.NONAME));
  }

  public String getCaption() {
    return isCollapsed() ? myLastBrief : null;
  }

  @NotNull
  public WidthDrivenComponent getContent() {
    return myAdapter;
  }

  public boolean isVisible() {
    return myVisible;
  }

  @Nullable
  public List<? extends ToolbarEntry> getActions() {
    return isCollapsed() ? null : myActions;
  }

  /**
   * If on the Mac, put the links table into a JPanel and set a margin
   * as appropriate.
   * @param c The links table.
   * @return Either the table itself, or a JPanel containing it.
   */
  private static JComponent enclose(JComponent c) {
    if(!Aqua.isAqua()) {
      return c;
    }

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(c, BorderLayout.CENTER);
    panel.setBorder(new EmptyBorder(0, 6, 0, 0));
    return panel;
  }
}