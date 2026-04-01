package ua.org.olden.stringnumeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Число довільної точності зі знаком, що зберігає значення у вигляді рядка
 * цифр.
 *
 * <p>
 * Внутрішнє представлення:
 * <ul>
 * <li>{@code digits} — рядок значущих цифр без десяткової крапки</li>
 * <li>{@code scale} — кількість знаків після коми (0 = ціле число)</li>
 * <li>{@code negative} — ознака від'ємного числа (для нуля завжди
 * {@code false})</li>
 * </ul>
 * Наприклад, {@code -48.12} зберігається як
 * {@code digits="4812", scale=2, negative=true}.
 *
 * <p>
 * Усі чотири арифметичні операції реалізовано у вигляді шкільних алгоритмів «в
 * стовпчик». За бажанням кожну операцію можна вивести у вигляді шкільного
 * запису (параметр {@code visualize}).
 */
public final class StringNumeric extends Number implements Comparable<StringNumeric> {

    private static final Pattern VALUE_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?");

    private final String digits;   // значущі цифри без десяткової крапки
    private final int scale;       // кількість знаків після коми (0 = ціле число)
    private final boolean negative; // true якщо число від'ємне (для нуля завжди false)

    /**
     * Результат однієї колонкової операції: цифра результату та перенос/позика
     * на наступний розряд.
     */
    private record ColumnResult(int digit, int nextTransfer) {

    }

    /**
     * Результат нормалізації рядка цифр: цифри без зайвих нулів та відповідний
     * scale.
     */
    private record returnStripZeros(String digit, int scale) {

    }

    /**
     * Функціональний інтерфейс для однієї колонкової операції (додавання або
     * віднімання). Приймає дві цифри та вхідний перенос/позику, повертає цифру
     * результату і вихідний перенос/позику.
     */
    @FunctionalInterface
    private interface ColumnOperation {

        ColumnResult apply(int digitA, int digitB, int incoming);
    }

    /**
     * Створює число з рядкового представлення. Допустимий формат:
     * необов'язковий знак {@code -}, цілі цифри, необов'язкова десяткова
     * частина через {@code .}. Провідні та хвостові нулі нормалізуються:
     * {@code "-007.50"} → {@code "-7.5"}.
     *
     * @param value
     * @throws IllegalArgumentException якщо рядок {@code null}, порожній або
     * має недопустимий формат
     */
    public StringNumeric(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null or blank");
        }
        value = value.strip();
        if (value.isBlank()) {
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

        returnStripZeros stripZeros = stripZeros(intPart + fracPart, fracPart.length());

        if (stripZeros.digit.isEmpty() || stripZeros.digit.equalsIgnoreCase("0")) {
            this.digits = "0";
            this.scale = 0;
        } else {
            this.digits = stripZeros.digit;
            this.scale = stripZeros.scale;
        }
        this.negative = neg && !stripZeros.digit.equals("0"); // -0 normalises to 0
    }

    /**
     * Створює ціле число з {@code int}.
     *
     * @param value
     */
    public StringNumeric(int value) {
        this((long) value);
    }

    /**
     * Створює ціле число з {@code long}.
     *
     * @param value
     */
    public StringNumeric(long value) {
        this.negative = value < 0;
        this.digits = Long.toString(Math.abs(value));
        this.scale = 0;
    }

    /**
     * Створює число з {@code double}, округлене до {@code scale} знаків після
     * коми. Явний {@code scale} обов'язковий, щоб уникнути сюрпризів двійкової
     * арифметики (наприклад, {@code 0.1 + 0.2 == 0.30000000000000004}).
     *
     * @param value
     * @param scale
     * @throws IllegalArgumentException якщо {@code scale} від'ємний
     */
    public StringNumeric(double value, int scale) {
        this(formatFloating(value, scale));
    }

