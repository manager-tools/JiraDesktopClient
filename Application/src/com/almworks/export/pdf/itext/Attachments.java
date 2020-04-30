package com.almworks.export.pdf.itext;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.download.DownloadManager;
import com.almworks.api.download.DownloadedFile;
import com.almworks.api.engine.Connection;
import com.almworks.engine.gui.attachments.Attachment;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.Computable;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.*;

/**
 * @author Alex
 */

public class Attachments extends PrintKeyElement<Document> {
  private final ModelKey<? extends Collection<? extends Attachment>> myModelKey;
  private final boolean myPrintText;
  private final boolean myPrintGraphics;
  private DownloadManager myDownloadManager;
  private ExportContext myContext;
  private List<AttachmentCopy> myData;

  protected Attachments(ModelKey<? extends Collection<? extends Attachment>> collectionModelKey, boolean printText, boolean printGraphics) {
    myModelKey = collectionModelKey;
    myPrintText = printText;
    myPrintGraphics = printGraphics;
    myDownloadManager = Context.require(DownloadManager.ROLE);
  }

  public void appendPrintElement(Document container, ReportMetrics metrics, PdfWriter writer) throws DocumentException {
    if (myData.isEmpty()) return;

    boolean headerPrinted = false;

    for (final AttachmentCopy attachmentCopy : myData) {
      String mime = attachmentCopy.getMimeType();
      String filePath;
      String fileName;
      File file = attachmentCopy.getLocalFile();
      if (file == null) {
        final DownloadedFile downloadedFile = myDownloadManager.getDownloadStatus(attachmentCopy.getUrl());
        if (downloadedFile.getState() != DownloadedFile.State.READY) continue;
        file = downloadedFile.getFile();
        if (file == null) continue;
        mime = downloadedFile.getMimeType();
      }
      filePath = file.getAbsolutePath();
      fileName = file.getName();
      assert fileName != null : attachmentCopy;
      assert filePath != null : attachmentCopy;
      if (mime == null) mime = FileUtil.guessMimeType(fileName);
      if (mime == null) continue;

      final Font font = metrics.whenWhoFont();

      PdfPTable attachTable = new PdfPTable(2);
      attachTable.setWidthPercentage(100);
      attachTable.setSpacingBefore(metrics.SPACING);
      attachTable.setExtendLastRow(false);
      attachTable.setSplitRows(true);
      attachTable.setSplitLate(false);
      Phrase phrase = new Phrase("", font);

      phrase.add(new Chunk("Attachment "));
      addLine(phrase, "  ", fileName);
      addLine(phrase, "  ", attachmentCopy.getUser());

      addPhraseCell(attachTable, phrase, Rectangle.ALIGN_LEFT);

      Date d = attachmentCopy.getDate();
      final String date = d == null ? "" : myContext.formatDate(d);
      phrase = new Phrase("", font);
      addLine(phrase, "", date);
      addPhraseCell(attachTable, phrase, Rectangle.ALIGN_RIGHT);

      boolean print = false;
      if (isGraphics(mime) && myPrintGraphics) {
        addContentLine(attachTable, printImage(container, filePath));
        attachTable.setKeepTogether(true);
        print = true;
      } else if (isText(mime) && myPrintText) {
        addContentLine(attachTable, printTextAttachment(metrics, attachmentCopy, filePath));
        print = true;
      }
      if (print) {
        if (!headerPrinted) {
          headerPrinted = true;
          final Paragraph paragraph = new Paragraph("Attachments", metrics.keyReportFont());
          paragraph.setSpacingBefore(metrics.SPACING);
          container.add(paragraph);
        }
        container.add(attachTable);
      }
    }
  }

  private void addPhraseCell(PdfPTable attachTable, Phrase phrase, int hAlign) {
    PdfPCell cell = new PdfPCell(phrase);
    cell.setPadding(0);
    cell.setVerticalAlignment(Rectangle.ALIGN_BASELINE);
    cell.setHorizontalAlignment(hAlign);
    cell.setUseAscender(true);
    cell.setBorder(0);
    attachTable.addCell(cell);
  }

