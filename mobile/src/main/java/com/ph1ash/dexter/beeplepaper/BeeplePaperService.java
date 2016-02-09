package com.ph1ash.dexter.beeplepaper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by dexter on 1/31/16.
 */
public class BeeplePaperService  extends WearableListenerService implements DataApi.DataListener, MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient mClient;

    private static final String UPDATE_BEEPLE_IMAGE = "/update_image";

    private static final String TAG = "BeeplePaperService";

    private BeeplePuller puller = new BeeplePuller();

    private AccessToken currentToken;

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        /*Bundle extras = intent.getExtras();
        if(extras != null)
        {
            String tok = (String) extras.get("Token");
            currentToken = AccessToken.getCurrentAccessToken();
        }*/
        return START_STICKY;
    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG, "Connected or something...");
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
        //Update API client to begin operations
        puller.setGoogleApiClient(mClient);
        Log.d(TAG, "Client set...");
        //Puller must be defined |after| Google API Client is populated
        if (!puller.getBeepleImages())
        {
            displayLoginNotification();
        }
        Log.d(TAG, "Images pulled");
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
