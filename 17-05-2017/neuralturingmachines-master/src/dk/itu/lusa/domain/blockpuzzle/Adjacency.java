package dk.itu.lusa.domain.blockpuzzle;

import com.ojcoleman.ahni.util.ArrayUtil;

/**
 * Created by lucas on 22/03/2017.
 */

public class Adjacency {
    private Relation relation;

    public Adjacency(Relation rel) {
        relation = rel;
    }

    public Adjacency(Character c)
    {
        switch (c)
        {
            case 'b':
                relation = Relation.Below;
                break;
            case 'a':
                relation = Relation.Above;
                break;
            case 'l':
                relation = Relation.Left;
                break;
            case 'r':
                relation = Relation.Right;
                break;
        }
    }

    public enum Relation {
        Left, Right, Above, Below
    }

    public Relation getRelation() {
        return relation;
    }

    public double[] getRepresentation() {
        if (!BlockPuzzle.SHORT_REPRESENTATION) {
            switch (relation) {
                case Left:
                    return new double[]{1, 0, 0, 0};
                case Right:
                    return new double[]{0, 1, 0, 0};
                case Above:
                    return new double[]{0, 0, 1, 0};
                case Below:
                    return new double[]{0, 0, 0, 1};
            }
            return ArrayUtil.newArray(Relation.values().length, 0.0);
        } else {
            switch (relation) {
                case Left:
                    return new double[]{Math.pow(3, 1)};
                case Right:
                    return new double[]{Math.pow(3, 2)};
                case Above:
                    return new double[]{Math.pow(3, 3)};
                case Below:
                    return new double[]{Math.pow(3, 4)};
            }
            return ArrayUtil.newArray(1, 0.0);
        }
    }

    public boolean equals(Object a) {
        return (a instanceof  Adjacency) && relation == ((Adjacency)a).relation;
    }
}