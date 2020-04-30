package com.almworks.export.pdf.itext;

import com.almworks.api.application.util.ItemExport;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * @author Alex
 */

public class LargeTextElement extends ExportedPrintElement<Document> {
  private final ItemExport myExport;

  protected LargeTextElement(ItemExport export) {
    super(export);
    myExport = export;
  }

  public void appendPrintElement(Document container, ReportMetrics metrics, PdfWriter writer) throws DocumentException {
    if (!getExportText().equals("")) {
      final float fontSize = metrics.keyReportFont().getSize();
      Paragraph paragraph = new Paragraph(myExport.getDisplayName(), metrics.keyReportFont());

      paragraph.setSpacingBefore(metrics.SPACING);
      paragraph.setSpacingAfter(fontSize / 3);
      container.add(paragraph);
      paragraph =
        new Paragraph(metrics.BASIC_TEXT_LEADING, getExportText(), metrics.commentDescriptionFont());
      paragraph.setSpacingAfter(metrics.SPACING);

      container.add(paragraph);
    }
  }
}
