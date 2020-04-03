package bibcheck;

import java.io.PrintWriter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.event.EntryEventSource;

import org.apache.commons.lang3.ArrayUtils;
import java.io.IOException;

public class SelectFields {

    // WARNING:
    //  Non-ASCII characters are not copied correctly, so use BibCatalog & editor to make sure there aren't any.

    // TODO:

    protected static BibHandler bh;
    protected static PrintWriter cleanBibWriter;
    protected static Boolean SomethingChanged = false;

    public static void main(String[] args) {
        // Utility to clean a database

		// *** Select file names (further selection at *** below)
    	String FilePath = "G:/eprints/";
		String Infile = "MillerJeffBIB.bib";
		String Outfile = "ToPub2.bib";

		System.out.println("Hello world!");

        // Read the input bib file:
        bh = new BibHandler(FilePath+Infile, FilePath+Outfile);
        bh.ReadBibFile();
        System.out.println("Done reading the input bib file.");

        // *** Select which fields to kill depending on what you want to share:
        // KillField("*", "abstract");
        // KillField("*", "pmid");
        KillField("*", FieldName.TIMESTAMP);
        KillField("*", "file");
        KillField("*", "groups");
        KillField("*", "jomhidden");
        KillField("*", "jomnotes");
        KillField("*", "keywords");
        KillField("*", "libaddr");
        KillField("*", "__markedentry");

        System.out.println("Writing out the cleaned database.");
        bh.WriteBibFile();

        System.out.println("Goodbye world!");

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
        if (NKilled>0) {
        	System.out.format("Removed %d %s fields from %s references.\n", NKilled, fieldName, entryType);
        	SomethingChanged = true;
        }
    }


}
