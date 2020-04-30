package com.almworks.engine.gui.attachments.content;

import com.almworks.api.config.MiscConfig;
import com.almworks.engine.gui.attachments.AttachmentUtils;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.IntArray;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.AToolbarButton;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.recent.UnwrapCombo;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.text.TextUtil;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attachment content view implements graphical representation of textual attachment
 * with search capabilities
 * I made this search functionality looking on a QuryTitle2 and TableControllerImpl
 */
public class TextContentView implements ContentView {
  private static final String WRAP_SETTING = "lineWrap";

  private final Pattern EMPTY_PATTERN = Pattern.compile("");
  private final JPanel myWholePanel = new JPanel();
  private final JTextArea myTextArea;
  private final TextSearchComponents mySearchComponents;
  private final AComboBox<Charset> myEncoding;
  private final byte[] myContentBytes;

  private Bottleneck myHighlightBottleneck;
  private Pattern mySearchPattern;

  private int myLastSearchPos = 0;
  private String myLastMatch = "";
  private final Highlighter.HighlightPainter myHighlightPainter = new SearchHighlightPainter(Color.YELLOW);
  private final Highlighter.HighlightPainter myActiveHighlightPainter = new SearchHighlightPainter(Color.GREEN);
  private Occurencies myLastOccurencies;
  private final JCheckBox myWrapCB = new JCheckBox("Wrap text");

  public TextContentView(JTextArea component, byte[] contentBytes) {
    myTextArea = component;
    myWholePanel.setLayout(Aqua.isAqua() ? new BorderLayout() : new BorderLayout(5, 5));
    mySearchComponents = new TextSearchComponents();
    myEncoding = new AComboBox<Charset>();
    myWholePanel.add(createToolBox(), BorderLayout.NORTH);

    myContentBytes = contentBytes;
    JScrollPane scrollPane = new JScrollPane(myTextArea);
    scrollPane.setOpaque(true);
    scrollPane.setBackground(UIUtil.getEditorBackground());
    myWholePanel.add(scrollPane, BorderLayout.CENTER);
    setupSearchActions();
    UIUtil.setInitialFocus(myTextArea);
    Aqua.cleanScrollPaneBorder(scrollPane);
    Aqua.cleanScrollPaneResizeCorner(scrollPane);
  }

