package com.almworks.feedback;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.platform.ProductInformation;
import com.almworks.util.LogHelper;
import com.almworks.util.Terms;
import com.almworks.util.files.FileUtil;
import com.almworks.util.i18n.Local;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.WindowUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.SimpleAction;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class ThreadDumpAction extends SimpleAction {
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(ThreadDumpAction.class.getClassLoader(), "com/almworks/feedback/message");

  private static final Comparator<Thread> THREADS_BY_NAME = new Comparator<Thread>() {
    @Override
    public int compare(Thread o1, Thread o2) {
      if (o1 == o2)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  };

  public ThreadDumpAction() {
    super(I18N.getFactory("threadDump.action.name"), null);
    watchRole(DialogManager.ROLE);
  }

  public static void createReportHeader(StringBuilder result, ProductInformation info) {
    result.append(Local.text(Terms.key_Deskzilla)).append(" Error Report\n");
    result.append("Build: ").append(info.getBuildNumber()).append("\n");
    result.append("Java:");
    prop(result, "java.version");
    prop(result, "java.vm.version");
    prop(result, "java.vm.vendor");
    prop(result, "java.vm.name");
    result.append('\n');
    result.append("OS:");
    prop(result, "os.name");
    prop(result, "os.version");
    prop(result, "sun.os.patch.level");
    result.append(',');
    prop(result, "os.arch");
    prop(result, "sun.arch.data.model");
    prop(result, "sun.cpu.endian");
    Runtime runtime = Runtime.getRuntime();
    result.append(", ").append(runtime.availableProcessors()).append(" cpu");
    result.append('\n');
    long total = runtime.totalMemory();
    result.append("Heap: ").append(FileUtil.getMemoryMegs(total)).append("m, ");
    result.append((int) (100L * (total - runtime.freeMemory() + 1) / (Math.abs(total) + 1)));
    result.append("% used, ").append(FileUtil.getMemoryMegs(runtime.maxMemory())).append("m max");
    result.append('\n');
    result.append("Env:");
    prop(result, "user.country");
    prop(result, "user.language", '/');
    appendTimezone(result);

    prop(result, "file.encoding");
    result.append('\n');
    result.append("Assertions: ");
    boolean assertions = false;
    assert assertions = true;
    result.append(assertions ? "on" : "off");
    result.append('\n');
    result.append('\n');
  }

  private static void appendTimezone(StringBuilder result) {
    TimeZone tz = TimeZone.getDefault();
    assert tz != null;
    Date now = new Date();
    boolean daylight = tz.inDaylightTime(now);
    result.append(' ').append(tz.getDisplayName(daylight, TimeZone.SHORT));
    int off = tz.getOffset(now.getTime());
    int hroff = off / 3600000;
    int minoff = (off % 3600000) / 60000;
    StringBuffer offset = new StringBuffer(hroff < 0 ? "-" : "+");
    hroff = Math.abs(hroff);
    if (hroff < 10)
      offset.append('0');
    offset.append(hroff);
    if (minoff < 10)
      offset.append('0');
    offset.append(minoff);
    result.append(' ').append(offset);
  }

  private static void prop(StringBuilder result, String key) {
    prop(result, key, ' ');
  }

  private static void prop(StringBuilder result, String key, char delim) {
    String prop = System.getProperty(key);
    if (prop != null) {
      result.append(delim).append(prop);
    }
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.getSourceObject(DialogManager.ROLE);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    DialogManager manager = context.getSourceObject(DialogManager.ROLE);
    ProductInformation productInfo = null;
    try {
      productInfo = context.getSourceObject(ProductInformation.ROLE);
    } catch (CantPerformException e) {
      productInfo = null;
    }
    dumpThreads(manager, productInfo);
  }

  private void dumpThreads(final DialogManager manager, final ProductInformation productInfo) throws CantPerformException {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          LogHelper.warning(e);
        }
        final String dump = prepareDump(productInfo);
        LogHelper.debug("Thread dump", dump);
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            showDump(manager, dump);
          }
        });
      }
    }, "Collect Thread dump").start();
  }

  private void showDump(DialogManager manager, String dump) {
    DialogBuilder builder = manager.createBuilder("threadDump");
    builder.setWindowPositioner(WindowUtil.WindowPositioner.CENTER);
    builder.setTitle(I18N.getString("threadDump.window.title"));
    builder.setEmptyOkAction();
    final JTextArea view = new JTextArea();
    view.setText(dump);
    view.setColumns(80);
    view.setRows(60);
    builder.setContent(new JScrollPane(view));
    JButton copyButton = new JButton(I18N.getString("threadDump.window.copyToClipboard.name"));
    JPanel bottomPanel = new JPanel(new BorderLayout());
    bottomPanel.add(copyButton, BorderLayout.WEST);
    copyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        UIUtil.copyToClipboard(view.getText());
      }
    });
    builder.setBottomLineComponent(bottomPanel);
    UIUtil.scrollToTop(view);
    builder.setModal(true);
    builder.showWindow();
  }

  private String prepareDump(ProductInformation productInfo) {
    Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
    ArrayList<Thread> threads = Collections15.arrayList(traces.keySet());
    Collections.sort(threads, THREADS_BY_NAME);
    StringBuilder dump = new StringBuilder();
    if (productInfo != null) createReportHeader(dump, productInfo);
    else dump.append("Missing product info");
    dump.append("\n\n");

    for (Thread thread : threads) {
      dump.append(thread.toString()).append(" state:").append(thread.getState()).append("\n");
      StackTraceElement[] elements = traces.get(thread);
      if (elements == null) {
        dump.append("\n");
        LogHelper.error("Missing trace for thread", thread);
        continue;
      }
      for (StackTraceElement element : elements) dump.append("  ").append(element).append("\n");
      dump.append("\n");
    }
    return dump.toString();
  }
}
