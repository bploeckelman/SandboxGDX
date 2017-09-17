package zendo.games.sandbox_gdx.dual_mesh;

import com.badlogic.gdx.utils.IntArray;

import java.util.Arrays;

/**
 * Port of https://github.com/redblobgames/dual-mesh
 * -------------------------------------------------
 *
 * Represent a triangle-polygon dual mesh with:
 *   - Regions (r)
 *   - Sides (s)
 *   - Triangles (t)
 *
 * Each element has an id:
 *   - 0 <= r < numRegions
 *   - 0 <= s < numSides
 *   - 0 <= t < numTriangles
 *
 * Naming convention: x_name_y takes x (r, s, t) as input and produces
 * y (r, s, t) as output. If the output isn't a mesh index (r, s, t)
 * then the _y suffix is omitted.
 *
 * A side is directed. If two triangles t0, t1 are adjacent, there will
 * be two sides representing the boundary, one for t0 and one for t1. These
 * can be accessed with s_inner_t and s_outer_t.
 *
 * A side also represents the boundary between two regions. If two regions
 * r0, r1 are adjacent, there will be two sides representing the boundary,
 * s_begin_r and s_end_r.
 *
 * Each side will have a pair, accessed with s_opposite_s.
 *
 * The mesh has no boundaries; it wraps around the "back" using a
 * "ghost" region. Some regions are marked as the boundary; these are
 * connected to the ghost region. Ghost triangles and ghost sides
 * connect these boundary regions to the ghost region. Elements that
 * aren't "ghost" are called "solid".
 */
public class TriangleMesh {

    static int s_to_t(int s)   { return (s / 3); }
    static int s_prev_s(int s) { return (s % 3 == 0) ? s+2 : s-1; }
    static int s_next_s(int s) { return (s % 3 == 2) ? s-2 : s+1; }

    int numBoundaryRegions;
    int numSolidSides;
    int[] r_vertex;       // TODO: type?
    int[] _s_start_r;     // TODO: type?
    int[] _s_opposite_s;  // TODO: type?

    int numSides;
    int numRegions;
    int numSolidRegions;
    int numTriangles;
    int numSolidTriangles;
    int[] _r_any_s;
    float[][] t_vertex; // TODO: type?

    public TriangleMesh(int numBoundaryRegions,
                        int numSolidSides,
                        int[] r_vertex,
                        int[] _s_start_r,
                        int[] _s_opposite_s) {
        // Object.assign(this, {numBoundaryRegions, numSolidSides, r_vertex, _s_start_r, _s_opposite_s});
        this.numBoundaryRegions = numBoundaryRegions;
        this.numSolidSides = numSolidSides;
        this.r_vertex = r_vertex;
        this._s_start_r = _s_start_r;
        this._s_opposite_s = _s_opposite_s;


        this.numSides = this._s_start_r.length;
        this.numRegions = this.r_vertex.length;
        this.numSolidRegions = this.numRegions - 1;
        this.numTriangles = this.numSides / 3;
        this.numSolidTriangles = this.numSolidSides / 3;

        // Construct an index for finding sides connected to a region
        this._r_any_s = new int[numRegions];
        for (int s = 0; s < _s_start_r.length; ++s) {
            // REPLACES: _r_any_s[_s_start_r[s]] || 0
            int value = 0;
            if (s < _s_start_r.length) {
                int index = _s_start_r[s];
                if (index >= 0 && index < _r_any_s.length) {
                    value = _r_any_s[index];
                }
            }

            _r_any_s[_s_start_r[s]] = value;
        }

        // Construct triangle coordinates
        this.t_vertex = new float[this.numTriangles][];
        for (int s = 0; s < _s_start_r.length; s += 3) {
            int[] a = { r_vertex[_s_start_r[s  ]], r_vertex[_s_start_r[s  ] + 1] };
            int[] b = { r_vertex[_s_start_r[s+1]], r_vertex[_s_start_r[s+1] + 1] };
            int[] c = { r_vertex[_s_start_r[s+2]], r_vertex[_s_start_r[s+2] + 1] };
            if (this.s_ghost(s)) {
                // ghost triangle center is just outside the unpaired side
                int dx = b[0]-a[0];
                int dy = b[1]-a[1];
                this.t_vertex[s/3] = new float[2];
                this.t_vertex[s/3][0] = a[0] + 0.5f * (dx+dy);
                this.t_vertex[s/3][1] = a[1] + 0.5f * (dy-dx);
            } else {
                // solid triangle center is at the centroid
                this.t_vertex[s/3] = new float[2];
                this.t_vertex[s/3][0] = (a[0] + b[0] + c[0]) / 3f;
                this.t_vertex[s/3][1] = (a[1] + b[1] + c[1]) / 3f;
            }
        }
    }

    int s_begin_r(int s)  { return _s_start_r[s]; }
    int s_end_r(int s)    { return _s_start_r[TriangleMesh.s_next_s(s)]; }

    int s_inner_t(int s)  { return TriangleMesh.s_to_t(s); }
    int s_outer_t(int s)  { return TriangleMesh.s_to_t(_s_opposite_s[s]); }

    int s_opposite_s(int s) { return _s_opposite_s[s]; }

    int[] t_circulate_s(int[] out_s, int t) { out_s = new int[3]; for (int i = 0; i < 3; i++) { out_s[i] = 3*t + i; } return out_s; }
    int[] t_circulate_r(int[] out_r, int t) { out_r = new int[3]; for (int i = 0; i < 3; i++) { out_r[i] = this._s_start_r[3*t+i]; } return out_r; }
    int[] t_circulate_t(int[] out_t, int t) { out_t = new int[3]; for (int i = 0; i < 3; i++) { out_t[i] = this.s_outer_t(3*t+i); } return out_t; }

    int[] r_circulate_s(int[] out_s, int r) {
        final int s0 = this._r_any_s[r];
        int s = s0;

        IntArray temp = new IntArray();
        do {
            temp.add(s);
            s = TriangleMesh.s_next_s(this._s_opposite_s[s]);
        } while (s != s0);

        out_s = new int[temp.items.length];
        out_s = Arrays.copyOf(temp.items, temp.items.length);
        return out_s;
    }

    int[] r_circulate_r(int[] out_r, int r) {
        final int s0 = this._r_any_s[r];
        int s = s0;

        IntArray temp = new IntArray();
        do {
            temp.add(this.s_end_r(s));
            s = TriangleMesh.s_next_s(this._s_opposite_s[s]);
        } while (s != s0);

        out_r = new int[temp.items.length];
        out_r = Arrays.copyOf(temp.items, temp.items.length);
        return out_r;
    }

    int[] r_circulate_t(int[] out_t, int r) {
        final int s0 = this._r_any_s[r];
        int s = s0;

        IntArray temp = new IntArray();
        do {
            temp.add(TriangleMesh.s_to_t(s));
            s = TriangleMesh.s_next_s(this._s_opposite_s[s]);
        } while (s != s0);

        out_t = new int[temp.items.length];
        out_t = Arrays.copyOf(temp.items, temp.items.length);
        return out_t;
    }

    int ghost_r() { return this.numRegions - 1; }
    boolean s_ghost(int s) { return s >= this.numSolidSides; }
    boolean r_ghost(int r) { return r == this.numRegions - 1; }
    boolean t_ghost(int t) { return s_ghost( 3 * t); }
    boolean s_boundary(int s) { return s_ghost(s) && (s % 3 == 0); }
    boolean r_boundary(int r) { return r < numBoundaryRegions; }

}
