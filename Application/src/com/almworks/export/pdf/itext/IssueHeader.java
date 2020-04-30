package com.almworks.export.pdf.itext;

import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.Connection;
import com.almworks.util.properties.PropertyMap;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.almworks.util.Collections15;

import java.util.List;

/**
 * @author Alex
 */

public class IssueHeader implements PrintElement<Document> {
  private final List<PrintKeyElement.PhrasePrintKeyElement> myModelKeyList;

  public IssueHeader(ItemExport issueKey, ItemExport ... others) {
    myModelKeyList = Collections15.arrayList();
    final PrintKeyElement.PhrasePrintKeyElement element = new PrintKeyElement.PhrasePrintKeyElement(issueKey);
    myModelKeyList.add(element);
    for (ItemExport modelKey : others) {
      myModelKeyList.add(new PrintKeyElement.PhrasePrintKeyElement(modelKey));
    }
  }

  public void setContext(Connection connection, PropertyMap propertyMap, ExportContext context) {
    for (PrintElement<Phrase> phrasePrintElement : myModelKeyList) {
      phrasePrintElement.setContext(connection, propertyMap, context);
    }
  }

  public void appendPrintElement(Document container, ReportMetrics metrics, PdfWriter writer) throws DocumentException {
    final Font font = metrics.headerFontBold();
    Paragraph par = new Paragraph(font.getSize() * 1.2f, "", font);
    ITextUtil.setSpacing(par, metrics.SPACING);

    StringBuffer sb = new StringBuffer("issue");

    for (PrintKeyElement.PhrasePrintKeyElement phrasePrintElement : myModelKeyList) {
      phrasePrintElement.appendPrintElement(par, metrics, writer);
      par.add(new Chunk(" "));
      sb.append(phrasePrintElement.getExportText()).append(' ');
    }

    Chunk label = new Chunk(" ");
    label.setGenericTag(sb.toString());
    par.add(label);

    container.add(par);
  }
}
