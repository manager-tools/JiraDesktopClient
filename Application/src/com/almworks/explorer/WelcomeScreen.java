package com.almworks.explorer;

import com.almworks.api.engine.ItemProvider;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.install.Setup;
import com.almworks.api.platform.ProductInformation;
import com.almworks.util.Terms;
import com.almworks.util.components.AActionButton;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.GlobalColors;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.IdActionProxy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collection;

class WelcomeScreen {
  private final Collection<ItemProvider> myProviders;
  private final ProductInformation myProductInfo;
  private final boolean myCoverpage;
  private JComponent myWelcomePanel;

  public WelcomeScreen(Collection<ItemProvider> providers, ProductInformation productInfo, boolean coverpage) {
    myProviders = providers;
    myProductInfo = productInfo;
    myCoverpage = coverpage;
    init();
  }

  private void init() {
    Box welcomeContent = Box.createVerticalBox();
    welcomeContent.setBorder(new EmptyBorder(0, 9, 0, 9));

    welcomeContent.add(createNoProvidersMessage());

    Color bg = ColorUtil.between(UIManager.getColor("EditorPane.background"), GlobalColors.CORPORATE_COLOR_1, 0.15F);

    //WelcomePanel panel = new WelcomePanel(welcomeContent, 0, 0, bg);
    JPanel panel = new JPanel(new BorderLayout(0, 15));
    panel.setOpaque(true);
    panel.setBackground(bg);
    panel.setBorder(new EmptyBorder(31, 51, 31, 51));
    panel.add(SingleChildLayout.envelop(welcomeContent, 0F, 0F), BorderLayout.CENTER);
    if (myCoverpage) {
      panel.add(createTopComponent(bg), BorderLayout.NORTH);
      panel.add(createBottomComponent(bg), BorderLayout.SOUTH);
    }
    myWelcomePanel = new JScrollPane(new ScrollablePanel(panel));
    Aqua.cleanScrollPaneBorder(myWelcomePanel);
  }

  private Component createNoProvidersMessage() {
    final JPanel panel = new JPanel(new BorderLayout(0, 9));
    panel.setOpaque(false);

    ALabel header = new ALabel();
    JLabel text = new JLabel();
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 2));
    buttons.setOpaque(false);

    panel.add(UIUtil.adjustFont(header, 1.6F, -1, true), BorderLayout.NORTH);
    panel.add(text, BorderLayout.CENTER);
    panel.add(buttons, BorderLayout.SOUTH);

    if (myProviders.size() == 0) {
      header.setText("No connector plug-ins installed");
      text.setText("<html>No active connector plug-ins found. A connector plug-in is a part of the application<br>" +
        "that allows to work with remote issue tracking systems.<br>" + "<br>" +
        "Please try to reinstall the application or contact support.");
      buttons.add(createExitButton());
    } else {
      header.setText("All connector plug-ins are disabled");
      StringBuilder plugins = new StringBuilder();
      for (ItemProvider provider : myProviders) {
        if (plugins.length() > 0)
          plugins.append(", ");
        plugins.append(provider.getProviderName());
      }
      text.setText("<html>" +
        "All connector plug-ins have been disabled. A connector plug-in is a part of the application<br>" +
        "that allows to work with remote issue tracking systems.<br>" + "<br>" + "Available plug-ins: " +
        plugins.toString() + "<br>" + "<br>" + "Please check that the license key permits these connectors.");
    }
    return panel;
  }

  private Component createExitButton() {
    return new AActionButton(new IdActionProxy(MainMenu.File.EXIT, true));
  }

  private Component createBottomComponent(Color bg) {
    StringBuilder footer = new StringBuilder();
    footer.append(Setup.getProductName());
    footer.append(" ").append(myProductInfo.getVersion());
    footer.append(", build ").append(myProductInfo.getBuildNumber().toDisplayableString());
    footer.append(" of ").append(DateUtil.US_MEDIUM.format(myProductInfo.getBuildDate()));
    if (!myProductInfo.isProductionVersion())
      footer.append(" (").append(myProductInfo.getVersionType()).append(")");
    ALabel label = new ALabel(footer.toString());
    label.setHorizontalAlignment(SwingConstants.TRAILING);
    label.setBorder(UIUtil.createNorthBevel(bg));

//    try {
//      LicenseType type = myLicenseInfo.get(LicenseParameter.LICENSE_TYPE);
//      if (type == LicenseType.TEMPORARY_EVALUATION) {
//        Component warning = createTemporaryLaunchWarning();
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.setOpaque(false);
//        panel.add(label, BorderLayout.CENTER);
//        panel.add(warning, BorderLayout.NORTH);
//        return panel;
//      }
//    } catch (LicenseException e) {
    // ignore
//    }

    return label;
  }

//  private Component createTemporaryLaunchWarning() {
//    JPanel panel = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 29));
//    panel.setOpaque(false);
//    panel.add(new JLabel("<html><b>WARNING:</b> A license key will be required next time " +
//      Local.text(Terms.key_Deskzilla) + " is started."));
//    panel.add(new URLLink(Setup.getUrlGetLicense_Eval(), true, "Get Free Evaluation License Key..."));
//    AnAction action = myActionRegistry.getAction(MainMenu.Help.SELECT_LICENSE);
//    if (action != null) {
//      Link link = new Link();
//      link.setAnAction(action);
//      panel.add(link);
//    }
//    return panel;
//  }

  private Component createTopComponent(Color bg) {
    ALabel label = new ALabel(Local.parse("Welcome to " + Terms.ref_Deskzilla));
    UIUtil.adjustFont(label, 2.05F, -1, true);
    label.setBorder(UIUtil.createSouthBevel(bg));
    label.setForeground(bg.darker().darker());
    label.setHorizontalAlignment(SwingConstants.TRAILING);
    return label;
  }

  public void reset() {
  }

  public JComponent getComponent() {
    return myWelcomePanel;
  }
}
