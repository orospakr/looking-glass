package ca.orospakr.lookingglass;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

/**
 * Responsible for getting the answer from the notifications kicked up by the SessionManager over in SessionManager.
 */
public class AuthAnswerService extends Service {

    public static final String FIELD_AUTHORITY_ARGUMENT = "authority";
    public static final String FIELD_ANSWER = "answer";

    private static final String LOG_TAG = "LookingGlass/AuthAnswerService";

    // SINGLETON, expected to live the lifetime of the :sync process.  somewhere better to put it than static, which is kind of sucky?  make it a singleton in the dagger graph?
    public static ConcurrentHashMap<String, Future<Boolean>> blockedRequests = new ConcurrentHashMap<String, Future<Boolean>>();

    @Inject
    public SessionManager sessionManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ((LookingGlassApp) getApplication()).inject(this);
        int r = super.onStartCommand(intent, flags, startId);

        Log.d(LOG_TAG, "GOT MY SESSION MANAGER: " + sessionManager);

        // here I'll get my answer from the notification.  pull the value back out!

        Bundle extras = intent.getExtras();

        String authority = extras.getString(FIELD_AUTHORITY_ARGUMENT);
        String answer = extras.getString(FIELD_ANSWER);
        Log.d(LOG_TAG, extras.toString());

        Log.d(LOG_TAG, "GOT REQUEST TO " + answer + ": " + authority);

        // TODO write the setting via SessionManager!

        // then resolve any blocked requests
        blockedRequests.get(authority);

        return r;
    }

    public static void putUpNotification(Context context, String authority) {
        Notification.BigTextStyle style = new Notification.BigTextStyle();
        style.bigText("A device would like to look at " + authority);
        style.setBigContentTitle("Looking Glass - Request");

        Notification.Builder notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Looking Glass - Request")
                .setContentText("A device would like to look at " + authority)
                .setTicker("Allow someone to look at " + authority + "?")
                .setStyle(style);


        Intent denyIntent = new Intent();
        denyIntent.setClass(context, AuthAnswerService.class);

        Bundle denyExtras = new Bundle();
        denyExtras.putString(FIELD_AUTHORITY_ARGUMENT, authority);
        denyExtras.putString(FIELD_ANSWER, "deny");
        denyIntent.putExtras(denyExtras);


        Intent allowIntent = new Intent();
        allowIntent.setClass(context, AuthAnswerService.class);
        Bundle allowExtras = new Bundle();
        allowExtras.putString(FIELD_ANSWER, "allow");
        allowExtras.putString(FIELD_AUTHORITY_ARGUMENT, authority);
        allowIntent.putExtras(allowExtras);

        // with this arrangement, this will cause existing notifications for another service to be destroyed if the user hasn't answered the yet.  Could Fix this by setting a request code that is a hash of the authority.
        PendingIntent denyAction = PendingIntent.getService(context, 0, denyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent allowAction = PendingIntent.getService(context, 1, allowIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.addAction(android.R.drawable.ic_lock_lock, "Deny", denyAction);
        notificationBuilder.addAction(R.drawable.ic_cab_done_holo_dark, "Allow", allowAction);
        NotificationManager notificationThingy = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationThingy.notify(56356, notificationBuilder.build());

        Object answerBlockLock = new Object();



        // first, create a callback that waits above.  however, it can only be for returning the value to the blocked consumer.  I still want the update of the setting to occur, in case the pendingintent is finally fired long after the process is gone.

        Future f = new Future<String>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                // this one is ill-advised.  print a warning?
                return null;
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }
}