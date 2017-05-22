package dk.itu.lusa.domain.blockpuzzle;

import com.ojcoleman.ahni.util.ArrayUtil;
import dk.itu.lusa.domain.blockpuzzle.reinforcement_learning.StateMap;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by lucas on 22/03/2017.
 */
public class BlockPuzzleInitializer {
    public static boolean PRINT_MEMORY_SCREENSHOTS;
    private static LinkedList<int[][]> starting_configurations = null;
    private static HashMap<Long, StateMap> state_maps = new HashMap<>();
    private static HashMap<Long, LinkedList<Goal>> goals_per_state_map = new HashMap<>();
    private static final Object lock = new Object();
    private static LinkedList<int[][]> gameFields = null;
    private static LinkedList<LinkedList<Goal>> goals = null;

    private static LinkedList<Long> hashAssociations = null;
    private static HashMap<Long, int[][]> availableGameFields = null;

    public static int[][] GetStartingConfigurationAt(int index) {
        synchronized (lock) {
            if (starting_configurations == null)
                ResetStartingConfigurations();
            int[][] m = starting_configurations.get(index);
            int[][] c = new int[m.length][];
            for (int i = 0; i < m.length; i++) {
                c[i] = new int[m[i].length];
                System.arraycopy(m[i], 0, c[i], 0, m[i].length);
            }

            return c;
        }
    }

    public static boolean IsGameFieldSet() {
        return gameFields == null;
    }

    public static boolean AreGoalsSet() {
        return goals == null;
    }

    public synchronized static void SetGoals(LinkedList<LinkedList<Goal>> g) {
        synchronized (lock) {
            goals = g;
        }
    }

    public synchronized static int GetNumberOfGoalsPerStateMap(Long h) {
        synchronized (lock) {
            if (goals_per_state_map.isEmpty())
                InitializeStaticData();
        }
        return goals_per_state_map.get(h).size();
    }

    public synchronized static int GetNumberOfGoalsPerStateMap(int[][] rootStateMap) {
        synchronized (lock) {
            if (goals_per_state_map.isEmpty())
                InitializeStaticData();
        }
        return goals_per_state_map.get(Utilities.GameFieldHashCode(rootStateMap)).size();
    }

    public synchronized static void ResetStartingConfigurations() {
        synchronized (lock) {
            starting_configurations = new LinkedList<>();
            for (int i = 0; i < BlockPuzzle.COMBINATIONS_NUMBER; i++)
                starting_configurations.add(GetRandomStartingGameField(i));
        }
    }

    public synchronized static void AddStartingGameField(int[][] field) {
        synchronized (lock) {
            if (gameFields == null)
                gameFields = new LinkedList<>();
            gameFields.add(field);
        }
    }

    public static LinkedList<Goal> GetGoalsOf(int[][] starting_comb) {
        long h = Utilities.GameFieldHashCode(starting_comb);
        if (goals_per_state_map.containsKey(h))
            return goals_per_state_map.get(h);
        else
            return null;
    }

    public synchronized static int[][] GetRandomStartingGameField(int combination_index) {
        int[][] gameField;
        if (BlockPuzzleInitializer.gameFields == null) {
            synchronized (lock) {
                gameField = ExtractAvailableGameField();
            }
        } else {
            synchronized (lock) {
                gameField = BlockPuzzleInitializer.gameFields.get(combination_index);
            }
        }
        long h = Utilities.GameFieldHashCode(gameField);
        if (!state_maps.containsKey(h))
            state_maps.put(h, new StateMap(gameField, BlockPuzzle.MAX_ACTIONS + BlockPuzzle.MAX_OVER_ACTIONS));

        return gameField;

    }

