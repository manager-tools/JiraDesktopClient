package com.almworks.explorer;

import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.English;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.AToolbarButton;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.RendererHostComponent;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.IdActionProxy;
import com.almworks.util.ui.actions.PresentationMapping;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

/**
 * @author dyoma
 */
class QueryTitle2 extends JPanel  {
  private JComponent myQuery = null;
  private final JLabel myLabel = createLabel(L.html("<html><body><b></b></body></html>")); // show nothing!;
  private final JLabel myCount = createLabel("");
  private final JLabel myInfo = createLabel("");
  private final JComponent myProgressPanel;
  private final JProgressBar myProgress = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
  private final JPanel myErrorsPanel;
  private final List<String> myLastErrors = Collections15.arrayList();
  private final JPanel myTopPanel = new JPanel(UIUtil.createBorderLayout());

  private final TextHighlightPanel myHighlightPanel;
  private final SimpleModifiable myHighlightPanelModifiable = new SimpleModifiable();

  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private Runnable myNextRun;
  private Runnable myPrevRun;

  public QueryTitle2() {
    super(new InlineLayout(InlineLayout.VERTICAL, 4));
    Color background = getDefaultBackground();
    Color foreground = getDefaultForeground();

    setBorder(new EmptyBorder(2, 5, 3, 5));
    setBackground(background);

    final JPanel westPanel = UIUtil.createInlinePanel(
      InlineLayout.HORISONTAL, UIUtil.GAP, true, createFindButton(), myCount);
    westPanel.setOpaque(false);

    myTopPanel.setOpaque(false);
    myTopPanel.add(myLabel, BorderLayout.WEST);
    myTopPanel.add(westPanel, BorderLayout.EAST);

    add(myTopPanel);
    myProgressPanel = createProgressPanel();
    add(myProgressPanel);
    myProgressPanel.setVisible(false);

    myErrorsPanel = createErrorsPanel();
    add(myErrorsPanel);
    myErrorsPanel.setVisible(false);

    myHighlightPanel = new TextHighlightPanel();
    setHighlightPanelVisible(false);

    add(myHighlightPanel.getComponent());
  }

  private AToolbarButton createFindButton() {
    final AToolbarButton button = new AToolbarButton(new IdActionProxy(MainMenu.Search.FIND)) {
      final Border myBorder = new EmptyBorder(2, 2, 2, 2);
      final Insets myMargin = new Insets(0, 0, 0, 0);
      @Override
      public void updateUI() {
        super.updateUI();
        setMargin(myMargin);
        setBorder(myBorder);
      }
    };
    button.overridePresentation(PresentationMapping.VISIBLE_NONAME);
    return button;
  }

  private Color getTitleForeground() {
    Color background = getDefaultBackground();
    Color foreground = getDefaultForeground();
    return ColorUtil.between(foreground, background, 0.2F);
  }

  public JComponent getComponent() {
    return this;
  }

  public void hideLoadingPanel() {
    myProgressPanel.setVisible(false);
  }

  public void updateArtifactCounter(int count) {
    myCount.setText(count + " " + English.getSingularOrPlural(Local.parse(Terms.ref_artifact), count));
  }

  public void showLoadingQuery(ItemCollectionContext info) {
    myInfo.setText("");
    myProgress.setVisible(false);
    myLabel.setText("<html><body><b>Showing:</b></body></html>");
    setQueryInfo(info);
    myCount.setText(L.content(Local.parse("No " + Terms.ref_artifacts + " found")));
    myProgressPanel.setVisible(true);
    revalidate();
  }

  public void setQueryInfo(ItemCollectionContext info) {
    if (myQuery != null)
      myTopPanel.remove(myQuery);
//    if (path == null)
//      myQuery = setupLink(info);
//    else {
      RendererHostComponent component = new RendererHostComponent();
      component.setRenderer(QueryPath.create(info));
      myQuery = component;
//    }
    myQuery.setForeground(getTitleForeground());
    myTopPanel.add(myQuery, BorderLayout.CENTER);
  }

  public void copyStateTo(QueryTitle2 title) {
    title.myInfo.setText(myInfo.getText());
    title.myProgress.setVisible(myProgress.isVisible());
    title.myProgress.setValue(myProgress.getValue());
    title.myLabel.setText(myLabel.getText());
    title.myCount.setText(myCount.getText());
    title.myProgressPanel.setVisible(myProgressPanel.isVisible());
    title.revalidate();
  }

  public void showLoadingMessage(String message) {
    myInfo.setText(message);
  }

  public void showProgress(int percent) {
    assert 0 <= percent && percent <= 100 : percent;
    myProgress.setVisible(true);
    int diff = percent - myProgress.getValue();
    if (diff < 0 || diff >= 2)
      myProgress.setValue(percent);
  }

