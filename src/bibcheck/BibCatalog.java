package bibcheck;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;

import net.sf.jabref.gui.FindUnlinkedFilesDialog;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

import java.util.stream.Collectors;
// Stream handling stuff:
import java.util.stream.Stream;

public class BibCatalog {

	// To do:

	protected static BibHandler bh;
	protected static PrintWriter catalogWriter;

	public static void main(String[] args) {
		// Some basic demos:

		System.out.println("Hello world!");

		// Read the input bib file:
		System.out.println("Reading the input bib file.");
		bh = new BibHandler("C:/Jabref/allmgc.bib", "null");
		bh.ReadBibFile();
		// int NRefs = bh.entries.size();

		// Open the output file to hold the generated reports.
		System.out.println("Opening the output catalog file.");
		try {
			catalogWriter = new PrintWriter("C:/JabRef/BibCatalog.txt", "UTF-8");
		} catch (IOException e) {
			System.out.println("Unable to open output file.");
			return;
			// Do something
		}

		List<BibEntry> some = bh.entries.stream().filter(
				t->t.hasField(FieldName.FILE)
				&!t.getFieldAsWords(FieldName.KEYWORDS).contains("eprint"))
                .collect(Collectors.toList());
		PrintEntryKeys("Entries with file but no eprint keyword:",some);

		PrintEntryKeys("Entries with eprint keyword but no file:",
				bh.entries.stream().filter(
				t->!t.hasField(FieldName.FILE)
				&t.getFieldAsWords(FieldName.KEYWORDS).contains("eprint"))
                .collect(Collectors.toList()));

		PrintEntryKeys("Entries with doi but no doi keyword:",
				bh.entries.stream().filter(
				t->t.hasField(FieldName.DOI)
				&!t.getFieldAsWords(FieldName.KEYWORDS).contains("doi"))
                .collect(Collectors.toList()));

		PrintEntryKeys("Entries with doi keyword but no doi:",
				bh.entries.stream().filter(
				t->!t.hasField(FieldName.DOI)
				&t.getFieldAsWords(FieldName.KEYWORDS).contains("doi"))
                .collect(Collectors.toList()));
		
		PrintEntryKeys("Entries with no keyword field:",
				bh.entries.stream().filter(
				t->!t.hasField(FieldName.KEYWORDS))
                .collect(Collectors.toList()));
		
		CheckEprints();

		ListNonAscii();

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
		rpt.ProcessEntries(bh.entries, FieldName.PUBLISHER);
		rpt.PrintTable(catalogWriter);
		rpt.Clear();
		rpt.ProcessEntries(bh.entries, FieldName.JOURNAL);
		rpt.PrintTable(catalogWriter);
		rpt.Clear();
		rpt.ProcessEntries(bh.entries, FieldName.ADDRESS);
		rpt.PrintTable(catalogWriter);
		rpt.Clear();

		DelimFieldReporter drpt = new DelimFieldReporter(" and ");
		drpt.ProcessEntries(bh.entries, FieldName.AUTHOR);
		drpt.PrintTable(catalogWriter);
		drpt.Clear();

		DelimFieldReporter drpt2 = new DelimFieldReporter(", ");
		drpt2.ProcessEntries(bh.entries, FieldName.KEYWORDS);
		drpt2.PrintTable(catalogWriter);
		drpt2.Clear();

	}

	public static void ListNonAscii() {
		System.out.println("Detecting NonAscii characters.");
		for (BibEntry entry : bh.entries) {
			// String thisType = entry.getType();
			// Add the counts for this entry's fields.
			Set<String> thisFieldList = entry.getFieldNames();
			for (String sField : thisFieldList) {
				String s = entry.getField(sField).orElse("A");
				String BadChars = "";
				Boolean NonAscii = false;
				for (int i = 1; i < s.length(); i++) {
					Boolean ThisBad = (s.charAt(i) >= 128);
					if (ThisBad)
						catalogWriter.format("%s has NonAscii char '%s' at character %d in %s field.\n", entry.getCiteKeyOptional().orElse("NOKEY"), s.charAt(i), i, sField);
					// if (ThisBad) BadChars += s.charAt(i);
					// NonAscii = (NonAscii || ThisBad);
				}
				// if (NonAscii) catalogWriter.format("%s has NonAscii chars '%s' in %s field.\n",entry.getCiteKeyOptional().orElse("NOKEY"),BadChars,sField);
			}
		}
	}

