package bibcheck;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.exporter.BibDatabaseWriter;
import net.sf.jabref.logic.exporter.BibtexDatabaseWriter;
import net.sf.jabref.logic.exporter.FileSaveSession;
import net.sf.jabref.logic.exporter.SaveException;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.exporter.SaveSession;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

public class BibHandler {

    String inputBibFileName;
    String outputBibFileName;
    List<BibEntry> entries;
    ImportFormatPreferences impFmtPrefs;
    BibtexParser btp;
    BibDatabaseContext dbc;
    ParserResult result;
    JabRefPreferences jrPrefs;
    SavePreferences prefs;
    BibDatabaseWriter<SaveSession> databaseWriter;
    SaveSession session;
    BibDatabase db;
    PrintWriter writer;

    public BibHandler(String inBibName, String outBibName) {
        inputBibFileName = inBibName;
        outputBibFileName = outBibName;
        impFmtPrefs = JabRefPreferences.getInstance().getImportFormatPreferences();
        btp = new BibtexParser(impFmtPrefs);
        jrPrefs = JabRefPreferences.getInstance();
        prefs = SavePreferences.loadForSaveFromPreferences(jrPrefs);
        databaseWriter = new BibtexDatabaseWriter<>(FileSaveSession::new);
    }

    public void ReadBibFile() {
        // Read the input bib file:
        try (BufferedReader br = new BufferedReader(new FileReader(inputBibFileName))) {
            result = btp.parse(br); // Read the whole file
            dbc = result.getDatabaseContext();
        } // end of try for BufferedReader
        catch (IOException e) {
            System.out.println("Unable to read input bib file.");
            System.exit(1);
            return;
        }
        // System.out.println("Finished reading the input bib file.");
        // Retrieve the individual entries in the database for processing.
        db = dbc.getDatabase();
        entries = db.getEntries();
    }

    public void WriteBibFile() {
        try {
        	// prefs = result.getBibDatabaseContext().getMetaData().getEncoding().orElse(Globals.prefs.getDefaultEncoding());
            session = databaseWriter.saveDatabase(dbc, prefs);
            // Show just a warning message if encoding did not work for all characters:
            if (!session.getWriter().couldEncodeAll()) {
                System.err.println(Localization.lang("Warning") + ": "
                        + Localization.lang("The chosen encoding '%0' could not encode the following characters:",
                                session.getEncoding().displayName())
                        + " " + session.getWriter().getProblemCharacters());
            }
            session.commit(outputBibFileName);
        } catch (SaveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
