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
    private static String candidateA = "Candidate A";
    private static String candidateB = "Candidate B";

    public static String getCandidateA() { return candidateA; }
    public static String getCandidateB() { return candidateB; }

    private static final Map<String, Integer> electoralVotes = new HashMap<>();
    private static final Map<String, Map<String, Integer>> voteResults = new HashMap<>();

    // ----------------------------- MAIN -----------------------------
    public static void main(String[] args) {
        System.out.println("==========  The Vizzard — Election Analyzer ==========\n");

        Scanner in = new Scanner(System.in);
        System.out.print("Enter name for Candidate A: ");
        candidateA = in.nextLine().trim();
        System.out.print("Enter name for Candidate B: ");
        candidateB = in.nextLine().trim();

        System.out.println("\nWelcome to The Vizzard Election Analyzer!");
        System.out.println("Comparing " + candidateA + " vs " + candidateB + "...\n");

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

            // One-state summary
            System.out.print("\nEnter State ID for summary: ");
            String state = in.nextLine().trim().toUpperCase();
            displayStateSummary(state);

            // Simulate partial reporting
            simulatePartialResults(in);

            // Simple bar chart
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
            // Print full stack once; comment back to message-only if you prefer
            e.printStackTrace();
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

            // Skip first two header-ish lines per your original file
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

    /** Reads vote results from Excel (Apache POI). Expect columns:
     *  [*, stateId at col 1, *, A_Votes at col 3, B_Votes at col 4, ...]
     */
    private static void loadVoteResultsXLSX(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) throw new FileNotFoundException(filename);

        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheet("Results");
            if (sheet == null) {
                // Fallback: first sheet if "Results" isn't found
                sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            }
            if (sheet == null) throw new IllegalStateException("No sheet found in " + filename);

            int lastRow = sheet.getLastRowNum();
            for (int r = 2; r <= lastRow; r++) {           // start at row 2 (0-based) per your file
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell stateCell = row.getCell(1);            // state ID
                Cell aCell     = row.getCell(3);            // A_Votes
                Cell bCell     = row.getCell(4);            // B_Votes
                if (stateCell == null || aCell == null || bCell == null) continue;

                String stateId = null;
                try {
                    stateId = stateCell.getStringCellValue();
                } catch (Exception ignore) {
                    // If it's numeric for some reason, try converting
                    if (stateCell.getCellType() == CellType.NUMERIC) {
                        stateId = String.valueOf((int) stateCell.getNumericCellValue());
                    }
                }
                if (stateId == null) continue;
                stateId = stateId.trim();

                int aVotes = safeInt(aCell);
                int bVotes = safeInt(bCell);

                Map<String, Integer> pair = new HashMap<>();
                pair.put("A", aVotes);
                pair.put("B", bVotes);
                voteResults.put(stateId, pair);
            }
        }
        System.out.println("Loaded " + voteResults.size() + " state vote results.");
    }

    private static int safeInt(Cell c) {
        try {
            if (c == null) return 0;
            if (c.getCellType() == CellType.NUMERIC) return (int) c.getNumericCellValue();
            if (c.getCellType() == CellType.STRING)  return Integer.parseInt(c.getStringCellValue().trim());
        } catch (Exception ignore) {}
        return 0;
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
            System.out.printf("%s: %s %.2f%% | %s %.2f%%%n",
                    state, candidateA, pct.get("A"), candidateB, pct.get("B"));
        }
    }

    private static void printPopularVoteResults(int[] totals) {
        System.out.println("\n========== POPULAR VOTE ==========");
        System.out.println(candidateA + " Total Votes: " + totals[0]);
        System.out.println(candidateB + " Total Votes: " + totals[1]);
        if (totals[0] > totals[1])
            System.out.println("Popular Vote Winner: " + candidateA);
        else if (totals[1] > totals[0])
            System.out.println("Popular Vote Winner: " + candidateB);
        else
            System.out.println("Popular Vote Result: Tie");
    }

    private static void printElectoralSummary(int[] evTotals, int[] popularTotals) {
        System.out.println("\n========== ELECTION SUMMARY ==========");
        System.out.println("Candidate     | Popular Votes | Electoral Votes");
        System.out.println("---------------------------------------------");
        System.out.printf("%-14s | %13d | %14d%n", candidateA, popularTotals[0], evTotals[0]);
        System.out.printf("%-14s | %13d | %14d%n", candidateB, popularTotals[1], evTotals[1]);
        System.out.println("---------------------------------------------");

        if (evTotals[0] > evTotals[1])
            System.out.println("Winner:" + candidateA);
        else if (evTotals[1] > evTotals[0])
            System.out.println("Winner:" + candidateB);
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
        String winner = votes.get("A") > votes.get("B") ? candidateA :
                        votes.get("B") > votes.get("A") ? candidateB : "Tie";
        int ev = electoralVotes.getOrDefault(state, 0);

        System.out.println("\nSummary for " + state);
        System.out.println("Total Votes: " + total);
        System.out.printf("%s: %d (%.2f%%)%n", candidateA, votes.get("A"), aPct);
        System.out.printf("%s: %d (%.2f%%)%n", candidateB, votes.get("B"), bPct);
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
        int statesReported = (int) Math.floor((hour / 24.0) * totalStates);

        int partialA = 0, partialB = 0, partialAEV = 0, partialBEV = 0;
        for (int i = 0; i < statesReported; i++) {
            String st = sortedStates.get(i);
            Map<String, Integer> votes = voteResults.get(st);
            partialA += votes.get("A");
            partialB += votes.get("B");
            if (votes.get("A") > votes.get("B"))
                partialAEV += electoralVotes.getOrDefault(st, 0);
            else if (votes.get("B") > votes.get("A"))
                partialBEV += electoralVotes.getOrDefault(st, 0);
        }

        double reportedPct = totalStates == 0 ? 0.0 : (statesReported * 100.0) / totalStates;

        System.out.println("\n========== PARTIAL RESULTS ==========");
        System.out.printf("Reporting Time: %02d:00 (%d/%d states, %.1f%%)%n",
                hour, statesReported, totalStates, reportedPct);
        System.out.println(candidateA + " Partial Votes: " + partialA);
        System.out.println(candidateB + " Partial Votes: " + partialB);
        System.out.println(candidateA + " Partial EV: " + partialAEV);
        System.out.println(candidateB + " Partial EV: " + partialBEV);
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
                if (maxVotes <= 0) return;

                g.setFont(new Font("SansSerif", Font.BOLD, 14));
                g.drawString(candidateA + " (Green)", 80, 30);
                g.drawString(candidateB + " (Magenta)", 320, 30);

                int heightA = (int) ((popular[0] / (double) maxVotes) * 200);
                g.setColor(Color.GREEN);
                g.fillRect(100, baseY - heightA, barWidth, heightA);

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
