package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.ViewerFieldsManager;
import com.almworks.api.application.field.RightViewerFields;
import com.almworks.api.application.viewer.DefaultUIController;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function2;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.components.layout.WidthDrivenColumn;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.components.renderer.RendererHostComponent;
import com.almworks.util.config.Configuration;
import com.almworks.util.model.ValueModel;
import com.almworks.util.text.TextUtil;
import com.almworks.util.ui.*;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.regex.Pattern;

public abstract class CommonIssueViewer implements UIComponentWrapper2 {
  public static final ComponentProperty<ValueModel<Pattern>> PATTERN_MODEL_PROPERTY =
    ComponentProperty.createProperty("highlight.pattern.updmodel");

  private static final Border LEFT_SIDE_BORDER;
  private static final Border RIGHT_SIDE_BORDER;
  private static final Border SPLIT_PANE_BORDER;
  private static final Border TOP_PANEL_BORDER;
  private static final Border CONTENT_PANEL_BORDER;

  static {
    if (Aqua.isAqua()) {
      CONTENT_PANEL_BORDER = null;
      TOP_PANEL_BORDER = new EmptyBorder(5, 5, 5, 5);
      SPLIT_PANE_BORDER = Aqua.MAC_LIGHT_BORDER_NORTH;
      LEFT_SIDE_BORDER = new EmptyBorder(4, 8, 5, 5);
      RIGHT_SIDE_BORDER = AwtUtil.EMPTY_BORDER;
    } else if(Aero.isAero()) {
      CONTENT_PANEL_BORDER = new EmptyBorder(5, 0, 0, 0);
      TOP_PANEL_BORDER = new EmptyBorder(0, 5, 5, 5);
      SPLIT_PANE_BORDER = null;
      LEFT_SIDE_BORDER = new EmptyBorder(1, 10, 5, 5);
      RIGHT_SIDE_BORDER = new EmptyBorder(0, 5, 5, 10);
    } else {
      // No bottom inset is necessary to avoid overlapping of north (toolbar) and south (myBottomPlace) components in the border space.
      CONTENT_PANEL_BORDER = new EmptyBorder(5, 5, 0, 5);
      TOP_PANEL_BORDER = new EmptyBorder(0, 0, 5, 0);
      SPLIT_PANE_BORDER = null;
      LEFT_SIDE_BORDER = new EmptyBorder(1, 5, 5, 5);
      RIGHT_SIDE_BORDER = new EmptyBorder(0, 5, 5, 5);
    }
  }

  private final Lifecycle myLife = new Lifecycle();
  private JComponent myComponent;
  protected final Configuration myConfig;

  private final DocumentFormAugmentor myDocumentFormAugmentor = new DocumentFormAugmentor();
  private final List<Highlightable> myHighlightables = Collections15.arrayList();

  @Nullable
  private ValueModel<Pattern> myPatternModel = null;

  private final PlaceHolder myToolbarPlace = new PlaceHolder();
  private final PlaceHolder myBottomPlace = new PlaceHolder();

  private final ChangeListener myPatternListener = new ChangeListener() {
    public void onChange() {
      ValueModel<Pattern> patternModel = myPatternModel;
      if (patternModel != null) {
        final Pattern pattern = patternModel.getValue();
        for (Highlightable h : myHighlightables) {
          h.setHighlightPattern(pattern);
        }
      }
    }
  };

  protected void addHighlightable(Highlightable component) {
    myHighlightables.add(component);
  }

  protected void removeHighlightable(Highlightable component) {
    myHighlightables.remove(component);
  }

  public CommonIssueViewer(Configuration config) {
    myConfig = config;
  }

  protected abstract void addLeftSideFields(ItemTableBuilder fields);

  protected abstract void addRightSideFormlets(WidthDrivenColumn column, Configuration settings);

  protected abstract JComponent createSummaryPanel();

  protected abstract JComponent createKeyPanel();


  protected abstract ViewerFieldsManager getViewerFieldManager();

  protected DocumentFormAugmentor getDocumentFormAugmentor() {
    return myDocumentFormAugmentor;
  }