    private static int[][] GenerateRandomGameField() {
        int[][] gameField;
        gameField = new int[BlockPuzzle.GRID_SIZE][];
        int[] gameFieldIndexes = ArrayUtil.newArray(BlockPuzzle.GRID_SIZE, 0);
        for (int i = 0; i < BlockPuzzle.GRID_SIZE; i++)
            gameField[i] = ArrayUtil.newArray(BlockPuzzle.GRID_SIZE, 0);


        LinkedList<Integer> indexes = new LinkedList<>();
        for (int i = 0; i < BlockPuzzle.GRID_SIZE; i++)
            indexes.add(i);

        for (int i = 0; i < BlockPuzzle.BLOCK_NUMBER; i++) {
            int rndIndex = (int) (Math.random() * Integer.MAX_VALUE) % indexes.size();
            int vectorIndex = indexes.get(rndIndex);
            gameField[gameFieldIndexes[vectorIndex]][vectorIndex] = i + 1;
            gameFieldIndexes[vectorIndex]++;
            if (gameFieldIndexes[vectorIndex] >= BlockPuzzle.GRID_SIZE)
                indexes.remove(rndIndex);

        }
        return gameField;
    }

    private static synchronized void GenerateAvailableGameFields() {
        synchronized (lock) {
            availableGameFields = new HashMap<>();
            hashAssociations = new LinkedList<>();
            RecursiveGeneration(GenerateRandomGameField(), 20);
        }
    }

    private static synchronized void RecursiveGeneration(int[][] element, int depth) {
        Long h = Utilities.GameFieldHashCode(element);
        if (!availableGameFields.containsKey(h) && depth > 0) {
            availableGameFields.put(h, element);
            hashAssociations.add(h);
            for (int i = 0; i < BlockPuzzle.GRID_SIZE; i++)
                for (int k = 0; k < BlockPuzzle.GRID_SIZE; k++)
                    if (element[0][i] != 0 && element[BlockPuzzle.GRID_SIZE - 1][k] == 0)
                        RecursiveGeneration(moveBlock(Utilities.CopyMatrix(element), i, k), depth - 1);
        }
    }

    private static synchronized int[][] ExtractAvailableGameField()
    {
        synchronized (lock)
        {
            int el = (int)(Math.random()*availableGameFields.size());
            int[][] ret = availableGameFields.get(hashAssociations.get(el));
            availableGameFields.remove(hashAssociations.get(el));
            hashAssociations.remove(el);
            return ret;
        }
    }

    private static synchronized int[][] moveBlock(int[][] gameField, int from, int to) {
        int block = 0;
        for (int i = gameField.length - 1; i >= 0; i--)
            if (gameField[i][from] != 0) {
                block = gameField[i][from];
                gameField[i][from] = 0;

                for (int k = 0; k < gameField.length; k++)
                    if (gameField[k][to] == 0) {
                        gameField[k][to] = block;
                        break;
                    }
                break;
            }
        return gameField;
    }

    public static synchronized void InitializeStaticData() {
        synchronized (lock) {
            if (gameFields== null && availableGameFields == null)
                GenerateAvailableGameFields();
            ResetStartingConfigurations();
            for (StateMap stateMap : state_maps.values())
                stateMap.calculateDistancesFromRoot();
            if (goals == null) {
                for (int i = 0; i < BlockPuzzle.NUMBER_OF_GOALS; i++) {
                    for (StateMap stateMap : state_maps.values()) {
                        int[][] starting_combination = stateMap.getRoot().getCurrentState();
                        long h = Utilities.GameFieldHashCode(starting_combination);
                        LinkedList<int[][]> exploredCombinations = state_maps.get(h).getExploredCombinationsWithinMaxActions();
                        int index = (int) (Math.random() * exploredCombinations.size());
                        int[][] combination_to_generate_goal = exploredCombinations.get(index);
                        LinkedList<Goal> goals = new LinkedList<>();
                        if (goals_per_state_map.containsKey(h))
                            goals = goals_per_state_map.get(h);
                        goals.add(ExtractGoalFromCombination(starting_combination, combination_to_generate_goal));
                        goals_per_state_map.put(h, goals);
                    }


                }
            } else {
                for (StateMap sm : state_maps.values()) {
                    int i = 0;
                    for (i = 0; i < gameFields.size(); i++)
                        if (Utilities.MatrixEqual(gameFields.get(i), sm.getRoot().getCurrentState()))
                            break;
                    goals_per_state_map.put(Utilities.GameFieldHashCode(sm.getRoot().getCurrentState()), goals.get(i));
                }
            }
            int h = 0, h1 = 0;
            for (StateMap stateMap : state_maps.values()) {
                stateMap.evaluateMap();
                for (int i = 0; i < BlockPuzzle.NUMBER_OF_GOALS; i++) {
                    h = stateMap.getNumberOfSuccessfullStatesForGoal(i);
                    h1 = stateMap.getNumberOfNullStatesForGoal(i);
                    System.out.println("Goal n°" + (i + 1) + ": 100s:" + h + " 0s:" + h1);
                }
            }

            for (long key : goals_per_state_map.keySet()) {
                LinkedList<Goal> goals = goals_per_state_map.get(key);
                for (int i = 0; i < goals.size(); i++)
                    goals.get(i).setSmallestStepsForWinningComb(state_maps.get(key).getSmallestStepForWinningComb(i));
                goals_per_state_map.put(key, goals);
            }
            Utilities.DeleteFile();
            Utilities.WriteToFile("SM:");
            String starting_combs = "";
            String goals = "";
            for (StateMap s : state_maps.values()) {
                starting_combs += s.getRoot().serialization() + "-";
                for (Goal g : goals_per_state_map.get(Utilities.GameFieldHashCode(s.getRoot().getCurrentState())))
                    goals += g.toString() + ";";
                goals += "-";
            }
            Utilities.WriteToFile(starting_combs);
            Utilities.WriteToFile("G:");
            Utilities.WriteToFile(goals);
            Utilities.WriteToFile("R:");
        }
    }

