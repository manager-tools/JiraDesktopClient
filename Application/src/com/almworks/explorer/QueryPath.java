package com.almworks.explorer;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.Pair;
import com.almworks.util.collections.CollectionUtil;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.IntIntFunction;
import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.renderer.Renderer;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.BottleneckJobs;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DefaultActionContext;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author dyoma
 */
class QueryPath implements Renderer {
  private final TitleSection myNameSection;
  private final List<TitleSection> myContext;
  private static final TypedKey<Pair<Integer, Rectangle>> ACTIVE_SECTION = TypedKey.create("activeSection");
  private static final TypedKey<Integer> NAME_GAP = TypedKey.create("nameGap");
  private static final TypedKey<Integer> CONTEXT_GAP = TypedKey.create("contextGap");
  private final String myContextSeparator;
  private static final String PATH_PREFIX = " (in ";
  private static final String PATH_POSTFIX = ")";

  private QueryPath(List<TitleSection> sections, TitleSection nameSection, String contextSeparator) {
    myNameSection = nameSection;
    myContext = sections;
    myContextSeparator = contextSeparator;
  }

  public static QueryPath create(ItemCollectionContext info) {
    GenericNode queryNode = info.getQuery();
    String separator = ", ";
    Collection<? extends GenericNode> contextNodes = info.getContextNodes();
    if(contextNodes == null) {
      contextNodes = Collections15.emptyList();
    }
    if(contextNodes.size() == 1) {
      Iterator<? extends GenericNode> ii = contextNodes.iterator();
      GenericNode node = ii.next();
      if (node != null) {
        contextNodes = node.getPathFromRoot();
        separator = ItemCollectionContext.PATH_SEPARATOR;
      }
    }
    TitleSection name = queryNode != null ? new NodeTitleSection(queryNode) : new PlainSection(info.getShortName());
    return new QueryPath(NodeTitleSection.CREATE.collectList(contextNodes), name, separator);
  }

  public int getPreferedWidth(RendererContext context) {
    int result = myNameSection.getPreferedWith(context);
    if (myContext.isEmpty())
      return result;
    result += getGapAfterName(context);
    for (TitleSection section : myContext)
      result += section.getPreferedWith(context);
    result += getContextGap(context) * (myContext.size() - 1);
    return result;
  }

  private int getGapAfterName(RendererContext context) {
    Integer gap = context.getCachedValue(NAME_GAP);
    if (gap == null) {
      gap = context.getStringWidth(PATH_PREFIX, Font.PLAIN);
      context.cacheValue(NAME_GAP, gap);
    }
    return gap;
  }

  private int getContextGap(RendererContext context) {
    Integer gap = context.getCachedValue(CONTEXT_GAP);
    if (gap == null) {
      gap = context.getStringWidth(myContextSeparator, Font.PLAIN);
      context.cacheValue(CONTEXT_GAP, gap);
    }
    return gap;
  }

  public int getPreferedHeight(int width, RendererContext context) {
    return getPreferedHeight(context);
  }

  private int getPreferedHeight(RendererContext context) {
    return context.getFontHeight(Font.BOLD);
  }

  public Dimension getPreferredSize(RendererContext context) {
    return new Dimension(getPreferedWidth(context), getPreferedHeight(context));
  }

  public void paint(Graphics g, RendererContext context) {
    ensureValid(context);
    final int vgap = Math.max((context.getHeight() - getPreferedHeight(context)) / 2, 0);
    if(vgap > 0) {
      g.translate(0, vgap);
      try {
        paintSections(g, context);
      } finally {
        g.translate(0, -vgap);
      }
    } else {
      paintSections(g, context);
    }
  }

  private void paintSections(Graphics g, RendererContext context) {
    final Pair<Integer, Rectangle> activeSection = context.getValue(ACTIVE_SECTION);
    final int activeIndex = activeSection != null ? activeSection.getFirst() : -2;
    myNameSection.paint(0, g, context, activeIndex == -1);

    if (myContext.isEmpty()) {
      return;
    }

    int x = myNameSection.getWidth(context);
    g.setFont(context.getFont(Font.PLAIN));
    g.drawString(PATH_PREFIX, x, context.getFontBaseLine(Font.PLAIN));
    x += getGapAfterName(context);
    int gap = getContextGap(context);
    for (int i = 0; i < myContext.size(); i++) {
      TitleSection section = myContext.get(i);
      section.paint(x, g, context, i == activeIndex);
      int width = section.getWidth(context);
      x += width;
      if (width > 0 && i < myContext.size() - 1) {
        g.setFont(context.getFont(Font.PLAIN));
        g.drawString(myContextSeparator, x, context.getFontBaseLine(Font.PLAIN));
        x += gap;
      }
    }
    g.drawString(PATH_POSTFIX, x, context.getFontBaseLine(Font.PLAIN));
  }

