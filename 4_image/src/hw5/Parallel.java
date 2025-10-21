package src.hw5;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.image.Raster;
import javax.imageio.ImageIO;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Parallel {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Parallel <input> <numThreads>");
            return;
        }

        String fileName = args[0];
        int numThreads = Integer.parseInt(args[1]);
        System.out.println("Input file: " + fileName);
        System.out.println("Using " + numThreads + " threads.");

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
                inputImage[y][x] = raster.getSample(x, y, 0);
            }
        }

        System.out.println("Image dimensions: " + width + "x" + height);

        // --- New Synchronization Primitives ---

        // 1. A 2D array of locks, one for each pixel, for atomic writes.
        Object[][] pixelLocks = new Object[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelLocks[y][x] = new Object();
            }
        }

        // 2. An array to track the progress of each row-processing task.
        // -1 means the row hasn't started.
        // A value of 'x' means processing for pixel (x, y) is complete.
        AtomicInteger[] rowProgress = new AtomicInteger[height];
        for (int y = 0; y < height; y++) {
            rowProgress[y] = new AtomicInteger(-1);
        }

        // -------------------------------------


        // Create a thread pool and submit one task for each row
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        long startTime = System.nanoTime();

        for (int y = 0; y < height; y++) {
            pool.submit(new MyThread(
                    inputImage, outputArray, y, width, height, pixelLocks, rowProgress
            ));
        }

        // Wait for all tasks to complete
        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long totalEndTime = System.nanoTime();
        double totalElapsedTime = (totalEndTime - startTime) / 1_000_000.0;
        System.out.printf("Dithering time: %.3f ms%n", totalElapsedTime);

        save(outputArray, width, height, "output_" + numThreads + "_thread");
    }

    public static void save(int[][] outputArray, int width, int height, String fileName) {
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
            ImageIO.write(outImg, "png", new File("output/"+fileName+".png"));
        } catch (IOException e) {
            System.out.println("Output error: " + e);
            return;
        }
        System.out.println("Saved to: output/" + fileName+".png");
    }
}