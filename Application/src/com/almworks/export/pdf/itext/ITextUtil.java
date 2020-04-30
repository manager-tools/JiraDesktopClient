package com.almworks.export.pdf.itext;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * @author Alex
 */

public class ITextUtil {
  public static void setSpacing(PdfPTable table, float spacing) {
    table.setSpacingAfter(spacing);
    table.setSpacingBefore(spacing);
  }

  public static void setSpacing(Paragraph par, float spacing) {
    par.setSpacingAfter(spacing);
    par.setSpacingBefore(spacing);
  }
}
