package src.hw5;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.image.Raster;
import javax.imageio.ImageIO;
import java.lang.Thread;
import java.util.concurrent.locks.*;


public class Parallel {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java src/Main <input> <numThreads>");
            return;
        }

        String fileName = args[0];
        int numThreads = Integer.parseInt(args[1]);
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
        Object[] locks = new Object[width+1];
        for (int i = 0; i <= width; i++) {
            locks[i] = new Object();
        }

        Object[] notifiers = new Object[numThreads];
        Object[] synchronizers = new Object[numThreads];

        // Create and run threads
        MyThread[] threads = new MyThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            notifiers[i] = new Object();
            synchronizers[i] = new Object();
            threads[i] = new MyThread(inputImage, outputArray, width, height, locks, notifiers[i], synchronizers[i]);
            threads[i].start();
        }

        System.out.println("Processing with " + numThreads + " threads...");
        long startTime = System.nanoTime();

        // Assign job
        for (int i = 0; i < height; i += numThreads) {
            for (int j = 0; j < numThreads && i + j < height; j++) {
                // Wait till thread finish processing row first
                System.out.println("WAIT FOR " + j);
                if (i != 0) {
                    while (!threads[j].done) {}
                    System.out.println("FINISHED WAITNG FOR " + j);
                }
                // Add more job
                System.out.println("ASSIGNING JOb TO " + j);
                threads[j].setY(i+j);

                synchronized (threads[j]) {
                    threads[j].notify();
                }
//                System.out.println("NOTIFIED " + j);

                // Wait till it's safe to launch another thread.
                try {
                    synchronized (notifiers[j]) {
                        notifiers[j].wait();
                    }
                } catch (InterruptedException e) {}
            }
        }
        System.out.println("Done assigning job to thread");
        for (int i = 0; i < numThreads; i++) {
            threads[i].setRunning(false);
            synchronized (threads[i]) {
                threads[i].notify();
            }
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        long totalEndTime = System.nanoTime();
        double totalElapsedTime = (totalEndTime - startTime) / 1_000_000_000.0;
        System.out.printf("Total time (including I/O): %.3f seconds%n", totalElapsedTime);

        save(outputArray, width, height, "output_"+numThreads+"_thread");
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
