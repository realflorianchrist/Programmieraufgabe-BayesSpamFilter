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
    private static final Map<String, Double> hamWordCounts = new HashMap<>();
    private static final Map<String, Double> spamWordCounts = new HashMap<>();
    private static double alpha = 0.001; // Kleiner Wert für unbekannte Wörter
    private static double threshold = 0.5; // Schwellenwert für die Klassifikation
    private static int totalHam;
    private static int totalSpam;
    private static AtomicInteger correctHam;
    private static AtomicInteger correctSpam;

    public static void main(String[] args) {
        BayesSpamFilter spamFilter = new BayesSpamFilter();

        // Trainieren des Filters mit Ham- und Spam-Mails
        spamFilter.train(hamAnlernPath, false);
        spamFilter.train(spamAnlernPath, true);

        System.out.println("Bestimmtes Alpha: " + alpha);
        System.out.println("Bestimmter Schwellenwert: " + threshold);

        double accuracy = spamFilter.evaluateAccuracy(hamTestPath, spamTestPath);

        System.out.println("Es wurden " + correctHam + " von " + totalHam + " Ham-Mails korrekt klassifiziert.");
        System.out.println("Es wurden " + correctSpam + " von " + totalSpam + " Spam-Mails korrekt klassifiziert.");

        System.out.println("Insgesamt wurden " + String.format("%.2f", accuracy) + "% korrekt klassifiziert.");

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
                                if (!word.isEmpty() && uniqueWords.add(word)
                                    && !word.matches(".*[=+/@].*")
                                    && word.length() >= 3 && word.length() < 15) {
                                    if (isSpam) {
                                        spamWordCounts.put(word, spamWordCounts.getOrDefault(word, 0.0) + 1);
                                    } else {
                                        hamWordCounts.put(word, hamWordCounts.getOrDefault(word, 0.0) + 1);
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
        //double spamProbability = 1.0;
        //double hamProbability = 1.0;

        double logSpamProbability = 0.0;
        double logHamProbability = 0.0;

        for (String word : words) {
            if (!word.matches(".*[=+/@].*") && word.length() >= 3 && word.length() < 15) {
                word = word.toLowerCase();
                double hamCount = hamWordCounts.getOrDefault(word, alpha);
                double spamCount = spamWordCounts.getOrDefault(word, alpha);

                // Bayes'sche Formel, um die Wahrscheinlichkeiten zu aktualisieren
//                hamProbability *= (hamCount) / (hamWordCounts.size());
//                spamProbability *= (spamCount) / (spamWordCounts.size());

                // Berechnung mit alternativer Formel
                logHamProbability += Math.log((hamCount) / (hamWordCounts.size()));
                logSpamProbability += Math.log((spamCount) / (spamWordCounts.size()));
            }
        }

        // Spamwahrscheinlichkeit basierend auf dem Schwellenwert
//        if (hamProbability + spamProbability == 0) {
//            return threshold;
//        } else {
//            return spamProbability / (spamProbability + hamProbability);
//        }

        // Berechnen der Spam-Wahrscheinlichkeit mit der Sigmoid-Funktion
        return 1.0 / (1.0 + Math.exp(logHamProbability - logSpamProbability));
    }

    // Methode zur Bewertung der Genauigkeit
    public double evaluateAccuracy(String hamTestPath, String spamTestPath) {
        correctHam = testHamMails(hamTestPath);
        correctSpam = testSpamMails(spamTestPath);

        totalHam = countTotalMails(hamTestPath);
        totalSpam = countTotalMails(spamTestPath);

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

    public static Map<String, Double> getHamWordCounts() {
        return hamWordCounts;
    }

    public static Map<String, Double> getSpamWordCounts() {
        return spamWordCounts;
    }
}