    /**
     * Створює число з {@code float}, округлене до {@code scale} знаків після
     * коми.
     *
     * @param value
     * @param scale
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

    // Приватний конструктор — digits/scale/negative вже нормалізовані.
    private StringNumeric(String digits, int scale, boolean negative) {
        this.digits = digits;
        this.scale = scale;
        this.negative = negative;
    }

    // --- знак ---
    /**
     * Повертає нове значення з протилежним знаком; {@code negate(0) == 0}.
     */
    public StringNumeric negate() {
        if (isZero()) {
            return this;
        }
        return new StringNumeric(digits, scale, !negative);
    }

    /**
     * Повертає {@code true}, якщо значення дорівнює нулю.
     *
     * @return
     */
    public boolean isZero() {
        return digits.equals("0"); // після нормалізації нуль завжди має scale == 0
    }

    // --- додавання ---
    /**
     * Додає {@code other} до цього значення. Результат може бути від'ємним.
     * Якщо {@code visualize == true} — виводить шкільний запис операції до
     * {@code System.out}.
     *
     * @param other
     * @param visualize
     * @return
     */
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

    /**
     * Додає {@code other} до цього значення.
     */
    public StringNumeric add(StringNumeric other) {
        return add(other, false);
    }

    // --- віднімання ---
    /**
     * Віднімає {@code other} від цього значення. Результат може бути від'ємним.
     * Делегує до {@code add(other.negate(), visualize)}, тому візуалізація
     * показує фактичну операцію над модулями (додавання або віднімання).
     *
     * @param other
     * @param visualize
     * @return
     */
    public StringNumeric sub(StringNumeric other, boolean visualize) {
        return add(other.negate(), visualize);
    }

    /**
     * Віднімає {@code other} від цього значення.
     *
     * @param other
     * @return
     */
    public StringNumeric sub(StringNumeric other) {
        return sub(other, false);
    }

    // --- множення ---
    /**
     * Множить це значення на {@code other}. Якщо {@code visualize == true} —
     * виводить шкільний запис множення стовпчиком до {@code System.out}.
     *
     * @param other
     * @param visualize
     * @return
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

    /**
     * Множить це значення на {@code other}.
     *
     * @param other
     * @return
     */
    public StringNumeric mul(StringNumeric other) {
        return mul(other, false);
    }

    // --- ділення ---
    /**
     * Ділить це значення на {@code other} з точністю {@code precision} знаків
     * після коми. Якщо {@code visualize == true} — виводить шкільний запис
     * ділення стовпчиком до {@code System.out}.
     *
     * @param other
     * @param precision
     * @param visualize
     * @return
     * @throws ArithmeticException якщо {@code other} дорівнює нулю
     */
    public StringNumeric div(StringNumeric other, int precision, boolean visualize) {
        if (other.isZero()) {
            throw new ArithmeticException("Division by zero");
        }
        if (this.isZero()) {
            return new StringNumeric("0", 0, false);
        }

        boolean resultNegative = this.negative != other.negative;

        StringNumeric mag = divideMagnitudes(this, other, precision, visualize);

        return (resultNegative && !mag.isZero())
               ? new StringNumeric(mag.digits, mag.scale, true)
               : mag;
    }

    /**
     * Ділить на {@code other} з точністю 10 знаків після коми.
     *
     * @param other
     * @return
     */
    public StringNumeric div(StringNumeric other) {
        return div(other, 10, false);
    }

    /**
     * Ділить на {@code other} з вказаною точністю {@code precision} знаків
     * після коми.
     */
    public StringNumeric div(StringNumeric other, int precision) {
        return div(other, precision, false);
    }

    /**
     * Ділить на {@code other} з точністю 10 знаків після коми та виводить
     * візуалізацію.
     */
    public StringNumeric div(StringNumeric other, boolean visualize) {
        return div(other, 10, visualize);
    }

    // -- операції над модулями --
    /**
     * Порівнює модулі двох чисел, не враховуючи знак. Повертає від'ємне
     * значення, нуль або додатне залежно від того, чи
     * {@code |a| < |b|}, {@code |a| == |b|} чи {@code |a| > |b|}.
     */
    private static int compareMagnitudes(StringNumeric a, StringNumeric b) {
        int maxScale = Math.max(a.scale, b.scale);
        String as = a.digits + "0".repeat(maxScale - a.scale);
        String bs = b.digits + "0".repeat(maxScale - b.scale);
        if (as.length() != bs.length()) {
            return Integer.compare(as.length(), bs.length());
        }
        return as.compareTo(bs);
    }

