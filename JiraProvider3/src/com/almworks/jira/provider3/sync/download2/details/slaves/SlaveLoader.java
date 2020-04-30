package com.almworks.jira.provider3.sync.download2.details.slaves;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface SlaveLoader<B> {
  Collection<? extends Parsed<B>> loadValue(Object jsonObject, int order);

  B createBags(EntityHolder master);

  interface Parsed<B> {
    void addTo(EntityHolder master, @Nullable B bags);
  }

  class AsValue implements JsonIssueField.ParsedValue {
    private final List<? extends Parsed<?>> myFullBag;

    public AsValue(List<? extends Parsed<?>> fullBag) {
      myFullBag = fullBag;
    }

    public static Collection<? extends JsonIssueField.ParsedValue> singleton(List<? extends Parsed<?>> fullBag) {
      return Collections.singleton(new AsValue(fullBag));
    }

    @Override
    public boolean addTo(EntityHolder issue) {
      for (SlaveLoader.Parsed<?> slave : myFullBag) slave.addTo(issue, null);
      return true;
    }
  }
}