  public boolean isFocusable() {
    return false;
  }

  public void processMouseEvent(int id, int x, int y, RendererContext context, RendererActivityController controller) {
    ensureValid(context);
    if (id == MouseEvent.MOUSE_EXITED) {
      clearPrevSection(context, controller);
      return;
    }
    Rectangle rect = new Rectangle();
    int index = findSection(x, context, rect);
    if (index < -1) {
      clearPrevSection(context, controller);
      return;
    }
    TitleSection section = getSection(index);
    if (id == MouseEvent.MOUSE_CLICKED)
      section.processClick(context);
    Pair<Integer, Rectangle> prevSection = context.getValue(ACTIVE_SECTION);
    if (prevSection != null && index == prevSection.getFirst())
      return;
    clearPrevSection(context, controller);
    context.putValue(ACTIVE_SECTION, Pair.create(index, rect));
    controller.repaint(rect);
    section.activate(context, controller, rect);
    return;
  }

  private TitleSection getSection(int index) {
    return index == -1 ? myNameSection : myContext.get(index);
  }

  public boolean updateMouseActivity(int id, int x, int y, RendererContext context, RendererActivityController controller) {
    return false;
  }

  public void addAWTListener(Lifespan life, Listener listener) {
  }

  private void ensureValid(RendererContext context) {
    int widthLeft = context.getWidth();
    myNameSection.setWidth(context, widthLeft);
    if (myContext.isEmpty())
      return;
    TitleSection section = myNameSection;
    int gap = getContextGap(context);
    for (int i = myContext.size() - 1; i >= 0; i--) {
      widthLeft -= section.getWidth(context) + gap;
      section = myContext.get(i);
      section.setWidth(context, Math.max(widthLeft, 0));
    }
  }

  private void clearPrevSection(RendererContext context, RendererActivityController controller) {
    Pair<Integer, Rectangle> prevSection = context.getValue(ACTIVE_SECTION);
    if (prevSection != null) {
      controller.repaint(prevSection.getSecond());
      getSection(prevSection.getFirst()).deactivate(context, controller);
    }
    context.putValue(ACTIVE_SECTION, null);
  }

  @Nullable
  public JComponent getNextLiveComponent(@NotNull RendererContext context, @Nullable JComponent current,
    @NotNull Rectangle targetArea, boolean next)
  {
    return null;
  }

  private int findSection(int x, RendererContext context, Rectangle rect) {
    int nameWidth = myNameSection.getWidth(context);
    if (x < nameWidth) {
      rect.x = 0;
      rect.y = 0;
      rect.height = getPreferedHeight(context);
      rect.width = nameWidth;
      return -1;
    }
    int offset = nameWidth + getGapAfterName(context);
    int gap = getContextGap(context);
    for (int i = 0; i < myContext.size(); i++) {
      if (x < offset)
        return -2;
      TitleSection section = myContext.get(i);
      int width = section.getWidth(context);
      if (x >= offset && x <= offset + width) {
        rect.x = offset;
        rect.y = 0;
        rect.height = getPreferedHeight(context);
        rect.width = width;
        return i;
      }
      if (width > 0)
        offset += width + gap;
    }
    return -2;
  }

  private interface TitleSection {
    int getPreferedWith(RendererContext context);

    int getWidth(RendererContext context);

    void paint(int x, Graphics g, RendererContext context, boolean active);

    void processClick(RendererContext context);

    void activate(RendererContext context, RendererActivityController controller, Rectangle rect);

    void setWidth(RendererContext context, int width);

    void deactivate(RendererContext context, RendererActivityController controller);
  }

  private static abstract class BaseTitleSection implements TitleSection {
    private final TypedKey<String> VISIBLE_TITLE = TypedKey.create("visibleTitle");

    public int getPreferedWith(RendererContext context) {
      return context.getStringWidth(getName(), getFontStyle());
    }

    protected abstract int getFontStyle();

    public int getWidth(RendererContext context) {
      assert checkValid(context);
      String title = context.getCachedValue(VISIBLE_TITLE);
      return context.getStringWidth(title, getFontStyle());
    }

