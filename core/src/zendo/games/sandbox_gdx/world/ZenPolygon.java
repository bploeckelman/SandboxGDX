package zendo.games.sandbox_gdx.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ShortArray;
import zendo.games.sandbox_gdx.utils.Assets;

public class ZenPolygon {

    private FloatArray vertices;
    private ShortArray triangles;
    private Vector2 center;

    public boolean drawVertices = true;
    public PolygonSprite sprite;

    public ZenPolygon(FloatArray vertices) {
        if (vertices == null) {
            throw new GdxRuntimeException("Cannot createConvexHullPolygon from null concaveVertices");
        }
        this.vertices = new FloatArray(vertices.shrink());

        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        this.triangles = new ShortArray(triangulator.computeTriangles(this.vertices));

        PolygonRegion polygonRegion = new PolygonRegion(Assets.whitePixelRegion, this.vertices.items, this.triangles.items);
        this.sprite = new PolygonSprite(polygonRegion);

        Vector2 min = new Vector2(Float.MAX_VALUE, Float.MAX_VALUE);
        Vector2 max = new Vector2(Float.MIN_VALUE, Float.MIN_VALUE);
        Vector2 accum = new Vector2();
        for (int i = 0; i < vertices.size; i += 2) {
            if (vertices.items[i] < min.x) min.x = vertices.items[i];
            if (vertices.items[i] > max.x) max.x = vertices.items[i];

            if (vertices.items[i+1] < min.y) min.y = vertices.items[i+1];
            if (vertices.items[i+1] > max.y) max.y = vertices.items[i+1];
            accum.add(vertices.items[i], vertices.items[i+1]);
        }
        float width  = max.x - min.x;
        float height = max.y - min.y;
        this.sprite.setOrigin(width / 2f, height / 2f);
        this.sprite.setSize(width, height);
        this.center = new Vector2(accum.x / (float) vertices.size, accum.y / (float) vertices.size);
    }

    public static ZenPolygon createConvexHullPolygon(FloatArray points) {
        if (points == null) {
            throw new GdxRuntimeException("Cannot createConvexHullPolygon from null points");
        }

        final ConvexHull convexHull = new ConvexHull();
        final FloatArray hullPoints = new FloatArray(convexHull.computePolygon(points, false).shrink());
        return new ZenPolygon(hullPoints);
    }

    public void render(PolygonSpriteBatch polyBatch) {
        polyBatch.setColor(Color.WHITE);
        sprite.draw(polyBatch);

        if (drawVertices) {
            final float vertex_size = 5f;

            // Vertices
            polyBatch.setColor(Color.BLUE);
            for (int i = 0; i < vertices.size; i += 2) {
                polyBatch.draw(Assets.whitePixelRegion,
                        vertices.items[i  ] + sprite.getX() - vertex_size / 2f,
                        vertices.items[i+1] + sprite.getY() - vertex_size / 2f,
                        vertex_size, vertex_size);
            }

            // Center
            polyBatch.setColor(Color.MAGENTA);
            polyBatch.draw(Assets.whitePixelRegion,
                    sprite.getX() + sprite.getOriginX() + center.x - vertex_size / 2f,
                    sprite.getY() + sprite.getOriginY() + center.y - vertex_size / 2f,
                    vertex_size, vertex_size);

            // Origin
            polyBatch.setColor(Color.RED);
            polyBatch.draw(Assets.whitePixelRegion,
                    sprite.getX() + sprite.getOriginX() - vertex_size / 2f,
                    sprite.getY() + sprite.getOriginY() - vertex_size / 2f,
                    vertex_size, vertex_size);

            // Position
            polyBatch.setColor(Color.GREEN);
            polyBatch.draw(Assets.whitePixelRegion,
                    sprite.getX() - vertex_size / 2f,
                    sprite.getY() - vertex_size / 2f,
                    vertex_size, vertex_size);

            polyBatch.setColor(Color.WHITE);
        }
    }

}
