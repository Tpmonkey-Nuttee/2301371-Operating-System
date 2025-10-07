import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("java src/Main.java <input> <output>");
            return;
        }

        String fileName = args[0];
        String outputName = args[1];
        System.out.println("Input file: " + fileName);

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.out.println("Cannot read file: " + e);
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        int[][] input = new int[height + 2][width + 2];
        byte[][] output = new byte[height][width];


        for (int m = 0; m < height; m++) {
            for (int n = 0; n < width; n++) {
                int gray = image.getRGB(n, m) & 0xFF;
                input[m + 1][n + 1] = gray;
            }
        }

        for (int i = 1; i <= height; i++) {
            for (int j = 1; j <= width; j++) {
                int oldVal = input[i][j];
                int newVal = (oldVal < 128) ? 0 : 255;
                output[i - 1][j - 1] = (byte) (newVal == 0 ? 0 : 1);

                int err = oldVal - newVal;

                input[i][j + 1] += (err * 7) / 16;
                input[i + 1][j - 1] += (err * 3) / 16;
                input[i + 1][j] += (err * 5) / 16;
                input[i + 1][j + 1] += err / 16;
            }
        }

        BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int val = (output[y][x] == 0) ? 0x000000 : 0xFFFFFF;
                outImg.setRGB(x, y, val);
            }
        }

        try {
            ImageIO.write(outImg, "png", new File(outputName));
        } catch (IOException e) {
            System.out.println("Output error: " + e);
            return;
        }
        System.out.println("Done! Output saved to: " + outputName);
    }
}