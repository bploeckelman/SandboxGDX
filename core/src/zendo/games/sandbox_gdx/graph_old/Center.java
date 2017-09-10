package zendo.games.sandbox_gdx.graph_old;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Center {

    public int index;

    public Vector2 point; // location
    public boolean border; // at the edge of the map

    // TODO: other fields describing this cell center

    public Array<Center> neighbors;
    public Array<Corner> corners;
    public Array<Edge> borders;

    public Center(int index, Vector2 point) {
        this.index = index;
        this.point = point;
        this.border = false;
        this.neighbors = new Array<Center>();
        this.corners = new Array<Corner>();
        this.borders = new Array<Edge>();
    }

}
