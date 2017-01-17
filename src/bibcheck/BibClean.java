package bibcheck;

import java.io.PrintWriter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.event.EntryEventSource;

import org.apache.commons.lang3.ArrayUtils;
import java.io.IOException;

public class BibClean {

    // NEWJEFF: To do:
    //  non-ASCII is not being copied correctly.
    //  More general Journal replacement allowing, eg, for leading "The"
	
    protected static BibHandler bh;
    protected static PrintWriter cleanBibWriter;

    public static void main(String[] args) {
        // Utility to clean a database

        System.out.println("Hello world!");

        // Read the input bib file:
        bh = new BibHandler("C:/Jabref/allmgc.bib", "C:/Jabref/allmgc.clean");
        bh.ReadBibFile();
        System.out.println("Read the input bib file.");

        KillField("*", "timestamp");
        KillField("article", "address");
        KillField("article", "issn");
        KillField("article", "publisher");
        KillFieldAifB("*", "url", "doi");

        // Replace '{\&}' with '\&' in journals, but...
        ReplaceInField("*", "journal", "\\Q{\\&}\\E", "\\&");
        // Replacing {\&} with \& is complicated due to escape sequences & regex!
        // \Q tells regex to "escape" to quoted mode and \E signals the end of the mode.

        // Kill any final period in address or publisher field.
        // \Q quotes the period (which is otherwise any character) and \Z ends the string.
        ReplaceInField("*", "address", "\\Q.\\E\\Z", "");
        ReplaceInField("*", "publisher", "\\Q.\\E\\Z", "");

        FileArrayProvider fap = new FileArrayProvider();
        try {
            String[] JournalsWithAmpersands = fap.readLines("C:/JabRef/JournalsWithAmpersands.txt");
            ReplaceAndWithAmpersand("*", "journal", JournalsWithAmpersands);
        } catch (IOException e) {
            System.out.println("Could not find file JournalsWithAmpersands.txt");
        }

        // Write out the revised database
        System.out.println("Writing out the cleaned database.");
        bh.WriteBibFile();
        // Writing the entire database ends here

        System.out.println("Goodbye world!");

    }

    public static void KillFieldAifB(String entryType, String fieldNameA, String fieldNameB) {
        // Remove the indicated fieldA from the indicated entryType if fieldB is present.
        // Use entryType = "*" to indicate all entry types.
        int NKilled = 0;
        for (BibEntry entry : bh.entries) {
            if (((entryType.equals("*")) || (entryType.equalsIgnoreCase(entry.getType()))) &&
                    (FieldReporter.FieldPresent(entry, fieldNameA)) &&
                    (FieldReporter.FieldPresent(entry, fieldNameB)) ) {
                // Try to set a field here
                NKilled++;
                entry.clearField(fieldNameA); // , eventSource);
            }
        }
        System.out.format("Removed %d %s fields from %s references with %s.\n", NKilled, fieldNameA, entryType, fieldNameB);
    }

    public static void KillField(String entryType, String fieldName) {
        // Remove the indicated field from the indicated entryType.
        // Use entryType = "*" to indicate all entry types.
        int NKilled = 0;
        for (BibEntry entry : bh.entries) {
            if (((entryType.equals("*")) || (entryType.equalsIgnoreCase(entry.getType()))) &&
                    (FieldReporter.FieldPresent(entry, fieldName))) {
                // Try to set a field here
                NKilled++;
                entry.clearField(fieldName); // , eventSource);
            }
        }
        System.out.format("Removed %d %s fields from %s references.\n", NKilled, fieldName, entryType);
    }

    public static void ReplaceInField(String entryType, String fieldName, String oldPat, String newPat) {
        int NReplaced = 0; // Counts modified entries, but there could be multiple replacements in one entry.
        EntryEventSource eventSource = EntryEventSource.LOCAL;
        for (BibEntry entry : bh.entries) {
            if (((entryType.equals("*")) || (entryType.equalsIgnoreCase(entry.getType()))) &&
                    (FieldReporter.FieldPresent(entry, fieldName))) {
                // Make replacements in field, if any
                String oldS = entry.getField(fieldName).orElse("");
                String newS = oldS.replaceAll(oldPat, newPat);
                if (!oldS.equals(newS)) {
                    NReplaced++;
                    entry.setField(fieldName, newS, eventSource);
                }
            }
        }
        System.out.format("Replaced %s with %s in %d %s fields from %s references.\n", oldPat, newPat, NReplaced,
                fieldName, entryType);
    }

    public static void ReplaceAndWithAmpersand(String entryType, String fieldName, String[] oldStrings) {
        int NReplaced = 0; // Counts modified entries, but there could be multiple replacements in one entry.
        EntryEventSource eventSource = EntryEventSource.LOCAL;
        for (BibEntry entry : bh.entries) {
            if (((entryType.equals("*")) || (entryType.equalsIgnoreCase(entry.getType()))) &&
                    (FieldReporter.FieldPresent(entry, fieldName))) {
                // Make replacements in field, if any
                String oldS = entry.getField(fieldName).orElse("");
                if (ArrayUtils.contains(oldStrings, oldS)) {
                    String newS = oldS.replaceAll(" and ", " \\& ");
                    NReplaced++;
                    entry.setField(fieldName, newS, eventSource);
                }
            }
        }
        System.out.format("Replaced 'and' with '&' in %d %s fields from %s references.\n", NReplaced, fieldName,
                entryType);

    }


}
