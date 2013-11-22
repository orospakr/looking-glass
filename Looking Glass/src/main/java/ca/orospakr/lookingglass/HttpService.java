package ca.orospakr.lookingglass;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import spark.Request;
import spark.Response;
import spark.Route;

import static spark.Spark.get;

/**
 * Our Service.
 */
public class HttpService extends Service {
    private static final String LOG_TAG = "LookingGlass/HttpService";


    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(LOG_TAG, "STATING UP HTTP SERVER");

        get(new Route("/") {

            @Override
            public Object handle(Request request, Response response) {
                return "POOP";
            }
        });
    }

    public IBinder onBind(Intent intent) {

        // start up a Spark server


        return null;
    }
}
