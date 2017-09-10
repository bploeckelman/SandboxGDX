package zendo.games.sandbox_gdx.graph_old;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Corner {

    public int index;
    public Vector2 point; // location
    public boolean border; // at the edge of the map

    // TODO: other fields describing this corner

    public Array<Center> touches;
    public Array<Corner> adjacent;
    public Array<Edge> portrudes;

    public Corner(int index, Vector2 point) {
        this.index = index;
        this.point = point;
        this.border = false;
        this.touches = new Array<Center>();
        this.adjacent = new Array<Corner>();
        this.portrudes = new Array<Edge>();
    }

}
