package com.strozor.view;

import com.strozor.engine.*;
import com.strozor.engine.audio.SoundClip;
import com.strozor.engine.gfx.Button;

import java.awt.event.KeyEvent;

public class Credits extends View {

    private Settings s;
    private GameMap map;
    private SoundClip select;
    private Button back;

    private String[] devs = {"ManuStrozor"};
    private String[] thanks = {"My family", "Beta testers"};
    private String[] contribs = {"Majoolwip"};

    public Credits(Settings s, GameMap map) {
        this.s = s;
        this.map = map;
        select = new SoundClip("/audio/select.wav");
        buttons.add(back = new Button(60, 20, "Back", 0));
    }

    @Override
    public void update(GameContainer gc, float dt) {

        if(gc.getInput().isKeyDown(KeyEvent.VK_ESCAPE)) gc.setState(gc.getLastState());

        //Button selection
        for(Button btn : buttons) {
            if (isSelected(gc, btn)) {
                select.play();
                gc.setState(btn.getGoState());
                gc.setLastState(6);
            }
        }
    }

    @Override
    public void render(GameContainer gc, GameRender r) {

        r.drawBackground(gc, map, "wall");
        r.drawMenuTitle(gc, s.translate("Game credits").toUpperCase(), s.translate("Development team"));

        r.drawList(gc.getWidth() / 4, gc.getHeight() / 3, s.translate("MAIN DEVELOPERS"), devs);
        r.drawList(gc.getWidth() / 2, gc.getHeight() / 3, s.translate("THANKS TO"), thanks);
        r.drawList(gc.getWidth() - gc.getWidth() / 4, gc.getHeight() / 3, s.translate("CONTRIBUTORS"), contribs);

        back.setOffX(5);
        back.setOffY(5);

        for(Button btn : buttons) r.drawButton(btn, s.translate(btn.getText()));
    }
}
