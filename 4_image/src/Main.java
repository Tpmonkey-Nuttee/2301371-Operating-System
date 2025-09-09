import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("No args found.");
            return;
        }

        String fileName = args[0];
        System.out.println("Input file: " + fileName);

        // Read from file
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            System.out.println("Cannot read file: " + e);
            return;
        }

        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();
        System.out.println("Image resolution is " + imageWidth + "x" + imageHeight);

        // Make it 2d array
        int[][] imageArray = new int[imageWidth][imageHeight];
        for (int i = 0; i < imageHeight -1; i++) {
            for (int j = 0; j < imageWidth -1; j++) {
                imageArray[j][i] = image.getRGB(j, i);
                System.out.print(image.getRGB(j, i) + " ");
            }
            System.out.println("\n");
        }
    }
}