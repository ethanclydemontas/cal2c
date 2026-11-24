package bucs.montas.cal2c;

public class BackEnd
{
    private Calculator calculator;

    public BackEnd(Calculator calculator)
    {
        this.calculator = calculator;
    }

    public void initialize()
    {
        handleMemory("MC", 0);
        handleReset("AC", 0);
    }

    void process(String command)
    {
        if (calculator.isThemeMode)
        {
            calculator.isThemeMode = !command.equals("AC");
            calculator.front.updateUI(command.equals("C") ? "THEME" : null);
            return;
        }
        if (!calculator.isOn && !command.equals("AC"))
        {
            return;
        }
        calculator.isOn = true;
        boolean isDigit = command.length() == 1 && Character.isDigit(command.charAt(0));
        if (calculator.valueString.equals(calculator.errorValue) && !command.equals("AC") && !command.equals("C"))
        {
            if (!isDigit && !command.equals("."))
            {
                return;
            }
            handleReset("AC", 0);
        }
        double currentValue = 0;
        try
        {
            if (calculator.valueString.contains("'"))
            {
                String[] parts = calculator.valueString.replace("\"", "").replace("-", " ").trim().split("[-' ]+");
                double value = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]) / 60.0 + Double.parseDouble(parts[2]) / 3600.0;
                currentValue = calculator.valueString.startsWith("-") ? -value : value;
            }
            else
            {
                currentValue = Double.parseDouble(calculator.valueString);
            }
        }
        catch (Exception e)
        {
        }
        if (calculator.isTaxMode)
        {
            if (command.equals("%") || command.equals("TAX+") || command.equals("TAX-"))
            {
                handleTax(command, currentValue);
                calculator.front.updateUI(null);
                return;
            }
            if (!command.equals("AC") && !command.equals("C") && !command.equals(".") && !command.equals("►") && !isDigit)
            {
                return;
            }
        }
        boolean isOperator = command.length() == 1 && "+-×÷⁒^√".indexOf(command.charAt(0)) >= 0;
        if (calculator.historyString.endsWith("=") && !isOperator && !command.equals("TAX") && !command.equals("M+") && !command.equals("M-") && !command.equals("MC"))
        {
            calculator.historyString = calculator.initialHistory;
        }
        else if ((calculator.historyString.endsWith(")") || calculator.historyString.endsWith("²")) && (isDigit || command.equals(".")))
        {
            calculator.historyString = (!calculator.pendingOperator.isEmpty() && calculator.historyString.contains(" ")) ? calculator.historyString.substring(0, calculator.historyString.lastIndexOf(" ")) : calculator.initialHistory;
        }
        switch (command)
        {
            case "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "+/-", "H/M/S" -> handleValue(command, currentValue);
            case "√", "x²", "1/x" -> handleUnary(command, currentValue);
            case "+", "-", "×", "÷", "^", "⁒" -> handleOperator(command, currentValue);
            case "(", ")", "=", "%" -> handleExpression(command, currentValue);
            case "►", "C", "AC" -> handleReset(command, currentValue);
            case "M+", "M-", "MR", "MC" -> handleMemory(command, currentValue);
            case "TAX+", "TAX-", "TAX" -> handleTax(command, currentValue);
        }
        calculator.front.updateUI(null);
    }

    void resolve(boolean isReduce)
    {
        if (isReduce)
        {
            String operator = calculator.operatorStack.pop();
            double right = calculator.valueStack.pop();
            double left = operator.equals("√") || calculator.valueStack.isEmpty() ? 0 : calculator.valueStack.pop();
            if ((operator.equals("÷") || operator.equals("⁒")) && right == 0)
            {
                calculator.valueStack.push(Double.NaN);
                return;
            }
            calculator.valueStack.push(switch (operator)
            {
                case "+" -> left + right;
                case "-" -> left - right;
                case "×" -> left * right;
                case "÷" -> left / right;
                case "⁒" -> left % right;
                case "^" -> Math.pow(left, right);
                case "√" -> Math.sqrt(right);
                default -> right;
            });
            return;
        }
        if (calculator.valueStack.isEmpty())
        {
            try
            {
                String rawExpression = calculator.historyString;
                if (rawExpression.endsWith(")"))
                {
                    for (int index = rawExpression.length() - 1, depth = 0; index >= 0; index--)
                    {
                        depth += rawExpression.charAt(index) == ')' ? 1 : (rawExpression.charAt(index) == '(' ? -1 : 0);
                        if (depth == 0)
                        {
                            int start = index > 1 && rawExpression.startsWith("1/", index - 2) ? index - 2 : (index > 0 && rawExpression.charAt(index - 1) == '√' ? index - 1 : index + 1);
                            rawExpression = rawExpression.substring(start, start > index ? rawExpression.length() - 1 : rawExpression.length());
                            break;
                        }
                    }
                }
                String normalized = rawExpression.replace("=", "").replace("²", " ^ 2").replace("√", " √ ").replace("1/(", "1 ÷ (").replace("(", " ( ").replace(")", " ) ").replaceAll("\\)\\s*\\(", ") × (").trim();
                calculator.operatorStack.clear();
                if (!normalized.isEmpty())
                {
                    for (String token : normalized.split("\\s+"))
                    {
                        int tokenIndex = token.length() == 1 ? "+-×÷⁒^√".indexOf(token.charAt(0)) : -1;
                        if (token.equals("("))
                        {
                            calculator.operatorStack.push(token);
                        }
                        else if (token.equals(")"))
                        {
                            while (!calculator.operatorStack.isEmpty() && !calculator.operatorStack.peek().equals("("))
                            {
                                resolve(true);
                            }
                            if (!calculator.operatorStack.isEmpty())
                            {
                                calculator.operatorStack.pop();
                            }
                        }
                        else if (tokenIndex >= 0)
                        {
                            int tokenRank = tokenIndex < 2 ? 1 : (tokenIndex < 5 ? 2 : tokenIndex - 2);
                            while (!calculator.operatorStack.isEmpty())
                            {
                                String top = calculator.operatorStack.peek();
                                int topIndex = top.length() == 1 ? "+-×÷⁒^√".indexOf(top.charAt(0)) : -1;
                                int topRank = topIndex < 0 ? 0 : (topIndex < 2 ? 1 : (topIndex < 5 ? 2 : topIndex - 2));
                                if (topRank < tokenRank || (token.equals("^") && top.equals("^")))
                                {
                                    break;
                                }
                                resolve(true);
                            }
                            calculator.operatorStack.push(token);
                        }
                        else
                        {
                            calculator.valueStack.push(Double.parseDouble(token));
                        }
                    }
                    while (!calculator.operatorStack.isEmpty())
                    {
                        resolve(true);
                    }
                }
            }
            catch (Exception e)
            {
                calculator.valueStack.clear();
                calculator.operatorStack.clear();
            }
        }
        double value = calculator.valueStack.isEmpty() ? 0 : calculator.valueStack.pop();
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            calculator.valueString = calculator.errorValue;
        }
        else if (!calculator.valueString.equals(calculator.errorValue))
        {
            format(value, false);
            calculator.valueString = calculator.formattedValue;
        }
        calculator.isNewEntry = true;
    }

    void format(double value, boolean isSpaced)
    {
        if (isSpaced && (calculator.historyString.endsWith(")") || calculator.historyString.endsWith("²")))
        {
            calculator.formattedValue = "";
            return;
        }
        String formatted;
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            formatted = calculator.errorValue;
        }
        else if (Math.abs(value) >= calculator.scientificNotationUpperThreshold || (Math.abs(value) < calculator.scientificNotationLowerThreshold && value != 0))
        {
            formatted = String.format(calculator.scientificNotationFormat, value);
        }
        else if (value == 0)
        {
            formatted = "0";
        }
        else
        {
            formatted = String.format("%." + calculator.normalDecimalScale + "f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
            if (formatted.length() - (formatted.startsWith("-") ? 1 : 0) - (formatted.contains(".") ? 1 : 0) > calculator.maximumValueDigits)
            {
                formatted = String.format("%." + calculator.scientificNotationDecimalScale + "f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
            }
        }
        calculator.formattedValue = (!isSpaced || calculator.historyString.isEmpty() || calculator.historyString.endsWith("(")) ? formatted : (formatted.startsWith("-") ? " (" + formatted + ")" : " " + formatted);
    }

    void handleValue(String token, double currentValue)
    {
        if (token.equals("+/-"))
        {
            if (currentValue != 0 || calculator.valueString.contains("."))
            {
                calculator.valueString = calculator.valueString.startsWith("-") ? calculator.valueString.substring(1) : "-" + calculator.valueString;
            }
            return;
        }
        if (token.equals("H/M/S"))
        {
            long seconds = Math.round(Math.abs(currentValue) * 3600);
            format(currentValue, false);
            calculator.valueString = calculator.valueString.contains("'") ? calculator.formattedValue : (currentValue < 0 ? "-" : "") + String.format(calculator.hmsFormat, seconds / 3600, (seconds % 3600) / 60, seconds % 60);
            return;
        }
        if (token.equals("."))
        {
            if (calculator.isNewEntry)
            {
                calculator.valueString = calculator.initialDecimal;
                calculator.isNewEntry = false;
            }
            else if (!calculator.valueString.contains(".") && calculator.valueString.length() - (calculator.valueString.startsWith("-") ? 1 : 0) < calculator.maximumValueDigits)
            {
                calculator.valueString += ".";
            }
            return;
        }
        if (calculator.valueString.equals(calculator.initialValue) && token.equals("0"))
        {
            return;
        }
        if (calculator.isNewEntry || calculator.valueString.equals(calculator.initialValue) || calculator.valueString.contains("'"))
        {
            calculator.valueString = token;
            calculator.isNewEntry = false;
        }
        else if (calculator.valueString.length() - (calculator.valueString.startsWith("-") ? 1 : 0) - (calculator.valueString.contains(".") ? 1 : 0) < calculator.maximumValueDigits)
        {
            calculator.valueString += token;
        }
    }

    void handleUnary(String operator, double currentValue)
    {
        format(currentValue, false);
        boolean isComplete = calculator.historyString.isEmpty() || calculator.historyString.endsWith("=") || calculator.historyString.endsWith(")") || calculator.historyString.endsWith("²");
        String prefix = isComplete ? "" : calculator.historyString + (calculator.historyString.endsWith("(") ? "" : " ");
        calculator.historyString = prefix + (operator.equals("x²") ? "(" + calculator.formattedValue + ")²" : (operator.equals("1/x") ? "1/(" : "√(") + calculator.formattedValue + ")");
        if ((operator.equals("√") && currentValue < 0) || (operator.equals("1/x") && currentValue == 0))
        {
            calculator.valueStack.push(Double.NaN);
        }
        else
        {
            calculator.valueStack.push(switch (operator)
            {
                case "√" -> Math.sqrt(currentValue);
                case "x²" -> currentValue * currentValue;
                case "1/x" -> 1.0 / currentValue;
                default -> currentValue;
            });
        }
        resolve(false);
    }

    void handleOperator(String operator, double currentValue)
    {
        if (calculator.historyString.endsWith("="))
        {
            format(currentValue, false);
            calculator.historyString = calculator.formattedValue + " " + operator;
        }
        else if (!calculator.pendingOperator.isEmpty() && calculator.isNewEntry && !calculator.historyString.endsWith(")") && !calculator.historyString.endsWith("²"))
        {
            if (operator.equals(calculator.pendingOperator))
            {
                calculator.constantOperand = currentValue;
                calculator.constantOperator = operator;
                calculator.historyString = calculator.initialHistory;
                return;
            }
            calculator.historyString = (calculator.historyString.contains(" ") ? calculator.historyString.substring(0, calculator.historyString.lastIndexOf(" ")) : calculator.historyString) + " " + operator;
        }
        else
        {
            format(currentValue, true);
            calculator.historyString += calculator.formattedValue + " " + operator;
        }
        calculator.constantOperator = "";
        calculator.firstOperand = currentValue;
        calculator.pendingOperator = operator;
        calculator.isNewEntry = true;
    }

    void handleExpression(String command, double currentValue)
    {
        if (command.equals("("))
        {
            if (calculator.parenthesisDepth >= calculator.maximumParenthesisDepth)
            {
                return;
            }
            if (calculator.historyString.endsWith(")") || calculator.historyString.endsWith("²") || (!calculator.isNewEntry && !calculator.valueString.equals(calculator.initialValue)))
            {
                format(currentValue, true);
                calculator.historyString += calculator.formattedValue + " ×";
            }
            calculator.parenthesisDepth++;
            calculator.historyString += (calculator.historyString.isEmpty() || calculator.historyString.endsWith("=") ? "" : " ") + "(";
            calculator.pendingOperator = "";
            calculator.isNewEntry = true;
            return;
        }
        if (command.equals(")"))
        {
            if (calculator.parenthesisDepth <= 0)
            {
                return;
            }
            format(currentValue, true);
            calculator.historyString += calculator.formattedValue + ")";
            calculator.parenthesisDepth--;
            calculator.valueStack.clear();
            resolve(false);
            return;
        }
        if (command.equals("="))
        {
            if (!calculator.constantOperator.isEmpty())
            {
                format(currentValue, false);
                String leftText = calculator.formattedValue;
                format(calculator.constantOperand, false);
                calculator.historyString = leftText + " " + calculator.constantOperator + " " + calculator.formattedValue + " =";
            }
            else
            {
                while (calculator.parenthesisDepth > 0)
                {
                    handleExpression(")", currentValue);
                }
                format(currentValue, true);
                calculator.historyString += calculator.formattedValue + " =";
                calculator.pendingOperator = "";
            }
            calculator.valueStack.clear();
            resolve(false);
            return;
        }
        if (command.equals("%"))
        {
            if (calculator.pendingOperator.isEmpty())
            {
                format(currentValue, false);
                calculator.historyString = calculator.formattedValue + "% =";
                calculator.valueStack.push(currentValue / calculator.percentDivisor);
            }
            else if ((calculator.pendingOperator.equals("÷") || calculator.pendingOperator.equals("⁒")) && currentValue == 0)
            {
                calculator.valueStack.push(Double.NaN);
            }
            else
            {
                double percentage = calculator.firstOperand * (currentValue / calculator.percentDivisor);
                format(calculator.firstOperand, false);
                String leftText = calculator.formattedValue;
                format(currentValue, false);
                calculator.historyString = leftText + " " + calculator.pendingOperator + " " + calculator.formattedValue + "% =";
                calculator.valueStack.push(switch (calculator.pendingOperator)
                {
                    case "+" -> calculator.firstOperand + percentage;
                    case "-" -> calculator.firstOperand - percentage;
                    case "÷" -> calculator.firstOperand * calculator.percentDivisor / currentValue;
                    case "^" -> Math.pow(calculator.firstOperand, currentValue / calculator.percentDivisor);
                    case "⁒" -> calculator.firstOperand % (currentValue / calculator.percentDivisor);
                    default -> percentage;
                });
                calculator.pendingOperator = "";
            }
            resolve(false);
        }
    }

    void handleReset(String command, double currentValue)
    {
        if (command.equals("►"))
        {
            if (!calculator.isNewEntry && calculator.valueString.length() > 0)
            {
                format(currentValue, false);
                String current = calculator.valueString.contains("'") ? calculator.formattedValue : calculator.valueString;
                calculator.valueString = current.substring(0, current.length() - 1);
                if (calculator.valueString.isEmpty() || calculator.valueString.equals("-"))
                {
                    calculator.valueString = calculator.initialValue;
                    calculator.isNewEntry = true;
                }
            }
            return;
        }
        calculator.valueString = calculator.initialValue;
        calculator.isNewEntry = true;
        if (!command.equals("C"))
        {
            calculator.isTaxMode = calculator.isThemeMode = false;
            calculator.constantOperator = calculator.pendingOperator = "";
            calculator.historyString = calculator.initialHistory;
            calculator.constantOperand = calculator.firstOperand = calculator.parenthesisDepth = 0;
            calculator.valueStack.clear();
            calculator.operatorStack.clear();
        }
    }

    void handleMemory(String command, double currentValue)
    {
        switch (command)
        {
            case "M+" -> calculator.memoryValue += currentValue;
            case "M-" -> calculator.memoryValue -= currentValue;
            case "MR" ->
            {
                calculator.valueStack.push(calculator.memoryValue);
                resolve(false);
            }
            case "MC" -> calculator.memoryValue = 0;
        }
        calculator.isNewEntry = true;
    }

    void handleTax(String command, double currentValue)
    {
        if (command.equals("TAX"))
        {
            calculator.savedHistory = calculator.historyString;
            calculator.savedValue = calculator.valueString;
            calculator.isTaxMode = true;
            calculator.historyString = calculator.taxRatePrompt;
            calculator.valueStack.push(calculator.taxRate * 100.0);
            resolve(false);
            return;
        }
        if (calculator.isTaxMode)
        {
            if (command.equals("%"))
            {
                calculator.taxRate = currentValue / 100.0;
                format(calculator.taxRate * 100.0, false);
                calculator.historyString = "RATE " + calculator.formattedValue + "% =";
            }
            else
            {
                calculator.historyString = calculator.savedHistory;
                calculator.valueString = calculator.savedValue;
            }
            calculator.isTaxMode = false;
            calculator.isNewEntry = true;
            return;
        }
        format(currentValue, false);
        calculator.historyString = command + "(" + calculator.formattedValue + ") =";
        calculator.valueStack.push(command.equals("TAX+") ? currentValue * (1.0 + calculator.taxRate) : currentValue / (1.0 + calculator.taxRate));
        resolve(false);
    }
}