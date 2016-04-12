package com.ph1ash.dexter.beeplepaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Random;

/**
 * Created by dexter on 1/31/16.
 */
public class BeeplePuller {

    public String WATCH_IMAGE_HEIGHT = "320";
    public String WATCH_IMAGE_WIDTH = "320";

    public String MOBILE_IMAGE_HEIGHT = "";
    public String MOBILE_IMAGE_WIDTH;

    private GoogleApiClient mClient;
    private AccessToken currentToken;

    private static final String TAG = "BeeplePuller";

    private int device = 0;
    private boolean pull_success = false;

    private boolean getImage(String id, int dev_num) {
        device = dev_num;

        // Reset before inner tests
        pull_success = false;

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
                                    String dev_height = "", dev_width = "";
                                    if (device == 1) {
                                        dev_height = WATCH_IMAGE_HEIGHT;
                                        dev_width = WATCH_IMAGE_WIDTH;
                                    }
                                    else if (device == 2)
                                    {
                                        dev_height = MOBILE_IMAGE_HEIGHT;
                                        dev_width = MOBILE_IMAGE_WIDTH;
                                    }

                                    if (height.equals(dev_height) || width.equals(dev_width)) {
                                        URL url = new URL(images.getJSONObject(idx).getString("source"));
                                        Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                                        if (bmp != null) {
                                            PutDataMapRequest request = PutDataMapRequest.create("/image");
                                            Asset asset = createAssetFromBitmap(bmp);
                                            request.getDataMap().putAsset("wallpaper", asset);

                                            PutDataRequest dataRequest = request.asPutDataRequest();
                                            Wearable.DataApi.putDataItem(mClient, dataRequest);

                                            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mClient).await();
                                            for (Node node : nodes.getNodes()){
                                                Wearable.MessageApi.sendMessage(mClient, node.getId(), "/faker", Integer.toString(new Random().nextInt()).getBytes()).await();
                                            }

                                            Log.d(TAG, "BMP Updated");

                                            pull_success = true;
                                        } else {
                                            Log.d(TAG, "Bitmap empty");
                                            pull_success = false;
                                        }
                                    }
                                } else {
                                    Log.d(TAG, "Null value for height or width of image");
                                    pull_success = false;
                                }
                            }
                        } catch (org.json.JSONException | java.io.IOException e) {
                            Log.e(TAG, e.toString());
                            pull_success = false;
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "images");
        request.setParameters(parameters);
        request.executeAsync();

        return pull_success;
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
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
                                getImage(newestImage, 1);
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
