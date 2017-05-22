package dk.itu.lusa.domain.blockpuzzle.screenshot;

import dk.itu.lusa.domain.blockpuzzle.BlockPuzzle;
import dk.itu.lusa.domain.blockpuzzle.Goal;

import java.awt.*;

/**
 * Created by lucas on 30/04/2017.
 */
public class GoalScreenshot {
    private Goal goal;
    private final int PIXELS_PER_BLOCK = 30;
    private final int SPACE_BETWEEN_CONSTRAINT = 5;
    private Color[][] goalScreenshot;

    public GoalScreenshot(Goal g)
    {
        goal = (Goal)g.clone();
        makeGoalScreenshot();
    }

    public GoalScreenshot(GoalScreenshot g)
    {
        goal =(Goal)g.goal.clone();
        makeGoalScreenshot();
    }

    public Color[][] getGoalScreenshot()
    {
        return goalScreenshot;
    }

    private void makeGoalScreenshot()
    {
        int[][] goalRepr = goal.getConstraintsInGameField();
        goalScreenshot = new Color[PIXELS_PER_BLOCK*goalRepr[0].length][];
        for (int i=0;i<goalScreenshot.length;i++)
            goalScreenshot[i] = new Color[PIXELS_PER_BLOCK*goalRepr.length];

        for(int i=0;i<goalRepr.length;i++)
            for(int k=0;k<goalRepr[i].length;k++)
            {
                for(int x = PIXELS_PER_BLOCK*k;x < (k+1)*PIXELS_PER_BLOCK;x++) {
                    int start_y = PIXELS_PER_BLOCK*BlockPuzzle.GRID_SIZE-1 - i*PIXELS_PER_BLOCK;
                    for (int y = start_y; y > start_y - PIXELS_PER_BLOCK; y-- )
                        goalScreenshot[x][y] = AvailableColors.COLORS[goalRepr[i][k]];
                }
            }
    }

    public int calculateWidth()
    {
        return goalScreenshot.length;
    }

    public int calculateHeight()
    {
        return goalScreenshot[0].length;
    }
}
