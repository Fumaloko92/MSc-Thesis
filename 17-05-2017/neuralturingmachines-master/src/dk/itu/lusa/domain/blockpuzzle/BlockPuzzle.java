package dk.itu.lusa.domain.blockpuzzle;

import com.anji.util.Properties;
import com.ojcoleman.ahni.util.ArrayUtil;
import dk.itu.ejuuragr.domain.BaseSimulator;
import dk.itu.ejuuragr.turing.MinimalTuringMachine;
import dk.itu.ejuuragr.turing.TuringController;
import dk.itu.ejuuragr.turing.TuringMachine;
import dk.itu.lusa.domain.blockpuzzle.reinforcement_learning.StateMap;
import dk.itu.lusa.domain.blockpuzzle.screenshot.SimulationScreenshot;
import dk.itu.lusa.domain.blockpuzzle.screenshot.SimulationsScreenshot;
import dk.itu.lusa.turing.GravesTMWithTemporalLinks;

import java.util.*;


/**
 * Created by lucas on 18/03/2017.
 */
public class BlockPuzzle extends BaseSimulator {
    public static int GRID_SIZE;
    public static int BLOCK_NUMBER;
    public static int MAX_ACTIONS;
    public static int COMBINATIONS_NUMBER;
    public static boolean SHORT_REPRESENTATION;
    public static String FILE_NAME;
    public static int NUMBER_OF_GOALS;
    public static int MAX_CONSTRAINTS_PER_GOAL;
    public static int MIN_CONSTRAINTS_PER_GOAL;
    public static int MAX_OVER_ACTIONS;
    public static boolean PRINT_SCREENSHOT;
    public static String EVOLUTION_MODE;
    private static final boolean DEBUG = false; // True if it should print all input and output
    //gridSize controls also the number of blocks present in the gameField. Change this number accordingly with the available rules
    private int step;
    private double score;
    private int chosenGoal;
    private int[][] gameField = null;
    private int[][] startingGameField = null;
    private LinkedList<Goal> goals = null;
    private int constraintSum;
    private static double multiplier_negative_move;
    private static double multiplier_wrong_move;
    private static boolean repeat_combinations;
    private int combination_index = -1;
    private int goal_index = -1;
    private static LinkedList<int[][]> starting_combinations;
    private int[][] pastOldGameField = null;
    private SimulationScreenshot screenshot;

    public BlockPuzzle(Properties props) {
        super(props);
        multiplier_negative_move = props.getDoubleProperty("simulator.blockpuzzle.multiplier_negative_move");
        multiplier_wrong_move = props.getDoubleProperty("simulator.blockpuzzle.multiplier_wrong_move");
        repeat_combinations = props.getBooleanProperty("simulator.blockpuzzle.repeat_combinations", false);
        if (PRINT_SCREENSHOT)
            screenshot = new SimulationScreenshot();
    }

    // The input of the simulation is used to perform the action
    @Override
    public int getInputCount() {
        return GRID_SIZE * (GRID_SIZE - 1) + 1;
    }

    @Override
    public int getOutputCount() {
        if (goals == null)
            initializeGoals();
        if (!SHORT_REPRESENTATION)
            return 2 + Adjacency.Relation.values().length + BLOCK_NUMBER * 2 + goals.size() + GRID_SIZE * GRID_SIZE * BLOCK_NUMBER;   // 2 to initiate and terminate the transmission
        else
            return 2 + 3 + goals.size() + GRID_SIZE * GRID_SIZE;
        // 1 for the goal
        // 3 for the constraint
        // the board representation
    }

    @Override
    public void restart() {
        initializeGameField();
        initializeGoals();
        constraintSum = 0;
        chosenGoal = goal_index;
        if (PRINT_SCREENSHOT) {
            SimulationsScreenshot.AddSimulation(screenshot);
            screenshot.reset();
            screenshot.addGameFieldScreenshot(goals.get(chosenGoal), gameField);
        }
        increaseGoalIndex();
        for (Goal goal : goals)
            constraintSum += goal.getConstraintSize();
        //score = goals.get(chosenGoal).getScore(getBoardRepresentation());
        score = BlockPuzzleInitializer.GetScore(startingGameField, gameField, null, chosenGoal);
        //constraints = goals.get((int) (Math.random() * goals.size())); GET A RANDOM GOAL
        step = 1;
    }