  protected JComponent createRightSide() {
    Configuration settings = myConfig.getOrCreateSubset("settings");
    WidthDrivenColumn panel = new WidthDrivenColumn();
    panel.setVericalGap(5);
    panel.setBackground(DocumentFormAugmentor.backgroundColor());
    panel.setLastFillsAll(false);
    panel.setBorder(RIGHT_SIDE_BORDER);
    addRightSideFormlets(panel, settings);
    return panel;
  }


  private JComponent init() {
    myLife.cycle();

    final RewindController rc = new RewindController();
    final JScrollPane leftScroll = new JScrollPane(createLeftSide());
    leftScroll.setBorder(AwtUtil.EMPTY_BORDER);
    UIController.CONTROLLER.putClientValue(leftScroll, rc);
    final JScrollPane rightScroll = new JScrollPane(createRightSide());
    rightScroll.setBorder(AwtUtil.EMPTY_BORDER);
    Aqua.cleanScrollPaneResizeCorner(rightScroll);
    UIController.CONTROLLER.putClientValue(rightScroll, rc);

    final JComponent keyPanel = createKeyPanel();
    final JComponent summaryPanel = createSummaryPanel();

    final JPanel topPanel = new JPanel(UIUtil.createBorderLayout());
    topPanel.setOpaque(false);
    topPanel.setBorder(TOP_PANEL_BORDER);
    topPanel.add(SingleChildLayout.envelopNorth(keyPanel), BorderLayout.WEST);
    topPanel.add(summaryPanel, BorderLayout.CENTER);
    topPanel.add(myToolbarPlace, BorderLayout.NORTH);

    final JPanel panel = new JPanel(new BorderLayout());
    ViewerFocusTraversalPolicy.install(panel);
    panel.setBorder(CONTENT_PANEL_BORDER);
    panel.setBackground(DocumentFormAugmentor.backgroundColor());
    panel.add(createSplitPane(leftScroll, rightScroll, myConfig.getOrCreateSubset("layout")), BorderLayout.CENTER);

    panel.add(topPanel, BorderLayout.NORTH);

    final JScrollPane overall = new JScrollPane(ScrollablePanel.adapt(panel)) {
      public void addNotify() {
        super.addNotify();
        ValueModel<Pattern> patternModel = getPatternModel(myComponent);
        myPatternModel = patternModel;
        if (patternModel != null) {
          patternModel.addAWTChangeListener(myLife.lifespan(), myPatternListener);
          myPatternListener.onChange();
        }
      }

      public void removeNotify() {
        ValueModel<Pattern> patternModel = myPatternModel;
        if (patternModel != null) {
          patternModel.removeChangeListener(myPatternListener);
        }
        myPatternModel = null;
        super.removeNotify();
      }
    };

    overall.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    overall.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    Aqua.cleanScrollPaneBorder(overall);
    Aero.cleanScrollPaneBorder(overall);

    myDocumentFormAugmentor.augmentForm(myLife.lifespan(), overall, false);

    panel.add(myBottomPlace, BorderLayout.SOUTH);
    // PLO-427 Glitch: icons and summary in item viewer are drawn above error notification pane
    panel.setComponentZOrder(topPanel, 1);
    panel.setComponentZOrder(myBottomPlace, 0);
    // make buttons not appear on bottom place when it covers them and mouse is hovered
    myBottomPlace.addMouseMotionListener(new MouseMotionListener() {
      public void mouseDragged(MouseEvent e) {
        e.consume();
      }

      public void mouseMoved(MouseEvent e) {
        e.consume();
      }
    });
    
    return overall;
  }

  public static ValueModel<Pattern> getPatternModel(Component comp) {
    Container parent = comp.getParent();
    ValueModel<Pattern> value;
    while (parent != null) {

      if (parent instanceof JComponent) {
        value = PATTERN_MODEL_PROPERTY.getClientValue(((JComponent) parent));
        if (value != null)
          return value;
      }

      parent = parent.getParent();
    }
    return null;
  }

