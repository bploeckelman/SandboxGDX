package zendo.games.sandbox_gdx.screens;

import aurelienribon.tweenengine.Tween;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import zendo.games.sandbox_gdx.utils.Assets;
import zendo.games.sandbox_gdx.utils.Config;

/**
 * Created by Brian on 7/25/2017
 */
public class TestScreen extends BaseScreen {

    public float accum;
    public float animStateTime = 0f;
    public Texture texture;

    public TestScreen() {
        accum = 0f;
    }

    @Override
    public void update(float dt) {
        accum += dt;
        animStateTime += dt;

        texture = Assets.testTexture;

        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            Tween.to(alpha, 1, 1)
                    .target(1)
                    .start(Assets.tween);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(Config.bgColor.r, Config.bgColor.g, Config.bgColor.b, Config.bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        {
            batch.draw(texture, 0, 0, hudCamera.viewportWidth, hudCamera.viewportHeight);

            batch.setColor(0, 0, 0, alpha.floatValue());
            batch.draw(Assets.whitePixel, 0, 0, hudCamera.viewportWidth, hudCamera.viewportHeight);
            batch.setColor(Color.WHITE);
        }
        batch.end();
    }

}
