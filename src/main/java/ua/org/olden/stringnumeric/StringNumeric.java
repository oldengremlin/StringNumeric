package ua.org.olden.stringnumeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringNumeric extends Number implements Comparable<StringNumeric> {

    private static final Pattern VALUE_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?");

    private final String digits;   // all significant digits without decimal point
    private final int scale;       // number of decimal places (0 = integer)
    private final boolean negative; // true if value is negative (always false for zero)

    private record ColumnResult(int digit, int nextTransfer) {

    }

    @FunctionalInterface
    private interface ColumnOperation {

        /**
         * Повертає масив з двох елементів: [resultDigit, nextCarry]
         */
        ColumnResult apply(int digitA, int digitB, int incoming);
    }

    public StringNumeric(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value must not be null or blank");
        }
        boolean neg = value.startsWith("-");
        String abs = neg ? value.substring(1) : value;
        Matcher m = VALUE_PATTERN.matcher(abs);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Value must consist of digits with optional dot: " + value);
        }
        String intPart = m.group(1);
        String fracPart = m.group(2) != null ? m.group(2) : "";

        String d = stripLeadingZeros(intPart + fracPart);
        int s = fracPart.length();
        // strip trailing fractional zeros (e.g. "1.50" → "1.5")
        while (s > 0 && !d.isEmpty() && d.charAt(d.length() - 1) == '0') {
            d = d.substring(0, d.length() - 1);
            s--;
        }
        if (d.isEmpty()) {
            d = "0";
            s = 0;
        }

        this.digits = d;
        this.scale = s;
        this.negative = neg && !d.equals("0"); // -0 normalises to 0
    }

    public StringNumeric(int value) {
        this((long) value);
    }

    public StringNumeric(long value) {
        this.negative = value < 0;
        this.digits = Long.toString(Math.abs(value));
        this.scale = 0;
    }

    /**
     * Creates a StringNumeric from a {@code double}, rounded to {@code scale}
     * decimal places. An explicit scale is required to avoid floating-point
     * representation surprises (e.g. {@code 0.1 + 0.2 == 0.30000000000000004}).
     */
    public StringNumeric(double value, int scale) {
        this(formatFloating(value, scale));
    }

    /**
     * @see #StringNumeric(double, int)
     */
    public StringNumeric(float value, int scale) {
        this(formatFloating(value, scale));
    }

    private static String formatFloating(double value, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("Scale must be non-negative");
        }
        String plain = BigDecimal.valueOf(Math.abs(value))
                .setScale(scale, RoundingMode.HALF_UP)
                .toPlainString();
        return value < 0 ? "-" + plain : plain;
    }

    // Private constructor — digits/scale/negative already normalised.
    private StringNumeric(String digits, int scale, boolean negative) {
        this.digits = digits;
        this.scale = scale;
        this.negative = negative;
    }

    // --- sign helpers ---
    /**
     * Returns a new value with the sign flipped; negate(0) == 0.
     */
    public StringNumeric negate() {
        if (isZero()) {
            return this;
        }
        return new StringNumeric(digits, scale, !negative);
    }

    public boolean isZero() {
        return digits.equals("0"); // after normalisation zero always has scale == 0
    }

    // --- add ---
    public StringNumeric add(StringNumeric other) {
        return add(other, false);
    }

    public StringNumeric add(StringNumeric other, boolean visualize) {
        if (this.negative == other.negative) {
            // same sign: add magnitudes, keep sign
            StringNumeric mag = addMagnitudes(this, other, visualize);
            return (this.negative && !mag.isZero())
                   ? new StringNumeric(mag.digits, mag.scale, true)
                   : mag;
        }
        // different signs: subtract smaller magnitude from larger
        int cmp = compareMagnitudes(this, other);
        if (cmp == 0) {
            return new StringNumeric("0", 0, false);
        }
        StringNumeric larger = cmp > 0 ? this : other;
        StringNumeric smaller = cmp > 0 ? other : this;
        StringNumeric mag = subMagnitudes(larger, smaller, visualize);
        return (larger.negative && !mag.isZero())
               ? new StringNumeric(mag.digits, mag.scale, true)
               : mag;
    }

    // --- sub ---
    public StringNumeric sub(StringNumeric other) {
        return sub(other, false);
    }

    /**
     * Subtracts {@code other} from this value. The result may be negative.
     * Delegates to {@code add(other.negate(), visualize)}, so the visualization
     * shows the underlying magnitude operation (addition or subtraction).
     */
    public StringNumeric sub(StringNumeric other, boolean visualize) {
        return add(other.negate(), visualize);
    }

    // --- mul ---
    public StringNumeric mul(StringNumeric other) {
        return mul(other, false);
    }

    /**
     * Multiplies this value by {@code other}. If {@code visualize} is
     * {@code true}, prints schoolbook-style multiplication to
     * {@code System.out}.
     */
    public StringNumeric mul(StringNumeric other, boolean visualize) {
        if (this.isZero() || other.isZero()) {
            return new StringNumeric("0", 0, false);
        }

        boolean resultNegative = this.negative != other.negative;
        StringNumeric mag = multiplyMagnitudes(this, other, visualize);

        return (resultNegative && !mag.isZero())
               ? new StringNumeric(mag.digits, mag.scale, true)
               : mag;
    }

    // --- div ---
    public StringNumeric div(StringNumeric other) {
        return div(other, false);
    }

    /**
     * Divides this value by {@code other}. Throws ArithmeticException if
     * dividing by zero. If {@code visualize} is true, prints long division
     * visualization.
     */
    public StringNumeric div(StringNumeric other, boolean visualize) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if (this.isZero()) {
            return new StringNumeric("0", 0, false);
        }

        boolean resultNegative = this.negative != other.negative;

        StringNumeric mag = divideMagnitudes(this, other, visualize);

        return (resultNegative && !mag.isZero())
               ? new StringNumeric(mag.digits, mag.scale, true)
               : mag;
    }

    // -- magnitudes --
    private static StringNumeric addMagnitudes(StringNumeric a, StringNumeric b, boolean visualize) {
        return magnitudes(a, b, visualize,
                (da, db, cin) -> {
                    int sum = da + db + cin;
                    return new ColumnResult(sum % 10, sum / 10);
                },
                true);
    }

    private static StringNumeric subMagnitudes(StringNumeric a, StringNumeric b, boolean visualize) {
        // precondition: |a| >= |b| вже гарантується в add()
        return magnitudes(a, b, visualize,
                (da, db, bin) -> {
                    int diff = da - db - bin;
                    if (diff < 0) {
                        diff += 10;
                        return new ColumnResult(diff, 1);
                    }
                    return new ColumnResult(diff, 0);
                },
                false);
    }

    private static StringNumeric magnitudes(
            StringNumeric a,
            StringNumeric b,
            boolean visualize,
            ColumnOperation operation,
            boolean isAddition) {   // для вибору візуалізації

        int maxScale = Math.max(a.scale, b.scale);

        // === спільна частина (вирівнювання) ===
        String as = a.digits + "0".repeat(maxScale - a.scale);
        String bs = b.digits + "0".repeat(maxScale - b.scale);
        int maxLen = Math.max(as.length(), bs.length());

        String pa = "0".repeat(maxLen - as.length()) + as;
        String pb = "0".repeat(maxLen - bs.length()) + bs;

        int[] transfer = new int[maxLen + 1];           // для add — carry, для sub — borrow
        int[] resultDigits = new int[maxLen + 1];

        // === єдиний цикл (тепер однаковий для обох операцій) ===
        for (int col = 0; col < maxLen; col++) {
            int i = maxLen - 1 - col;
            int da = pa.charAt(i) - '0';
            int db = pb.charAt(i) - '0';

            ColumnResult res = operation.apply(da, db, transfer[col]);
            resultDigits[maxLen - col] = res.digit();   // результат цифри
            transfer[col + 1] = res.nextTransfer();     // наступний перенос/позика
        }

        resultDigits[0] = transfer[maxLen];       // для sub завжди 0 (precondition |a| >= |b|)

        // === побудова рядка (тепер однакова для add і sub) ===
        StringBuilder sb = new StringBuilder();
        if (resultDigits[0] > 0) {
            sb.append((char) ('0' + resultDigits[0]));
        }
        for (int i = 1; i <= maxLen; i++) {
            sb.append((char) ('0' + resultDigits[i]));
        }
        String resultStr = sb.toString();

        // === візуалізація ===
        if (visualize) {
            if (isAddition) {
                System.out.println(buildAddVisualization(as, bs, resultStr, transfer, maxLen, maxScale));
            } else {
                System.out.println(buildSubVisualization(as, bs, resultStr, transfer, maxLen, maxScale));
                // transfer тут виконує роль incomingBorrow — значення ті самі (0/1)
            }
        }

        return normalize(resultStr, maxScale);
    }

    private static StringNumeric multiplyMagnitudes(StringNumeric a, StringNumeric b, boolean visualize) {
        if (a.isZero() || b.isZero()) {
            if (visualize) {
                System.out.println(buildFullMulVisualization(a.toString(), b.toString(), "0", new String[0]));
            }
            return new StringNumeric("0", 0, false);
        }

        String da = a.digits;
        String db = b.digits;
        int lenA = da.length();
        int lenB = db.length();

        String[] partialProducts = new String[lenB];
        int[] res = new int[lenA + lenB];

        for (int j = lenB - 1; j >= 0; j--) {
            int digitB = db.charAt(j) - '0';
            int shift = lenB - 1 - j;

            if (digitB == 0) {
                partialProducts[shift] = "0";
                continue;
            }

            StringBuilder partial = new StringBuilder();
            int carry = 0;

            for (int i = lenA - 1; i >= 0; i--) {
                int product = (da.charAt(i) - '0') * digitB + carry;
                partial.append(product % 10);
                carry = product / 10;
            }
            while (carry > 0) {
                partial.append(carry % 10);
                carry /= 10;
            }

            String part = partial.reverse().toString();
            partialProducts[shift] = part;

            for (int k = 0; k < part.length(); k++) {
                int digit = part.charAt(part.length() - 1 - k) - '0';
                res[res.length - 1 - k - shift] += digit;
            }
        }

        // carry
        int carry = 0;
        for (int i = res.length - 1; i >= 0; i--) {
            int temp = res[i] + carry;
            res[i] = temp % 10;
            carry = temp / 10;
        }

        StringBuilder sb = new StringBuilder();
        boolean leading = true;
        for (int digit : res) {
            if (leading && digit == 0) {
                continue;
            }
            leading = false;
            sb.append(digit);
        }
        String resultDigits = sb.length() == 0 ? "0" : sb.toString();

        int totalScale = a.scale + b.scale;

        if (visualize) {
            String aDisplay = a.toString();           // зі знаком!
            String bDisplay = b.toString();           // зі знаком!
            String resultDisplay = insertDot(stripLeadingZeros(resultDigits), totalScale);
            if (a.negative != b.negative && !resultDigits.equals("0")) {
                resultDisplay = "-" + resultDisplay;
            }

            System.out.println(buildFullMulVisualization(aDisplay, bDisplay, resultDisplay, partialProducts));
        }

        return normalize(resultDigits, totalScale);
    }

    private static StringNumeric divideMagnitudes(StringNumeric dividend, StringNumeric divisor, boolean visualize) {
        if (divisor.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if (dividend.isZero()) {
            if (visualize) {
                System.out.println(buildDivVisualization(dividend.toString(), divisor.toString(), "0"));
            }
            return new StringNumeric("0", 0, false);
        }

        String num = dividend.digits;
        String den = divisor.digits;
        long denVal = Long.parseLong(den);

        // Компенсуємо scale дільника — додаємо нулі до діленого
        num += "0".repeat(divisor.scale);

        StringBuilder quotient = new StringBuilder();
        StringBuilder remainder = new StringBuilder();

        boolean inDecimalPart = false;
        int decimalDigits = 0;
        final int MAX_PRECISION = 40;

        for (int i = 0; i < num.length() + MAX_PRECISION; i++) {
            System.err.println("remainder: " + num + " " + den + " " + remainder);
            // Додаємо наступну цифру до залишку
            if (i < num.length()) {
                remainder.append(num.charAt(i));
            } else {
                if (!inDecimalPart) {
                    if (quotient.length() == 0) {
                        quotient.append('0');
                    }
                    quotient.append('.');
                    inDecimalPart = true;
                }
                remainder.append('0');
                decimalDigits++;
                if (decimalDigits > MAX_PRECISION) {
                    break;
                }
            }

            // Прибираємо провідні нулі
            String remStr = remainder.toString().replaceFirst("^0+", "");
            if (remStr.isEmpty()) {
                remStr = "0";
            }
            remainder = new StringBuilder(remStr);

            long remVal = Long.parseLong(remainder.toString());

            int digit = 0;
            if (remVal >= denVal) {
                digit = (int) (remVal / denVal);
                remainder = new StringBuilder(String.valueOf(remVal - digit * denVal));
            }

            quotient.append(digit);
        }

        String resultStr = quotient.toString();

        // Нормалізація
        if (resultStr.contains(".")) {
            resultStr = resultStr.replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        // Видаляємо зайвий '0' на початку, якщо він є (наприклад "05" → "5", "025" → "25")
        if (resultStr.startsWith("0") && !resultStr.startsWith("0.")) {
            resultStr = resultStr.replaceFirst("^0+", "");
            if (resultStr.isEmpty() || resultStr.startsWith(".")) {
                resultStr = "0" + resultStr;
            }
        }

        if (visualize) {
            System.out.println(buildDivVisualization(dividend.toString(), divisor.toString(), resultStr));
        }

        return new StringNumeric(resultStr);
    }

    // -- visualization--
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
        return buildVisualization(aRaw, bRaw, resultRaw, carryInto, maxLen, scale, '+', carry -> (char) ('0' + carry));
    }

    /**
     * Builds a column-style visualization of subtraction, for example:
     * <pre>
     *  1
     *  52
     * -27
     *  --
     *  25
     * </pre> The {@code 1} above a digit marks that it was borrowed from
     * (effectively reduced by 1).
     */
    private static String buildSubVisualization(String aRaw, String bRaw, String resultRaw,
                                                int[] incomingBorrow, int maxLen, int scale) {
        return buildVisualization(aRaw, bRaw, resultRaw, incomingBorrow, maxLen, scale, '-', borrow -> '1');
    }

    private static String buildVisualization(String aRaw, String bRaw, String resultRaw,
                                             int[] marks, int maxLen, int scale, char operationSign, IntFunction<Character> markToChar) {

        // reinsert decimal point for display only
        String a = insertDot(aRaw, scale);
        String b = insertDot(bRaw, scale);
        String result = insertDot(stripLeadingZeros(resultRaw), scale);

        int dw = Math.max(Math.max(a.length(), b.length()), result.length());

        // marks[] — це або carryInto (для додавання), або incomingBorrow (для віднімання)
        char[] markChars = new char[dw];
        Arrays.fill(markChars, ' ');

        for (int col = 1; col <= maxLen; col++) {
            if (marks[col] > 0) {
                int dotShift = (scale > 0 && col >= scale) ? 1 : 0;
                int idx = dw - 1 - col - dotShift;
                if (idx >= 0) {
                    markChars[idx] = markToChar.apply(marks[col]);
                }
            }
        }

        String markLine = (" " + new String(markChars)).stripTrailing();

        StringBuilder vis = new StringBuilder();
        if (!markLine.isBlank()) {
            vis.append(markLine).append('\n');
        }

        vis.append(' ').append(padLeft(a, dw)).append('\n');
        vis.append(operationSign).append(padLeft(b, dw)).append('\n');
        vis.append(' ').repeat("-", dw).append('\n');
        vis.append(' ').append(padLeft(result, dw));

        return vis.toString();
    }

    /**
     * Повна шкільна візуалізація множення з усіма частковими добутками.
     */
    private static String buildFullMulVisualization(String aStr, String bStr, String resultStr, String[] partials) {
        int maxWidth = Math.max(Math.max(aStr.length(), bStr.length() + 2), resultStr.length());
        for (String p : partials) {
            if (p != null) {
                maxWidth = Math.max(maxWidth, p.length() + 4);
            }
        }

        StringBuilder vis = new StringBuilder();

        vis.append(padLeft(aStr, maxWidth)).append('\n');
        vis.append('×').append(padLeft(bStr, maxWidth - 1)).append('\n');
        vis.append("─".repeat(maxWidth)).append('\n');

        // Часткові добутки (завжди позитивні)
        for (int i = 0; i < partials.length; i++) {
            String part = partials[i];
            if (part == null || (part.equals("0") && partials.length > 1)) {
                continue;
            }

            String shifted = padLeft(part, maxWidth - i) + " ".repeat(i);
            vis.append(shifted).append('\n');
        }

        vis.append("─".repeat(maxWidth)).append('\n');
        vis.append(padLeft(resultStr, maxWidth));

        return vis.toString();
    }

    private static String buildDivVisualization(String dividend, String divisor, String quotient) {
        int width = Math.max(Math.max(dividend.length(), divisor.length() + 2), quotient.length() + 2);

        StringBuilder vis = new StringBuilder();
        vis.append(padLeft(dividend, width)).append('\n');
        vis.append('÷').append(padLeft(divisor, width - 1)).append('\n');
        vis.append("─".repeat(width)).append('\n');
        vis.append(padLeft(quotient, width));

        return vis.toString();
    }

    // --- Number ---
    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public long longValue() {
        long abs = new BigInteger(digits)
                .divide(BigInteger.TEN.pow(scale))
                .longValueExact();
        return negative ? -abs : abs;
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
        if (this.negative != other.negative) {
            return this.negative ? -1 : 1;
        }
        int cmp = compareMagnitudes(this, other);
        return this.negative ? -cmp : cmp; // for negatives: larger magnitude is smaller value
    }

    private static int compareMagnitudes(StringNumeric a, StringNumeric b) {
        int maxScale = Math.max(a.scale, b.scale);
        String as = a.digits + "0".repeat(maxScale - a.scale);
        String bs = b.digits + "0".repeat(maxScale - b.scale);
        if (as.length() != bs.length()) {
            return Integer.compare(as.length(), bs.length());
        }
        return as.compareTo(bs);
    }

    // --- Object ---
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StringNumeric sn)) {
            return false;
        }
        return negative == sn.negative && scale == sn.scale && digits.equals(sn.digits);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * digits.hashCode() + scale) + Boolean.hashCode(negative);
    }

    @Override
    public String toString() {
        String abs = insertDot(digits, scale);
        return negative ? "-" + abs : abs;
    }

    // --- helpers ---
    /**
     * Inserts a decimal point {@code scale} places from the right, or returns
     * {@code s} as-is.
     */
    private static String insertDot(String s, int scale) {
        if (scale == 0) {
            return s;
        }
        int intLen = s.length() - scale;
        if (intLen > 0) {
            return s.substring(0, intLen) + "." + s.substring(intLen);
        }
        return "0." + "0".repeat(-intLen) + s;
    }

    /**
     * Strips trailing fractional zeros, strips leading zeros, and wraps into a
     * StringNumeric. Always returns a non-negative result; the caller applies
     * the sign.
     */
    private static StringNumeric normalize(String rawDigits, int scale) {
        String d = stripLeadingZeros(rawDigits);
        int s = scale;
        while (s > 0 && !d.isEmpty() && d.charAt(d.length() - 1) == '0') {
            d = d.substring(0, d.length() - 1);
            s--;
        }
        if (d.isEmpty()) {
            return new StringNumeric("0", 0, false);
        }
        return new StringNumeric(d, s, false);
    }

    private static String padLeft(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return " ".repeat(width - s.length()) + s;
    }

    private static String stripLeadingZeros(String s) {
        return s.replaceAll("^0+", "");
    }
}
