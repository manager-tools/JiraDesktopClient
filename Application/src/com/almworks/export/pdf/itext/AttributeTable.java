package com.almworks.export.pdf.itext;

import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.engine.Connection;
import com.almworks.util.properties.PropertyMap;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.List;

/**
 * @author Alex
 */

public class AttributeTable implements PrintElement<Document> {
  private int myColCount;
  List<AttributeLine> myList = Collections15.arrayList();

  public AttributeTable() {
    myColCount = 1;
  }

  public void addAttribute(ItemExport att) {
    myList.add(new AttributeLine(att));
  }

  public void setContext(Connection connection, PropertyMap propertyMap, ExportContext context) {
    for (PrintElement printElement : myList) {
      printElement.setContext(connection, propertyMap, context);
    }
  }

  public void appendPrintElement(Document container, ReportMetrics metrics, PdfWriter writer) throws DocumentException {
    int size = myList.size();
    int colCount = size <= 3 ? 1 : myColCount;
    PdfPTable table = new PdfPTable(colCount);
    table.setSplitRows(false);
    table.setSplitLate(true);
    table.setExtendLastRow(false);
    table.setWidthPercentage(100);
    ITextUtil.setSpacing(table, metrics.SPACING);

    if (colCount == 2) {
      PdfPTable col1Table = createColumnTable(metrics, myList.subList(0, size / 2 + size % 2), 2);
      PdfPTable col2Table = createColumnTable(metrics, myList.subList(size / 2 + size % 2, size), 2);
      addBorderlessTableCell(table, col1Table);
      addBorderlessTableCell(table, col2Table);
    } else if (colCount == 1) {
      PdfPTable colTable = createColumnTable(metrics, myList.subList(0, size), 1);
      addBorderlessTableCell(table, colTable);
    } else {
      assert false : myColCount + " " + colCount;
      Log.warn("PdfExp.AT: col count " + myColCount + " " + colCount);
      return;
    }
    container.add(table);
  }

  private void addBorderlessTableCell(PdfPTable table, PdfPTable col1Table) {
    PdfPCell cell = new PdfPCell(col1Table);
    cell.setBorder(0);
    table.addCell(cell);
  }

  private PdfPTable createColumnTable(ReportMetrics metrics, List<AttributeLine> lines, int colCount) throws DocumentException {
    PdfPTable colTable = new PdfPTable(2);
    int counter = 0;

    for (int i = 0; i < lines.size(); i++) {
      AttributeLine printElement = lines.get(i);

      printElement.setDark((counter++) % 2 == 0);
      printElement.setCompact(colCount > 1);
      printElement.appendPrintElement(colTable, metrics, null);
    }

    float[] floats = new float[2];
    float x = (100.f / colCount) / 5;
    for (int i = 0; i < floats.length; i++) {
      floats[i] = 100.f / colCount - x;
      x = -x;
    }

    colTable.setWidths(floats);
    colTable.setExtendLastRow(false);
    return colTable;
  }

  public void setColCount(int colCount) {
    myColCount = colCount;
  }

  public static class AttributeLine extends ExportedPrintElement<PdfPTable> {
    boolean myIsDark;
    public static final BaseColor LIGHT_GRAY = new BaseColor(230, 230, 230);
    private boolean myCompact;

    public void setDark(boolean dark) {
      myIsDark = dark;
    }

    public void setCompact(boolean isCompact) {
      myCompact = isCompact;
    }

    protected AttributeLine(ItemExport export) {
      super(export);
    }

    public void appendPrintElement(PdfPTable container, ReportMetrics metrics, PdfWriter writer)
      throws DocumentException
    {
      PdfPCell cell =
        new PdfPCell(new Paragraph(getExport().getDisplayName(), metrics.attribureNameFont(myCompact)));
      cell.setBackgroundColor(myIsDark ? LIGHT_GRAY : BaseColor.WHITE);
      cell.setBorder(0);
      cell.setVerticalAlignment(Element.ALIGN_BASELINE);
      cell.setUseAscender(true);

      container.addCell(cell);

      cell = new PdfPCell(new Paragraph(metrics.attribureValueFont(myCompact).getSize(), getExportText(),
        metrics.attribureValueFont(myCompact)));
      cell.setVerticalAlignment(Element.ALIGN_BASELINE);
      cell.setBackgroundColor(myIsDark ? LIGHT_GRAY : BaseColor.WHITE);
      cell.setBorder(0);
      cell.setUseAscender(true);
      container.addCell(cell);
    }
  }
}
