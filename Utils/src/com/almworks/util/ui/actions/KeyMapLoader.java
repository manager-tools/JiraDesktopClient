package com.almworks.util.ui.actions;

import com.almworks.util.Env;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ReadonlyConfiguration;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class KeyMapLoader {
  private static final String SHORTCUT = "shortcut";
  private static final String ACTION_ID = "actionId";
  private static final String KEY_STROKE = "keystroke";
  private static final String KEY_STROKE_MAC = "mac";
  private static final String NO_KEY_STROKE = "none";
  private static final String WINDOW_ID = "windowId";

  @NotNull
  private final ReadonlyConfiguration myConfig;

  public KeyMapLoader(@NotNull ReadonlyConfiguration config) {
    myConfig = config;
  }

  @NotNull
  public Map<String, ScopedKeyStroke> loadKeyMap() throws ConfigurationException {
    final boolean mac = Env.isMac();
    final List<? extends ReadonlyConfiguration> shortcuts = myConfig.getAllSubsets(SHORTCUT);
    final Map<String, ScopedKeyStroke> result = Collections15.hashMap();

    for (final ReadonlyConfiguration shortcut : shortcuts) {
      try {
        final String actionId = shortcut.getMandatorySetting(ACTION_ID);

        String strokeString = null;

        if (mac) {
          strokeString = shortcut.getSetting(KEY_STROKE_MAC, null);
        }

        if (strokeString == null) {
          strokeString = shortcut.getMandatorySetting(KEY_STROKE);
        }

        if(NO_KEY_STROKE.equalsIgnoreCase(strokeString)) {
          continue;
        }

        final KeyStroke stroke = KeyStroke.getKeyStroke(strokeString);
        if (stroke == null) {
          throw new ConfigurationException("KeyStroke syntax error. Id: " + actionId + " stroke: " + strokeString);
        }

        final ScopedKeyStroke scoped = new ScopedKeyStroke(stroke, shortcut.getAllSettings(WINDOW_ID));

        for(final Map.Entry<String, ScopedKeyStroke> e : result.entrySet()) {
          if(scoped.conflictsWith(e.getValue())) {
            throw new ConfigurationException("Conflicting keystrokes: " +
              "stroke1: " + e.getValue() + " action1: " + e.getKey() +
              " stroke2: " + scoped + " action2: " + actionId);
          }
        }

        if(result.containsKey(actionId)) {
          throw new ConfigurationException("Duplicate actionId: " + actionId
              + " stroke1: " + result.get(actionId) + " stroke2: " + stroke);
        }

        result.put(actionId, scoped);
      } catch (ConfigurationException e) {
        Log.warn("cannot install shortcut", e);
      }
    }

    return result;
  }

  public void registerKeyMap(ActionRegistry registry) throws ConfigurationException {
    for (final Map.Entry<String, ScopedKeyStroke> entry : loadKeyMap().entrySet()) {
      final String actionId = entry.getKey();
      final ScopedKeyStroke oldStroke = registry.getKeyStroke(actionId);
      final ScopedKeyStroke stroke = entry.getValue();

      if (oldStroke != null && !oldStroke.equals(stroke)) {
        throw new ConfigurationException(
          "KeyStroke already registered. actionId: " + actionId + " stroke:" + oldStroke + " new stroke:" + stroke);
      }

      registry.registerKeyStroke(actionId, stroke);
    }
  }
}