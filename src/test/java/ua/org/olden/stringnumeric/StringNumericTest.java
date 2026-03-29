package ua.org.olden.stringnumeric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class StringNumericTest {

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
        "48.12, 15.67, 63.79",   // basic decimal add
        "1.5,   2.25,  3.75",    // different scales
        "0.1,   0.9,   1",       // fractional carry produces integer
        "9.99,  0.01,  10",      // carry across decimal point
        "0.5,   0.5,   1",       // sum is whole number
        "1.50,  2.50,  4",       // trailing zeros normalise
        "100.001, 0.999, 101",   // deep fractional carry
    })
    void testAddDecimal(String a, String b, String expected) {
        assertEquals(expected, new StringNumeric(a).add(new StringNumeric(b)).toString());
    }

    @Test
    void testAddBeyondLongRange() {
        // 20-digit numbers — well beyond long/BigInteger convenience range
        StringNumeric result = new StringNumeric("99999999999999999999")
                .add(new StringNumeric("1"));
        assertEquals("100000000000000000000", result.toString());
    }

    @Test
    void testAddVisualizationWithCarry() {
        // 29 + 12 = 41, carry from units to tens
        StringNumeric a = new StringNumeric("29");
        StringNumeric b = new StringNumeric("12");

        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = a.add(b, true);
        System.setOut(original);

        assertEquals("41", result.toString());
        assertEquals(" 1\n 29\n+12\n --\n 41", buf.toString().stripTrailing());
    }

    @Test
    void testAddVisualizationWithChainedCarry() {
        // 99 + 1 = 100, carry chain propagates through two columns
        StringNumeric a = new StringNumeric("99");
        StringNumeric b = new StringNumeric("1");

        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = a.add(b, true);
        System.setOut(original);

        assertEquals("100", result.toString());
        assertEquals(" 11\n  99\n+  1\n ---\n 100", buf.toString().stripTrailing());
    }

    @Test
    void testAddVisualizationNoCarry() {
        // 10 + 20 = 30, no carries at all
        StringNumeric a = new StringNumeric("10");
        StringNumeric b = new StringNumeric("20");

        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        a.add(b, true);
        System.setOut(original);

        // No carry line expected
        assertEquals(" 10\n+20\n --\n 30", buf.toString().stripTrailing());
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

    @Test
    void testConstructorStripsLeadingZeros() {
        assertEquals("42", new StringNumeric("0042").toString());
        assertEquals("0", new StringNumeric("0").toString());
        assertEquals("0", new StringNumeric("000").toString());
    }

    @Test
    void testConstructorDecimal() {
        assertEquals("48.12", new StringNumeric("48.12").toString());
        assertEquals("0.5",   new StringNumeric("0.5").toString());
        assertEquals("1.5",   new StringNumeric("1.50").toString());   // trailing zero stripped
        assertEquals("5",     new StringNumeric("5.00").toString());   // all fractional zeros → integer
    }

    @Test
    void testConstructorNumericTypes() {
        assertEquals("42",   new StringNumeric(42).toString());
        assertEquals("42",   new StringNumeric(42L).toString());
        assertEquals("3.14", new StringNumeric(3.14159, 2).toString());   // rounded to 2 places
        assertEquals("3.14", new StringNumeric(3.14159f, 2).toString());
        assertEquals("1",    new StringNumeric(0.999, 0).toString());     // rounds to 1
        assertEquals("3",    new StringNumeric(3.0, 2).toString());       // trailing zeros stripped
    }

    @Test
    void testConstructorRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric(""));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric("12a3"));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric("12.a3"));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric("5."));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric((String) null));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric(-1L));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric(-1.5, 2));
        assertThrows(IllegalArgumentException.class, () -> new StringNumeric(1.5, -1));
    }

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
        "63.79, 15.67, 48.12",   // basic decimal sub
        "3.75,  1.5,   2.25",    // different scales
        "1,     0.9,   0.1",     // integer minus decimal
        "10,    9.99,  0.01",    // borrow across decimal point
        "1,     0.5,   0.5",     // result is half
        "4,     1.50,  2.5",     // trailing zeros normalise
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

    @Test
    void testSubVisualizationWithBorrow() {
        // 52 - 27 = 25, borrow from tens column
        StringNumeric a = new StringNumeric("52");
        StringNumeric b = new StringNumeric("27");

        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = a.sub(b, true);
        System.setOut(original);

        assertEquals("25", result.toString());
        assertEquals(" 1\n 52\n-27\n --\n 25", buf.toString().stripTrailing());
    }

    @Test
    void testSubVisualizationWithChainedBorrow() {
        // 100 - 1 = 99, borrow chain through two columns
        StringNumeric a = new StringNumeric("100");
        StringNumeric b = new StringNumeric("1");

        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        StringNumeric result = a.sub(b, true);
        System.setOut(original);

        assertEquals("99", result.toString());
        assertEquals(" 11\n 100\n-  1\n ---\n  99", buf.toString().stripTrailing());
    }

    @Test
    void testSubVisualizationNoBorrow() {
        // 30 - 20 = 10, no borrows
        StringNumeric a = new StringNumeric("30");
        StringNumeric b = new StringNumeric("20");

        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        a.sub(b, true);
        System.setOut(original);

        assertEquals(" 30\n-20\n --\n 10", buf.toString().stripTrailing());
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
    void testSubNegativeResultThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new StringNumeric("1").sub(new StringNumeric("2")));
    }

    @Test
    void testCompareTo() {
        StringNumeric one    = new StringNumeric("1");
        StringNumeric ten    = new StringNumeric("10");
        StringNumeric twenty = new StringNumeric("20");
        StringNumeric tenB   = new StringNumeric("10");

        assertTrue(one.compareTo(ten) < 0);
        assertTrue(ten.compareTo(twenty) < 0);
        assertTrue(twenty.compareTo(ten) > 0);
        assertEquals(0, ten.compareTo(tenB));
    }

    @Test
    void testEquals() {
        assertEquals(new StringNumeric("42"), new StringNumeric("42"));
        assertEquals(new StringNumeric("42"), new StringNumeric("042"));
        assertNotEquals(new StringNumeric("42"), new StringNumeric("43"));
    }

    @Test
    void testNumberInterface() {
        StringNumeric n = new StringNumeric("42");
        assertEquals(42, n.intValue());
        assertEquals(42L, n.longValue());
        assertEquals(42.0f, n.floatValue());
        assertEquals(42.0, n.doubleValue());
    }
}
