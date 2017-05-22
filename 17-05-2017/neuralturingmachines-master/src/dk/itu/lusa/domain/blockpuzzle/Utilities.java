package dk.itu.lusa.domain.blockpuzzle;

import com.anji.util.Arrays;
import com.anji.util.Properties;
import com.ojcoleman.ahni.util.ArrayUtil;
import dk.itu.lusa.domain.blockpuzzle.reinforcement_learning.StateMap;
import dk.itu.lusa.turing.memory_screenshot.MemoryScreenshot;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by lucas on 25/03/2017.
 */
public class Utilities {
    public static void InitializeStaticVariables(Properties props) {
        BlockPuzzle.GRID_SIZE = props.getIntProperty("simulator.blockpuzzle.static.grid_size", 3);
        BlockPuzzle.BLOCK_NUMBER = props.getIntProperty("simulator.blockpuzzle.static.block_number", 6);
        BlockPuzzle.MAX_ACTIONS = props.getIntProperty("simulator.blockpuzzle.static.max_actions", 10);
        BlockPuzzle.COMBINATIONS_NUMBER = props.getIntProperty("simulator.blockpuzzle.static.combinations_number", 3);
        BlockPuzzle.SHORT_REPRESENTATION = props.getBooleanProperty("simulator.blockpuzzle.static.short_representation", false);
        BlockPuzzle.FILE_NAME = System.getProperty("user.dir") + "\\logs\\" + props.getProperty("run.name") + ".txt";
        BlockPuzzle.MAX_CONSTRAINTS_PER_GOAL = props.getIntProperty("simulator.blockpuzzle.static.max_constraints_per_goal", 10);
        BlockPuzzle.MIN_CONSTRAINTS_PER_GOAL = props.getIntProperty("simulator.blockpuzzle.static.min_constraints_per_goal", 2);
        BlockPuzzle.NUMBER_OF_GOALS = props.getIntProperty("simulator.blockpuzzle.static.number_of_goals", 1);
        BlockPuzzle.EVOLUTION_MODE = props.getProperty("simulator.blockpuzzle.static.evolution_mode", "normal");
        BlockPuzzle.MAX_OVER_ACTIONS = props.getIntProperty("simulator.blockpuzzle.static.max_over_actions", 0);
        BlockPuzzle.PRINT_SCREENSHOT = props.getBooleanProperty("simulator.blockpuzzle.static.print_screenshot", false);

        MemoryScreenshot.PRINT_MEMORY_SCREENSHOTS = props.getBooleanProperty("tm.static.print_memory_screenshots", false);

        StateMap.DISCOUNT_FACTOR = props.getDoubleProperty("simulator.blockpuzzle.rl.discount_factor", 1.0);
        StateMap.LEARNING_RATE = props.getDoubleProperty("simulator.blockpuzzle.rl.learning_rate", 1.0);
        StateMap.TD_ENABLED = props.getBooleanProperty("simulator.blockpuzzle.rl.td_learning_enabled", false);
        if (props.containsKey("simulator.blockpuzzle.game_field") && props.containsKey("simulator.blockpuzzle.goals")) {
            LinkedList<int[][]> gameFields = new LinkedList<>();
            String s_field = props.getProperty("simulator.blockpuzzle.game_field");
            String[] g_fields = s_field.split("-");
            for (int ind = 0; ind < g_fields.length; ind++) {
                String[] rows = g_fields[ind].split(";");
                int[][] gameField = new int[rows.length][];
                for (int i = 0; i < gameField.length; i++) {
                    int k = 0;
                    String[] elements = rows[i].split(",");
                    gameField[i] = new int[elements.length];
                    for (String el : elements) {
                        gameField[i][k] = Integer.parseInt(el);
                        k++;
                    }
                }
                gameFields.add(gameField);
                BlockPuzzleInitializer.AddStartingGameField(gameField);
            }

            LinkedList<LinkedList<Goal>> goals = new LinkedList<>();
            String s_goals = props.getProperty("simulator.blockpuzzle.goals");
            String[] m_goals = s_goals.split("-");
            for(int ind=0;ind<m_goals.length;ind++) {
                LinkedList<Goal> m_goal = new LinkedList<>();
                for (String goal : m_goals[ind].split(";")) {
                    LinkedList<Constraint> constraints = new LinkedList<>();
                    for (String constraint : goal.split(","))
                        constraints.add(new Constraint(constraint));
                    m_goal.add(new Goal(constraints, gameFields.get(ind)));
                }
                goals.add(m_goal);
            }
            BlockPuzzleInitializer.SetGoals(goals);
        }
    }

    public static void WriteToFile(String s) {
        try {
            File file = new File(BlockPuzzle.FILE_NAME);
            FileWriter fw;
            if (file.exists()) {
                fw = new FileWriter(file, true);//if file exists append to file. Works fine.
            } else {
                fw = new FileWriter(file);// If file does not exist. Create it. This throws a FileNotFoundException. Why?
            }
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(s + System.lineSeparator());
            bw.close();
            fw.close();
        } catch (IOException e) {
            System.out.println("Houston we have a problem");
        }
    }

    public static void DeleteFile() {
        try {
            File file = new File(BlockPuzzle.FILE_NAME);
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static double[] getBlockRepresentation(int block) {
        if (!BlockPuzzle.SHORT_REPRESENTATION) {
            double[] repr = ArrayUtil.newArray(BlockPuzzle.BLOCK_NUMBER, 0.0);
            if (block != 0)
                repr[block - 1] = 1.0;
            return repr;
        }
        double[] repr = new double[1];
        if (block != 0)
            repr[0] = Math.pow(2, block);
        return repr;
    }

    public static boolean blocksEqual(double[] block_1, double[] block_2) {
        if (!BlockPuzzle.SHORT_REPRESENTATION) {
            if (block_1.length != block_2.length)
                System.out.println(Arrays.toString(block_1) + " vs " + Arrays.toString(block_2));
            for (int i = 0; i < block_1.length; i++)
                if (block_1[i] != block_2[i])
                    return false;
            return true;
        } else {
            if (block_1.length == block_2.length) {
                if (block_1.length != 1) {
                    System.out.println(Arrays.toString(block_1) + " vs " + Arrays.toString(block_2));
                    return false;
                }
                return block_1[0] == block_2[0];
            }
            System.out.println(Arrays.toString(block_1) + " vs " + Arrays.toString(block_2));
            return false;
        }
    }

    public static int[][] CopyMatrix(int[][] source) {
        int[][] result = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = new int[source[i].length];
            System.arraycopy(source[i], 0, result[i], 0, source[i].length);
        }
        return result;
    }

    public static long GameFieldHashCode(int[][] gameField) {
        long r = 0;
        String s = "";
        int count = gameField.length * gameField.length;
        HashMap<Integer, Integer> encodings = new HashMap<>();
        for (int i = 0, j = 1; i < gameField.length; i++)
            for (int k = 0; k < gameField.length; k++, j++) {
                if (gameField[i][k] != 0)
                    encodings.put(gameField[i][k], count * (gameField[i][k] - 1) + j);
            }
        for (int i = 1; i <= BlockPuzzle.BLOCK_NUMBER; i++)
            s += encodings.get(i);
        r = Long.parseLong(s);
        return r;
    }

    public static boolean MatrixEqual(int[][] m, int[][] m1) {
        if (m.length != m1.length)
            return false;
        for (int i = 0; i < m.length; i++) {
            if (m[i].length != m1[i].length)
                return false;
            for (int k = 0; k < m[i].length; k++)
                if (m[i][k] != m1[i][k])
                    return false;
        }
        return true;
    }

}
