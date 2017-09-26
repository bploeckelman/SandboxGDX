package zendo.games.sandbox_gdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;

import java.util.*;

/**
Data: list A with edges for the convex hull
Result: list B with edges for a concave hull

Sort list A after the length of the edges;

while list A is not empty
    Select the longest edge e from list A;
    Remove edge e from list A;

    Calculate local maximum distance d for edges;
    if length of edge is larger than distance d
        Find the point p with the smallest maximum angle a;
        if angle a is small enough and point p is not on the boundary
            Create edges e2 and e3 between point p and endpoints of edge e;

        if edge e2 and e3 don't intersect any other edge
            Add edge e2 and e3 to list A;
            Set point p to be on the boundary;

    if edge e2 and e3 was not added to list A
    Add edge e to list B;
*/
public class ConcaveHull {

    private static int edgeId = 0;

    public class Edge {
        private final FloatArray vertices;

        int id;
        int index1;
        int index2;

        Edge(int index1, int index2, FloatArray vertices) {
            this.id = edgeId++;
            this.vertices = vertices;
            this.index1 = index1;
            this.index2 = index2;
        }

        Edge(Edge edge) {
            this.vertices = edge.vertices;
            this.index1 = edge.index1;
            this.index2 = edge.index2;
        }

        float length() {
            float e1_p1x = vertices.get(index1 * 2);
            float e1_p1y = vertices.get(index1 * 2 + 1);
            float e1_p2x = vertices.get(index2 * 2);
            float e1_p2y = vertices.get(index2 * 2 + 1);
            float e1_dx = e1_p2x - e1_p1x;
            float e1_dy = e1_p2y - e1_p1y;
            return (float) Math.sqrt(e1_dx * e1_dx + e1_dy * e1_dy);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Edge)) return false;
            final Edge edge = (Edge) other;
            return (this.index1 == edge.index1
                 && this.index2 == edge.index2
                 && this.vertices == edge.vertices);
        }

        @Override
        public String toString() {
            return "Edge@" + id + "(" + index1 + ", " + index2 + ") length = " + length();
        }
    }

    private LinkedList<Edge> convexHullEdges;
    private LinkedList<Edge> concaveHullEdges;

    // Custom comparator for sorting edges by length
    private Comparator<Edge> edgeLengthComparator = new Comparator<Edge>() {
        @Override
        public int compare(Edge edge1, Edge edge2) {
            return Float.compare(edge2.length(), edge1.length());
        }
    };

    private FloatArray convexHullVertices;
    private FloatArray vertices;

    private IntArray convexHullIndices;
    private IntArray concaveHullIndices;
    private IntArray innerPoints;


    public ConcaveHull(List<Vector2> pointsList, float N) {
        // Copy pointsList to FloatArray for convex hull generation
        vertices = new FloatArray(pointsList.size() * 2);
        for (Vector2 point : pointsList) {
            vertices.addAll(point.x, point.y);
        }
        vertices.shrink();

        // Compute convex hull vertices / indices
        final ConvexHull convexHull = new ConvexHull();
        convexHullVertices = new FloatArray(convexHull.computePolygon(vertices, false).shrink());
        convexHullIndices = new IntArray(convexHull.computeIndices(vertices, false, false).shrink());

        // Generate convex hull edge list
        convexHullEdges = new LinkedList<Edge>();
        for (int i = 0; i < convexHullIndices.size - 1; ++i) {
            convexHullEdges.add(new Edge(convexHullIndices.get(i), convexHullIndices.get(i + 1), vertices));
        }

        // Collect non-boundary points
        innerPoints = new IntArray();
        for (int i = 0; i < vertices.size; i += 2) {
            float vx = vertices.get(i);
            float vy = vertices.get(i + 1);

            boolean isPointInConvexHull = false;
            for (int j = 0; j < convexHullVertices.size; j += 2) {
                float cvx = convexHullVertices.get(j);
                float cvy = convexHullVertices.get(j + 1);
                if (vx == cvx && vy == cvy) {
                    isPointInConvexHull = true;
                }
            }

            if (!isPointInConvexHull) {
                innerPoints.add(i / 2);
            }
        }
        innerPoints.shrink();

        // Process edges
        concaveHullEdges = new LinkedList<Edge>();
        LinkedList<Edge> edges = new LinkedList<Edge>(convexHullEdges);
        while (!edges.isEmpty()) {
            // Sort working edge list by edge length
            Collections.sort(edges, edgeLengthComparator);
            Edge edge = edges.pop();
            Gdx.app.log("ProcessingEdges", "Current edge: " + edge.toString());

            // TODO: Calculate local max distance d for edges
            // For now, just set a standard max length
            float d = 19f;

            Edge edge1, edge2;
            boolean didAddNewEdges = false;
            float len = edge.length();
            if (len > d) {
                // Fetch the vertices for edge
                final float e1_x = edge.vertices.get(edge.index1 * 2);
                final float e1_y = edge.vertices.get(edge.index1 * 2 + 1);
                final float e2_x = edge.vertices.get(edge.index2 * 2);
                final float e2_y = edge.vertices.get(edge.index2 * 2 + 1);

                // Collect inner points that are closer to current edge than other edges
                // TODO: test against working edge list (edges) or current concave edge list?
                IntArray nearestInnerPoints = findInnerPointsNearestToEdge(edge, concaveHullEdges, innerPoints);

                // Find the point p : innerPoints with the smallest max angle 'a'
                float minAngle = Float.MAX_VALUE;
                int minAngleInnerPointsIndex = -1;
                for (int i = 0; i < nearestInnerPoints.size; ++i) {
                    int innerPointIndex = nearestInnerPoints.get(i);
                    float p_x = vertices.items[innerPointIndex * 2];
                    float p_y = vertices.items[innerPointIndex * 2 + 1];

                    // Check what angles exist for potential new edges between p and edge
                    float d1_x = p_x - e1_x;
                    float d1_y = p_y - e1_y;
                    float d2_x = p_x - e2_x;
                    float d2_y = p_y - e2_y;
                    float angle1 = MathUtils.radiansToDegrees * MathUtils.atan2(d1_y, d1_x);
                    float angle2 = MathUtils.radiansToDegrees * MathUtils.atan2(d2_y, d2_x);
                    if (angle1 < 0) angle1 += 360f;
                    if (angle2 < 0) angle2 += 360f;
                    float angle = Math.min(angle1, angle2);

                    if (angle < minAngle) {
                        minAngle = angle;
                        minAngleInnerPointsIndex = i;
                    }
                }
                Gdx.app.log("ProcessingEdges", "\tminAngle: " + minAngle);

                // if minAngle is small enough...
                float minAngleThreshold = 190f; // TODO: determine how to set this value
                if (minAngle < minAngleThreshold) {
                    // Create edges edge1, edge2 between p and edge
                    edge1 = new Edge(edge.index1, innerPoints.items[minAngleInnerPointsIndex], vertices);
                    edge2 = new Edge(edge.index2, innerPoints.items[minAngleInnerPointsIndex], vertices);
                    // NOTE: reverse order of indices for edge2?

                    // If edge1 and edge2 don't intersect any other edge...
                    if (!doEdgesIntersectOtherEdges(edge1, edge2, concaveHullEdges)) {
                        // add edge1, edge2 to edges
                        edges.add(edge1);
                        edges.add(edge2);
                        // remove point p from innerPoints
                        innerPoints.removeIndex(minAngleInnerPointsIndex);
                        didAddNewEdges = true;
                        Gdx.app.log("ProcessingEdges", "\tDIG: Adding edges: " + edge1.toString() + ", " + edge2.toString());
                    }
                }
            }

            // if edge1 and edge2 was not added to edges
            if (!didAddNewEdges) {
                // add edge to list concaveHullEdges
                concaveHullEdges.add(edge);
                Gdx.app.log("ProcessingEdges", "\tNO DIG: Adding edge: " + edge.toString());
            }
        }
        Gdx.app.log("ConcaveHull", "Completed with...\n"
                + "\t" + convexHullEdges.size() + " convex edges\n"
                + "\t" + concaveHullEdges.size() + " concave edges\n"
                + "\t" + innerPoints.size + " remaining interior points");

        concaveHullIndices = new IntArray();
        for (Edge edge : concaveHullEdges) {
            if (!concaveHullIndices.contains(edge.index1)) concaveHullIndices.add(edge.index1);
            if (!concaveHullIndices.contains(edge.index2)) concaveHullIndices.add(edge.index2);
        }
    }

    /**
     * Return a list of point indices from pointIndices that are closer to the specified 'edge'
     * than to any other edges in the specified list 'edges'
     * @param edge the edge to find closest points to
     * @param edges the other edges to check distance against
     * @param pointIndices the list of indices of points to check
     * @return a list of point indices that are closer to 'edge' than any other edge in 'edges'
     */
    private IntArray findInnerPointsNearestToEdge(Edge edge, LinkedList<Edge> edges, IntArray pointIndices) {
        final IntArray nearestInnerPoints = new IntArray();
        final FloatArray pointVertices = edge.vertices;

        for (int i = 0; i < pointIndices.size; ++i) {
            int pointIndex = pointIndices.get(i);
            float px = pointVertices.get(pointIndex * 2);
            float py = pointVertices.get(pointIndex * 2 + 1);
            float e1_x = edge.vertices.get(edge.index1 * 2);
            float e1_y = edge.vertices.get(edge.index1 * 2 + 1);
            float e2_x = edge.vertices.get(edge.index2 * 2);
            float e2_y = edge.vertices.get(edge.index2 * 2 + 1);
            float currentEdgeDist = DE(px, py, e1_x, e1_y, e2_x, e2_y);

            boolean pointIsClosestToCurrentEdge = true;
            for (Edge otherEdge : edges) {
                if (edge == otherEdge) continue;
                float oe1_x = otherEdge.vertices.get(otherEdge.index1 * 2);
                float oe1_y = otherEdge.vertices.get(otherEdge.index1 * 2 + 1);
                float oe2_x = otherEdge.vertices.get(otherEdge.index2 * 2);
                float oe2_y = otherEdge.vertices.get(otherEdge.index2 * 2 + 1);
                float otherEdgeDist = DE(px, py, oe1_x, oe1_y, oe2_x, oe2_y);
                if (otherEdgeDist < currentEdgeDist) {
                    pointIsClosestToCurrentEdge = false;
                    break;
                }
            }

            if (pointIsClosestToCurrentEdge) {
                nearestInnerPoints.add(pointIndex);
                Gdx.app.log("ProcessingEdges", "\t\tInner point " + pointIndex + " is closer to edge: " + edge.toString() + " than other edges");
            }
        }

        nearestInnerPoints.shrink();
        return nearestInnerPoints;
    }

    /**
     * Check whether the specified edges (edge1, edge2) intersect any edges in the supplied list
     * NOTE: this ignores endpoint-only intersections
     * @param edge1 the first edge to check
     * @param edge2 the second edge to check
     * @param edges the edges to check against
     * @return true if edge1 or edge2 intersect other edges, false otherwise
     */
    private boolean doEdgesIntersectOtherEdges(Edge edge1, Edge edge2, LinkedList<Edge> edges) {
        float e1a_x = vertices.items[edge1.index1 * 2];
        float e1a_y = vertices.items[edge1.index1 * 2 + 1];
        float e1b_x = vertices.items[edge1.index2 * 2];
        float e1b_y = vertices.items[edge1.index2 * 2 + 1];

        float e2a_x = vertices.items[edge2.index1 * 2];
        float e2a_y = vertices.items[edge2.index1 * 2 + 1];
        float e2b_x = vertices.items[edge2.index2 * 2];
        float e2b_y = vertices.items[edge2.index2 * 2 + 1];

        Vector2 intersection = new Vector2();
        for (Edge edge : edges) {
            float ea_x = vertices.items[edge.index1 * 2];
            float ea_y = vertices.items[edge.index1 * 2 + 1];
            float eb_x = vertices.items[edge.index2 * 2];
            float eb_y = vertices.items[edge.index2 * 2 + 1];

            // NOTE: ignore intersections at endpoints
            boolean intersectsEdge1 = Intersector.intersectSegments(ea_x, ea_y, eb_x, eb_y, e1a_x, e1a_y, e1b_x, e1b_y, intersection);
            if (intersectsEdge1 &&
               ((intersection.x == ea_x && intersection.y == ea_y)
             || (intersection.x == eb_x && intersection.y == eb_y))) {
                intersectsEdge1 = false;
            }

            boolean intersectsEdge2 = Intersector.intersectSegments(ea_x, ea_y, eb_x, eb_y, e2a_x, e2a_y, e2b_x, e2b_y, intersection);
            if (intersectsEdge2 &&
               ((intersection.x == ea_x && intersection.y == ea_y)
             || (intersection.x == eb_x && intersection.y == eb_y))) {
                intersectsEdge2 = false;
            }

            if (intersectsEdge1 || intersectsEdge2) {
                Gdx.app.log("ProcessingEdges", "\t\tINTERSECTS: edge1 " + edge1.toString() + ", edge2 " + edge2.toString() + ": intersect " + edge.toString());
                return true;
            }
        }

        Gdx.app.log("ProcessingEdges", "\t\tNO INTERSECT: edge1 " + edge1.toString() + ", edge2 " + edge2.toString());
        return false;
    }


    // ------------------------------------------------------------------------
    // Render Helpers
    // ------------------------------------------------------------------------


    /**
     * Render points on the Convex Hull edges
     * @param shapes the ShapeRenderer to use
     */
    public void renderConvexHullPoints(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        {
            shapes.setColor(Color.CYAN);
            final float circle_radius = 2f;
            for (int i = 0; i < convexHullVertices.size; i += 2) {
                float px = convexHullVertices.get(i);
                float py = convexHullVertices.get(i+1);
                shapes.circle(px, py, circle_radius);
            }
            shapes.setColor(Color.WHITE);
        }
        shapes.end();
    }

    /**
     * Render points on the Concave Hull edges
     * @param shapes the ShapeRenderer to use
     */
    public void renderConcaveHullPoints(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        {
            shapes.setColor(Color.GREEN);
            final float circle_radius = 1.5f;
            for (int i = 0; i < concaveHullIndices.size; ++i) {
                float px = vertices.get(concaveHullIndices.get(i) * 2);
                float py = vertices.get(concaveHullIndices.get(i) * 2 + 1);
                shapes.circle(px, py, circle_radius);
            }
            shapes.setColor(Color.WHITE);
        }
        shapes.end();
    }

    /**
     * Render interior points (ie. not on convex or concave hull edges)
     * @param shapes the ShapeRenderer to use
     */
    public void renderInnerPoints(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        {
            shapes.setColor(Color.ORANGE);
            final float circle_radius = 1f;
            for (int i = 0; i < innerPoints.size; ++i) {
                float px = vertices.get(innerPoints.get(i)*2);
                float py = vertices.get(innerPoints.get(i)*2+1);
                shapes.circle(px, py, circle_radius);
            }
        }
        shapes.end();
    }

    /**
     * Render Convex Hull Edges
     * @param shapes the ShapeRenderer to use
     */
    public void renderConvexHull(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        {
            shapes.setColor(Color.GOLD);
            final float line_width = 1f;
            for (Edge edge : convexHullEdges) {
                float p1_x = vertices.get(edge.index1 * 2);
                float p1_y = vertices.get(edge.index1 * 2 + 1);
                float p2_x = vertices.get(edge.index2 * 2);
                float p2_y = vertices.get(edge.index2 * 2 + 1);
                shapes.rectLine(p1_x, p1_y, p2_x, p2_y, line_width);
            }
            shapes.setColor(Color.WHITE);
        }
        shapes.end();
    }

    /**
     * Render Concave Hull Edges
     * @param shapes the ShapeRenderer to use
     */
    public void renderConcaveHull(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        {
            shapes.setColor(Color.SALMON);
            final float line_width = 0.5f;
            for (Edge edge : concaveHullEdges) {
                float p1_x = vertices.get(edge.index1 * 2);
                float p1_y = vertices.get(edge.index1 * 2 + 1);
                float p2_x = vertices.get(edge.index2 * 2);
                float p2_y = vertices.get(edge.index2 * 2 + 1);
                shapes.rectLine(p1_x, p1_y, p2_x, p2_y, line_width);
            }
            shapes.setColor(Color.WHITE);
        }
        shapes.end();
    }

    // ------------------------------------------------------------------------
    // Distance Helper Functions
    // ------------------------------------------------------------------------

    /**
     * D = distance between points 1 and 2
     * @param x1 x value of point 1
     * @param y1 y value of point 1
     * @param x2 x value of point 2
     * @param y2 y value of point 2
     * @return distance between points 1 and 2
     */
    private float D(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * DD = decision distance between point p and set of points q = {q_1, q_2, ..., q_n}
     * @param px x value for point p
     * @param py y value for point p
     * @param q set of points q = [q_1x, q_1y, q_2x, q_2y, ..., q_nx, q_ny]
     * @return minimum distance between p and q
     */
    private float DD(float px, float py, FloatArray q) {
        float minD = Float.MAX_VALUE;
        for (int i = 0; i < q.items.length; i += 2) {
            float D = D(px, py, q.items[i], q.items[i+1]);
            if (D < minD) minD = D;
        }
        return minD;
    }

    /**
     * DE = distance between point p and edge (e_1, e_2)
     * @param px x value for point p
     * @param py y value for point p
     * @param e_1x x value for edge point 1
     * @param e_1y y value for edge point 1
     * @param e_2x x value for edge point 2
     * @param e_2y y value for edge point 2
     * @return minimum distance between p and edge points e_1 and e_2
     */
    private float DE(float px, float py, float e_1x, float e_1y, float e_2x, float e_2y) {
        float midx = (e_1x + e_2x) / 2f;
        float midy = (e_1y + e_2y) / 2f;
        return D(px, py, midx, midy);
    }

    /**
     * TODO: not correct, returns distance to nearest edge
     * DT = distance between point p and triangle points (t_1, t_2, t_3)
     * @param px x value for point p
     * @param py y value for point p
     * @param t_1x x value for triangle point 1
     * @param t_1y y value for triangle point 1
     * @param t_2x x value for triangle point 2
     * @param t_2y y value for triangle point 2
     * @param t_3x x value for triangle point 3
     * @param t_3y y value for triangle point 3
     * @return minimum distance between p and triangle points t_1, t_2 and t_3
     */
    private float DT(float px, float py, float t_1x, float t_1y, float t_2x, float t_2y, float t_3x, float t_3y) {
        float d1 = D(px, py, t_1x, t_1y);
        float d2 = D(px, py, t_2x, t_2y);
        float d3 = D(px, py, t_3x, t_3y);
        return Math.min(d1, Math.min(d2, d3));
    }

}