  private JComponent createLeftSide() {
    RendererHostComponent host = new RendererHostComponent();
    addHighlightable(host);

    host.setBorder(LEFT_SIDE_BORDER);
    LeftFieldsBuilder fields = new LeftFieldsBuilder();
    addLeftSideFields(fields);
    host.setRenderer(fields.getPresentation());
    UIController.CONTROLLER.putClientValue(host, LeftFieldsBuilder.CONTROLLER);
    return new ScrollablePanel(host);
  }

  private JSplitPane createSplitPane(JScrollPane leftScroll, JScrollPane rightScroll, Configuration layout) {
    final JSplitPane splitPane = UIUtil.createSplitPane(leftScroll, rightScroll, true, layout, "splitter", 0.4d, 380);
    splitPane.setOpaque(false);
    splitPane.setResizeWeight(0F);
    splitPane.setBorder(SPLIT_PANE_BORDER);
    Aqua.makeLeopardStyleSplitPane(splitPane);
    return splitPane;
  }

  protected JComponent createSummaryPanel(ModelKey<String> modelKey) {
    LargeTextFormlet formlet = LargeTextFormlet.withString(modelKey, null);
    formlet.adjustFont(1.3F, Font.BOLD, true);
    formlet.trackViewportDimensions();
    addHighlightable(formlet);
    return formlet.getComponent();
  }

  protected JComponent createKeyPanel(ModelKey<String> modelKey) {
    LargeTextFormlet formlet = LargeTextFormlet.withString(modelKey, null);
    formlet.adjustFont(1.3F, Font.BOLD, true);
    formlet.setLineWrap(false);
    addHighlightable(formlet);
    return formlet.getComponent();
  }

  protected <T> JComponent createToStringKeyPanel(String prefix, ModelKey<T> modelKey) {
    LargeTextFormlet formlet = LargeTextFormlet.headerWithInt(modelKey, TextUtil.<T>prefixToString(prefix), null);
    formlet.adjustFont(1.3F, Font.BOLD, true);
    formlet.setLineWrap(false);
    addHighlightable(formlet);
    return formlet.getComponent();
  }

  public JComponent getComponent() {
    if (myComponent == null) {
      myComponent = init();

      if (myComponent == null) {
        assert false : "no component";
        Log.error("no viewer component", new IllegalStateException());
        myComponent = new JLabel("error");
      }

      myComponent.setName("CommonIssueViewer");
    }
    return myComponent;
  }

  public PlaceHolder getToolbarPlace() {
    return myToolbarPlace;
  }

  public PlaceHolder getBottomPlace() {
    return myBottomPlace;
  }

  @Deprecated
  public void dispose() {
    myLife.cycle();
    myComponent = null;
  }

  public Detach getDetach() {
    return myLife.getCurrentCycleDetach();
  }

  public static void addFormlet(WidthDrivenColumn column, String title, Formlet formlet, int index) {
    column.addComponent(new TitledFormlet(formlet, title, index));
  }

  protected WidthDrivenComponent createRightFieldFormlets(Configuration settings) {
    final WidthDrivenComponent widthDrivenComponent =
      RightViewerFields.createComponent(settings.getOrCreateSubset("rightCustomFields"), getViewerFieldManager(), getDocumentFormAugmentor(),
        new Procedure<Highlightable>() {
          public void invoke(Highlightable arg) {
            addHighlightable(arg);
          }
        }, new Procedure<Highlightable>() {
          public void invoke(Highlightable arg) {
            removeHighlightable(arg);
          }
        });
    return widthDrivenComponent;
  }

  private static class ViewerFocusTraversalPolicy extends FocusTraversalPolicy {
    private static final Function2<FocusTraversalPolicy, Container, Component> FIRST =
      new Function2<FocusTraversalPolicy, Container, Component>() {
        public Component invoke(FocusTraversalPolicy policy, Container container) {
          return policy.getFirstComponent(container);
        }
      };
    private static final Function2<FocusTraversalPolicy, Container, Component> LAST =
      new Function2<FocusTraversalPolicy, Container, Component>() {
        public Component invoke(FocusTraversalPolicy policy, Container container) {
          return policy.getLastComponent(container);
        }
      };
    private static final Function2<FocusTraversalPolicy, Container, Component> DEFAULT =
      new Function2<FocusTraversalPolicy, Container, Component>() {
        public Component invoke(FocusTraversalPolicy policy, Container container) {
          return policy.getDefaultComponent(container);
        }
      };
    private final JComponent myComponent;

