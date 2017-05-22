package dk.itu.lusa.turing.memory_screenshot;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

/**
 * Created by lucas on 21/04/2017.
 */

class ScreenshotMemory {
    LinkedList<double[][]> modes;
    LinkedList<int[]> chosen_modes;
    LinkedList<LinkedList<double[]>> tapes;
    LinkedList<LinkedList<LinkedList<Double>>> temporalMatrixes;
    LinkedList<int[]> read_head;
    LinkedList<int[]> after_shift;
    LinkedList<int[]> write_head;
    LinkedList<LinkedList<Double>> precedenceVectors;

    ScreenshotMemory() {
        tapes = new LinkedList<>();
        temporalMatrixes = new LinkedList<>();
        modes = new LinkedList<>();
        chosen_modes = new LinkedList<>();
        read_head = new LinkedList<>();
        after_shift = new LinkedList<>();
        write_head = new LinkedList<>();
        precedenceVectors = new LinkedList<>();
    }

    int getHeight() {
        int height = 0;
        for (int i = 0; i < tapes.size(); i++) {
            if(!modes.isEmpty() && !temporalMatrixes.isEmpty())
            height = Math.max(height, MemoryScreenshot.CELL_SIZE * (modes.get(i).length + tapes.get(i).size() + temporalMatrixes.get(i).size() + 2));
            else
                height = Math.max(height, MemoryScreenshot.CELL_SIZE * (tapes.get(i).size() + 1));
        }
        return height;
    }

    int getWidth() {
        int width = 0;
        for (int i = 0; i < tapes.size(); i++) {
            int max_modes_length = 0;
            if(!modes.isEmpty() && !temporalMatrixes.isEmpty()) {
                for (int k = 0; k < modes.get(i).length; k++)
                    if (max_modes_length < modes.get(i)[k].length)
                        max_modes_length = modes.get(i)[k].length;
                width += (Math.max(max_modes_length, Math.max(tapes.get(i).get(0).length, temporalMatrixes.get(i).get(0).size()) + 1)) * MemoryScreenshot.CELL_SIZE;
            }else
                width+=(Math.max(max_modes_length,tapes.get(i).get(0).length)+1)*MemoryScreenshot.CELL_SIZE;
        }
        return width;
    }

    void setScreenshotMemory(LinkedList<double[]> tape,int[] write_head, int[] read_head, int[] after_shift, double[][] modes, int[] chosen_modes, LinkedList<LinkedList<Double>> temporalMatrix, LinkedList<Double> precedenceVectors)
    {
        LinkedList<double[]> tape_copy = new LinkedList<>();
        for (double[] loc : tape) {
            double[] loc_copy = new double[loc.length];
            System.arraycopy(loc, 0, loc_copy, 0, loc.length);
            tape_copy.add(loc_copy);
        }
        tapes.add(tape_copy);

        LinkedList<LinkedList<Double>> temporalMatrix_copy = new LinkedList<>();
        for (LinkedList<Double> row : temporalMatrix) {
            LinkedList<Double> row_copy = new LinkedList<>();
            row_copy.addAll(row);
            temporalMatrix_copy.add(row_copy);
        }
        temporalMatrixes.add(temporalMatrix_copy);

        int[] chosen_mode = new int[chosen_modes.length];
        System.arraycopy(chosen_modes, 0, chosen_mode, 0, chosen_modes.length);
        this.chosen_modes.add(chosen_mode);

        double[][] modes_copies = new double[modes.length][];
        for (int i = 0; i < modes.length; i++) {
            double[] modes_copy = new double[modes[i].length];
            System.arraycopy(modes[i], 0, modes_copy, 0, modes[i].length);
            modes_copies[i] = modes_copy;
        }

        this.modes.add(modes_copies);

        int[] local_read_head = new int[read_head.length];
        System.arraycopy(read_head,0,local_read_head,0,read_head.length);
        this.read_head.add(local_read_head);

        int[] local_write_head = new int[write_head.length];
        System.arraycopy(write_head,0,local_write_head,0,write_head.length);
        this.write_head.add(local_write_head);

        int[] local_after_shift = new int[after_shift.length];
        System.arraycopy(after_shift,0,local_after_shift,0,after_shift.length);
        this.after_shift.add(local_after_shift);

        LinkedList<Double> copy_precedence_vectors = new LinkedList<>();
        copy_precedence_vectors.addAll(precedenceVectors);
        this.precedenceVectors.add(copy_precedence_vectors);
    }

    void setScreenshotMemory(LinkedList<double[]> tape,int[] read_head,int[] write_head, int[] after_shift )
    {
        LinkedList<double[]> tape_copy = new LinkedList<>();
        for (double[] loc : tape) {
            double[] loc_copy = new double[loc.length];
            System.arraycopy(loc, 0, loc_copy, 0, loc.length);
            tape_copy.add(loc_copy);
        }
        tapes.add(tape_copy);

        int[] head_copies = new int[read_head.length];
        System.arraycopy(read_head,0,head_copies,0,read_head.length);
        this.read_head.add(head_copies);

        int[] write_copies = new int[write_head.length];
        System.arraycopy(write_head,0,write_copies,0,write_head.length);
        this.write_head.add(write_copies);

        int[] shift_copies = new int[after_shift.length];
        System.arraycopy(after_shift,0,shift_copies,0,after_shift.length);
        this.after_shift.add(shift_copies);
    }
}
