package com.almworks.util.config;


import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Comparing;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.properties.StringSerializer;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author : Dyoma
 */
public class ConfigurationUtil {
  public static final String CONFIG_CHARSET = "UTF-8";

  static {
    assert Charset.isSupported(CONFIG_CHARSET);
  }

  public static Configuration copy(ReadonlyConfiguration original, MediumWatcher notificator) {
    Configuration copy = MapMedium.createConfig(notificator, original.getName());
    copyTo(original, copy);
    return copy;
  }

  public static Configuration copy(ReadonlyConfiguration original) {
    return copy(original, MediumWatcher.BLIND);
  }

  public static void copyTo(ReadonlyConfiguration source, Configuration dest) {
    copyTo(source, dest, null);
  }

  private static void copyTo(ReadonlyConfiguration source, Configuration dest, List<String> buffer) {
    if (buffer == null)
      buffer = Collections15.arrayList();
    dest.clear();
    Collection<String> settings = source.getAllSettingNames();
    for (String settingName : settings) {
      dest.setSettings(settingName, source.getAllSettings(settingName, buffer));
    }
    Collection<String> subsets = source.getAllSubsetNames();
    for (String subsetName : subsets) {
      List<? extends ReadonlyConfiguration> subsetsToCopy = source.getAllSubsets(subsetName);
      for (ReadonlyConfiguration subset : subsetsToCopy) {
        copyTo(subset, dest.createSubset(subsetName), buffer);
      }
    }
    buffer.clear();
  }

  public static boolean haveSameSettings(ReadonlyConfiguration config1, ReadonlyConfiguration config2) {
    Collection<String> settings1 = config1.getAllSettingNames();
    Collection<String> settings2 = config2.getAllSettingNames();
    if (settings1.size() != settings2.size())
      return false;
    for (Iterator<String> iterator = settings1.iterator(); iterator.hasNext();) {
      String name = iterator.next();
      if (!settings2.contains(name))
        return false;
      List<String> values1 = config1.getAllSettings(name);
      List<String> values2 = config2.getAllSettings(name);
      if (!Comparing.areSetsEqual(values1, values2))
        return false;
    }
    Collection<String> subsets1 = config1.getAllSubsetNames();
    Collection<String> subsets2 = config2.getAllSubsetNames();
    if (subsets1.size() != subsets2.size())
      return false;
    for (Iterator<String> iterator = subsets1.iterator(); iterator.hasNext();) {
      String name = iterator.next();
      if (!subsets2.contains(name))
        return false;
      List<ReadonlyConfiguration> allSubsets1 = Collections15.arrayList(config1.getAllSubsets(name));
      List<ReadonlyConfiguration> allSubsets2 = Collections15.arrayList(config2.getAllSubsets(name));
      if (allSubsets1.size() != allSubsets2.size())
        return false;
      while (allSubsets1.size() > 0) {
        ReadonlyConfiguration[] equals = findEquals(allSubsets1, allSubsets2);
        if (equals == null)
          return false;
        boolean removed = allSubsets1.remove(equals[0]);
        assert removed;
        removed = allSubsets2.remove(equals[1]);
        assert removed;
      }
    }
    return true;
  }

  public static List<String> getSettingsDifference(ReadonlyConfiguration config1, ReadonlyConfiguration config2) {
    final Collection<String> settings = Collections15.hashSet();
    settings.addAll(config1.getAllSettingNames());
    settings.addAll(config2.getAllSettingNames());

    final List<String> result = Collections15.arrayList();
    for(final String name : settings) {
      if(!Util.equals(config1.getAllSettings(name, null), config2.getAllSettings(name, null))) {
        result.add(name);
      }
    }

    return result;
  }

  private static ReadonlyConfiguration[] findEquals(List<? extends ReadonlyConfiguration> allSubsets1,
    List<? extends ReadonlyConfiguration> allSubsets2)
  {
    for (Iterator<? extends ReadonlyConfiguration> iterator1 = allSubsets1.iterator(); iterator1.hasNext();) {
      ReadonlyConfiguration subset = iterator1.next();
      for (Iterator<? extends ReadonlyConfiguration> iterator2 = allSubsets2.iterator(); iterator2.hasNext();) {
        ReadonlyConfiguration otherSubset = iterator2.next();
        if (haveSameSettings(subset, otherSubset))
          return new ReadonlyConfiguration[] {subset, otherSubset};
      }
    }
    return null;
  }

  public static String dumpConfiguration(ReadonlyConfiguration config) {
    StringBuffer result = new StringBuffer();
    dumpLevel(config, "", result);
    return result.toString();
  }

  private static void dumpLevel(ReadonlyConfiguration config, String padding, StringBuffer result) {
    String name = config.getName();
    String nextPadding = padding + "  ";
    result.append(padding).append('<').append(name).append(">\n");
    Collection<String> settingNames = config.getAllSettingNames();
    for (Iterator<String> ii = settingNames.iterator(); ii.hasNext();) {
      String settingName = ii.next();
      List<String> settings = config.getAllSettings(settingName);
      for (Iterator<String> jj = settings.iterator(); jj.hasNext();) {
        String value = jj.next();
        result.append(nextPadding).append('<').append(settingName).append('>');
        result.append(value);
        result.append('<').append(settingName).append("/>\n");
      }
    }
    Collection<String> subsetNames = config.getAllSubsetNames();
    for (Iterator<String> ii = subsetNames.iterator(); ii.hasNext();) {
      String subsetName = ii.next();
      List<? extends ReadonlyConfiguration> subsets = config.getAllSubsets(subsetName);
      for (Iterator<? extends ReadonlyConfiguration> jj = subsets.iterator(); jj.hasNext();)
        dumpLevel(jj.next(), nextPadding, result);
    }
    result.append(padding).append('<').append(name).append("/>\n");
  }

  public static void copySubsetsTo(ReadonlyConfiguration source, Configuration target, String subsetName) {
    List<? extends ReadonlyConfiguration> subsets = source.getAllSubsets(subsetName);
    if (subsets.size() > 0) {
      List<String> buffer = Collections15.arrayList();
      for (ReadonlyConfiguration subset : subsets) {
        copyTo(subset, target.createSubset(subsetName), buffer);
      }
    }
  }

  public static <T> void attachConfig(final SelectionAccessor<T> accessor, final Configuration config, final String setting, final StringSerializer<T> valueConvertor) {
    accessor.addChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        String strValue = valueConvertor.storeToString(accessor.hasSelection() ? accessor.getSelection() : null);
        config.setSetting(setting, strValue);
      }
    });
    try {
      String strValue = config.isSet(setting) ? config.getMandatorySetting(setting) : null;
      T value = valueConvertor.restoreFromString(strValue);
      if (value != null)
        accessor.setSelected(value);
      else
        accessor.clearSelection();
    } catch (ReadonlyConfiguration.NoSettingException e) {
      assert false;
    }
  }
}
