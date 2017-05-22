package dk.itu.lusa.turing;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import com.anji.util.Properties;

import dk.itu.ejuuragr.fitness.Utilities;
import dk.itu.ejuuragr.replay.Replayable;
import dk.itu.ejuuragr.replay.TuringTimeStep;
import dk.itu.lusa.turing.GravesTMWithTemporalLinks.GravesTMWithTemporalLinksTimeStep;
import dk.itu.ejuuragr.turing.TuringMachine;

import javax.rmi.CORBA.Util;

/**
 * Our simplified version of a Turing Machine for
 * use with a neural network. It uses the general
 * TuringMachine interface so can be used in the
 * same contexts as the GravesTuringMachine.
 *
 * @author Emil
 */
public class GravesTMWithTemporalLinks implements TuringMachine, Replayable<GravesTMWithTemporalLinksTimeStep> {

    private static final boolean DEBUG = false;


    private int lastRead;

    private LinkedList<MemoryLocation> tape;
    private int[] pointers;
    private int m;
    private int n;
    private int shiftLength;
    private String shiftMode;
    private boolean enabled;
    private int heads;
    private String temp;
    private boolean recordTimeSteps = false;
    private GravesTMWithTemporalLinksTimeStep lastTimeStep;
    private GravesTMWithTemporalLinksTimeStep internalLastTimeStep;
    private boolean increasedSizeDown = false;
    private int zeroPosition = 0;

    private double[][] initialRead;

    public GravesTMWithTemporalLinks(Properties props) {
        this.m = props.getIntProperty("tm.m");
        this.n = props.getIntProperty("tm.n", -1);
        this.shiftLength = props.getIntProperty("tm.shift.length");
        this.shiftMode = props.getProperty("tm.shift.mode", "multiple");
        this.enabled = props.getBooleanProperty("tm.enabled", true);
        this.heads = props.getIntProperty("tm.heads.readwrite", 1);

        tape = new LinkedList<MemoryLocation>();
        this.reset();
        initialRead = new double[heads][];
        for (int i = 0; i < heads; i++) {
            initialRead[i] = getRead(i);
        }

    }

