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

/**
 * Responsible for getting the answer from the notifications kicked up by the SessionManager over in SessionManager.
 */
public class AuthAnswerService extends Service {

    public static final String FIELD_AUTHORITY_ARGUMENT = "authority";
    public static final String FIELD_ANSWER = "answer";

    private static final String LOG_TAG = "LookingGlass/AuthAnswerService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int r = super.onStartCommand(intent, flags, startId);

        // here I'll get my answer from the notification.  pull the value back out!

        Bundle extras = intent.getExtras();

        String authority = extras.getString(FIELD_AUTHORITY_ARGUMENT);
        String answer = extras.getString(FIELD_ANSWER);
        Log.d(LOG_TAG, extras.toString());

        Log.d(LOG_TAG, "GOT REQUEST TO " + answer + ": " + authority);
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
    }
}
