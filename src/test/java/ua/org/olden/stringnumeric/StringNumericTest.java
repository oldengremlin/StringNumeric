package ua.org.olden.stringnumeric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class StringNumericTest {

    // ── add (positive) ───────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",
        "1, 0, 1",
        "0, 1, 1",
        "29, 12, 41",
        "9, 1, 10",
        "99, 1, 100",
        "999, 1, 1000",
        "10, 20, 30",
        "123456789, 987654321, 1111111110"
    })
    void testAdd(String a, String b, String expected) {
        assertEquals(expected, new StringNumeric(a).add(new StringNumeric(b)).toString());
    }

    @ParameterizedTest
    @CsvSource({
        "48.12, 15.67, 63.79", // basic decimal add
        "1.5,   2.25,  3.75", // different scales
        "0.1,   0.9,   1", // fractional carry produces integer
        "9.99,  0.01,  10", // carry across decimal point
        "0.5,   0.5,   1", // sum is whole number
        "1.50,  2.50,  4", // trailing zeros normalise
        "100.001, 0.999, 101", // deep fractional carry
    })
    void testAddDecimal(String a, String b, String expected) {
        assertEquals(expected, new StringNumeric(a).add(new StringNumeric(b)).toString());
    }

    @Test
    void testAddBeyondLongRange() {
        StringNumeric result = new StringNumeric("99999999999999999999")
                .add(new StringNumeric("1"));
        assertEquals("100000000000000000000", result.toString());
    }

    // ── add (negative) ────────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "-1,   -2,   -3", // both negative
        "-5,    3,   -2", // |negative| > |positive|
        " 3,   -5,   -2", // |positive| < |negative|
        "-5,    5,    0", // cancel out
        "-0.5,  0.5,  0", // decimal cancel
        "-1.5,  1,   -0.5", // decimal mixed
        " 1,   -1.5, -0.5",
        "-10,   3,   -7",
        " 7,  -10,   -3",})
    void testAddWithNegatives(String a, String b, String expected) {
        assertEquals(expected.strip(),
                new StringNumeric(a.strip()).add(new StringNumeric(b.strip())).toString());
    }

    // ── add visualization ─────────────────────────────────────────────────────
    @Test
    void testAddVisualizationWithCarry() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = new StringNumeric("29").add(new StringNumeric("12"), true);
        System.setOut(original);

        assertEquals("41", result.toString());
        assertEquals(" 1\n 29\n+12\n ──\n 41", buf.toString().stripTrailing());
    }

    @Test
    void testAddVisualizationWithChainedCarry() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = new StringNumeric("99").add(new StringNumeric("1"), true);
        System.setOut(original);

        assertEquals("100", result.toString());
        assertEquals(" 11\n  99\n+  1\n ───\n 100", buf.toString().stripTrailing());
    }

    @Test
    void testAddVisualizationNoCarry() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("10").add(new StringNumeric("20"), true);
        System.setOut(original);

        assertEquals(" 10\n+20\n ──\n 30", buf.toString().stripTrailing());
    }

    @Test
    void testAddWithoutVisualizationProducesNoOutput() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("29").add(new StringNumeric("12"));
        new StringNumeric("29").add(new StringNumeric("12"), false);
        System.setOut(original);

        assertTrue(buf.toString().isEmpty());
    }

    // ── sub (positive) ────────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "0,   0,   0",
        "1,   0,   1",
        "1,   1,   0",
        "41,  12,  29",
        "10,  1,   9",
        "100, 1,   99",
        "1000,1,   999",
        "30,  20,  10",
        "1111111110, 987654321, 123456789"
    })
    void testSub(String a, String b, String expected) {
        assertEquals(expected, new StringNumeric(a).sub(new StringNumeric(b)).toString());
    }

    @ParameterizedTest
    @CsvSource({
        "63.79, 15.67, 48.12", // basic decimal sub
        "3.75,  1.5,   2.25", // different scales
        "1,     0.9,   0.1", // integer minus decimal
        "10,    9.99,  0.01", // borrow across decimal point
        "1,     0.5,   0.5", // result is half
        "4,     1.50,  2.5", // trailing zeros normalise
        "101,   0.999, 100.001", // deep fractional borrow
    })
    void testSubDecimal(String a, String b, String expected) {
        assertEquals(expected, new StringNumeric(a).sub(new StringNumeric(b)).toString());
    }

    @Test
    void testSubBeyondLongRange() {
        StringNumeric result = new StringNumeric("100000000000000000000")
                .sub(new StringNumeric("1"));
        assertEquals("99999999999999999999", result.toString());
    }

    // ── sub (negative results & negative operands) ────────────────────────────
    @ParameterizedTest
    @CsvSource({
        " 1,   2,   -1", // positive result flips sign
        " 0,   5,   -5",
        " 1.5, 3,   -1.5", // decimal
        "-5,  -3,   -2", // both negative: -5 - (-3) = -5 + 3 = -2
        "-3,  -5,    2", // -3 - (-5) = -3 + 5 = 2
        "-1,   2,   -3", // negative minus positive
        " 5,  -3,    8", // positive minus negative = sum
        " 1.5,-1.5,  3",})
    void testSubWithNegatives(String a, String b, String expected) {
        assertEquals(expected.strip(),
                new StringNumeric(a.strip()).sub(new StringNumeric(b.strip())).toString());
    }

    // ── sub visualization ─────────────────────────────────────────────────────
    @Test
    void testSubVisualizationWithBorrow() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = new StringNumeric("52").sub(new StringNumeric("27"), true);
        System.setOut(original);

        assertEquals("25", result.toString());
        assertEquals(" 1\n 52\n-27\n ──\n 25", buf.toString().stripTrailing());
    }

    @Test
    void testSubVisualizationWithChainedBorrow() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = new StringNumeric("100").sub(new StringNumeric("1"), true);
        System.setOut(original);

        assertEquals("99", result.toString());
        assertEquals(" 11\n 100\n-  1\n ───\n  99", buf.toString().stripTrailing());
    }

    @Test
    void testSubVisualizationNoBorrow() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("30").sub(new StringNumeric("20"), true);
        System.setOut(original);

        assertEquals(" 30\n-20\n ──\n 10", buf.toString().stripTrailing());
    }

    @Test
    void testSubWithoutVisualizationProducesNoOutput() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("52").sub(new StringNumeric("27"));
        new StringNumeric("52").sub(new StringNumeric("27"), false);
        System.setOut(original);

        assertTrue(buf.toString().isEmpty());
    }

    @Test
    void testSubProducesNegativeResult() {
        assertEquals("-1", new StringNumeric("1").sub(new StringNumeric("2")).toString());
        assertEquals("-0.5", new StringNumeric("0.5").sub(new StringNumeric("1")).toString());
    }

    // ── mul (positive) ───────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",
        "1, 0, 0",
        "29, 12, 348",
        "999, 1, 999",
        "10, 20, 200",
        "123456789, 987654321, 121932631112635269"
    })
    void testMul(String a, String b, String expected) {
        assertEquals(expected, new StringNumeric(a).mul(new StringNumeric(b)).toString());
    }

    @ParameterizedTest
    @CsvSource({
        "48.12, 15.67, 754.0404",
        "1.5,   2.25,  3.375",
        "0.1,   0.9,   0.09",
        "9.99,  0.01,  0.0999",
        "1.50,  2.50,  3.75"
    })
    void testMulDecimal(String a, String b, String expected) {
        assertEquals(expected, new StringNumeric(a).mul(new StringNumeric(b)).toString());
    }

    @Test
    void testMulBeyondLongRange() {
        StringNumeric result = new StringNumeric("99999999999999999999")
                .mul(new StringNumeric("12345678901234567890"));
        assertEquals("1234567890123456788987654321098765432110", result.toString());
    }

    // ── mul (negative) ───────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "-5,   3,   -15",
        " 3,  -5,   -15",
        "-5,  -3,    15",
        "-1.5, 2.25, -3.375",
        " 1.5,-2.25, -3.375"
    })
    void testMulWithNegatives(String a, String b, String expected) {
        assertEquals(expected.strip(),
                new StringNumeric(a.strip()).mul(new StringNumeric(b.strip())).toString());
    }

    // ── mul visualization ────────────────────────────────────────────────────
    @Test
    void testMulVisualization() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("29").mul(new StringNumeric("12"), true);
        System.setOut(original);

        assertEquals("    29\n×   12\n──────\n    58\n   29 \n──────\n   348", buf.toString().stripTrailing());
    }

    @Test
    void testMulWithoutVisualizationProducesNoOutput() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));

        new StringNumeric("29").mul(new StringNumeric("12"), false);
        new StringNumeric("29").mul(new StringNumeric("12"));

        System.setOut(original);
        assertTrue(buf.toString().isEmpty());
    }

    // ── div (positive) ───────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "100, 4,   25",
        "10,  2,   5",
        "15,  3,   5",
        "1,   1,   1",
        "0,   5,   0"
    })
    void testDiv(String a, String b, String expected) {
        assertEquals(expected.strip(),
                new StringNumeric(a.strip()).div(new StringNumeric(b.strip())).toString());
    }

    @ParameterizedTest
    @CsvSource({
        "3.75, 1.5,   2.5",
        "10,   4,     2.5",
        "41,   12,    3.4166666666",
        "1,    0.9,   1.1111111111",
        "0.1,  0.3,   0.3333333333",
        "48.12,15.67, 3.0708359923"
    })
    void testDivDecimal(String a, String b, String expected) {
        assertEquals(expected.strip(),
                new StringNumeric(a.strip()).div(new StringNumeric(b.strip())).toString());
    }

    @Test
    void testDivBeyondLongRange() {
        StringNumeric result = new StringNumeric("99999999999999999999")
                .div(new StringNumeric("123456789"));
        assertEquals("810000007371.000067068", result.toString());
    }

    @Test
    void testDivByZeroThrows() {
        assertThrows(ArithmeticException.class,
                () -> new StringNumeric("5").div(new StringNumeric("0")));
    }

    // ── div (negative) ───────────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "-15,   3,   -5",
        " 15,  -3,   -5",
        "-15,  -3,    5",
        "-3.75, 1.5, -2.5",
        " 3.75,-1.5, -2.5"
    })
    void testDivWithNegatives(String a, String b, String expected) {
        assertEquals(expected.strip(),
                new StringNumeric(a.strip()).div(new StringNumeric(b.strip())).toString());
    }

    // ── div visualization ────────────────────────────────────────────────────
    @Test
    void testDivVisualization() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("100").div(new StringNumeric("4"), true);
        System.setOut(original);

        String out = buf.toString().stripTrailing();
        assertTrue(out.contains("100"), "should contain dividend");
        assertTrue(out.contains("4"), "should contain divisor");
        assertTrue(out.contains("25"), "should contain quotient");
        assertTrue(out.contains("-8"), "should show first subtraction");
        assertTrue(out.contains("-20"), "should show second subtraction");
    }

    @Test
    void testDivWithoutVisualizationProducesNoOutput() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("100").div(new StringNumeric("4"));
        new StringNumeric("100").div(new StringNumeric("4"), false);
        System.setOut(original);
        assertTrue(buf.toString().isEmpty());
    }

    // ── constructors ──────────────────────────────────────────────────────────
    @Test
    void testConstructorStripsLeadingZeros() {
        assertEquals("42", new StringNumeric("0042").toString());
        assertEquals("0", new StringNumeric("0").toString());
        assertEquals("0", new StringNumeric("000").toString());
    }

    @Test
    void testConstructorDecimal() {
        assertEquals("48.12", new StringNumeric("48.12").toString());
        assertEquals("0.5", new StringNumeric("0.5").toString());
        assertEquals("1.5", new StringNumeric("1.50").toString());   // trailing zero stripped
        assertEquals("5", new StringNumeric("5.00").toString());   // all fractional zeros → integer
    }

    @Test
    void testConstructorNumericTypes() {
        assertEquals("42", new StringNumeric(42).toString());
        assertEquals("42", new StringNumeric(42L).toString());
        assertEquals("3.14", new StringNumeric(3.14159, 2).toString());   // rounded to 2 places
        assertEquals("3.14", new StringNumeric(3.14159f, 2).toString());
        assertEquals("1", new StringNumeric(0.999, 0).toString());     // rounds to 1
        assertEquals("3", new StringNumeric(3.0, 2).toString());       // trailing zeros stripped
    }

    @Test
    void testConstructorNegativeString() {
        assertEquals("-42", new StringNumeric("-42").toString());
        assertEquals("-3.14", new StringNumeric("-3.14").toString());
        assertEquals("-0.5", new StringNumeric("-0.5").toString());
        assertEquals("0", new StringNumeric("-0").toString());   // -0 normalises to 0
        assertEquals("0", new StringNumeric("-0.00").toString());
    }

    @Test
    void testConstructorNegativeNumericTypes() {
        assertEquals("-42", new StringNumeric(-42).toString());
        assertEquals("-42", new StringNumeric(-42L).toString());
        assertEquals("-3.14", new StringNumeric(-3.14159, 2).toString());
        assertEquals("-3.14", new StringNumeric(-3.14159f, 2).toString());
        assertEquals("0", new StringNumeric(-0L).toString());    // -0 normalises to 0
    }

    @Test
    void testConstructorRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric(""));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric("12a3"));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric("12.a3"));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric("5."));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric((String) null));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric(1.5, -1));
    }

    // ── negate ────────────────────────────────────────────────────────────────
    @Test
    void testNegate() {
        assertEquals("-5", new StringNumeric("5").negate().toString());
        assertEquals("5", new StringNumeric("-5").negate().toString());
        assertEquals("0", new StringNumeric("0").negate().toString());   // negate(0) == 0
        assertEquals("-0.5", new StringNumeric("0.5").negate().toString());
        assertEquals("0.5", new StringNumeric("-0.5").negate().toString());
    }

    // ── compareTo ─────────────────────────────────────────────────────────────
    @Test
    void testCompareTo() {
        StringNumeric one = new StringNumeric("1");
        StringNumeric ten = new StringNumeric("10");
        StringNumeric twenty = new StringNumeric("20");
        StringNumeric tenB = new StringNumeric("10");

        assertTrue(one.compareTo(ten) < 0);
        assertTrue(ten.compareTo(twenty) < 0);
        assertTrue(twenty.compareTo(ten) > 0);
        assertEquals(0, ten.compareTo(tenB));
    }

    @Test
    void testCompareToWithNegatives() {
        StringNumeric negFive = new StringNumeric("-5");
        StringNumeric negThree = new StringNumeric("-3");
        StringNumeric three = new StringNumeric("3");

        assertTrue(negFive.compareTo(three) < 0);    // -5 < 3
        assertTrue(negFive.compareTo(negThree) < 0); // -5 < -3
        assertTrue(negThree.compareTo(negFive) > 0); // -3 > -5
        assertEquals(0, negFive.compareTo(new StringNumeric("-5")));
        assertTrue(negThree.compareTo(three) < 0);   // -3 < 3
    }

    // ── equals ────────────────────────────────────────────────────────────────
    @Test
    void testEquals() {
        assertEquals(new StringNumeric("42"), new StringNumeric("42"));
        assertEquals(new StringNumeric("42"), new StringNumeric("042"));
        assertNotEquals(new StringNumeric("42"), new StringNumeric("43"));
    }

    @Test
    void testEqualsWithNegatives() {
        assertEquals(new StringNumeric("-42"), new StringNumeric("-42"));
        assertNotEquals(new StringNumeric("-42"), new StringNumeric("42"));
        // both representations of zero are equal
        assertEquals(new StringNumeric("0"), new StringNumeric("-0"));
    }

    // ── Number interface ──────────────────────────────────────────────────────
    @Test
    void testNumberInterface() {
        StringNumeric n = new StringNumeric("42");
        assertEquals(42, n.intValue());
        assertEquals(42L, n.longValue());
        assertEquals(42.0f, n.floatValue());
        assertEquals(42.0, n.doubleValue());
    }

    @Test
    void testNumberInterfaceNegative() {
        StringNumeric n = new StringNumeric("-42");
        assertEquals(-42, n.intValue());
        assertEquals(-42L, n.longValue());
        assertEquals(-42.0f, n.floatValue());
        assertEquals(-42.0, n.doubleValue());
    }

    // ── toBigInteger ──────────────────────────────────────────────────────────
    @Test
    void testToBigInteger() {
        assertEquals("5", new StringNumeric("5").toBigInteger().toString());
        assertEquals("3", new StringNumeric("3.7").toBigInteger().toString());  // truncates
        assertEquals("2", new StringNumeric("2.25").toBigInteger().toString()); // truncates
        assertEquals("0", new StringNumeric("0.99").toBigInteger().toString()); // < 1 → 0
        assertEquals("-5", new StringNumeric("-5.9").toBigInteger().toString()); // negative
    }

    // ── sqrtApproximate ───────────────────────────────────────────────────────
    @Test
    void testSqrtApproximateIntegers() {
        // Для perfect-square результат точний
        assertEquals("1", new StringNumeric("1").sqrtApproximate().toString());
        assertEquals("3", new StringNumeric("9").sqrtApproximate().toString());
        assertEquals("10", new StringNumeric("100").sqrtApproximate().toString());
    }

    @Test
    void testSqrtApproximateDecimalDoesNotThrow() {
        // Раніше падало з NumberFormatException
        assertDoesNotThrow(() -> new StringNumeric("2.25").sqrtApproximate());
        assertDoesNotThrow(() -> new StringNumeric("0.25").sqrtApproximate());
        assertDoesNotThrow(() -> new StringNumeric("0.0001").sqrtApproximate());
    }

    @Test
    void testSqrtApproximateNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new StringNumeric("-4").sqrtApproximate());
    }

    // ── sqrtIterative (Герон) ─────────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "4,   0,  2",
        "9,   0,  3",
        "100, 0,  10",
        "1,   0,  1",
        "0,   0,  0",})
    void testSqrtIterativeInteger(String a, int precision, String expected) {
        assertEquals(expected,
                new StringNumeric(a).sqrtIterative(precision).toString());
    }

    @ParameterizedTest
    @CsvSource({
        "2.25, 4, 1.5",
        "0.25, 4, 0.5",
        "6.25, 4, 2.5",
        "0.01, 4, 0.1",})
    void testSqrtIterativeDecimal(String a, int precision, String expected) {
        assertEquals(expected,
                new StringNumeric(a).sqrtIterative(precision).toString());
    }

    @Test
    void testSqrtIterativeNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new StringNumeric("-4").sqrtIterative());
    }

    // ── sqrtLongDivision (в стовпчик) ─────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
        "4,   0,  2",
        "9,   0,  3",
        "100, 0,  10",
        "25,  0,  5",
        "1,   0,  1",
        "0,   0,  0",})
    void testSqrtLongDivisionInteger(String a, int precision, String expected) {
        assertEquals(expected,
                new StringNumeric(a).sqrtLongDivision(precision).toString());
    }

    @ParameterizedTest
    @CsvSource({
        "2.25,   0,  1.5",
        "0.25,   0,  0.5",
        "6.25,   0,  2.5",
        "0.0025, 0,  0.05",
        "56.25,  0,  7.5",})
    void testSqrtLongDivisionDecimal(String a, int precision, String expected) {
        assertEquals(expected,
                new StringNumeric(a).sqrtLongDivision(precision).toString());
    }

    @Test
    void testSqrtLongDivisionWithPrecision() {
        // sqrt(2) ≈ 1.41421356...
        String result = new StringNumeric("2").sqrtLongDivision(4).toString();
        assertTrue(result.startsWith("1.4142"), "sqrt(2) should start with 1.4142, got: " + result);
    }

    @Test
    void testSqrtLongDivisionDecimalWithPrecision() {
        // sqrt(2.25) = 1.5 точно, precision=4 дає 1.5000
        String result = new StringNumeric("2.25").sqrtLongDivision(4).toString();
        assertEquals("1.5", result); // trailing zeros normalised
    }

    @Test
    void testSqrtLongDivisionNegativeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new StringNumeric("-9").sqrtLongDivision());
    }

    @Test
    void testSqrtLongDivisionVisualizationProducesOutput() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("144").SqrtLongDivision(0, true);
        System.setOut(original);
        assertFalse(buf.toString().isEmpty(), "visualize=true should produce output");
    }

    @Test
    void testSqrtLongDivisionWithoutVisualizationProducesNoOutput() {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        new StringNumeric("144").SqrtLongDivision(0, false);
        new StringNumeric("144").sqrtLongDivision(0);
        System.setOut(original);
        assertTrue(buf.toString().isEmpty());
    }
}
