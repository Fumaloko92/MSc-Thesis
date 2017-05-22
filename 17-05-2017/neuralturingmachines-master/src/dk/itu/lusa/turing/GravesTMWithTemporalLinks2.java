package dk.itu.lusa.turing;

import com.anji.util.Properties;
import com.ojcoleman.ahni.util.ArrayUtil;
import com.ojcoleman.ahni.util.DoubleVector;
import dk.itu.ejuuragr.fitness.Utilities;
import dk.itu.ejuuragr.replay.Replayable;
import dk.itu.ejuuragr.replay.TuringTimeStep;
import dk.itu.ejuuragr.turing.TuringMachine;
import dk.itu.lusa.domain.blockpuzzle.BlockPuzzle;
import dk.itu.lusa.turing.GravesTMWithTemporalLinks2.GravesTMWithTemporalLinks2TimeStep;
import dk.itu.lusa.turing.memory_screenshot.MemoryScreenshot;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Our simplified version of a Turing Machine for
 * use with a neural network. It uses the general
 * TuringMachine interface so can be used in the
 * same contexts as the GravesTuringMachine.
 *
 * @author Emil
 */
public class GravesTMWithTemporalLinks2 implements TuringMachine, Replayable<GravesTMWithTemporalLinks2TimeStep> {
    private static final boolean DEBUG = false;
    private TemporalLinkageSystem temporalLinkageSystem;
    private LinkedList<double[]> tape;
    private int[] pointers;
    private int m;
    private int n;
    private int shiftLength;
    private String shiftMode;
    private boolean enabled;
    private int heads;

    private boolean recordTimeSteps = false;
    private GravesTMWithTemporalLinks2TimeStep lastTimeStep;
    private GravesTMWithTemporalLinks2TimeStep internalLastTimeStep;
    private boolean increasedSizeDown = false;
    private int zeroPosition = 0;
    public int memoryScreenshotIndex;
    private double[][] initialRead;

    public GravesTMWithTemporalLinks2(Properties props) {
        this.m = props.getIntProperty("tm.m");
        this.n = props.getIntProperty("tm.n", -1);
        this.shiftLength = props.getIntProperty("tm.shift.length");
        this.shiftMode = props.getProperty("tm.shift.mode", "multiple");
        this.enabled = props.getBooleanProperty("tm.enabled", true);
        this.heads = props.getIntProperty("tm.heads.readwrite", 1);

        tape = new LinkedList<double[]>();

        this.reset();
        initialRead = new double[heads][];
        for (int i = 0; i < heads; i++) {
            initialRead[i] = getRead(i);
        }
    }

    @Override
    public void reset() {
        if (MemoryScreenshot.PRINT_MEMORY_SCREENSHOTS)
            memoryScreenshotIndex = MemoryScreenshot.CreateNewScreenshot();
        tape.clear();
        tape.add(new double[m]);
        pointers = new int[heads];
        temporalLinkageSystem = new TemporalLinkageSystem();
        if (recordTimeSteps) {
            internalLastTimeStep = new GravesTMWithTemporalLinks2TimeStep(new double[m], 0, 0, new double[shiftLength], new double[m], 0, 0, 0, 0, 0, 0);
            lastTimeStep = new GravesTMWithTemporalLinks2TimeStep(new double[m], 0, 0, new double[shiftLength], new double[m], 0, 0, 0, 0, 0, 0);
        }
        if (DEBUG) printState();
    }

