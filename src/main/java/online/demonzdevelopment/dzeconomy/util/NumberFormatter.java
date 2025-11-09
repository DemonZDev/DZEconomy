package online.demonzdevelopment.dzeconomy.util;

import java.text.DecimalFormat;

/**
 * Utility class for formatting numbers with decimal truncation and short form notation
 */
public class NumberFormatter {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    
    // Short form suffixes
    private static final String[] SUFFIXES = {
        "", "K", "M", "B", "T", "Q", "Qi", "S", "Sp", "O", "N", "D",
        "Ud", "Dd", "Td", "Qd", "Qnd", "Sd", "Spd", "Od", "Nd", "V",
        "Uv", "Dv", "Tv", "Qv", "Qnv", "Sv", "Spv", "Ov", "Nv", "Tr",
        "Utr", "Dtr", "G"
    };
    
    // Powers for each suffix
    private static final double[] POWERS = {
        1.0, 1000.0, 1000000.0, 1000000000.0, 1000000000000.0, 1000000000000000.0,
        1e18, 1e21, 1e24, 1e27, 1e30, 1e33, 1e36, 1e39, 1e42, 1e45, 1e48, 1e51,
        1e54, 1e57, 1e60, 1e63, 1e66, 1e69, 1e72, 1e75, 1e78, 1e81, 1e84, 1e87,
        1e90, 1e93, 1e96, 1e99, 1e100
    };
    
    /**
     * Truncate a number to 2 decimal places
     */
    public static double truncateDecimal(double value) {
        return Math.floor(value * 100) / 100;
    }
    
    /**
     * Format a number in short form notation (1K, 1M, 1B, etc.)
     */
    public static String formatShort(double value) {
        if (value < 1000) {
            return DECIMAL_FORMAT.format(truncateDecimal(value));
        }
        
        // Find the appropriate suffix
        for (int i = SUFFIXES.length - 1; i >= 0; i--) {
            if (value >= POWERS[i]) {
                double shortened = value / POWERS[i];
                double truncated = truncateDecimal(shortened);
                
                // If truncated value rounds to >= 1000, move to next suffix
                if (truncated >= 1000 && i < SUFFIXES.length - 1) {
                    continue;
                }
                
                String formatted = DECIMAL_FORMAT.format(truncated);
                
                // Remove trailing .0 if present
                if (formatted.endsWith(".0")) {
                    formatted = formatted.substring(0, formatted.length() - 2);
                }
                
                return formatted + SUFFIXES[i];
            }
        }
        
        return DECIMAL_FORMAT.format(truncateDecimal(value));
    }
    
    /**
     * Format a number in full form with commas
     */
    public static String formatFull(double value) {
        double truncated = truncateDecimal(value);
        
        // If it's a whole number, format without decimals
        if (truncated == (long) truncated) {
            return String.format("%,d", (long) truncated);
        }
        
        return String.format("%,.2f", truncated);
    }
    
    /**
     * Parse a number from a string (supports K, M, B suffixes)
     */
    public static double parse(String input) throws NumberFormatException {
        if (input == null || input.isEmpty()) {
            throw new NumberFormatException("Input is null or empty");
        }
        
        input = input.trim().toUpperCase();
        
        // Check for suffix
        for (int i = SUFFIXES.length - 1; i > 0; i--) {
            if (input.endsWith(SUFFIXES[i])) {
                String numberPart = input.substring(0, input.length() - SUFFIXES[i].length());
                double number = Double.parseDouble(numberPart);
                return truncateDecimal(number * POWERS[i]);
            }
        }
        
        // No suffix, parse as regular number
        return truncateDecimal(Double.parseDouble(input));
    }
    
    /**
     * Check if a string is a valid number
     */
    public static boolean isValidNumber(String input) {
        try {
            parse(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}