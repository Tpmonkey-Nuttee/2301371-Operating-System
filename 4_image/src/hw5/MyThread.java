package src.hw5;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThread implements Runnable {
    private final int y;
    private final int width;
    private final int height;
    private final int[][] inputImage;
    private final int[][] outputArray;
    private final Object[][] pixelLocks;
    private final AtomicInteger[] rowProgress;

    MyThread(int[][] inputImage, int[][] outputImage, int y, int width, int height,
                 Object[][] pixelLocks, AtomicInteger[] rowProgress) {
        this.y = y;
        this.width = width;
        this.height = height;
        this.inputImage = inputImage;
        this.outputArray = outputImage;
        this.pixelLocks = pixelLocks;
        this.rowProgress = rowProgress;
    }

    @Override
    public void run() {
        for (int x = 0; x < width; x++) {

            // 1. WAIT FOR DEPENDENCIES
            // This is the core of the pipeline.
            // We must wait for the thread processing the row ABOVE (y-1)
            // to finish processing the pixel at (x+1, y-1).
            // This ensures all error has been propagated to pixel (x, y)
            // before we read it.
            if (y > 0) {
                // Handle the right-edge boundary case
                int waitPixelX = Math.min(x + 1, width - 1);

                // Spin-wait until the row above is far enough ahead.
                while (rowProgress[y - 1].get() < waitPixelX) {
                    Thread.yield();
                }
            }

            // 2. PROCESS THE PIXEL
            // We can now safely read our pixel's value, as all dependencies are met.
            int oldPixel;
            synchronized (pixelLocks[y][x]) {
                oldPixel = inputImage[y][x];
            }

            int newPixel = (oldPixel > 128) ? 255 : 0;
            outputArray[y][x] = newPixel;
            int error = oldPixel - newPixel;

            // 3. DIFFUSE THE ERROR (ATOMICALLY)
            // We must lock each neighbor pixel individually before writing
            // to prevent race conditions from other threads.

            // Right pixel
            if (x + 1 < width) {
                synchronized (pixelLocks[y][x + 1]) {
                    inputImage[y][x + 1] += Math.floorDiv(error * 7, 16);
                }
            }
            // Bottom-left pixel
            if (x - 1 >= 0 && y + 1 < height) {
                synchronized (pixelLocks[y + 1][x - 1]) {
                    inputImage[y + 1][x - 1] += Math.floorDiv(error * 3, 16);
                }
            }
            // Bottom pixel
            if (y + 1 < height) {
                synchronized (pixelLocks[y + 1][x]) {
                    inputImage[y + 1][x] += Math.floorDiv(error * 5, 16);
                }
            }
            // Bottom-right pixel
            if (x + 1 < width && y + 1 < height) {
                synchronized (pixelLocks[y + 1][x + 1]) {
                    inputImage[y + 1][x + 1] += Math.floorDiv(error * 1, 16);
                }
            }

            // 4. ANNOUNCE PROGRESS
            // Announce that we have finished processing pixel 'x' for our row 'y'.
            // This will unblock the thread for row 'y+1' that might be waiting.
            rowProgress[y].set(x);
        }
    }
}