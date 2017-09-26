package zendo.games.sandbox_gdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;

import java.util.*;

public class ConcaveHull {




    /*
    public FloatArray convexVertices;
    public FloatArray concaveVertices;
    public FloatArray vertices;
    public IntArray innerPoints;
    */

    public class Edge {
        private final FloatArray vertices;
        public int index1;
        public int index2;

        public Edge(int index1, int index2, FloatArray vertices) {
            this.vertices = vertices;
            this.index1 = index1;
            this.index2 = index2;
        }

        public Edge(Edge edge) {
            this.vertices = edge.vertices;
            this.index1 = edge.index1;
            this.index2 = edge.index2;
        }

        public float length() {
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
    }

    LinkedList<Edge> convexHullEdges;
    LinkedList<Edge> concaveHullEdges;


    FloatArray convexHullVertices;
    IntArray convexHullIndices;
    IntArray concaveHullIndices;
    FloatArray vertices;
    IntArray innerPoints;


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

        // Generate convex hull edge list
        convexHullEdges = new LinkedList<Edge>();
        for (int i = 0; i < convexHullIndices.size - 1; ++i) {
            convexHullEdges.add(new Edge(convexHullIndices.get(i), convexHullIndices.get(i + 1), vertices));
        }

        // Sort working edge list by edge length
        Comparator<Edge> edgeLengthComparator = new Comparator<Edge>() {
            @Override
            public int compare(Edge edge1, Edge edge2) {
                return Float.compare(edge2.length(), edge1.length());
            }
        };
        // Sort inner points list by distance to edge

        // Process edges
        concaveHullEdges = new LinkedList<Edge>();
        LinkedList<Edge> edges = new LinkedList<Edge>(convexHullEdges);
        while (!edges.isEmpty()) {
            Collections.sort(edges, edgeLengthComparator);
            Edge edge = edges.pop();
            Gdx.app.log("ProcessingEdges", "edge: " + edge.length());

            // TODO: Calculate local max distance d for edges
            // For now, just set a standard max length
            float d = 19f;

            Edge edge1, edge2;
            boolean didAddNewEdges = false;
            if (edge.length() > d) {
                // Fetch the vertices for edge
                final float e1_x = vertices.items[edge.index1 * 2];
                final float e1_y = vertices.items[edge.index1 * 2 + 1];
                final float e2_x = vertices.items[edge.index2 * 2];
                final float e2_y = vertices.items[edge.index2 * 2 + 1];

                // Sort  ----------------------------------------------------
                // Sort points by distance to edge then evaluate them in that order to find a candidate
                Integer[] sortedInnerPoints = new Integer[innerPoints.size];
                for (int i = 0; i < innerPoints.size; ++i) {
                    sortedInnerPoints[i] = innerPoints.get(i);
                }
                final FloatArray verts = vertices;
                Arrays.sort(sortedInnerPoints, new Comparator<Integer>(){
                    @Override
                    public int compare(Integer i1, Integer i2) {
                        float p1_x = verts.get(i1 * 2);
                        float p1_y = verts.get(i1 * 2 + 1);
                        float p2_x = verts.get(i2 * 2);
                        float p2_y = verts.get(i2 * 2 + 1);
                        float p1_dist = DE(p1_x, p1_y, e1_x, e1_y, e2_x, e2_y);
                        float p2_dist = DE(p2_x, p2_y, e1_x, e1_y, e2_x, e2_y);
                        return Float.compare(p1_dist, p2_dist);
                    }
                });
                for (int i = 0; i < sortedInnerPoints.length; ++i) {
                    float p1_x = verts.get(i * 2);
                    float p1_y = verts.get(i * 2 + 1);

                    float p1_dist = DE(p1_x, p1_y, e1_x, e1_y, e2_x, e2_y);
                    Gdx.app.log("DEBUG", "sortedInnerPoints dist to edge: " + p1_dist + ", for edge with length: " + edge.length());
                }

                // ----------------------------------------------------------

                // Find the point p : innerPoints with the smallest max angle 'a'
                float minAngle = Float.MAX_VALUE;
                int minAngleInnerPointsIndex = -1;
//                for (int i = 0; i < sortedInnerPoints.length; ++i) {
                for (int i = 0; i < innerPoints.size; ++i) {
//                    int innerPointIndex = innerPoints.items[i];
                    int innerPointIndex = sortedInnerPoints[i];
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
                Gdx.app.log("DEBUG", "minAngle: " + minAngle);

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
                        Gdx.app.log("ProcessingEdges", "added to list: Edge@" + System.identityHashCode(edge1) + ", Edge@" + System.identityHashCode(edge2));
                    }
                }
            }