	public static void CheckEprints() {
		System.out.println("Checking eprint files.");

		// Get a list of all eprint files in the eprints directory:
		// FileDirectoryPreferences fdPrefs;
		// List<String> sEprintDir = bh.dbc.getFileDirectory();
		String sEprintDir = "C:/eprints"; // I should be able to get this from metadata.
		File folder = new File(sEprintDir);
		File[] listOfFiles = folder.listFiles();
		// boolean a = listOfFiles[1].isDirectory();
		System.out.format("Found %d files in eprints folder %s.\n", listOfFiles.length, sEprintDir);

		// Get a list of all the files claimed by the entries.
		TreeMap<String, Integer> linkedEprints = new TreeMap<>((String.CASE_INSENSITIVE_ORDER));
		for (BibEntry entry : bh.entries) {
			String s = entry.getField(FieldName.FILE).orElse("");
			if (s.length() > 0) {
				List<ParsedFileField> fileList = FileField.parse(s);
				for (ParsedFileField file : fileList) {
					String s2 = file.getLink();
					if (!linkedEprints.containsKey(s2)) {
						linkedEprints.put(s2, 0);
					} else
						linkedEprints.put(s2, linkedEprints.get(s2) + 1);
				}
			}
		}
		System.out.format("Found references to %d files in database.\n", linkedEprints.size());

		// List any files that are claimed more than once.
		catalogWriter.format("Files linked to more than once:\n");
		for (Map.Entry<String, Integer> link : linkedEprints.entrySet()) {
			if (link.getValue() > 1)
				catalogWriter.format("% 3d : %s\n", link.getValue(), link.getKey());
		}
		catalogWriter.format("\n");

		// Reset all the values to 0 so that the map can be re-used to indicate
		// whether each file is present (value>0) or not.
		for (Map.Entry<String, Integer> link : linkedEprints.entrySet()) {
			link.setValue(0);
		}

		List<String> unLinkedFiles = new ArrayList<>();

		// Go through the list of files in the directory list.
		// Mark each one that appears as a linked file by setting IsReferenced = true.
		// Mark each linked eprint if it matches a directory file by setting Value to 1.
		for (File iFile : listOfFiles) if (!iFile.isDirectory()) {
			String thisname = iFile.getName();
			if (linkedEprints.containsKey(thisname)) {
				linkedEprints.put(thisname, 1);
			} else
				unLinkedFiles.add(thisname);
		}

		// Report any linked files that are missing.
		catalogWriter.format("Linked files that are missing:\n");
		for (Map.Entry<String, Integer> link : linkedEprints.entrySet()) {
			if (link.getValue() == 0)
				catalogWriter.format("%s\n", link.getKey());
		}
		catalogWriter.format("\n");

		// Report any directory files that are not linked.
		catalogWriter.format("Files in the directory that are not linked:\n");
		for (String fname : unLinkedFiles) {
			catalogWriter.format("%s\n", fname);
		}
		catalogWriter.format("\n");

	}  // CheckEprints
	
	public static void PrintEntryKeys(String sTitle, List<BibEntry> thisList) {
		if (thisList.size()<1) return;
		System.out.println(sTitle);
		catalogWriter.format("%s\n",sTitle);
		for (BibEntry entry : thisList) {
			catalogWriter.format("%s\n", entry.getCiteKeyOptional().get());
		}
		catalogWriter.format("\n");
	}

} // end of class