    @Override
    public double[] getInitialObservation() {
        return getObservation(0);
    }

    @Override
    public double[] performAction(double[] action) {
        if (step > constraintSum + 2) {
            if (DEBUG) System.out.println("-------------------------- BLOCKPUZZLE --------------------------");
            int highest_index = ArrayUtil.getMaxIndex(action);
            for (int i = 0, j = 0; i < GRID_SIZE; i++)
                for (int k = 0; k < GRID_SIZE; k++)
                    if (i != k) {
                        if (j == highest_index)
                            moveBlockAndCalculateScore(i, k);
                        j++;
                    }
            if (highest_index >= GRID_SIZE * (GRID_SIZE - 1))
                updateScoreBasedOnExcessMoves();
            if (PRINT_SCREENSHOT)
                screenshot.addGameFieldScreenshot(goals.get(chosenGoal), gameField);
            if (DEBUG) {
                System.out.println("Score: " + score);
                System.out.println("--------------------------------------------------------------");
            }
        }
        double[] result = getObservation(step);
        step++; // Increment step
        return result;
    }

    private void moveBlockAndCalculateScore(int from, int to) {
        if (DEBUG)
            System.out.println(from + " >> " + to);
        int[][] oldGameField = Utilities.CopyMatrix(gameField);
        if (isColumnEmpty(from) || isColumnFull(to)) {
            this.score *= multiplier_wrong_move;
            if (DEBUG)
                System.out.println(toString());
        } else {
            if (DEBUG)
                System.out.println(toString());
            moveBlock(from, to);
            if (DEBUG)
                System.out.println(toString());
            score = BlockPuzzleInitializer.GetScore(startingGameField, gameField, oldGameField, chosenGoal);
            if (StateMap.TD_ENABLED) {
                double reward = BlockPuzzleInitializer.GetReward(startingGameField, oldGameField, gameField, chosenGoal);
                score += StateMap.DISCOUNT_FACTOR * reward;
            }
            score = dk.itu.ejuuragr.fitness.Utilities.clamp(score, 0, Goal.MAX_SCORE);
            updateScoreBasedOnExcessMoves();
        }
        pastOldGameField = oldGameField;
    }

    private void updateScoreBasedOnExcessMoves() {
        int n_steps = step - constraintSum - 2;
        if (n_steps > goals.get(chosenGoal).getSmallestStepsForWinningComb())
            score *= multiplier_negative_move;
    }

    @Override
    public String toString() {
        String gameField = "";
        for (int i = GRID_SIZE - 1; i >= 0; i--) {
            for (int k = 0; k < GRID_SIZE; k++)
                if (this.gameField[i][k] != 0)
                    gameField += " " + this.gameField[i][k];
                else
                    gameField += "  ";
            gameField += System.lineSeparator();
        }
        return gameField;
    }

    @Override
    public double getCurrentScore() {
        return score;
    }

    @Override
    public int getMaxScore() {
        return 100;
    }

    @Override
    public void reset() {
        super.reset();
        constraintSum = 0;
        goal_index = 0;
        combination_index = 0;
        if (getController() instanceof TuringController) {
            TuringMachine tm = ((TuringController) getController()).getTuringMachine();
            if (tm instanceof MinimalTuringMachine) {
                ((MinimalTuringMachine) tm).setRecordTimeSteps(true);
            }
            if (tm instanceof GravesTMWithTemporalLinks) {
                ((GravesTMWithTemporalLinks) tm).setRecordTimeSteps(true);
            }
        }

        if (DEBUG) System.out.println("---------- RESET ----------");
    }

    @Override
    public boolean isTerminated() {
        return step >= 2 + constraintSum + goals.get(chosenGoal).getSmallestStepsForWinningComb() + BlockPuzzle.MAX_OVER_ACTIONS || goals.get(chosenGoal).isGoalSatisfied(gameField);
    }

