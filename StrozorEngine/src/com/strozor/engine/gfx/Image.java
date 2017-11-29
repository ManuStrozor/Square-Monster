package com.strozor.engine.gfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;

import javax.swing.JOptionPane;

public class Image {

    private int w, h;
    private int[] p;
    private boolean alpha = false;

    public Image(String path) {
        BufferedImage image = null;

        try {
            image = ImageIO.read(Image.class.getResourceAsStream(path));
        } catch(IOException e) {
            e.printStackTrace();
        }

        w = image.getWidth();
        h = image.getHeight();
        p = image.getRGB(0, 0, w, h, null, 0, w);

        image.flush();
    }

    public Image(int[] p, int w, int h) {
        this.p = p;
        this.w = w;
        this.h = h;
    }

    // External Images
    public Image(String path, boolean test) {
        BufferedImage image = null;

        try {
            image = ImageIO.read(new File(path));
        } catch(IOException e) {
            e.printStackTrace();
        }

        w = image.getWidth();
        h = image.getHeight();
        p = image.getRGB(0, 0, w, h, null, 0, w);

        image.flush();
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public int[] getP() {
        return p;
    }

    public void setP(int x, int y, int value) {
        this.p[x + y * getW()] = value;
    }

    public boolean isAlpha() {
        return alpha;
    }

    public void saveImage() {
        JOptionPane jop = new JOptionPane(), jop2 = new JOptionPane();

        String filename = jop.showInputDialog(null,
                "Give a name to your level map",
                "Skewer Maker - Creative Mode",
                JOptionPane.QUESTION_MESSAGE);

        if(filename == null || filename.equals("")) {
            filename = "creativeMode";
            jop2.showMessageDialog(null,
                    "Default : " + filename + ".png",
                    "Skewer Maker - Creative Mode",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            jop2.showMessageDialog(null,
                    filename + ".png",
                    "Skewer Maker - Creative Mode",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        try {
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    bi.setRGB(x, y, p[x + y * w]);
                }
            }
            File out = new File(filename+".png");
            ImageIO.write(bi, "png", out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
