package bucs.montas.cal2c;

public class FrontEnd
{
    private Calculator calculator;

    public FrontEnd(Calculator calculator)
    {
        this.calculator = calculator;
    }

    public void initialize()
    {
        calculator.initialize();
        buildUI();
        remapRegion();
        calculator.window.setLocationRelativeTo(null);
    }

    void buildUI()
    {
        calculator.window.setUndecorated(true);
        calculator.window.setBackground(calculator.transparentColor);
        calculator.window.setDefaultCloseOperation(calculator.closeOperation);
        calculator.holdTimer.setRepeats(false);
        calculator.contentPanel.setOpaque(false);
        calculator.contentPanel.addMouseListener(calculator.dragAdapter);
        calculator.contentPanel.addMouseMotionListener(calculator.dragAdapter);
        calculator.window.setFocusable(true);
        calculator.window.setAlwaysOnTop(calculator.isPinned);
        calculator.window.setContentPane(calculator.contentPanel);
        for (int index = 0; index < calculator.solarButtons.length; index++)
        {
            var button = calculator.solarButtons[index];
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setBorder(null);
            button.addMouseListener(calculator.dragAdapter);
            button.addMouseMotionListener(calculator.dragAdapter);
            button.addChangeListener(event -> updateUI(null));
            calculator.contentPanel.add(button);
        }
        calculator.pinButton.addActionListener(event -> updateUI("PIN"));
        calculator.zoomButton.addActionListener(event -> updateUI("ZOOM"));
        calculator.closeButton.addActionListener(event -> System.exit(0));
        for (int row = 0; row < calculator.keypadGrid.length; row++)
        {
            for (int column = 0; column < calculator.keypadGrid[row].length; column++)
            {
                String key = calculator.keypadGrid[row][column];
                var button = calculator.keypadButtons[row][column];
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
                button.setBorder(null);
                button.addMouseListener(calculator.dragAdapter);
                button.addMouseMotionListener(calculator.dragAdapter);
                button.getModel().addChangeListener(event ->
                {
                    if (button.getModel().isPressed())
                    {
                        handleButton(key);
                    }
                    else
                    {
                        calculator.holdTimer.stop();
                    }
                });
                button.addActionListener(event ->
                {
                    if (!calculator.longPress)
                    {
                        calculator.back.process(key);
                    }
                });
                calculator.contentPanel.add(button);
            }
        }
        calculator.contentPanel.add(calculator.backgroundLabel);
    }

    void updateUI(String action)
    {
        if (action != null)
        {
            switch (action)
            {
                case "MODE" ->
                {
                    calculator.isOn = true;
                    calculator.isThemeMode = true;
                }
                case "THEME" ->
                {
                    calculator.frameIndex++;
                    calculator.lastDisplayState = "";
                    calculator.initialize();
                    remapRegion();
                    return;
                }
                case "PIN" ->
                {
                    calculator.isPinned = !calculator.isPinned;
                    calculator.window.setAlwaysOnTop(calculator.isPinned);
                }
                case "ZOOM" ->
                {
                    calculator.isZoomed = !calculator.isZoomed;
                    remapRegion();
                    return;
                }
            }
        }
        calculator.window.setTitle(calculator.isOn ? calculator.applicationTitle : "");
        if (!calculator.isOn)
        {
            drawDisplay("", "", "");
            return;
        }
        String statusText = "";
        if (!calculator.isThemeMode)
        {
            if (calculator.memoryValue != 0)
            {
                statusText += calculator.memoryStatus;
            }
            if (!calculator.constantOperator.isEmpty())
            {
                statusText += (statusText.isEmpty() ? "" : " ") + calculator.constantStatus;
            }
            if (calculator.isTaxMode)
            {
                statusText += (statusText.isEmpty() ? "" : " ") + calculator.taxStatus;
            }
        }
        String historyText = calculator.isThemeMode ? calculator.themeHistory : calculator.historyString;
        String valueText = calculator.isThemeMode ? calculator.themeValue : calculator.valueString;
        drawDisplay(statusText, historyText, valueText);
    }

