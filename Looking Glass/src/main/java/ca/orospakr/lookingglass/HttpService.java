package ca.orospakr.lookingglass;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PathPermission;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.util.component.LifeCycle;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

    @Inject
    public SessionManager mSessionManager;

    public static Object executeWithErrorHandling(Response response, Callable<Object> cb) {
        try {
            return cb.call();
        } catch (SecurityException e) {
            response.status(401);
            return "Looking Glass itself lacks permission to glimpse at that content provider: " + e.getMessage();
        } catch (SessionManager.UnauthorizedException e) {
            response.status(403);
            return "Phone's owner denied the request.";
        } catch (Exception e) {
            response.status(500);
            e.printStackTrace();
            // TODO: with my upcoming security stuff, logging exception in response might be OK
            return "You've found a bug.  Check lolcat for details.";
        }
    }

    protected void authorize(String authority, Request request, Response response) throws SessionManager.UnauthorizedException{
        SessionManager.Session session = mSessionManager.get(request, response);

        mSessionManager.authorize("", session, authority);
    }

    private static abstract class CursorTransformerRoute extends ResponseTransformerRoute {

        // instead of using acceptType we're going to do a respond_to like thing here
        // and do our own switching for different accept types (json, CSV, HTML, etc.)...
        // ... actually, no, I can't do it here, I can't set the response content type from here.
        protected CursorTransformerRoute(String path) {
            super(path);
        }

        @Override
        public String render(Object o) {

            // just pass through a string if we got one (the path handler returns an informative
            // error string if it gets an error, mainly)
            if (o instanceof String) {
                return (String)o;
            }

            Cursor cursor = (Cursor)o;

            int columnCount = cursor.getColumnCount();

            // rather than buffering all this crap, can/should I put Spark into chunked and stream it?
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
                // TODO: throw internal server error from here?
                // catching all exceptions here because Jetty/Spark's general exception handling
                // is subtly broken on Android (missing Java 7 classes, yeeeeg)
                Log.e(LOG_TAG, "ERROR " + e.toString());
                e.printStackTrace();
                return null;
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

        ((LookingGlassApp) getApplication()).inject(this);

        Log.i(LOG_TAG, "STATING UP HTTP SERVER, session manager is " + mSessionManager);

        // mSessionManager = new SessionManager(this);

        get(new Route("/") {

            @Override
            public Object handle(Request request, Response response) {
                logRequest(request);
                return "Welcome to looking glass!";
            }
        });

        // get a list of all installed providers and their details (ProviderInfos)
        get(new Route("/cp") {

            @Override
            public Object handle(Request request, Response response) {
                logRequest(request);

                return executeWithErrorHandling(response, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        PackageManager pm = HttpService.this.getApplicationContext().getPackageManager();

                        ArrayList<ProviderInfo> installedProviders = new ArrayList<ProviderInfo>();

                        for(PackageInfo pack : pm.getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                            // installedProviders.addAll(pack.providers); lol, why do I have to do this instead:
                            if(pack.providers == null) {
                                continue;
                            }
                            for (ProviderInfo pi : pack.providers) {
                                installedProviders.add(pi);
                            }
                        }

                        Gson gson = new Gson();

                        return gson.toJson(installedProviders);
                    }
                });

            }
        });

        // poop out a list of all possible Provider permissions as required, for convenient pasta into AndroidManifest.xml
        get(new Route("/util/needed_permissions") {
            @Override
            public Object handle(Request request, Response response) {
                logRequest(request);

                response.type("text/plain");

                return executeWithErrorHandling(response, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {

                        HashSet<String> foundPermissions = new HashSet<String>();


                        StringBuilder sb = new StringBuilder();
                        PackageManager pm = HttpService.this.getApplicationContext().getPackageManager();

                        for(PackageInfo pack : pm.getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                            if(pack.providers != null) {
                                for (ProviderInfo provider : pack.providers) {

                                    if(provider.pathPermissions != null) {
                                        for( PathPermission pi : provider.pathPermissions) {
                                            foundPermissions.add(pi.getReadPermission());
                                            foundPermissions.add(pi.getWritePermission());


                                        }
                                    }

                                    if(provider.readPermission != null) {

                                        foundPermissions.add(provider.readPermission);
                                    }

                                    if(provider.writePermission != null) {
                                        foundPermissions.add(provider.writePermission);
                                    }
                                }

                            }
                        }

                        for(String perm : foundPermissions) {
                            if(perm == null)
                                continue;
                            sb.append("<uses-permission android:name=\"");
                            sb.append(perm);

                            sb.append("\" />\n");
                        }


                        return sb.toString();
                    }
                });
            }
        });

        // get details (ProviderInfo) of a given authority.
        get(new Route("/cp/:authority") {

            @Override
            public Object handle(final Request request, final Response response) {
                logRequest(request);

                final String authority = request.params(":authority");

                return executeWithErrorHandling(response, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        PackageManager pm = HttpService.this.getApplicationContext().getPackageManager();
                        ProviderInfo pi = pm.resolveContentProvider(authority, 0);

                        if(pi == null) {
                            response.status(404);
                            return "That content provider does not exist.";
                        }
                        Gson gson = new Gson();

                        response.type("application/json");
                        return gson.toJson(pi);
                    }
                });
            }
        });

        // TODO: endpoint for listing *all* installed content providers

        // TODO: endpoint(s) for single resources? /bookmarks/2?  could be handy for fast fetch of
        //       single things in the REST-style.  Content Providers, though, typically only use such paths
        // when doing subresources.  so, we could do this by adding our own appended path that just builds
        //  a query for that single row.

        get(new CursorTransformerRoute("/cp/:authority/:path") {

            @Override
            public Object handle(final Request request, final Response response) {
                logRequest(request);

                final String authority = request.params(":authority");



                return executeWithErrorHandling(response, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {

                        authorize(authority, request, response);
                        // time to whip myself up a contentresolver

                        // TODO: listen on v6 if available

                        // TODO: thread safety concerns?  what's spark's concurrency model?

                        // TODO: build Uri properly

                        // my god all of this is extremely dangerous

                        // TODO: any way on a rooted phone to force through perms to query *all* content providers?  this would be badass

                        // TODO: any way to discover all paths answered by a content provider?

                            Cursor fetched = HttpService.this.getContentResolver().query(Uri.parse("content://" + authority + "/" + request.params(":path")), null, null, null, null);
                            // return "Request for authority " + request.params(":authority") + " and path " + request.params(":path") + ", which got " + fetched.getCount() + " rows";
                            response.type("application/json");
                            return fetched;

                    }
                });


            }
        });
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
