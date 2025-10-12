import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.image.Raster;
import javax.imageio.ImageIO;
import java.util.concurrent.*;
        import java.util.ArrayList;
import java.util.List;

public class PararellByAI {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java src/Main <input> <output>");
            return;
        }

        String fileName = args[0];
        String outputName = args[1];
        System.out.println("Input file: " + fileName);

        // Open input image
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.out.println("Cannot read file: " + e);
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Use int array to match Python's int32
        int[][] inputImage = new int[height][width];
        int[][] outputArray = new int[height][width];

        // Use getSample() instead of getRGB() to match PIL in Python
        Raster raster = image.getData();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                inputImage[y][x] = raster.getSample(x, y, 0) & 0xFF;
            }
        }

        // Parallel processing
        int numThreads = (args.length >= 3) ? Integer.parseInt(args[2]) : Runtime.getRuntime().availableProcessors();
        int chunkSize = Math.max(1, height / numThreads);

        System.out.println("Processing with " + numThreads + " threads...");
        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int startY = 0; startY < height; startY += chunkSize) {
            final int start = startY;
            final int end = Math.min(startY + chunkSize, height);

            Future<?> future = executor.submit(() -> {
                processRows(inputImage, outputArray, start, end, width, height);
            });
            futures.add(future);
        }

        // Wait for all threads to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Processing error: " + e);
                executor.shutdown();

                long endTime = System.nanoTime();
                double elapsedTime = (endTime - startTime) / 1_000_000_000.0;
                System.out.printf("Processing completed in %.3f seconds%n", elapsedTime);
                return;
            }
        }
        executor.shutdown();

        // Save processed image
        BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int val = Math.max(0, Math.min(255, outputArray[y][x]));
                int rgb = (val == 0) ? 0x000000 : 0xFFFFFF;
                outImg.setRGB(x, y, rgb);
            }
        }

        // Write image out
        try {
            ImageIO.write(outImg, "png", new File(outputName));
        } catch (IOException e) {
            System.out.println("Output error: " + e);
            return;
        }
        long totalEndTime = System.nanoTime();
        double totalElapsedTime = (totalEndTime - startTime) / 1_000_000_000.0;
        System.out.printf("Total time (including I/O): %.3f seconds%n", totalElapsedTime);
        System.out.println("Done! Output saved to: " + outputName);
    }

    private static void processRows(int[][] inputImage, int[][] outputArray,
                                    int startY, int endY, int width, int height) {
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < width; x++) {
                int oldPixel = inputImage[y][x];
                int newPixel = (oldPixel > 128) ? 255 : 0;
                outputArray[y][x] = newPixel;
                int error = oldPixel - newPixel;

                // Use Math.floorDiv() to match // in Python
                if (x + 1 < width) {
                    synchronized (inputImage[y]) {
                        inputImage[y][x + 1] += Math.floorDiv(error * 7, 16);
                    }
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    synchronized (inputImage[y + 1]) {
                        inputImage[y + 1][x - 1] += Math.floorDiv(error * 3, 16);
                    }
                }
                if (y + 1 < height) {
                    synchronized (inputImage[y + 1]) {
                        inputImage[y + 1][x] += Math.floorDiv(error * 5, 16);
                    }
                }
                if (x + 1 < width && y + 1 < height) {
                    synchronized (inputImage[y + 1]) {
                        inputImage[y + 1][x + 1] += Math.floorDiv(error, 16);
                    }
                }
            }
        }
    }
}
