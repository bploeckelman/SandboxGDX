package zendo.games.sandbox_gdx.screens;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Created by Brian on 7/25/2017
 */
public class TestScreen extends BaseScreen {

    // NOTE: this is a holdover from a previous attempt at converting Amit's old mapgen2 stuff to java
    @Override
    public void update(float dt) {

    }

    @Override
    public void render(SpriteBatch batch) {

    }

    /*
    private BlueNoiseGenerator blueNoiseGenerator;
    private FloatArray triangleVertices;
    private ShortArray triangleIndices;
    private Rectangle triangleBounds;
    private List<Triangle> triangles;

    private ShapeRenderer shapes;

    private final float width  = 500;
    private final float height = 500;
    private final int   num_bounds_samples = 20;

    public TestScreen() {
        shapes = Assets.shapes;

        triangleBounds = new Rectangle(
                camera.viewportWidth  / 2f - width  / 2f,
                camera.viewportHeight / 2f - height / 2f,
                width, height
        );
        generateTriangleVertices(triangleBounds);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

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

        float movementDt = 500 * dt * camera.zoom;
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
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        {
            final float width = 3f;
            shapes.setColor(Color.YELLOW);
            shapes.rectLine(triangleBounds.x, triangleBounds.y, triangleBounds.x + triangleBounds.width, triangleBounds.y, width);
            shapes.rectLine(triangleBounds.x, triangleBounds.y, triangleBounds.x, triangleBounds.y + triangleBounds.height, width);
            shapes.rectLine(triangleBounds.x, triangleBounds.y + triangleBounds.height, triangleBounds.x + triangleBounds.width, triangleBounds.y + triangleBounds.height, width);
            shapes.rectLine(triangleBounds.x + triangleBounds.width, triangleBounds.y, triangleBounds.x + triangleBounds.width, triangleBounds.y + triangleBounds.height, width);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        {
            shapes.setColor(Color.BLACK);
            for (Triangle triangle : triangles) {
                shapes.triangle(triangle.v1.x, triangle.v1.y, triangle.v2.x, triangle.v2.y, triangle.v3.x, triangle.v3.y);
            }

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
            for (Triangle triangle : triangles) {
                batch.draw(Assets.whitePixel, triangle.centroid.x - half_size, triangle.centroid.y - half_size, size, size);
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
            blueNoiseGenerator = new BlueNoiseGenerator(bounds, num_bounds_samples);
        } else {
            blueNoiseGenerator.generate();
        }

        triangleVertices = new FloatArray();
        for (Vector2 sample : blueNoiseGenerator.getSamples()) {
            triangleVertices.addAll(sample.x, sample.y);
        }
        DelaunayTriangulator triangulator = new DelaunayTriangulator();
        triangleIndices = triangulator.computeTriangles(triangleVertices, false);

        triangles = new ArrayList<Triangle>();
        for (int i = 0; i < triangleIndices.size; i += 3) {
            short i1 = triangleIndices.get(i + 0);
            short i2 = triangleIndices.get(i + 1);
            short i3 = triangleIndices.get(i + 2);
            float x1 = triangleVertices.get(i1 * 2);
            float x2 = triangleVertices.get(i2 * 2);
            float x3 = triangleVertices.get(i3 * 2);
            float y1 = triangleVertices.get(i1 * 2 + 1);
            float y2 = triangleVertices.get(i2 * 2 + 1);
            float y3 = triangleVertices.get(i3 * 2 + 1);
            triangles.add(new Triangle(i1, i2, i3, x1, y1, x2, y2, x3, y3));
        }

        // TODO: this is stupid, just generate them in a gdx Array to begin with
        Array<Vector2> points = new Array<Vector2>((Vector2[]) blueNoiseGenerator.getSamples().toArray());

        generateGraph(points);
    }


    //
    // Build graph data structure in 'edges', 'centers', 'corners'
    // based on information in the voronoi result:
    //
    // point.neighbors - list of neighboring points of the same type (corner or center)
    // points.edges  - list of edges that include that point
    //
    // Each edge connects to four points:
    // - the Voronoi edge: edge.{v0,v1}
    // - the Delaunay triangle edge:  edge.{d0,d1}
    //
    // For boundary polygons:
    // - the Voronoi edge may be null.
    // - the Delaunay edge will have one null point
    //
    // @param points
    //
    private Array<Center> centers;
    private Array<Corner> corners;
    private Array<Edge>   edges;
    private void generateGraph(Array<Vector2> points) {
        centers = new Array<Center>();
        corners = new Array<Corner>();
        edges = new Array<Edge>();

        Voronoi voronoi = new Voronoi(points, triangleBounds);
        Array<Edge> libedges = voronoi.edges;
        Rectangle bounds = triangleBounds;

        // Build Center objects for each of the points, and a lookup map
        // to find those Center objects again as we build the graph
        Dictionary<Vector2, Center> centerLookup = new Hashtable<Vector2, Center>();
        for (Vector2 point : points) {
            Center center = new Center(centers.size, point);
            centers.add(center);
            centerLookup.put(point, center);
        }

        // Workaround for Voronoi lib bug: we need to call region()
        // before Edges or neighboringSites are available
        for (Center center : centers) {
            voronoi.region(center.point);
        }

        // The Voronoi library generates multiple Point objects for
        // corners, and we need to canonicalize to one Corner object.
        // To make lookup fast, we keep an array of Points, bucketed by
        // x value, and then we only have to look at other Points in
        // nearby buckets. When we fail to find one, we'll create a new
        // Corner object.
        Array<Array<Corner>> cornerMap = new Array<Array<Corner>>();
        // NOTE: makeCorner() previously defined here...

        for (Edge libedge : libedges) {
            LineSegment dEdge = libedge.delaunayLine();
            LineSegment vEdge = libedge.voronoiEdge();

            // Fill the graph data. Make an Edge object corresponding to
            // the edge from the voronoi library
            Edge edge = new Edge(edges.size);
            if (vEdge.p0 != null && vEdge.p1 != null) {
                edge.midpoint = new Vector2(
                        vEdge.p0.x + (vEdge.p1.x - vEdge.p0.x) * 0.5f,
                        vEdge.p0.y + (vEdge.p1.y - vEdge.p0.y) * 0.5f
                );
            }
            edges.add(edge);

            // Edges point to corners. Edges point to centers.
            edge.v0 = makeCorner(vEdge.p0, bounds, cornerMap);
            edge.v1 = makeCorner(vEdge.p1, bounds, cornerMap);
            edge.d0 = centerLookup.get(dEdge.p0);
            edge.d1 = centerLookup.get(dEdge.p1);

            // Cennters point to edges. Corners point to edges.
            if (edge.d0 != null) { edge.d0.borders.add(edge); }
            if (edge.d1 != null) { edge.d1.borders.add(edge); }
            if (edge.v0 != null) { edge.v0.portrudes.add(edge); }
            if (edge.v1 != null) { edge.v1.portrudes.add(edge); }

            // Centers point to centers.
            if (edge.d0 != null && edge.d1 != null) {
                addToCenterList(edge.d0.neighbors, edge.d1);
                addToCenterList(edge.d1.neighbors, edge.d0);
            }

            // Corners point to corners.
            if (edge.v0 != null && edge.v1 != null) {
                addToCornerList(edge.v0.adjacent, edge.v1);
                addToCornerList(edge.v1.adjacent, edge.v0);
            }

            // Centers point to corners
            if (edge.d0 != null) {
                addToCornerList(edge.d0.corners, edge.v0);
                addToCornerList(edge.d0.corners, edge.v1);
            }
            if (edge.d1 != null) {
                addToCornerList(edge.d1.corners, edge.v0);
                addToCornerList(edge.d1.corners, edge.v1);
            }

            // Corners point to centers.
            if (edge.v0 != null) {
                addToCenterList(edge.v0.touches, edge.d0);
                addToCenterList(edge.v0.touches, edge.d1);
            }
            if (edge.v1 != null) {
                addToCenterList(edge.v1.touches, edge.d0);
                addToCenterList(edge.v1.touches, edge.d1);
            }
        }
    }

    private Corner makeCorner(Vector2 point, Rectangle bounds, Array<Array<Corner>> cornerMap) {
        if (point == null) return null;
        for (int bucket = (int) point.x - 1; bucket <= (int) point.x + 1; ++bucket) {
            for (Corner c : cornerMap.get(bucket)) {
                float dx = point.x - c.point.x;
                float dy = point.y - c.point.y;
                if (dx * dx + dy * dy < 1e-6) {
                    return c;
                }
            }
        }
        int bucket = (int) point.x;
        if (cornerMap.get(bucket) == null) {
            cornerMap.set(bucket, new Array<Corner>());
        }
        Corner corner = new Corner(corners.size, point);
        corner.border = (point.x == bounds.x || point.x == bounds.x + bounds.width
                && point.y == bounds.y || point.y == bounds.y + bounds.height);
        cornerMap.get(bucket).add(corner);
        return corner;
    }

    private void addToCornerList(Array<Corner> cornerList, Corner corner) {
        if (corner != null && cornerList.indexOf(corner, true) < 0) {
            cornerList.add(corner);
        }
    }

    private void addToCenterList(Array<Center> centerList, Center center) {
        if (center != null && centerList.indexOf(center, true) < 0) {
            centerList.add(center);
        }
    }

    // Look up a Voronoi Edge object given two adjacent Voronoi
    // polygons, or two adjacent Voronoi corners
    private Edge lookupEdgeFromCenter(Center p, Center r) {
        for (Edge edge : p.borders) {
            if (edge.d0 == r || edge.d1 == r) return edge;
        }
        return null;
    }

    private Edge lookupEdgeFromCorner(Corner q, Corner s) {
        for (Edge edge : q.portrudes) {
            if (edge.v0 == s || edge.v1 == s) return edge;
        }
        return null;
    }
    */

}
