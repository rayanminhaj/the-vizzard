import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * 🎩 The Vizzard — Election Analyzer
 *
 * Reads election data from Excel and CSV files,
 * calculates results, displays summaries,
 * simulates partial reporting, and visualizes data.
 */
public class TheVizzard {

    // ----------------------------- DATA -----------------------------
    private static Map<String, Integer> electoralVotes = new HashMap<>();
    private static Map<String, Map<String, Integer>> voteResults = new HashMap<>();

    // ----------------------------- MAIN -----------------------------
    public static void main(String[] args) {
        System.out.println("========== 🗳️  The Vizzard — Election Analyzer ==========\n");

        try {
            // Load all files
            loadElectoralVotesCSV("data/State-Info.csv");
            loadVoteResultsXLSX("data/Vote-Results.xlsx");

            // Calculate stats
            Map<String, Map<String, Double>> percentages = calculateStatePercentages();
            int[] electoralTotals = calculateElectoralVotes();
            int[] popularTotals = calculatePopularVotes();

            // Print results
            printStatePercentages(percentages);
            printPopularVoteResults(popularTotals);
            printElectoralSummary(electoralTotals, popularTotals);

            // Display one state
            Scanner in = new Scanner(System.in);
            System.out.print("\nEnter State ID for summary: ");
            String state = in.nextLine().trim().toUpperCase();
            displayStateSummary(state);

            // Simulate partial reporting
            simulatePartialResults(in);

            // Draw chart
            showResultChart(popularTotals, electoralTotals);

            System.out.println("\n✅ Analysis complete. Visualization displayed.");
            // Step 1: National map
            TheVizzardMap.displayNationalMap();

            // Step 2: Ask for specific state map
            System.out.print("\nEnter a State ID to plot (e.g., CA, TX, PA): ");
            String mapState = in.nextLine().trim();
            if (!mapState.isEmpty()) {
                TheVizzardMap.displayStateMap(mapState);
            }
            in.close();
        } catch (FileNotFoundException e) {
            System.out.println("❌ Error: One or more files not found in /data/ directory.");
        } catch (Exception e) {
            System.out.println("⚠️  Unexpected error: " + e.getMessage());
        }
    }

    // ----------------------------- LOADERS -----------------------------
    /** Reads electoral votes from CSV. */
    private static void loadElectoralVotesCSV(String filename) throws IOException {
    File file = new File(filename);
    if (!file.exists()) throw new FileNotFoundException(filename);

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }

