package com.almworks.explorer;

import com.almworks.api.explorer.ApplicationToolbar;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.components.AToolbar;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIComponentWrapper2Support;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.IdActionProxy;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ApplicationToolbarImpl extends UIComponentWrapper2Support implements ApplicationToolbar {
  private static final int SECTION_GAP = 11;

  private final Map<Section, PlaceHolder> mySections = Collections15.hashMap();
  private final JPanel myWholePanel = new JPanel();
  private final DetachComposite myLife = new DetachComposite();
  private final Set<Section> myEndingSections = Collections15.hashSet();

  private ApplicationToolbarImpl() {
    createPlaceholders();
    configure();
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        fillDefaultSections();
      }
    });
  }

  private void fillDefaultSections() {
    ToolbarBuilder builder;
    builder = ToolbarBuilder.buttonsWithText();
    builder.addAction(MainMenu.Edit.NEW_ITEM);
    setSectionComponent(Section.CREATE_ARTIFACT, builder.createHorizontalToolbar());

    builder = ToolbarBuilder.smallVisibleButtons();
    builder.addAction(
      new IdActionProxy(MainMenu.File.DOWNLOAD_CHANGES_QUICK), null,
      makeMappings("", Icons.ACTION_UPDATE_ALL_ARTIFACTS));
    builder.addAction(
      new IdActionProxy(MainMenu.File.UPLOAD_ALL_CHANGES), null,
      makeMappings("", Icons.ACTION_COMMIT_ALL_ARTIFACTS));
    setSectionComponent(Section.SYNCHRONIZATION, builder.createHorizontalToolbar());

    builder = ToolbarBuilder.smallVisibleButtons();
    builder.addAction(MainMenu.Search.NEW_QUERY);
    builder.addAction(MainMenu.Search.NEW_DISTRIBUTION);
    builder.addAction(MainMenu.Search.RUN_QUERY);
    setSectionComponent(Section.NAVIGATION_NODE_ACTIONS, builder.createHorizontalToolbar());

    builder = ToolbarBuilder.smallVisibleButtons();
    builder.addAction(MainMenu.Windows.SHOW_NAVIGATION_AREA);
    builder.addAction(MainMenu.Windows.SHOW_DISTRIBUTION_TABLE);
    builder.addAction(MainMenu.Tools.TIME_TRACKING);
    builder.addAction(MainMenu.X_PUBLISH_TIME);
    setSectionComponent(Section.TOOLS, builder.createHorizontalToolbar());
  }

  private Map<String, PresentationMapping<?>> makeMappings(String name, Icon icon) {
    final Map<String, PresentationMapping<?>> map = Collections15.hashMap();
    map.put(Action.NAME, PresentationMapping.constant(name));
    map.put(Action.SMALL_ICON, PresentationMapping.constant(icon));
    map.put(Action.SHORT_DESCRIPTION, PresentationMapping.GET_NAME);
    return Collections.unmodifiableMap(map);
  }

  private void createPlaceholders() {
    for (Section section : Section.values()) {
      mySections.put(section, new PlaceHolder());
    }
  }

  private void configure() {
    myWholePanel.setLayout(new BorderLayout());

    final JPanel fixed = new JPanel(InlineLayout.horizontal(0));
    fixed.add(mySections.get(Section.CREATE_ARTIFACT));
    fixed.add(mySections.get(Section.SYNCHRONIZATION));
    fixed.add(mySections.get(Section.NAVIGATION_NODE_ACTIONS));
    fixed.add(mySections.get(Section.TOOLS));
    fixed.add(mySections.get(Section.EXTERNAL_ARTIFACT_TOOLS));
    myWholePanel.add(fixed, BorderLayout.WEST);

    myWholePanel.add(mySections.get(Section.SEARCH), BorderLayout.CENTER);
    myEndingSections.add(Section.SEARCH);

    if(Aqua.isAqua()) {
      final AToolbar t = new AToolbar();
      final Border b = t.getBorder();
      if(b != null) {
        final Insets i = b.getBorderInsets(t);
        myWholePanel.setBorder(new EmptyBorder(0, i.left, 0, i.right));
      }
    }
  }

  private void fillPlaceHolder(PlaceHolder holder, JComponent content) {
    holder.show(content);
    Container parent = holder.getParent();
    if (parent != null) {
      parent.invalidate();
      myWholePanel.revalidate();
      myWholePanel.repaint();
    }
  }

  public void setSectionComponent(Section section, JComponent component) {
    Threads.assertAWTThread();
    if (component != null && !myEndingSections.contains(section)) {
      component.setBorder(new SectionBorder(component));
    }
    PlaceHolder placeHolder = mySections.get(section);
    fillPlaceHolder(placeHolder, component);
  }

  public PlaceHolder getSectionHolder(Section section) {
    return mySections.get(section);
  }

  public Detach getDetach() {
    return myLife;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  private static class SectionBorder extends EmptyBorder {
    private final JComponent myComponent;

    public SectionBorder(JComponent c) {
      super(0, 0, 0, SECTION_GAP);
      myComponent = c;
      UIUtil.visitComponents(myComponent, JComponent.class, new Watcher());
    }

    public Insets getBorderInsets(Component c) {
      return isEmptyContainer(myComponent) ? new Insets(0, 0, 0, 0) : super.getBorderInsets(c);
    }


    public Insets getBorderInsets(Component c, Insets insets) {
      if (isEmptyContainer(myComponent)) {
        insets.left = insets.right = insets.top = insets.bottom = 0;
        return insets;
      } else {
        return super.getBorderInsets(c, insets);
      }
    }

    public Insets getBorderInsets() {
      return isEmptyContainer(myComponent) ? new Insets(0, 0, 0, 0) : super.getBorderInsets();
    }

    private boolean isEmptyContainer(Component c) {
      if (!(c instanceof Container))
        return false;
      Container container = (Container) c;
      for (int i = 0; i < container.getComponentCount(); i++) {
        Component child = container.getComponent(i);
        if (child.isVisible() && child.getWidth() > 0)
          return false;
      }
      return true;
    }

    private class Watcher implements ElementVisitor<JComponent>, ComponentListener {
      @Override
      public boolean visit(JComponent element) {
        element.addComponentListener(this);
        return true;
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        myComponent.revalidate();
      }

      @Override
      public void componentResized(ComponentEvent e) {
        myComponent.revalidate();
      }

      @Override
      public void componentMoved(ComponentEvent e) {
        myComponent.revalidate();
      }

      @Override
      public void componentShown(ComponentEvent e) {
        myComponent.revalidate();
      }
    }
  }
}