    /**
     * Operation order:
     * write
     * jump
     * shift
     * read
     */
    @Override
    public double[][] processInput(double[] fromNN) {
        if (!enabled)
            return initialRead;

        Queue<Double> queue = new LinkedList<Double>();
        for (double d : fromNN) queue.add(d);

        double[][] result = new double[heads][];

        double[][] writeKeys = new double[heads][];
        double[] interps = new double[heads];
        double[] contents = new double[heads];
        double[] data_influence_ratio = new double[heads];
        double[][] modes = new double[heads][];
        double[][] shifts = new double[heads][];

        // First all writes
        for (int i = 0; i < heads; i++) {
            // Should be M + 2 + S elements
            writeKeys[i] = take(queue, this.m);
            interps[i] = queue.poll();
            contents[i] = queue.poll();
            modes[i] = take(queue, getModeInputs());
            data_influence_ratio[i] = queue.poll();
            shifts[i] = take(queue, getShiftInputs());

            if (DEBUG) {
                System.out.println("------------------- MINIMAL TURING MACHINE (HEAD " + (i + 1) + ") -------------------");
                System.out.println("Write=" + Utilities.toString(writeKeys[i], "%.4f") + " Interp=" + interps[i]);
                System.out.println("Content?=" + contents[i] + " Shift=" + Utilities.toString(shifts[i], "%.4f"));
            }

            write(i, writeKeys[i], interps[i], data_influence_ratio[i]);
        }
        int[] write_head = new int[pointers.length];
        System.arraycopy(pointers, 0, write_head, 0, pointers.length);
        int[] chosen_modes = new int[heads];
        for (int i = 0; i < heads; i++) {
            chosen_modes[i] = ArrayUtil.getMaxIndex(modes[i]);
        }

        // Shift and read (no interaction)
        for (int i = 0; i < heads; i++) {
            int writePosition = pointers[i];
            increasedSizeDown = false;
            if (chosen_modes[i] == 1) {
                performContentJump(i, contents[i], writeKeys[i]);
            } else if (chosen_modes[i] == 0) {
                pointers[i] = temporalLinkageSystem.getForwardOf(pointers[i]);
            } else {
                pointers[i] = temporalLinkageSystem.getBackwardOf(pointers[i]);
            }

            double[] headResult = getRead(i);
            int[] read_pointers = new int[pointers.length];
            System.arraycopy(pointers, 0, read_pointers, 0, pointers.length);

            moveHead(i, shifts[i]);
            if (increasedSizeDown)
                for (int k = 0; k < write_head.length; k++)
                    write_head[k]++;
            if (MemoryScreenshot.PRINT_MEMORY_SCREENSHOTS)
                MemoryScreenshot.AddMemoryToScreenshot(memoryScreenshotIndex, tape, write_head, read_pointers, pointers, modes, chosen_modes, temporalLinkageSystem.dynamicTemporalMatrix, temporalLinkageSystem.precedenceVector);
            result[i] = headResult;

            if (recordTimeSteps) {
                int readPosition = pointers[i];
                int correctedWritePosition = writePosition - zeroPosition;

                if (increasedSizeDown) {
                    writePosition++;
                    zeroPosition++;
                }
                int correctedReadPosition = readPosition - zeroPosition;
                lastTimeStep = new GravesTMWithTemporalLinks2TimeStep(writeKeys[i], interps[i], contents[i], shifts[i], headResult, writePosition, readPosition, zeroPosition, zeroPosition, correctedWritePosition, correctedReadPosition);
//				                                               (double[] key, double write, double jump, double[] shift, double[] read, int writePosition, int readPosition, int writeZeroPosition, int readZeroPosition  , int correctedWritePosition, int correctedReadPosition){

//				correctedReadPosition = readPosition - zeroPosition;
                lastTimeStep = new GravesTMWithTemporalLinks2TimeStep(writeKeys[i], interps[i], contents[i], shifts[i], headResult, writePosition, readPosition, zeroPosition, zeroPosition, correctedWritePosition, correctedReadPosition);
                internalLastTimeStep = new GravesTMWithTemporalLinks2TimeStep(writeKeys[i], interps[i], contents[i], shifts[i], headResult, writePosition, readPosition, zeroPosition, zeroPosition, correctedWritePosition, correctedReadPosition);
            }
        }

        if (DEBUG) {
            printState();
            System.out.println("Sending to NN: " + Utilities.toString(result, "%.4f"));
            System.out.println("--------------------------------------------------------------");
        }

        return result;
    }

    public GravesTMWithTemporalLinks2TimeStep getLastTimeStep() {
        return lastTimeStep;
    }


    @Override
    public void setRecordTimeSteps(boolean setRecordTimeSteps) {
        recordTimeSteps = setRecordTimeSteps;
    }

    @Override
    public GravesTMWithTemporalLinks2TimeStep getInitialTimeStep() {
        return lastTimeStep = new GravesTMWithTemporalLinks2TimeStep(new double[m], 0, 0, new double[shiftLength], new double[m], 0, 0, 0, 0, 0, 0);
    }


    public static class GravesTMWithTemporalLinks2TimeStep implements TuringTimeStep {
        public final double[] key;
        public final double writeInterpolation, contentJump;
        public final double[] shift;
        public final double[] read;
        public final int writePosition;
        public final int readPosition;
        public final int writeZeroPosition;
        public final int readZeroPosition;
        public final int correctedWritePosition;
        public final int correctedReadPosition;

        public GravesTMWithTemporalLinks2TimeStep(double[] key, double write, double jump, double[] shift, double[] read, int writePosition, int readPosition, int writeZeroPosition, int readZeroPosition, int correctedWritePosition, int correctedReadPosition) {
            this.key = key;
            writeInterpolation = write;
            contentJump = jump;
            this.shift = shift;
            this.read = read;
            this.writePosition = writePosition;
            this.readPosition = readPosition;
            this.writeZeroPosition = writeZeroPosition;
            this.readZeroPosition = readZeroPosition;
            this.correctedWritePosition = correctedWritePosition;
            this.correctedReadPosition = correctedReadPosition;
        }

    }

