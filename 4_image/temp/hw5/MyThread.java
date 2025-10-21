package src.hw5;
import java.lang.Runnable;
import java.util.concurrent.locks.Lock;

public class MyThread extends Thread {
    int y;
	int width;
	int height;
	int[][] inputImage;
	int [][] outputArray;
	Object[] locks;
    Object notifier;
    Object synchronizer;

    private boolean running = true;
    public boolean done = false;

	MyThread(int[][] inputImage, int[][] outputImage, int width, int height, Object[] locks, Object notifier, Object synchronizer) {
        this.width = width;
        this.height = height;
        this.inputImage = inputImage;
        this.outputArray = outputImage;
		this.locks = locks;
        this.notifier = notifier;
        this.synchronizer = synchronizer;
	}


    public void setY(int y) {
        this.y = y;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
	public void run() {
        while (running) {
            synchronized (this) {
                try {
                    this.wait();
                } catch (InterruptedException e) {}
            }
            if (!running) {break;}

            done = false;
            System.out.println((y % 2) + " processing...");
            process_image(this.y);
            done = true;
            System.out.println((y % 2) + " DONE PROCeSSING");
        }
	}

    private void process_image(int row) {
        for (int x = 0; x < width; x++) {
            synchronized (locks[x+1]) {}
            synchronized (locks[x]) {
                int oldPixel = inputImage[row][x];
                int newPixel = (oldPixel > 128) ? 255 : 0;
                outputArray[row][x] = newPixel;
                int error = oldPixel - newPixel;

                // Use Math.floorDiv() to match // in Python
                if (x + 1 < width) {
                    // Right
                    inputImage[row][x + 1] += Math.floorDiv(error * 7, 16);
                }
                if (x - 1 >= 0 && row + 1 < height) {
                    // Bottom left
                    inputImage[row + 1][x - 1] += Math.floorDiv(error * 3, 16);
                }
                if (y + 1 < height) {
                    // Bottom
                    inputImage[row + 1][x] += Math.floorDiv(error * 5, 16);
                }
                if (x + 1 < width && row + 1 < height) {
                    // Bottom right
                    inputImage[row + 1][x + 1] += Math.floorDiv(error, 16);
                }

                if (x == 2) {
                    synchronized (notifier) {
                        System.out.println((y % 2) + " NOTIFY ");
                        notifier.notify();
                    }
                }
            }
        }
    }
}

