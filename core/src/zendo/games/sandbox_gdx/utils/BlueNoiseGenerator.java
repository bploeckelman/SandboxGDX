package zendo.games.sandbox_gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlueNoiseGenerator {

    private static final float default_annulus_radius = 10f;
    private static final int default_create_attempts = 30;

    private int numCreateAttempts;
    private int numBoundsSamples;
    private float annulusRadius;
    private List<Vector2> samples;
    private Rectangle bounds;

    public BlueNoiseGenerator(Rectangle bounds, int numBoundsSamples) {
        this(default_annulus_radius, default_create_attempts, bounds, numBoundsSamples);
    }

    public BlueNoiseGenerator(float annulusRadius, int numCreateAttempts, Rectangle bounds, int numBoundsSamples) {
        this.annulusRadius = annulusRadius;
        this.numCreateAttempts = numCreateAttempts;
        this.bounds = bounds;
        this.numBoundsSamples = numBoundsSamples;
        generate();
    }

    public void generate() {
        Vector2 initialSample = new Vector2(
                bounds.x + MathUtils.random(bounds.width),
                bounds.y + MathUtils.random(bounds.height)
        );
        samples = new ArrayList<Vector2>();
        samples.add(initialSample);

        if (numBoundsSamples > 0) {
            final int intervals = numBoundsSamples;
            final float interval_width = bounds.width / intervals;
            for (int i = 0; i <= intervals; ++i) {
                float x = i * interval_width;
                samples.add(new Vector2(bounds.x + x, bounds.y));
                samples.add(new Vector2(bounds.x + x, bounds.y + bounds.height));
            }
            final float interval_height = bounds.height / intervals;
            for (int i = 0; i < intervals; ++i) {
                float y = i * interval_height;
                samples.add(new Vector2(bounds.x, bounds.y + y));
                samples.add(new Vector2(bounds.x + bounds.width, bounds.y + y));
            }
        }

        List<Vector2> activeList = new ArrayList<Vector2>();
        activeList.add(initialSample);
        while (activeList.size() > 0) {
            int lastActiveIndex = activeList.size() - 1;
            int currentActiveIndex = MathUtils.random(lastActiveIndex);
            Collections.swap(activeList,  lastActiveIndex, currentActiveIndex);
            Vector2 currentSample = activeList.get(lastActiveIndex);

            boolean didCreateSample = false;
            for (int i = 0; i < numCreateAttempts; ++i) {
                float theta = MathUtils.random(360f);
                float radius = MathUtils.random(annulusRadius) + annulusRadius;
                Vector2 newSample = new Vector2(
                        radius * MathUtils.cosDeg(theta),
                        radius * MathUtils.sinDeg(theta)
                ).add(currentSample);

                boolean isValidSample = true;
                for (Vector2 existingSample : samples) {
                    if (newSample.dst(existingSample) <= annulusRadius) {
                        isValidSample = false;
                        break;
                    }
                }

                if (isValidSample) {
                    final float margin = 10f;
                    if (bounds.x + margin <= newSample.x && newSample.x < bounds.x + bounds.width - margin
                     && bounds.y + margin <= newSample.y && newSample.y < bounds.y + bounds.height - margin) {
                        samples.add(newSample);
                        activeList.add(newSample);
                        didCreateSample = true;
                    }
                }
            }

            if (!didCreateSample) {
                activeList.remove(lastActiveIndex);
            }
        }

        // Remove duplicate samples because I'm a terrible programmer
        for (int i = samples.size() - 1; i >= 0; --i) {
            boolean isDuplicate = false;
            for (int j = samples.size() - 1; j >= 0; --j) {
                if (i == j) continue;
                if (samples.get(i) == samples.get(j)) {
                    isDuplicate = true;
                    break;
                }
            }
            if (isDuplicate) {
                samples.remove(i);
            }
        }
    }

    public List<Vector2> getSamples() { return samples; }

}
