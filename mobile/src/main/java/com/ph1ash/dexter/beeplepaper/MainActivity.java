package com.ph1ash.dexter.beeplepaper;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener {

    private static final String TAG = "BeepleMobile";
    private static final String IMAGE_PATH = "/image";

    CallbackManager callbackManager;

    private AccessToken currentToken;

    private GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        callbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_friends");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Getting access token");
                currentToken = loginResult.getAccessToken();
                Log.d(TAG, loginResult.getAccessToken().toString());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Login Cancelled");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e(TAG, exception.toString());
            }
        });

        Log.d(TAG, getPackageName());

        final Button button = (Button) findViewById(R.id.get_img_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                } else {
                    Log.d(TAG, "Current access token does not exist");
                }
            }
        });
    }

    // Used for adding string data into desired data map
    private void putDataMapString(String path, String data)
    {
        PutDataMapRequest dataMapValue = PutDataMapRequest.create(path);
        dataMapValue.getDataMap().putString(path, data);
        dataMapValue.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest requestData = dataMapValue.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, requestData);
    }

    private void getImage(String id)
    {

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        GraphRequest request = GraphRequest.newGraphPathRequest(
                currentToken,
                "/"+id,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONObject data = response.getJSONObject();
                        try
                        {
                            JSONArray images = data.getJSONArray("images");
                            for(int idx=0; idx < images.length() ; idx++){
                                String height = images.getJSONObject(idx).getString("height");
                                String width = images.getJSONObject(idx).getString("width");
                                if(height != null && width != null) {
                                    //Log.d(TAG, height);
                                    //Log.d(TAG, width);
                                    if (height.equals("320") || width.equals("320")) {
                                        //Log.d(TAG , images.getJSONObject(idx).getString("source"));
                                        URL url = new URL(images.getJSONObject(idx).getString("source"));
                                        Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                        ImageView imageView = (ImageView) findViewById(R.id.image_viewer);
                                        imageView.setImageBitmap(bmp);

                                        PutDataMapRequest request = PutDataMapRequest.create("/image");
                                        Asset asset = createAssetFromBitmap(bmp);
                                        request.getDataMap().putAsset("wallpaper", asset);

                                        DataMap dataMap = request.getDataMap();
                                        dataMap.putLong("timestamp", System.currentTimeMillis());

                                        PutDataRequest dataRequest = request.asPutDataRequest();
                                        Wearable.DataApi.putDataItem(mGoogleApiClient, dataRequest);

                                        Log.d(TAG,"I hope I'm connected... " + mGoogleApiClient.isConnected());
                                        Log.d(TAG, "Updated data items");
                                    }
                                }
                                else
                                {
                                    Log.d(TAG, "Null value for height or width of image" );
                                }
                            }
                        }
                        catch(org.json.JSONException | java.io.IOException e)
                        {
                            Log.e(TAG , e.toString());
                        }


                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "images");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "Getting results");
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                //if (DataLayerListenerService.TEMPERATURE_PATH.equals(path))
                //{
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
//                    temperature = dataMapItem.getDataMap().getString(DataLayerListenerService.TEMPERATURE_PATH);
                //                  mHandler.post(new Runnable() {
                //                    @Override
                //                  public void run() {
                //                    Log.d(TAG, "Setting temperature params to "+temperature);
                //                  TextView currentView = (TextView) findViewById(R.id.temperatureText);
                //                currentView.setText(temperature+"°F");
                //          }
                //    });
            }
            //} else if (event.getType() == DataEvent.TYPE_DELETED) {
            //generateEvent("DataItem Deleted", event.getDataItem().toString());
            //} else {
            //generateEvent("Unknown data event type", "Type = " + event.getType());
            //}
        }
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.d(TAG, "onMessageReceived: " + event);
        //generateEvent("Message", event.toString());
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}