    /**
     * Спільне ядро для додавання й віднімання модулів. Вирівнює обидва операнди
     * за десятковою крапкою, потім обходить розряди справа наліво, застосовуючи
     * {@code operation} до кожної пари цифр разом із переносом/позикою.
     *
     * @param isAddition {@code true} — будує візуалізацію додавання,
     * {@code false} — віднімання
     */
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

    /**
     * Складає модулі двох чисел. Передумова: обидва ненегативні.
     */
    private static StringNumeric addMagnitudes(StringNumeric a, StringNumeric b, boolean visualize) {
        return magnitudes(a, b, visualize,
                (da, db, cin) -> {
                    int sum = da + db + cin;
                    return new ColumnResult(sum % 10, sum / 10);
                },
                true);
    }

    /**
     * Віднімає модуль {@code b} від модуля {@code a}. Передумова:
     * {@code |a| >= |b|} — гарантується викликачем ({@code add()}).
     */
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

    /**
     * Множить модулі двох чисел шкільним алгоритмом: для кожної цифри множника
     * обчислюється частковий добуток, потім усі часткові добутки
     * підсумовуються.
     */
    private static StringNumeric multiplyMagnitudes(StringNumeric a, StringNumeric b, boolean visualize) {
        if (a.isZero() || b.isZero()) {
            if (visualize) {
                System.out.println(buildMulVisualization(a.toString(), b.toString(), "0", new String[0]));
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

            System.out.println(buildMulVisualization(aDisplay, bDisplay, resultDisplay, partialProducts));
        }

        return normalize(resultDigits, totalScale);
    }

    /**
     * Ділить модуль {@code dividend} на модуль {@code divisor} довгим діленням
     * «в стовпчик».
     *
     * <p>
     * Алгоритм:
     * <ol>
     * <li>Обидва операнди зводяться до цілих чисел через спільний
     * {@code scale}.</li>
     * <li>Цифри діленого знімаються по одній; якщо поточний залишок більший або
     * рівний дільнику — обчислюється цифра частки через {@link BigInteger}
     * (O(1)), інакше цифра частки дорівнює 0.</li>
     * <li>Після вичерпання цифр діленого знімаються нулі (дробова частина)
     * доти, доки не набрано {@code precision} знаків після коми.</li>
     * </ol>
     */
    private static StringNumeric divideMagnitudes(StringNumeric dividend, StringNumeric divisor, int precision, boolean visualize) {
        // 1. Приводимо до цілих чисел (прибираємо кому)
        // Наприклад: 4.8 / 1.2 -> 48 / 12
        int commonScale = Math.max(dividend.scale, divisor.scale);
        String d1Digits = dividend.digits + "0".repeat(commonScale - dividend.scale);
        String d2Digits = divisor.digits + "0".repeat(commonScale - divisor.scale);

        // Прибираємо ведучі нулі для коректної роботи
        d1Digits = stripLeadingZeros(d1Digits);
        d2Digits = stripLeadingZeros(d2Digits);

        StringNumeric div = new StringNumeric(d2Digits);
        StringBuilder quotient = new StringBuilder();
        String currentRemainder = "";

        // Для візуалізації
        java.util.List<String> steps = new java.util.ArrayList<>();
        boolean hasStarted = false; // перша ненульова цифра частки вже зустрілася

        // 2. Визначаємо, скільки знаків після коми ми хочемо (наприклад, 10)
        int totalDigitsToProcess = d1Digits.length() + precision;
        int dotPosition = d1Digits.length();

        for (int i = 0; i < totalDigitsToProcess; i++) {
            // Беремо наступну цифру або 0, якщо цифри діленого закінчилися
            char nextDigit = (i < d1Digits.length()) ? d1Digits.charAt(i) : '0';
            currentRemainder = stripLeadingZeros(currentRemainder + nextDigit);

            if (currentRemainder.isEmpty()) {
                currentRemainder = "0";
            }

            StringNumeric window = new StringNumeric(currentRemainder);
            int qDigit = 0;

            if (window.compareTo(div) >= 0) {
//                // Підбираємо цифру частки
//                while (window.compareTo(div) >= 0) {
//                    window = window.sub(div);
//                    qDigit++;
//                }
                qDigit = new BigInteger(currentRemainder)
                        .divide(new BigInteger(d2Digits))
                        .intValue();
                window = new StringNumeric(
                        new BigInteger(currentRemainder)
                                .subtract(
                                        new BigInteger(d2Digits)
                                                .multiply(
                                                        BigInteger.valueOf(qDigit)
                                                )
                                ).toString()
                );
            }

            if (qDigit > 0) {
                hasStarted = true;
            }
            if (visualize) {
                if (qDigit > 0) {
                    steps.add(currentRemainder);
                    steps.add(div.mul(new StringNumeric(qDigit)).digits);
                } else if (hasStarted && i < d1Digits.length()) {
                    // Нульова цифра у цілій частині: показуємо знесення
                    steps.add(currentRemainder);
                    steps.add("0");
                }
            }

            quotient.append(qDigit);
            currentRemainder = window.digits;

            // Додаємо крапку в рядок частки, якщо пройшли цілу частину
            if (i == dotPosition - 1 && i < totalDigitsToProcess - 1) {
                quotient.append('.');
            }
        }

        String resultStr = quotient.toString();
        // Якщо візуалізація увімкнена, тут можна викликати оновлений buildDivVisualization
        // з використанням зібраних steps

        if (visualize) {
            System.out.println(buildDivVisualization(d1Digits, d2Digits, resultStr, steps));
        }

        return new StringNumeric(resultStr);
    }

    // -- візуалізація --
    /**
     * Будує шкільний запис додавання в стовпчик, наприклад:
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
     * Будує шкільний запис віднімання в стовпчик, наприклад:
     * <pre>
     *  1
     *  52
     * -27
     *  --
     *  25
     * </pre> Цифра {@code 1} над розрядом означає, що з нього була взята позика
     * (фактично він зменшується на 1).
     */
    private static String buildSubVisualization(String aRaw, String bRaw, String resultRaw,
                                                int[] incomingBorrow, int maxLen, int scale) {
        return buildVisualization(aRaw, bRaw, resultRaw, incomingBorrow, maxLen, scale, '-', borrow -> '1');
    }

    /**
     * Спільне ядро рендерингу для додавання та віднімання. Вставляє десяткову
     * крапку, вирівнює рядки по правому краю, розставляє мітки переносів/позик
     * над відповідними розрядами.
     *
     * @param marks масив переносів (add) або позик (sub) по розрядах
     * @param operationSign символ операції ({@code '+'} або {@code '-'})
     * @param markToChar перетворює числове значення позначки на символ для
     * відображення
     */
    private static String buildVisualization(String aRaw, String bRaw, String resultRaw,
                                             int[] marks, int maxLen, int scale, char operationSign, IntFunction<Character> markToChar) {

        // вставляємо десяткову крапку лише для відображення
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
            vis
                    .append(markLine)
                    .append('\n');
        }

        vis
                .append(' ')
                .append(padLeft(a, dw))
                .append('\n')
                .append(operationSign)
                .append(padLeft(b, dw))
                .append('\n')
                .append(' ')
                .repeat("-", dw)
                .append('\n')
                .append(' ')
                .append(padLeft(result, dw));

        return vis.toString();
    }

