import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class BayesSpamFilter {
    private static Map<String, Integer> hamWordCounts = new HashMap<>();
    private static Map<String, Integer> spamWordCounts = new HashMap<>();
    private static double alpha = 0.001; // Kleiner Wert für unbekannte Wörter
    private static double threshold = 0.5; // Schwellenwert für die Klassifikation

    public static void main(String[] args) {
        BayesSpamFilter spamFilter = new BayesSpamFilter();

        // Trainieren des Filters mit Ham- und Spam-Mails
        spamFilter.train("src/main/resources/data/ham-anlern", false);
        spamFilter.train("src/main/resources/data/spam-anlern", true);

        System.out.println("Bestimmter Alpha: " + alpha);
        System.out.println("Bestimmter Schwellenwert: " + threshold);

        double accuracy = spamFilter.evaluateAccuracy("src/main/resources/data/ham-test",
            "src/main/resources/data/spam-test");

        System.out.println("Es wurden " + accuracy + " % korrekt klassifiziert.");
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
                                // Entferne Sonderzeichen und konvertiere zu Kleinbuchstaben
                                word = word.replaceAll("[^a-zA-Z]", "").toLowerCase()
                                    .replaceAll("[^\\p{Alnum}]", "");
                                if (!word.isEmpty() && uniqueWords.add(word)) {
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
            if (!word.isEmpty()) {
                word = word.replaceAll("[^a-zA-Z]", "").toLowerCase()
                    .replaceAll("[^\\p{Alnum}]", "");
                int hamCount = hamWordCounts.getOrDefault(word, 0);
                int spamCount = spamWordCounts.getOrDefault(word, 0);

                // Bayes'sche Formel, um die Wahrscheinlichkeiten zu aktualisieren
                hamProbability *= (hamCount + alpha) / (hamWordCounts.size() + alpha);
                spamProbability *= (spamCount + alpha) / (spamWordCounts.size() + alpha);
            }
        }

        // Spamwahrscheinlichkeit basierend auf dem Schwellenwert
        if (hamProbability + spamProbability == 0) {
            return 0.5; // Wenn die Summe von beiden null ist, geben Sie 0.5 zurück (neutral).
        } else {
            return spamProbability / (spamProbability + hamProbability);
        }
    }


    // Methode zur Bewertung der Genauigkeit
    public double evaluateAccuracy(String hamTestPath, String spamTestPath) {

        AtomicInteger correctHam = new AtomicInteger();
        AtomicInteger correctSpam = new AtomicInteger();
        int totalHam = 0;
        int totalSpam = 0;

        // Klassifizieren und überprüfen der Ham-Test-E-Mails
        try {
            Stream<Path> hamFilesStream = Files.list(Paths.get(hamTestPath));
            totalHam = (int) hamFilesStream.filter(Files::isRegularFile).count();

            hamFilesStream = Files.list(Paths.get(hamTestPath));
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

        // Klassifizieren und überprüfen der Spam-Test-E-Mails
        try {
            Stream<Path> spamFilesStream = Files.list(Paths.get(spamTestPath));
            totalSpam = (int) spamFilesStream.filter(Files::isRegularFile).count();

            spamFilesStream = Files.list(Paths.get(spamTestPath)); // Öffnen Sie den Stream erneut
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

        // Berechnen Sie die Genauigkeit
        return (double) (correctHam.get() + correctSpam.get()) / (totalHam + totalSpam);
    }


    public static Map<String, Integer> getHamWordCounts() {
        return hamWordCounts;
    }

    public static Map<String, Integer> getSpamWordCounts() {
        return spamWordCounts;
    }
}
