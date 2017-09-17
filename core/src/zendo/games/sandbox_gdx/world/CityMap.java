package zendo.games.sandbox_gdx.world;

import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.Array;

public class CityMap {

    /*
    public class Road {
        public void render(SpriteBatch batch) {
        }
    }
    Array<Road> roads;

    public class Patch {
        public void render(SpriteBatch batch) {
            // switch (patch.ward.type) { case Castle: ... case Cathedral: ... case OtherWardTypes: ... }
        }
    }
    Array<Patch> patches;

    public class Ward {
        static final float main_street = 2.0f;
        static final float regular_street = 1.0f;
        static final float alley = 0.6f;

//        Model model;
        Patch patch;
        Array<ZenPolygon> geometry;

        public Ward(Patch patch) {
            this.patch = patch;
            this.geometry = new Array<ZenPolygon>();
        }

        public void createGeometry() {
            if (geometry == null) {
                geometry = new Array<ZenPolygon>();
            }
            geometry.clear();
        }

        // static
        public Array<ZenPolygon> createOrthoBuilding(ZenPolygon poly, float minBlockSq, float fill) {
            Array<ZenPolygon> buildings = new Array<ZenPolygon>();
            // ...
            return buildings;
        }

    }

    public CityMap() {
        roads = new Array<Road>();
        patches = new Array<Patch>();
    }
    */

    public void update(float dt) {

    }

    public void render(SpriteBatch batch) {
//        for (Road road : roads) {
//            road.render(batch);
//        }
//        for (Patch patch : patches) {
//            patch.render(batch);
//        }
    }

}
