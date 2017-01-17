package bibcheck;

// import java.util.TreeMap;

public class DelimFieldReporter extends FieldReporter {

    private final String delim;

    public DelimFieldReporter(String passdelim) {
        super(); // MyTOC = new TreeMap<>();
        delim = passdelim;
    }

    @Override
    public String[] FieldToTokens(String fieldContents) {
        return fieldContents.split(delim);
    }

}
