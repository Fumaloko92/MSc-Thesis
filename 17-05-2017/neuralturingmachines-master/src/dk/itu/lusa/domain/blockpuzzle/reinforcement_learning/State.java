package dk.itu.lusa.domain.blockpuzzle.reinforcement_learning;

import com.ojcoleman.ahni.util.ArrayUtil;
import dk.itu.lusa.domain.blockpuzzle.BlockPuzzleInitializer;
import dk.itu.lusa.domain.blockpuzzle.Goal;
import dk.itu.lusa.domain.blockpuzzle.Utilities;

import java.util.HashSet;

/**
 * Created by lucas on 03/04/2017.
 */
public class State {
    private HashSet<State> tos;
    private HashSet<State> froms;
    private double[] scores;
    private int[][] currentState;
    private StateMap belongedMap;
    private boolean[] evaluated;
    private HashSet<Long> explored;
    private static HashSet<Long> visited;
    private int shortestDistanceFromRoot;
    private int[] shortestDistanceFromGoals;

    public State(int[][] state, StateMap owner) {
        scores = null;
        currentState = new int[state.length][];
        currentState = Utilities.CopyMatrix(state);
        tos = new HashSet<>();
        froms = new HashSet<>();
        belongedMap = owner;
        explored = new HashSet<>();
        shortestDistanceFromRoot = Integer.MAX_VALUE;
        //shortestDistanceFromGoals = new int[BlockPuzzleInitializer.GetNumberOfGoalsPerStateMap(belongedMap.getRoot().getCurrentState())];
    }

    public HashSet<State> getTos() {
        return tos;
    }

    public void connectTo(State s) {
        tos.add(s);
        s.froms.add(this);
    }

    public void explore(Long h) {
        explored.add(h);
    }

    public boolean isExplored(Long h) {
        return explored.contains(h);
    }

    public void connectFrom(State s) {
        s.tos.add(this);
        froms.add(s);
    }

    public int[][] getCurrentState() {
        return currentState;
    }

    @Override
    public boolean equals(Object s) {
        if (!(s instanceof State))
            return false;
        State state = (State) s;
        if (currentState.length != state.currentState.length)
            return false;
        for (int i = 0; i < currentState.length; i++) {
            if (currentState[i].length != state.currentState[i].length)
                return false;
            for (int k = 0; k < currentState[i].length; k++)
                if (currentState[i][k] != state.currentState[i][k])
                    return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(Utilities.GameFieldHashCode(currentState));
    }

    public double getScoreOfGoal(int index) {
        if (scores == null)
            return 0;
        return scores[index];
    }

    public void evaluate(int goalIndex) {
        if (scores == null) {
            int nGoals = BlockPuzzleInitializer.GetNumberOfGoalsPerStateMap(belongedMap.getRoot().getCurrentState());
            scores = ArrayUtil.newArray(nGoals, Double.MIN_VALUE);
            evaluated = new boolean[nGoals];
            for (int i = 0; i < evaluated.length; i++)
                evaluated[i] = false;
        }

            if (!evaluated[goalIndex]) {
                scores[goalIndex] = BlockPuzzleInitializer.GetGoalsOf(belongedMap.getRoot().getCurrentState()).get(goalIndex).getScore(currentState);
                evaluated[goalIndex] = true;
                if (!tos.isEmpty() && scores[goalIndex] < Goal.MAX_SCORE) {
                    double t[] = new double[tos.size()];
                    int k = 0;
                    for (State s : tos) {
                        if (s.evaluated == null || !s.evaluated[goalIndex])
                            s.evaluate(goalIndex);
                        t[k] = s.getScoreOfGoal(goalIndex);
                        k++;
                    }
                    if (StateMap.TD_ENABLED) {
                        scores[goalIndex] += StateMap.LEARNING_RATE * (StateMap.DISCOUNT_FACTOR * ArrayUtil.getMaxValue(t) - scores[goalIndex]);
                        scores[goalIndex] = scores[goalIndex] > Goal.MAX_SCORE ? Goal.MAX_SCORE : scores[goalIndex];
                    }
                }

            }

    }

    public double getReward(int[][] stateTo, int goalIndex) {
        if (tos.isEmpty())
            return 0;
        double maxScore = -1;
        int maxIndex = -1;
        int toIndex = -1;
        double toScore = 0;
        int index = 0;
        for (State s : tos) {
            if (maxScore < s.getScoreOfGoal(goalIndex)) {
                maxScore = s.getScoreOfGoal(goalIndex);
                maxIndex = index;
            }

            if (Utilities.GameFieldHashCode(s.getCurrentState()) == Utilities.GameFieldHashCode(stateTo)) {
                toIndex = index;
                toScore = s.getScoreOfGoal(goalIndex);
            }
            index++;
        }
        if (maxIndex != -1 && toIndex != -1) {
            if (maxIndex != toIndex)
                return (maxScore - toScore) / Goal.MAX_SCORE * BlockPuzzleInitializer.GetGoalsOf(belongedMap.getRoot().getCurrentState()).get(goalIndex).getScorePerConstraint();
        } else
            System.out.println("Houston we have a problem");

        return 1;
    }

    @Override
    public String toString() {
        String gameField = "";
        for (int i = currentState.length - 1; i >= 0; i--) {
            for (int k = 0; k < currentState.length; k++)
                if (this.currentState[i][k] != 0)
                    gameField += this.currentState[i][k] + " ";
                else
                    gameField += "  ";
            gameField += System.lineSeparator();
        }
        return gameField;
    }

    public String serialization() {
        String gameField = "";
        for (int i = 0; i < currentState.length; i++) {
            for (int k = 0; k < currentState.length - 1; k++)
                if (this.currentState[i][k] != 0)
                    gameField += this.currentState[i][k] + ",";
                else
                    gameField += "0,";
            gameField += this.currentState[i][currentState.length - 1];
            gameField += ";";
        }
        return gameField;
    }

    public void resetEvaluated() {
        evaluated = new boolean[BlockPuzzleInitializer.GetNumberOfGoalsPerStateMap(belongedMap.getRoot().getCurrentState())];
        for (int i = 0; i < evaluated.length; i++)
            evaluated[i] = false;
    }

    public int getShortestDistanceFromRoot() {
        return shortestDistanceFromRoot;
    }

    public void testShortestDistanceFromRoot(int v) {
        if (shortestDistanceFromRoot > v)
            shortestDistanceFromRoot = v;
    }

    public int getStepsToRoot() {
        visited = new HashSet<>();
        return stepsToRoot(0);
    }

    public void calculateStepsToGoals()
    {

    }

    private int stepsToRoot(int depth) {
        if (Utilities.MatrixEqual(belongedMap.getRoot().getCurrentState(), currentState))
            return depth;
        visited.add(Utilities.GameFieldHashCode(currentState));
        int min = Integer.MAX_VALUE;
        for (State from : froms) {
            if (!visited.contains(Utilities.GameFieldHashCode(from.getCurrentState()))) {
                int m = from.stepsToRoot(depth + 1);
                if (m < min)
                    min = m;
            }
        }
        return min;
    }
}
