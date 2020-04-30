package com.almworks.tags;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.application.tree.TagNode;
import com.almworks.api.application.tree.TreeNodeFactory;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.ProviderDisabledException;
import com.almworks.explorer.ItemUrlServiceImpl;
import com.almworks.explorer.tree.TagEditor;
import com.almworks.explorer.tree.TagsFolderNode;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterable;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.dp.DPIntersects;
import com.almworks.util.Pair;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Functional;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.tags.TagFileStorage;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.almworks.util.collections.Containers.constF;
import static com.almworks.util.collections.Functional.first;
import static org.almworks.util.Collections15.*;

public class TagsImporter {
  private final File myFile;

  private final RootNode myRootNode;
  private final TagsFolderNode myTagsFolder;
  private final ConnectionManager myConnectionManager;
  private final Database myDb;

  private final Progress myProgress;
  private final Procedure<String> myOnFatalError;

  private final Function<String, Boolean> myConfirmMergeTag;

  // confined to AWT thread
  private final List<Pair<ItemSource, TaggedItemsCollector>> myTaggedItemSources = arrayList();

  private TagsImporter(File file, RootNode rootNode, TagsFolderNode tagsFolder, ConnectionManager connectionManager, Database db, Function<String, Boolean> confirmMergeTag, Progress progress,
    Procedure<String> onFatalError)
  {
    myFile = file;
    myRootNode = rootNode;
    myTagsFolder = tagsFolder;
    myConnectionManager = connectionManager;
    myDb = db;
    myConfirmMergeTag = confirmMergeTag;
    myProgress = progress;
    myOnFatalError = onFatalError;
  }

  public static TagsImporter create(File file, Progress progress, ActionContext context, Function<String, Boolean> confirmMergeTag, Procedure<String> onFatalError) throws CantPerformException {
    Engine engine = context.getSourceObject(Engine.ROLE);
    Database db = context.getSourceObject(Database.ROLE);
    RootNode rootNode = CantPerformException.ensureNotNull(context.getSourceObject(ExplorerComponent.ROLE).getRootNode());
    return create(file, progress, rootNode, engine, db, confirmMergeTag, onFatalError);
  }

  @ThreadAWT
  public static TagsImporter create(File tagsFile, Progress progress, @NotNull RootNode rootNode, Engine engine, Database db, Function<String, Boolean> confirmMergeTag, Procedure<String> onFatalError) {
    TagsFolderNode tagsFolder = TagEditor.getTagsFolder(rootNode);
    return new TagsImporter(tagsFile, rootNode, tagsFolder, engine.getConnectionManager(), db, confirmMergeTag, progress, onFatalError);
  }

  public void run() {
    ThreadGate.LONG.execute(new Runnable() {
      @Override
      public void run() {
        try {
          myProgress.setActivity("Reading tags from file");
          final List<TagFileStorage.TagInfo> tags = TagFileStorage.read(myFile);
          final LongList existingEmptyTags = retrieveEmptyTags();
          ThreadGate.AWT.execute(new Runnable() { public void run() {
            myProgress.setProgress(0.1, "Importing tags");
            createTagNodesAndImport(tags, existingEmptyTags, myProgress.createDelegate(0.9, "TN"));
          }});
        } catch (IOException e) {
          String msg = "Cannot read file: " + e.getMessage();
          Log.warn("TI: " + msg, e);
          ThreadGate.AWT.execute(Functional.apply(msg, myOnFatalError));
          myProgress.setDone();
        }
      }
    });
  }

  @ThreadAWT
  public void cancel() {
    for (Pair<ItemSource, TaggedItemsCollector> source_collector : myTaggedItemSources) {
      source_collector.getFirst().stop(source_collector.getSecond());
    }
  }
  
  @CanBlock
  private LongList retrieveEmptyTags() {
    return myDb.readForeground(new ReadTransaction<LongList>() {
      @Override
      public LongList transaction(DBReader reader) throws DBOperationCancelledException {
        LongArray tags = reader.query(DPEqualsIdentified.create(DBAttribute.TYPE, TagsComponentImpl.TYPE_TAG)).copyItemsSorted();
        LongArray emptyTags = new LongArray();
        for (LongIterator tag : tags) {
          long tagItem = tag.value();
          if (reader.query(DPIntersects.create(TagsComponentImpl.TAGS_ATTRIBUTE, Collections.singleton(tagItem))).count() == 0) {
            emptyTags.add(tagItem);
          }
        }
        return emptyTags;
      }
    }).waitForCompletion();
  }

  @ThreadAWT
  private void createTagNodesAndImport(List<TagFileStorage.TagInfo> tags, LongList existingEmptyTags, Progress progress) {
    TreeNodeFactory nodeFactory = myRootNode.getNodeFactory();
    Map<Pair<String, String>, TagNode> existingTags = hashMap();
    for (GenericNode existingTag : myTagsFolder.getChildren()) {
      if (existingTag instanceof TagNode) {
        TagNode tagNode = (TagNode) existingTag;
        existingTags.put(Pair.create(tagNode.getName(), tagNode.getIconPath()), tagNode);
      }
    }
    int nTags = tags.size();
    double step = 1.0 / nTags;
    for (int i = 0; i < nTags; ++i) {
      TagFileStorage.TagInfo importedTag = tags.get(i);
      String tagName = importedTag.getName();
      String tagIconPath = importedTag.getIconPath();

      progress.setActivity(tagName);
      Progress tagProgress = progress.createDelegate(step, "T_" + tagName);

      TagNode existingTag = existingTags.get(Pair.create(tagName, tagIconPath));
      TagNode targetNode = null;
      if (existingTag != null && (existingEmptyTags.contains(existingTag.getTagItem()) || confirmMerge(existingTag)))
        targetNode = existingTag;
      if (targetNode == null) {
        targetNode = nodeFactory.createTag(myTagsFolder);
        TagEditor.editTag(targetNode, tagName, tagIconPath);
      }

      List<String> urls = importedTag.getItemUrls();
      if (!urls.isEmpty()) {
        acceptItems(targetNode, urls, tagProgress);
      } else {
        tagProgress.setDone();
      }
    }
  }

