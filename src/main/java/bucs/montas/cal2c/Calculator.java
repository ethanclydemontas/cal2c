package bucs.montas.cal2c;

public class Calculator
{
    public static void main(String[] args)
    {
        new Calculator().window.setVisible(true);
    }

    public Calculator()
    {
        front = new FrontEnd(this);
        back = new BackEnd(this);
        back.initialize();
        front.initialize();
    }

    public void initialize()
    {
        try
        {
            frameImage = javax.imageio.ImageIO.read(getClass().getResource("/" + frameImages[frameIndex %= frameImages.length]));
            window.setIconImage(frameImage);
            if (compositeImage == null)
            {
                compositeImage = new java.awt.image.BufferedImage(frameImage.getWidth(), frameImage.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                graphics = compositeImage.createGraphics();
                graphics.setBackground(transparentColor);
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            }
        }
        catch (Exception e)
        {
            frameImage = null;
        }
        if (backgroundLabel == null)
        {
            backgroundLabel = new javax.swing.JLabel()
            {
                protected void paintComponent(java.awt.Graphics graphics)
                {
                    ((java.awt.Graphics2D) graphics).setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics.drawImage(compositeImage, 0, 0, getWidth(), getHeight(), null);
                }
            };
            dragAdapter = new java.awt.event.MouseAdapter()
            {
                public void mousePressed(java.awt.event.MouseEvent e)
                {
                    isDragging = !(e.getClickCount() == 3 && e.getSource() == contentPanel) && e.getSource() != pinButton && e.getSource() != zoomButton && e.getSource() != closeButton;
                    if (e.getClickCount() == 3 && e.getSource() == contentPanel)
                    {
                        window.setLocationRelativeTo(null);
                    }
                    dragX = e.getXOnScreen() - window.getX();
                    dragY = e.getYOnScreen() - window.getY();
                }
                public void mouseDragged(java.awt.event.MouseEvent e)
                {
                    if (isDragging)
                    {
                        window.setLocation(e.getXOnScreen() - dragX, e.getYOnScreen() - dragY);
                    }
                }
            };
        }
    }

    public static final int BASE_WIDTH = 564, BASE_HEIGHT = 952;
    public FrontEnd front;
    public BackEnd back;
    public int frameIndex = 0, parenthesisDepth = 0, maximumParenthesisDepth = 5, maximumValueDigits = 10, normalDecimalScale = 8, scientificNotationDecimalScale = 4, longPressDurationMilliseconds = 600, dragX, dragY, closeOperation = javax.swing.JFrame.EXIT_ON_CLOSE;
    public int solarFontSize = 35, valueFontSize = 70, historyFontSize = 35, statusFontSize = 20, minimumFontSize = 6, textPadding = 2, fontPlain = java.awt.Font.PLAIN, fontBold = java.awt.Font.BOLD;
    public int solarXs[] = {294, 365, 437}, solarY = 50, solarWidth = 80, solarHeight = 80;
    public int screenValueX = 70, screenValueY = 240, screenValueWidth = 435, screenValueHeight = 80, screenStatusX = 70, screenStatusY = 165, screenStatusWidth = 160, screenStatusHeight = 30, screenHistoryX = 70, screenHistoryY = 170, screenHistoryWidth = 435, screenHistoryHeight = 65;
    public int keypadColumnsX[] = {42, 142, 242, 342, 444}, keypadRowsY[] = {385, 455, 550, 640, 735, 825}, keypadRowsHeight[] = {55, 70, 70, 70, 70, 75}, keypadButtonWidth = 80, keypadPlusHeight = 165;
    public double defaultScale = 0.388, zoomedScale = 0.50, scientificNotationUpperThreshold = 1e10, scientificNotationLowerThreshold = 1e-6, percentDivisor = 100.0, taxRate = 0.10, firstOperand = 0, memoryValue = 0, constantOperand = 0;
    public boolean isPinned = false, isZoomed = false, isOn = false, isThemeMode = false, isTaxMode = false, isNewEntry = true, longPress = false, isDragging = false;
    public String frameImages[] = {"BK.png", "WE.png", "RD.png", "PK.png", "RG.png", "YG.png", "GN.png", "BU.png", "LB.png", "PL.png"};
    public String keypadGrid[][] = {{"H/M/S", "√", "%", "TAX-", "TAX+"}, {"MC", "MR", "M-", "M+", "÷"}, {"+/-", "7", "8", "9", "×"}, {"►", "4", "5", "6", "-"}, {"C", "1", "2", "3", "+"}, {"AC", "0", ".", "="}};
    public String applicationTitle = "made with <3 by ethan", themeHistory = "GO B[AC]K", themeValue = "[C]HOOSE A THEME";
    public String pinSymbol = "📌︎", zoomSymbol = "🔺︎", closeSymbol = "❌︎", solarSymbols[] = {pinSymbol, zoomSymbol, closeSymbol}, solarFontName = "Monospaced", valueFontName = "Monospaced", historyFontName = "Monospaced", statusFontName = "Monospaced";
    public String memoryStatus = "M", constantStatus = "K", taxStatus = "TAX", taxRatePrompt = "TAX RATE %", errorValue = "Err", scientificNotationFormat = "%.4e", hmsFormat = "%d-%02d'%02d\"";
    public String initialHistory = "", initialValue = "0", initialDecimal = "0.", historyString = initialHistory, valueString = initialValue, savedHistory = initialHistory, savedValue = initialValue, pendingOperator = "", constantOperator = "", activeHoldCommand = "", lastDisplayState = "", formattedValue = "";
    public java.awt.Color transparentColor = new java.awt.Color(0, 0, 0, 0), solarTextNormalColor = new java.awt.Color(190, 170, 165, 128), solarTextActiveColor = new java.awt.Color(190, 170, 165, 255), screenValueColor = new java.awt.Color(90, 99, 100), screenHistoryColor = new java.awt.Color(90, 99, 100, 180), screenStatusColor = screenHistoryColor;
    public java.awt.Dimension contentDimension = new java.awt.Dimension();
    public java.awt.Font statusFont = new java.awt.Font(statusFontName, fontBold, statusFontSize), solarFont = new java.awt.Font(solarFontName, fontPlain, solarFontSize), valueFont = new java.awt.Font(valueFontName, fontBold, valueFontSize), historyFont = new java.awt.Font(historyFontName, fontPlain, historyFontSize);
    public java.awt.image.BufferedImage frameImage, compositeImage;
    public java.awt.Graphics2D graphics;
    public java.awt.event.MouseAdapter dragAdapter;
    public java.util.ArrayDeque<Double> valueStack = new java.util.ArrayDeque<>();
    public java.util.ArrayDeque<String> operatorStack = new java.util.ArrayDeque<>();
    public javax.swing.JFrame window = new javax.swing.JFrame();
    public javax.swing.JLabel backgroundLabel;
    public javax.swing.JButton pinButton = new javax.swing.JButton(), zoomButton = new javax.swing.JButton(), closeButton = new javax.swing.JButton(), solarButtons[] = {pinButton, zoomButton, closeButton};
    public javax.swing.JButton keypadButtons[][] = java.util.Arrays.stream(keypadGrid).map(row -> java.util.Arrays.stream(row).map(key -> new javax.swing.JButton()).toArray(javax.swing.JButton[]::new)).toArray(javax.swing.JButton[][]::new);
    public javax.swing.JPanel contentPanel = new javax.swing.JPanel(null);
    public javax.swing.Timer holdTimer = new javax.swing.Timer(longPressDurationMilliseconds, e -> front.handleButton(null));
}