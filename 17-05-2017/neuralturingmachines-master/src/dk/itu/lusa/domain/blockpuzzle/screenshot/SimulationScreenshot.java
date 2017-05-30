package dk.itu.lusa.domain.blockpuzzle.screenshot;

import dk.itu.lusa.domain.blockpuzzle.Goal;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by lucas on 23/04/2017.
 */
public class SimulationScreenshot {
    private LinkedList<GameFieldScreenshot> gameFieldScreenshots;
    private GoalScreenshot simulationGoal = null;
    private final int SPACE_BETWEEN_GAMEFIELDS = 1;
    private static int count = 0;


    public SimulationScreenshot() {
        gameFieldScreenshots = new LinkedList<>();
    }

    public void addGameFieldScreenshot(Goal g,int[][] gameField) {
        gameFieldScreenshots.add(new GameFieldScreenshot(gameField));
        if(simulationGoal == null)
            simulationGoal = new GoalScreenshot(g);
    }

    public int[][] generateScreenshot() {
        int width = calculateWidth();
        int height = calculateHeight();
        int[][] screenshotR = new int[height][];
        for (int i = 0; i < height; i++)
            screenshotR[i] = new int[width];
        int x = 0, y = 0;
        if(simulationGoal!=null) {
            Color[][] s_g = simulationGoal.getGoalScreenshot();
            for (int i = 0; i < s_g.length; i++, x++) {
                y = 0;
                for (int k = 0; k < s_g[i].length; k++, y++)
                    screenshotR[y][x] = s_g[i][k].getRGB();
            }
            x = s_g.length;
        }

        for (GameFieldScreenshot screenshot : gameFieldScreenshots) {
            Color[][] s = screenshot.getScreenshot();
            for (int i = 0; i < SPACE_BETWEEN_GAMEFIELDS; i++, x++) {
                for (y = 0; y < height; y++)
                    screenshotR[y][x] = Color.BLACK.getRGB();
            }
            for (int i = 0; i < s.length; i++, x++) {
                y = 0;
                for (int k = 0; k < s.length; k++, y++)
                    screenshotR[y][x] = s[i][k].getRGB();
            }


        }
        return screenshotR;
    }

    public boolean isValid()
    {
        return gameFieldScreenshots.size()>1;
    }

    public int calculateWidth() {
        int width = 0;
        width = gameFieldScreenshots.size() * gameFieldScreenshots.get(0).getScreenshot().length + (gameFieldScreenshots.size()) * SPACE_BETWEEN_GAMEFIELDS + (simulationGoal != null ? simulationGoal.calculateWidth() : 0);
        return width;
    }

    public int calculateHeight() {
        int height = 0;
        height = simulationGoal!=null ? Math.max(gameFieldScreenshots.get(0).getScreenshot().length, simulationGoal.calculateHeight()) : gameFieldScreenshots.get(0).getScreenshot().length;
        return height;
    }

    public void reset() {
        gameFieldScreenshots.clear();
        simulationGoal = null;
    }

    public Object clone() {
        SimulationScreenshot s = new SimulationScreenshot();
        for (GameFieldScreenshot gameFieldScreenshot : gameFieldScreenshots)
            s.gameFieldScreenshots.add((GameFieldScreenshot) gameFieldScreenshot.clone());
        s.simulationGoal = new GoalScreenshot(simulationGoal);
        return s;
    }

    public boolean isEmpty() {
        return gameFieldScreenshots.isEmpty();
    }
}
