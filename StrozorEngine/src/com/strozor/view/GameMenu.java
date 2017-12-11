package com.strozor.view;

import com.strozor.engine.GameContainer;
import com.strozor.engine.GameRender;
import com.strozor.engine.Settings;
import com.strozor.engine.View;
import com.strozor.engine.audio.SoundClip;
import com.strozor.engine.gfx.Button;

import java.awt.event.KeyEvent;

public class GameMenu extends View {

    private Settings s;
    private SoundClip select;

    public GameMenu(Settings settings) {
        s = settings;
        select = new SoundClip("/audio/select.wav");
        buttons.add(new Button("Back to game", 1));
        buttons.add(new Button("Options", 3));
        buttons.add(new Button("Quit to title", 0));
    }

    @Override
    public void update(GameContainer gc, float dt) {

        if(gc.getInput().isKeyDown(KeyEvent.VK_ESCAPE)) gc.setState(1);

        //Button selection
        for(Button btn : buttons) {
            if (isSelected(gc, btn)) {
                select.play();
                gc.setState(btn.getGoState());
                gc.setLastState(2);
            }
        }
    }

    @Override
    public void render(GameContainer gc, GameRender r) {

        r.fillRect(0, 0, gc.getWidth(), gc.getHeight(), 0x99000000);

        int x = gc.getWidth() / 2 - 170 / 2;
        int y = gc.getHeight() / 3 - 20 / 2;

        for(int i = 0; i < buttons.size(); i++) {
            Button btn = buttons.get(i);
            btn.setOffX(x);
            btn.setOffY(y + i * 30);
            r.drawButton(btn, s.translate(btn.getText()));
        }
    }
}
