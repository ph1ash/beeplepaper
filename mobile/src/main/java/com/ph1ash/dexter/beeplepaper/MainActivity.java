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

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

public class MainActivity extends AppCompatActivity {

    CallbackManager callbackManager;

    private AccessToken currentToken;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        callbackManager = CallbackManager.Factory.create();

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_friends");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("FBResponse", "Getting access token");
                currentToken = loginResult.getAccessToken();
                Log.d("FBResponse", loginResult.getAccessToken().toString());
            }

            @Override
            public void onCancel() {
                Log.d("FBResponse", "Login Cancelled");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e("FBResponse", exception.toString());
            }
        });

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
                                        Log.d("FBResponse", newestImage);
                                    } catch (org.json.JSONException e) {
                                        Log.e("FBResponse", e.toString());
                                    }
                                }
                            });

                    request.executeAsync();
                } else {
                    Log.d("FBResponse", "Current access token does not exist");
                }
            }
        });
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
                                if(height.equals("320")){
                                    Log.d("FBResponse", images.getJSONObject(idx).getString("source"));
                                    URL url = new URL(images.getJSONObject(idx).getString("source"));
                                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                    ImageView imageView = (ImageView) findViewById(R.id.image_viewer);
                                    imageView.setImageBitmap(bmp);
                                }
                            }
                        }
                        catch(org.json.JSONException | java.io.IOException e)
                        {
                            Log.e("FBResponse", e.toString());
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
        Log.d("FBResponse", "Getting results");
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
