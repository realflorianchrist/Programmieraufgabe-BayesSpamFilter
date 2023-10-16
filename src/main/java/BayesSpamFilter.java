import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class BayesSpamFilter {
    private static final Map<String, Integer> hamWordCounts = new HashMap<>();
    private static final Map<String, Integer> spamWordCounts = new HashMap<>();
    private static double alpha = 0.001; // Kleiner Wert für unbekannte Wörter
    private static double threshold = 0.5; // Schwellenwert für die Klassifikation

    public static void main(String[] args) {
        BayesSpamFilter spamFilter = new BayesSpamFilter();

        // Trainieren des Filters mit Ham- und Spam-Mails
        spamFilter.train("src/main/resources/data/ham-anlern", false);
        spamFilter.train("src/main/resources/data/spam-anlern", true);

        // Testen des Filters mit neuen E-Mails
        String testEmail = "This is a test email containing words like money and Viagra.";
        double spamProbability = spamFilter.calculateSpamProbability(testEmail);

        if (spamProbability >= spamFilter.threshold) {
            System.out.println("Diese E-Mail wird als Spam klassifiziert.");
        } else {
            System.out.println("Diese E-Mail wird als Ham klassifiziert.");
        }

        System.out.println("Bestimmter Alpha: " + alpha);
        System.out.println("Bestimmter Schwellenwert: " + threshold);
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
                            String[] words = emailContent.split(" ");

                            Set<String> uniqueWords = new HashSet<>();

                            for (String word : words) {
                                // Entferne Sonderzeichen und konvertiere zu Kleinbuchstaben
                                word = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
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

        String[] words = emailContent.split(" ");
        double spamProbability = 1.0;
        double hamProbability = 1.0;

        for (String word : words) {
            int hamCount = hamWordCounts.getOrDefault(word, 0);
            int spamCount = spamWordCounts.getOrDefault(word, 0);

            // Bayes'sche Formel, um die Wahrscheinlichkeiten zu aktualisieren
            hamProbability *= (hamCount + alpha) / (hamWordCounts.size() + alpha);
            spamProbability *= (spamCount + alpha) / (spamWordCounts.size() + alpha);
        }

        // Spamwahrscheinlichkeit basierend auf dem Schwellenwert
        return spamProbability / (spamProbability + hamProbability);
    }

}
