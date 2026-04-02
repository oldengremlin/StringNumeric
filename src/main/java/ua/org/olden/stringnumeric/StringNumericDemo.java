package ua.org.olden.stringnumeric;

public class StringNumericDemo {

    public static void main(String[] args) {

        section("Конструктори");
        demo("String", new StringNumeric("-0.00"));
        demo("String", new StringNumeric("-0.0"));
        demo("String", new StringNumeric("-0"));
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

        section("Множення цілих");
        mul("29", "12");
        mul("999", "1");
        mul("123456789", "987654321");

        section("Множення дробових");
        mul("48.12", "15.67");
        mul("1.5", "2.25");
        mul("9.99", "0.01");
        mul("0.1", "0.9");

        section("Числа за межами long");
        mul("99999999999999999999", "12345678901234567890");

        section("Множення від'ємних");
        mul("-5", "3");
        mul("3", "-5");
        mul("-5", "-3");
        mul("-1.5", "2.25");
        mul("-1.5", "-2.5");

        section("Візуалізація множення");
        System.out.println(new StringNumeric("29") + "×" + new StringNumeric("12") + "="
                + new StringNumeric("29").mul(new StringNumeric("12")));
        new StringNumeric("29").mul(new StringNumeric("12"), true);
        System.out.println();
        System.out.println(new StringNumeric("-29") + "×" + new StringNumeric("12") + "="
                + new StringNumeric("-29").mul(new StringNumeric("12")));
        new StringNumeric("-29").mul(new StringNumeric("12"), true);
        System.out.println();
        System.out.println(new StringNumeric("48.12") + "×" + new StringNumeric("15.67") + "="
                + new StringNumeric("48.12").mul(new StringNumeric("15.67")));
        new StringNumeric("48.12").mul(new StringNumeric("15.67"), true);
        System.out.println();
        System.out.println(new StringNumeric("48.12") + "×" + new StringNumeric("-15.67") + "="
                + new StringNumeric("48.12").mul(new StringNumeric("-15.67")));
        new StringNumeric("48.12").mul(new StringNumeric("-15.67"), true);
        System.out.println();
        System.out.println(new StringNumeric("99") + "×" + new StringNumeric("99") + "="
                + new StringNumeric("99").mul(new StringNumeric("99")));
        new StringNumeric("99").mul(new StringNumeric("99"), true);
        System.out.println();
        System.out.println(new StringNumeric("-99") + "×" + new StringNumeric("-99") + "="
                + new StringNumeric("-99").mul(new StringNumeric("-99")));
        new StringNumeric("-99").mul(new StringNumeric("-99"), true);

        System.out.println();
        System.out.println(new StringNumeric("29") + "×" + new StringNumeric("12") + "="
                + new StringNumeric("29").mul(new StringNumeric("12")));
        new StringNumeric("29").mul(new StringNumeric("12"), true);

        section("Ділення цілих");
        div("41", "12");
        div("100", "4");
        div("123456789", "987654321");

        section("Ділення дробових");
        div("48.12", "15.67");
        div("3.75", "1.5");
        div("10", "4");
        div("1", "0.9");
        div("0.1", "0.3");

        section("Ділення з великими числами");
        div("99999999999999999999", "123456789");

        section("Ділення від'ємних");
        div("-15", "3");
        div("15", "-3");
        div("-15", "-3");
        div("-3.75", "1.5");
        div("3.75", "-1.5");

        section("Візуалізація ділення");
        divVis("48.12", "15.67");
        divVis("-100", "4");
        divVis("1", "0.9");
        divVis("48.12", "15.678");
        divVis("700", "7");

        section("Корні");
        sqrt("25");
        sqrt("10");
        sqrt("250");
        sqrt("5253");
        sqrt("100500");
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

    private static void mul(String a, String b) {
        StringNumeric result = new StringNumeric(a).mul(new StringNumeric(b));
        System.out.printf("  %s × %s = %s%n", a, b, result);
    }

    private static void div(String a, String b) {
        StringNumeric result = new StringNumeric(a).div(new StringNumeric(b));
        System.out.printf("  %s ÷ %s = %s%n", a, b, result);
    }

    private static void divVis(String a, String b) {
        System.out.println(new StringNumeric(a) + " ÷ " + new StringNumeric(b) + " = "
                + new StringNumeric(a).div(new StringNumeric(b)));
        new StringNumeric(a).div(new StringNumeric(b), true);
        System.out.println();
    }

    private static void cmp(String a, String b) {
        StringNumeric na = new StringNumeric(a);
        StringNumeric nb = new StringNumeric(b);
        int c = na.compareTo(nb);
        String rel = c < 0 ? "<" : c > 0 ? ">" : "=";
        System.out.printf("  %s %s %s  (equals: %b)%n", a, rel, b, na.equals(nb));
    }

    private static void sqrt(String a) {
        StringNumeric na = new StringNumeric(a);
        System.out.printf("  Табличний √%s ≈ %s%n", a, na.sqrtApproximate());
        System.out.printf("  Табличний з візуалізацією √%s ≈ %s%n", a, na.sqrtApproximate(true));
        System.out.printf("  Інтерактивний √%s ≈ %s%n", a, na.sqrtIterative());
        System.out.printf("  Інтерактивний з візуалізацією √%s ≈ %s%n", a, na.sqrtIterative(true));
        System.out.printf("  Стовпчиком √%s ≈ %s%n", a, na.sqrtLongDivision());
        System.out.printf("  Стовпчиком з візуалізацією √%s ≈ %s%n", a, na.sqrtLongDivision(true));

        System.out.println();
    }

}