  private boolean confirmMerge(TagNode existingTag) {
    // Favorites is always merged
    if (isFavorites(existingTag)) return true;
    return myConfirmMergeTag.invoke(existingTag.getName());
  }

  private void acceptItems(final TagNode targetNode, final List<String> itemUrls, final Progress tagProgress) {
    ItemSource source = getSource(itemUrls, tagProgress, targetNode.getName());
    if (source == null) {
      tagDone(tagProgress);
      return;
    }
    final TaggedItemsCollector itemsCollector = new TaggedItemsCollector(itemUrls, tagProgress);
    final ProgressSource collectProgress = source.getProgress(itemsCollector);
    tagProgress.delegate(collectProgress, 0.9);
    myTaggedItemSources.add(Pair.create(source, itemsCollector));
    final DetachComposite detach = new DetachComposite();
    collectProgress.getModifiable().addChangeListener(detach, ThreadGate.STRAIGHT, new ChangeListener() {
      @Override
      public void onChange() {
        if (collectProgress.isDone()) {
          detach.detach();
          myDb.writeForeground(new WriteTransaction<Object>() {
            @Override
            public Object transaction(DBWriter writer) throws DBOperationCancelledException {
              long tag = writer.materialize(targetNode.getTagDbObj());
              for (LongIterator i = itemsCollector.getItems().iterator(); i.hasNext();) {
                long item = i.nextValue();
                Set<Long> tags = writer.getValue(item, TagsComponentImpl.TAGS_ATTRIBUTE);
                if (tags == null)
                  tags = hashSet(1);
                tags.add(tag);
                writer.setValue(item, TagsComponentImpl.TAGS_ATTRIBUTE, tags);
              }
              return null;
            }
          }).onSuccess(ThreadGate.AWT, new Procedure<Object>() {
            @Override
            public void invoke(Object arg) {
              tagDone(tagProgress);
            }
          });
        }
      }
    });
    source.reload(itemsCollector);
  }

  private void tagDone(Progress tagProgress) {
    tagProgress.setDone();
  }

  private ItemSource getSource(List<String> itemUrls, final Progress tagProgress, final String tagName) {
    try {
      return ItemUrlServiceImpl.getItemSourceForUrls(itemUrls, myConnectionManager, constF(true).<Integer>f(), false, new Procedure<List<String>>() {
        @Override
        public void invoke(List<String> notLoadedUrls) {
          StringBuilder msg = new StringBuilder();
          msg.append("Tag ").append(tagName).append(": cannot tag ");
          if (notLoadedUrls.size() == 1) {
            msg.append(first(notLoadedUrls));
          } else {
            msg.append(Local.parse(Terms.ref_artifacts)).append(':');
            for (String url : notLoadedUrls) {
              msg.append("\n  ").append(url);
            }
          }
          tagProgress.addError(msg.toString());
        }
      });
    } catch (CantPerformExceptionExplained e) {
      tagProgress.addError(tagName + ": " + e.getMessage());
    } catch (ProviderDisabledException e) {
      Log.warn(e);
      tagProgress.addError("Cannot import tag " + tagName + " (" + e.getMessage() + ')');
    } catch (ConfigurationException e) {
      // Weird
      Log.warn(e);
      tagProgress.addError("Cannot automatically create connection (" + e.getMessage() + ") for tag " + tagName);
    }
    return null;
  }

  private static boolean isFavorites(TagNode existingTag) {
    return "Favorites".equals(existingTag.getName()) && ":favorites:".equals(existingTag.getIconPath());
  }

  private class TaggedItemsCollector implements ItemsCollector {
    private final PropertyMap myClientValues;
    private final LongArray myItems;
    private final List<String> myItemUrls;
    private final Progress myTagProgress;

    public TaggedItemsCollector(List<String> itemUrls, Progress tagProgress) {
      myItemUrls = itemUrls;
      myTagProgress = tagProgress;
      myClientValues = new PropertyMap();
      myItems = new LongArray(myItemUrls.size());
    }

    @Override
    public synchronized void addItem(long item, DBReader reader) {
      myItems.add(item);
    }

    @Override
    public synchronized void removeItem(long item) {
      myItems.remove(item);
      Log.debug("TagsImporter: item " + item + " has disappeared");
    }

    @Override
    public <T> T getValue(TypedKey<T> key) {
      return myClientValues.get(key);
    }

    @Override
    public <T> T putValue(TypedKey<T> key, T value) {
      return myClientValues.put(key, value);
    }

    @Override
    public void reportError(String error) {
      myTagProgress.addError(error);
    }

    public synchronized LongIterable getItems() {
      return LongArray.copy(myItems);
    }
  }
}
