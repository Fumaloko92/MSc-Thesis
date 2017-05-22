package dk.itu.lusa.domain.blockpuzzle;

import jdk.nashorn.internal.ir.Block;

/**
 * Created by lucas on 22/03/2017.
 */
public class Constraint implements Cloneable {
    private int block_1;
    private int block_2;
    private Adjacency adjacency;

    Constraint(int b1, int b2, Adjacency.Relation arrangement) {
        block_1 = b1;
        block_2 = b2;
        this.adjacency = new Adjacency(arrangement);
    }

    Constraint(String toParse){
        for(int i=0;i<toParse.length();i++)
        {
            if(Character.isLetter(toParse.charAt(i)))
            {
                block_1 = Integer.parseInt(toParse.substring(0,i));
                block_2 = Integer.parseInt(toParse.substring(i+1,toParse.length()));
                adjacency = new Adjacency(toParse.charAt(i));
            }
        }
    }

    boolean isSatisfied(int[][] boardRepresentation){
        for(int i=0;i<boardRepresentation.length;i++)
        {
            for(int k=0;k<boardRepresentation[i].length;k++)
            {
                if(boardRepresentation[i][k] == block_1)
                {
                    int b2 = 0;
                    switch (adjacency.getRelation())
                    {
                        case Left:
                            if(k+1 < boardRepresentation[i].length)
                            b2 = boardRepresentation[i][k+1];
                            break;
                        case Right:
                            if(k-1>=0)
                            b2 = boardRepresentation[i][k-1];
                            break;
                        case Above:
                            if(i-1 >=0)
                            b2 = boardRepresentation[i-1][k];
                            break;
                        case Below:
                            if(i+1 <boardRepresentation.length)
                            b2 = boardRepresentation[i+1][k];
                            break;
                    }
                    return b2 == block_2;
                }
            }
        }
        return false;
    }

    boolean isSatisfied(double[] boardRepresentation) {
        boolean shortRepresentation = BlockPuzzle.SHORT_REPRESENTATION;
        double[] block_repr_1 = Utilities.getBlockRepresentation(block_1), block_repr_2 = Utilities.getBlockRepresentation(block_2);
        int step;
        if (!shortRepresentation)
            step = BlockPuzzle.BLOCK_NUMBER;
        else
            step = 1;
        for (int i = 0; i < boardRepresentation.length; i += step) {
            double[] currentBlock_1;
            if (!shortRepresentation) {
                currentBlock_1 = new double[BlockPuzzle.BLOCK_NUMBER];
                System.arraycopy(boardRepresentation, i, currentBlock_1, 0, BlockPuzzle.BLOCK_NUMBER);
            } else {
                currentBlock_1 = new double[1];
                System.arraycopy(boardRepresentation, i, currentBlock_1, 0, 1);
            }
            if (Utilities.blocksEqual(currentBlock_1, block_repr_1)) {
                double[] currentBlock_2;
                if (!shortRepresentation)
                    currentBlock_2 = new double[BlockPuzzle.BLOCK_NUMBER];
                else
                    currentBlock_2 = new double[1];
                switch (adjacency.getRelation()) {
                    case Left:
                        if (i + BlockPuzzle.BLOCK_NUMBER < boardRepresentation.length)
                            if (!shortRepresentation)
                                System.arraycopy(boardRepresentation, i + BlockPuzzle.BLOCK_NUMBER, currentBlock_2, 0, BlockPuzzle.BLOCK_NUMBER);
                            else
                                System.arraycopy(boardRepresentation, i + 1, currentBlock_2, 0, 1);
                        break;
                    case Right:
                        if (i - BlockPuzzle.BLOCK_NUMBER >= 0)
                            if (!shortRepresentation)
                                System.arraycopy(boardRepresentation, i - BlockPuzzle.BLOCK_NUMBER, currentBlock_2, 0, BlockPuzzle.BLOCK_NUMBER);
                            else
                                System.arraycopy(boardRepresentation, i - 1, currentBlock_2, 0, 1);
                        break;
                    case Below:
                        if (i + BlockPuzzle.BLOCK_NUMBER * BlockPuzzle.GRID_SIZE < boardRepresentation.length)
                            if (!shortRepresentation)
                                System.arraycopy(boardRepresentation, i + BlockPuzzle.BLOCK_NUMBER * BlockPuzzle.GRID_SIZE, currentBlock_2, 0, BlockPuzzle.BLOCK_NUMBER);
                            else
                                System.arraycopy(boardRepresentation, i + BlockPuzzle.GRID_SIZE, currentBlock_2, 0, 1);
                        break;
                    case Above:
                        if (i - BlockPuzzle.BLOCK_NUMBER * BlockPuzzle.GRID_SIZE >= 0)
                            if (!shortRepresentation)
                                System.arraycopy(boardRepresentation, i - BlockPuzzle.BLOCK_NUMBER * BlockPuzzle.GRID_SIZE, currentBlock_2, 0, BlockPuzzle.BLOCK_NUMBER);
                            else
                                System.arraycopy(boardRepresentation, i - BlockPuzzle.GRID_SIZE, currentBlock_2, 0, 1);
                        break;
                }
                return (Utilities.blocksEqual(currentBlock_2, block_repr_2));
            }
        }
        return false;
    }

