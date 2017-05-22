package dk.itu.lusa.turing;

import com.anji.util.Properties;
import dk.itu.ejuuragr.domain.Simulator;
import dk.itu.ejuuragr.domain.tmaze.RoundsTMaze;
import dk.itu.ejuuragr.fitness.BaseController;
import dk.itu.ejuuragr.fitness.Utilities;
import dk.itu.ejuuragr.replay.StepSimulator;
import dk.itu.ejuuragr.turing.TuringMachine;
import com.anji.integration.Activator;
import dk.itu.lusa.domain.blockpuzzle.BlockPuzzle;
import dk.itu.lusa.domain.blockpuzzle.Goal;
import dk.itu.lusa.domain.blockpuzzle.screenshot.SimulationScreenshot;
import dk.itu.lusa.domain.blockpuzzle.screenshot.SimulationsScreenshot;
import dk.itu.lusa.turing.memory_screenshot.MemoryScreenshot;

import java.util.Arrays;

/**
 * A Controller which adds a Turing Machine to the
 * simulation and uses outputs from the NN to to TM (write)
 * and gives its output (read) to the inputs of the NN.
 * This way the NN has the capability to save data in
 * memory.
 *
 * @author Emil
 */
public class TuringControllerRepeater extends BaseController implements RoundsTMaze.RestartListener {

    protected TuringMachine tm;


    /**
     * The required constructor for instantiation via reflection
     * from the properties file.
     *
     * @param props The properties where it can read everything.
     * @param sim   The simulator of the domain to behave in.
     */
    public TuringControllerRepeater(Properties props, Simulator sim) {
        super(props, sim);
        // Initialize everything (using properties)
        this.tm = (TuringMachine) Utilities.instantiateObject(props.getProperty("tm.class"), new Object[]{props}, null);

        // Don't look at me! I'm hideous!
        if (this.sim instanceof RoundsTMaze) {
            ((RoundsTMaze) sim).setRestartListener(this);
        } else if (this.sim instanceof StepSimulator) {
            StepSimulator ss = (StepSimulator) this.sim;
            Simulator subSim = ss.getSimulator();
            if (subSim instanceof RoundsTMaze) {
                ((RoundsTMaze) subSim).setRestartListener(this);
            }
        }
    }

    @Override
    public double evaluate(Activator nn) {
        double totalScore;
        if (!BlockPuzzle.EVOLUTION_MODE.equals("minimum"))
            totalScore = 0;
        else
            totalScore = Goal.MAX_SCORE;
        for (int k = 0; k < iterations; k++) {
            sim.reset();
            int steps = 0;

            long time = System.currentTimeMillis();
            long nnTime = 0;
            long contTime = 0;
            long simTime = 0;

            // For each iteration
            for (int i = 0; i < BlockPuzzle.COMBINATIONS_NUMBER; i++) {
                boolean goal_achieved = true;
                for (int q = 0; q < BlockPuzzle.NUMBER_OF_GOALS && goal_achieved; q++) {
                    this.reset();
                    sim.restart();

                    double[] controllerOutput = this.getInitialInput();
                    double[] simOutput = sim.getInitialObservation();

                    while (!sim.isTerminated()) {
                        time = System.currentTimeMillis();

                        double[] nnOutput = this.activateNeuralNetwork(nn, simOutput, controllerOutput);

                        nnTime += (System.currentTimeMillis() - time);
                        time = System.currentTimeMillis();

                        // CopyTask can rely on the TM acting first
                        controllerOutput = this.getControllerResponse(Arrays.copyOfRange(nnOutput, sim.getInputCount(), nnOutput.length));

                        contTime += (System.currentTimeMillis() - time);
                        time = System.currentTimeMillis();

                        simOutput = this.getSimulationResponse(Arrays.copyOfRange(nnOutput, 0, sim.getInputCount()));

                        simTime += (System.currentTimeMillis() - time);
                        time = System.currentTimeMillis();
                        steps++;
                    }

                    if (BlockPuzzle.EVOLUTION_MODE.equals("sequential")) {
                        if (sim.getCurrentScore() < Goal.ALLOWED_MAX_SCORE)
                            goal_achieved = false;
                    }
                    if (BlockPuzzle.EVOLUTION_MODE.equals("minimum")) {
                        if (totalScore > sim.getCurrentScore())
                            totalScore = sim.getCurrentScore();
                    } else
                        totalScore += sim.getCurrentScore();
                }
                sim.restart();

            }
            if (BlockPuzzle.PRINT_SCREENSHOT) {
                SimulationsScreenshot.GenerateScreenshot("simulation_screenshot");
                SimulationsScreenshot.ClearSimulations();
            }
            if (MemoryScreenshot.PRINT_MEMORY_SCREENSHOTS) {
                MemoryScreenshot.GenerateScreenshot("screenshot");
                MemoryScreenshot.IncreaseCount();
            }
        }
        if (!BlockPuzzle.EVOLUTION_MODE.equals("minimum"))
            totalScore /= BlockPuzzle.COMBINATIONS_NUMBER * BlockPuzzle.NUMBER_OF_GOALS;
        return Math.max(0.0, totalScore);
    }

    @Override
    public double[] processOutputs(double[] fromNN) {
        double[] result = Utilities.flatten(tm.processInput(fromNN));

//		System.out.println("fromNN: "+Arrays.toString(fromNN));
//		System.out.println("toNN: "+Arrays.toString(result));

        return result;
    }

    @Override
    public double[] getInitialInput() {
        return Utilities.flatten(tm.getDefaultRead());
    }

    @Override
    public void reset() {
        this.tm.reset();
    }

    public TuringMachine getTuringMachine() {
        return tm;
    }

    @Override
    public void onRestart(RoundsTMaze tmaze) {
        this.reset();
    }
}
