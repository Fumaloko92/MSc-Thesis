package dk.itu.lusa.domain.blockpuzzle.reinforcement_learning;

import dk.itu.lusa.domain.blockpuzzle.BlockPuzzle;
import dk.itu.lusa.domain.blockpuzzle.Goal;
import dk.itu.lusa.domain.blockpuzzle.Utilities;

import java.util.*;

/**
 * Created by lucas on 03/04/2017.
 */
public class StateMap {
    private State root;
    private HashMap<Long, State> states;
    private int maxDepth;
    public static double DISCOUNT_FACTOR = 0.5;
    public static double LEARNING_RATE = 0.5;
    public static boolean TD_ENABLED;

    public StateMap(int[][] startingGameField, int maxDepth) {
        states = new HashMap<>();
        root = new State(startingGameField, this);
        states.put(Utilities.GameFieldHashCode(root.getCurrentState()), root);
        this.maxDepth = maxDepth;
        generateStates();
    }

    public void evaluateMap() {
        for (int i = 0; i < BlockPuzzle.NUMBER_OF_GOALS; i++)
            root.evaluate(i);
    }

    private void generateStates() {
        recursiveGeneration(root, maxDepth);
    }

    private void recursiveGeneration(State state, int depth) {
        if (depth >= 0) {
            long h_state = Utilities.GameFieldHashCode(state.getCurrentState());
            if (states.containsKey(h_state))
                state = states.get(h_state);
            int[][] field = state.getCurrentState();
            for (int i = 0; i < field.length; i++) {
                for (int k = 0; k < field.length; k++) {
                    if (i != k && field[0][i] != 0 && field[field.length - 1][k] == 0) {
                        State after_move = new State(moveBlock(Utilities.CopyMatrix(field), i, k), this);
                        long h = Utilities.GameFieldHashCode(after_move.getCurrentState());

                        if (!states.containsKey(h) || !states.get(h).isExplored(h_state))
                            recursiveGeneration(after_move, depth - 1);
                        if (states.containsKey(h))
                            after_move = states.get(h);
                        state.connectTo(after_move);
                        states.put(h_state, state);
                        states.put(h, after_move);
                    }
                }
            }
        }

    }

    private int[][] moveBlock(int[][] gameField, int from, int to) {
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

    public LinkedList<int[][]> getExploredCombinationsWithinMaxActions() {
        LinkedList<int[][]> r = new LinkedList<>();
        for (State s : states.values())
            if (s.getShortestDistanceFromRoot() > 0 && s.getShortestDistanceFromRoot() < BlockPuzzle.MAX_ACTIONS)
                r.add(s.getCurrentState());
        return r;
    }

    public State getRoot() {
        return root;
    }

    public int getNumberOfSuccessfullStatesForGoal(int index) {
        int count = 0;
        for (State state : states.values())
            if (state.getScoreOfGoal(index) == Goal.MAX_SCORE)
                count++;
        return count;
    }

    public int getNumberOfNullStatesForGoal(int index) {
        int count = 0;
        for (State state : states.values())
            if (state.getScoreOfGoal(index) == 0)
                count++;
        return count;
    }

    public HashMap<State, Double> getStatesScores(int index) {
        HashMap<State, Double> r = new HashMap<>();
        for (State state : states.values())
            r.put(state, state.getScoreOfGoal(index));
        return r;
    }

    public double getStateScoreOfGoal(int[][] state, int[][] previousState, int goalIndex) {
        long l = Utilities.GameFieldHashCode(state);
        if (!states.containsKey(l)) {
            State p = states.get(Utilities.GameFieldHashCode(previousState));
            p.resetEvaluated();
            p.evaluate(goalIndex);
        }
        State s = states.get(l);
        return s.getScoreOfGoal(goalIndex);
    }

    public double getReward(int[][] stateFrom, int[][] stateTo, int goalIndex) {
        State s = states.get(Utilities.GameFieldHashCode(stateFrom));
        return s.getReward(stateTo, goalIndex);
    }

    public int getSmallestStepForWinningComb(int goal) {
        int min = Integer.MAX_VALUE;
        for (State s : states.values())
            if (s.getScoreOfGoal(goal) == Goal.MAX_SCORE)
                if (s.getShortestDistanceFromRoot() < min)
                    min = s.getShortestDistanceFromRoot();
        return min;
    }

    public void calculateDistancesFromRoot() {
        HashSet<State> s = new HashSet<>();
        HashSet<State> q = new HashSet<>();
        HashMap<State, Integer> dist = new HashMap<>();
        for (State state : states.values()) {
            q.add(state);
            dist.put(state, Integer.MAX_VALUE);
        }
        dist.put(root, 0);
        while (!q.isEmpty()) {
            int min = Integer.MAX_VALUE;
            State selected = null;
            for (State state : q)
                if (!s.contains(state) && min > dist.get(state)) {
                    min = dist.get(state);
                    selected = state;
                }
            q.remove(selected);
            s.add(selected);
            for (State adjacent : selected.getTos()) {
                if (dist.get(adjacent) > dist.get(selected) + 1)
                    dist.put(adjacent, dist.get(selected) + 1);
            }
        }

        for (State state : dist.keySet())
            state.testShortestDistanceFromRoot(dist.get(state));
    }
}
