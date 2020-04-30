package com.almworks.explorer;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

public class PrimaryItemKeyTransferHandler extends TransferHandler {
  private static PrimaryItemKeyTransferHandler SINGLE = new PrimaryItemKeyTransferHandler(true);
  private static PrimaryItemKeyTransferHandler MULTIPLE = new PrimaryItemKeyTransferHandler(false);

  private final boolean mySingleKey;

  private PrimaryItemKeyTransferHandler(boolean singleKey) {
    mySingleKey = singleKey;
  }

  public static PrimaryItemKeyTransferHandler getInstance(boolean singleKey) {
    return singleKey ? PrimaryItemKeyTransferHandler.SINGLE : PrimaryItemKeyTransferHandler.MULTIPLE;
  }

  public int getSourceActions(JComponent c) {
    return COPY;
  }

  protected Transferable createTransferable(JComponent comp) {
    String text = ((JTextComponent) comp).getSelectedText();
    return text == null || text.length() == 0 ? null : new StringSelection(text);
  }

  protected void exportDone(JComponent source, Transferable data, int action) {
  }

  public boolean importData(JComponent comp, Transferable t) {
    JTextComponent c = (JTextComponent) comp;
    List<ItemWrapper> wrappers = ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getDataOrNull(t);
    if (wrappers != null) {
      return mySingleKey ? importOneArtifact(c, wrappers) : importManyArtifacts(c, wrappers);
    } else {
      try {
        return importString(c, (String) t.getTransferData(DataFlavor.stringFlavor));
      } catch (UnsupportedFlavorException e) {
        // ignore
      } catch (IOException e) {
        // ignore
      }
    }
    return false;
  }

  private boolean importManyArtifacts(JTextComponent c, List<ItemWrapper> wrappers) {
    try {
      Document document = c.getDocument();
      int length = document.getLength();
      int insert = 0;
      String text = null;
      if (length > 0) {
        text = document.getText(0, length);
        int i;
        for (i = text.length() - 1; i >= 0; i--) {
          char ch = text.charAt(i);
          if (ch != ',' && ch != ';' && !Character.isWhitespace(ch))
            break;
        }
        insert = i + 1;
      }
      String keys = buildKeys(wrappers, text);
      if (keys.isEmpty()) {
        return false;
      }
      if (insert < length) {
        document.remove(insert, length - insert);
      }
      if (insert > 0) {
        keys = ", " + keys;
      }
      document.insertString(insert, keys, new SimpleAttributeSet());
      return true;
    } catch (BadLocationException e) {
      Log.error(e);
      return false;
    }
  }

  private boolean importOneArtifact(JTextComponent c, List<ItemWrapper> wrappers) {
    if(wrappers.size() != 1) {
      return false;
    }

    final String key = buildKeys(Collections15.list(wrappers.get(0)), null);
    if(key.isEmpty()) {
      return false;
    }

    c.setText(key);
    return true;
  }

  @NotNull
  private static String buildKeys(List<ItemWrapper> wrappers, @Nullable String text) {
    Set<String> existing =
      text == null ? Collections15.<String>hashSet() : Collections15.hashSet(text.split("[,;\\s]+"));
    StringBuilder buffer = new StringBuilder();
    for (ItemWrapper wrapper : wrappers) {
      Connection connection = wrapper.getConnection();
      if (connection == null) continue;
      String key = connection.getDisplayableItemId(wrapper);
      if (key != null && key.length() > 0) {
        if (existing.add(key)) {
          if (buffer.length() > 0)
            buffer.append(", ");
          buffer.append(key);
        }
      }
    }
    return buffer.toString();
  }

  private boolean importString(JTextComponent c, String s) {
    int startPosition = c.getSelectionStart();
    int endPosition = c.getSelectionEnd();
    int length = endPosition - startPosition;
    EditorKit kit = c.getUI().getEditorKit(c);
    Document doc = c.getDocument();
    if (length > 0) {
      try {
        doc.remove(startPosition, length);
      } catch (BadLocationException e) {
        Log.error(e);
      }
    }
    Reader in = null;
    try {
      in = new StringReader(s);
      kit.read(in, doc, startPosition);
      return true;
    } catch (IOException e) {
      Log.error(e);
    } catch (BadLocationException e) {
      Log.error(e);
    } finally {
      IOUtils.closeReaderIgnoreExceptions(in);
    }
    return false;
  }

  public boolean canImport(JComponent comp, DataFlavor[] flavors) {
    JTextComponent c = (JTextComponent) comp;
    if (!c.isEnabled() || !c.isEditable())
      return false;

    for (DataFlavor flavor : flavors) {
      if (DataFlavor.stringFlavor.equals(flavor) ||
        ItemWrappersTransferrable.ARTIFACTS_FLAVOR.getFlavor().equals(flavor))
        return true;
    }

    return false;
  }
}
