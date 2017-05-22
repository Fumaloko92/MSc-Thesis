package dk.itu.lusa.domain.blockpuzzle.screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by lucas on 23/04/2017.
 */
public class SimulationsScreenshot {
    private static LinkedList<SimulationScreenshot> simulations = new LinkedList<>();
    private static Object lock = new Object();
    private static int count = 0;
    private static final int ROW_SEPARATOR_HEIGHT = 10;

    public static synchronized void AddSimulation(SimulationScreenshot s) {
        if (!s.isEmpty())
            synchronized (lock) {
                if (s.isValid())
                    simulations.add((SimulationScreenshot) s.clone());
            }
    }

    public static synchronized void ClearSimulations() {
        synchronized (lock) {
            simulations.clear();
        }
    }

    public static synchronized void GenerateScreenshot(String name) {
        int width = 0;
        int height = 0;
        for (SimulationScreenshot simulation : simulations) {
            width = Math.max(simulation.calculateWidth(), width);
            height += simulation.calculateHeight() + ROW_SEPARATOR_HEIGHT;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int offset_H = 0;
        for (SimulationScreenshot simulation : simulations) {
            int[][] repr = simulation.generateScreenshot();
            for (int x = 0; x < repr.length; x++)
                for (int y = 0; y < repr[x].length; y++)
                    image.setRGB(y, offset_H + x, repr[x][y]);
            offset_H += simulation.calculateHeight() + ROW_SEPARATOR_HEIGHT;
        }
        synchronized (lock) {
            try {
                File f = new File("screenshots/" + name + count + ".png");
                ImageIO.write(image, "PNG", f);
                count++;
            } catch (IOException e) {
                System.out.println("Problems with the screenshot");
            }
        }
    }
}
