package com.almworks.items.gui.meta;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.ViewerFieldsManager;
import com.almworks.api.application.field.RightViewerFields;
import com.almworks.api.engine.Connection;
import com.almworks.engine.gui.CommonIssueViewer;
import com.almworks.engine.gui.Formlet;
import com.almworks.engine.gui.ItemTableBuilder;
import com.almworks.engine.gui.LeftFieldsBuilder;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.ItemImageCollector;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.schema.gui.ViewerField;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.SequenceRunner;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.layout.WidthDrivenColumn;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererLine;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.ComponentProperty;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.almworks.util.Collections15.arrayList;
import static org.almworks.util.Collections15.emptyList;

public class ViewerFieldsCollector implements ItemImageCollector.ImageFactory<ViewerField>, ViewerFieldsManager {
  private static final DataLoader<LongList> CONNECTION_FIELDS = ViewerField.CONNECTION_FIELDS;
  private final ItemImageCollector<ViewerField> myFields;
  private final ImageSlice myConnections;
  private final SequenceRunner myRightFieldsUpdaters = new SequenceRunner();
  private final Bottleneck myRightUpdater = new Bottleneck(200, ThreadGate.AWT_QUEUED, myRightFieldsUpdaters);

  private ViewerFieldsCollector(ImageSlice slice, ImageSlice connections) {
    myConnections = connections;
    myFields = ItemImageCollector.create(slice, this, false);
  }

