package dk.itu.lusa.turing.memory_screenshot;

import dk.itu.ejuuragr.fitness.Utilities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

/**
 * Created by lucas on 21/04/2017.
 */
class WholeScreenshot {
    LinkedList<ScreenshotMemory> goal_screenshots;

    WholeScreenshot() {
        goal_screenshots = new LinkedList<>();
    }

    BufferedImage generateImage() {
        int width = 0, height = 0, CELL_SIZE = MemoryScreenshot.CELL_SIZE;
        for (ScreenshotMemory s : goal_screenshots) {
            width = Math.max(width, s.getWidth());
            height += s.getHeight() + 1;
        }
        BufferedImage image = new BufferedImage(width + 6, height, BufferedImage.TYPE_INT_RGB);
        int row = 0;
        for (ScreenshotMemory s : goal_screenshots) {
            for (int i = 0, starting_k = 6; i < s.tapes.size(); i++) {
                int modes_l = 0;
                if (!s.modes.isEmpty()) {
                    for (int i1 = 0; i1 < s.modes.get(i).length; i1++) {
                        for (int k = 0; k < s.modes.get(i)[i1].length; k++) {
                            int value;
                            if (k == s.chosen_modes.get(i)[i1])
                                value = Color.BLUE.getRGB();
                            else
                                value = Color.RED.getRGB();
                            for (int x = i1 * CELL_SIZE; x < (i1 + 1) * CELL_SIZE; x++) {
                                for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                                    image.setRGB(starting_k + y, row + x, value);
                                }
                            }
                        }
                    }
                    modes_l = s.modes.get(i).length;
                }
                for (int i1 = 0; i1 < s.tapes.get(i).size(); i1++) {
                    if (i1 == s.write_head.get(i)[0]) {
                        int value = Color.RED.getRGB();
                        for (int x = (i1 + modes_l) * CELL_SIZE; x < (i1 + modes_l + 1) * CELL_SIZE; x++) {
                            for (int y = 0; y < 2; y++) {
                                image.setRGB(starting_k - 6 + y, row + x, value);
                            }
                        }
                    }

                    if (i1 == s.read_head.get(i)[0]) {
                        int value = Color.GREEN.getRGB();
                        for (int x = (i1 + modes_l) * CELL_SIZE; x < (i1 + modes_l + 1) * CELL_SIZE; x++) {
                            for (int y = 0; y < 2; y++) {
                                image.setRGB(starting_k - 4 + y, row + x, value);
                            }
                        }
                    }

                    if (i1 == s.after_shift.get(i)[0]) {
                        int value = Color.ORANGE.getRGB();
                        for (int x = (i1 + modes_l) * CELL_SIZE; x < (i1 + modes_l + 1) * CELL_SIZE; x++) {
                            for (int y = 0; y < 2; y++) {
                                image.setRGB(starting_k - 2 + y, row + x, value);
                            }
                        }
                    }



                    for (int k = 0; k < s.tapes.get(i).get(i1).length; k++) {
                        int value = new Color((int)(s.tapes.get(i).get(i1)[k]*255), (int)(s.tapes.get(i).get(i1)[k]*255), (int)(s.tapes.get(i).get(i1)[k]*255)).getRGB();
                        for (int x = (i1 + modes_l) * CELL_SIZE; x < (i1 + modes_l + 1) * CELL_SIZE; x++) {
                            for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                                image.setRGB(starting_k + y, row + x, value);
                            }
                        }
                    }
                }
                if (!s.temporalMatrixes.isEmpty()) {
                    int s_tm = s.tapes.get(i).size() + s.modes.get(i).length + 1;
                    for (int k = 0; k < Math.max(s.temporalMatrixes.get(i).size(), s.tapes.get(i).getFirst().length); k++)
                        for (int x = (s_tm - 1) * CELL_SIZE + CELL_SIZE / 4; x < (s_tm) * CELL_SIZE - CELL_SIZE / 4; x++) {
                            for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                                image.setRGB(starting_k + y, row + x, Color.YELLOW.getRGB());
                            }
                        }

                    for (int i1 = s_tm, j = 0; i1 < s_tm + s.temporalMatrixes.get(i).size(); i1++, j++) {
                        int v = new Color(0, (int)(s.precedenceVectors.get(i).get(j) * 255), 0).getRGB();
                        for (int x = (i1) * CELL_SIZE; x < (i1 + 1) * CELL_SIZE; x++) {
                            for (int y = 0; y < 4; y++) {
                                image.setRGB(starting_k - 6 + y, row + x, v);
                            }
                        }
                        if (i - 1 >= 0 && s.chosen_modes.get(i)[0] == 0 && j == s.after_shift.get(i - 1)[0]) {
                            int value = Color.BLUE.getRGB();
                            for (int x = (i1) * CELL_SIZE; x < (i1 + 1) * CELL_SIZE; x++) {
                                for (int y = 0; y < 2; y++) {
                                    image.setRGB(starting_k - 2 + y, row + x, value);
                                }
                            }
                        }
                        for (int k = 0; k < s.temporalMatrixes.get(i).get(j).size(); k++) {
                            if(i1==s_tm && i - 1 >= 0 && s.chosen_modes.get(i)[0] == 2 && k == s.after_shift.get(i - 1)[0])
                            {
                                int value = Color.BLUE.getRGB();
                                for (int x = i1 * CELL_SIZE; x < i1 * CELL_SIZE +2; x++) {
                                    for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                                        image.setRGB(starting_k + y, row + x -2, value);
                                    }
                                }
                            }
                            int value = new Color((int)(s.temporalMatrixes.get(i).get(j).get(k)*255), 0, (int)(s.temporalMatrixes.get(i).get(j).get(k)*255)).getRGB();
                            for (int x = i1 * CELL_SIZE; x < (i1 + 1) * CELL_SIZE; x++) {
                                for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                                    image.setRGB(starting_k + y, row + x, value);
                                }
                            }
                        }
                    }
                    for (int k = 0; k < Math.max(s.temporalMatrixes.get(i).size(), s.tapes.get(i).getFirst().length); k++)
                        for (int x = (s_tm + s.temporalMatrixes.get(i).size()) * CELL_SIZE + CELL_SIZE / 4; x < (s_tm + s.temporalMatrixes.get(i).size() + 1) * CELL_SIZE - CELL_SIZE / 4; x++) {
                            for (int y = k * CELL_SIZE; y < (k + 1) * CELL_SIZE; y++) {
                                image.setRGB(starting_k + y, row + x, Color.YELLOW.getRGB());
                            }
                        }
                }
                if (!s.modes.isEmpty() && !s.temporalMatrixes.isEmpty())
                    starting_k += (Math.max(s.tapes.get(i).get(0).length, s.temporalMatrixes.get(i).get(0).size()) + 1) * CELL_SIZE;
                else
                    starting_k += (s.tapes.get(i).get(0).length + 1) * CELL_SIZE;
                /*
                for (int i1 = 0; i1 < height / CELL_SIZE; i1++)
                    for (int x = i1 * CELL_SIZE; x < (i1 + 1) * CELL_SIZE; x++)
                        for (int y = 0; y < CELL_SIZE / 2; y++)
                            image.setRGB(starting_k - CELL_SIZE * 3 / 4 + y, x, Color.YELLOW.getRGB());
                */

            }
            row += s.getHeight() + 1;
        }
        goal_screenshots.clear();
        return image;
    }
}