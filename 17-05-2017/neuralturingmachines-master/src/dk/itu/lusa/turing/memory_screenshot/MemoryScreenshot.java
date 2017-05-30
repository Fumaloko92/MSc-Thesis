package dk.itu.lusa.turing.memory_screenshot;

import com.ojcoleman.ahni.util.ArrayUtil;
import dk.itu.lusa.domain.blockpuzzle.Utilities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Created by lucas on 02/04/2017.
 */
public class MemoryScreenshot {
    static final int CELL_SIZE = 3;
    static public boolean PRINT_MEMORY_SCREENSHOTS;
    static private final Object lock = new Object();
    static private int count = 0;
    static private WholeScreenshot screenshotMemories = new WholeScreenshot();

    static public void CreateScreenshotTemporalMatrix(String name, LinkedList<LinkedList<Double>> values) {
        if (values.size() > 0) {
            BufferedImage image = new BufferedImage(CELL_SIZE * values.get(0).size(), CELL_SIZE * values.size(), BufferedImage.TYPE_BYTE_GRAY);
            for (int i = 0; i < values.size(); i++) {
                for (int k = 0; k < values.get(i).size(); k++) {
                    int value = (int) (values.get(i).get(k) * 256) << 16 | (int) (values.get(i).get(k) * 256) << 8 | (int) (values.get(i).get(k) * 256);
                    for (int x = i * CELL_SIZE; x < (i + 1) * CELL_SIZE; x++) {
                        for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                            image.setRGB(y, x, value);
                        }
                    }
                }
            }
            synchronized (lock) {
                try {
                    File f = new File("screenshots/" + name + count + ".png");
                    ImageIO.write(image, "PNG", f);
                } catch (IOException e) {
                    System.out.println("Problems with the screenshot");
                }
            }
        }
    }

    static public void MakeScreenshot(String name, LinkedList<double[]> tape, LinkedList<LinkedList<Double>> temporalMatrix) {
        if (tape.size() > 0 || temporalMatrix.size() > 0) {
            BufferedImage image = new BufferedImage(CELL_SIZE * Math.max(tape.get(0).length, temporalMatrix.get(0).size()), CELL_SIZE * (tape.size() + temporalMatrix.size() + 1), BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < tape.size(); i++) {
                for (int k = 0; k < tape.get(i).length; k++) {
                    int value = (int) (tape.get(i)[k] * 256) << 16 | (int) (tape.get(i)[k] * 256) << 8 | (int) (tape.get(i)[k] * 256);
                    for (int x = i * CELL_SIZE; x < (i + 1) * CELL_SIZE; x++) {
                        for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                            image.setRGB(y, x, value);
                        }
                    }
                }
            }
            int s_tm = tape.size() + 1;
            for (int i = s_tm, j = 0; i < s_tm + temporalMatrix.size(); i++, j++) {
                for (int k = 0; k < temporalMatrix.get(j).size(); k++) {
                    int value = (int) (temporalMatrix.get(j).get(k) * 256) << 16 | (int) (256) << 8 | (int) (temporalMatrix.get(j).get(k) * 256);
                    for (int x = i * CELL_SIZE; x < (i + 1) * CELL_SIZE; x++) {
                        for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                            image.setRGB(y, x, value);
                        }
                    }
                }
            }
            synchronized (lock) {
                try {
                    File f = new File("screenshots/" + name + count + ".png");
                    ImageIO.write(image, "PNG", f);
                } catch (IOException e) {
                    System.out.println("Problems with the screenshot");
                }
            }
        }
    }

    static public void CreateScreenshot(String name, LinkedList<double[]> values) {
        if (values.size() > 0) {
            BufferedImage image = new BufferedImage(CELL_SIZE * values.get(0).length, CELL_SIZE * values.size(), BufferedImage.TYPE_BYTE_GRAY);
            for (int i = 0; i < values.size(); i++) {
                for (int k = 0; k < values.get(i).length; k++) {
                    int value = (int) (values.get(i)[k] * 256) << 16 | (int) (values.get(i)[k] * 256) << 8 | (int) (values.get(i)[k] * 256);
                    for (int x = i * CELL_SIZE; x < (i + 1) * CELL_SIZE; x++) {
                        for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                            image.setRGB(y, x, value);
                        }
                    }
                }
            }
            synchronized (lock) {
                try {
                    File f = new File("screenshots/" + name + count + ".png");
                    ImageIO.write(image, "PNG", f);
                } catch (IOException e) {
                    System.out.println("Problems with the screenshot");
                }
            }
        }
    }

    static public void IncreaseCount() {
        count++;
    }


    static public synchronized int CreateNewScreenshot() {
        synchronized (lock) {
            ScreenshotMemory m = new ScreenshotMemory();
            screenshotMemories.goal_screenshots.add(m);
            return screenshotMemories.goal_screenshots.size() - 1;
        }
    }

    static public synchronized void AddMemoryToScreenshot(int index, LinkedList<double[]> tape,int[] write_head, int[] read_head, int[] after_shift, double[][] modes, int[] chosen_modes, LinkedList<LinkedList<Double>> temporalMatrix, LinkedList<Double> precedenceVector) {
        synchronized (lock) {
            ScreenshotMemory m = screenshotMemories.goal_screenshots.get(index);
            m.setScreenshotMemory(tape,write_head, read_head, after_shift, modes, chosen_modes, temporalMatrix, precedenceVector);
            screenshotMemories.goal_screenshots.set(index, m);
        }
    }

    static public synchronized void AddMemoryToScreenshot(int index, LinkedList<double[]> tape, int[] read_head, int[] write_head, int[] shift_head) {
        synchronized (lock) {
            ScreenshotMemory m = screenshotMemories.goal_screenshots.get(index);
            m.setScreenshotMemory(tape,read_head,write_head,shift_head );
            screenshotMemories.goal_screenshots.set(index, m);
        }
    }

    static public synchronized void GenerateScreenshot(String name) {
        synchronized (lock) {
            try {
                File f = new File("screenshots/" + name + count + ".png");
                ImageIO.write(screenshotMemories.generateImage(), "PNG", f);
            } catch (IOException e) {
                System.out.println("Problems with the screenshot");
            }
        }
    }
}

