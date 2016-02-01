package com.ph1ash.dexter.beeplepaper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.d(TAG,"Connected or something...");
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
    public void onMessageReceived(MessageEvent event)
    {
        Log.d(TAG,"Message received");
        if (event.getPath().equals(UPDATE_BEEPLE_IMAGE))
        {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(BeeplePaperService.this.getApplicationContext(), "Updating Beeple Data", Toast.LENGTH_LONG).show();
                }
            });
        }
        //Update API client to begin operations
        puller.setGoogleApiClient(mClient);
        Log.d(TAG,"Client set...");
        //Puller must be defined |after| Google API Client is populated
        puller.getBeepleImages();
        Log.d(TAG,"Images pulled");
    }
}