    /**
     * Повна шкільна візуалізація множення з усіма частковими добутками.
     */
    private static String buildMulVisualization(String aStr, String bStr, String resultStr, String[] partials) {
        int maxWidth = Math.max(Math.max(aStr.length(), bStr.length() + 2), resultStr.length());
        for (String p : partials) {
            if (p != null) {
                maxWidth = Math.max(maxWidth, p.length() + 4);
            }
        }

        StringBuilder vis = new StringBuilder();

        vis
                .append(padLeft(aStr, maxWidth))
                .append('\n')
                .append('×')
                .append(padLeft(bStr, maxWidth - 1))
                .append('\n')
                .append("─".repeat(maxWidth))
                .append('\n');

        // Часткові добутки (завжди позитивні)
        for (int i = 0; i < partials.length; i++) {
            String part = partials[i];
            if (part == null || (part.equals("0") && partials.length > 1)) {
                continue;
            }

            String shifted = padLeft(part, maxWidth - i) + " ".repeat(i);
            vis
                    .append(shifted)
                    .append('\n');
        }

        vis
                .append("─".repeat(maxWidth))
                .append('\n')
                .append(padLeft(resultStr, maxWidth));

        return vis.toString();
    }

    /**
     * Будує шкільний запис ділення «кутиком», наприклад:
     * <pre>
     *   48120 │ 15678
     *  -15678 ├────
     *  ────── │ 3
     *   32342
     *   ...
     * </pre> {@code steps} — список пар (window, subtracted): що ділимо і що
     * віднімаємо на кожному кроці. Якщо {@code subtracted == "0"}, цифра частки
     * нульова — малюємо лише роздільну лінію.
     */
    private static String buildDivVisualization(String d1, String d2, String quotient, java.util.List<String> steps) {
        StringBuilder sb = new StringBuilder();

        // Шапка: 48120 | 15678
        sb
                .append(" ")
                .append(d1)
                .append(" │ ")
                .append(d2)
                .append("\n");

        // quotient може починатися з нулів, як у вашому прикладі - їх краще почистити для заголовка
        String cleanQuotient = quotient.replaceFirst("^0+", "");
        if (cleanQuotient.startsWith(".")) {
            cleanQuotient = "0" + cleanQuotient;
        }

        int currentPos = 0;

        for (int i = 0; i < steps.size(); i += 2) {
            String window = steps.get(i);      // Що зараз ділимо (напр. 48120)
            String subtracted = steps.get(i + 1); // Що віднімаємо (напр. 47034)

            // Визначаємо відступ. У стовпчику він збільшується поступово
            String indent = " ".repeat(currentPos);

            // Малюємо поточне число (остачу з минулого кроку + знесену цифру)
            if (i > 0) {
                sb
                        .append(" ")
                        .append(indent)
                        .append(window)
                        .append("\n");
            }

            if (subtracted.equals("0")) {
                // Нульова цифра частки: знесене число менше дільника, рядок віднімання не потрібен
                sb.append(indent).append("─".repeat(window.length() + 1)).append("\n");
            } else {
                // Малюємо віднімання
                sb
                        .append(indent)
                        .append(" ".repeat(Math.abs(subtracted.length() - window.length())))
                        .append("-")
                        .append(subtracted);
                if (i == 0) {
                    sb
                            .append(" ".repeat(d1.length() - window.length()))
                            .append(" ├") // ╰
                            .append("─".repeat(cleanQuotient.replaceAll("0+$", "").replaceAll("\\.$", "").length() + 2));
                }
                sb.append("\n");

                // Лінія підкреслення довжиною з вікно
                sb.append(indent).append("─".repeat(Math.max(subtracted.length(), window.length()) + 1));
                if (i == 0) {
                    sb
                            .append(" ".repeat(d1.length() - window.length() + 1))
                            .append("│ ")
                            .append(cleanQuotient.replaceAll("0+$", "").replaceAll("\\.$", ""));
                }
                sb.append("\n");
            }

            // Розраховуємо позицію для наступного кроку
            // currentPos зміщується вправо залежно від того, скільки цифр "з'їло" віднімання
            int remainderLen = new StringNumeric(window).sub(new StringNumeric(subtracted)).digits.length();
            if (window.equals(subtracted)) {
                remainderLen = 0;
            }

            currentPos += (window.length() - remainderLen);
        }

        return sb.toString();
    }