    public boolean checkValid(RendererContext context) {
      assert context.getCachedValue(VISIBLE_TITLE) != null;
      return true;
    }

    public void paint(int x, Graphics g, RendererContext context, boolean active) {
      assert checkValid(context);
      int y = context.getFontBaseLine(getFontStyle());
      g.setFont(context.getFont(getFontStyle()));
      g.drawString(context.getCachedValue(VISIBLE_TITLE), x, y);
      if (active)
        g.drawLine(x, y + 1, x + getWidth(context) - 1, y + 1);
    }

    public void setWidth(final RendererContext context, final int width) {
      String prev = context.getValue(VISIBLE_TITLE);
      if (prev == null) {
        String name = getName();
        if (context.getStringWidth(name, getFontStyle()) < width) {
          context.cacheValue(VISIBLE_TITLE, name);
          return;
        }
        prev = name;
      } else {
        int prevWidth = context.getStringWidth(prev, getFontStyle());
        if (prevWidth >= width && prevWidth < width + context.getFontMetrics(getFontStyle()).charWidth('W'))
          return;
      }
      final String name = getName();
      int length = CollectionUtil.binarySearch(name.length(), new IntIntFunction() {
        public int invoke(int a) {
          return context.getStringWidth(name.substring(0, a), getFontStyle()) - width;
        }
      });
      if (length < 0)
        length = -length - 1;
      if (length > name.length() * 0.3)
        context.cacheValue(VISIBLE_TITLE, name.substring(0, length));
      else
        context.cacheValue(VISIBLE_TITLE, "");
    }

    protected abstract String getName();
  }


  private static class PlainSection extends BaseTitleSection {
    private final String myName;

    public PlainSection(String name) {
      myName = name;
    }

    protected String getName() {
      return myName;
    }

    public void paint(int x, Graphics g, RendererContext context, boolean active) {
      super.paint(x, g, context, false);
    }

    public void processClick(RendererContext context) {}

    public void activate(RendererContext context, RendererActivityController controller, Rectangle rect) {}

    public void deactivate(RendererContext context, RendererActivityController controller) {}

    protected int getFontStyle() {
      return Font.PLAIN;
    }
  }

  private static class NodeTitleSection extends BaseTitleSection {
    private static final Convertor<GenericNode, TitleSection> CREATE = new Convertor<GenericNode, TitleSection>() {
      public TitleSection convert(GenericNode value) {
        return new NodeTitleSection(value);
      }
    };
    private static final BottleneckJobs<NodeTitleSection> HIGHLIGHT = new BottleneckJobs<NodeTitleSection>(150, ThreadGate.AWT) {
      protected void execute(NodeTitleSection job) {
        Set<GenericNode> nodes = job.myNode == null ? Collections.<GenericNode>emptySet() : Collections.singleton(job.myNode);
        Context.require(ExplorerComponent.class).setHighlightedNodes(HIGHLIGHT_KEY, nodes, Color.RED, null, null);
      }
    };


    private final GenericNode myNode;
    private static final TypedKey<Object> HIGHLIGHT_KEY = TypedKey.create("hoverHighlight");

    public NodeTitleSection(GenericNode node) {
      myNode = node;
    }

    public void deactivate(RendererContext context, RendererActivityController controller) {
      controller.removeMouseActivity();
      HIGHLIGHT.abort();
      getExplorer(context).clearHighlightedNodes(HIGHLIGHT_KEY);
    }

    public void activate(RendererContext context, RendererActivityController controller, Rectangle rectangle) {
      controller.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), rectangle);
      HIGHLIGHT.addJobDelayed(this);
    }

    public void processClick(RendererContext rendererContext) {
      if (myNode != null) {
        getExplorer(rendererContext).selectNavigationNode(myNode);
      }
    }

    private ExplorerComponent getExplorer(RendererContext rendererContext) {
      DefaultActionContext context = new DefaultActionContext(rendererContext.getComponent());
      ExplorerComponent explorerComponent = null;
      try {
        explorerComponent = context.getSourceObject(ExplorerComponent.ROLE);
      } catch (CantPerformException e) {
        throw new Failure(e);
      }
      return explorerComponent;
    }

    protected String getName() {
      return myNode == null ? "" : myNode.getName();
    }

    protected int getFontStyle() {
      return Font.BOLD;
    }
  }
}
