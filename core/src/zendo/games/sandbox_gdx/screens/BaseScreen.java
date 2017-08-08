package zendo.games.sandbox_gdx.screens;

import aurelienribon.tweenengine.primitives.MutableFloat;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import zendo.games.sandbox_gdx.utils.Config;

/**
 * Created by Brian on 4/16/2017
 */
public abstract class BaseScreen extends InputAdapter {

    public MutableFloat overayAlpha;
    public OrthographicCamera camera;
    public OrthographicCamera hudCamera;

    public BaseScreen () {
        super();
        camera = new OrthographicCamera(Config.gameWidth, Config.gameHeight);
        camera.translate(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();

        hudCamera = new OrthographicCamera(Config.gameWidth, Config.gameHeight);
        hudCamera.translate(hudCamera.viewportWidth / 2, hudCamera.viewportHeight / 2, 0);
        hudCamera.update();

        overayAlpha = new MutableFloat(0f);
    }

    public abstract void update(float dt);
    public abstract void render(SpriteBatch batch);

}