  public static ViewerFieldsCollector create(DBImage image) {
    QueryImageSlice slice = image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, ViewerField.DB_TYPE));
    QueryImageSlice connections = image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, SyncAttributes.TYPE_CONNECTION));
    return new ViewerFieldsCollector(slice, connections);
  }

  public void start(Lifespan life) {
    myConnections.ensureStarted(life);
    myConnections.addData(CONNECTION_FIELDS);
    myFields.start(life);
    myFields.getSlice().addData(ViewerField.LOADER);
  }

  @Override
  public boolean update(ViewerField image, long item) {
    ViewerField updated = create(item);
    return image.updateFrom(updated);
  }

  @Override
  public void onRemoved(ViewerField image) {
    scheduleUiRedraw();
  }

  @Override
  public ViewerField create(long item) {
    scheduleUiRedraw();
    return myFields.getSlice().getValue(item, ViewerField.LOADER);
  }

  private void scheduleUiRedraw() {
    myRightUpdater.request();
  }

  public void addLeftFields(ItemTableBuilder builder) {
    builder.addLine(new LeftFieldsTableRendererLine());
  }

  @Override
  public void addRightFields(final RightViewerFields host, final WidthDrivenColumn hostComponent, Lifespan life, final ModelMap model, final Configuration settings) {
    RightFieldsCreator creator = new RightFieldsCreator(life, settings, host, hostComponent, model);
    creator.run();
    life.add(myRightFieldsUpdaters.addReturningDetach(creator));
  }

  /** @return {@code List<@NotNull ViewerField>} */
  private List<ViewerField> getFields(@Nullable ModelMap modelMap) {
    LoadedItemServices service = modelMap != null ? LoadedItemServices.VALUE_KEY.getValue(modelMap) : null;
    Connection connection = service != null ? service.getConnection() : null;
    long connectionItem = connection != null ? connection.getConnectionItem() : 0L;
    if (connectionItem <= 0L || connection == null || connection.getState().getValue().isDegrading()) return emptyList();
    LongList fieldItems = myConnections.getValue(connectionItem, CONNECTION_FIELDS);
    List<ViewerField> fields = arrayList(fieldItems.size());
    for (LongListIterator i = fieldItems.iterator(); i.hasNext(); ) {
      long fieldItem = i.nextValue();
      ViewerField field = myFields.getImage(fieldItem);
      if (field == null) {
        LogHelper.error("Missing field", fieldItem);
        continue;
      }
      fields.add(field);
    }
    return fields;
  }


  private class LeftFieldsTableRendererLine implements TableRendererLine {
    private final TypedKey<List<TableRendererLine>> LINES = TypedKey.create("lines");
    private final ComponentProperty<Integer> LINE = ComponentProperty.createProperty("cfLine");

    private final TableRenderer myRenderer = new TableRenderer();

    public LeftFieldsTableRendererLine() {
    }

    @Override
    public int getHeight(int width, RendererContext context) {
      List<TableRendererLine> lines = getLines(context);
      int height = 0;
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < lines.size(); i++) {
        TableRendererLine line = lines.get(i);
        height += line.getHeight(width, context);
      }
      if (!lines.isEmpty())
        height += myRenderer.getVerticalGap() * (lines.size() - 1);
      return height;
    }

    @Override
    public int getWidth(int columnIndex, RendererContext context) {
      List<TableRendererLine> lines = getLines(context);
      int width = 0;
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < lines.size(); i++) {
        TableRendererLine line = lines.get(i);
        width = Math.max(width, line.getWidth(columnIndex, context));
      }
      return width;
    }

    @Override
    public boolean isVisible(RendererContext context) {
      return true;
    }

    @Override
    public void paint(Graphics g, int y, RendererContext context) {
      List<TableRendererLine> lines = getLines(context);
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < lines.size(); i++) {
        TableRendererLine line = lines.get(i);
        line.paint(g, y, context);
        y += line.getHeight(context.getWidth(), context) + myRenderer.getVerticalGap();
      }
    }

    @Override
    public void invalidateLayout(RendererContext context) {
    }

    @Override
    public RendererActivity getActivityAt(int id, int columnIndex, int x, int y, RendererContext context,
      Rectangle rectangle)
    {
      List<TableRendererLine> lines = getLines(context);
      int yOffset = 0;
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < lines.size(); i++) {
        if (y < 0)
          return null;
        TableRendererLine line = lines.get(i);
        int height = line.getHeight(context.getWidth(), context);
        if (y < height) {
          RendererActivity activity = line.getActivityAt(id, columnIndex, x, y, context, rectangle);
          if (activity != null) {
            rectangle.y += yOffset;
            activity.storeValue(LINE, i);
          }
          return activity;
        }
        y -= height + myRenderer.getVerticalGap();
        yOffset += height + myRenderer.getVerticalGap();
      }
      return null;
    }

    @Override
    public int getPreferedWidth(int column, RendererContext context) {
      return -1;
    }

    @Override
    public JComponent getNextLiveComponent(@NotNull RendererContext context, TableRenderer tableRenderer,
      @Nullable JComponent current, @NotNull Rectangle targetArea, boolean next)
    {
      Integer firstLine = current != null ? LINE.getClientValue(current) : Integer.valueOf(-1);
      if (firstLine == null) {
        assert false : current;
        firstLine = -1;
      }
      List<TableRendererLine> lines = getLines(context);
      if (firstLine == -1) {
        firstLine = next ? 0 : lines.size() - 1;
        current = null;
      }
      for (int i = firstLine; next ? (i < lines.size()) : i >= 0; i += next ? 1 : -1) {
        TableRendererLine line = lines.get(i);
        JComponent result = line.getNextLiveComponent(context, myRenderer, current, targetArea, next);
        if (result != null) {
          targetArea.y += TableRenderer.getLineTop(lines, i, context, myRenderer.getVerticalGap(), null);
          LINE.putClientValue(result, i);
          return result;
        }
        current = null;
      }
      return null;
    }

    @Override
    public boolean getLineRectangle(int y, Rectangle lineRectangle, RendererContext context) {
      List<TableRendererLine> lines = getLines(context);
      int yOffset = 0;
      for (TableRendererLine line : lines) {
        int lineHeight = line.getHeight(lineRectangle.width, context);
        if (y < lineHeight) {
          lineRectangle.y += yOffset;
          lineRectangle.height = lineHeight;
          return line.getLineRectangle(y, lineRectangle, context);
        }
        int yDelta = lineHeight + myRenderer.getVerticalGap();
        y -= yDelta;
        yOffset += yDelta;
        if (y < 0)
          return false;
      }
      return false;
    }

    private List<TableRendererLine> getLines(RendererContext context) {
      List<TableRendererLine> lines = context.getCachedValue(LINES);
      if (lines == null) {
        lines = createLines(context);
        context.cacheValue(LINES, lines);
      }
      return lines;
    }

    protected List<TableRendererLine> createLines(RendererContext context) {
      ModelMap modelMap = LeftFieldsBuilder.getModelMapFromContext(context);
      List<TableRendererLine> result = arrayList();
      for (ViewerField field : getFields(modelMap)) {
        List<? extends TableRendererLine> lines = field.createLeftFieldLines(myRenderer, context);
        for (TableRendererLine line : lines) {
          if (line == null || !line.isVisible(context)) continue;
          result.add(line);
        }
      }
      return result;
    }
  }


  private class RightFieldsCreator implements Runnable, ChangeListener {
    private final Lifecycle myChangeCycle = new Lifecycle();
    private final Configuration mySettings;
    private final RightViewerFields myHost;
    private final WidthDrivenColumn myHostComponent;
    private final ModelMap myModel;
    private boolean myInUpdate;

    public RightFieldsCreator(Lifespan life, Configuration settings, RightViewerFields host, WidthDrivenColumn hostComponent, ModelMap model) {
      life.add(myChangeCycle.getDisposeDetach());
      mySettings = settings;
      myHost = host;
      myHostComponent = hostComponent;
      myModel = model;
    }

    @Override
    public void run() {
      update();
    }

    @Override
    public void onChange() {
      update();
    }

    private void update() {
      if (myInUpdate) return;
      myInUpdate = true;
      try {
        doUpdate();
      } finally {
        myInUpdate = false;
      }
    }

    private void doUpdate() {
      myHostComponent.removeAllComponents();
      addFields();
      Container parent = myHostComponent.getParent();
      if (parent.isShowing()) {
        parent.invalidate();
        parent.repaint();
      }
    }

    private void addFields() {
      myChangeCycle.cycle();
      Lifespan beforeUpdate = myChangeCycle.lifespan();
      for (ViewerField field : getFields(myModel)) {
        ViewerField.RightField rf = field.createRightField(mySettings);
        if (rf == null) continue;
        Formlet formlet = rf.getFormlet();
        CommonIssueViewer.addFormlet(myHostComponent, Util.NN(rf.getDisplayName()), formlet, -1);
        if (formlet instanceof Highlightable) {
          myHost.setupHighlighting((Highlightable) formlet, beforeUpdate);
        }
        formlet.getModifiable().addAWTChangeListener(beforeUpdate, this);
        rf.connectUI(beforeUpdate, myModel);
        myHost.getDocumentFormAugmentor().setupDescendantsOpaque(myHostComponent);
        myHostComponent.setOpaque(false);
      }
    }
  }
}
