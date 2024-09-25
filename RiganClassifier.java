import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.util.*;

public class RiganClassifier {

    // Helper function to check if a pixel is a skin pixel
    public static boolean isSkinPixel(int r, int g, int b) {
        float[] hsv = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);

        float h = hsv[0] * 360;
        float s = hsv[1];
        float v = hsv[2];

        return (h >= 0 && h <= 50) && (s >= 0.23 && s <= 0.68) && (v >= 0.35);
    }

    // Calculate percentage
    public static double calculatePercentage(double part, double whole) {
        return (part / whole) * 100;
    }

    // Get bounding polygon for a list of skin pixels
    public static int[][] getBoundingPolygon(List<int[]> skinPixels) {
        if (skinPixels.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (int[] pixel : skinPixels) {
            minX = Math.min(minX, pixel[0]);
            minY = Math.min(minY, pixel[1]);
            maxX = Math.max(maxX, pixel[0]);
            maxY = Math.max(maxY, pixel[1]);
        }

        return new int[][]{{minX, minY}, {maxX, maxY}};
    }

    // Bounding polygon area
    public static double boundingPolygonArea(int[][] boundingPolygon) {
        if (boundingPolygon == null) {
            return 0;
        }
        int xMin = boundingPolygon[0][0];
        int yMin = boundingPolygon[0][1];
        int xMax = boundingPolygon[1][0];
        int yMax = boundingPolygon[1][1];
        return (xMax - xMin) * (yMax - yMin);
    }

    // Label connected regions (connected component labeling)
    public static int[][] labelConnectedComponents(boolean[][] skinMask, int height, int width) {
        int[][] labels = new int[height][width];
        int label = 1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (skinMask[y][x] && labels[y][x] == 0) {
                    // Perform BFS to label the region
                    Queue<int[]> queue = new LinkedList<>();
                    queue.add(new int[]{x, y});
                    labels[y][x] = label;

                    while (!queue.isEmpty()) {
                        int[] pos = queue.poll();
                        int px = pos[0], py = pos[1];

                        for (int i = -1; i <= 1; i++) {
                            for (int j = -1; j <= 1; j++) {
                                int nx = px + i, ny = py + j;
                                if (nx >= 0 && nx < width && ny >= 0 && ny < height &&
                                        skinMask[ny][nx] && labels[ny][nx] == 0) {
                                    labels[ny][nx] = label;
                                    queue.add(new int[]{nx, ny});
                                }
                            }
                        }
                    }

                    label++;
                }
            }
        }

        return labels;
    }

    // Count the size of each region
    public static List<int[]> countRegionSizes(int[][] labeledSkin, int numRegions) {
        int[] regionSizes = new int[numRegions + 1];
        List<int[]> sortedRegions = new ArrayList<>();

        for (int[] row : labeledSkin) {
            for (int label : row) {
                if (label > 0) {
                    regionSizes[label]++;
                }
            }
        }

        for (int i = 1; i <= numRegions; i++) {
            sortedRegions.add(new int[]{i, regionSizes[i]});
        }

        // Sort by size descending
        sortedRegions.sort((a, b) -> Integer.compare(b[1], a[1]));
        return sortedRegions;
    }

    // Main image classification function
    public static String classifyImage(String imagePath) {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            int width = image.getWidth();
            int height = image.getHeight();

            boolean[][] skinMask = new boolean[height][width];
            List<int[]> skinPixels = new ArrayList<>();

            // Detect skin pixels
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color color = new Color(image.getRGB(x, y));
                    int r = color.getRed();
                    int g = color.getGreen();
                    int b = color.getBlue();

                    if (isSkinPixel(r, g, b)) {
                        skinMask[y][x] = true;
                        skinPixels.add(new int[]{x, y});
                    }
                }
            }

            // Get total skin pixel count
            int totalSkinPixels = skinPixels.size();

            // Label connected skin regions
            int[][] labeledSkin = labelConnectedComponents(skinMask, height, width);
            List<int[]> sortedRegions = countRegionSizes(labeledSkin, Arrays.stream(labeledSkin).flatMapToInt(Arrays::stream).max().orElse(0));

            // Analyze largest skin regions
            int largestSkinRegionPixels = sortedRegions.get(0)[1];
            int secondLargestSkinRegionPixels = sortedRegions.size() > 1 ? sortedRegions.get(1)[1] : 0;
            int thirdLargestSkinRegionPixels = sortedRegions.size() > 2 ? sortedRegions.get(2)[1] : 0;

            // Bounding polygon and skin region analysis
            List<int[]> largestRegionPixels = new ArrayList<>();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (labeledSkin[y][x] == sortedRegions.get(0)[0]) {
                        largestRegionPixels.add(new int[]{x, y});
                    }
                }
            }

            int[][] boundingPolygon = getBoundingPolygon(largestRegionPixels);
            double polygonArea = boundingPolygonArea(boundingPolygon);
            double polygonSkinPixels = largestRegionPixels.size();
            double polygonSkinPercentage = calculatePercentage(polygonSkinPixels, polygonArea);

            // Calculate skin percentage
            double skinPercentage = calculatePercentage(totalSkinPixels, width * height);
            double largestSkinRegionPercentage = calculatePercentage(largestSkinRegionPixels, width * height);
            double averageIntensity = largestRegionPixels.stream().mapToDouble(p -> {
                Color c = new Color(image.getRGB(p[0], p[1]));
                return (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
            }).average().orElse(0.0) / 255.0;

            // Print debug information
            System.out.println("Skin Percentage: " + skinPercentage);
            System.out.println("Largest Skin Region Percentage: " + largestSkinRegionPercentage);
            System.out.println("Polygon Skin Percentage: " + polygonSkinPercentage);
            System.out.println("Average Intensity: " + averageIntensity);

            // Classification logic
            if (skinPercentage < 15) {
                return "Not Nude";
            }
            if (largestSkinRegionPixels < 0.35 * totalSkinPixels &&
                    secondLargestSkinRegionPixels < 0.3 * totalSkinPixels &&
                    thirdLargestSkinRegionPixels < 0.3 * totalSkinPixels) {
                return "Not Nude";
            }
            if (largestSkinRegionPixels < 0.45 * totalSkinPixels) {
                return "Not Nude";
            }
            if (totalSkinPixels < 0.3 * height * width && polygonSkinPixels < 0.55 * polygonArea) {
                return "Not Nude";
            }
            if (sortedRegions.size() > 60 && averageIntensity < 0.25) {
                return "Not Nude";
            }

            return "Nude";

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Error";
    }

    public static void main(String[] args) {
        String imagePath = "test.jpg";
        String result = classifyImage(imagePath);
        System.out.println("The image is classified as: " + result);
    }
}