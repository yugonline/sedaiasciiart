import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UKPostcodeAsciiArt {

    // Simple holder for lat/lon
    static class LatLon {
        double lat;
        double lon;
        LatLon(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    public static void main(String[] args) {

        // 1. Load data
        List<LatLon> points = new ArrayList<>();
        String csvFilePath = "ukpostcodes.csv";  // Adjust path as needed

        if (extractPointsFromCsv(csvFilePath, points)) {
            return;
        }

        // >>> Sort the points: North to South (desc lat) and West to East (asc lon) <<<
        // North to South means larger lat first, so descending lat.
        // West to East means smaller lon first, so ascending lon.
        points.sort((p1, p2) -> {
            // compare lat in descending order
            int lonComp = Double.compare(p2.lon, p1.lon);
            if (lonComp != 0) {
                return lonComp;
            }
            // if lat is the same, compare lon in ascending order
            return Double.compare(p1.lat, p2.lat);
        });

        // 2. Load the image (if you still want to reference an actual map file)
        try {
            File imgFile = new File("uk_map.png");
            BufferedImage img = ImageIO.read(imgFile);
            // (You might do something with the image here if desired)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 3. Determine bounding box
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (LatLon ll : points) {
            if (ll.lat < minLat) minLat = ll.lat;
            if (ll.lat > maxLat) maxLat = ll.lat;
            if (ll.lon < minLon) minLon = ll.lon;
            if (ll.lon > maxLon) maxLon = ll.lon;
        }

        // 4. Choose ASCII grid size (adjust as needed)
        int gridWidth = 80;   // x-axis
        int gridHeight = 100;  // y-axis

        // 5. Initialize ASCII grid with background char '.'
        char[][] asciiGrid = new char[gridHeight][gridWidth];
        for (int r = 0; r < gridHeight; r++) {
            for (int c = 0; c < gridWidth; c++) {
                asciiGrid[r][c] = '.';
            }
        }

        // 6. Map each lat/lon to the grid
        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;

        for (LatLon ll : points) {
            // row: larger lat should map to row 0 (top),
            // so we do (maxLat - ll.lat) / latRange

            int row = (int) ((maxLat - ll.lat) / latRange * (gridHeight - 1));
            int col = (int) ((ll.lon - minLon) / lonRange * (gridWidth - 1));

            // Bounds checking
            if (row >= 0 && row < gridHeight && col >= 0 && col < gridWidth) {
                asciiGrid[row][col] = 'f';
            }
        }

        // 7. Print out the ASCII map
        // Note: row 0 is top row, row gridHeight-1 is bottom
        for (int r = 0; r < gridHeight; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < gridWidth; c++) {
                sb.append(asciiGrid[r][c]);
            }
            System.out.println(sb.toString());
        }

        System.out.println("ASCII Art Map generated with " + points.size() + " points.");
    }

    private static boolean extractPointsFromCsv(String csvFilePath, List<LatLon> points) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            // Skip header (assuming the first line is a header)
            br.readLine(); // e.g. "id,postcode,latitude,longitude"

            while ((line = br.readLine()) != null) {
                // Example line: "1,AB10 1XG,57.14416516,-2.114847768"
                String[] tokens = line.split(",");
                if (tokens.length < 4) {
                    continue; // skip if not enough columns
                }

                // parse lat/lon
                try {
                    double lat = Double.parseDouble(tokens[2]);
                    double lon = Double.parseDouble(tokens[3]);
                    if(lat == 99.999999) continue;
                    points.add(new LatLon(lat, lon));
                } catch (NumberFormatException e) {
                    // skip invalid data
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }
}
