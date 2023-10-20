import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class BayesSpamFilter {
    private static final String hamAnlernPath = "src/main/resources/data/ham-anlern";
    private static final String spamAnlernPath = "src/main/resources/data/spam-anlern";
    private static final String hamTestPath = "src/main/resources/data/ham-test";
    private static final String spamTestPath = "src/main/resources/data/spam-test";
    private static final Map<String, Integer> hamWordCounts = new HashMap<>();
    private static final Map<String, Integer> spamWordCounts = new HashMap<>();
    private static double alpha = 0.9; // Kleiner Wert für unbekannte Wörter
    private static double threshold = 0.8; // Schwellenwert für die Klassifikation

    public static void main(String[] args) {
        BayesSpamFilter spamFilter = new BayesSpamFilter();

        // Trainieren des Filters mit Ham- und Spam-Mails
        spamFilter.train(hamAnlernPath, false);
        spamFilter.train(spamAnlernPath, true);

        System.out.println("Bestimmter Alpha: " + alpha);
        System.out.println("Bestimmter Schwellenwert: " + threshold);

        double accuracy = spamFilter.evaluateAccuracy(hamTestPath, spamTestPath);

        System.out.println("Es wurden " + String.format("%.2f", accuracy) + " % korrekt klassifiziert.");

    }

    // Methode zum Einlesen und Markieren der E-Mails
    public void train(String folderPath, boolean isSpam) {
        try {
            // Durchsuche Dateien im Ordner und lese E-Mails ein
            try (Stream<Path> filesStream = Files.list(Paths.get(folderPath))) {
                filesStream
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String emailContent = Files.readString(file, StandardCharsets.ISO_8859_1);
                            String[] words = emailContent.split("[\\s]+");

                            Set<String> uniqueWords = new HashSet<>();

                            for (String word : words) {
                                word = word.toLowerCase();
                                if (!word.isEmpty() && uniqueWords.add(word) && !word.matches(".*[=+/@].*") && word.length() >= 3 && word.length() < 15) {
                                    if (isSpam) {
                                        spamWordCounts.put(word, spamWordCounts.getOrDefault(word, 0) + 1);
                                    } else {
                                        hamWordCounts.put(word, hamWordCounts.getOrDefault(word, 0) + 1);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Methode zur Berechnung der Spamwahrscheinlichkeit
    public double calculateSpamProbability(String emailContent) {

        String[] words = emailContent.split("[\\s]+");
        double spamProbability = 1.0;
        double hamProbability = 1.0;

        for (String word : words) {
            if (!word.matches(".*[=+/@].*") && word.length() >= 3 && word.length() < 15) {
                word = word.toLowerCase();
                int hamCount = hamWordCounts.getOrDefault(word, 0);
                int spamCount = spamWordCounts.getOrDefault(word, 0);

                // Bayes'sche Formel, um die Wahrscheinlichkeiten zu aktualisieren
                hamProbability *= (hamCount + alpha) / (hamWordCounts.size());
                spamProbability *= (spamCount + alpha) / (spamWordCounts.size());
            }
        }

        // Spamwahrscheinlichkeit basierend auf dem Schwellenwert
        if (hamProbability + spamProbability == 0) {
            return 0.5;
        } else {
            return spamProbability / (spamProbability + hamProbability);
        }
    }

    // Methode zur Bewertung der Genauigkeit
    public double evaluateAccuracy(String hamTestPath, String spamTestPath) {
        AtomicInteger correctHam = testHamMails(hamTestPath);
        AtomicInteger correctSpam = testSpamMails(spamTestPath);

        int totalHam = countTotalMails(hamTestPath);
        int totalSpam = countTotalMails(spamTestPath);

        // Berechnen Sie die Genauigkeit
        return (double) (correctHam.get() + correctSpam.get()) / (totalHam + totalSpam) * 100;
    }

    // Klassifizieren und überprüfen der Ham-Test-E-Mails
    private AtomicInteger testHamMails(String hamTestPath) {
        AtomicInteger correctHam = new AtomicInteger();

        try {
            Stream<Path> hamFilesStream = Files.list(Paths.get(hamTestPath));
            hamFilesStream.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String emailContent = Files.readString(file, StandardCharsets.ISO_8859_1);
                    double spamProbability = calculateSpamProbability(emailContent);
                    if (spamProbability < threshold) {
                        correctHam.getAndIncrement();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return correctHam;
    }

    // Klassifizieren und überprüfen der Spam-Test-E-Mails
    private AtomicInteger testSpamMails(String spamTestPath) {
        AtomicInteger correctSpam = new AtomicInteger();

        try {
            Stream<Path> spamFilesStream = Files.list(Paths.get(spamTestPath));
            spamFilesStream.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String emailContent = Files.readString(file, StandardCharsets.ISO_8859_1);
                    double spamProbability = calculateSpamProbability(emailContent);
                    if (spamProbability >= threshold) {
                        correctSpam.getAndIncrement();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return correctSpam;
    }

    private int countTotalMails(String hamTestPath) {
        int totalMails = 0;

        try {
            Stream<Path> hamFilesStream = Files.list(Paths.get(hamTestPath));
            totalMails = (int) hamFilesStream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return totalMails;
    }

    public static Map<String, Integer> getHamWordCounts() {
        return hamWordCounts;
    }

    public static Map<String, Integer> getSpamWordCounts() {
        return spamWordCounts;
    }
}
