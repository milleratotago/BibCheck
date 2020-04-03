package bibcheck;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.sf.jabref.model.entry.BibEntry;

public class FieldReporter {

    protected final TreeMap<String, Integer> MyTOC;

    protected String TargetField;

    public FieldReporter() {
        MyTOC = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    public static boolean FieldPresent(BibEntry entry, String fieldName) {
        return entry.getField(fieldName).isPresent();
    }

    public void Clear() {
        MyTOC.clear();
    }

    public String[] FieldToTokens(String fieldContents) {
        String[] news = {fieldContents};
        return news;
    }

    public void ProcessEntries(List<BibEntry> entries, String targetField) {
        // go through all the entries and build the table of contents
        TargetField = targetField;
        System.out.println("Processing " + TargetField);
        for (BibEntry entry : entries) {
            if (FieldPresent(entry, TargetField)) {
                String fieldContents = entry.getField(TargetField).orElse("DISASTER");
                String[] fieldTokens = FieldToTokens(fieldContents);
                for (String s : fieldTokens) {
                    if (!MyTOC.containsKey(s)) {
                        MyTOC.put(s, 0);
                    }
                    MyTOC.put(s, MyTOC.get(s) + 1);
                }
            }
        }
    }

    public void PrintTable(PrintWriter writer) {
        // Overload to provide a default formatstr
        PrintTable(writer, "% 6d : %s\n");
    }

    public void PrintTable(PrintWriter writer, String formatstr) {
        // Print counts and keys
        writer.format("*** Report for \"%s\" field:\n", TargetField);
        for (Map.Entry<String, Integer> entry : MyTOC.entrySet()) {
            writer.format(formatstr, entry.getValue(), entry.getKey());
        }
        writer.format("\n");
    }

}
