package com.almworks.util.model.config;

import com.almworks.util.collections.Convertor;
import com.almworks.util.config.Configuration;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import org.almworks.util.detach.Lifespan;

/**
 * :todoc:
 *
 * @author sereda
 */
public class PersistentModels {
  public static <T> BasicScalarModel<T> createModel(final Configuration config, final String setting,
    final Convertor<T, String> marshaller, Convertor<String, T> unmarshaller) {

    String stored = config.getSetting(setting, null);
    T value = unmarshaller.convert(stored);
    BasicScalarModel<T> result = BasicScalarModel.createWithValue(value, true);
    result.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<T>() {
      public void onScalarChanged(ScalarModelEvent<T> event) {
        config.setSetting(setting, marshaller.convert(event.getNewValue()));
      }
    });
    return result;
  }

  public static BasicScalarModel<Boolean> createBoolean(Configuration config, String setting, final boolean defaultValue) {
    return createModel(config, setting,
      new Convertor<Boolean, String>() {
        public String convert(Boolean b) {
          return (b == null || !b.booleanValue()) ? "false" : "true";
        }
      },
      new Convertor<String, Boolean>() {
        public Boolean convert(String s) {
          return s == null ? Boolean.valueOf(defaultValue) : Boolean.valueOf(s);
        }
      });
  }
}
