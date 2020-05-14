/**
 * rscminus
 *
 * This file is part of rscminus.
 *
 * rscminus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * rscminus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with rscminus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Authors: see <https://github.com/OrN/rscminus>
 */

package rscminus.scraper;

import rscminus.common.Logger;
import rscminus.common.Settings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class StripperWindow {

  private JFrame frame;

  JTabbedPane tabbedPane;


  public StripperWindow() {
    try {
      // Set System L&F as a fall-back option.
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          NimbusLookAndFeel laf = (NimbusLookAndFeel) UIManager.getLookAndFeel();
          laf.getDefaults().put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 11));
          break;
        }
      }
    } catch (UnsupportedLookAndFeelException e) {
      Logger.Error("Unable to set L&F: Unsupported look and feel");
    } catch (ClassNotFoundException e) {
      Logger.Error("Unable to set L&F: Class not found");
    } catch (InstantiationException e) {
      Logger.Error("Unable to set L&F: Class object cannot be instantiated");
    } catch (IllegalAccessException e) {
      Logger.Error("Unable to set L&F: Illegal access exception");
    }
    initialize();
  }

  public void showStripperWindow() {
    frame.setVisible(true);
  }

  public void hideStripperWindow() {
    frame.setVisible(false);
  }

  /** Initialize the contents of the frame. */
  private void initialize() {
    try {
      SwingUtilities.invokeAndWait(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    runInit();
                  } catch (HeadlessException e) {
                    Logger.Warn("If you had a display environment, we'd try to open a GUI for you!");
                  }
                }
              });
    } catch (InvocationTargetException e) {
      Logger.Error("There was a thread-related error while setting up the Stripper window!");
      e.printStackTrace();
    } catch (InterruptedException e) {
      Logger.Error(
              "There was a thread-related error while setting up the Stripper window! The window may not be initialized properly!");
      e.printStackTrace();
    }
  }

  private void runInit() {
    frame = new JFrame();

    Logger.Info("Creating Stripper Window"); // only show this after JFrame is created in case user is headless

    ExecutorService executor = Executors.newFixedThreadPool(3);
    frame.setTitle("RSCMinus");
    frame.setBounds(100, 100, 800, 650);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout(0, 0));
    URL iconURL = Settings.getResource("/assets/rscminus-logo.png");
    if (iconURL != null) {
      ImageIcon icon = new ImageIcon(iconURL);
      frame.setIconImage(icon.getImage());
    }

    tabbedPane = new JTabbedPane();

    JScrollPane aboutScrollPane = new JScrollPane();
    JScrollPane scrapeScrollPane = new JScrollPane();
    JScrollPane stripScrollPane = new JScrollPane();

    JPanel aboutPanel = new JPanel();
    JPanel scrapePanel = new JPanel();
    JPanel stripPanel = new JPanel();


    frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
    tabbedPane.setFont(new Font("", Font.PLAIN,16));
    tabbedPane.addTab("About", null, aboutScrollPane, null);
    tabbedPane.addTab("Scrape", null, scrapeScrollPane, null);
    tabbedPane.addTab("Strip/Optimize", null, stripScrollPane, null);

    aboutScrollPane.setViewportView(aboutPanel);
    scrapeScrollPane.setViewportView(scrapePanel);
    stripScrollPane.setViewportView(stripPanel);

    // Adding padding for aesthetics
    aboutPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    scrapePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    stripPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    setScrollSpeed(aboutScrollPane,  20, 15);
    setScrollSpeed(scrapeScrollPane, 20, 15);
    setScrollSpeed(stripScrollPane,  20, 15);


    /*
     * About tab
     */
    aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));

    JPanel thirdsPanel= new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    c.gridx = 0;
    c.fill = GridBagConstraints.HORIZONTAL;

    JPanel logoPanel = new JPanel();
    try {
      BufferedImage rscminusLogo = ImageIO.read(Settings.getResource("/assets/rscminus-logo.png"));
      JLabel rscminusLogoJLabel = new JLabel(new ImageIcon(rscminusLogo.getScaledInstance(256, 256, Image.SCALE_SMOOTH)));
      rscminusLogoJLabel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
      logoPanel.add(rscminusLogoJLabel);
    } catch(Exception e) {
      e.printStackTrace();
    }

    thirdsPanel.add(logoPanel,c);

    JPanel rightPane = new JPanel(new GridBagLayout());
    GridBagConstraints cR = new GridBagConstraints();
    cR.fill = GridBagConstraints.VERTICAL;
    cR.anchor = GridBagConstraints.LINE_START;
    cR.weightx = 0.5;
    cR.gridy = 0;
    cR.gridwidth = 3;

    JLabel RSCMinusText = new JLabel("<html><div style=\"font-size:45px; padding-bottom:10px;\"<b>RSC</b>Minus</div><div style=\"font-size:20px;\">Gui Edition v" + Settings.versionNumber + "</div></html>");

    rightPane.add(RSCMinusText);

    cR.gridy = 1;

    JLabel aboutText = new JLabel("<html><head><style>p{font-size:10px; padding-top:15px;}</style></head><p><b>RSC</b>Minus is a 235-protocol compatible \"Proof of Concept\"<br/>"+
            "server core. It also has the ability to very quickly process<br/>"+
            "replays to automatically scrape data, or to strip chat<br/>"+
            "out of them & optimize their compression.</p>"+
            "<p>Use the tabs at the top to pick a function.</p></html>");


    rightPane.add(aboutText, cR);
    c.gridx = 2;
    thirdsPanel.add(rightPane,c);

    JPanel bottomPane = new JPanel(new GridBagLayout());
    GridBagConstraints cB = new GridBagConstraints();
    cB = new GridBagConstraints();
    cB.fill = GridBagConstraints.HORIZONTAL;
    cB.anchor = GridBagConstraints.NORTH;

    cB.gridx = 0;
    cB.weightx = 0.33;
    cB.gridwidth = 1;

    JLabel licenseText = new JLabel("Licensed under GPLv3");
    bottomPane.add(licenseText,cB);

    cB.gridx = 5;
    cB.weightx = 1;
    cB.gridwidth = 20;
    JLabel blank = new JLabel("");
    bottomPane.add(blank,cB);

    cB.gridx = 30;
    cB.weightx = 0.33;
    cB.gridwidth = 1;
    JLabel authorsLink = new JLabel("Authors: Ornox & Logg");
    authorsLink.setBorder(BorderFactory.createEmptyBorder(0,400,0,0)); //don't ask
    bottomPane.add(authorsLink,cB);

    c.gridy = 10;
    c.gridx = 0;
    c.gridwidth = 10;
    thirdsPanel.add(bottomPane, c);

    aboutPanel.add(thirdsPanel);

    /*
     * Scrape tab
     */

    scrapePanel.setLayout(new BoxLayout(scrapePanel, BoxLayout.Y_AXIS));
    scrapePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    addSettingsHeader(scrapePanel, "Get stuff out of replays");

    JPanel scrapePanelCheckboxesPanel = new JPanel();
    scrapePanelCheckboxesPanel.setLayout(new BoxLayout(scrapePanelCheckboxesPanel,BoxLayout.Y_AXIS));
    JCheckBox dumpSceneryCheckbox =  addCheckbox("Dump Scenery",scrapePanelCheckboxesPanel);
    dumpSceneryCheckbox.setSelected(Settings.dumpScenery);
    dumpSceneryCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.dumpScenery = !Settings.dumpScenery;
              }
            }
    );

    JCheckBox dumpBoundariesCheckbox =  addCheckbox("Dump Boundaries",scrapePanelCheckboxesPanel);
    dumpBoundariesCheckbox.setSelected(Settings.dumpBoundaries);
    dumpBoundariesCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.dumpBoundaries = !Settings.dumpBoundaries;
              }
            }
    );

    JLabel explainWhatTheTextBoxIsFor = new JLabel("Paste the top directory for the replay(s) you want to scrape: (CTRL-V)");
    scrapePanelCheckboxesPanel.add(explainWhatTheTextBoxIsFor);

    JPanel replayDirectoryTextFieldJpanel = new JPanel();
    replayDirectoryTextFieldJpanel.setLayout(new BoxLayout(replayDirectoryTextFieldJpanel, BoxLayout.Y_AXIS));
    replayDirectoryTextFieldJpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    JTextField replayDirectoryTextField = new JTextField();
    replayDirectoryTextField.setMinimumSize(new Dimension(100, 28));
    replayDirectoryTextField.setMaximumSize(new Dimension(500, 28));
    replayDirectoryTextField.setAlignmentY((float) 0.75);
    replayDirectoryTextFieldJpanel.add(replayDirectoryTextField);
    scrapePanelCheckboxesPanel.add(replayDirectoryTextFieldJpanel);

    addButton("Start",scrapePanelCheckboxesPanel,Component.LEFT_ALIGNMENT)
            .addActionListener(
                new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    executor.submit(() -> scrapeButtonAction(replayDirectoryTextField));
                  }
                }
            );

    scrapePanel.add(scrapePanelCheckboxesPanel);

    //TODO put "start" button inline, to the right of the text field

    //TODO add jlabel "OR" here
    //TODO add file chooser here

    /*
     * Strip tab
     */
    stripPanel.setLayout(new BoxLayout(stripPanel, BoxLayout.Y_AXIS));
    addSettingsHeader(stripPanel, "Strip Chat or Just Optimize Replay Data (recompress)");
    stripPanel.setLayout(new BoxLayout(stripPanel, BoxLayout.Y_AXIS));
    stripPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPanel stripPanelCheckboxesPanel = new JPanel();
    stripPanelCheckboxesPanel.setLayout(new BoxLayout(stripPanelCheckboxesPanel,BoxLayout.Y_AXIS));

    JCheckBox stripPublicChatCheckbox =  addCheckbox("Delete Public Chat (makes replays depressing, only use if necessary)",stripPanelCheckboxesPanel);
    stripPublicChatCheckbox.setSelected(Settings.sanitizePublicChat);
    stripPublicChatCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.sanitizePublicChat = !Settings.sanitizePublicChat;
              }
            }
    );

    JCheckBox stripPrivateChatCheckbox =  addCheckbox("Delete Private Messages",stripPanelCheckboxesPanel);
    stripPrivateChatCheckbox.setSelected(Settings.sanitizePrivateChat);
    stripPrivateChatCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.sanitizePrivateChat = !Settings.sanitizePrivateChat;
              }
            }
    );

    JCheckBox stripPrivateFriendsUpdateCheckbox =  addCheckbox("Delete Friends/Ignore Lists and Log In/Out Messages (only use if necessary, deletes info on worlds that you & others are logged into)",stripPanelCheckboxesPanel);
    stripPrivateFriendsUpdateCheckbox.setSelected(Settings.sanitizeFriendsIgnore);
    stripPrivateFriendsUpdateCheckbox.addActionListener(
            new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                Settings.sanitizeFriendsIgnore = !Settings.sanitizeFriendsIgnore;
              }
            }
    );

    explainWhatTheTextBoxIsFor = new JLabel("Paste the top directory for the replay(s) you want to optimize or strip data from: (CTRL-V)");
    stripPanelCheckboxesPanel.add(explainWhatTheTextBoxIsFor);

    JPanel replayDirectoryTextField2Jpanel = new JPanel();
    replayDirectoryTextField2Jpanel.setLayout(new BoxLayout(replayDirectoryTextField2Jpanel, BoxLayout.Y_AXIS));
    replayDirectoryTextField2Jpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    JTextField replayDirectoryTextField2 = new JTextField();
    replayDirectoryTextField2.setMinimumSize(new Dimension(100, 28));
    replayDirectoryTextField2.setMaximumSize(new Dimension(500, 28));
    replayDirectoryTextField2.setAlignmentY((float) 0.75);
    replayDirectoryTextField2Jpanel.add(replayDirectoryTextField2);
    stripPanelCheckboxesPanel.add(replayDirectoryTextField2Jpanel);

    addButton("Start",stripPanelCheckboxesPanel,Component.LEFT_ALIGNMENT)
            .addActionListener(
                    new ActionListener() {
                      @Override
                      public void actionPerformed(ActionEvent e) {
                          executor.submit(() -> stripButtonAction(replayDirectoryTextField2));
                      }
                    }
            );

    stripPanel.add(stripPanelCheckboxesPanel);
    //TODO put "start" button inline, to the right of the text field

    //TODO add jlabel "OR" here
    //TODO add file chooser here
  }

  private void scrapeButtonAction(JTextField replayDirectoryTextField) {
    Settings.sanitizePath = replayDirectoryTextField.getText();
    Settings.sanitizeOutputPath = new File(Settings.sanitizePath, "../output").toString();
    if (replayDirectoryTextField.getText().length() > 0) {
      replayDirectoryTextField.setText("Processing!");
      if (!Scraper.scraping) {
        Logger.Info("@|green,intensity_bold Scraping " + Settings.sanitizePath+"|@");
        Scraper.strip();
        replayDirectoryTextField.setText("Finished!");
      } else {
        Logger.Warn("@|red Already scraping, please wait.|@");
      }
    }
  }

  private void stripButtonAction(JTextField replayDirectoryTextField2) {
    Settings.sanitizeReplays = true;
    Settings.sanitizePath = replayDirectoryTextField2.getText();
    Settings.sanitizeOutputPath = new File(Settings.sanitizePath,"../output").toString();
    if (replayDirectoryTextField2.getText().length() > 0) {
      replayDirectoryTextField2.setText("Processing!");
      if (!Scraper.stripping) {
        Logger.Info("@|green,intensity_bold Stripping/Optimizing " + Settings.sanitizePath + "|@");
        Scraper.strip();
        replayDirectoryTextField2.setText("Finished!");
      } else {
        Logger.Warn("@|red Already stripping/optimizing, please wait.|@");
      }
    }
    Settings.sanitizeReplays = false;
  }


  /**
   * Adds a new category title to the notifications list.
   *
   * @param panel Panel to add the title to.
   * @param categoryName Name of the category to add.
   */
  private void addSettingsHeader(JPanel panel, String categoryName) {
    addSettingsHeaderLabel(panel, "<html><div style=\"font-size:10px;\"><b>" + categoryName + "</b></div></html>");
    //TODO: addSettingsHeaderSeparator(panel);
  }

  /**
   * Adds a new horizontal separator to the notifications list.
   *
   * @param panel Panel to add the separator to.
   *
  private void addSettingsHeaderSeparator(JPanel panel) {
    JSeparator jsep = new JSeparator(SwingConstants.HORIZONTAL);
    jsep.setMaximumSize(new Dimension(Short.MAX_VALUE, 7));
    panel.add(jsep);
  }

  /**
   * Adds a new category label to the notifications list.
   *
   * @param panel Panel to add the label to.
   * @param categoryName Name of the category to add.
   * @return The label that was added.
   */
  private JLabel addSettingsHeaderLabel(JPanel panel, String categoryName) {
    JLabel jlbl = new JLabel(categoryName);
    jlbl.setHorizontalAlignment(SwingConstants.LEFT);
    panel.add(jlbl);
    return jlbl;
  }

  /**
   * Adds a preStripperured JCheckbox to the specified container, setting its alignment constraint to
   * left and adding an empty padding border.
   *
   * @param text The text of the checkbox
   * @param container The container to add the checkbox to.
   * @return The newly created JCheckBox.
   */
  private JCheckBox addCheckbox(String text, Container container) {
    JCheckBox checkbox = new JCheckBox(text);
    checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
    checkbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 5));
    container.add(checkbox);
    return checkbox;
  }

  /**
   * Adds a preStripperured JButton to the specified container using the specified alignment
   * constraint. Does not modify the button's border.
   *
   * @param text The text of the button
   * @param container The container to add the button to
   * @param alignment The alignment of the button.
   * @return The newly created JButton.
   */
  private JButton addButton(String text, Container container, float alignment) {
    JButton button = new JButton(text);
    button.setAlignmentX(alignment);
    container.add(button);
    return button;
  }

  /**
   * Adds a preStripperured radio button to the specified container. Does not currently assign the
   * radio button to a group.
   *
   * @param text The text of the radio button
   * @param container The container to add the button to
   * @param leftIndent The amount of padding to add to the left of the radio button as an empty
   *     border argument.
   * @return The newly created JRadioButton
   */
  private JRadioButton addRadioButton(String text, Container container, int leftIndent) {
    JRadioButton radioButton = new JRadioButton(text);
    radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    radioButton.setBorder(BorderFactory.createEmptyBorder(0, leftIndent, 7, 5));
    container.add(radioButton);
    return radioButton;
  }

  /**
   * Sets the scroll speed of a JScrollPane
   *
   * @param scrollPane The JScrollPane to modify
   * @param horizontalInc The horizontal increment value
   * @param verticalInc The vertical increment value
   */
  private void setScrollSpeed(JScrollPane scrollPane, int horizontalInc, int verticalInc) {
    scrollPane.getVerticalScrollBar().setUnitIncrement(verticalInc);
    scrollPane.getHorizontalScrollBar().setUnitIncrement(horizontalInc);
  }
  public void disposeJFrame() {
    frame.dispose();
  }

  //TODO implement
  /*
  public static boolean replayFileChooser() {
    JFileChooser j;
    try {
      //j = new JFileChooser(Settings.REPLAY_BASE_PATH.get("custom"));
    } catch (Exception e) {
      j = new JFileChooser(Settings.Dir.REPLAY);
    }
    j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    //int response = j.showDialog(,"Select Folder");

    File selection = j.getSelectedFile();

    return false;
  }
  */

}
