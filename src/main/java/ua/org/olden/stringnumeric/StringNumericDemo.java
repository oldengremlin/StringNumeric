package ua.org.olden.stringnumeric;

public class StringNumericDemo {

    public static void main(String[] args) {

        section("Конструктори");
        demo("String", new StringNumeric("48.12"));
        demo("int", new StringNumeric(42));
        demo("long", new StringNumeric(123_456_789_012_345L));
        demo("double scale=2", new StringNumeric(3.14159, 2));
        demo("float  scale=4", new StringNumeric(2.71828f, 4));
        demo("нормалізація", new StringNumeric("007.500"));

        section("Додавання цілих");
        add("29", "12");
        add("999", "1");
        add("123456789", "987654321");

        section("Додавання дробових");
        add("48.12", "15.67");
        add("1.5", "2.25");
        add("9.99", "0.01");
        add("0.1", "0.9");

        section("Числа за межами long");
        add("99999999999999999999", "1");
        add("99999999999999999999999999", "99999999999999999999999999");

        section("Віднімання цілих");
        sub("41", "12");
        sub("1000", "1");
        sub("1111111110", "987654321");

        section("Віднімання дробових");
        sub("63.79", "15.67");
        sub("3.75", "1.5");
        sub("10", "9.99");
        sub("1", "0.9");

        section("Числа за межами long");
        sub("100000000000000000000", "1");

        section("Порівняння та рівність");
        cmp("42", "43");
        cmp("100", "99");
        cmp("3.14", "3.14");
        cmp("1.50", "1.5");

        section("Візуалізація додавання");
        new StringNumeric("29").add(new StringNumeric("12"), true);
        System.out.println();
        new StringNumeric("99").add(new StringNumeric("1"), true);
        System.out.println();
        new StringNumeric("48.12").add(new StringNumeric("15.67"), true);

        section("Візуалізація віднімання");
        new StringNumeric("52").sub(new StringNumeric("27"), true);
        System.out.println();
        new StringNumeric("100").sub(new StringNumeric("1"), true);
        System.out.println();
        new StringNumeric("63.79").sub(new StringNumeric("15.67"), true);

        // ── від'ємні числа ────────────────────────────────────────────────────
        section("Від'ємні числа — конструктори");
        demo("String \"-42\"", new StringNumeric("-42"));
        demo("String \"-3.14\"", new StringNumeric("-3.14"));
        demo("long -42", new StringNumeric(-42L));
        demo("double -3.14159 s=2", new StringNumeric(-3.14159, 2));
        demo("float  -2.71828 s=4", new StringNumeric(-2.71828f, 4));
        demo("\"-0\" → 0", new StringNumeric("-0"));

        section("Від'ємні числа — додавання");
        add("-1", "-2");    // обидва від'ємні
        add("-5", "3");     // |від'ємний| > |додатний|
        add("3", "-5");    // |додатний| < |від'ємний|
        add("-5", "5");     // взаємно скасовуються → 0
        add("-1.5", "1");
        add("1", "-1.5");
        add("-99999999999999999999", "99999999999999999999"); // великі числа

        section("Від'ємні числа — віднімання");
        sub("1", "2");     // додатний мінус більший → від'ємний
        sub("0", "5");
        sub("-5", "-3");     // -5 - (-3) = -2
        sub("-3", "-5");     // -3 - (-5) = +2
        sub("-1", "2");     // від'ємний мінус додатний
        sub("5", "-3");     // додатний мінус від'ємний → більший додатний
        sub("1.5", "3");
        sub("-1.5", "-3");

        section("Від'ємні числа — negate");
        demo("negate(5)", new StringNumeric("5").negate());
        demo("negate(-5)", new StringNumeric("-5").negate());
        demo("negate(0)", new StringNumeric("0").negate());
        demo("negate(-0.5)", new StringNumeric("-0.5").negate());

        section("Від'ємні числа — порівняння та рівність");
        cmp("-5", "3");
        cmp("-5", "-3");
        cmp("-3", "-5");
        cmp("-42", "-42");

        section("\"Складні\" операції з від'ємними числами");
        System.out.println("Доодаємо до 48.12 значення -15.67 = "
                + new StringNumeric("48.12").add(new StringNumeric("-15.67"), false));
        new StringNumeric("48.12").add(new StringNumeric("-15.67"), true);
        System.out.println("Доодаємо до -48.12 значення 15.67 = "
                + new StringNumeric("-48.12").add(new StringNumeric("15.67"), false));
        new StringNumeric("-48.12").add(new StringNumeric("15.67"), true);
        System.out.println("Доодаємо до -48.12 значення -15.67 = "
                + new StringNumeric("-48.12").add(new StringNumeric("-15.67"), false));
        new StringNumeric("-48.12").add(new StringNumeric("-15.67"), true);
        System.out.println();
        System.out.println("Віднімаємо від -63.79 значення 15.67 = "
                + new StringNumeric("-63.79").sub(new StringNumeric("15.67"), false));
        new StringNumeric("-63.79").sub(new StringNumeric("15.67"), true);
        System.out.println("Віднімаємо від 63.79 значення -15.67 = "
                + new StringNumeric("63.79").sub(new StringNumeric("-15.67"), false));
        new StringNumeric("63.79").sub(new StringNumeric("-15.67"), true);
        System.out.println("Віднімаємо від -63.79 значення -15.67 = "
                + new StringNumeric("-63.79").sub(new StringNumeric("-15.67"), false));
        new StringNumeric("-63.79").sub(new StringNumeric("-15.67"), true);

    }

    // --- helpers ---
    private static void section(String title) {
        System.out.println("\n── " + title + " " + "─".repeat(Math.max(0, 40 - title.length())));
    }

    private static void demo(String label, StringNumeric n) {
        System.out.printf("  %-22s → %s%n", label, n);
    }

    private static void add(String a, String b) {
        StringNumeric result = new StringNumeric(a).add(new StringNumeric(b));
        System.out.printf("  %s + %s = %s%n", a, b, result);
    }

    private static void sub(String a, String b) {
        StringNumeric result = new StringNumeric(a).sub(new StringNumeric(b));
        System.out.printf("  %s - %s = %s%n", a, b, result);
    }

    private static void cmp(String a, String b) {
        StringNumeric na = new StringNumeric(a);
        StringNumeric nb = new StringNumeric(b);
        int c = na.compareTo(nb);
        String rel = c < 0 ? "<" : c > 0 ? ">" : "=";
        System.out.printf("  %s %s %s  (equals: %b)%n", a, rel, b, na.equals(nb));
    }
}