    public ViewerFocusTraversalPolicy(JComponent component) {
      myComponent = component;
    }

    @Nullable
    public Component getComponentAfter(Container container, Component component) {
      Component next = getDefaultPolicy(container, component).getComponentAfter(container, component);
      if (next == null || isSameAncestor(container, component, next))
        return next;
      Container provider = SwingTreeUtil.findFocusPolicyProvider(next, myComponent);
      if (provider != null) {
        Component first = provider.getFocusTraversalPolicy().getFirstComponent(provider);
        if (first != null)
          next = first;
      }
      return next;
    }

    @Nullable
    public Component getComponentBefore(Container container, Component component) {
      Component prev = getDefaultPolicy(container, component).getComponentBefore(container, component);
      if (prev == null || isSameAncestor(container, component, prev))
        return prev;
      Container provider = SwingTreeUtil.findFocusPolicyProvider(prev, myComponent);
      if (provider != null) {
        Component last = provider.getFocusTraversalPolicy().getLastComponent(provider);
        if (last != null)
          prev = last;
      }
      return prev;
    }

    @Nullable
    public Component getFirstComponent(Container container) {
      return getBoundComponent(container, FIRST);
    }

    @Nullable
    private Component getBoundComponent(Container container,
      Function2<FocusTraversalPolicy, Container, Component> method)
    {
      FocusTraversalPolicy defaultPolicy = getDefaultPolicy();
      Component first = method.invoke(defaultPolicy, container);
      if (first == null)
        return null;
      FocusTraversalPolicy policy = getDefaultPolicy(container, first);
      if (policy == defaultPolicy)
        return first;
      Component other = method.invoke(policy, container);
      return other != null ? other : first;
    }

    @Nullable
    public Component getLastComponent(Container container) {
      return getBoundComponent(container, LAST);
    }

    @Nullable
    public Component getDefaultComponent(Container container) {
      return getBoundComponent(container, DEFAULT);
    }

    private FocusTraversalPolicy getDefaultPolicy() {
      return myComponent.getFocusCycleRootAncestor().getFocusTraversalPolicy();
    }

    private FocusTraversalPolicy getDefaultPolicy(Container container, Component component) {
      Container stopContainer;
      if (myComponent == container)
        stopContainer = myComponent;
      else if (!myComponent.isAncestorOf(container))
        return getDefaultPolicy();
      else
        stopContainer = container.getParent();
      if (stopContainer != null) {
        Container provider = SwingTreeUtil.findFocusPolicyProvider(component, stopContainer);
        if (provider != null)
          return provider.getFocusTraversalPolicy();
      }
      return getDefaultPolicy();
    }

    private boolean isSameAncestor(Container container, Component... components) {
      for (Component component : components) {
        if (!SwingTreeUtil.isAncestor(container, component))
          return false;
      }
      return true;
    }

    public static void install(JComponent component) {
      component.setFocusTraversalPolicy(new ViewerFocusTraversalPolicy(component));
      component.setFocusTraversalPolicyProvider(true);
    }
  }

  private static class RewindController implements UIController<JScrollPane> {
    @Override
    public void connectUI(@NotNull Lifespan lifespan, @NotNull ModelMap model, @NotNull JScrollPane component) {
      rewindScrollBar(component.getVerticalScrollBar());
      rewindScrollBar(component.getHorizontalScrollBar());
      DefaultUIController.connectChildren(component, lifespan, model);
    }

    private void rewindScrollBar(JScrollBar bar) {
      if(bar != null && bar.isVisible()) {
        bar.setValue(bar.getMinimum());
      }
    }
  }
}
