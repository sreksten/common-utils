package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.HintsDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * An implementation of a HintsDisplayer which uses a window.
 */
public class HintsWindowImpl implements HintsDisplayer {

    private final ResourceBundle bundle;
    private final HintsSupport hintsSupport;
    private final String applicationName;
    private final JLabel hintIndexLabel;
    private final JHintArea hintArea;

    public HintsWindowImpl(String applicationName, HintsSupport hintsSupport) {
        bundle = ResourceBundle.getBundle("com.threeamigos.common.util.implementations.ui.HintsWindowImpl.HintsWindowImpl");
        this.applicationName = applicationName;
        this.hintsSupport = hintsSupport;
        hintIndexLabel = new JLabel();
        updateHintsLabel();
        hintArea = new JHintArea(hintsSupport.getNextHint());
    }

    @Override
    public void showHints(Component component) {

        Box panel = Box.createVerticalBox();

        JPanel hintsBorderPanel = new JPanel();
        hintsBorderPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("hint")));
        panel.add(hintsBorderPanel);

        hintArea.setPreferredSize(new Dimension(480, 100));
        hintArea.setLineWrap(true);
        hintArea.setWrapStyleWord(true);
        hintArea.setEditable(false);
        hintsBorderPanel.add(hintArea);

        panel.add(Box.createVerticalStrut(5));

        Box showHintsBox = Box.createHorizontalBox();
        JCheckBox showHintsCheckBox = new JCheckBox();
        showHintsCheckBox.setSelected(hintsSupport.isHintsVisibleAtStartup());
        showHintsCheckBox.addActionListener(e -> hintsSupport.setHintsVisibleAtStartup(showHintsCheckBox.isSelected()));
        showHintsBox.add(showHintsCheckBox);
        showHintsBox.add(new JLabel(bundle.getString("showHintsAtStartup")));
        showHintsBox.add(Box.createGlue());
        panel.add(showHintsBox);

        panel.add(Box.createVerticalStrut(5));

        Box buttonsBox = Box.createHorizontalBox();
        Dimension buttonsDimension = new Dimension(150, 40);
        JButton previousHintButton = new JButton(bundle.getString("previousHint"));
        previousHintButton.setPreferredSize(buttonsDimension);
        previousHintButton.addActionListener(e -> goToPreviousHint());
        buttonsBox.add(previousHintButton);
        buttonsBox.add(Box.createHorizontalStrut(10));
        buttonsBox.add(hintIndexLabel);
        buttonsBox.add(Box.createHorizontalStrut(10));
        JButton nextHintButton = new JButton(bundle.getString("nextHint"));
        nextHintButton.setPreferredSize(buttonsDimension);
        nextHintButton.addActionListener(e -> goToNextHint());
        buttonsBox.add(nextHintButton);
        panel.add(buttonsBox);

        panel.add(Box.createVerticalStrut(5));

        JOptionPane.showOptionDialog(component, panel, applicationName, JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, null, null);
    }

    void goToNextHint() {
        hintArea.setHint(hintsSupport.getNextHint());
        updateHintsLabel();
    }

    void goToPreviousHint() {
        hintArea.setHint(hintsSupport.getPreviousHint());
        updateHintsLabel();
    }

    private void updateHintsLabel() {
        hintIndexLabel.setText(String.format(bundle.getString("hintIndexOfTotal"), hintsSupport.getCurrentHintIndex() + 1, hintsSupport.getTotalHints()));
    }

    // For test purposes only
    String getIndexLabelText() {
        return hintIndexLabel.getText();
    }

    // For test purposes only
    String getHintText() {
        return hintArea.getText();
    }
}
