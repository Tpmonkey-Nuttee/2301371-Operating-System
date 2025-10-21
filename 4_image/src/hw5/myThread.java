import java.lang.Thread;

public class myThread extends Thread {
    int y;
	int width;
	int height;
	int[][] inputImage;
	int [][] outputArray;
	Object[] locks;

	myThread(int[][] inputImage, int[][] outputImage, int y, int width, int height, Object[] locks) {
		this.y = y;
        this.width = width;
		this.height = height;
		this.inputImage = inputImage;
		this.outputArray = outputImage;
		this.locks = locks;
	}

	public void run() {
        for (int x = 0; x < width; x++) {
            int oldPixel = inputImage[y][x];
            int newPixel = (oldPixel > 128) ? 255 : 0;
            outputArray[y][x] = newPixel;
            int error = oldPixel - newPixel;

            // Use Math.floorDiv() to match // in Python
            synchronized (locks[x]) {
                if (x + 1 < width) {
                    // Right
                    inputImage[y][x + 1] += Math.floorDiv(error * 7, 16);
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    // Bottom left
                    inputImage[y + 1][x - 1] += Math.floorDiv(error * 3, 16);
                }
                if (y + 1 < height) {
                    // Bottom
                    inputImage[y + 1][x] += Math.floorDiv(error * 5, 16);
                }
                if (x + 1 < width && y + 1 < height) {
                    // Bottom right
                    inputImage[y + 1][x + 1] += Math.floorDiv(error, 16);
                }
            }
        }
	}
}