    @Override
    public double[][] getDefaultRead() {
        if (DEBUG) printState();
        return initialRead;
    }

    @Override
    public int getReadHeadCount() {
        return 1;
    }

    @Override
    public int getWriteHeadCount() {
        return 1;
    }

    @Override
    public int getInputCount() {
        // WriteKey, Interpolation, ToContentJump, Shift
        return this.heads * (this.m + 2 + getModeInputs() + getShiftInputs() +1);
    }

    @Override
    public int getOutputCount() {
        return this.m * this.heads;
    }

    @Override
    public double[][] getTapeValues() {
        return Utilities.deepCopy(tape.toArray(new double[tape.size()][]));
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(Utilities.toString(tape.toArray(new double[tape.size()][])));
        b.append("\n");
        b.append("Pointers=");
        b.append(Arrays.toString(pointers));
        return b.toString();
    }

    // PRIVATE HELPER METHODS

    private static double[] take(Queue<Double> coll, int amount) {
        double[] result = new double[amount];
        for (int i = 0; i < amount; i++)
            result[i] = coll.poll();
        return result;
    }

    private int getShiftInputs() {
        switch (shiftMode) {
            case "single":
                return 1;
            default:
                return this.shiftLength;
        }
    }

    private void printState() {
        System.out.println("TM: " + Utilities.toString(tape.toArray(new double[tape.size()][])) + " pointers=" + Arrays.toString(pointers));
    }

    private void write(int head, double[] content, double interp, double data_strength) {
        double[] toWrite = interpolate(content, tape.get(pointers[head]), interp);
        temporalLinkageSystem.writeOn(pointers[head], toWrite, interp, data_strength);
        tape.set(pointers[head], toWrite);
    }

    private double[] interpolate(double[] first, double[] second, double interp) {
        double[] result = new double[first.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = interp * first[i] + (1 - interp) * second[i];
        }
        return result;
    }

    private void performContentJump(int head, double contentJump, double[] key) {
        if (contentJump >= 0.5) {
            // JUMPING POINTER TO BEST MATCH
            int bestPos = 0;
            double similarity = -1d;
            for (int i = 0; i < tape.size(); i++) {
                double curSim = Utilities.emilarity(key, tape.get(i));
                if (DEBUG) System.out.println("Pos " + i + ": sim =" + curSim + (curSim > similarity ? " better" : ""));
                if (curSim > similarity) {
                    similarity = curSim;
                    bestPos = i;
                }
            }

            if (DEBUG) System.out.println("PERFORMING CONTENT JUMP! from " + this.pointers[head] + " to " + bestPos);

            this.pointers[head] = bestPos;

        }
    }

    private void moveHead(int head, double[] shift) {
        // SHIFTING
        int highest;
        switch (shiftMode) {
            case "single":
                highest = (int) (shift[0] * this.shiftLength);
                break; // single
            default:
                highest = Utilities.maxPos(shift);
                break; // multiple
        }

        int offset = highest - (this.shiftLength / 2);

//		System.out.println("Highest="+highest);
//		System.out.println("Offset="+offset);

        while (offset != 0) {
            if (offset > 0) {
                if (this.n > 0 && tape.size() >= this.n) {
                    pointers[head] = 0;
                } else {
                    pointers[head] = pointers[head] + 1;

                    if (pointers[head] >= tape.size()) {
                        tape.addLast(new double[this.m]);
                        temporalLinkageSystem.expandOn(false);
                    }
                }

            } else {
                if (this.n > 0 && tape.size() >= this.n) {
                    pointers[head] = tape.size() - 1;
                } else {
                    pointers[head] = pointers[head] - 1;
                    if (pointers[head] < 0) {
                        tape.addFirst(new double[this.m]);
                        pointers[head] = 0;

                        // Moving all other heads accordingly
                        for (int i = 0; i < heads; i++) {
                            if (i != head)
                                pointers[i] = pointers[i] + 1;
                        }
                        temporalLinkageSystem.expandOn(true);
                        increasedSizeDown = true;
                    }
                }

            }

            offset = offset > 0 ? offset - 1 : offset + 1; // Go closer to 0
        }
    }

    private double[] getRead(int head) {
        return tape.get(pointers[head]).clone();
    }

    private int getModeInputs() {
        return 3;
    }

    private class TemporalLinkageSystem {
        private final double EROSION_MULTIPLIER = 0.6;
        private LinkedList<LinkedList<Double>> dynamicTemporalMatrix;
        private LinkedList<Double> precedenceVector;
        private int lastWritten;
        private int size;


        TemporalLinkageSystem() {
            dynamicTemporalMatrix = new LinkedList<>();
            LinkedList<Double> starter = new LinkedList<>();
            precedenceVector = new LinkedList<>();
            starter.add(0.0);
            dynamicTemporalMatrix.add(starter);
            size = 1;
            lastWritten = -1;
            precedenceVector.add(0.0);
        }

