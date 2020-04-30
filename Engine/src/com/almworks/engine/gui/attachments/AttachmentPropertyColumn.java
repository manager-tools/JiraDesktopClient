package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadedFile;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.models.TableColumnAccessor;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AttachmentPropertyColumn<T extends Attachment> extends BaseTableColumnAccessor<T, Object> {
  private final AttachmentProperty<? super T, ?> myProperty;
  private final AttachmentDownloadStatus<T> myStatus;

  public AttachmentPropertyColumn(AttachmentProperty<? super T, ?> property, AttachmentDownloadStatus<T> status) {
    super(property.getName(), Renderers.createRenderer(new AttachmentPropertyRenderer<T>(property, status)), (Comparator) property);
    myProperty = property;
    myStatus = status;
  }

  public Object getValue(T attachment) {
    String url = attachment.getUrl();
    DownloadedFile file = myStatus.getDownloadedFile(url);
    Object value = ((AttachmentProperty<? super T, ?>) myProperty).getColumnValue(attachment, file);
    return value;
  }

  public static <T extends Attachment> List<TableColumnAccessor<T, ?>> collectList(Collection<AttachmentProperty<? super T, ?>> properties,
    AttachmentDownloadStatus<T> status) {
    List<TableColumnAccessor<T, ?>> result = Collections15.arrayList();
    for (final AttachmentProperty<? super T, ?> property : properties)
      result.add(new AttachmentPropertyColumn(property, status));
    return result;
  }

  private static class AttachmentPropertyRenderer<T extends Attachment> implements CanvasRenderer<T> {
    private final AttachmentProperty<? super T, ?> myProperty;
    private final AttachmentDownloadStatus<T> myStatus;

    public AttachmentPropertyRenderer(AttachmentProperty<? super T, ?> property, AttachmentDownloadStatus<T> status) {
      myProperty = property;
      myStatus = status;
    }

    public void renderStateOn(CellState state, Canvas canvas, T attachment) {
      String url = attachment.getUrl();
      DownloadedFile file = myStatus.getDownloadedFile(url);
      String str = myProperty.getStringValue(attachment, file);
      if (str != null) canvas.appendText(str);
//      Object value1 = ((AttachmentProperty<? super T, ?>) myProperty).getColumnValue(attachment, file);
//      Object value = value1;
//      ((CanvasRenderer) myProperty).renderStateOn(state, canvas, value);
    }
  }
}
