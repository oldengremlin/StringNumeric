package ua.org.olden.stringnumeric;

public class StringNumericDemo {

    public static void main(String[] args) {

        section("Конструктори");
        demo("String",        new StringNumeric("48.12"));
        demo("int",           new StringNumeric(42));
        demo("long",          new StringNumeric(123_456_789_012_345L));
        demo("double scale=2",new StringNumeric(3.14159, 2));
        demo("float  scale=4",new StringNumeric(2.71828f, 4));
        demo("нормалізація",  new StringNumeric("007.500"));

        section("Додавання цілих");
        add("29",  "12");
        add("999", "1");
        add("123456789", "987654321");

        section("Додавання дробових");
        add("48.12", "15.67");
        add("1.5",   "2.25");
        add("9.99",  "0.01");
        add("0.1",   "0.9");

        section("Числа за межами long");
        add("99999999999999999999", "1");
        add("99999999999999999999999999", "99999999999999999999999999");

        section("Порівняння та рівність");
        cmp("42",   "43");
        cmp("100",  "99");
        cmp("3.14", "3.14");
        cmp("1.50", "1.5");

        section("Візуалізація додавання");
        new StringNumeric("29").add(new StringNumeric("12"),   true);
        System.out.println();
        new StringNumeric("99").add(new StringNumeric("1"),    true);
        System.out.println();
        new StringNumeric("48.12").add(new StringNumeric("15.67"), true);
    }

    // --- helpers ---

    private static void section(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(40 - title.length()));
    }

    private static void demo(String label, StringNumeric n) {
        System.out.printf("  %-18s → %s%n", label, n);
    }

    private static void add(String a, String b) {
        StringNumeric result = new StringNumeric(a).add(new StringNumeric(b));
        System.out.printf("  %s + %s = %s%n", a, b, result);
    }

    private static void cmp(String a, String b) {
        StringNumeric na = new StringNumeric(a);
        StringNumeric nb = new StringNumeric(b);
        int c = na.compareTo(nb);
        String rel = c < 0 ? "<" : c > 0 ? ">" : "=";
        System.out.printf("  %s %s %s  (equals: %b)%n", a, rel, b, na.equals(nb));
    }
}
