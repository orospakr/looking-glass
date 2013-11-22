package ca.orospakr.lookingglass;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;

import spark.Request;
import spark.Response;
import spark.ResponseTransformerRoute;
import spark.Route;

import static spark.Spark.get;

/**
 * Our Service.
 */
public class HttpService extends Service {
    private static final String LOG_TAG = "LookingGlass/HttpService";

    private static abstract class CursorTransformerRoute extends ResponseTransformerRoute {

        // instead of using acceptType we're going to do a respond_to like thing here
        // and do our own switching for different accept types (json, CSV, HTML, etc.)...
        // ... actually, no, I can't do it here, I can't set the response content type from here.
        protected CursorTransformerRoute(String path) {
            super(path);
        }

        @Override
        public String render(Object o) {

            Cursor cursor = (Cursor)o;

            int columnCount = cursor.getColumnCount();

            StringWriter sw = new StringWriter();
            JsonWriter jw = new JsonWriter(sw);

            try {
                jw.beginArray();

                // TODO: paging

                // man that's some old-skool imperative iteration right there

                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {

                    jw.beginObject();

                    for(int c = 0; c < columnCount; c++) {
                        // TODO do column names need to be memoized for perf?

                        String columnName = cursor.getColumnName(c);

                        // Log.i(LOG_TAG, "COLUMN NAME: " + columnName);
                        jw.name(columnName);

                        switch (cursor.getType(c)) {
                            case Cursor.FIELD_TYPE_BLOB:
                                jw.value("BLOB");
                                 break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                jw.value(cursor.getFloat(c));
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                jw.value(cursor.getInt(c));
                                break;
                            case Cursor.FIELD_TYPE_NULL:
                                jw.nullValue();
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                jw.value(cursor.getString(c));
                            break;
                            default:
                                jw.nullValue();
                        }
                    }

                    jw.endObject();

                    cursor.move(1);
                }

                cursor.close();

                jw.endArray();

                jw.flush();
            } catch (Exception e) {
                // TODO: throw internal server error?
                // catching all exceptions here because Jetty/Spark's general exception handling
                // is subtly broken on Android (missing Java 7 classes, yeeeeg)
                Log.e(LOG_TAG, "ERROR " + e.toString());
                e.printStackTrace();
            }

           return sw.getBuffer().toString();
        }
    }

    public static void logRequest(Request request) {
        Log.i(LOG_TAG, "Servicing request for " + request.requestMethod() + " " + request.pathInfo());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(LOG_TAG, "STATING UP HTTP SERVER");

        get(new Route("/") {

            @Override
            public Object handle(Request request, Response response) {
                logRequest(request);
                return "Welcome to looking glass!";
            }
        });

        // TODO: endpoint for listing *all* installed content providers

        // TODO: endpoint(s) for single resources? /bookmarks/2?  could be handy for fast fetch of
        //       single things in the REST-style.  Content Providers, though, typically only use such paths
        // when doing subresources.  so, we could do this by adding our own appended path that just builds
        //  a query for that single row.

        get(new CursorTransformerRoute("/cp/:authority/:path") {

            @Override
            public Object handle(Request request, Response response) {
                logRequest(request);

                // time to whip myself up a contentresolver

                // TODO: listen on v6 if available

                // TODO: thread safety concerns?  what's spark's concurrency model?

                // TODO: build Uri properly

                // my god all of this is extremely dangerous

                // TODO: any way on a rooted phone to force through perms to query *all* content providers?  this would be badass

                // TODO: any way to discover all paths answered by a content provider?
                try {
                    Cursor fetched = HttpService.this.getContentResolver().query(Uri.parse("content://" + request.params(":authority") + "/" + request.params(":path")), null, null, null, null);
                    // return "Request for authority " + request.params(":authority") + " and path " + request.params(":path") + ", which got " + fetched.getCount() + " rows";
                    response.type("application/json");
                    return fetched;
                } catch(SecurityException e) {
                    response.status(500);
                    return "Looking Glass itself lacks permission to glimpse at that content provider: " + e.getMessage();
                }
            }
        });

    }

    public IBinder onBind(Intent intent) {

        // start up a Spark server


        return null;
    }
}
