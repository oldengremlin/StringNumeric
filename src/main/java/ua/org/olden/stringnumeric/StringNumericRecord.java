package ua.org.olden.stringnumeric;

/**
 *
 * @author olden
 */
public class StringNumericRecord {

    private final StringNumeric value;
    private final String visualize;

    public StringNumericRecord(StringNumeric value, String visualize) {
        this.value = value;
        this.visualize = visualize;
    }

    public StringNumeric value() {
        return value;
    }

    public String visualize() {
        return this.visualize;
    }

}