        for (int i = 2; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            if (parts.length >= 15) {
                String stateCode = parts[1].trim();
                String evStr = parts[14].trim().replaceAll("[^0-9.]", ""); // strip text like "votes"
                if (!evStr.isEmpty()) {
                    try {
                        int ev = (int) Double.parseDouble(evStr);
                        electoralVotes.put(stateCode, ev);
                    } catch (NumberFormatException e) {
                        System.out.println("Skipping invalid EV for " + stateCode + ": " + evStr);
                    }
                }
            }
        }
    }
    System.out.println("Loaded " + electoralVotes.size() + " state electoral entries.");
}

    /** Reads vote results from Excel (Apache POI). */
    private static void loadVoteResultsXLSX(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) throw new FileNotFoundException(filename);

        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = new XSSFWorkbook(fis)) 
             {
            Sheet sheet = wb.getSheet("Results");
            for (int r = 2; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell stateCell = row.getCell(1);
                Cell rrCell = row.getCell(3);
                Cell bbCell = row.getCell(4);

                if (stateCell == null || rrCell == null || bbCell == null) continue;

                String stateId = stateCell.getStringCellValue();
                int rrVotes = 0;
                int bbVotes = 0;

                try {
                    if (rrCell.getCellType() == CellType.NUMERIC)
                        rrVotes = (int) rrCell.getNumericCellValue();
                    else if (rrCell.getCellType() == CellType.STRING)
                        rrVotes = Integer.parseInt(rrCell.getStringCellValue().trim());
                    } 
                    catch (Exception e) 
                    {
                        rrVotes = 0;
                    }

                try {
                    if (bbCell.getCellType() == CellType.NUMERIC)
                        bbVotes = (int) bbCell.getNumericCellValue();
                    else if (bbCell.getCellType() == CellType.STRING)
                        bbVotes = Integer.parseInt(bbCell.getStringCellValue().trim());
                    } 
                    catch (Exception e) 
                    {
                        bbVotes = 0;
                    }
                    
                Map<String, Integer> pair = new HashMap<>();
                pair.put("A", bbVotes);
                pair.put("B", rrVotes);
                voteResults.put(stateId, pair);
            }
        }
        System.out.println("Loaded " + voteResults.size() + " state vote results.");
    }

    // ----------------------------- CALCULATIONS -----------------------------
    private static Map<String, Map<String, Double>> calculateStatePercentages() {
        Map<String, Map<String, Double>> result = new HashMap<>();
        for (String state : voteResults.keySet()) {
            Map<String, Integer> votes = voteResults.get(state);
            int total = votes.get("A") + votes.get("B");
            if (total > 0) {
                double aPct = (votes.get("A") * 100.0) / total;
                double bPct = (votes.get("B") * 100.0) / total;
                Map<String, Double> pct = new HashMap<>();
                pct.put("A", aPct);
                pct.put("B", bPct);
                result.put(state, pct);
            }
        }
        return result;
    }

    private static int[] calculatePopularVotes() {
        int aTotal = 0, bTotal = 0;
        for (Map<String, Integer> votes : voteResults.values()) {
            aTotal += votes.get("A");
            bTotal += votes.get("B");
        }
        return new int[]{aTotal, bTotal};
    }

    private static int[] calculateElectoralVotes() {
        int aEV = 0, bEV = 0;
        for (String state : voteResults.keySet()) {
            Map<String, Integer> votes = voteResults.get(state);
            int a = votes.get("A");
            int b = votes.get("B");
            if (electoralVotes.containsKey(state)) {
                int ev = electoralVotes.get(state);
                if (a > b) aEV += ev;
                else if (b > a) bEV += ev;
            }
        }
        return new int[]{aEV, bEV};
    }

    // ----------------------------- OUTPUT -----------------------------
    private static void printStatePercentages(Map<String, Map<String, Double>> percentages) {
        System.out.println("\n========== STATE PERCENTAGES ==========");
        for (String state : percentages.keySet()) {
            Map<String, Double> pct = percentages.get(state);
            System.out.printf("%s: Candidate A %.2f%% | Candidate B %.2f%%%n",
                    state, pct.get("A"), pct.get("B"));
        }
    }

    private static void printPopularVoteResults(int[] totals) {
        System.out.println("\n========== POPULAR VOTE ==========");
        System.out.println("Candidate A Total Votes: " + totals[0]);
        System.out.println("Candidate B Total Votes: " + totals[1]);
        if (totals[0] > totals[1])
            System.out.println("Popular Vote Winner: Candidate A");
        else if (totals[1] > totals[0])
            System.out.println("Popular Vote Winner: Candidate B");
        else
            System.out.println("Popular Vote Result: Tie");
    }

    private static void printElectoralSummary(int[] evTotals, int[] popularTotals) {
        System.out.println("\n========== ELECTION SUMMARY ==========");
        System.out.println("Candidate     | Popular Votes | Electoral Votes");
        System.out.println("---------------------------------------------");
        System.out.printf("Candidate A   | %13d | %14d%n", popularTotals[0], evTotals[0]);
        System.out.printf("Candidate B   | %13d | %14d%n", popularTotals[1], evTotals[1]);
        System.out.println("---------------------------------------------");

        if (evTotals[0] > evTotals[1])
            System.out.println("Winner: Candidate A");
        else if (evTotals[1] > evTotals[0])
            System.out.println("Winner: Candidate B");
        else
            System.out.println("Result: Tie");
    }

    private static void displayStateSummary(String state) {
        if (!voteResults.containsKey(state)) {
            System.out.println("State not found.");
            return;
        }
        Map<String, Integer> votes = voteResults.get(state);
        int total = votes.get("A") + votes.get("B");
        double aPct = (votes.get("A") * 100.0) / total;
        double bPct = (votes.get("B") * 100.0) / total;
        String winner = votes.get("A") > votes.get("B") ? "Candidate A"
                : votes.get("B") > votes.get("A") ? "Candidate B" : "Tie";
        int ev = electoralVotes.getOrDefault(state, 0);

        System.out.println("\nSummary for " + state);
        System.out.println("Total Votes: " + total);
        System.out.printf("Candidate A: %d (%.2f%%)%n", votes.get("A"), aPct);
        System.out.printf("Candidate B: %d (%.2f%%)%n", votes.get("B"), bPct);
        System.out.println("Winner: " + winner);
        System.out.println("Electoral Votes: " + ev);
    }

    // ----------------------------- SIMULATION -----------------------------
    private static void simulatePartialResults(Scanner in) {
        System.out.print("\nEnter report cutoff hour (e.g., 12 or 18): ");
        int hour;
        try {
            hour = Integer.parseInt(in.nextLine().trim());
        } catch (Exception e) {
            System.out.println("Invalid input. Skipping simulation.");
            return;
        }

        List<String> sortedStates = new ArrayList<>(voteResults.keySet());
        Collections.sort(sortedStates);
        int totalStates = sortedStates.size();
        int statesReported = (int) ((hour / 24.0) * totalStates);

        int partialA = 0, partialB = 0, partialAEV = 0, partialBEV = 0;
        for (int i = 0; i < statesReported; i++) {
            String state = sortedStates.get(i);
            Map<String, Integer> votes = voteResults.get(state);
            partialA += votes.get("A");
            partialB += votes.get("B");
            if (votes.get("A") > votes.get("B"))
                partialAEV += electoralVotes.getOrDefault(state, 0);
            else if (votes.get("B") > votes.get("A"))
                partialBEV += electoralVotes.getOrDefault(state, 0);
        }

        double reportedPct = (statesReported * 100.0) / totalStates;

        System.out.println("\n========== PARTIAL RESULTS ==========");
        System.out.printf("Reporting Time: %02d:00 (%d/%d states, %.1f%%)%n",
                hour, statesReported, totalStates, reportedPct);
        System.out.println("Candidate A Partial Votes: " + partialA);
        System.out.println("Candidate B Partial Votes: " + partialB);
        System.out.println("Candidate A Partial EV: " + partialAEV);
        System.out.println("Candidate B Partial EV: " + partialBEV);
    }

    // ----------------------------- VISUALIZATION -----------------------------
    private static void showResultChart(int[] popular, int[] ev) {
        JFrame frame = new JFrame("The Vizzard — Election Results");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int baseY = getHeight() - 60;
                int barWidth = 150;
                int maxVotes = Math.max(popular[0] + ev[0], popular[1] + ev[1]);

                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.drawString("Candidate A (Green)", 80, 30);
                g.drawString("Candidate B (Magenta)", 320, 30);

                // Bar for Candidate A
                int heightA = (int) ((popular[0] / (double) maxVotes) * 200);
                g.setColor(Color.GREEN);
                g.fillRect(100, baseY - heightA, barWidth, heightA);

                // Bar for Candidate B
                int heightB = (int) ((popular[1] / (double) maxVotes) * 200);
                g.setColor(Color.MAGENTA);
                g.fillRect(350, baseY - heightB, barWidth, heightB);

                g.setColor(Color.BLACK);
                g.drawString("Popular + EV Comparison", 200, baseY + 30);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }
}