package com.blazemeter.jmeter.correlation.gui.settings;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Set;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.action.AbstractActionWithNoRunningTest;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.gui.plugin.MenuCreator;

/**
 * Tools > Auto Correlation Recorder: the plugin's settings in JMeter's menu, and the command that
 * opens them.
 */
public class CorrelationRecorderMenuCommand extends AbstractActionWithNoRunningTest
    implements MenuCreator {

  private static final String ACTION_JMETER_PROPERTIES = "correlation_recorder_jmeter_properties";
  private static final Set<String> ACTION_NAMES =
      Collections.singleton(ACTION_JMETER_PROPERTIES);

  @Override
  public Set<String> getActionNames() {
    return ACTION_NAMES;
  }

  @Override
  protected void doActionAfterCheck(ActionEvent e) {
    GuiPackage gui = GuiPackage.getInstance();
    new RecommendedJMeterPropertiesDialog(gui != null ? gui.getMainFrame() : null)
        .setVisible(true);
  }

  @Override
  public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
    if (location != MENU_LOCATION.TOOLS) {
      return new JMenuItem[0];
    }
    JMenu root = new JMenu("BlazeMeter Auto Correlation Recorder");
    JMenuItem properties = new JMenuItem("JMeter Properties...");
    properties.setName(ACTION_JMETER_PROPERTIES);
    properties.setActionCommand(ACTION_JMETER_PROPERTIES);
    properties.addActionListener(ActionRouter.getInstance());
    root.add(properties);
    return new JMenuItem[] {root};
  }

  @Override
  public JMenu[] getTopLevelMenus() {
    return new JMenu[0];
  }

  @Override
  public boolean localeChanged(MenuElement menu) {
    return false;
  }

  @Override
  public void localeChanged() {
    // labels are fixed English text
  }
}
