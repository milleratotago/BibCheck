package bibcheck;

import java.io.*;

import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

import net.sf.jabref.model.entry.BibEntry;

public class BibCatalog {

    // NEWJEFF: To do:
    //  DOI checks
    //  eprint checks
    //  ASCII

    protected static BibHandler bh;
    protected static PrintWriter catalogWriter;

    public static void main(String[] args) {
        // Some basic demos:

        System.out.println("Hello world!");

        // Read the input bib file:
        bh = new BibHandler("C:/Jabref/allmgc.bib", "null");
        bh.ReadBibFile();
        System.out.println("Read the input bib file.");

        // Open the output file to hold the generated reports.
        try {
            catalogWriter = new PrintWriter("C:/JabRef/BibCatalog.txt", "UTF-8");
        } catch (IOException e) {
            System.out.println("Unable to open output file.");
            return;
            // Do something
        }

        TypeAndFieldCounts();

        FieldContents();

        catalogWriter.close();

        System.out.println("Goodbye world!");
    }

    public static void TypeAndFieldCounts() {
        // Count the occurrences of the different entry types & of the different fields within each entry type.
        // Each value of the entrytypeMap will be a TreeMap for the fields/counts of that entrytype.
        System.out.println("Counting reference types and fields.");
        TreeMap<String, Integer> entrytypeCountMap = new TreeMap<>();
        TreeMap<String, TreeMap<String, Integer>> entrytypeFieldsMap = new TreeMap<>();
        for (BibEntry entry : bh.entries) {
            String thisType = entry.getType();
            if (!entrytypeCountMap.containsKey(thisType)) {
                entrytypeCountMap.put(thisType, 0);
            }
            entrytypeCountMap.put(thisType, entrytypeCountMap.get(thisType) + 1);
            // Make sure this entry type is in entrytypeMap
            if (!entrytypeFieldsMap.containsKey(thisType)) {
                entrytypeFieldsMap.put(thisType, new TreeMap<String, Integer>());
            }
            TreeMap<String, Integer> thisfieldMap = entrytypeFieldsMap.get(thisType);
            // Add the counts for this entry's fields.
            Set<String> thisFieldList = entry.getFieldNames();
            for (String sField : thisFieldList) {
                if (!thisfieldMap.containsKey(sField)) {
                    thisfieldMap.put(sField, 0);
                }
                thisfieldMap.put(sField, thisfieldMap.get(sField) + 1);
            }
        }

        // Print the article types & counts.
        catalogWriter.format("Entry counts & types:\n");
        for (Map.Entry<String, Integer> entrytype : entrytypeCountMap.entrySet()) {
            catalogWriter.format("% 6d : %s\n", entrytype.getValue(), entrytype.getKey());
        }
        catalogWriter.format("\n");

        // Print the article types, fields, and counts.
        for (Map.Entry<String, Integer> entrytype : entrytypeCountMap.entrySet()) {
            String thisType = entrytype.getKey();
            catalogWriter.format("% 6d : %s entries:\n", entrytype.getValue(), thisType);
            TreeMap<String, Integer> thisfieldMap = entrytypeFieldsMap.get(thisType);
            for (Map.Entry<String, Integer> field : thisfieldMap.entrySet()) {
                catalogWriter.format("% 12d : %s\n", field.getValue(), field.getKey());
            }
            catalogWriter.format("\n");
        }

    }

    public static void FieldContents() {
        // Report on the contents of each field using FieldReporter Class
        FieldReporter rpt = new FieldReporter();
        rpt.ProcessEntries(bh.entries, "publisher");
        rpt.PrintTable(catalogWriter);
        rpt.Clear();
        rpt.ProcessEntries(bh.entries, "journal");
        rpt.PrintTable(catalogWriter);
        rpt.Clear();
        rpt.ProcessEntries(bh.entries, "address");
        rpt.PrintTable(catalogWriter);
        rpt.Clear();

        DelimFieldReporter drpt = new DelimFieldReporter(" and ");
        drpt.ProcessEntries(bh.entries, "author");
        drpt.PrintTable(catalogWriter);
        drpt.Clear();

        DelimFieldReporter drpt2 = new DelimFieldReporter(", ");
        drpt2.ProcessEntries(bh.entries, "keywords");
        drpt2.PrintTable(catalogWriter);
        drpt2.Clear();

    }

}
