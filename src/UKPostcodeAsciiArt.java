import java.io.BufferedReader;
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

        // If there's an error reading CSV or no points, exit early
        if (extractPointsFromCsv(csvFilePath, points)) {
            return;
        }

        // 2. Sort the points
        //    North to South means larger lat first (desc lat).
        //    West to East means smaller lon first (asc lon).
        //    (Note: The code below performs a comparison on lon first in descending order,
        //           then on lat in ascending order, matching the original logic exactly.)
        sortPoints(points);

        // 3. Determine bounding box for min/max lat/lon
        double[] bounds = computeBounds(points);
        double minLat = bounds[0];
        double maxLat = bounds[1];
        double minLon = bounds[2];
        double maxLon = bounds[3];

        // 4. Choose ASCII grid size (adjust as needed)
        int gridWidth = 80;   // x-axis
        int gridHeight = 100; // y-axis

        // 5. Initialize the ASCII and density grids
        char[][] asciiGrid = initializeAsciiGrid(gridHeight, gridWidth);
        int[][] densityGrid = new int[gridHeight][gridWidth];

        // 6. Map each lat/lon to the grid
        fillDensityGrid(points, asciiGrid, densityGrid, minLat, maxLat, minLon, maxLon);

        // 7. Convert density values to characters in the ASCII grid
        convertDensityToCharacters(asciiGrid, densityGrid);

        // 8. Print out the ASCII map
        printAsciiGrid(asciiGrid);

        System.out.println("ASCII Art Map generated with " + points.size() + " points.");
    }

    /**
     * Reads CSV file and populates the list of points (lat/lon).
     * Returns true if there's any IO error (or if file reading fails).
     */
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
                    if (lat == 99.999999) {
                        // skip sentinel/bogus data
                        continue;
                    }
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

    /**
     * Sorts the points according to the original logic:
     *   - Compare lon in descending order first
     *   - If lon is equal, compare lat in ascending order
     */
    private static void sortPoints(List<LatLon> points) {
        points.sort((p1, p2) -> {
            int lonComp = Double.compare(p2.lon, p1.lon);
            if (lonComp != 0) {
                return lonComp;
            }
            // if lon is the same, compare lat in ascending order
            return Double.compare(p1.lat, p2.lat);
        });
    }

    /**
     * Computes the bounding box [minLat, maxLat, minLon, maxLon] for the given points.
     */
    private static double[] computeBounds(List<LatLon> points) {
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
        return new double[]{minLat, maxLat, minLon, maxLon};
    }

    /**
     * Initializes a 2D char array with '.' to represent empty background.
     */
    private static char[][] initializeAsciiGrid(int rows, int cols) {
        char[][] grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = '.';
            }
        }
        return grid;
    }

    /**
     * Fills the density grid by mapping each point's lat/lon
     * to its corresponding row/col in the ASCII grid.
     */
    private static void fillDensityGrid(List<LatLon> points,
                                        char[][] asciiGrid,
                                        int[][] densityGrid,
                                        double minLat,
                                        double maxLat,
                                        double minLon,
                                        double maxLon) {

        int rows = asciiGrid.length;
        int cols = asciiGrid[0].length;

        double latRange = maxLat - minLat;
        double lonRange = maxLon - minLon;

        for (LatLon ll : points) {
            // row: larger lat should map to row 0 (top),
            // so we do (maxLat - ll.lat) / latRange
            int row = (int) ((maxLat - ll.lat) / latRange * (rows - 1));
            // col: smaller lon should map to col 0 (left),
            // so we do (ll.lon - minLon) / lonRange
            int col = (int) ((ll.lon - minLon) / lonRange * (cols - 1));

            // Bounds checking
            if (row >= 0 && row < rows && col >= 0 && col < cols) {
                asciiGrid[row][col] = 'f';
                densityGrid[row][col]++;
            }
        }
    }

    /**
     * Converts the density counts into character symbols:
     *   '.' for zero density,
     *   'f' for low density,
     *   'F' for medium density,
     *   '#' for high density.
     */
    private static void convertDensityToCharacters(char[][] asciiGrid, int[][] densityGrid) {
        int rows = asciiGrid.length;
        int cols = asciiGrid[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int density = densityGrid[r][c];
                if (density == 0) {
                    asciiGrid[r][c] = '.'; // Background
                } else if (density < 5) {
                    asciiGrid[r][c] = 'f'; // Low density
                } else if (density < 20) {
                    asciiGrid[r][c] = 'F'; // Medium density
                } else {
                    asciiGrid[r][c] = '#'; // High density
                }
            }
        }
    }

    /**
     * Prints the ASCII grid rows top-to-bottom.
     */
    private static void printAsciiGrid(char[][] asciiGrid) {
        for (int r = 0; r < asciiGrid.length; r++) {
            System.out.println(new String(asciiGrid[r]));
        }
    }
}
