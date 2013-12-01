package ca.orospakr.lookingglass;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Main ownership point of Looking Glass' own objects.
 *
 * The two services are included in the injects list because they are instantiated by the Andorid platform, and are responsible for injecting themselves into the object graph.
 */
@Module(addsTo = AndroidModule.class, injects = {AuthAnswerService.class, HttpService.class})
public class LookingGlassModule {

}
