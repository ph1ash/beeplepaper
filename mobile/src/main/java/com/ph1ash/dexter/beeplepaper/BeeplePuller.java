package com.ph1ash.dexter.beeplepaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.appdatasearch.GetRecentContextCall;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URL;

/**
 * Created by dexter on 1/31/16.
 */
public class BeeplePuller {

    private GoogleApiClient mClient;
    private AccessToken currentToken;

    private static final String TAG = "BeeplePuller";

    public Bitmap bmp;

    private void setCurrentBitmap(Bitmap newBmp)
    {
        bmp = newBmp;
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    public boolean sendBitmapToClient(Bitmap mBmp) {
        try
        {
            boolean sent = false;
            while(!sent) {
                if(mClient.isConnected())
                {
                    Log.d(TAG, "Client connected - sending images");

                    // Create data request with the image path
                    PutDataMapRequest request = PutDataMapRequest.create("/image");
                    Asset asset = createAssetFromBitmap(mBmp);
                    request.getDataMap().putAsset("wallpaper", asset);

                    /*
                    // Possible way to trigger an update
                    DataMap dataMap = request.getDataMap();
                    dataMap.putLong("timestamp", System.currentTimeMillis());

                    // Send the trigger data
                    PutDataRequest dataRequest = request.asPutDataRequest();
                    Wearable.DataApi.putDataItem(mClient, dataRequest);*/

                    Log.d(TAG, "Updated data items");
                    sent = true;
                }

            }
            return true; //Success
        }
        catch(NullPointerException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private void getImage(String id) {

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        GraphRequest request = GraphRequest.newGraphPathRequest(
                currentToken,
                "/" + id,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject data = response.getJSONObject();
                        try {
                            JSONArray images = data.getJSONArray("images");
                            for (int idx = 0; idx < images.length(); idx++) {
                                String height = images.getJSONObject(idx).getString("height");
                                String width = images.getJSONObject(idx).getString("width");
                                if (height != null && width != null) {
                                    if (height.equals("320") || width.equals("320")) {
                                        URL url = new URL(images.getJSONObject(idx).getString("source"));
                                        setCurrentBitmap(BitmapFactory.decodeStream(url.openConnection().getInputStream()));
                                    }
                                } else {
                                    Log.d(TAG, "Null value for height or width of image");
                                }
                            }
                        } catch (org.json.JSONException | java.io.IOException e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "images");
        request.setParameters(parameters);
        request.executeAsync();
    }


    public boolean getBeepleImages() {
        currentToken = AccessToken.getCurrentAccessToken();
        if (currentToken != null) {
            GraphRequest request = GraphRequest.newGraphPathRequest(
                    currentToken,
                    "/10150428845566781/photos",
                    new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse response) {
                            JSONObject data = response.getJSONObject();
                            try {
                                String newestImage = data.getJSONArray("data").getJSONObject(0).getString("id");
                                getImage(newestImage);
                                Log.d(TAG, newestImage);
                            } catch (org.json.JSONException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });

            request.executeAsync();
            return true;
        } else {
            Log.d(TAG, "Current access token does not exist");
            return false;
        }
    }

    public void setGoogleApiClient(GoogleApiClient client)
    {
        mClient = client;
    }

}
