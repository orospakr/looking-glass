package ca.orospakr.lookingglass;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import spark.Request;
import spark.Response;

/**
 * Holds the sessions.  Persists them on disk.
 *
 * A pretty naiive implementation; not particularly atomic, all synchronous I/O.  We can get away with this because we're not clustering this at all.
 */
public class SessionManager {

    public static class UnauthorizedException extends Exception {
        public UnauthorizedException(String authority) {
            super("This session is not permitted to use " + authority);
        }
    }

    private static final String LOG_TAG = "LookingGlass/SessionManager";
    private static String COOKIE_KEY = "lg_cookie";

    private static String SESSIONS_PATH = "session";
    private final Context mContext;

    public static class Session {
        // per-authority permissions and such go here

        /** Content providers the device's owner has permitted this session to access. */
        ArrayList<String> permittedAuthorities = new ArrayList<String>();

        /** Content providers the device's owner has explicitly disallowed this session from accessing. */
        ArrayList<String> disallowedAuthorites = new ArrayList<String>();
    }

    public SessionManager(Context context) {
        mContext = context;
    }

    protected String createAuthToken() {
        byte[] bytes = new byte[25];
        new Random().nextBytes(bytes);
        try {
            return new String(Base64.encode(bytes, Base64.URL_SAFE), "UTF-8").trim();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Session newSession(Response response) {
        String authToken = createAuthToken();

        Session session = new Session();


        // save session on disk

        Gson gson = new Gson();

        FileWriter fw;
        BufferedWriter writer = null;
        try {
            fw = new FileWriter(sessionPath(authToken));
            writer = new BufferedWriter(fw);

            gson.toJson(session, writer);

            writer.close();
            fw.close();

            if(!sessionPath(authToken).exists()) {
                throw new RuntimeException("Apparently failed at creating session file.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Problem writing a session to private storage", e);
        }

        // TODO: black hat could spam the server and fill the private storage with sessions. simple rate limit may be easiest answer.  how does the java goopstack normally do it?

        // TODO: when I turn on https, enable secure cookie here
        response.cookie(COOKIE_KEY, authToken, 86400 * 365 * 5);

        return session;
    }

    private File sessionPath(String authToken) {
        // TODO: is this use of mContext a thread issue?
        File sessionsPath = mContext.getDir(SESSIONS_PATH, Context.MODE_PRIVATE);

        return new File(sessionsPath, authToken);
    }

    private Session retrieveSession(String authToken) {
        File sessionPath = sessionPath(authToken);
        if(sessionPath.exists()) {
            try {
                Gson gson = new Gson();
                try {
                    return gson.fromJson(new BufferedReader(new FileReader(sessionPath)), Session.class);
                } catch (JsonSyntaxException e) {
                    Log.e(LOG_TAG, "Bogus state file at " + authToken);
                    return null;
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Session file disappeared underneath my nose?", e);
            }
        } else {
            Log.d(LOG_TAG, "Session " + authToken + " does not exist.");
            return null;
        }
    }

    // Return our session object for the given request, creating a new one if need be.
    public Session get(Request request, Response response) {
        // does request have a session id?
        String authToken = request.cookie(COOKIE_KEY);
        if (authToken == null) {
            // needs an auth token
            Log.i(LOG_TAG, "Starting a new session for client " + request.ip());
            return newSession(response);
        } else {
            // does session exist?
            Session foundSession =  retrieveSession(authToken);
            if(foundSession == null) {
                Log.w(LOG_TAG, "Session client " + request.ip() + " claims to have had does not exist.  Starting new one.");
                return newSession(response);
            } else {
                return foundSession;
            }
        }
    }

    /** Throws exception if that Session is unauthorized.
     *
     * May block as it queries the user! */
    public void authorize(Session session, String authority) throws UnauthorizedException {
        // do nothing, thus authorizing everything for now
        if(session.disallowedAuthorites.contains(authority)) {
            throw new UnauthorizedException(authority);
        }
        if(!session.permittedAuthorities.contains(authority)) {
            // open the authorization activity, then wait for our AuthAnswerService to be told when.
            AuthAnswerService.putUpNotification(mContext, authority);

            // now block

        }
    }
}