    // --- Number ---
    /**
     * Повертає ціле значення (відкидає дробову частину). Якщо значення виходить
     * за межі {@code int} — можливе переповнення.
     */
    @Override
    public int intValue() {
        return (int) longValue();
    }

    /**
     * Повертає ціле значення як {@code long} (відкидає дробову частину).
     *
     * @throws ArithmeticException якщо значення виходить за межі {@code long}
     */
    @Override
    public long longValue() {
        long abs = new BigInteger(digits)
                .divide(BigInteger.TEN.pow(scale))
                .longValueExact();
        return negative ? -abs : abs;
    }

    /**
     * Повертає наближене значення у типі {@code float}.
     */
    @Override
    public float floatValue() {
        return Float.parseFloat(toString());
    }

    /**
     * Повертає наближене значення у типі {@code double}.
     */
    @Override
    public double doubleValue() {
        return Double.parseDouble(toString());
    }

    // --- Comparable ---
    /**
     * Порівнює це число з {@code other}. Спочатку порівнюються знаки, потім —
     * модулі (для від'ємних: більший модуль → менше значення).
     *
     * @param other
     */
    @Override
    public int compareTo(StringNumeric other) {
        if (this.negative != other.negative) {
            return this.negative ? -1 : 1;
        }
        int cmp = compareMagnitudes(this, other);
        return this.negative ? -cmp : cmp; // for negatives: larger magnitude is smaller value
    }

