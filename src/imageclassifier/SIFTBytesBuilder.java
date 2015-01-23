/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imageclassifier;

import de.lmu.ifi.dbs.jfeaturelib.features.Sift;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 *
 * @author carlosmagno
 */
public class SIFTBytesBuilder {

    public static void buildBytes(String file_path) {
        File file = new File(file_path);
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                buildBytes(f.getAbsolutePath());
            }
        } else {
            if (file_path.endsWith(".jpg")) {
                String OS = System.getProperty("os.name").toLowerCase();
                BufferedImage img;
                int width, height;
                try {
                    img = ImageIO.read(new File(file_path));
                    width = img.getWidth();
                    height = img.getHeight();

                    byte[][] pxMatrix = new byte[width][height];
                    boolean[][] boMatrix = new boolean[width][height];

                    System.out.println("width x height " + width + " x " + height);
                    ImageProcessor ip = new ColorProcessor(img);
                    ip = ip.convertToByte(false);
                    File siftFile = null;
                    if (OS.contains("win")) {
                        siftFile = new File("siftWin32.exe");
                    } else if (OS.contains("unix") || OS.contains("linux")) {
                        siftFile = new File("sift");
                    } else {
                        System.out.println("Operational System not detected");
                        return;
                    }
                    Sift sift = new Sift(siftFile);
                    sift.run(ip);

                    //note that result.get(i)[0..3] represent y/x/scale/rotation, respectively.
                    List<double[]> result = sift.getFeatures();
                    
                    for (double[] pt : result) {
                        double dx = Math.cos(pt[3]) * pt[2];
                        double dy = Math.sin(pt[3]) * pt[2];
                        long xo, yo, xf, yf;
                        if(dx < 0){
                            xo = Math.round(pt[1] + dx);
                            xf = Math.round(pt[1]);
                        }else{
                            xo = Math.round(pt[1]);
                            xf = Math.round(pt[1] + dx);
                        }
                        if(dy < 0){
                            yo = Math.round(pt[0]);
                            yf = Math.round(pt[0] - dy);
                        }else{
                            yo = Math.round(pt[0] - dy);
                            yf = Math.round(pt[0]);
                        }
                        long yo_temp = yo;
                        for(; xo <= xf; xo++){
                            for(yo = yo_temp ;yo <= yf; yo++){
                                boMatrix[(int)xo][(int)yo] = true;
                                pxMatrix[(int)xo][(int)yo] = (byte)(img.getRGB((int)xo, (int)yo) / (65536 + 256 + 1));
                            }
                        }
                    }
                    BufferedImage newImage = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
                    for(int i = 0; i < width; i++){
                        for (int j = 0; j < height; j++) {
                            if(boMatrix[i][j]){
                                int rgb = pxMatrix[i][j];
                                newImage.setRGB(i, j, (rgb*65536)+(rgb*256)+rgb);
                            }else{
                                newImage.setRGB(i, j, (0*65536)+(255*256)+0);
                            }
                        }
                    }
                    ImageIO.write(newImage, "jpg", new File(file_path + "_sif.jpg"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
