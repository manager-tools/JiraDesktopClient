package com.almworks.application;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.viewer.textdecorator.TextDecoration;
import com.almworks.api.application.viewer.textdecorator.TextDecorationParser;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.picocontainer.Startable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextDecoratorRegistryImpl implements TextDecoratorRegistry, Startable {
  private static final Comparator<MyLinkArea> AREA_START = new Comparator<MyLinkArea>() {
    public int compare(MyLinkArea o1, MyLinkArea o2) {
      int offset1 = o1.getOffset();
      int offset2 = o2.getOffset();
      if (offset1 != offset2)
        return offset1 - offset2;
      return o2.getLength() - o1.getLength();
    }
  };
  
  private final List<TextDecorationParser> myParsers = new CopyOnWriteArrayList<TextDecorationParser>();

  public void addParser(TextDecorationParser parser) {
    myParsers.add(parser);
  }

  public Collection<? extends TextDecoration> processText(String text) {
    if (myParsers.isEmpty())
      return Collections15.emptyList();
    MyContext[] processed = new MyContext[myParsers.size()];
    for (int i = 0; i < myParsers.size(); i++) {
      TextDecorationParser parser = myParsers.get(i);
      MyContext context = new MyContext(text);
      processed[i] = context;
      parser.decorate(context);
    }
    List<MyLinkArea> allLinks = Collections15.arrayList();
    for (MyContext context : processed) {
      allLinks.addAll(context.myLinks);
    }
    if (allLinks.isEmpty())
      return Collections15.emptyList();
    Collections.sort(allLinks, AREA_START);
    MyLinkArea prev = null;
    for (Iterator<MyLinkArea> iterator = allLinks.iterator(); iterator.hasNext();) {
      MyLinkArea area = iterator.next();
      if (prev != null) {
        if (area.getOffset() < prev.getEnd()) {
          iterator.remove();
          continue;
        }
      }
      prev = area;
    }
    return allLinks;
  }

  public void start() {
    addParser(new URLParser());
  }

  public void stop() {

  }

  private static class MyContext implements TextDecorationParser.Context {
    private final String myText;
    private final List<MyLinkArea> myLinks = Collections15.arrayList();

    public MyContext(String text) {
      myText = text;
    }

    public String getText() {
      return myText;
    }

    public TextDecorationParser.LinkArea addLink(int offset, int length) {
      MyLinkArea area = new MyLinkArea(myText, offset, length);
      myLinks.add(area);
      return area;
    }

    public TextDecorationParser.LinkArea addLink(Matcher matcher) {
      final int offset = matcher.start();
      return addLink(offset, matcher.end() - offset);
    }
  }

  private static class MyLinkArea implements TextDecorationParser.LinkArea, TextDecoration {
    private final String myText;
    private final int myOffset;
    private final int myLength;
    private AnAction myDefaultAction;
    private final List<AnAction> myActions = Collections15.arrayList();

    public MyLinkArea(String text, int offset, int length) {
      myText = text;
      myOffset = offset;
      myLength = length;
    }

    public void setDefaultAction(AnAction action) {
      myDefaultAction = action;
    }

    public void addActions(AnAction... actions) {
      myActions.addAll(Arrays.asList(actions));
    }

    public int getEnd() {
      return myOffset + myLength;
    }

    public int getOffset() {
      return myOffset;
    }

    public int getLength() {
      return myLength;
    }

    public AnAction getDefaultAction() {
      return myDefaultAction;
    }

    public List<AnAction> getNotDefaultActions() {
      return Collections.unmodifiableList(myActions);
    }

    public int getEndOffset() {
      return myOffset + myLength;
    }

    public String getText() {
      return myText.substring(myOffset, getEndOffset());
    }
  }

  private static class URLParser implements TextDecorationParser {
    private static final Pattern PROTOCOL_HOST_PATH = Pattern.compile(
      "\\w{3,}://[\\w\\-_]+(\\.[\\w\\-_]+)*(:\\d+)?(?:(?:/\\S+)*/?)");

    private static final Pattern NO_PROTOCOL_HOST_PATH_1 = Pattern.compile(
      "(www|ftp)(\\.[\\w\\-_]+)+(?:(?:/\\S+)*/?)");

    private static final Pattern EMAIL = Pattern.compile("(mailto:\\s*)?([\\w\\-\\+_.]+@([\\w\\-_]+\\.)+[a-zA-Z]+)");

    public void decorate(Context context) {
      collectUrls(context, PROTOCOL_HOST_PATH);
      collectUrls(context, NO_PROTOCOL_HOST_PATH_1);
      collectEMails(context, EMAIL);
    }

    private void collectUrls(Context context, Pattern pattern) {
      final Matcher matcher = pattern.matcher(context.getText());
      while (matcher.find()) {
        final LinkArea link = addStrippedLink(context, matcher);
        link.setDefaultAction(new ClickLinkAction(link.getText()));
      }
    }

    public TextDecorationParser.LinkArea addStrippedLink(Context context, Matcher matcher) {
      final String text = context.getText();
      final int offset = matcher.start();
      final int end = matcher.end() - 1;

      int poffset = 0; // punctuation offset at the end
      while(".,;:?!()[]'\"<>".indexOf(text.charAt(end - poffset)) >= 0) {
        poffset++;
      }

      return context.addLink(offset, matcher.end() - offset - poffset);
    }

    private void collectEMails(Context context, Pattern pattern) {
      final Matcher m = pattern.matcher(context.getText());
      while (m.find()) {
        final String email = m.group(2);
        context.addLink(m).setDefaultAction(new ClickLinkAction("mailto:" + email));
      }
    }
  }

  private static class ClickLinkAction extends SimpleAction {
    private final String myLink;

    private ClickLinkAction(String link) {
      super("Open " + link);
      myLink = link;
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      ConnectionManager connectionManager = context.getSourceObject(Engine.ROLE).getConnectionManager();
      Connection connection = connectionManager.getConnectionForUrl(myLink);
      if (connection == null) {
        openExternal();
        return;
      }
      ItemSource itemSource = connection.getItemSourceForUrls(Collections.singleton(myLink));
      if (itemSource == null) {
        openExternal();
        return;
      }
      ItemCollectionContext itemContext = ItemCollectionContext.createGeneral(myLink, connection);
      context.getSourceObject(ExplorerComponent.ROLE).showItemsInTab(itemSource, itemContext, true);
    }

    private void openExternal() {
      ExternalBrowser.openURL(myLink, true);
    }
  }
}