  public void hideProgress() {
    myProgress.setVisible(false);
  }

  private JComponent createProgressPanel() {
    final JPanel panel = new JPanel(new BorderLayout(6, 0));
    panel.setOpaque(false);

    final JLabel label = createLabel("Loading\u2026");
    final int height = UIUtil.getProgressBarPreferredSize(myProgress).height;
    final Dimension progressSize = new Dimension(20 * label.getFontMetrics(label.getFont()).charWidth('m'), height);
    myProgress.setPreferredSize(progressSize);
    myProgress.setMaximumSize(progressSize);

    final ToolbarBuilder builder = ToolbarBuilder.smallEnabledButtons();

    builder.addComponent(label);
    builder.addSeparator();
//    myProgress.setStringPainted(true);
//    myProgress.setString(label.getText());

    builder.addComponent(myProgress);
    builder.addComponent(Box.createHorizontalStrut(3));
    builder.addAction(MainMenu.Search.STOP_QUERY);
    final JPanel toolbar = builder.createHorizontalPanel();
    toolbar.setOpaque(false);

    panel.add(toolbar, BorderLayout.WEST);
    panel.add(myInfo, BorderLayout.CENTER);

    return panel;
  }

  private JPanel createErrorsPanel() {
    JPanel panel = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 0));
    panel.setOpaque(false);
    //panel.setPreferredSize(new Dimension(0, 0));
    //panel.setBorder(new EmptyBorder(0, 0, 4, 0));
    return panel;
  }

  private static JLabel createLabel(String text) {
    JLabel label = new JLabel(text);
    label.setForeground(getDefaultForeground());
    return label;
  }

  private static Color getDefaultBackground() {
    Color color = UIManager.getColor("HalfInverse.background");
    return color == null ? Color.GRAY : color;
  }

