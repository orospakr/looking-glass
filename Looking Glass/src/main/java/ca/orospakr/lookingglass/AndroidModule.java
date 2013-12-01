package ca.orospakr.lookingglass;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for Android-specific dependencies which require a {@link Context} or
 * {@link android.app.Application} to create.
 *
 * Necessary glue for Dagger; makes Android singletons available in the object graph.
 */
@Module(library = true)
public class AndroidModule {
    private final LookingGlassApp application;

    public AndroidModule(LookingGlassApp application) {
        this.application = application;
    }

    @Provides
    @Singleton
    Context provideApplicationContext() {
        return application;
    }
}