package com.threeamigos.common.util.implementations.ui;

import com.threeamigos.common.util.interfaces.ui.HintsDisplayer;

import javax.swing.*;
import java.awt.*;

public class HintsWindowImpl implements HintsDisplayer {

    private final HintsSupport hintsSupport;
    private final String applicationName;
    private final JLabel hintIndexLabel;
    private final JHintArea hintArea;

    public HintsWindowImpl(String applicationName, HintsSupport hintsSupport) {
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
        hintsBorderPanel.setBorder(BorderFactory.createTitledBorder("Hint"));
        panel.add(hintsBorderPanel);

        hintArea.setPreferredSize(new Dimension(400, 100));
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
        showHintsBox.add(new JLabel("Show hints at startup"));
        showHintsBox.add(Box.createGlue());
        panel.add(showHintsBox);

        panel.add(Box.createVerticalStrut(5));

        Box buttonsBox = Box.createHorizontalBox();
        Dimension buttonsDimension = new Dimension(150, 40);
        JButton previousHintButton = new JButton("Previous hint");
        previousHintButton.setPreferredSize(buttonsDimension);
        previousHintButton.addActionListener(e -> goToPreviousHint());
        buttonsBox.add(previousHintButton);
        buttonsBox.add(Box.createHorizontalStrut(10));
        buttonsBox.add(hintIndexLabel);
        buttonsBox.add(Box.createHorizontalStrut(10));
        JButton nextHintButton = new JButton("Next hint");
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
        hintIndexLabel.setText(String.format("(%d/%d)", hintsSupport.getCurrentHintIndex() + 1, hintsSupport.getTotalHints()));
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