    @Override
    public void reset() {
        tape.clear();
        tape.add(new MemoryLocation(m));
        pointers = new int[heads];
        lastRead = -1;
        if (recordTimeSteps) {
            internalLastTimeStep = new GravesTMWithTemporalLinksTimeStep(new double[m], 0, 0, new double[shiftLength], new double[m], 0, 0, 0, 0, 0, 0);
            lastTimeStep = new GravesTMWithTemporalLinksTimeStep(new double[m], 0, 0, new double[shiftLength], new double[m], 0, 0, 0, 0, 0, 0);
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
        double[] f = new double[heads];
        double[] c = new double[heads];
        double[] b = new double[heads];
        ReadMode[] m = new ReadMode[heads];
        double[][] shifts = new double[heads][];


        // First all writes
        for (int i = 0; i < heads; i++) {
            // Should be M + 2 + S elements
            writeKeys[i] = take(queue, this.m);
            interps[i] = queue.poll();
            contents[i] = queue.poll();
            f[i] = queue.poll();
            c[i] = queue.poll();
            b[i] = queue.poll();
            if (!indexAllowed(lastRead))
                m[i] = ReadMode.Content;
            else {
                if (f[i] > c[i] && f[i] > b[i])
                    m[i] = ReadMode.Forward;
                else {
                    if (b[i] > c[i] && b[i] > c[i])
                        m[i] = ReadMode.Backward;
                    else
                        m[i] = ReadMode.Content;
                }
            }
            shifts[i] = take(queue, getShiftInputs());

            if (DEBUG) {
                System.out.println("------------------- MINIMAL TURING MACHINE (HEAD " + (i + 1) + ") -------------------");
                System.out.println("Write=" + Utilities.toString(writeKeys[i], "%.4f") + " Interp=" + interps[i]);
                System.out.println("Content=" + contents[i] + " Shift=" + Utilities.toString(shifts[i], "%.4f"));
                System.out.println("Forward Mode=" + f[i] + " Content Mode=" + c[i] + " Backward Mode:" + b[i]);
            }

            temp = write(i, writeKeys[i], interps[i]);
        }
        int todel = 4;
        // Perform content jump
        for (int i = 0; i < heads; i++) {
            if (m[i] == ReadMode.Content)
                performContentJump(i, contents[i], writeKeys[i]);
        }


        // Shift and read (no interaction)
        for (int i = 0; i < heads; i++) {
            int writePosition = pointers[i];
            increasedSizeDown = false;
            int t = -1;
            if (m[i] == ReadMode.Forward)
                t = tape.get(lastRead).f;
            else if (m[i] == ReadMode.Backward)
                t = tape.get(lastRead).b;

            if (!indexAllowed(t))
                moveHead(i, shifts[i]);
            else
                pointers[i] = t;

            double[] headResult = getRead(i);
            result[i] = headResult;

            if (recordTimeSteps) {
                int readPosition = pointers[i];
                int correctedWritePosition = writePosition - zeroPosition;

                if (increasedSizeDown) {
                    writePosition++;
                    zeroPosition++;
                }
                int correctedReadPosition = readPosition - zeroPosition;
                lastTimeStep = new GravesTMWithTemporalLinksTimeStep(writeKeys[i], interps[i], contents[i], shifts[i], headResult, writePosition, readPosition, zeroPosition, zeroPosition, correctedWritePosition, correctedReadPosition);
                //				                                               (double[] key, double write, double jump, double[] shift, double[] read, int writePosition, int readPosition, int writeZeroPosition, int readZeroPosition  , int correctedWritePosition, int correctedReadPosition){

                //				correctedReadPosition = readPosition - zeroPosition;
                lastTimeStep = new GravesTMWithTemporalLinksTimeStep(writeKeys[i], interps[i], contents[i], shifts[i], headResult, writePosition, readPosition, zeroPosition, zeroPosition, correctedWritePosition, correctedReadPosition);
                internalLastTimeStep = new GravesTMWithTemporalLinksTimeStep(writeKeys[i], interps[i], contents[i], shifts[i], headResult, writePosition, readPosition, zeroPosition, zeroPosition, correctedWritePosition, correctedReadPosition);
            }
        }

        if (DEBUG) {
            printState();
            System.out.println("Sending to NN: " + Utilities.toString(result, "%.4f"));
            System.out.println("--------------------------------------------------------------");
        }
//		return new double[1][result.length];
        return result;
    }

    public GravesTMWithTemporalLinksTimeStep getLastTimeStep() {
        return lastTimeStep;
    }


    @Override
    public void setRecordTimeSteps(boolean setRecordTimeSteps) {
        recordTimeSteps = setRecordTimeSteps;
    }

    @Override
    public GravesTMWithTemporalLinksTimeStep getInitialTimeStep() {
        return lastTimeStep = new GravesTMWithTemporalLinksTimeStep(new double[m], 0, 0, new double[shiftLength], new double[m], 0, 0, 0, 0, 0, 0);
    }


    public static class GravesTMWithTemporalLinksTimeStep implements TuringTimeStep {
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

        public GravesTMWithTemporalLinksTimeStep(double[] key, double write, double jump, double[] shift, double[] read, int writePosition, int readPosition, int writeZeroPosition, int readZeroPosition, int correctedWritePosition, int correctedReadPosition) {
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

    private class MemoryLocation implements Cloneable {
        public double timeErosion = 0.95;
        private double[] memory;
        //precedence factor
        public double p;
        private double[] futureMemory;
        private double[] pastMemory;
        private boolean isEmpty;
        public int f;
        public int b;

        public MemoryLocation(int m) {
            memory = new double[m];
            futureMemory = new double[m];
            pastMemory = new double[m];
            p = 1;
            f = -1;
            b = -1;
            isEmpty = true;
        }

        public MemoryLocation(double[] memory) {
            setMemory(memory);
            futureMemory = new double[memory.length];
            pastMemory = new double[memory.length];
            p = 1;
            f = -1;
            b = -1;
            isEmpty = false;
        }

        public MemoryLocation clone() {
            MemoryLocation newLoc = new MemoryLocation(memory);
            newLoc.setFutureMemory(futureMemory);
            newLoc.setPastMemory(pastMemory);
            newLoc.p = p;
            newLoc.f = f;
            newLoc.b = b;
            return newLoc;
        }

        public void erodeMemory() {
            p *= timeErosion;
        }

        private double[] deepCopy(double[] v) {
            double[] c = new double[v.length];
            for (int i = 0; i < v.length; i++)
                c[i] = v[i];
            return c;
        }

        public double[] getMemory() {
            return memory;
        }

        public double[] getPastMemory() {
            return pastMemory;
        }

        public double[] getFutureMemory() {
            return futureMemory;
        }

        public void setMemory(double[] memory) {
            this.memory = deepCopy(memory);
            isEmpty = false;
        }

        public void setFutureMemory(double[] futureMemory) {
            this.futureMemory = deepCopy(futureMemory);
            isEmpty = false;
        }

        public void setPastMemory(double[] pastMemory) {
            this.pastMemory = deepCopy(pastMemory);
            isEmpty = false;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public String toString() {
            return Utilities.toString(memory) + " f:" + f + " b:" + b;
        }

        public void resetFutureMemory() {
            f = -1;
            futureMemory = new double[futureMemory.length];
        }

        public void resetPastMemory() {
            b = -1;
            pastMemory = new double[pastMemory.length];
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
        return this.heads * (this.m + 2 +  getModeCount() + getShiftInputs());
    }

    @Override
    public int getOutputCount() {
        return this.m * this.heads;
    }

    @Override
    public double[][] getTapeValues() {
        LinkedList<double[]> tapeMemory = new LinkedList<>();
        for (int i = 0; i < tape.size(); i++) {
            tapeMemory.add(tape.get(i).deepCopy(tape.get(i).memory));
        }
        return Utilities.deepCopy(tapeMemory.toArray(new double[tape.size()][]));
    }

    @Override
    public String toString() {
        LinkedList<double[]> tapeMemory = new LinkedList<>();
        for (int i = 0; i < tape.size(); i++) {
            tapeMemory.add(tape.get(i).deepCopy(tape.get(i).memory));
        }
        StringBuilder b = new StringBuilder();
        b.append(Utilities.toString(tapeMemory.toArray(new double[tape.size()][])));
        b.append("\n");
        b.append("Pointers=");
        b.append(Arrays.toString(pointers));
        return b.toString();
    }

    // PRIVATE HELPER METHODS

    private enum ReadMode {
        Forward, Content, Backward
    }


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
        LinkedList<double[]> tapeMemory = new LinkedList<>();
        for (int i = 0; i < tape.size(); i++) {
            tapeMemory.add(tape.get(i).deepCopy(tape.get(i).memory));
        }
        System.out.println("TM: " + Utilities.toString(tapeMemory.toArray(new double[tape.size()][])) + " pointers=" + Arrays.toString(pointers));
    }

    /**
     * When writing a new memorylocation different situations can occur:
     * 1) We write on a freshly created memory location;
     * 2) We write on a non-empty memory location. In such case different situations can occur:
     * 2a) The forward value of the old location is not set;
     * 2b) The backward value of the old location is not set;
     * 2c) The forward and the backward value of the old location are not set;
     * 2d) The forward value and the backward value are set. In such case different situations can occur:
     * 2da) The last read position is different from the current writing position
     * 2db) The last read position is equal to the current writing position
     */
    private String write(int head, double[] content, double interp) {
        String code = "";
        MemoryLocation currLoc = tape.get(pointers[head]);
        MemoryLocation newLoc = new MemoryLocation(m);
        newLoc.setMemory(interpolate(content, tape.get(pointers[head]).getMemory(), interp));
        if (!currLoc.isEmpty()) //Writing in a non-empty location
        {
            code = "1";
            if (lastRead != -1) // The last timestep something was read
            {
                MemoryLocation last = tape.get(lastRead);
                code = "2";
                if (lastRead != pointers[head]) // Not writing in the last read location
                {
                    last.f = pointers[head];
                    last.setFutureMemory(newLoc.getMemory());
                    newLoc.b = lastRead;
                    newLoc.setPastMemory(newLoc.getMemory());
                    tape.set(lastRead, last);
                    code = "3";
                    if (currLoc.b != -1 && currLoc.f == -1) // The location that will be overridden has a forward element
                    {
                        MemoryLocation bCurrLoc = tape.get(currLoc.b);
                        bCurrLoc.resetFutureMemory();
                        tape.set(currLoc.b, bCurrLoc);
                        code = "3a";
                    }
                    if (currLoc.b == -1 && currLoc.f != -1) // The location that will be overridden has a backward element
                    {
                        MemoryLocation fCurrLoc = tape.get(currLoc.f);
                        fCurrLoc.resetPastMemory();
                        tape.set(currLoc.f, fCurrLoc);
                        code = "3b";
                    }
                    if (currLoc.b != -1 && currLoc.f != -1) // The location that will be overridden has a forward and backward element
                    {
                        MemoryLocation bCurrLoc = tape.get(currLoc.b);
                        MemoryLocation fCurrLoc = tape.get(currLoc.f);
                        bCurrLoc.f = currLoc.f;
                        bCurrLoc.setFutureMemory(fCurrLoc.getMemory());
                        fCurrLoc.b = currLoc.b;
                        fCurrLoc.setPastMemory(bCurrLoc.getMemory());
                        tape.set(currLoc.b, bCurrLoc);
                        tape.set(currLoc.f, fCurrLoc);
                        code = "3c";
                    }
                } else    // Writing in the last read location
                {
                    if (last.b != -1 && last.f == -1)    // Has an element backward
                    {
                        MemoryLocation bLast = tape.get(last.b);
                        bLast.setFutureMemory(newLoc.getMemory());
                        newLoc.b = last.b;
                        newLoc.setPastMemory(last.getPastMemory());
                        tape.set(last.b, bLast);
                        code = "2a";
                    }
                    if (last.b == -1 && last.f != -1) // Has an element forward
                    {
                        MemoryLocation fLast = tape.get(last.f);
                        fLast.setPastMemory(newLoc.getMemory());
                        newLoc.f = last.f;
                        newLoc.setFutureMemory(last.getFutureMemory());
                        tape.set(last.f, fLast);
                        code = "2b";
                    }
                    if (last.b != -1 && last.f != -1) // Has an element backward and forward
                    {
                        MemoryLocation fLast = tape.get(last.f);
                        fLast.setPastMemory(newLoc.getMemory());
                        newLoc.f = last.f;
                        newLoc.setFutureMemory(last.getFutureMemory());
                        tape.set(last.f, fLast);

                        MemoryLocation bLast = tape.get(last.b);
                        bLast.setFutureMemory(newLoc.getMemory());
                        newLoc.b = last.b;
                        newLoc.setPastMemory(last.getPastMemory());
                        tape.set(last.b, bLast);
                        code = "2c";
                    }
                }
            }
        } else {
            if (lastRead != -1) {
                MemoryLocation last = tape.get(lastRead);
                last.f = pointers[head];
                last.setFutureMemory(newLoc.getMemory());
                newLoc.b = lastRead;
                newLoc.setPastMemory(last.getMemory());
                tape.set(lastRead, last);
            }
        }
        tape.set(pointers[head], newLoc);
        return code;
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
                double curSim = Utilities.emilarity(key, tape.get(i).getMemory());
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
                        tape.addLast(new MemoryLocation(m));
                    }
                }

            } else {
                if (this.n > 0 && tape.size() >= this.n) {
                    pointers[head] = tape.size() - 1;
                } else {
                    pointers[head] = pointers[head] - 1;
                    if (pointers[head] < 0) {
                        tape.addFirst(new MemoryLocation(m));
                        pointers[head] = 0;

                        // Moving all other heads accordingly
                        for (int i = 0; i < heads; i++) {
                            if (i != head) {
                                pointers[i] = pointers[i] + 1;
                            }
                        }
                        for (int i = 0; i < tape.size(); i++) {
                            MemoryLocation m = tape.get(i);
                            if (m.f != -1)
                                m.f++;
                            if (m.b != -1)
                                m.b++;
                            tape.set(i, m);
                        }
                        lastRead++;
                        increasedSizeDown = true;
                    }
                }

            }

            offset = offset > 0 ? offset - 1 : offset + 1; // Go closer to 0
        }
    }

    private double[] getRead(int head) {
        lastRead = pointers[head];
        if (increasedSizeDown)
            lastRead++;
        return tape.get(pointers[head]).getMemory().clone();
    }

    private boolean indexAllowed(int index) {
        return index >= 0 && index < tape.size();
    }

    private int getModeCount()
    {
        //Forward, Content-Lookup and Backward
        return 3;
    }
}