    void render(boolean isHistory, String text, double scale, int padding)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }
        int rawX = isHistory ? calculator.screenHistoryX : calculator.screenValueX;
        int rawY = isHistory ? calculator.screenHistoryY : calculator.screenValueY;
        int rawWidth = isHistory ? calculator.screenHistoryWidth : calculator.screenValueWidth;
        int rawHeight = isHistory ? calculator.screenHistoryHeight : calculator.screenValueHeight;
        int fontSize = (int) Math.round((isHistory ? calculator.historyFontSize : calculator.valueFontSize) * scale);
        int maximumWidth = (int) Math.round(rawWidth * scale);
        fitFont(isHistory, fontSize, maximumWidth, text);
        calculator.graphics.setColor(isHistory ? calculator.screenHistoryColor : calculator.screenValueColor);
        var fontMetrics = calculator.graphics.getFontMetrics();
        int textX = (int) Math.round((rawX + rawWidth) * scale) - fontMetrics.stringWidth(text) - padding;
        int textY = (int) Math.round(rawY * scale) + ((int) Math.round(rawHeight * scale) - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();
        calculator.graphics.drawString(text, textX, textY);
    }

    void handleButton(String key)
    {
        if (key == null)
        {
            calculator.longPress = true;
            if (calculator.activeHoldCommand != null)
            {
                if ("THEME".equals(calculator.activeHoldCommand))
                {
                    updateUI("MODE");
                }
                else
                {
                    calculator.back.process(calculator.activeHoldCommand);
                }
            }
            return;
        }
        calculator.activeHoldCommand = switch (key)
        {
            case "AC", "C" -> "THEME";
            case "TAX+", "TAX-" -> "TAX";
            case "√" -> "x²";
            case "÷" -> "1/x";
            case "×" -> "^";
            case "%" -> "⁒";
            case "." -> "(";
            case "=" -> ")";
            default -> null;
        };
        calculator.longPress = false;
        if (calculator.activeHoldCommand != null)
        {
            calculator.holdTimer.restart();
        }
    }

    void remapRegion()
    {
        double scale = calculator.isZoomed ? calculator.zoomedScale : calculator.defaultScale;
        int width = (int) Math.round(Calculator.BASE_WIDTH * scale);
        int height = (int) Math.round(Calculator.BASE_HEIGHT * scale);
        calculator.window.setSize(width, height);
        calculator.contentDimension.setSize(width, height);
        calculator.contentPanel.setPreferredSize(calculator.contentDimension);
        calculator.backgroundLabel.setBounds(0, 0, width, height);
        int solarY = (int) Math.round(calculator.solarY * scale);
        int solarWidth = (int) Math.round(calculator.solarWidth * scale);
        int solarHeight = (int) Math.round(calculator.solarHeight * scale);
        for (int index = 0; index < calculator.solarButtons.length; index++)
        {
            int solarX = (int) Math.round(calculator.solarXs[index] * scale);
            calculator.solarButtons[index].setBounds(solarX, solarY, solarWidth, solarHeight);
        }
        int buttonWidth = (int) Math.round(calculator.keypadButtonWidth * scale);
        for (int row = 0; row < calculator.keypadGrid.length; row++)
        {
            int buttonY = (int) Math.round(calculator.keypadRowsY[row] * scale);
            for (int column = 0; column < calculator.keypadGrid[row].length; column++)
            {
                int rawHeight = calculator.keypadGrid[row][column].equals("+") ? calculator.keypadPlusHeight : calculator.keypadRowsHeight[row];
                calculator.keypadButtons[row][column].setBounds((int) Math.round(calculator.keypadColumnsX[column] * scale), buttonY, buttonWidth, (int) Math.round(rawHeight * scale));
            }
        }
        updateUI(null);
    }

    void drawDisplay(String status, String history, String value)
    {
        if (calculator.frameImage == null || calculator.compositeImage == null)
        {
            return;
        }
        int solarMask = 0;
        for (int index = 0; index < calculator.solarButtons.length; index++)
        {
            var button = calculator.solarButtons[index];
            boolean active = (button == calculator.pinButton && calculator.window.isAlwaysOnTop()) || (button == calculator.zoomButton && calculator.isZoomed) || button.getModel().isPressed();
            solarMask |= active ? (1 << index) : 0;
        }
        String displayState = calculator.isOn + "|" + solarMask + "|" + status + "|" + history + "|" + value;
        if (displayState.equals(calculator.lastDisplayState))
        {
            return;
        }
        calculator.lastDisplayState = displayState;
        double scale = calculator.frameImage.getWidth() / (double) Calculator.BASE_WIDTH;
        calculator.graphics.clearRect(0, 0, calculator.frameImage.getWidth(), calculator.frameImage.getHeight());
        calculator.graphics.drawImage(calculator.frameImage, 0, 0, null);
        calculator.graphics.setFont(calculator.solarFont.deriveFont((float) Math.round(calculator.solarFontSize * scale)));
        var solarMetrics = calculator.graphics.getFontMetrics();
        int solarWidth = (int) Math.round(calculator.solarWidth * scale);
        int solarY = (int) Math.round(calculator.solarY * scale);
        int solarHeight = (int) Math.round(calculator.solarHeight * scale);
        int iconY = solarY + (solarHeight - solarMetrics.getHeight()) / 2 + solarMetrics.getAscent();
        for (int index = 0; index < calculator.solarButtons.length; index++)
        {
            calculator.graphics.setColor((solarMask & (1 << index)) != 0 ? calculator.solarTextActiveColor : calculator.solarTextNormalColor);
            String symbol = calculator.solarSymbols[index];
            int iconX = (int) Math.round(calculator.solarXs[index] * scale) + (solarWidth - solarMetrics.stringWidth(symbol)) / 2;
            calculator.graphics.drawString(symbol, iconX, iconY);
        }
        if (calculator.isOn)
        {
            int textPadding = (int) Math.round(calculator.textPadding * scale);
            if (status != null && !status.isEmpty())
            {
                calculator.graphics.setFont(calculator.statusFont.deriveFont((float) Math.round(calculator.statusFontSize * scale)));
                calculator.graphics.setColor(calculator.screenStatusColor);
                var fontMetrics = calculator.graphics.getFontMetrics();
                int statusX = (int) Math.round(calculator.screenStatusX * scale);
                int statusY = (int) Math.round(calculator.screenStatusY * scale) + ((int) Math.round(calculator.screenStatusHeight * scale) - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();
                calculator.graphics.drawString(status, statusX, statusY);
            }
            render(true, history, scale, textPadding);
            render(false, value, scale, textPadding);
        }
        calculator.backgroundLabel.repaint();
    }

    void fitFont(boolean isHistory, int fontSize, int maximumWidth, String text)
    {
        var baseFont = isHistory ? calculator.historyFont : calculator.valueFont;
        var font = baseFont.deriveFont((float) Math.max(calculator.minimumFontSize, fontSize));
        var fontMetrics = calculator.graphics.getFontMetrics(font);
        int width = fontMetrics.stringWidth(text);
        if (width > maximumWidth && font.getSize() > calculator.minimumFontSize)
        {
            font = font.deriveFont(Math.max(calculator.minimumFontSize, (float) Math.floor(font.getSize() * ((double) maximumWidth / width))));
            fontMetrics = calculator.graphics.getFontMetrics(font);
            while (fontMetrics.stringWidth(text) > maximumWidth && font.getSize() > calculator.minimumFontSize)
            {
                font = font.deriveFont((float) (font.getSize() - 1));
                fontMetrics = calculator.graphics.getFontMetrics(font);
            }
        }
        calculator.graphics.setFont(font);
    }
}