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
        if (args.length < 3) {
            System.out.println("Usage: java src/Main <input> <output> <numThreads>");
            return;
        }

        String fileName = args[0];
        String outputName = args[1];
        int numThreads = Integer.parseInt(args[2]);
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

        // Locks
        Object[] locks = new Object[width];
		for (int i=0; i<width; i++) {
				locks[i] = new Object();
		}

        System.out.println("Processing with " + numThreads + " threads...");
        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int startY = 0; startY < height; startY += 1) {
            final int start = startY;
            final int end = width;

            Future<?> future = executor.submit(() -> {
                processRows(inputImage, outputArray, start, end, width, height, locks);
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
}
