package bibcheck;

import java.io.PrintWriter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.event.EntryEventSource;

import org.apache.commons.lang3.ArrayUtils;
import java.io.IOException;

public class BibClean {

	// WARNING:
	//  Non-ASCII characters are not copied correctly, so use BibCatalog & editor to make sure there aren't any.

	// TODO:
	//   REMOVE LINES: LANGUAGE = {eng}
	//   DOI corrections

	protected static BibHandler bh;
	protected static PrintWriter cleanBibWriter;
	protected static Boolean SomethingChanged = false;

	public static void main(String[] args) {
		// Utility to clean a database

		System.out.println("Hello world!");

		Boolean ShortTest = false;

		if (ShortTest) {
			// Some tests fooling around with regex.
			String oldS = "(Hedges' g> = 0.20; CI\\textsubscript{95%} -0.11~0.51; k = 6) to published studies";
			String oldPat = "\\Q\\textsubscript\\E\\{([^}]+)\\}";  // Note doubled backslashes required here
			String newPat = "_{$1}";
			newPat = "\\$$1\\$";
			String newS = oldS.replaceAll(oldPat, newPat);
			System.out.println("Using patterns");
			System.out.println(oldPat);
			System.out.println(newPat);
			System.out.println(oldS);
			System.out.println("changed to");
			System.out.println(newS);

		} else {

			// Read the input bib file:
			bh = new BibHandler("C:/Jabref/allmgc.bib", "C:/Jabref/allmgc.clean");
			bh.ReadBibFile();
			System.out.println("Done reading the input bib file.");

			KillField("*", FieldName.TIMESTAMP);
			KillField("article", FieldName.ADDRESS);
			KillField("article", FieldName.ISSN);
			KillField("article", FieldName.PUBLISHER);
			KillField("article", "document_type");
			KillField("article", "article_type");
			KillField("article", "elocation-id");
			KillField("article", "citation");
			KillField("article", "pub_date");
			KillField("article", "source");
			KillField("article", "refid");
			KillField("article", "day");
			KillField("article", "status");
			//       KillField("article", "month"); // Want month for some refs, e.g., Science
			KillField("article", "subset");
			KillField("article", "title-abbreviation");
			KillField("article", "linking-issn");
			KillField("article", "location-id");
			KillField("article", "electronic-issn");
			KillField("article", "history");
			KillField("article", "nlm-unique-id");
			KillField("article", "owner");
			KillField("article", "publication-status");
			KillField("article", "revised");
			// KillField("article", "");
			KillFieldAifB("article", FieldName.EPRINT, FieldName.DOI);
			KillFieldAifB("article", "article-doi", FieldName.DOI);
			KillFieldAifB("article", FieldName.EPRINT, FieldName.FILE);
			KillFieldAifB("*", FieldName.URL, FieldName.DOI);

			// Replace '{\&}' with '\&' in journals, but...
			ReplaceInField("*", FieldName.JOURNAL, "\\Q{\\&}\\E", "\\&");
			// Replacing {\&} with \& is complicated due to escape sequences & regex!
			// \Q tells regex to "escape" to quoted mode and \E signals the end of the mode.

			// Kill any final period in address or publisher field.
			// \Q quotes the period (which is otherwise any character) and \Z ends the string.
			ReplaceInField("*", FieldName.ADDRESS, "\\Q.\\E\\Z", "");
			ReplaceInField("*", FieldName.PUBLISHER, "\\Q.\\E\\Z", "");
			ReplaceInField("*", FieldName.DOI, "//", "/");  // Replace 2 forward slashes with one in DOIs
			ReplaceInField("article", FieldName.KEYWORDS, " own, ", " ");  // Remove 'own, ' from keywords

			ReplaceJournalTitles();
			ReplaceFieldTextFromFile("*", FieldName.AUTHOR, "C:/JabRef/AuthorCorrections.tab");
			ReplaceFieldTextFromFile("*", FieldName.ABSTRACT, "C:/JabRef/AbstractCorrections.tab");
			ReplaceFieldTextFromFile("*", FieldName.TITLE, "C:/JabRef/TitleCorrections.tab");
			ReplaceFieldTextFromFile("*", FieldName.KEYWORDS, "C:/JabRef/KeywordCorrections.tab");
			ReplaceFieldTextFromFile("*", FieldName.PAGES, "C:/JabRef/PageCorrections.tab");
			
			KillFieldAifBeqFile("article", "month", "journal", "C:/Jabref/Journals2DeleteMonth.txt");

			if (SomethingChanged) {
				// Write out the revised database
				System.out.println("Writing out the cleaned database.");
				bh.WriteBibFile();
				// Writing the entire database ends here
			} else {
				System.out.println("No changes needed.");
			}

		} // % end else from ShortTest

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
		if (NKilled>0) {
			System.out.format("Removed %d %s fields from %s references with %s.\n", NKilled, fieldNameA, entryType, fieldNameB);
			SomethingChanged = true;
		}
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

	public static void ReplaceInField(String entryType, String fieldName, String oldPat, String newPat) {
		int NReplaced = 0; // Counts modified entries, but there could be multiple replacements in one entry.
		EntryEventSource eventSource = EntryEventSource.LOCAL;
		for (BibEntry entry : bh.entries) {
			if (((entryType.equals("*")) || (entryType.equalsIgnoreCase(entry.getType()))) &&
					(FieldReporter.FieldPresent(entry, fieldName))) {
				// Make replacements in field, if any
				String oldS = entry.getField(fieldName).orElse("");
				String newS = oldS;
				if (fieldName.equals(FieldName.KEYWORDS)) {
					// for the keyword field, use string.split to separate keywords with delimiter ', '.
					// Then just replace exact matches.
					// Then re-assemble using string.join
					String[] Parts = oldS.split(", ");
					int numel = Parts.length;
					for (int i=0; i<numel; i++) {
						if (oldPat.equals(Parts[i])) {
							Parts[i] = newPat;
						}
					}
					newS = String.join(", ",Parts);
				} else {
					newS = oldS.replaceAll(oldPat, newPat);
				}
				if (!oldS.equals(newS)) {
					NReplaced++;
					entry.setField(fieldName, newS, eventSource);
				}
			}
		}
		if (NReplaced>0) {
			System.out.format("Replaced %s with %s in %d %s fields from %s references.\n", oldPat, newPat, NReplaced, fieldName, entryType);
			SomethingChanged = true;
		}
	}

	public static void ReplaceExactField(String entryType, String fieldName, String oldVal, String newVal) {
		int NReplaced = 0; // Counts modified entries, but there could be multiple replacements in one entry.
		EntryEventSource eventSource = EntryEventSource.LOCAL;
		for (BibEntry entry : bh.entries) {
			if (((entryType.equals("*")) || (entryType.equalsIgnoreCase(entry.getType()))) &&
					(FieldReporter.FieldPresent(entry, fieldName))) {
				// Make replacements in field, if any
				String oldS = entry.getField(fieldName).orElse("");
				if (oldS.equals(oldVal)) {
					NReplaced++;
					entry.setField(fieldName, newVal, eventSource);
				}
			}
		}
		if (NReplaced>0) {
			System.out.format("Replaced %s with %s in %d %s fields from %s references.\n", oldVal, newVal, NReplaced, fieldName, entryType);
			SomethingChanged = true;
		}
	}

	public static void ReplaceJournalTitles() {
		// Read a tab-separated file with Alias-Correct_name pairs on each line and
		// replace any occurrences of the alias with the correct name.
		FileArrayProvider fap = new FileArrayProvider();
		String[] AliasCorrectLines;
		try {
			AliasCorrectLines = fap.readLines("C:/JabRef/JournalNameCorrections.tab");
		} catch (IOException e) {
			System.out.println("Could not find the file JournalNameCorrections.tab");
			return;
		}
		String[] OnePair = new String[2];
		for (String s : AliasCorrectLines) {
			OnePair = s.split("\t");
			ReplaceExactField("article", FieldName.JOURNAL, OnePair[0], OnePair[1]);
		}
	}

	public static void ReplaceFieldTextFromFile(String entryType, String fieldName, String fileName) {
		// Read a tab-separated file with Alias-Correct_name pairs on each line and
		// replace any occurrences of the alias with the correct name.
		FileArrayProvider fap = new FileArrayProvider();
		String[] AliasCorrectLines;
		try {
			AliasCorrectLines = fap.readLines(fileName);
		} catch (IOException e) {
			System.out.println("ReplaceFieldTextFromFile could not find the requested file.");
			return;
		}
		String[] OnePair = new String[2];
		for (String s : AliasCorrectLines) {
			OnePair = s.split("\t");
			ReplaceInField(entryType, fieldName, OnePair[0], OnePair[1]);
		}
	}

	public static void KillFieldAifBeqFile(String entryType, String fieldNameA, String fieldNameB, String fileNameC) {
		// Remove the indicated fieldA from the indicated entryType if the contents of fieldB matches a line in fileC.
		// Use entryType = "*" to indicate all entry types.
		// Example: KillFieldAifBeqFile("article", "month", "journal", "Journals2DeleteMonth.txt")
		FileArrayProvider fap = new FileArrayProvider();
		String[] StringsInFile;
		try {
			StringsInFile = fap.readLines(fileNameC);
		} catch (IOException e) {
			System.out.format("Could not find the file %s.\n",fileNameC);
			return;
		}
		int NKilled = 0;
		for (String s : StringsInFile) {
			for (BibEntry entry : bh.entries) {
				String fieldBContents = entry.getField(fieldNameB).orElse("");
				if (((entryType.equals("*")) || (entryType.equalsIgnoreCase(entry.getType()))) &&
						(FieldReporter.FieldPresent(entry, fieldNameA)) &&
						(FieldReporter.FieldPresent(entry, fieldNameB)) &&
						(fieldBContents.equals(s))        ) {
					NKilled++;
					entry.clearField(fieldNameA);
				} // if entry is correct type & has field with value s
			} // for bh.entries
		}  // for StringsInFile

		if (NKilled>0) {
			System.out.format("Removed %d %s fields from %s references with %s matching a line in %s.\n", NKilled, fieldNameA, entryType, fieldNameB, fileNameC);
			SomethingChanged = true;
		}
	}  // KillFieldAifBeqFile
	
}  // class body