//  private Color getDefaultLinkPressed() {
//    Color color = UIManager.getColor("HalfInverse.Link.pressed");
//    return color == null ? Color.LIGHT_GRAY.brighter() : color;
//  }
//
//  private static Color getDefaultLinkForeground() {
//    Color color = UIManager.getColor(LINK_FOREGROUND_COLOR);
//    return color == null ? Color.LIGHT_GRAY.brighter() : color;
//  }

  private static Color getDefaultForeground() {
    Color color = UIManager.getColor("HalfInverse.foreground");
    return color == null ? Color.WHITE : color;
  }

  public void showErrors(List<String> errors) {
    if (myLastErrors.size() == 0 && (errors == null || errors.size() == 0))
      return;
    if (myLastErrors.equals(errors))
      return;

    myLastErrors.clear();
    if (errors != null)
      myLastErrors.addAll(errors);

    myErrorsPanel.setVisible(false);
    myErrorsPanel.removeAll();

    for (int i = 0; i < myLastErrors.size(); i++) {
      String error = myLastErrors.get(i);
      if (i == 0)
        myErrorsPanel.add(createLabel(L.content("Query result may be inaccurate: ")));
      if (i > 0)
        myErrorsPanel.add(createLabel(", "));
      myErrorsPanel.add(createErrorsLabel(error));
    }

    myErrorsPanel.setVisible(myLastErrors.size() > 0);
  }

  private JLabel createErrorsLabel(String text) {
    JLabel label = new JLabel(text);
    label.setOpaque(false);
    label.setForeground(getErrorColor());
    UIUtil.adjustFont(label, -1, Font.BOLD, false);
    return label;
  }

  private Color getErrorColor() {
    return GlobalColors.ERROR_COLOR;
  }

  public void setHighlightPanelVisible(boolean b) {
    if(isHighlightPanelVisible() != b) {
      myHighlightPanel.setVisible(b);
      myHighlightPanelModifiable.fireChanged();
    }
  }

  public boolean isHighlightPanelVisible() {
    return myHighlightPanel.isVisible();
  }

  public Modifiable getHighlightPanelModifiable() {
    return myHighlightPanelModifiable;
  }

  public void setHighlightBackground(Color color) {
    myHighlightPanel.myHighlightPattern.setBackground(color);
  }

  public String getHighlightText() {
    return myHighlightPanel.myHighlightPattern.getText().trim();
  }

  public boolean getRegexp() {
    return myHighlightPanel.myRegexp.isSelected();
  }

  public boolean getCaseSensitive() {
    return myHighlightPanel.myCaseSensitive.isSelected();
  }

  public boolean getFilterMatched() {
    return myHighlightPanel.myFilterMatched.isSelected();
  }

  public void setNextPrevActions(Runnable nextAction, Runnable prevAction) {
    myNextRun = nextAction;
    myPrevRun = prevAction;
  }

  public void setFilterPattern(String shortName, boolean caseSensitive, boolean regexp, boolean filterMatched) {
    myHighlightPanel.setPattern(shortName, caseSensitive, regexp, filterMatched);
  }

  public class TextHighlightPanel implements UIComponentWrapper2, ActionListener {
    public final JTextField myHighlightPattern;
    public final JCheckBox myRegexp;
    public final JCheckBox myCaseSensitive;
    public final JCheckBox myFilterMatched;

    private final JButton myNext;
    private final JButton myPrev;
    private final AToolbarButton myCancel;

    private Box myWraperPanel;

    public TextHighlightPanel() {
      myHighlightPattern = new JTextField(20);
      if(!Aqua.isAqua()) {
        myHighlightPattern.setOpaque(true);
      }
      Aqua.makeSearchField(myHighlightPattern);
      
      JPanel patternPanel = SingleChildLayout.envelop(
        myHighlightPattern, SingleChildLayout.CONTAINER, SingleChildLayout.PREFERRED,
        SingleChildLayout.CONTAINER, SingleChildLayout.CONTAINER, 0.5f, 0.5f);

      myCaseSensitive = new JCheckBox("Case sensitive");
      myCaseSensitive.setMnemonic('c');
      myCaseSensitive.setFocusable(false);
      myCaseSensitive.addActionListener(this);
      myCaseSensitive.setOpaque(false);

      myFilterMatched = new JCheckBox("Filter");
      myFilterMatched.setFocusable(false);;
      myFilterMatched.setMnemonic('f');
      myFilterMatched.addActionListener(this);
      myFilterMatched.setOpaque(false);

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
      
      myNext = new AToolbarButton();
      myNext.setIcon(Icons.ARROW_DOWN);
      myNext.setMnemonic(KeyEvent.VK_DOWN);
      myNext.setFocusable(false);
      myNext.addActionListener(this);
      myNext.setOpaque(false);

      myCancel = new AToolbarButton();
      myCancel.setIcon(Icons.ACTION_GENERIC_CANCEL_OR_REMOVE);
      myCancel.addActionListener(this);
      myCancel.setFocusable(false);
      myCancel.setOpaque(false);

      myWraperPanel = new Box(BoxLayout.X_AXIS);
//      myWraperPanel.add(myCancel);
      myWraperPanel.add(Box.createHorizontalStrut(3));
      myWraperPanel.add(patternPanel);
      myWraperPanel.add(Box.createHorizontalStrut(3));
      myWraperPanel.add(myNext);
      myWraperPanel.add(myPrev);
      myWraperPanel.add(myCaseSensitive);
      myWraperPanel.add(myRegexp);
      myWraperPanel.add(myFilterMatched);
      myWraperPanel.setOpaque(false);
      myWraperPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

      DocumentUtil.addChangeListener(Lifespan.FOREVER, myHighlightPattern.getDocument(), myModifiable);
    }

    public void clear() {
      myHighlightPattern.setText("");
    }

    public boolean isVisible() {
      return myWraperPanel.isVisible();
    }

    public void setVisible(boolean vis) {
      myWraperPanel.setVisible(vis);
      if (!vis) {
        clear();
      } else {
        myHighlightPattern.requestFocusInWindow();
      }
    }

    public JComponent getComponent() {
      return myWraperPanel;
    }

    @Deprecated
    public void dispose() {}

    public Detach getDetach() {
      return Detach.NOTHING;
    }

    public void actionPerformed(ActionEvent e) {
      Object source = e.getSource();
      if (source == myPrev) {
        if (myPrevRun != null)
          myPrevRun.run();
      } else if (source == myNext) {
        if (myNextRun != null)
          myNextRun.run();
      } else if (source == myCancel) {
        setHighlightPanelVisible(false);
      } else if (source == myCaseSensitive || source == myRegexp || source == myFilterMatched) {
        fireChange();
      }
    }

    public void fireChange() {
      myModifiable.fireChanged();
    }

    public void setPattern(String pattern, boolean caseSensitive, boolean regexp, boolean filterMatched) {
      pattern = Util.NN(pattern);
      if (!pattern.equals(myHighlightPattern.getText())) {
        myHighlightPattern.setText(pattern);
      }
      myCaseSensitive.setSelected(caseSensitive);
      myRegexp.setSelected(regexp);
      myFilterMatched.setSelected(filterMatched);
    }
  }

  public void addHighlightKeyListener(KeyListener l) {
    myHighlightPanel.myHighlightPattern.addKeyListener(l);
  }

  public Modifiable getFilterParamModifiable() {
    return myModifiable;
  }
}
