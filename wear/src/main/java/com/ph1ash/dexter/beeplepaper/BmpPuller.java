package com.ph1ash.dexter.beeplepaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by dexter on 12/17/15.
 */
public class BmpPuller extends WearableListenerService{

    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "BeepleWear";

    private Bitmap image;

    @Override
    public void onCreate()
    {
    }

    /*@Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG,"OnDataChanged fired");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/image")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset profileAsset = dataMapItem.getDataMap().getAsset("wallpaper");
                image = loadBitmapFromAsset(profileAsset);
            }
        }
    }*/

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(10000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    public Bitmap getBitmap()
    {
        if(image != null)
        {
            Log.d(TAG,"Image not null");
            return image;
        }
        else
        {
            return null;
        }
    }
}
