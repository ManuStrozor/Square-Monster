package engine.view;

import engine.audio.SoundClip;
import engine.gfx.Button;
import engine.gfx.Font;
import game.AbstractGame;
import game.GameManager;
import engine.*;

import java.awt.event.KeyEvent;

public class GameSelection extends View {

    public static boolean focus, once;
    public static int fIndex, scroll, sMax;

    static {
        once = false;
        focus = true;
        fIndex = 0;
        scroll = 0;
        sMax = 0;
    }

    private Settings s;
    private World world;
    private GameManager gameManager;
    private Button play, back;
    private SoundClip hover, click;

    public GameSelection(Settings s, World world, AbstractGame game) {
        this.s = s;
        this.world = world;
        this.gameManager = (GameManager) game;
        hover = new SoundClip("/audio/hover.wav");
        click = new SoundClip("/audio/click.wav");
        buttons.add(play = new Button(170, 20, "Play", "game"));
        buttons.add(back = new Button(170, 20, "Back", "mainMenu"));
    }

    @Override
    public void update(GameContainer gc, float dt) {

        if (!once) {
            sMax = 10 - (gc.getHeight() - 3 * GameManager.TS);
            for (int i = 0; i < gc.getPlayerStats().getValueOf("Level up"); i++) {
                sMax += 30 + 10;
            }
            if (sMax < 0) sMax = 0;
            once = true;
        }

        if (gc.getInputHandler().isKeyDown(KeyEvent.VK_ESCAPE)) {
            focus = false;
            gc.setActiView("mainMenu");
            gc.setPrevView(gc.getPrevView());
            once = false;
        }
        //Scroll control
        if (gc.getInputHandler().getScroll() < 0) {
            scroll -= 20;
            if (scroll < 0) scroll = 0;
        } else if (gc.getInputHandler().getScroll() > 0) {
            scroll += 20;
            if (scroll > sMax) scroll = sMax;
        }
        //Selected control
        for (int i = 0; i <= gc.getPlayerStats().getValueOf("Level up"); i++) {
            if (levelSelected(gc, i, scroll)) {
                fIndex = i;
                focus = true;
            }
        }
        //Button selection
        for (Button btn : buttons) {
            btn.setBgColor(0xff616E7A);
            if (!focus && btn == play) {
                btn.setBgColor(0xffdedede);
            } else if (isSelected(gc, btn)) {
                if (btn == back) focus = false;
                if (btn == play) {
                    GameManager.current = fIndex;
                    gameManager.load(GameManager.levels[fIndex][0]);
                }
                click.play();
                gc.setActiView(btn.getTargetView());
                gc.setPrevView(gc.getPrevView());
                once = false;
            }

            if (btn.setHover(isHover(gc, btn))) {
                if (!btn.isHoverSounded()) {
                    if (!hover.isRunning()) hover.play();
                    btn.setHoverSounded(true);
                }
            } else {
                btn.setHoverSounded(false);
            }
        }
    }

    @Override
    public void render(GameContainer gc, Renderer r) {
        //Fill general background
        r.drawBackground(gc, world, "wall");
        r.fillRect(0, 0, gc.getWidth(), gc.getHeight(), 0x55000000);
        //Draw list of files & scroll bar
        if (sMax <= 0) scroll = 0;
        r.drawLevels(gc, GameManager.levels);
        //Draw background & Top title
        r.fillAreaBloc(0, 0, gc.getWidth() / GameManager.TS + 1, 1, world, "wall");
        r.drawText(s.translate("Choose a level"), gc.getWidth() / 2, GameManager.TS / 2, 0, 0, -1, Font.STANDARD);
        //Draw background & buttons
        r.fillAreaBloc(0, gc.getHeight() - GameManager.TS * 2, gc.getWidth() / GameManager.TS + 1, 2, world, "wall");
        play.setOffX(gc.getWidth() / 2 - play.getWidth() / 2);
        play.setOffY(gc.getHeight() - 2 * GameManager.TS + 10);
        back.setOffX(play.getOffX());
        back.setOffY(gc.getHeight() - GameManager.TS + 5);
        //Draw Buttons
        for (Button btn : buttons) r.drawButton(btn, s.translate(btn.getText()));
    }
}