package zendo.games.sandbox_gdx.graph_old;

import com.badlogic.gdx.math.Vector2;

import java.util.Comparator;

public class Edge {

    public int index;
    public Center d0, d1; // Delaunay edge
    public Corner v0, v1; // Voronoi edge
    public Vector2 midpoint; // halfway between v0,v1

    public Edge(int index) {
        this(index, null, null, null, null);
    }

    public Edge(int index, Center d0, Center d1, Corner v0, Corner v1) {
        this.index = index;
        this.d0 = d0;
        this.d1 = d1;
        this.v0 = v0;
        this.v1 = v1;
        if (v0 != null && v1 != null) {
            this.midpoint = new Vector2(v0.point.x + (v1.point.x - v0.point.x) * 0.5f,
                                        v0.point.y + (v1.point.y - v1.point.x) * 0.5f);
        } else {
            this.midpoint = null;
        }
    }

    public static Comparator<Edge> compareSitesDistances = new Comparator<Edge>() {
        @Override
        public int compare(Edge edge1, Edge edge2) {
            // TODO: write compare function
            return 0;
        }
    };

}
