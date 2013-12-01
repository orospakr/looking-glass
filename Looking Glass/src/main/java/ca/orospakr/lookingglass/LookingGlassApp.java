package ca.orospakr.lookingglass;

import android.app.Application;

import java.util.Arrays;
import java.util.List;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * Primary Android singleton.
 */
public class LookingGlassApp extends Application {

    private ObjectGraph graph;

    @Override
    public void onCreate() {
        super.onCreate();
        graph = ObjectGraph.create(new AndroidModule(this), new LookingGlassModule());

    }

    public void inject(Object o) {
        graph.inject(o);
    }
}