    private static Goal ExtractGoalFromCombination(int[][] root_combination, int[][] starting_combination) {
        LinkedList<Constraint> constraints = ExtractAllConstraintsFromCombination(starting_combination);
        LinkedList<Constraint> goal_constraints = new LinkedList<>();
        for (int i = 0; i < BlockPuzzle.MAX_CONSTRAINTS_PER_GOAL; i++) {
            if (i < BlockPuzzle.MIN_CONSTRAINTS_PER_GOAL) {
                int index = (int) (Math.random() * constraints.size());
                goal_constraints.add(constraints.get(index));
                constraints.remove(index);
            } else if (constraints.size() > 0 && Math.random() < 0.5) {
                int index = (int) (Math.random() * constraints.size());
                goal_constraints.add(constraints.get(index));
                constraints.remove(index);
            }

        }
        return new Goal(goal_constraints, root_combination);
    }

    private static LinkedList<Constraint> ExtractAllConstraintsFromCombination(int[][] combination) {
        LinkedList<Constraint> constraints = new LinkedList<>();
        for (int i = 0; i < combination.length; i++) {
            for (int k = 0; k < combination[i].length; k++) {
                if (combination[i][k] != 0) {
                    if (k - 1 >= 0) {
                        if (combination[i][k - 1] != 0) {
                            Constraint c = new Constraint(combination[i][k], combination[i][k - 1], Adjacency.Relation.Right);
                            if (!constraints.contains(c))
                                constraints.add(c);
                        }
                    }
                    if (k + 1 < combination[i].length) {
                        if (combination[i][k + 1] != 0) {
                            Constraint c = new Constraint(combination[i][k], combination[i][k + 1], Adjacency.Relation.Left);
                            if (!constraints.contains(c))
                                constraints.add(c);
                        }
                    }
                    if (i + 1 < combination.length) {
                        if (combination[i + 1][k] != 0) {
                            Constraint c = new Constraint(combination[i][k], combination[i + 1][k], Adjacency.Relation.Below);
                            if (!constraints.contains(c))
                                constraints.add(c);
                        }
                    }
                    if (i - 1 >= 0) {
                        if (combination[i - 1][k] != 0) {
                            Constraint c = new Constraint(combination[i][k], combination[i - 1][k], Adjacency.Relation.Above);
                            if (!constraints.contains(c))
                                constraints.add(c);
                        }
                    }
                }
            }
        }
        return constraints;
    }

    public static double GetScore(int[][] stateMapRoot, int[][] currentState, int[][] previousState, int goalIndex) {
        return state_maps.get(Utilities.GameFieldHashCode(stateMapRoot)).getStateScoreOfGoal(currentState, previousState, goalIndex);
    }

    public static double GetReward(int[][] stateMapRoot, int[][] oldState, int[][] newState, int goalIndex) {
        return state_maps.get(Utilities.GameFieldHashCode(stateMapRoot)).getReward(oldState, newState, goalIndex);
    }
}
