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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import zendo.games.sandbox_gdx.utils.Assets;
import zendo.games.sandbox_gdx.utils.BlueNoiseGenerator;
import zendo.games.sandbox_gdx.utils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian on 7/25/2017
 */
public class TestScreen extends BaseScreen {

    private BlueNoiseGenerator blueNoiseGenerator;
    private FloatArray triangleVertices;
    private ShortArray triangleIndices;
    private Rectangle triangleBounds;
    private List<Vector2> triangleCentroids;

    private ShapeRenderer shapes;

    public TestScreen() {
        shapes = Assets.shapes;

        float width = 500;
        float height = 500;
        triangleBounds = new Rectangle(
                camera.viewportWidth / 2f - width / 2f,
                camera.viewportHeight / 2f - height / 2f,
                width, height
        );
        generateTriangleVertices(triangleBounds);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            final float duration = 0.25f;
            Timeline.createSequence()
                    .push(Tween.to(overayAlpha, 1, duration).target(1))
                    .push(Tween.call(new TweenCallback() {
                        @Override
                        public void onEvent(int i, BaseTween<?> baseTween) {
                            generateTriangleVertices(triangleBounds);
                        }
                    }))
                    .push(Tween.to(overayAlpha, 1, duration).target(0))
                    .start(Assets.tween);
        }

        float movementDt = 200 * dt * camera.zoom;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, movementDt);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -movementDt);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-movementDt, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(movementDt, 0);
        }
        updateCamera();

        camera.update();
        hudCamera.update();
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(Config.bgColor.r, Config.bgColor.g, Config.bgColor.b, Config.bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        {
            shapes.setColor(Color.BLACK);
            for (int i = 0; i < triangleIndices.size; i += 3) {
                float x1 = triangleVertices.get(triangleIndices.get(i + 0) * 2);
                float x2 = triangleVertices.get(triangleIndices.get(i + 1) * 2);
                float x3 = triangleVertices.get(triangleIndices.get(i + 2) * 2);
                float y1 = triangleVertices.get(triangleIndices.get(i + 0) * 2 + 1);
                float y2 = triangleVertices.get(triangleIndices.get(i + 1) * 2 + 1);
                float y3 = triangleVertices.get(triangleIndices.get(i + 2) * 2 + 1);
                shapes.triangle(x1, y1, x2, y2, x3, y3);
            }
            shapes.setColor(Color.YELLOW);
            shapes.rect(triangleBounds.x, triangleBounds.y, triangleBounds.width, triangleBounds.height);
            shapes.setColor(Color.WHITE);
        }
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            float size = 2f;
            float half_size = size / 2f;
            batch.setColor(Color.RED);
            for (Vector2 point : blueNoiseGenerator.getSamples()) {
                batch.draw(Assets.whitePixel, point.x - half_size, point.y - half_size, size, size);
            }

            size = 1f;
            half_size = size / 2f;
            batch.setColor(Color.BLUE);
            for (Vector2 centroid : triangleCentroids) {
                batch.draw(Assets.whitePixel, centroid.x - half_size, centroid.y - half_size, size, size);
            }

            batch.setColor(Color.WHITE);
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

    private Vector3 cameraTouchStart = new Vector3();
    private Vector3 touchStart = new Vector3();
    private Vector3 tp = new Vector3();
    private boolean cancelTouchUp = false;

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        cameraTouchStart.set(camera.position);
        touchStart.set(screenX, screenY, 0);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (cancelTouchUp) {
            cancelTouchUp = false;
            return false;
        }
        return true;
    }


    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        final float drag_delta = 0.1f;

        camera.position.x = cameraTouchStart.x + (touchStart.x - screenX) * camera.zoom;
        camera.position.y = cameraTouchStart.y + (screenY - touchStart.y) * camera.zoom;
        if (cameraTouchStart.dst(camera.position) > drag_delta) {
            cancelTouchUp = true;
        }
        return true;
    }

    @Override
    public boolean scrolled(int change) {
        final float zoom_scale = 0.15f;

        camera.unproject(tp.set(Gdx.input.getX(), Gdx.input.getY(), 0));
        float px = tp.x;
        float py = tp.y;
        camera.zoom += change * camera.zoom * zoom_scale;
        updateCamera();

        camera.unproject(tp.set(Gdx.input.getX(), Gdx.input.getY(), 0));
        camera.position.add(px - tp.x, py - tp.y, 0);
        camera.update();

        return true;
    }

    private void updateCamera() {
        final float zoom_min = 0.05f;
        final float zoom_max = 3f;

        camera.zoom = MathUtils.clamp(camera.zoom, zoom_min, zoom_max);
        camera.update();
    }

    private void generateTriangleVertices(Rectangle bounds) {
        if (blueNoiseGenerator == null) {
            blueNoiseGenerator = new BlueNoiseGenerator(bounds, 20);
        } else {
            blueNoiseGenerator.generate();
        }

        triangleVertices = new FloatArray();
        for (Vector2 sample : blueNoiseGenerator.getSamples()) {
            triangleVertices.addAll(sample.x, sample.y);
        }
        DelaunayTriangulator triangulator = new DelaunayTriangulator();
        triangleIndices = triangulator.computeTriangles(triangleVertices, false);

        triangleCentroids = new ArrayList<Vector2>();
        for (int i = 0; i < triangleIndices.size; i += 3) {
            float x1 = triangleVertices.get(triangleIndices.get(i + 0) * 2);
            float x2 = triangleVertices.get(triangleIndices.get(i + 1) * 2);
            float x3 = triangleVertices.get(triangleIndices.get(i + 2) * 2);
            float y1 = triangleVertices.get(triangleIndices.get(i + 0) * 2 + 1);
            float y2 = triangleVertices.get(triangleIndices.get(i + 1) * 2 + 1);
            float y3 = triangleVertices.get(triangleIndices.get(i + 2) * 2 + 1);
            triangleCentroids.add(GeometryUtils.triangleCentroid(x1, y1, x2, y2, x3, y3, new Vector2()));
        }
    }

}
