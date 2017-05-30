package dk.itu.lusa.domain.blockpuzzle.screenshot;

import dk.itu.lusa.domain.blockpuzzle.BlockPuzzle;

import java.awt.*;

/**
 * Created by lucas on 23/04/2017.
 */
public class GameFieldScreenshot {
    static private final int PIXEL_PER_BLOCK = 5;
    private Color[][] screenshot;

    public GameFieldScreenshot(int[][] gameField) {
        screenshot = new Color[PIXEL_PER_BLOCK * BlockPuzzle.GRID_SIZE][];
        for (int i = 0; i < screenshot.length; i++)
            screenshot[i] = new Color[PIXEL_PER_BLOCK * BlockPuzzle.GRID_SIZE];

        for(int i=0;i<gameField.length;i++)
            for(int k=0;k<gameField[i].length;k++)
            {
                for(int x = PIXEL_PER_BLOCK*k;x < (k+1)*PIXEL_PER_BLOCK;x++) {
                    int start_y = PIXEL_PER_BLOCK*BlockPuzzle.GRID_SIZE-1 - i*PIXEL_PER_BLOCK;
                    for (int y = start_y; y > start_y - PIXEL_PER_BLOCK; y-- )
                        screenshot[x][y] = AvailableColors.COLORS[gameField[i][k]];
                }
            }
    }

    private GameFieldScreenshot(GameFieldScreenshot c)
    {
        screenshot = new Color[c.screenshot.length][];
        for(int i=0; i < c.screenshot.length;i++) {
            screenshot[i] = new Color[c.screenshot[i].length];
            for (int k = 0; k < c.screenshot[i].length; k++)
                screenshot[i][k] = c.screenshot[i][k];
        }
    }

    public Color[][] getScreenshot() {
        return screenshot;
    }

    public Object clone()
    {
        GameFieldScreenshot g = new GameFieldScreenshot(this);
        return g;
    }
}