        void expandOn(boolean true_if_head) {
            size++;
            LinkedList<Double> l = new LinkedList<>();
            for (int i = 0; i < size; i++)
                l.add(0.0);
            for (int i = 0; i < size - 1; i++) {
                LinkedList<Double> temp = dynamicTemporalMatrix.get(i);
                if (true_if_head)
                    temp.addFirst(0.0);
                else
                    temp.addLast(0.0);
                dynamicTemporalMatrix.set(i, temp);
            }
            if (true_if_head) {
                dynamicTemporalMatrix.addFirst(l);
                precedenceVector.addFirst(0.0);
                lastWritten++;
            } else {
                dynamicTemporalMatrix.addLast(l);
                precedenceVector.addLast(0.0);
            }
        }

      /*  private void erodeMemory() {
            for (int i = 0; i < dynamicTemporalMatrix.size(); i++)
                for (int k = 0; k < dynamicTemporalMatrix.get(i).size(); k++)
                    dynamicTemporalMatrix.get(i).set(k, dynamicTemporalMatrix.get(i).get(k) * EROSION_MULTIPLIER);
        }
*/

        private void updatePrecedenceVector(int write_position, double interp_gate, double data_influence_ratio) {
            for (int i = 0; i < precedenceVector.size(); i++) {
                double el = precedenceVector.get(i);
                el -= el*(1-data_influence_ratio)*(1-EROSION_MULTIPLIER);
                if (write_position == i) {
                    double diff = 1 - el;
                    el += diff * (1 - data_influence_ratio) + diff * data_influence_ratio * interp_gate;
                }
                precedenceVector.set(i, el);
            }
        }

        private void updateTemporalLinkMatrix() {
            for (int i = 0; i < dynamicTemporalMatrix.size(); i++)
                for (int k = 0; k < dynamicTemporalMatrix.size(); k++)
                    if (i != k)
                        dynamicTemporalMatrix.get(i).set(k, dynamicTemporalMatrix.get(i).get(k) * precedenceVector.get(i));
        }

        void writeOn(int write_position, double[] toWrite, double interp_gate, double data_influence_ratio) {
            updatePrecedenceVector(write_position, interp_gate, data_influence_ratio);
            if (lastWritten != -1) {
                LinkedList<Double> row;
                double p = precedenceVector.get(write_position);

                //erodeMemory();
                if (lastWritten != write_position) {
                    //Updates the last written location with a linkage to the currently written position
                    row = dynamicTemporalMatrix.get(lastWritten);
                    row.set(write_position, p);
                    dynamicTemporalMatrix.set(lastWritten, row);
                } else {
                    int b = getBackwardOf(lastWritten);
                    if (b != lastWritten) {
                        row = dynamicTemporalMatrix.get(b);
                        row.set(write_position, p);
                        dynamicTemporalMatrix.set(b, row);
                    }
                }
                double similarity = Utilities.cosineSimilarity(tape.get(write_position), toWrite);
                //Updates the current writing position links depending on the similarity between the old and the new values
                //This block updates all the links from the write_position to the rest of the memory
                row = dynamicTemporalMatrix.get(write_position);
                for (int i = 0; i < row.size(); i++)
                    if (i != write_position)
                        row.set(i, row.get(i)*similarity);

                dynamicTemporalMatrix.set(write_position, row);
                //This block updates all the links from the rest of the memory to the write position
                for (int i = 0; i < dynamicTemporalMatrix.size(); i++) {
                    if (i != write_position && i != lastWritten) {
                        row = dynamicTemporalMatrix.get(i);
                        row.set(write_position, row.get(write_position) * similarity);
                        dynamicTemporalMatrix.set(i, row);
                    }
                }
                updateTemporalLinkMatrix();
            }
            lastWritten = write_position;
        }

        int getForwardOf(int index) {
            double max = -1;
            int max_index = index;
            for (int i = 0; i < dynamicTemporalMatrix.get(index).size(); i++) {
                if (index != i && max < dynamicTemporalMatrix.get(index).get(i)) {
                    max = dynamicTemporalMatrix.get(index).get(i);
                    max_index = i;
                }
            }
            return max_index;
        }

        int getBackwardOf(int index) {
            double max = -1;
            int max_index = index;
            for (int i = 0; i < dynamicTemporalMatrix.size(); i++) {
                if (index != i && max < dynamicTemporalMatrix.get(i).get(index)) {
                    max = dynamicTemporalMatrix.get(i).get(index);
                    max_index = i;
                }
            }
            return max_index;
        }

        public String toString() {
            String r = "";
            for (LinkedList<Double> row : dynamicTemporalMatrix) {
                for (Double val : row)
                    r += val + " ";
                r += System.lineSeparator();
            }
            return r;
        }
    }

}
