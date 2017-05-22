package dk.itu.lusa.domain.blockpuzzle;

import com.ojcoleman.ahni.util.ArrayUtil;

import java.util.LinkedList;

/**
 * Created by lucas on 22/03/2017.
 */
public class Goal {
    private int[][] referred_starting_comb;
    private LinkedList<Constraint> constraints;
    private double scorePerConstraint;
    public static final double MAX_SCORE = 100;
    public static final double ALLOWED_MAX_SCORE = 90;
    private int smallestStepsForWinningComb;

    Goal(LinkedList<Constraint> constraints, int[][] starting_comb) {
        this.constraints = constraints;
        scorePerConstraint = MAX_SCORE / (double) constraints.size();
        referred_starting_comb = starting_comb;
        smallestStepsForWinningComb = -1;
    }

    public void setSmallestStepsForWinningComb(int v) {
        smallestStepsForWinningComb = v;
    }

    public int getSmallestStepsForWinningComb() {
        return smallestStepsForWinningComb;
    }

    Constraint getConstraint(int index) {
        return constraints.get(index);
    }

    int getConstraintSize() {
        return constraints.size();
    }

    public double getScorePerConstraint() {
        return scorePerConstraint;
    }

    boolean isGoalSatisfied(int[][] boardRepresentation) {
        for (Constraint constraint : constraints) {
            if (!constraint.isSatisfied(boardRepresentation))
                return false;
        }
        return true;
    }

    double getScore(double[] boardRepresentation) {
        LinkedList<Constraint> constraints = new LinkedList<Constraint>();
        for (int i = 0; i < this.constraints.size(); i++)
            constraints.add(this.constraints.get(i).clone());

        for (int c = constraints.size() - 1; c >= 0; c--) {
            if (constraints.get(c).isSatisfied(boardRepresentation))
                constraints.remove(c);
        }
        return (this.constraints.size() - constraints.size()) * scorePerConstraint;
    }

    public double getScore(int[][] boardRepresentation) {
        LinkedList<Constraint> constraints = new LinkedList<Constraint>();
        for (int i = 0; i < this.constraints.size(); i++)
            constraints.add(this.constraints.get(i).clone());

        for (int c = constraints.size() - 1; c >= 0; c--) {
            if (constraints.get(c).isSatisfied(boardRepresentation))
                constraints.remove(c);
        }
        return (this.constraints.size() - constraints.size()) * scorePerConstraint;
    }

    double[] getRepresentation() {

        LinkedList<Goal> goals = BlockPuzzleInitializer.GetGoalsOf(referred_starting_comb);
        double[] r = ArrayUtil.newArray(goals.size(), 0.0);
        for (int i = 0; i < goals.size(); i++)
            if (equals(goals.get(i))) {
                if (!BlockPuzzle.SHORT_REPRESENTATION)
                    r[i] = 1;
                else {
                    r = new double[1];
                    r[0] = Math.pow(2, i);
                }
                break;
            }
        return r;
    }

    public boolean equals(Goal g) {
        if (getConstraintSize() != g.getConstraintSize())
            return false;
        for (int i = 0; i < constraints.size(); i++)
            if (!getConstraint(i).equals(g.getConstraint(i)))
                return false;
        return true;
    }

    public String toString() {
        String s = "";
        for (int i = 0; i < constraints.size() - 1; i++)
            s += constraints.get(i) + ",";
        s += constraints.get(constraints.size() - 1);
        return s;
    }

    public Object clone() {
        LinkedList<Constraint> c = new LinkedList<>();
        for (Constraint constraint : constraints)
            c.add(constraint.clone());
        return new Goal(c, Utilities.CopyMatrix(referred_starting_comb));
    }

    public int[][] getConstraintsInGameField() {
        int[][] repr = new int[BlockPuzzle.GRID_SIZE][];
        for (int i = 0; i < repr.length; i++)
            repr[i] = new int[BlockPuzzle.GRID_SIZE * constraints.size()];
        int c_index = 0;
        for (Constraint c : constraints) {
            int[][] c_repr = c.getConstraintInGameField();
            for (int i = 0; i < c_repr.length; i++)
                System.arraycopy(c_repr[i], 0, repr[i], c_index * BlockPuzzle.GRID_SIZE, c_repr.length);
            c_index++;
        }

        return repr;
    }
}
