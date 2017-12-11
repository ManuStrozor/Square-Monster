package com.strozor.engine;

import com.strozor.engine.gfx.*;
import com.strozor.game.GameManager;
import com.strozor.game.GameObject;

import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class GameRender {

    private ImageTile objsImg;
    private ArrayList<ImageRequest> imageRequest = new ArrayList<>();

    private int pW, pH;
    private int[] p, zb, lm;
    private int camX, camY;
    private int zDepth = 0;
    private boolean processing = false;

    public GameRender(GameContainer gc) {
        String path = System.getenv("APPDATA") + "\\.squaremonster\\assets\\objects.png";
        objsImg = new ImageTile(path, GameManager.TS, GameManager.TS, true);
        pW = gc.getWidth();
        pH = gc.getHeight();
        p = ((DataBufferInt)gc.getWindow().getImage().getRaster().getDataBuffer()).getData();
        zb = new int[p.length];
        lm = new int[p.length];
    }

    void clear() {
        for(int i = 0; i < p.length; i++) {
            p[i] = 0;
            zb[i] = 0;
            lm[i] = 0xff898989;
        }
    }

    void process() {
        processing = true;

        Collections.sort(imageRequest, new Comparator<ImageRequest>(){
            @Override
            public int compare(ImageRequest i0, ImageRequest i1) {
                return Integer.compare(i0.zDepth, i1.zDepth);
            }
        });

        for(ImageRequest ir : imageRequest) {
            setzDepth(ir.zDepth);
            drawImage(ir.image, ir.offX, ir.offY);
        }

        for(int i = 0; i < p.length; i++) {

            int pR = (p[i] >> 16) & 255;
            int pG = (p[i] >> 8) & 255;
            int pB = p[i] & 255;

            int lR = (lm[i] >> 16) & 255;
            int lG = (lm[i] >> 8) & 255;
            int lB = lm[i] & 255;

            p[i] = (int)(pR * lR / 255f) << 16 | (int)(pG * lG / 255f) << 8 | (int)(pB * lB / 255f);
        }

        imageRequest.clear();
        processing = false;
    }

    private void setPixel(int x, int y, int value) {
        int alpha = (value >> 24) & 255;

        if((x < 0 || x >= pW || y < 0 || y >= pH) || alpha == 0) return;

        int index = x + y * pW;

        if(zb[index] > zDepth) return;

        zb[index] = zDepth;

        if(alpha == 255) {
            p[index] = value;
        } else {
            int pR = (p[index] >> 16) & 255;
            int pG = (p[index] >> 8) & 255;
            int pB = p[index] & 255;

            int vR = (value >> 16) & 255;
            int vG = (value >> 8) & 255;
            int vB = value & 255;

            int newR = pR - (int)((pR - vR) * alpha / 255f);
            int newG = pG - (int)((pG - vG) * alpha / 255f);
            int newB = pB - (int)((pB - vB) * alpha / 255f);

            p[index] = newR << 16 | newG << 8 | newB;
        }
    }

    private void setLightMap(int x, int y, int value) {
        if(x < 0 || x >= pW || y < 0 || y >= pH) return;

        int baseColor = lm[x + y * pW];

        int maxR = Math.max((baseColor >> 16) & 255, (value >> 16) & 255);
        int maxG = Math.max((baseColor >> 8) & 255, (value >> 8) & 255);
        int maxB = Math.max(baseColor & 255, value & 255);

        lm[x + y * pW] = maxR << 16 | maxG << 8 | maxB;
    }

    private int darken(int color, int diff) {

        int a = (color >> 24) & 0xff;
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;

        int newA = (a - diff < 0 ? 0 : a - diff) << 24;
        int newR = (r - diff < 0 ? 0 : r - diff) << 16;
        int newG = (g - diff < 0 ? 0 : g - diff) << 8;
        int newB = b - diff < 0 ? 0 : b - diff;

        return newA | newR | newG | newB;
    }

    private int lighten(int color, int diff) {

        int a = (color >> 24) & 0xff;
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;

        int newA = (a + diff > 255 ? 255 : a + diff) << 24;
        int newR = (r + diff > 255 ? 255 : r + diff) << 16;
        int newG = (g + diff > 255 ? 255 : g + diff) << 8;
        int newB = b + diff > 255 ? 255 : b + diff;

        return newA | newR | newG | newB;
    }

    private int textSize(String text, Font font) {
        int textW = 0;
        for(int i = 0; i < text.length(); i++)
            textW += font.getWidths()[text.codePointAt(i)];
        return textW;
    }

    public void drawText(String text, int offX, int offY, int alignX, int alignY, int color, Font font) {
        if(alignX != 1) {
            if(alignX == 0) offX -= textSize(text, font) / 2;
            else if(alignX == -1) offX -= textSize(text, font);
        }

        if(alignY != 1) {
            if(alignY == 0) offY -= font.getFontImage().getH() / 2;
            else if(alignY == -1) offY -= font.getFontImage().getH();
        }

        int offset = 0;
        for(int i = 0; i < text.length(); i++) {
            int unicode = text.codePointAt(i);
            //With darker color
            for(int y = 0; y < font.getFontImage().getH(); y++) {
                for(int x = 0; x < font.getWidths()[unicode]; x++) {
                    if(font.getFontImage().getP()[(x + font.getOffsets()[unicode]) + y * font.getFontImage().getW()] == 0xff000000) {
                        setPixel(x + offX + offset - 1, y + offY - 1, darken(color, 100));
                    }
                }
            }
            //With normal color
            for(int y = 0; y < font.getFontImage().getH(); y++) {
                for(int x = 0; x < font.getWidths()[unicode]; x++) {
                    if(font.getFontImage().getP()[(x + font.getOffsets()[unicode]) + y * font.getFontImage().getW()] == 0xff000000) {
                        setPixel(x + offX + offset, y + offY, color);
                    }
                }
            }
            offset += font.getWidths()[unicode];
        }
    }

    private void drawImage(Image image, int offX, int offY) {

        offX -= camX;
        offY -= camY;

        if(image.isAlpha() && !processing) {
            imageRequest.add(new ImageRequest(image, zDepth, offX, offY));
            return;
        }

        if(offX < -image.getW()) return;
        if(offY < -image.getH()) return;
        if(offX >= pW) return;
        if(offY >= pH) return;

        int newX = 0;
        int newY = 0;
        int newWidth = image.getW();
        int newHeight = image.getH();

        if(offX < 0) newX -= offX;
        if(offY < 0) newY -= offY;
        if(offX + newWidth >= pW) newWidth -= newWidth + offX - pW;
        if(offY + newHeight >= pH) newHeight -= newHeight + offY - pH;

        for(int y = newY; y < newHeight; y++) {
            for(int x = newX; x < newWidth; x++) {
                setPixel(x + offX, y + offY, image.getP()[x + y * image.getW()]);
            }
        }
    }

    public void drawImageTile(ImageTile image, int offX, int offY, int tileX, int tileY) {

        offX -= camX;
        offY -= camY;

        if(image.isAlpha() && !processing) {
            imageRequest.add(new ImageRequest(image.getTileImage(tileX, tileY), zDepth, offX, offY));
            return;
        }

        if(offX < -image.getTileW()) return;
        if(offY < -image.getTileH()) return;
        if(offX >= pW) return;
        if(offY >= pH) return;

        int newX = 0;
        int newY = 0;
        int newWidth = image.getTileW();
        int newHeight = image.getTileH();

        if(offX < 0) newX -= offX;
        if(offY < 0) newY -= offY;
        if(offX + newWidth >= pW) newWidth -= newWidth + offX - pW;
        if(offY + newHeight >= pH) newHeight -= newHeight + offY - pH;

        for(int y = newY; y < newHeight; y++) {
            for(int x = newX; x < newWidth; x++) {
                setPixel(x + offX, y + offY, image.getP()[(x + tileX * image.getTileW()) + (y + tileY * image.getTileH()) * image.getW()]);
            }
        }
    }

    private void drawRect(int x, int y, int w, int h, int col) {

        x -= camX;
        y -= camY;

        for(int i = 0; i < w; i++) {
            setPixel(x+i, y, col);
            setPixel(x+i, y+h-1, col);
        }

        for(int i = 0; i < h; i++) {
            setPixel(x, y+i, col);
            setPixel(x+w-1, y+i, col);
        }
    }

    public void fillRect(int x, int y, int w, int h, int col) {

        x -= camX;
        y -= camY;

        if(x < -w) return;
        if(y < -h) return;
        if(x >= pW) return;
        if(y >= pH) return;

        int newX = 0;
        int newY = 0;
        int newWidth = w;
        int newHeight = h;

        if(x < 0) newX -= x;
        if(y < 0) newY -= y;
        if(x + newWidth >= pW) newWidth -= newWidth + x - pW;
        if(y + newHeight >= pH) newHeight -= newHeight + y - pH;

        for(int j = newY; j < newHeight; j++) {
            for(int i = newX; i < newWidth; i++) {
                setPixel(i + x, j + y, col);
            }
        }
    }

    private void drawLight(Light l, int offX, int offY) {

        offX -= camX;
        offY -= camY;

        for(int i = 0; i < l.getDiameter(); i++) {
            drawLightLine(l, l.getRadius(), l.getRadius(), i, 0, offX, offY);
            drawLightLine(l, l.getRadius(), l.getRadius(), i, l.getDiameter(), offX, offY);
            drawLightLine(l, l.getRadius(), l.getRadius(), 0, i, offX, offY);
            drawLightLine(l, l.getRadius(), l.getRadius(), l.getDiameter(), i, offX, offY);
        }
    }

    private void drawLightLine(Light l, int x0, int y0, int x1, int y1, int offX, int offY) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;

        while(true) {

            int screenX = x0 - l.getRadius() + offX;
            int screenY = y0 - l.getRadius() + offY;

            int lightColor = l.getLightValue(x0, y0);
            if(lightColor == 0) return;

            setLightMap(screenX, screenY, lightColor);

            if(x0 == x1 && y0 == y1) break;

            e2 = 2 * err;

            if(e2 > -dy) {
                err -= dy;
                x0 += sx;
            }

            if(e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    public void drawButton(Button b, String text) {
        //Border-out
        drawRect(b.getOffX()+camX, b.getOffY()+camY, b.getWidth(), b.getHeight(), 0xff333333);
        //background & text
        fillRect(b.getOffX()+camX+1, b.getOffY()+camY+1, b.getWidth()-2, b.getHeight()-2, b.getBgColor());
        drawText(text, b.getOffX()+b.getWidth()/2, b.getOffY()+b.getHeight()/2, 0, 0, lighten(b.getBgColor(), 150), Font.STANDARD);
        //Border-in lighter
        fillRect(b.getOffX()+camX+1, b.getOffY()+camY+1, 1, b.getHeight()-2, lighten(b.getBgColor(), 70));
        fillRect(b.getOffX()+camX+2, b.getOffY()+camY+1, b.getWidth()-4, 1, lighten(b.getBgColor(), 70));
        //Border-in darker
        fillRect(b.getOffX()+camX+b.getWidth()-2, b.getOffY()+camY+1, 1, b.getHeight()-2, darken(b.getBgColor(), 70));
        fillRect(b.getOffX()+camX+2, b.getOffY()+camY+b.getHeight()-2, b.getWidth()-4, 1, darken(b.getBgColor(), 70));
    }

    public void drawInput(int x, int y, int w, int h, int color, String text) {
        //Background & text
        fillRect(x, y, w, h, color);
        drawText(text, x+4, y+h/2, 1, 0, -1, Font.STANDARD);
        //Border-out darker
        fillRect(x, y, w, 1, darken(color, 40));
        fillRect(x-1, y+1, 1, h-1, darken(color, 40));
        //Border-out lighter
        fillRect(x, y+h, w, 1, lighten(color, 40));
        fillRect(x+w, y+1, 1, h-1, lighten(color, 40));
    }

    void drawGameStates(GameContainer gc, GameObject obj) {
        int width = GameManager.TS * 6;
        int x = gc.getWidth() / 2 - width / 2;
        fillRect(x, 0, width, GameManager.TS, 0x99000000);
        drawBloc(new Bloc(2), x, 0);
        drawBloc(new Bloc(7), x + GameManager.TS * 2, 0);
        drawBloc(new Bloc(5), x + GameManager.TS * 4, 0);
        drawText("x" + obj.getLives(), x + GameManager.TS-4, GameManager.TS, 1, -1,0xffcdcdcd, Font.BIG_STANDARD);
        drawText("x" + obj.getCoins(), x + GameManager.TS * 3-4, GameManager.TS, 1, -1,0xffcdcdcd, Font.BIG_STANDARD);
        drawText("x" + obj.getKeys(), x + GameManager.TS * 5-4, GameManager.TS, 1, -1,0xffcdcdcd, Font.BIG_STANDARD);
    }

    public void drawMap(GameMap gameMap) {
        for(int y = 0; y < gameMap.getHeight(); y++) {
            for(int x = 0; x < gameMap.getWidth(); x++) {

                Bloc curr = gameMap.getBloc(x, y);

                //draw wall behind non-solid bloc
                if(!curr.isSolid() && !curr.getName().equals("Wall"))
                    drawImageTile(objsImg, x * GameManager.TS, y * GameManager.TS, 1, 0);

                //draw bloc
                drawImageTile(objsImg, x * GameManager.TS, y * GameManager.TS, curr.getTileX(), curr.getTileY());

                //draw shadow under solid bloc
                if(!curr.isSolid() && gameMap.isSolid(x, y-1))
                    drawImageTile(objsImg, x * GameManager.TS, y * GameManager.TS, 0, 3);

                //draw top part of the door
                if(curr.getName().equals("Door"))
                    drawImageTile(objsImg, x * GameManager.TS, (y - 1) * GameManager.TS, curr.getTileX(), curr.getTileY()-1);
            }
        }
    }

    public void drawMapLights(GameMap gameMap, Light lamp) {
        for(int y = 0; y < gameMap.getHeight(); y++) {
            for(int x = 0; x < gameMap.getWidth(); x++) {
                if(gameMap.getBloc(x, y).getName().equals("Torch"))
                    drawLight(lamp, x * GameManager.TS + GameManager.TS / 2, y * GameManager.TS + GameManager.TS / 3);
            }
        }
    }

    private void drawBloc(Bloc b, int x, int y) {
        drawImageTile(objsImg, x, y, b.getTileX(), b.getTileY());
    }

    public void drawDock(GameContainer gc, int[] dock, int scroll) {
        int midH = camY + gc.getHeight() / 2;
        int s = GameManager.TS + 1;
        int y = midH - (dock.length * s)/2 + (dock.length/2 - scroll) * s;

        fillRect(camX, camY, s + 4, gc.getHeight(), 0x89000000);

        for(int i = 0; i < dock.length; i++)
            drawBloc(new Bloc(dock[i]), camX + 4, y - GameManager.TS/2 + s * i);

        drawRect(camX + 1, midH - GameManager.TS/2 - 3, s + 4, s + 4, 0xbbffffff);
        drawRect(camX + 2, midH - GameManager.TS/2 - 2, s + 2, s + 2, 0x77ffffff);
        drawRect(camX + 3, midH - GameManager.TS/2 - 1, s, s, 0x33ffffff);
    }

    public void drawMiniMap(GameContainer gc, Image img) {
        int xMMap = camX + gc.getWidth() - img.getW() - 1;
        int yMMap = camY + gc.getHeight() - img.getH() - 1;
        fillRect(xMMap, yMMap, img.getW(), img.getH(), 0x99ababab);
        drawRect(xMMap + camX/32 + 1, yMMap + camY/32, gc.getWidth()/32 - 2, gc.getHeight()/32, 0x99ababab);
        drawImage(img, xMMap, yMMap);
    }

    public void drawArrows(GameContainer gc, int width, int height) {
        if(camY > 0)
            drawBloc(new Bloc(14), camX + gc.getWidth()/2 - GameManager.TS/2, camY);
        if(camY + gc.getHeight() < height * GameManager.TS)
            drawBloc(new Bloc(15), camX + gc.getWidth()/2 - GameManager.TS/2, camY + gc.getHeight() - GameManager.TS);
        if(camX > -GameManager.TS)
            drawBloc(new Bloc(16), camX, camY + gc.getHeight()/2 - GameManager.TS/2);
        if(camX + gc.getWidth() < width * GameManager.TS)
            drawBloc(new Bloc(17), camX + gc.getWidth() - GameManager.TS, camY + gc.getHeight()/2 - GameManager.TS/2);
    }

    public void drawListOfFiles(GameContainer gc, ArrayList<Image> f, ArrayList<String> n, ArrayList<Date> d, int scroll, boolean is, int curr, String nothing) {

        if(f.size() == 0)
            drawText(nothing, gc.getWidth()/2, gc.getHeight()/2, 0, 0, 0xffababab, Font.STANDARD);

        int largest = 0;
        for(int i = 0; i < f.size(); i++) {
            int size = Math.max(textSize(n.get(i), Font.STANDARD), textSize(d.get(i).toString(), Font.STANDARD));
            size = Math.max(size, textSize("Dimensions: "+f.get(i).getW()+"*"+f.get(i).getH(), Font.STANDARD));
            if(size+f.get(i).getW() > largest) largest = size+f.get(i).getW();
        }

        int x = gc.getWidth()/2-largest/2;
        int y = GameManager.TS+10-scroll;

        int hUsed = 10;
        for(int i = 0; i < f.size(); i++) {

            int w = f.get(i).getW();
            int h = f.get(i).getH() < 30 ? 30 : f.get(i).getH();

            if(i != 0) y += f.get(i-1).getH() < 30 ? 30+10 : f.get(i-1).getH()+10;

            if(is && i == curr) {
                drawRect(x-4, y-4, largest+12, f.get(i).getH() < 30 ? 30+8 : f.get(i).getH()+8, 0xff696969);
            }

            fillRect(x, y, w, f.get(i).getH(), -1);
            drawImage(f.get(i), x, y);

            drawText(n.get(i), x+w+4, y-2, 1, 1, -1, Font.STANDARD);
            drawText("Dimensions: "+w+"*"+f.get(i).getH(), x+w+4, y+15, 1, 0, 0xff898989, Font.STANDARD);
            drawText(d.get(i).toString(), x+w+4, y+30+2, 1, -1, 0xff898989, Font.STANDARD);

            hUsed += h+10;
        }

        int hTotal = gc.getHeight()-3*GameManager.TS;
        int minus = hUsed-hTotal < 0 ? 0 : hUsed-hTotal;

        if(f.size() != 0)
            drawScrollBar(gc.getWidth()/2+largest/2+20, GameManager.TS, 8, hTotal, scroll/8, minus/8);
    }

    private void drawScrollBar(int x, int y, int w, int h, int scroll, int minus) {
        //BackBar
        fillRect(x, y, w, h, 0x66ffffff);
        fillRect(x+8, y, 1, h, 0xff444244);
        //Bar
        fillRect(x, y+scroll, w, h-minus, 0xffd4d2cc);
        //Relief
        fillRect(x+7, y+1+scroll, 1, h-1-minus, 0xff848284);
        fillRect(x+1, y+h-minus+scroll-1, w-1, 1, 0xff848284);
        fillRect(x+1, y+1+scroll, w-2, 1, 0xfffcfefc);
        fillRect(x+1, y+1+scroll, 1, h-2-minus, 0xfffcfefc);
        fillRect(x, y+h-minus+scroll, w, 1, 0xff444244);
    }

    public void drawBackground(GameContainer gc, Bloc bloc) {
        for(int y = 0; y <= gc.getHeight() / GameManager.TS; y++) {
            for(int x = 0; x <= gc.getWidth() / GameManager.TS; x++) {
                drawBloc(bloc, x * GameManager.TS, y * GameManager.TS);
            }
        }
    }

    public void fillAreaBloc(int nX, int nY, int nW, int nH, Bloc bloc) {
        for(int y = 0; y < nH; y++) {
            for(int x = 0; x < nW; x++) {
                drawBloc(bloc, nX + x * GameManager.TS, nY + y * GameManager.TS);
            }
        }
    }

    public void drawMenuTitle(GameContainer gc, String bigTitle, String smallTitle) {
        drawText(bigTitle, gc.getWidth() / 2, 45, 0, 1, 0xffc0392b, Font.BIG_STANDARD);
        if(!smallTitle.equals(""))
            drawText(smallTitle, gc.getWidth() / 2, 60, 0, 1, 0xffababab, Font.STANDARD);
    }

    public void drawList(int offX, int offY, String title, String[] list) {
        drawText(title, offX, offY, 0, 0, 0xff27ae60, Font.STANDARD);
        for(int i = 0; i < list.length; i++)
            drawText(list[i], offX, offY + 14 * (i + 1), 0, 0, 0xffababab, Font.STANDARD);
    }

    public int getCamX() {
        return camX;
    }

    public void setCamX(int camX) {
        this.camX = camX;
    }

    public int getCamY() {
        return camY;
    }

    public void setCamY(int camY) {
        this.camY = camY;
    }

    public void setCoorCam(int camX, int camY) {
        this.camX = camX;
        this.camY = camY;
    }

    private void setzDepth(int zDepth) {
        this.zDepth = zDepth;
    }
}
