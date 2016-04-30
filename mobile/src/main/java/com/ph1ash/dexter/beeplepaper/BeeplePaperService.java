package com.ph1ash.dexter.beeplepaper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Calendar;

/**
 * Created by dexter on 1/31/16.
 */
public class BeeplePaperService  extends WearableListenerService implements DataApi.DataListener, MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient mClient;

    private static final String UPDATE_BEEPLE_IMAGE = "/update_image";

    private static final String TAG = "BeeplePaperService";

    private BeeplePuller puller = new BeeplePuller();


    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public class mClockReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Date changed, getting and sending images");
            sendImages();
        }
    }

    public class mButtonPressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Screen on, getting and sending images");
            sendImages();
        }
    }

    private void sendImages()
    {
        /* Clear data */

        /* Create image pull request */
        puller.setGoogleApiClient(mClient);
        Log.d(TAG, "Client set...");
        //Puller must be defined |after| Google API Client is populated
        if (!puller.getBeepleImages())
        {
            displayLoginNotification();
        }
        Log.d(TAG, "Images pulled");

        /* Get & send image */
        Toast.makeText(getApplicationContext(), "Sending images...", Toast.LENGTH_SHORT).show();
        puller.sendBitmapToClient(puller.bmp);
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "Connected to Google API Service...");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Beeple Paper Service created");

        FacebookSdk.sdkInitialize(getApplicationContext());

        mClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d(TAG, "Message received");
        if (event.getPath().equals(UPDATE_BEEPLE_IMAGE)) {
            //If app expands to multi message, move items below up here.
        }

    }

    protected void displayLoginNotification()
    {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.common_full_open_on_phone)
                .setContentTitle("BeeplePaper - Verify Login")
                .setContentText("Verify your Facebook login for to allow BeeplePaper to update.");

        Intent resultIntent = new Intent(this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_ONE_SHOT
        );
        mBuilder.setContentIntent(resultPendingIntent);
        Notification notif = mBuilder.build();
        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, notif);
    }
}
