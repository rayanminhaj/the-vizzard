import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * 🗺️ TheVizzardMap
 * Displays election results as colored county plots on a U.S. map background.
 * Green = Candidate A
 * Magenta = Candidate B
 */
public class TheVizzardMap extends JPanel {

    private static class County {
        double lat, lon;
        String stateId, winner;
        County(String stateId, double lat, double lon, String winner) {
            this.stateId = stateId;
            this.lat = lat;
            this.lon = lon;
            this.winner = winner;
        }
    }

    private final java.util.List<County> counties = new ArrayList<>();
    private final Image mapImage;
    private final String focusState;

    public TheVizzardMap(String csvPath, String mapPath, String focusState) throws IOException {
        this.focusState = (focusState != null && !focusState.isEmpty()) ? focusState.toUpperCase() : null;
        mapImage = new ImageIcon(mapPath).getImage();
        loadCounties(csvPath);
    }

    /** Load county data from CSV. Handles flexible header names. */
    private void loadCounties(String csvPath) throws IOException {
        File file = new File(csvPath);
        if (!file.exists()) {
            System.out.println("❌ Counties CSV not found: " + csvPath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.trim().split(",");
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                idx.put(headers[i].trim().toLowerCase(), i);
            }

            // Support multiple possible header styles
            int latIdx   = idx.getOrDefault("lat", idx.getOrDefault("latitude", -1));
            int lonIdx   = idx.getOrDefault("lng", idx.getOrDefault("longitude", -1));
            int stateIdx = idx.getOrDefault("state_id", idx.getOrDefault("state", -1));
            int aIdx     = idx.getOrDefault("a_votes", idx.getOrDefault("bb votes", -1));
            int bIdx     = idx.getOrDefault("b_votes", idx.getOrDefault("rr votes", -1));

            if (latIdx < 0 || lonIdx < 0 || aIdx < 0 || bIdx < 0 || stateIdx < 0) {
                System.out.println("⚠️  Header mismatch: expected lat,lng,state_id,A_Votes,B_Votes");
                System.out.println("Found headers: " + Arrays.toString(headers));
                return;
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                try {
                    if (parts.length <= Math.max(stateIdx,
                            Math.max(aIdx, Math.max(bIdx, Math.max(latIdx, lonIdx))))) continue;

                    double lat = Double.parseDouble(parts[latIdx]);
                    double lon = Double.parseDouble(parts[lonIdx]);
                    int aVotes = Integer.parseInt(parts[aIdx]);
                    int bVotes = Integer.parseInt(parts[bIdx]);
                    String state = parts[stateIdx].trim().toUpperCase();
                    String winner = (aVotes > bVotes) ? "A" : "B";
                    counties.add(new County(state, lat, lon, winner));
                } catch (Exception ignored) {}
            }
        }
        System.out.println("Loaded " + counties.size() + " counties for visualization.");
    }

    /** Convert lat/lon to (x, y) for map plotting */
    private Point project(double lat, double lon, int width, int height) {
        // Simple equirectangular-style projection tuned for US map alignment
        double x = (lon + 125) * (width / 58.0);
        double y = (50 - lat) * (height / 30.0);
        return new Point((int) x, (int) y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);

        for (County c : counties) {
            if (focusState != null && !focusState.equals(c.stateId)) continue;
            Point p = project(c.lat, c.lon, getWidth(), getHeight());
            g.setColor("A".equals(c.winner) ? Color.GREEN : Color.MAGENTA);
            g.fillOval(p.x, p.y, 5, 5);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        String label = (focusState == null)
                ? "National County Results — " + TheVizzard.getCandidateA() + " (Green) vs " + TheVizzard.getCandidateB() + " (Magenta)"
                : focusState + " County Results — " + TheVizzard.getCandidateA() + " (Green) vs " + TheVizzard.getCandidateB() + " (Magenta)";
        g.drawString(label, 20, 25);
        g.drawString(TheVizzard.getCandidateA() + " Counties (Green)", 20, 45);
        g.drawString(TheVizzard.getCandidateB() + " Counties (Magenta)", 20, 65);
    }

    /** Display full USA map */
    public static void displayNationalMap() {
        displayMap(null);
    }

    /** Display one state only */
    public static void displayStateMap(String stateCode) {
        displayMap(stateCode);
    }

    /** Core display logic (thread-safe Swing) */
    private static void displayMap(String stateCode) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame frame = new JFrame("The Vizzard — " +
                        (stateCode == null ? "National Map" : stateCode.toUpperCase() + " Map"));
                TheVizzardMap panel = new TheVizzardMap(
                        "data/Voting-Counties.csv",
                        "data/us_map.png",
                        stateCode
                );
                frame.add(panel);
                frame.setSize(900, 600);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            } catch (IOException e) {
                System.out.println("❌ Failed to load map: " + e.getMessage());
            }
        });
    }
}