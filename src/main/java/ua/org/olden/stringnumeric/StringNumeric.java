package ua.org.olden.stringnumeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringNumeric extends Number implements Comparable<StringNumeric> {

    private static final Pattern VALUE_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?");

    private final String digits; // all significant digits without decimal point
    private final int scale;     // number of decimal places (0 = integer)

    public StringNumeric(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value must not be null or blank");
        }
        Matcher m = VALUE_PATTERN.matcher(value);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Value must consist of digits with optional dot: " + value);
        }
        String intPart  = m.group(1);
        String fracPart = m.group(2) != null ? m.group(2) : "";
        String d = stripLeadingZeros(intPart + fracPart);
        int    s = fracPart.length();
        // strip trailing fractional zeros (e.g. "1.50" → "1.5")
        while (s > 0 && d.charAt(d.length() - 1) == '0') {
            d = d.substring(0, d.length() - 1);
            s--;
        }
        if (d.isEmpty()) d = "0";
        this.digits = d;
        this.scale  = s;
    }

    public StringNumeric(int value) {
        this((long) value);
    }

    public StringNumeric(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative values are not supported yet");
        }
        this.digits = Long.toString(value);
        this.scale  = 0;
    }

    /**
     * Creates a StringNumeric from a {@code double}, rounded to {@code scale} decimal places.
     * An explicit scale is required to avoid floating-point representation surprises
     * (e.g. {@code 0.1 + 0.2 == 0.30000000000000004}).
     */
    public StringNumeric(double value, int scale) {
        this(formatFloating(value, scale));
    }

    /** @see #StringNumeric(double, int) */
    public StringNumeric(float value, int scale) {
        this(formatFloating(value, scale));
    }

    private static String formatFloating(double value, int scale) {
        if (value < 0) throw new IllegalArgumentException("Negative values are not supported yet");
        if (scale < 0) throw new IllegalArgumentException("Scale must be non-negative");
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .toPlainString();
    }

    // Private constructor used by arithmetic methods (digits/scale already normalised).
    private StringNumeric(String digits, int scale) {
        this.digits = digits;
        this.scale  = scale;
    }

    // --- add ---

    public StringNumeric add(StringNumeric other) {
        return add(other, false);
    }

    public StringNumeric add(StringNumeric other, boolean visualize) {
        int maxScale = Math.max(this.scale, other.scale);

        // align decimal places: pad the shorter fractional part with trailing zeros
        String a = this.digits  + "0".repeat(maxScale - this.scale);
        String b = other.digits + "0".repeat(maxScale - other.scale);

        int maxLen = Math.max(a.length(), b.length());
        String pa = "0".repeat(maxLen - a.length()) + a;
        String pb = "0".repeat(maxLen - b.length()) + b;

        // carryInto[k] = carry entering column k from the right (0 = units column)
        int[] carryInto   = new int[maxLen + 1];
        int[] resultDigits = new int[maxLen + 1]; // [0] = potential leading digit

        for (int col = 0; col < maxLen; col++) {
            int i   = maxLen - 1 - col;
            int sum = (pa.charAt(i) - '0') + (pb.charAt(i) - '0') + carryInto[col];
            resultDigits[maxLen - col] = sum % 10;
            carryInto[col + 1]         = sum / 10;
        }
        resultDigits[0] = carryInto[maxLen];

        StringBuilder sb = new StringBuilder();
        if (resultDigits[0] > 0) sb.append((char) ('0' + resultDigits[0]));
        for (int i = 1; i <= maxLen; i++) sb.append((char) ('0' + resultDigits[i]));
        String resultStr = sb.toString();

        if (visualize) {
            System.out.println(buildAddVisualization(a, b, resultStr, carryInto, maxLen, maxScale));
        }

        return normalize(resultStr, maxScale);
    }

    /**
     * Builds a column-style visualization of addition, for example:
     * <pre>
     *  1
     *  48.12
     * +15.67
     *  -----
     *  63.79
     * </pre>
     */
    private static String buildAddVisualization(String aRaw, String bRaw, String resultRaw,
                                                int[] carryInto, int maxLen, int scale) {
        // reinsert decimal point for display only
        String a      = insertDot(aRaw,      scale);
        String b      = insertDot(bRaw,      scale);
        String result = insertDot(resultRaw, scale);

        int dw = Math.max(Math.max(a.length(), b.length()), result.length());

        // Place each non-zero carry above the column it is being added into.
        // Raw col → display position from right:
        //   col <  scale : same position (dot is to the left, no shift yet)
        //   col >= scale : +1 because the dot character sits between them
        char[] carryChars = new char[dw];
        Arrays.fill(carryChars, ' ');
        for (int col = 1; col <= maxLen; col++) {
            if (carryInto[col] > 0) {
                int dotShift = (scale > 0 && col >= scale) ? 1 : 0;
                int idx = dw - 1 - col - dotShift;
                if (idx >= 0) carryChars[idx] = (char) ('0' + carryInto[col]);
            }
        }

        String carryLine = (" " + new String(carryChars)).stripTrailing();

        StringBuilder vis = new StringBuilder();
        if (!carryLine.isBlank()) {
            vis.append(carryLine).append('\n');
        }
        vis.append(' ').append(padLeft(a,      dw)).append('\n');
        vis.append('+').append(padLeft(b,      dw)).append('\n');
        vis.append(' ').repeat("-", dw).append('\n');
        vis.append(' ').append(padLeft(result, dw));

        return vis.toString();
    }

    // --- sub ---

    public StringNumeric sub(StringNumeric other) {
        return sub(other, false);
    }

    public StringNumeric sub(StringNumeric other, boolean visualize) {
        if (this.compareTo(other) < 0) {
            throw new IllegalArgumentException(
                    "Subtraction would produce a negative result, which is not supported");
        }

        int maxScale = Math.max(this.scale, other.scale);

        // align decimal places: pad the shorter fractional part with trailing zeros
        String a = this.digits  + "0".repeat(maxScale - this.scale);
        String b = other.digits + "0".repeat(maxScale - other.scale);

        int maxLen = Math.max(a.length(), b.length());
        String pa = "0".repeat(maxLen - a.length()) + a;
        String pb = "0".repeat(maxLen - b.length()) + b;

        // incomingBorrow[k] = borrow entering column k from the right (0 = units column)
        int[] incomingBorrow = new int[maxLen + 1];
        int[] resultDigits   = new int[maxLen];

        for (int col = 0; col < maxLen; col++) {
            int i    = maxLen - 1 - col;
            int diff = (pa.charAt(i) - '0') - (pb.charAt(i) - '0') - incomingBorrow[col];
            if (diff < 0) {
                diff += 10;
                incomingBorrow[col + 1] = 1;
            }
            resultDigits[i] = diff;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLen; i++) sb.append((char) ('0' + resultDigits[i]));
        String resultStr = sb.toString();

        if (visualize) {
            System.out.println(buildSubVisualization(a, b, resultStr, incomingBorrow, maxLen, maxScale));
        }

        return normalize(resultStr, maxScale);
    }

    /**
     * Builds a column-style visualization of subtraction, for example:
     * <pre>
     *  1
     *  52
     * -27
     *  --
     *  25
     * </pre>
     * The {@code 1} above a digit marks that it was borrowed from (effectively reduced by 1).
     */
    private static String buildSubVisualization(String aRaw, String bRaw, String resultRaw,
                                                 int[] incomingBorrow, int maxLen, int scale) {
        String a      = insertDot(aRaw,                         scale);
        String b      = insertDot(bRaw,                         scale);
        String result = insertDot(stripLeadingZeros(resultRaw), scale);

        int dw = Math.max(Math.max(a.length(), b.length()), result.length());

        // Place a '1' above each column that was borrowed from.
        // incomingBorrow[col] = 1 means the digit of `a` at column col was reduced by 1.
        char[] borrowChars = new char[dw];
        Arrays.fill(borrowChars, ' ');
        for (int col = 1; col <= maxLen; col++) {
            if (incomingBorrow[col] > 0) {
                int dotShift = (scale > 0 && col >= scale) ? 1 : 0;
                int idx = dw - 1 - col - dotShift;
                if (idx >= 0) borrowChars[idx] = '1';
            }
        }

        String borrowLine = (" " + new String(borrowChars)).stripTrailing();

        StringBuilder vis = new StringBuilder();
        if (!borrowLine.isBlank()) {
            vis.append(borrowLine).append('\n');
        }
        vis.append(' ').append(padLeft(a,      dw)).append('\n');
        vis.append('-').append(padLeft(b,      dw)).append('\n');
        vis.append(' ').repeat("-", dw).append('\n');
        vis.append(' ').append(padLeft(result, dw));

        return vis.toString();
    }

    // --- Number ---

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public long longValue() {
        return new BigInteger(digits)
                .divide(BigInteger.TEN.pow(scale))
                .longValueExact();
    }

    @Override
    public float floatValue() {
        return Float.parseFloat(toString());
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(toString());
    }

    // --- Comparable ---

    @Override
    public int compareTo(StringNumeric other) {
        int maxScale = Math.max(this.scale, other.scale);
        String a = this.digits  + "0".repeat(maxScale - this.scale);
        String b = other.digits + "0".repeat(maxScale - other.scale);
        if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
        return a.compareTo(b);
    }

    // --- Object ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringNumeric sn)) return false;
        return scale == sn.scale && digits.equals(sn.digits);
    }

    @Override
    public int hashCode() {
        return 31 * digits.hashCode() + scale;
    }

    @Override
    public String toString() {
        return insertDot(digits, scale);
    }

    // --- helpers ---

    /** Inserts a decimal point {@code scale} places from the right, or returns {@code s} as-is. */
    private static String insertDot(String s, int scale) {
        if (scale == 0) return s;
        int intLen = s.length() - scale;
        if (intLen > 0) {
            return s.substring(0, intLen) + "." + s.substring(intLen);
        }
        return "0." + "0".repeat(-intLen) + s;
    }

    /** Strips trailing fractional zeros and wraps into a StringNumeric. */
    private static StringNumeric normalize(String rawDigits, int scale) {
        String d = stripLeadingZeros(rawDigits);
        int    s = scale;
        while (s > 0 && d.charAt(d.length() - 1) == '0') {
            d = d.substring(0, d.length() - 1);
            s--;
        }
        if (d.isEmpty()) d = "0";
        return new StringNumeric(d, s);
    }

    private static String padLeft(String s, int width) {
        if (s.length() >= width) return s;
        return " ".repeat(width - s.length()) + s;
    }

    private static String stripLeadingZeros(String s) {
        int i = 0;
        while (i < s.length() - 1 && s.charAt(i) == '0') i++;
        return s.substring(i);
    }
}
