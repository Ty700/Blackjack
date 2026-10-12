package tech.ty700.blackjack.gui;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class Main {
    public void main()
    {
        var game = new BlackjackGame();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Blackjack!");
        config.useVsync(true);
        config.setWindowedMode(1920, 1080);
        Lwjgl3Application app = new Lwjgl3Application(game, config);
    }
}
