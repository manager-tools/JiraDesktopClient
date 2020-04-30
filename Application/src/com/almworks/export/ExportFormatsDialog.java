package com.almworks.export;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.advmodel.*;
import com.almworks.util.commons.Function;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.properties.StringSerializer;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.EnabledAction;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static com.almworks.util.ui.SingleChildLayout.CONTAINER;
import static com.almworks.util.ui.SingleChildLayout.PREFERRED;

public class ExportFormatsDialog {
  public static final StringSerializer<NumberFormat> DECIMAL_SERIALIZER = new StringSerializer<NumberFormat>() {
    public String storeToString(NumberFormat value) {
      if (value == null)
        return "";
      assert value instanceof DecimalFormat : value;
      if (!(value instanceof DecimalFormat))
        return "";
      return ((DecimalFormat) value).toPattern();
    }

    public NumberFormat restoreFromString(String string) {
      try {
        return new DecimalFormat(string);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  };
  private static final StringSerializer<DateFormat> DATE_SERIALIZER = new StringSerializer<DateFormat>() {
    public String storeToString(DateFormat value) {
      if (value == null)
        return "";
      assert value instanceof SimpleDateFormat : value;
      if (!(value instanceof SimpleDateFormat))
        return "";
      return ((SimpleDateFormat) value).toPattern();
    }

    public DateFormat restoreFromString(String string) {
      try {
        return new SimpleDateFormat(string);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  };

  private final DialogManager myDialogManager;
  private JPanel myWholePanel;
  private JLabel myLocaleLabel;
  private JPanel myNumberFormatPlace;
  private JPanel myDateFormatPlace;
  private AComboBox<Locale> myLocaleCombo;

  /**
   * null value means default locale
   */
  private final BasicScalarModel<Locale> mySelectedLocale = BasicScalarModel.create(true, false);

  private final DetachComposite myLife = new DetachComposite();
  private static final String LOCALE_SETTING = "locale";

  private final BasicScalarModel<DateFormat> myApprovedDateFormat = BasicScalarModel.create(true, false);
  private final BasicScalarModel<NumberFormat> myApprovedNumberFormat = BasicScalarModel.create(true, false);
  private BasicScalarModel<Locale> myApprovedLocale = BasicScalarModel.create(true, false);
  private ExportFormatForm<NumberFormat> myNumberFormatForm;
  private ExportFormatForm<DateFormat> myDateFormatForm;

  public ExportFormatsDialog(DialogManager dialogManager, Configuration configuration) {
    myDialogManager = dialogManager;
    setupLocale(configuration);
    setupLocaleCombo();
    createDialog(configuration);
  }

  private void setupLocaleCombo() {
    List<Locale> locales = Collections15.arrayList();
    Locale[] allLocales = Locale.getAvailableLocales();
    final Locale defaultLocale = Locale.getDefault();
    locales.add(defaultLocale);
    if (!Util.NN(defaultLocale.getLanguage()).equalsIgnoreCase(Locale.ENGLISH.getLanguage())) {
      locales.add(Locale.ENGLISH);
    }
    for (Locale locale : allLocales) {
      if (Util.NN(locale.getCountry()).length() == 0) {
        String language = Util.NN(locale.getLanguage());
        if (language.length() > 0 && !language.equals(defaultLocale.getLanguage()) &&
          !language.equals(Locale.ENGLISH.getLanguage())) {
          locales.add(locale);
        }
      }
    }
    Comparator<Locale> comparator = new Comparator<Locale>() {
      private String str(Locale o) {
        String result = o.getDisplayName(Locale.US);
        if (o == defaultLocale)
          result = "00001" + result;
        else if (o == Locale.ENGLISH)
          result = "00002" + result;
        return result;
      }

      public int compare(Locale o1, Locale o2) {
        return String.CASE_INSENSITIVE_ORDER.compare(str(o1), str(o2));
      }
    };
    Collections.sort(locales, comparator);
    AListModel<Locale> sorted = FixedListModel.create(locales);
    Locale current = mySelectedLocale.getValue();
    if (sorted.indexOf(current) == -1 && sorted.getSize() > 0)
      current = sorted.getAt(0);
    final SelectionInListModel<Locale> comboModel = SelectionInListModel.createForever(sorted, current);
    comboModel.addSelectionListener(myLife, new SelectionListener.SelectionOnlyAdapter() {
      public void onSelectionChanged() {
        Locale item = comboModel.getSelectedItem();
        if (!Util.equals(item, mySelectedLocale.getValue()))
          mySelectedLocale.setValue(item);
      }
    });
    myLocaleCombo.setModel(comboModel);
    myLocaleCombo.setCanvasRenderer(new CanvasRenderer<Locale>() {
      public void renderStateOn(CellState state, Canvas canvas, Locale item) {
        if (item == defaultLocale || item == Locale.ENGLISH)
          canvas.setFontStyle(Font.BOLD);
        canvas.appendText(item.getDisplayName(Locale.US));
      }
    });
    myLocaleCombo.setColumns(0);
    myLocaleLabel.setLabelFor(myLocaleCombo.getCombobox());
  }

  private void setupLocale(final Configuration configuration) {
    String setting = configuration.getSetting(LOCALE_SETTING, null);
    Locale locale = null;
    if (setting != null && setting.length() > 0) {
      Locale[] locales = Locale.getAvailableLocales();
      for (Locale l : locales) {
        if (setting.equalsIgnoreCase(l.getDisplayName(Locale.US))) {
          locale = l;
          break;
        }
      }
    }
    if (locale == null)
      locale = Locale.getDefault();
    mySelectedLocale.setValue(locale);
    myLife.add(mySelectedLocale.getEventSource().addStraightListener(new ScalarModel.Adapter<Locale>() {
      public void onScalarChanged(ScalarModelEvent<Locale> event) {
        Locale locale = event.getNewValue();
        if (locale == null) {
          configuration.setSetting(LOCALE_SETTING, "");
        } else {
          configuration.setSetting(LOCALE_SETTING, locale.getDisplayName(Locale.US));
        }
      }
    }));
  }

  private void createDialog(Configuration configuration) {
    myNumberFormatForm = createNumberFormatForm(configuration, mySelectedLocale);
    myDateFormatForm = createDateFormatForm(configuration, mySelectedLocale);
    myLife.add(myDateFormatForm.getDetach());
    myLife.add(myNumberFormatForm.getDetach());
    myDateFormatPlace.setLayout(new SingleChildLayout(CONTAINER, PREFERRED));
    myDateFormatPlace.add(myDateFormatForm.getComponent());
    myNumberFormatPlace.setLayout(new SingleChildLayout(CONTAINER, PREFERRED));
    myNumberFormatPlace.add(myNumberFormatForm.getComponent());
    apply();
  }

  private void apply() {
    myApprovedDateFormat.setValue(myDateFormatForm.getSelectedFormatModel().getValue());
    myApprovedNumberFormat.setValue(myNumberFormatForm.getSelectedFormatModel().getValue());
    myApprovedLocale.setValue(mySelectedLocale.getValue());
  }

  private ExportFormatForm<NumberFormat> createNumberFormatForm(Configuration configuration,
    ScalarModel<Locale> locale)
  {
    Configuration config = configuration.getOrCreateSubset("number");
    SelectionInListModel<NumberFormat> comboModel = createFormatsModel(new Function<Locale, Set<NumberFormat>>() {
      public Set<NumberFormat> invoke(Locale locale) {
        Set<NumberFormat> set = Collections15.linkedHashSet();
        set.add(NumberFormat.getInstance(locale));
        set.add(NumberFormat.getIntegerInstance(locale));
        set.add(NumberFormat.getPercentInstance(locale));
        return set;
      }
    });
    Double sampleValue = 1267332.98;
    return new ExportFormatForm<NumberFormat>(config, DECIMAL_SERIALIZER, comboModel, sampleValue, "Number Format",
      locale);
  }

  /**
   * Creates combobox model that updates when locale is changed
   */
  private <T> SelectionInListModel<T> createFormatsModel(final Function<Locale, Set<T>> defaults) {
    final OrderListModel<T> list = OrderListModel.create();
    final SelectionInListModel<T> comboModel = SelectionInListModel.createForever(list, null);
    mySelectedLocale.getEventSource().addAWTListener(myLife, new ScalarModel.Adapter<Locale>() {
      public void onScalarChanged(ScalarModelEvent<Locale> event) {
        Locale locale = mySelectedLocale.getValue();
        if (locale == null)
          locale = Locale.getDefault();
        int selectedItem = comboModel.indexOf(comboModel.getSelectedItem());
        Set<T> set = defaults.invoke(locale);
        list.clear();
        list.addAll(set);
        if ((selectedItem < 0 || selectedItem >= comboModel.getSize()) && comboModel.getSize() > 0)
          selectedItem = 0;
        comboModel.setSelectedItem(comboModel.getAt(selectedItem));
      }
    });
    return comboModel;
  }

  private ExportFormatForm<DateFormat> createDateFormatForm(Configuration configuration, ScalarModel<Locale> locale) {
    Configuration config = configuration.getOrCreateSubset("date");
    SelectionInListModel<DateFormat> comboModel = createFormatsModel(new Function<Locale, Set<DateFormat>>() {
      public Set<DateFormat> invoke(Locale locale) {
        Set<DateFormat> set = Collections15.linkedHashSet();
        set.add(DateFormat.getDateInstance(DateFormat.SHORT, locale));
        set.add(DateFormat.getDateInstance(DateFormat.MEDIUM, locale));
        set.add(DateFormat.getDateInstance(DateFormat.LONG, locale));
        set.add(DateFormat.getDateInstance(DateFormat.FULL, locale));
        set.add(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale));
        set.add(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale));
        set.add(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale));
        set.add(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale));
        return set;
      }
    });
    Date sampleValue = new Date();
    return new ExportFormatForm<DateFormat>(config, DATE_SERIALIZER, comboModel, sampleValue, "Date Format", locale);
  }

  public void show() {
    DialogBuilder builder = myDialogManager.createBuilder("formats");
    builder.setTitle("Specify Formats");
    builder.setModal(true);
    builder.setContent(myWholePanel);
    builder.setEmptyCancelAction();
    builder.setOkAction(new EnabledAction("OK") {
      protected void doPerform(ActionContext context) throws CantPerformException {
        apply();
      }
    });
    builder.showWindow();
  }

  public ScalarModel<NumberFormat> getNumberFormatModel() {
    return myApprovedNumberFormat;
  }

  public ScalarModel<DateFormat> getDateFormatModel() {
    return myApprovedDateFormat;
  }

  public BasicScalarModel<Locale> getLocaleModel() {
    return myApprovedLocale;
  }
}