  private Paragraph printTextAttachment(ReportMetrics metrics, AttachmentCopy attachment, String filename)
    throws DocumentException
  {
    File textFile = new File(filename);
    try {

      int expectedSize = attachment.getExpectedSize();
      StringBuilder sb = expectedSize <=0 ? new StringBuilder() : new StringBuilder(expectedSize);
      int len;
      char[] buffer = new char[1024];

      Reader readstream = new FileReader(textFile);
      try {
        while ((len = readstream.read(buffer)) > -1) {
          sb.append(buffer, 0, len);
        }
      } finally {
        readstream.close();
      }

      final Paragraph paragraph = new Paragraph(metrics.attachFont().getSize());
      paragraph.setAlignment(Paragraph.ALIGN_LEFT);
      paragraph.setFirstLineIndent(0);
      paragraph.setSpacingBefore(3);
      paragraph.setSpacingAfter(3);

      paragraph.add(new Phrase(metrics.attachFont().getSize(), sb.toString(), metrics.attachFont()));

      return paragraph;
    } catch (IOException e) {
      Log.error("Can't open file " + filename + "(" + e + ")");
      return null;
    }
  }

  private Image printImage(Document document, String filename) throws DocumentException {
    try {
      Image image = Image.getInstance(filename);

      image.scalePercent(40, 40);
      image.setInterpolation(true);

      float prWidth = document.right() - document.left();
      float prHeight = document.top() - document.bottom();

      if (image.getScaledWidth() > prWidth || image.getScaledHeight() > prHeight) {
        image.scaleToFit(prWidth, prHeight);
      }

      return image;
    } catch (IOException e) {
      Log.warn("can't load image " + filename);
    }
    return null;
  }

  private void addLine(Phrase phrase, String key, String value) {
    if (value == null || value.equals(""))
      return;
    phrase.add(new Chunk(key + value + " "));
  }

  private void addContentLine(PdfPTable attachTable, Image image) {
    if (image == null)
      return;
    PdfPCell cell = new PdfPCell(image);
    cell.setPadding(8f);
    cell.setColspan(2);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cell.setBorder(0);
    attachTable.addCell(cell);
  }

  private void addContentLine(PdfPTable attachTable, Paragraph text) {
    if (text == null)
      return;
    PdfPCell cell = new PdfPCell(text);
    cell.setColspan(2);
    cell.setBorder(0);
    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
    attachTable.addCell(cell);
  }

  private boolean isText(String mime) {
    return mime.equals("text/plain") || mime.contains("text") || mime.contains("html") || mime.contains("xml");
  }

  private boolean isGraphics(String mime) {
    return mime.startsWith("image") &&
      (mime.endsWith("jpg") || mime.endsWith("jpeg") || mime.endsWith("gif") || mime.endsWith("png"));
  }

  public void setContext(Connection connection, PropertyMap propertyMap, ExportContext context) {
    myData = AttachmentCopy.copyCollection(myModelKey.getValue(propertyMap));
    myContext = context;
  }

  private static class AttachmentCopy {
    private final String myUrl;
    private final String myUser;
    private final Date myDate;
    private final int mySize;
    private final String myMimeType;
    private final File myFile;

    private AttachmentCopy(String url, String user, Date date, int size, String mimeType, File file) {
      myUrl = url;
      myUser = user;
      myDate = date;
      mySize = size;
      myMimeType = mimeType;
      myFile = file;
    }

    public String getUrl() {
      return myUrl;
    }

    @Nullable
    public File getLocalFile() {
      return myFile;
    }

    public String getMimeType() {
      return myMimeType;
    }

    public String getUser() {
      return myUser;
    }

    public Date getDate() {
      return myDate;
    }

    public int getExpectedSize() {
      return mySize;
    }

    public static List<AttachmentCopy> copyCollection(final Collection<? extends Attachment> collection) {
      if (collection == null || collection.isEmpty()) return Collections.emptyList();
      return ThreadGate.AWT_IMMEDIATE.compute(new Computable<List<AttachmentCopy>>() {
        @Override
        public List<AttachmentCopy> compute() {
          ArrayList<AttachmentCopy> result = Collections15.arrayList();
          for (Attachment attachment : collection) result.add(copy(attachment));
          return result;
        }
      });
    }

    private static AttachmentCopy copy(Attachment attachment) {
      long size = attachment.getExpectedSize();
      if (size > Integer.MAX_VALUE) size = 0;

      return new AttachmentCopy(attachment.getUrl(), attachment.getUser(), attachment.getDate(), (int)size, attachment.getMimeType(null), attachment.getLocalFile(
        null));
    }
  }
}
