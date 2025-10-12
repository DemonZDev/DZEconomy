package online.demonzdevelopment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtil {
    public static BigDecimal parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        try {
            input = input.trim().toUpperCase();
            
            String suffix = "";
            String numberPart = input;
            
            for (int i = input.length() - 1; i >= 0; i--) {
                char c = input.charAt(i);
                if (!Character.isDigit(c) && c != '.') {
                    suffix = input.substring(i + 1);
                    numberPart = input.substring(0, i + 1);
                    break;
                }
            }
            
            BigDecimal base = new BigDecimal(numberPart);
            
            if (suffix.isEmpty()) {
                return base.setScale(2, RoundingMode.DOWN);
            }
            
            BigDecimal multiplier = getMultiplier(suffix);
            if (multiplier == null) {
                return null;
            }
            
            return base.multiply(multiplier).setScale(2, RoundingMode.DOWN);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal getMultiplier(String suffix) {
        return switch (suffix) {
            case "K" -> BigDecimal.valueOf(1_000);
            case "M" -> BigDecimal.valueOf(1_000_000);
            case "B" -> BigDecimal.valueOf(1_000_000_000);
            case "T" -> BigDecimal.valueOf(1_000_000_000_000L);
            case "Q" -> new BigDecimal("1000000000000000");
            case "QI" -> new BigDecimal("1000000000000000000");
            case "S" -> new BigDecimal("1000000000000000000000");
            case "SP" -> new BigDecimal("1000000000000000000000000");
            case "O" -> new BigDecimal("1000000000000000000000000000");
            case "N" -> new BigDecimal("1000000000000000000000000000000");
            case "D" -> new BigDecimal("1000000000000000000000000000000000");
            default -> null;
        };
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNonNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0 ? a : b;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) < 0 ? a : b;
    }
}