    @Override
    public Constraint clone() {
        return new Constraint(block_1, block_2, adjacency.getRelation());
    }

    public double[] getRepresentation() {
        double[] b1 = Utilities.getBlockRepresentation(block_1), b2 = Utilities.getBlockRepresentation(block_2), c = adjacency.getRepresentation();
        double[] r = new double[b1.length + b2.length + c.length];
        System.arraycopy(b1, 0, r, 0, b1.length);
        System.arraycopy(c, 0, r, b1.length, c.length);
        System.arraycopy(b2, 0, r, b1.length + c.length, b2.length);
        return r;
    }

    public boolean equals(Object o) {
        if(!(o instanceof Constraint))
            return false;
        Constraint c = (Constraint)o;
        if(block_1 == c.block_1 && block_2 == c.block_2 && adjacency.equals(c.adjacency))
            return true;
        if(block_1 == c.block_2 && block_2 == c.block_1)
        {
            switch (adjacency.getRelation())
            {
                case Left:
                    return c.adjacency.getRelation() == Adjacency.Relation.Right;
                case Right:
                    return c.adjacency.getRelation() == Adjacency.Relation.Left;
                case Above:
                    return c.adjacency.getRelation() == Adjacency.Relation.Below;
                case Below:
                    return c.adjacency.getRelation() == Adjacency.Relation.Above;
            }
        }
        return false;
    }

    public String toString()
    {
        String s="";
        s+= block_1;
        switch (adjacency.getRelation())
        {
            case Above:
                s+="a";
                break;
            case Right:
                s+="r";
                break;
            case Below:
                s+="b";
                break;
            case Left:
                s+="l";
                break;
        }
        s+=block_2;
        return s;
    }

    public int[][] getConstraintInGameField()
    {
        int[][] gameField = new int[BlockPuzzle.GRID_SIZE][];
        for(int i=0;i<gameField.length;i++)
            gameField[i] = new int[BlockPuzzle.GRID_SIZE];
        switch (adjacency.getRelation())
        {
            case Above:
                gameField[1][BlockPuzzle.GRID_SIZE/2] = block_1;
                gameField[0][BlockPuzzle.GRID_SIZE/2] = block_2;
                break;
            case Right:
                gameField[0][1] = block_1;
                gameField[0][0] = block_2;
                break;
            case Left:
                gameField[0][0] = block_1;
                gameField[0][1] = block_2;
                break;
            case Below:
                gameField[0][BlockPuzzle.GRID_SIZE/2] = block_1;
                gameField[1][BlockPuzzle.GRID_SIZE/2] = block_2;
                break;
        }
        return gameField;
    }
}
