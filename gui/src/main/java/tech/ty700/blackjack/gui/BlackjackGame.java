package tech.ty700.blackjack.gui;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;

public class BlackjackGame extends ApplicationAdapter {

    @Override
    public void render() {
        Color tableColor = new Color();
        tableColor.r = 0.05f;
        tableColor.g = 0.35f;
        tableColor.b = 0.15f;

        ScreenUtils.clear(tableColor);
    }
}