    // --- Object ---
    /**
     * Два числа рівні, якщо у них однакові {@code digits}, {@code scale} і
     * {@code negative}. Це відповідає математичній рівності після нормалізації.
     *
     * @param o
     */
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

    /**
     * Узгоджений з {@code equals}: однакові числа мають однаковий хеш.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return 31 * (31 * digits.hashCode() + scale) + Boolean.hashCode(negative);
    }

    /**
     * Повертає десяткове рядкове представлення з десятковою крапкою та знаком
     * (якщо від'ємне).
     *
     * @return
     */
    @Override
    public String toString() {
        String abs = insertDot(digits, scale);
        return negative ? "-" + abs : abs;
    }

    // --- допоміжні методи ---
    /**
     * Вставляє десяткову крапку на {@code scale} позицій від правого краю рядка
     * цифр. Якщо {@code scale == 0} — повертає рядок без змін. Якщо цілова
     * частина відсутня (наприклад {@code scale >= s.length()}) — додає
     * {@code "0."} і нулі-заповнювачі.
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
     * Вирівнює рядок {@code s} по правому краю до ширини {@code width}
     * пробілами зліва.
     */
    private static String padLeft(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return " ".repeat(width - s.length()) + s;
    }

    /**
     * Нормалізує рядок цифр: прибирає хвостові нулі дробової частини та
     * провідні нулі, і повертає ненегативний {@code StringNumeric}. Знак
     * застосовується викликачем.
     */
    private static StringNumeric normalize(String rawDigits, int scale) {
        returnStripZeros stripZeros = stripZeros(rawDigits, scale);
        if (stripZeros.digit.isEmpty()) {
            return new StringNumeric("0", 0, false);
        }
        return new StringNumeric(stripZeros.digit, stripZeros.scale, false);
    }

    /**
     * Прибирає хвостові нулі дробової частини та провідні нулі, зменшуючи
     * {@code scale} відповідно. Якщо після очищення рядок порожній — повертає
     * {@code "0"}.
     */
    private static returnStripZeros stripZeros(String rawDigits, int scale) {
        String d = stripLeadingZeros(rawDigits);
        int s = scale;
        while (s > 0 && !d.isEmpty() && d.charAt(d.length() - 1) == '0') {
            d = d.substring(0, d.length() - 1);
            s--;
        }
        d = d.isEmpty() ? "0" : d;
        return new returnStripZeros(d, s);
    }

    /**
     * Прибирає провідні нулі з рядка цифр; порожній рядок повертається без
     * змін.
     */
    private static String stripLeadingZeros(String s) {
        return s.replaceAll("^0+", "");
    }
}
