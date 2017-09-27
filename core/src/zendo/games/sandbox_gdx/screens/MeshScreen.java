package zendo.games.sandbox_gdx.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import zendo.games.sandbox_gdx.utils.Assets;
import zendo.games.sandbox_gdx.utils.BlueNoiseGenerator;
import zendo.games.sandbox_gdx.utils.ConcaveHull;
import zendo.games.sandbox_gdx.utils.Config;
import zendo.games.sandbox_gdx.world.ZenPolygon;

import java.util.ArrayList;
import java.util.List;

public class MeshScreen extends BaseScreen {

    ShapeRenderer shapes;
    PolygonSpriteBatch polys;

    List<Vector2> concaveSamples;
    List<Vector2> samples;
    ZenPolygon polygon;
    ConcaveHull concaveHull;

    float N = 1.5f;

    final int num_samples = 10;
    final int num_boundary_samples = 0;
    final float width = 100;
    final float height = 100;

    List<Vector2> testSamples;

    public MeshScreen() {
        testSamples = new ArrayList<Vector2>();
        testSamples.add(new Vector2(-10f, -10f));
        testSamples.add(new Vector2( 10f, -10f));
        testSamples.add(new Vector2( 15f,  10f));
        testSamples.add(new Vector2(-15f,  10f));
        testSamples.add(new Vector2(  0f,   8f));
        testSamples.add(new Vector2(  0f,   -8f));
        testSamples.add(new Vector2(-11f,  0f));
        testSamples.add(new Vector2(11f,  0f));

        shapes = Assets.shapes;
        polys = Assets.polys;
        polygon = ZenPolygon.createConvexHullPolygon(generateSamplePoints());

//        final Rectangle points_bounds = new Rectangle(-width / 2f, -height / 2f, width, height);
//        BlueNoiseGenerator pointsGenerator = new BlueNoiseGenerator(points_bounds, num_boundary_samples, num_samples);
//        this.concaveSamples = pointsGenerator.getSamples();
//        concaveHull = new ConcaveHull(concaveSamples, N);
        concaveHull = new ConcaveHull(testSamples, N);

        camera.translate(-camera.viewportWidth / 2f, -camera.viewportHeight / 2f);
        camera.zoom -= 0.5f;
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            polygon = ZenPolygon.createConvexHullPolygon(generateSamplePoints());

            final Rectangle points_bounds = new Rectangle(-width / 2f, -height / 2f, width, height);
            BlueNoiseGenerator pointsGenerator = new BlueNoiseGenerator(points_bounds, num_boundary_samples, num_samples);
            concaveSamples = pointsGenerator.getSamples();
            concaveHull = new ConcaveHull(concaveSamples, N);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            N += 0.1f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            N -= 0.1f;
        }

        updateCamera();

        camera.update();
        hudCamera.update();
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(Config.bgColor.r, Config.bgColor.g, Config.bgColor.b, Config.bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setColor(Color.LIGHT_GRAY);
        batch.begin();
        {
            Assets.font.draw(batch, "N: " + N, 10, 30);
        }
        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        concaveHull.renderConvexHull(shapes);
        concaveHull.renderConcaveHull(shapes);
        concaveHull.renderInnerPoints(shapes);
        concaveHull.renderConvexHullPoints(shapes);
        concaveHull.renderConcaveHullPoints(shapes);

        /*
        polys.setProjectionMatrix(camera.combined);
        polys.begin();
        {
            polys.setColor(Color.ORANGE);
            for (Vector2 sample : samples) {
                polys.draw(Assets.whitePixelRegion, sample.x - 4f, sample.y - 4f, 8f, 8f);
            }
            polys.setColor(Color.WHITE);
            polygon.render(polys);
        }
        polys.end();
        */

        /*
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        {
            shapes.setColor(Color.FOREST);
            shapes.polygon(concaveHull.convexVertices.items);
            shapes.setColor(Color.GOLD);
            shapes.polygon(concaveHull.concaveVertices.items);
            shapes.setColor(Color.WHITE);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        {
            final float points_radius = 2f;
            shapes.setColor(Color.BLUE);
            for (Vector2 sample : concaveSamples) {
                shapes.circle(sample.x, sample.y, points_radius);
            }
            shapes.setColor(Color.WHITE);
        }
        shapes.end();
       */
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

    private FloatArray generateSamplePoints() {
        return generateSamplePoints(500, 500);
    }

    private FloatArray generateSamplePoints(float width, float height) {
        final int num_samples = 0;
        final Rectangle points_bounds = new Rectangle(-width / 2f, -height / 2f, width, height);
        BlueNoiseGenerator pointsGenerator = new BlueNoiseGenerator(points_bounds, num_samples, 50);
        FloatArray points = new FloatArray();
        samples = pointsGenerator.getSamples();
        for (Vector2 sample : samples) {
            points.addAll(sample.x, sample.y);
        }
        return points;
    }

}
