package zendo.games.sandbox_gdx.screens;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import zendo.games.sandbox_gdx.utils.Assets;
import zendo.games.sandbox_gdx.utils.BlueNoiseGenerator;
import zendo.games.sandbox_gdx.utils.Config;

/**
 * Created by Brian on 7/25/2017
 */
public class TestScreen extends BaseScreen {

    public float accum;
    public float animStateTime = 0f;
    public BlueNoiseGenerator blueNoiseGenerator;

    public TestScreen() {
        accum = 0f;

        float width = 500;
        float height = 500;
        Rectangle bounds = new Rectangle(
                camera.viewportWidth / 2f - width / 2f,
                camera.viewportHeight / 2f - height / 2f,
                width, height
        );
        blueNoiseGenerator = new BlueNoiseGenerator(bounds);
    }

    @Override
    public void update(float dt) {
        accum += dt;
        animStateTime += dt;

        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            Timeline.createSequence()
                    .push(Tween.to(overayAlpha, 1, 1).target(1))
                    .push(Tween.call(new TweenCallback() {
                        @Override
                        public void onEvent(int i, BaseTween<?> baseTween) {
                            blueNoiseGenerator.generate();
                        }
                    }))
                    .push(Tween.to(overayAlpha, 1, 1).target(0))
                    .start(Assets.tween);
        }

        camera.update();
        hudCamera.update();
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(Config.bgColor.r, Config.bgColor.g, Config.bgColor.b, Config.bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            batch.setColor(Color.RED);
            final float size = 9f;
            final float half_size = size / 2f;
            for (Vector2 point : blueNoiseGenerator.getSamples()) {
                batch.draw(Assets.circleTexture, point.x - half_size, point.y - half_size, size, size);
            }
        }
        batch.end();

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        {
            batch.setColor(Color.WHITE);
            Assets.font.draw(batch, "Samples: " + blueNoiseGenerator.getSamples().size(), 10f, hudCamera.viewportHeight - 10f);

            batch.setColor(0, 0, 0, overayAlpha.floatValue());
            batch.draw(Assets.whitePixel, 0, 0, hudCamera.viewportWidth, hudCamera.viewportHeight);
            batch.setColor(Color.WHITE);
        }
        batch.end();
    }

}
