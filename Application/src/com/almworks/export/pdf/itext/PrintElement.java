package com.almworks.export.pdf.itext;

import com.almworks.api.application.util.ExportContext;
import com.almworks.api.engine.Connection;
import com.almworks.util.properties.PropertyMap;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * @author Alex
 */
public interface PrintElement<D> {
  void setContext(Connection connection, PropertyMap propertyMap, ExportContext context);

  void appendPrintElement(D container, ReportMetrics metrics, PdfWriter writer) throws DocumentException;
}
