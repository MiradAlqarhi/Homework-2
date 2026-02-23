import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LibraryBookTracker {

    static List<Book> books = new ArrayList<>();
    static int errorCount = 0;

    /**
     * Program entry point.
     * @param args command line arguments
     */
    public static void main(String[] args) {

        try {
            runProgram(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println(
                    "Thank you for using The Library Book Tracker."
            );
        }
    }

    /**
     * Controls overall program execution.
     * @param args command-line arguments
     * @throws Exception if validation fails
     */
    static void runProgram(String[] args) throws Exception {

        if (args.length < 2)
            throw new InsufficientArgumentsException(
                    "Not enough arguments provided.");

        if (!args[0].endsWith(".txt"))
            throw new InvalidFileNameException(
                    "Catalog file must end with .txt");

        File catalog = new File(args[0]);
        if (catalog.getParentFile() != null) {
            catalog.getParentFile().mkdirs();
        }

        catalog.createNewFile();

        loadBooks(catalog);

        String operation = args[1];

        if (operation.matches("\\d{13}")) {
            searchByISBN(operation);
        }
        else if (operation.contains(":")) {
            addBook(operation, catalog);
        }
        else {
            searchByTitle(operation);
        }
    }

    /**
     * Reads catalog file and loads valid books.
     * @param file catalog file
     * @throws IOException if file reading fails
     */
    static void loadBooks(File file) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
            try {
                Book b = parseBook(line);
                books.add(b);
            } catch (BookCatalogException e) {
                logError(file, line, e.getMessage());
            }
        }

        br.close();
    }

    /**
     * Converts a text line into a Book object.
     * @param line catalog line
     * @return Book object
     * @throws BookCatalogException if format is invalid
     */
    static Book parseBook(String line)
            throws BookCatalogException {

        String[] parts = line.split(":");

        if (parts.length != 4)
            throw new MalformedBookEntryException(
                    "Missing required fields.");

        String title = parts[0];
        String author = parts[1];
        String isbn = parts[2];
        String copiesStr = parts[3];

        if (title.isEmpty() || author.isEmpty())
            throw new MalformedBookEntryException(
                    "Title or author is empty.");

        if (!isbn.matches("\\d{13}"))
            throw new InvalidISBNException(
                    "ISBN must contain exactly 13 digits.");

        int copies;

        try {
            copies = Integer.parseInt(copiesStr);
            if (copies <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException(
                    "Copies must be a positive integer.");
        }

        return new Book(title, author, isbn, copies);
    }

    /**
     * Searches books by title keyword.
     * @param keyword search keyword
     */
    static void searchByTitle(String keyword) {

        printHeader();

        int count = 0;

        for (Book b : books) {
            if (b.title.toLowerCase()
                    .contains(keyword.toLowerCase())) {
                printBook(b);
                count++;
            }
        }

        System.out.println("Results: " + count);
    }

    /**
     * Searches book by ISBN.
     * @param isbn ISBN number
     * @throws DuplicateISBNException if duplicates exist
     */
    static void searchByISBN(String isbn)
            throws DuplicateISBNException {

        List<Book> found = new ArrayList<>();

        for (Book b : books)
            if (b.isbn.equals(isbn))
                found.add(b);

        if (found.size() > 1)
            throw new DuplicateISBNException(
                    "Duplicate ISBN detected.");

        printHeader();

        for (Book b : found)
            printBook(b);
    }

    /**
     * Adds a new book and rewrites catalog sorted by title.
     * @param record book record string
     * @param file catalog file
     * @throws Exception if parsing or writing fails
     */
    static void addBook(String record, File file)
            throws Exception {

        Book newBook = parseBook(record);

        books.add(newBook);

        books.sort(Comparator.comparing(b -> b.title));

        BufferedWriter bw =
                new BufferedWriter(new FileWriter(file));

        for (Book b : books) {
            bw.write(b.title + ":" + b.author + ":" +
                    b.isbn + ":" + b.copies);
            bw.newLine();
        }

        bw.close();

        printHeader();
        printBook(newBook);
    }

    /**
     * Prints table header.
     */
    static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s\n",
                "Title", "Author", "ISBN", "Copies");
    }

    /**
     * Prints a formatted book row.
     * @param b book object
     */
    static void printBook(Book b) {
        System.out.printf("%-30s %-20s %-15s %5d\n",
                b.title, b.author, b.isbn, b.copies);
    }

    /**
     * Logs invalid entries into errors.log file.
     * @param catalog catalog file
     * @param text invalid line
     * @param msg error message
     */
    static void logError(File catalog,
                         String text,
                         String msg) {

        try {

            File log =
                    new File(catalog.getParent(), "errors.log");

            BufferedWriter bw =
                    new BufferedWriter(
                            new FileWriter(log, true));

            String time = LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            bw.write("[" + time + "] INVALID LINE: \"" +
                    text + "\" - " + msg);

            bw.newLine();
            bw.close();

            errorCount++;

        } catch (IOException ignored) {}
    }
}