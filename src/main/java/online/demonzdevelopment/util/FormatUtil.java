package online.demonzdevelopment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormatUtil {
    private static final String[] SUFFIXES = {
        "", "K", "M", "B", "T", "Q", "Qi", "S", "Sp", "O", "N", "D",
        "Ud", "Dd", "Td", "Qd", "Qnd", "Sd", "Spd", "Od", "Nd", "V",
        "Uv", "Dv", "Tv", "Qv", "Qnv", "Sv", "Spv", "Ov", "Nv", "Tr",
        "Utr", "Dtr", "G"
    };

    public static String format(BigDecimal amount, boolean useShortForm, int decimalLimit) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        
        amount = amount.setScale(decimalLimit, RoundingMode.DOWN);
        
        if (!useShortForm) {
            return amount.toPlainString();
        }
        
        if (amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return amount.toPlainString();
        }
        
        double value = amount.doubleValue();
        int suffixIndex = 0;
        
        while (value >= 1000 && suffixIndex < SUFFIXES.length - 1) {
            value /= 1000;
            suffixIndex++;
        }
        
        BigDecimal formatted = BigDecimal.valueOf(value).setScale(decimalLimit, RoundingMode.DOWN);
        String result = formatted.toPlainString();
        
        if (result.contains(".")) {
            result = result.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        
        return result + SUFFIXES[suffixIndex];
    }

    public static String formatCurrency(BigDecimal amount, boolean useShortForm, int decimalLimit) {
        return format(amount, useShortForm, decimalLimit);
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } else {
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + "d " + hours + "h";
        }
    }
}