            // if edge1 and edge2 was not added to edges
            if (!didAddNewEdges) {
                // add edge to list concaveHullEdges
                concaveHullEdges.add(edge);
                Gdx.app.log("ProcessingEdges", "(did not dig) added to hull: Edge@" + System.identityHashCode(edge));
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

    private boolean doEdgesIntersectOtherEdges(Edge edge1, Edge edge2, LinkedList<Edge> edges) {
        float e1a_x = vertices.items[edge1.index1 * 2];
        float e1a_y = vertices.items[edge1.index1 * 2 + 1];
        float e1b_x = vertices.items[edge1.index2 * 2];
        float e1b_y = vertices.items[edge1.index2 * 2 + 1];

        float e2a_x = vertices.items[edge2.index1 * 2];
        float e2a_y = vertices.items[edge2.index1 * 2 + 1];
        float e2b_x = vertices.items[edge2.index2 * 2];
        float e2b_y = vertices.items[edge2.index2 * 2 + 1];

        for (Edge edge : edges) {
            float ea_x = vertices.items[edge.index1 * 2];
            float ea_y = vertices.items[edge.index1 * 2 + 1];
            float eb_x = vertices.items[edge.index2 * 2];
            float eb_y = vertices.items[edge.index2 * 2 + 1];

            if (Intersector.intersectSegments(ea_x, ea_y, eb_x, eb_y, e1a_x, e1a_y, e1b_x, e1b_y, null)
             || Intersector.intersectSegments(ea_x, ea_y, eb_x, eb_y, e2a_x, e2a_y, e2b_x, e2b_y, null)) {
                Gdx.app.log("DEBUG", "did intersect other edges");
                return true;
            }
        }

        Gdx.app.log("DEBUG", "did not intersect other edges");
        return false;
    }

/*
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


/*
    public void secondImplementation(List<Vector2> pointsList, float N) {
        // Copy pointsList to FloatArray for convex hull generation
        vertices = new FloatArray(pointsList.size() * 2);
        for (Vector2 point : pointsList) {
            vertices.addAll(point.x, point.y);
        }
        vertices.shrink();

        // TODO: temp
        convexVertices = new FloatArray(vertices);

        // Compute convex hull vertices / indices
        final ConvexHull convexHull = new ConvexHull();
        FloatArray convexHullVertices = new FloatArray(convexHull.computePolygon(vertices, false).shrink());
        IntArray   convexHullIndices  = new IntArray(convexHull.computeIndices(vertices, false, false).shrink());
        // NOTE: indices refer to original vertices array; index i -> vertices[i*2],[i*2+1]
        // NOTE: edges then are pairs of indices, indices[0,1],[1,2],...,[n-2,n-1]
        // NOTE: indices wraps, ie. indices[0] = indices[n-1]

        // Generate edge list for convex hull
        convexHullEdges = new LinkedList<Edge>();
        for (int i = 0; i < convexHullIndices.size - 1; ++i) {
            Edge edge = new Edge(convexHullIndices.get(i), convexHullIndices.get(i+1));
            convexHullEdges.add(edge);
        }

        // Generate inner points list (ie. all points from vertices that are not a part of the convex hull)
//        IntArray innerPoints = new IntArray();
        innerPoints = new IntArray();
        for (int i = 0; i < vertices.size; i += 2) {
            float vx = vertices.get(i);
            float vy = vertices.get(i+1);
            boolean isPointInConvexHull = false;
            for (int j = 0; j < convexHullVertices.size; j += 2) {
                float cvx = convexHullVertices.get(j);
                float cvy = convexHullVertices.get(j+1);
                if (vx == cvx && vy == cvy) {
                    isPointInConvexHull = true;
                }
            }
            if (!isPointInConvexHull) {
                innerPoints.add(i / 2);
            }
        }
        innerPoints.shrink();

        // TODO: keep a list of edges to assess, handle these independently of the actual concave edge list
        // TODO: this way we can recursively keep checking new edges until we're out of edges or inner points to check
        // TODO: while not screwing with the edge ordering in the main concave edge list
        LinkedList<Edge> edgeList = new LinkedList<Edge>(convexHullEdges);

        // Initialize concave hull edge list with copy of convex hull edges
        concaveHullEdges = new LinkedList<Edge>(convexHullEdges);

        // Dig into edges
        while (!edgeList.isEmpty() && innerPoints.size > 0) {
            // Get edge
            Edge edge = edgeList.getFirst();

            // Get edge 1 points
            float c_i1x = vertices.get(edge.index1 * 2);
            float c_i1y = vertices.get(edge.index1 * 2 + 1);

            // Get edge 2 points
            float c_i2x = vertices.get(edge.index2 * 2);
            float c_i2y = vertices.get(edge.index2 * 2 + 1);

            // Find nearest inner point p_k from edge (c_i1, c_i2)
            // NOTE: p_k should _not_ be closer to neighbor edges than to (c_i1, c_i2)
            int p_i = findNearestInnerPointFromEdge(innerPoints, vertices, edge);
            if (p_i > -1) {
                float p_kx = vertices.get(p_i * 2);
                float p_ky = vertices.get(p_i * 2 + 1);

                //   calculate eh = D(c_i1, c_i2); // the length of the edge
                float eh = D(c_i1x, c_i1y, c_i2x, c_i2y);
                //   calculate dd = DD(p_k, {c_i1, c_i2});
                FloatArray q = new FloatArray(new float[]{c_i1x, c_i1y, c_i2x, c_i2y});
                float dd = DD(p_kx, p_ky, q);
                float n = eh / dd;

                //   if (eh / dd) > N // digging process
                if (n > N) {
                    // create new edges (c1,p) and (p, c2)
                    Edge edge_c1p = new Edge(edge.index1, p_i);
                    Edge edge_c2p = new Edge(p_i, edge.index2);

                    // insert new edges into tail of edgeList for later processing
                    edgeList.addLast(edge_c1p);
                    edgeList.addLast(edge_c2p);

                    // replace current edge from concaveList with new edges
                    int index = concaveHullEdges.indexOf(edge);
                    concaveHullEdges.add(index + 1, edge_c1p);
                    concaveHullEdges.add(index + 2, edge_c2p);
                    concaveHullEdges.remove(index);

                    // delete current edge from ConcaveList
                    edgeList.remove(edge);

                    // remove p_i from innerPoints list
                    innerPoints.removeValue(p_i);
                    innerPoints.shrink();
                } else {
                    // remove current edge from edgeList
                    edgeList.remove(edge);
                }
            } else {
                // remove current edge from edgeList
                edgeList.remove(edge);
            }
        }
    }
*/

    private int findNearestInnerPointFromEdge(IntArray innerPoints, FloatArray vertices, Edge edge) {
        final float c_i1x = vertices.get(edge.index1 * 2);
        final float c_i1y = vertices.get(edge.index1 * 2 + 1);
        final float c_i2x = vertices.get(edge.index2 * 2);
        final float c_i2y = vertices.get(edge.index2 * 2 + 1);

        /*
        // TODO: only consider innerPoints that are closer to 'edge' than any other edge in the current list
        IntArray innerPointIndicesToConsider = new IntArray();
        for (int i = 0; i < innerPoints.size; ++i) {
            float px = vertices.get(innerPoints.get(i) * 2);
            float py = vertices.get(innerPoints.get(i) * 2 + 1);

            float dist = DE(px, py, c_i1x, c_i1y, c_i2x, c_i2y);
            boolean isCloser = true;
            for (Edge otherEdge : concaveHullEdges) {
                if (edge == otherEdge) continue;

                float e_1x = vertices.get(otherEdge.index1 * 2);
                float e_1y = vertices.get(otherEdge.index1 * 2 + 1);
                float e_2x = vertices.get(otherEdge.index2 * 2);
                float e_2y = vertices.get(otherEdge.index2 * 2 + 1);

                float d = DE(px, py, e_1x, e_1y, e_2x, e_2y);
                if (d < dist) {
                    isCloser = false;
                    break;
                }
            }

            if (isCloser) {
                innerPointIndicesToConsider.add(i);
            }
        }
        */
        // TODO: sort points by distance to edge then evaluate them in that order to find a candidate
        Integer[] sortedInnerPoints = new Integer[innerPoints.size];
        for (int i = 0; i < innerPoints.size; ++i) {
            sortedInnerPoints[i] = innerPoints.get(i);
        }
        final FloatArray verts = vertices;
        Arrays.sort(sortedInnerPoints, new Comparator<Integer>(){
            @Override
            public int compare(Integer i1, Integer i2) {
                float p1_x = verts.get(i1 * 2);
                float p1_y = verts.get(i1 * 2 + 1);
                float p2_x = verts.get(i2 * 2);
                float p2_y = verts.get(i2 * 2 + 1);
                float p1_dist = DE(p1_x, p1_y, c_i1x, c_i1y, c_i2x, c_i2y);
                float p2_dist = DE(p2_x, p2_y, c_i1x, c_i1y, c_i2x, c_i2y);
                return Float.compare(p1_dist, p2_dist);
            }
        });

        int minDIndex = -1;
        float minD = Float.MAX_VALUE;
        for (int i = 0; i < sortedInnerPoints.length; ++i) {

            float s_px = vertices.get(sortedInnerPoints[i] * 2);
            float s_py = vertices.get(sortedInnerPoints[i] * 2 + 1);

            float D = DE(s_px, s_py, c_i1x, c_i1y, c_i2x, c_i2y);
            if (D < minD) {
                minD = D;
                minDIndex = i;
            }
        }

        return (minDIndex == -1) ? -1 : sortedInnerPoints[minDIndex];

        /*
        for (int i = 0; i < innerPoints.size; ++i) {
//            if (!innerPointIndicesToConsider.contains(i)) continue;

            float s_px = vertices.get(innerPoints.get(i) * 2);
            float s_py = vertices.get(innerPoints.get(i) * 2 + 1);

            float D = DE(s_px, s_py, c_i1x, c_i1y, c_i2x, c_i2y);
            if (D < minD) {
                minD = D;
                minDIndex = i;
            }
        }

        return (minDIndex == -1) ? -1 : innerPoints.get(minDIndex);
        */
    }

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

/*
    public void firstImplementation(Array<Vector2> pointsList, float N) {
        // Copy source points list items into a FloatArray
        pointsList.shrink();
        final int capacity = pointsList.size * 2;
        final FloatArray sourcePoints = new FloatArray(capacity);
        for (Vector2 point : pointsList) {
            sourcePoints.addAll(point.x, point.y);
        }

        // Generate ConvexList(G)
        final FloatArray convexList = convexList(sourcePoints);
        this.convexVertices = convexList;

        // NOTE: extra work to make inner point lookup easier later
        final FloatArray sourcePointsExcludingConvexPoints = new FloatArray();
        for (int i = 0; i < sourcePoints.items.length; i += 2) {
            float s_px = sourcePoints.items[i];
            float s_py = sourcePoints.items[i+1];
            boolean isInConvexList = false;
            for (int j = 0; j < convexList.items.length; j += 2) {
                float c_px = convexList.items[j];
                float c_py = convexList.items[j+1];
                if (s_px == c_px && s_py == c_py) {
                    isInConvexList = true;
                }
            }
            if (!isInConvexList) {
                sourcePointsExcludingConvexPoints.addAll(s_px, s_py);
            }
        }
        sourcePointsExcludingConvexPoints.shrink();

        // Choose threshold N
        // [INPUT]

        // Copy ConvexList(G) to ConcaveList
        // NOTE: skipping this step because we don't want to alter a float array in place
        // NOTE: rather we'll populate this list with values from ConvexList
        // NOTE: and the new edges added by digging, as they are added
        FloatArray concaveList = new FloatArray();

        // Dig into ConvexList

        // for i = 1 to end of ConcaveList
        for (int i = 0; i < convexList.items.length; i += 2) {
            // get edge 1 points
            float c_i1x = convexList.items[i];
            float c_i1y = convexList.items[i+1];

            // get edge 2 points
            int i2 = (i + 2) % convexList.items.length;
            float c_i2x = convexList.items[i2];
            float c_i2y = convexList.items[i2+1];

            // TODO:
            //   find nearest inner point p_k of G from edge (c_i1, c_i2)
            //   // p_k should _not_ be closer to neighbor edges of (c_i1, c_i2) than (c_i1, c_i2)
            Vector2 p = new Vector2();
            int p_i = findNearestInnerPointFromEdge(sourcePointsExcludingConvexPoints, convexList, i, i2, p);
            if (p_i == -1) {
                // no more inner points available
                concaveList.addAll(c_i1x, c_i1y, c_i2x, c_i2y);
                Gdx.app.log("ConcaveHull", "Added existing convex edge");
            } else {
                float p_kx = p.x;
                float p_ky = p.y;

                //   calculate eh = D(c_i1, c_i2); // the length of the edge
                float eh = D(c_i1x, c_i1y, c_i2x, c_i2y);
                //   calculate dd = DD(p_k, {c_i1, c_i2});
                FloatArray q = new FloatArray(new float[]{c_i1x, c_i1y, c_i2x, c_i2y});
                float dd = DD(p_kx, p_ky, q);

                //   if (eh / dd) > N // digging process
                if (eh / dd > N) {
                    // TODO: can't add and remove willy-nilly on this list while traversing it
                    // TODO: note, this add-edges-as-we-go approach won't work as we're not redigging the newly added edges
                    // TODO: as we would be if they were adding to the end of the list we're already processing
                    //     insert new edges (c_i1, k) and (c_i2, k) into tail of ConcaveList
                    concaveList.addAll(
                            c_i1x, c_i1y, p_kx, p_ky,
                            p_kx, p_ky, c_i2x, c_i2y
                    );
                    sourcePointsExcludingConvexPoints.removeIndex(p_i + 1);
                    sourcePointsExcludingConvexPoints.removeIndex(p_i);
                    sourcePointsExcludingConvexPoints.shrink();
                    Gdx.app.log("ConcaveHull", "Added two new edges");

                    // NOTE: no longer needed, we just don't add that exiting edge when we're creating new ones
                    //     delete edge (c_i1, c_i2) from ConcaveList
                } else {
                    // No new edge dug out
                    // insert existing edge (c_i1, c_i2) into tail of ConcaveList
                    concaveList.addAll(c_i1x, c_i1y, c_i2x, c_i2y);
                    Gdx.app.log("ConcaveHull", "Added existing convex edge");
                }
            }
        }

        // return ConcaveList
        this.concaveVertices = new FloatArray(concaveList.shrink());


        // NOTE: old
        // For each edge of the convex hull e_i = ({v_1x, v_1y}, {v_2x, v_2y)
        //    Find the nearest inner points from convex hull edge e_n
        //    For each inner point p_i
        //        dist_1 = dist(v_1, p_i)
        //        dist_2 = dist(v_2, p_i)
        //        decision_dist = min(dist_1, dist_2)
        //        if (length(e_i) / (decision_dist) > N) {
        //          do the dig
    }
*/

    private int findNearestInnerPointFromEdge(FloatArray sourcePointsExcludingConvexPoints,
                                              FloatArray convexList, int i1, int i2,
                                              Vector2 p) {
        float c_i1x = convexList.items[i1];
        float c_i1y = convexList.items[i1+1];
        float c_i2x = convexList.items[i2];
        float c_i2y = convexList.items[i2+1];

        int minDIndex = -1;
        float minD = Float.MAX_VALUE;
        for (int i = 0; i < sourcePointsExcludingConvexPoints.items.length; i += 2) {
            float s_px = sourcePointsExcludingConvexPoints.items[i];
            float s_py = sourcePointsExcludingConvexPoints.items[i+1];

            float D = DE(s_px, s_py, c_i1x, c_i1y, c_i2x, c_i2y);
            if (D < minD) {
                minD = D;
                minDIndex = i;
            }
        }
        if (minDIndex >= 0 && minDIndex < sourcePointsExcludingConvexPoints.items.length) {
            p.set(sourcePointsExcludingConvexPoints.items[minDIndex],
                    sourcePointsExcludingConvexPoints.items[minDIndex + 1]);
        }

        return minDIndex;
    }

    /**
     * ConvexList(G) = list of points in convex hull surrounding sourcePoints
     * @param sourcePoints the set of source points to get a convex hull for
     * @return the list of points in the convex hull
     */
    private FloatArray convexList(FloatArray sourcePoints) {
        final ConvexHull convexHull = new ConvexHull();
        final FloatArray convexHullVertices = new FloatArray(convexHull.computePolygon(sourcePoints, false));
        final IntArray   convexHullIndices  = new IntArray(convexHull.computeIndices(sourcePoints, false, false));
        Gdx.app.log("ConvexList",
                "Generated " + convexHullVertices.items.length + " concaveVertices"
                  + " and " + convexHullIndices.items.length + " indices");
        return convexHullVertices;
    }

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
        float d1 = D(px, py, e_1x, e_1y);
        float d2 = D(px, py, e_2x, e_2y);
        return Math.min(d1, d2);
    }

    /**
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