  private JPanel createToolBox() {

    JPanel panel = new JPanel();
    FormLayout layout = new FormLayout("p, 3dlu, p:g, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout, panel);
    builder.append(mySearchComponents.mySearchLabel, mySearchComponents.mySearchText);
    builder.append(mySearchComponents.myNext, mySearchComponents.myPrev);
    builder.append(mySearchComponents.myCaseSensitive, mySearchComponents.myHighlightAll, mySearchComponents.myRegexp);

    builder.nextLine();
    builder.append("&Encoding:", myEncoding);
    builder.append(createWrapControl(), 5);

    panel.setOpaque(false);
    if (Aqua.isAqua()) {
      panel.setBorder(UIUtil.getCompoundBorder(Aqua.MAC_BORDER_SOUTH, UIUtil.BORDER_5));
    } else {
      panel.setBorder(BorderFactory.createEmptyBorder());
    }
    return panel;
  }

  private JComponent createWrapControl() {
    myTextArea.setWrapStyleWord(true);
    myWrapCB.setMnemonic('w');
    UIUtil.addChangeListener(Lifespan.FOREVER, new ChangeListener() {
      @Override
      public void onChange() {
        myTextArea.setLineWrap(myWrapCB.isSelected());
      }
    }, myWrapCB.getModel());
    return myWrapCB;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  /**
   * Setting up Bottleneck for highlighting occurencies,
   * and listener which perform Bottleneck request
   * on any criteria change. Also sets keybinding for search panel
   */
  private void setupSearchActions() {
/*    KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK + Event.SHIFT_MASK);
    InputMap im = myTextArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    im.put(key, "showSearchPanel");
    ActionMap am = myTextArea.getActionMap();
    am.put("showSearchPanel", new AbstractAction("showSearchPanel") {
      public void actionPerformed(ActionEvent e) {
        mySearchPanel.setVisible(true);
      }
    });*/

    myHighlightBottleneck = new Bottleneck(100, ThreadGate.AWT, new Runnable() {
      public void run() {
        boolean smthFound = onSearchPatternChanged(mySearchComponents.getPattern(), mySearchComponents.getRegexp(),
          mySearchComponents.getCaseSensitive(), mySearchComponents.getHighlightAll());
        mySearchComponents.setHighlightBackground(smthFound ? Color.WHITE : Color.PINK);
      }
    });

    InputMap im = myTextArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap am = myTextArea.getActionMap();
    Action forward = new SearchAction(mySearchComponents.myNext);
    Action backward = new SearchAction(mySearchComponents.myPrev);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), forward);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), forward);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), backward);
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), backward);
    am.put(forward, forward);
    am.put(backward, backward);
  }

  /**
   * Perform a Pattern preparation and calls incremental search
   *
   * @param str           Search criterion (no mater string or regular expression)
   * @param regexp        If true specified criterion is regular expression
   * @param caseSensitive If true search should be performed in case sensitive manner
   * @param highlightAll  if true all occurencies will be hoghlighted during search
   * @return true if any match found or if no pattern specified, false in other cases
   */
  private boolean onSearchPatternChanged(String str, boolean regexp, boolean caseSensitive, boolean highlightAll) {
    boolean active = str.length() > 0;

    int attr = 0;
    attr |= (caseSensitive) ? 0 : Pattern.CASE_INSENSITIVE;
    attr |= (regexp) ? 0 : Pattern.LITERAL;

    Pattern pattern = null;
    try {
      pattern = active ? Pattern.compile(str, attr) : EMPTY_PATTERN;
    } catch (Exception e) {
      // ignore
    }

    if (pattern != null)
      mySearchPattern = pattern;
    else
      pattern = mySearchPattern;

    invalidateOccurencies();
    if (highlightAll)
      myLastOccurencies = getAllOccurencies();

    if (pattern != null) {
      int result;
      result = searchForward();
      return result >= 0 ? true : false;
    }
    return true;
  }

  private void invalidateOccurencies() {
    myLastOccurencies = null;
  }

  public void copyToClipboard() {
    UIUtil.copyToClipboard(myTextArea.getText());
  }

  private int searchForward() {
    myLastSearchPos = 0;
    return search(false, true);
  }

  private int searchNext() {
    return search(true, true);
  }

  private int searchPrev() {
    return search(true, false);
  }

  private int searchNextFromStart() {
    myLastSearchPos = 0;
    return searchForward();
  }

  private int search(boolean skipLast, boolean forward) {
    if (mySearchPattern == null || mySearchPattern.pattern().trim().equals("")) {
      myLastMatch = "";
      myLastSearchPos = 0;
      removeHighlights();
      invalidateOccurencies();
      return 0;
    }

    int startPos = getStartPos(skipLast, forward);
    int begin = -1;
    int end = -1;
    int[] bounds = new int[] {begin, end};
    if (myLastOccurencies != null) {
      bounds = myLastOccurencies.getOccurenceByPosition(startPos, forward);
    } else {
      bounds = getUncachedOccurence(startPos, forward);
    }
    begin = bounds[0];
    end = bounds[1];
    if (begin >= 0 && end >= 0) {
      String match = null;
      try {
        match = myTextArea.getDocument().getText(begin, end - begin);
      } catch (BadLocationException e) {
        // Ignore
      }
      myTextArea.setCaretPosition(begin);
      scrollToVisible(begin, end);
      myLastSearchPos = begin;
      myLastMatch = match;
      addHighlights(myLastOccurencies, begin, end);
    }
    return begin;
  }

  private void scrollToVisible(int begin, int end) {
    try {
      Rectangle r1 = myTextArea.modelToView(begin);
      Rectangle r2 = myTextArea.modelToView(end);
      Rectangle r;
      if (r1.y == r2.y) {
        r = new Rectangle(r1.x, r1.y, r2.x - r1.x + r2.width, r2.height);
      } else {
        Dimension sz = myTextArea.getSize();
        r = new Rectangle(r1.x, r1.y, sz.width - r1.x, r1.height);
      }
      myTextArea.scrollRectToVisible(r);
    } catch (BadLocationException e) {
      // ignore
    }
  }

  /**
   * Perform search in both direction without caching rsults
   *
   * @param pos     Character position which represent start or end of searchable segment
   * @param forward Direction or search. If true search forward, else backward
   * @return Array of two elements. Start and end position of occurence. Returns -1,-1 if no occurence found
   */
  private int[] getUncachedOccurence(int pos, boolean forward) {
    Document doc = myTextArea.getDocument();
    int end;
    int start;
    if (forward) {
      start = pos;
      end = doc.getLength();
    } else {
      start = 0;
      end = pos;
    }
    SearchableSegment seg = new SearchableSegment();
    Pattern regexp = mySearchPattern;
    int[] bounds = new int[] {-1, -1};
    while (start < end) {
      try {
        doc.getText(start, end - start, seg);
        Matcher matcher = regexp.matcher(seg);
        while (matcher.find()) {
          if (matcher.end() - matcher.start() > 0)
            bounds = new int[] {matcher.start() + seg.offset, matcher.end() + seg.offset};
          if (forward && bounds[0] >= 0)
            return bounds;
        }
        start += seg.count;
      } catch (BadLocationException e) {
        // Ignore
      }
    }
    return bounds;
  }

  /**
   * Highlights all occurencies in text area.
   *
   * @param occurencies Object represents cached occurencies
   * @param selStart    Start character index of currently found occurence
   * @param selEnd      End character index of currently found occurence
   */
  private void addHighlights(Occurencies occurencies, Integer selStart, Integer selEnd) {
    removeHighlights();
    Highlighter.HighlightPainter painter;
    Highlighter hilite = myTextArea.getHighlighter();
    if (occurencies != null) {
      painter = myHighlightPainter;
      for (int i = 0; i < occurencies.count(); i++) {
        try {
          int[] bounds = occurencies.getOccurence(i);
          if (selStart != null && selStart == bounds[0])
            continue;
          hilite.addHighlight(bounds[0], bounds[1], painter);
        } catch (Exception e) {
          // Ignore
        }
      }
    }
    if (selStart != null) {
      painter = myActiveHighlightPainter;
      try {
        hilite.addHighlight(selStart, selEnd, painter);
      } catch (BadLocationException e) {
        // Ignore
      }
    }
  }

  /**
   * Calculates the position from which search will begun,
   * depending on last search position and passed parameters
   *
   * @param skipLast is last matched string should be ignored
   * @param forward  true for searching forward, false for backward
   * @return starting position for search
   */
  private int getStartPos(boolean skipLast, boolean forward) {
    int docLen = myTextArea.getDocument().getLength();
    int startPos = myLastSearchPos;
    if (skipLast)
      startPos += forward ? myLastMatch.length() : -myLastMatch.length();
    if (startPos < 0)
      startPos = 0;
    if (startPos > docLen)
      startPos = docLen;
    return startPos;
  }

  /**
   * Search in TextArea for all pattern occurencies,
   * using Segment (prevents gigant strings to be copied)
   *
   * @return Object represents cached occurencies
   */
  private Occurencies getAllOccurencies() {
    Occurencies occurencies = new Occurencies();
    int startPos = 0;
    Document doc = myTextArea.getDocument();
    int docLen = doc.getLength();
    SearchableSegment seg = new SearchableSegment();
    Pattern regexp = mySearchPattern;
    while (startPos < docLen) {
      try {
        doc.getText(startPos, docLen - startPos, seg);
        Matcher matcher = regexp.matcher(seg);
        while (matcher.find())
          if (matcher.end() - matcher.start() > 0)
            occurencies.add(matcher.start() + seg.offset, matcher.end() + seg.offset);
        startPos += seg.count;
      } catch (BadLocationException e) {
        // Ignore
      }
    }
    return occurencies;
  }

  private void removeHighlights() {
    Highlighter hilite = myTextArea.getHighlighter();
    Highlighter.Highlight[] hilites = hilite.getHighlights();
    for (int i = 0; i < hilites.length; i++) {
      if (hilites[i].getPainter() instanceof SearchHighlightPainter)
        hilite.removeHighlight(hilites[i]);
    }
  }

  public void init(final Lifespan life, final Configuration config, MiscConfig miscConfig) {
    Procedure<Charset> applyEncoding = new Procedure<Charset>() {
      @Override
      public void invoke(Charset charset) {
        TextUtil.setTextAndPreserveCaret(myTextArea, new String(myContentBytes, charset));
      }
    };
    AttachmentUtils.configureEncodingCombo(life, myEncoding, config, miscConfig, applyEncoding);
    applyEncoding.invoke(UnwrapCombo.<Charset>getUnwrapSelected(myEncoding.getModel()));

    UIUtil.setCaretAlwaysVisible(life, myTextArea);

    final Configuration textWrapConfig = miscConfig.getConfig("attachmentsTextViewer");
    final boolean wrap = textWrapConfig.getBooleanSetting(WRAP_SETTING, false);
    myWrapCB.setSelected(wrap);
    UIUtil.addChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        textWrapConfig.setSetting(WRAP_SETTING, myWrapCB.isSelected());
      }
    }, myWrapCB.getModel());
  }

  public class TextSearchComponents implements ActionListener {
    private final JLabel mySearchLabel;
    private final JTextField mySearchText;
    private final JButton myNext;
    private final JButton myPrev;
    private final JCheckBox myHighlightAll;
    private final JCheckBox myRegexp;
    private final JCheckBox myCaseSensitive;

    public TextSearchComponents() {

      mySearchText = new JTextField(15);
      mySearchText.setOpaque(true);

      myHighlightAll = new JCheckBox("Highlight all");
      myHighlightAll.setMnemonic('h');
      myHighlightAll.setFocusable(false);
      myHighlightAll.addActionListener(this);
      myHighlightAll.setOpaque(false);

      myCaseSensitive = new JCheckBox("Case sensitive");
      myCaseSensitive.setMnemonic('s');
      myCaseSensitive.setFocusable(false);
      myCaseSensitive.addActionListener(this);
      myCaseSensitive.setOpaque(false);

      myRegexp = new JCheckBox("Regexp");
      myRegexp.setMnemonic('r');
      myRegexp.setFocusable(false);
      myRegexp.addActionListener(this);
      myRegexp.setOpaque(false);

      myPrev = new AToolbarButton();
      myPrev.setIcon(Icons.ARROW_UP);
      myPrev.setMnemonic(KeyEvent.VK_UP);
      myPrev.setFocusable(false);
      myPrev.addActionListener(this);
      myPrev.setOpaque(false);
      myPrev.setToolTipText("Find Previous (shift F3, Up Arrow)");

      myNext = new AToolbarButton();
      myNext.setIcon(Icons.ARROW_DOWN);
      myNext.setMnemonic(KeyEvent.VK_DOWN);
      myNext.setFocusable(false);
      myNext.addActionListener(this);
      myNext.setOpaque(false);
      myNext.setToolTipText("Find Next (F3, Down Arrow)");

      mySearchLabel = new JLabel();
      NameMnemonic.parseString("&Find:").setToLabel(mySearchLabel);
      mySearchLabel.setLabelFor(mySearchText);

      addPatternChangeListener(new DocumentAdapter() {
        protected void documentChanged(DocumentEvent e) {
          fireChange();
        }
      });
    }

    private void addPatternChangeListener(DocumentListener ds) {
      DocumentUtil.addListener(Lifespan.FOREVER, mySearchText.getDocument(), ds);
    }

    public void clear() {
      mySearchText.setText("");
    }

    public void actionPerformed(ActionEvent e) {
      Object source = e.getSource();
      if (source == myPrev) {
        searchPrev();
      } else if (source == myNext) {
        searchNext();
      } else if (source == myCaseSensitive || source == myRegexp || source == myHighlightAll) {
        fireChange();
      }
    }

    public void fireChange() {
      myHighlightBottleneck.request();
    }

    public void setPattern(String pattern, boolean caseSensitive, boolean regexp, boolean filterMatched) {
      pattern = Util.NN(pattern);
      if (!pattern.equals(getPattern())) {
        mySearchText.setText(pattern);
      }
      myCaseSensitive.setSelected(caseSensitive);
      myRegexp.setSelected(regexp);
    }

    public void setHighlightBackground(Color color) {
      mySearchText.setBackground(color);
    }

    public String getPattern() {
      if (mySearchText.getDocument() != null)
        return mySearchText.getText().trim();
      else
        return "";
    }

    public boolean getRegexp() {
      return myRegexp.isSelected();
    }

    public boolean getCaseSensitive() {
      return myCaseSensitive.isSelected();
    }

    public boolean getHighlightAll() {
      return myHighlightAll.isSelected();
    }
  }


  /*
   * Custom HighlightPainter class, which makes removing highlights
   * more carefull and easier
   */
  class SearchHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

    /**
     * Constructs a new highlight painter. If <code>c</code> is null,
     * the JTextComponent will be queried for its selection color.
     *
     * @param c the color for the highlight
     */
    public SearchHighlightPainter(Color c) {
      super(c);
    }
  }


  // Need for searching without copiying CharSequence (like in JDK 1.6)
  class SearchableSegment extends Segment implements CharSequence {

    public char charAt(int index) {
      if (index < 0 || index >= count) {
        throw new StringIndexOutOfBoundsException(index);
      }
      return array[offset + index];
    }

    public int length() {
      return count;
    }

    public CharSequence subSequence(int start, int end) {
      if (start < 0) {
        throw new StringIndexOutOfBoundsException(start);
      }
      if (end > count) {
        throw new StringIndexOutOfBoundsException(end);
      }
      if (start > end) {
        throw new StringIndexOutOfBoundsException(end - start);
      }
      SearchableSegment segment = new SearchableSegment();
      segment.array = this.array;
      segment.offset = this.offset + start;
      segment.count = end - start;
      return segment;
    }
  }


  /**
   * Object represents and store cached searh occurencies
   */
  class Occurencies {
    final private IntArray starts = new IntArray();
    final private IntArray ends = new IntArray();

    public void add(int start, int end) {
      starts.add(start);
      ends.add(end);
    }

    public void sort() {
      starts.sort();
      ends.sort();
    }

    public int count() {
      return starts.size() > ends.size() ? starts.size() : ends.size();
    }

    public int[] getOccurence(int i) {
      return new int[] {starts.get(i), ends.get(i)};
    }

    public int[] getOccurenceByPosition(int startPos, boolean forward) {
      int index = starts.binarySearch(startPos);
      if (!forward && index != 0)
        index--;
      if (index >= count())
        return new int[] {-1, -1};
      return new int[] {starts.get(index), ends.get(index)};
    }
  }


  private class SearchAction extends AbstractAction {
    private final JButton myButton;

    public SearchAction(JButton button) {
      myButton = button;
    }

    public void actionPerformed(ActionEvent e) {
      if (mySearchComponents.getPattern().length() == 0) {
        mySearchComponents.mySearchText.requestFocusInWindow();
      } else {
        myButton.doClick();
      }
    }
  }
}
