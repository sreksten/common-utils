package com.threeamigos.common.util.ui.effects.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BorderedStringRenderer unit test")
@Tag("unit")
@EnabledIfEnvironmentVariable(named = "AWT_TESTS", matches = "true", disabledReason = "Environment variable AWT_TESTS is not true")
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = "java.lang.System#properties", mode = ResourceAccessMode.READ_WRITE)
class BorderedStringRendererUnitTest {

    @Test
    @DisplayName("Test rendering of bordered string")
    void shouldRenderABorderedString() {
        // Given
        final int width = 400;
        final int height = 300;
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = newImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        int fontHeight = height / 10;
        Font font = new Font("Arial", Font.BOLD, fontHeight);
        g2d.setFont(font);
        int y = (height - fontHeight) / 2;
        BorderedStringRenderer.drawString(g2d, "TEST", 10, y, Color.BLACK, Color.RED);
        g2d.dispose();

        // When
        // Use JOptionPane to show the image and ask for confirmation
        int response = JOptionPane.showConfirmDialog(
                null,
                new JLabel(new ImageIcon(newImage)),
                "Do you see a TEST message in red bordered in black?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        // Then
        assertEquals(JOptionPane.YES_OPTION, response, "User indicated the image was NOT rendered correctly.");
    }
}