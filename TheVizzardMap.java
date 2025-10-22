import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * 🗺️ TheVizzardMap
 * Displays election results as colored county plots on a U.S. map background.
 * Green = Candidate A (Bugs Bunny)
 * Magenta = Candidate B (Road Runner)
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
    private String focusState = null;

    public TheVizzardMap(String csvPath, String mapPath, String focusState) throws IOException {
        this.focusState = (focusState != null && !focusState.isEmpty()) ? focusState.toUpperCase() : null;
        mapImage = new ImageIcon(mapPath).getImage();
        loadCounties(csvPath);
    }

    /** Load county data from CSV */
    private void loadCounties(String csvPath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String headerLine = br.readLine();
            if (headerLine == null) return;
            String[] headers = headerLine.split(",");

            int latIdx = Arrays.asList(headers).indexOf("lat");
            int lonIdx = Arrays.asList(headers).indexOf("lng");
            int bbIdx = Arrays.asList(headers).indexOf("BB Votes");
            int rrIdx = Arrays.asList(headers).indexOf("RR Votes");
            int stateIdx = Arrays.asList(headers).indexOf("state_id");

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                try {
                    if (parts.length > Math.max(latIdx, lonIdx)) {
                        double lat = Double.parseDouble(parts[latIdx]);
                        double lon = Double.parseDouble(parts[lonIdx]);
                        int bb = Integer.parseInt(parts[bbIdx]);
                        int rr = Integer.parseInt(parts[rrIdx]);
                        String state = parts[stateIdx].trim();
                        String winner = (bb > rr) ? "A" : "B";
                        counties.add(new County(state, lat, lon, winner));
                    }
                } catch (Exception ignored) {}
            }
        }
        System.out.println("Loaded " + counties.size() + " counties for visualization.");
    }

    /** Convert lat/lon to (x, y) for map plotting */
    private Point project(double lat, double lon, int width, int height) {
        // Adjusted scaling for USA map alignment
        double x = (lon + 125) * (width / 58.0);
        double y = (50 - lat) * (height / 30.0);
        return new Point((int)x, (int)y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(mapImage, 0, 0, getWidth(), getHeight(), this);

        for (County c : counties) {
            if (focusState != null && !focusState.equals(c.stateId)) continue; // filter by state if needed
            Point p = project(c.lat, c.lon, getWidth(), getHeight());
            g.setColor(c.winner.equals("A") ? Color.GREEN : Color.MAGENTA);
            g.fillOval(p.x, p.y, 5, 5);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        String label = (focusState == null)
                ? "National County Results — Bugs Bunny (Green) vs Road Runner (Magenta)"
                : focusState + " County Results — Bugs Bunny (Green) vs Road Runner (Magenta)";
        g.drawString(label, 20, 25);
    }

    /** Display full USA map */
    public static void displayNationalMap() {
        displayMap(null);
    }

    /** Display one state only */
    public static void displayStateMap(String stateCode) {
        displayMap(stateCode);
    }

    /** Core display logic */
    private static void displayMap(String stateCode) {
        try {
            JFrame frame = new JFrame("The Vizzard — " +
                    (stateCode == null ? "National Map" : stateCode + " Map"));
            TheVizzardMap panel = new TheVizzardMap("data/Voting-Counties.csv",
                    "data/us_map.png", stateCode);
            frame.add(panel);
            frame.setSize(900, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        } catch (IOException e) {
            System.out.println("❌ Failed to load map: " + e.getMessage());
        }
    }
}
