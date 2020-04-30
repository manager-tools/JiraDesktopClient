package com.almworks.export.pdf.itext;

import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.Connection;
import com.almworks.util.properties.PropertyMap;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * @author Alex
 */
public abstract class PrintKeyElement<D> implements PrintElement<D> {
  public PrintKeyElement() {
  }

  public abstract void setContext(Connection connection, PropertyMap propertyMap, ExportContext context);

  public static class PhrasePrintKeyElement<T> extends ExportedPrintElement<Phrase> {

    protected PhrasePrintKeyElement(ItemExport tModelKey) {
      super(tModelKey);
    }

    public void appendPrintElement(Phrase container, ReportMetrics metrics, PdfWriter writer) throws DocumentException {
      if (getExportText() != null) {
        final Chunk o = new Chunk(getExportText() + " ");
        container.add(o);
      }
    }
  }
}