    //PRIVATE HELPER METHODS/STRUCTURES

    private double[] getObservation(int step) {
        double[] result = new double[getOutputCount()];
        if (step == 0) {
            result[0] = 1;
        } else {
            if (step <= constraintSum) {
                int correct_step = step - 1;
                int goalIndex = -1;
                int constraintIndex = -1;
                for (int i = 0, goal_step = 0; i < goals.size(); goal_step += goals.get(i).getConstraintSize(), i++)
                    if (correct_step < goal_step + goals.get(i).getConstraintSize()) {
                        goalIndex = i;
                        constraintIndex = correct_step - goal_step;
                        break;
                    }
                double[] goalRepr = goals.get(goalIndex).getRepresentation();
                System.arraycopy(goalRepr, 0, result, 2, goalRepr.length);
                double[] constraintRepr = goals.get(goalIndex).getConstraint(constraintIndex).getRepresentation();
                System.arraycopy(constraintRepr, 0, result, 2 + goalRepr.length, constraintRepr.length);
            } else {
                if (step == constraintSum + 1)
                    result[1] = 1;
                else if (step == constraintSum + 2) {
                    double[] goalRepr = goals.get(chosenGoal).getRepresentation();
                    System.arraycopy(goalRepr, 0, result, 2, goalRepr.length);
                } else if (step > constraintSum + 2) {
                    double[] boardRepr = getBoardRepresentation();
                    if (!SHORT_REPRESENTATION)
                        System.arraycopy(boardRepr, 0, result, 2 + goals.size() + 2 * BLOCK_NUMBER + Adjacency.Relation.values().length, boardRepr.length);
                    else
                        System.arraycopy(boardRepr, 0, result, 2 + goals.size() + 3, boardRepr.length);
                }
            }
        }

        return result;
    }

    private double[] getBoardRepresentation() {
        double[] output;
        if (!SHORT_REPRESENTATION)
            output = new double[GRID_SIZE * GRID_SIZE * BLOCK_NUMBER];
        else
            output = new double[GRID_SIZE * GRID_SIZE];
        for (int i = 0, j = 0; i < GRID_SIZE; i++) {
            for (int k = 0; k < GRID_SIZE; k++, j++) {
                double[] block_repr = Utilities.getBlockRepresentation((gameField[i][k]));
                if (!SHORT_REPRESENTATION)
                    System.arraycopy(block_repr, 0, output, j * BLOCK_NUMBER, block_repr.length);
                else
                    System.arraycopy(block_repr, 0, output, j, block_repr.length);
            }
        }
        return output;
    }


    private void initializeGoals() {
        if (gameField == null)
            initializeGameField();
        goals = BlockPuzzleInitializer.GetGoalsOf(gameField);
    }

    private void initializeGameField() {
        if (!repeat_combinations) {
            gameField = BlockPuzzleInitializer.GetRandomStartingGameField(combination_index);
            startingGameField = Utilities.CopyMatrix(gameField);
        } else {
            gameField = BlockPuzzleInitializer.GetStartingConfigurationAt(combination_index);
            startingGameField = Utilities.CopyMatrix(gameField);
            increaseCombinatonIndex();
        }
    }

    private boolean isColumnEmpty(int col) {
        return gameField[0][col] == 0;
    }

    private boolean isColumnFull(int col) {
        return gameField[GRID_SIZE - 1][col] != 0;
    }

    private void moveBlock(int from, int to) {
        int block = 0;
        for (int i = GRID_SIZE - 1; i >= 0; i--)
            if (gameField[i][from] != 0) {
                block = gameField[i][from];
                gameField[i][from] = 0;
                break;
            }
        for (int i = 0; i < GRID_SIZE; i++)
            if (gameField[i][to] == 0) {
                gameField[i][to] = block;
                break;
            }
    }

    private void increaseCombinatonIndex() {
        combination_index++;
        if (combination_index >= COMBINATIONS_NUMBER)
            combination_index = 0;
    }

    private void increaseGoalIndex() {
        goal_index++;
        if (goal_index >= NUMBER_OF_GOALS)
            goal_index = 0;
    }
}
