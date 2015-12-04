package io.stalk.android.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.stalk.android.R;
import io.xpush.chat.core.CallbackEvent;
import io.xpush.chat.core.XPushCore;
import io.xpush.chat.network.StringRequest;

public class LoginActivity extends AppCompatActivity  {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int REQUEST_SIGNUP = 100;

    @Bind(R.id.input_id)
    EditText mIdText;

    @Bind(R.id.input_password)
    EditText mPasswordText;

    ProgressDialog progressDialog;

    @OnClick(R.id.link_signup)
    public void signUp() {
        Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
        startActivityForResult(intent, REQUEST_SIGNUP);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);
    }

    @Bind(R.id.btn_login)
    Button mLoginButton;

    @OnClick(R.id.btn_login)
    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        mLoginButton.setEnabled(false);

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        final Map<String,String> params = new HashMap<String, String>();
        String url = getString(R.string.stalk_front_url);

        final String id = mIdText.getText().toString();
        final String password = mPasswordText.getText().toString();

        params.put("email", id);
        params.put("password", password);

        url = url + "/api/auths/signin";

        StringRequest request = new StringRequest(url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, response.toString());

                            if( "ok".equalsIgnoreCase(response.getString("status")) ){

                                final String uid = response.getJSONObject( "result" ).getString("uid");
                                final String image = response.getJSONObject( "result" ).getString("image");
                                final String name = response.getJSONObject( "result" ).getString("name");
                                final String email = response.getJSONObject( "result" ).getString("email");

                                final String pw = hmacSHA256encode(uid, "sha256");

                                XPushCore.getInstance().login(uid, pw, new CallbackEvent() {
                                    @Override
                                    public void call(Object... args) {
                                        if (args == null || args.length == 0) {
                                            progressDialog.dismiss();
                                            onLoginSuccess(image,name,email);
                                        } else {

                                            if( "NOT-EXIST-DEVICE".equals( (String)args[0] ) ) {
                                                addDeivce( uid, pw, image, name, email );
                                            } else {
                                                progressDialog.dismiss();
                                                onLoginFailed();
                                            }
                                            
                                        }
                                    }
                                });
                            } else {
                                Log.d(TAG, "Login error ======================");
                                if( response.has("message") ){

                                } else {

                                }

                                progressDialog.dismiss();
                                onLoginFailed();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Login error ======================");
                        error.printStackTrace();
                        progressDialog.dismiss();
                        onLoginFailed();
                    }
                }
        );

        RequestQueue queue = Volley.newRequestQueue(getBaseContext());
        queue.add(request);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getBaseContext(), "Signup success", Toast.LENGTH_LONG).show();
                //this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void onLoginSuccess(String image, String name, String email) {

        if( image != null || name != null || email != null ) {
            if (image != null) {
                XPushCore.getInstance().getXpushSession().setImage(image);
            }

            if (name != null) {
                XPushCore.getInstance().getXpushSession().setName(name);
            }

            if (email != null) {
                XPushCore.getInstance().getXpushSession().setEmail(email);
            }

            XPushCore.getInstance().setXpushSession(XPushCore.getInstance().getXpushSession());
        }

        mLoginButton.setEnabled(true);
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        mLoginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String id = mIdText.getText().toString();
        String password = mPasswordText.getText().toString();

        if (id.isEmpty() || id.length() < 4 || id.length() > 20) {
            mIdText.setError("between 4 and 20 alphanumeric characters");
            valid = false;
        } else {
            mIdText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 20) {
            mPasswordText.setError("between 4 and 20 alphanumeric characters");
            valid = false;
        } else {
            mPasswordText.setError(null);
        }

        return valid;
    }

    private String hmacSHA256encode(String data, String key) {
        String result = null;

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = null;
            secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            result = Base64.encodeToString(sha256_HMAC.doFinal(data.getBytes()), 0);
            result = result.replace("\n", "");

            Log.d(TAG, result);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException ie) {
            ie.printStackTrace();
        }

        return result;
    }

    private void addDeivce(final String uid, final String pw, final String image, final String name, final String email){
        XPushCore.getInstance().addDevice(uid, pw, new CallbackEvent() {
            @Override
            public void call(Object... args) {
                if (args == null || args.length == 0) {
                    XPushCore.getInstance().login(uid, pw, new CallbackEvent() {
                        @Override
                        public void call(Object... args) {
                            if (args == null || args.length == 0) {
                                progressDialog.dismiss();
                                onLoginSuccess(image, name, email);
                            } else {
                                progressDialog.dismiss();
                                onLoginFailed();
                            }
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    onLoginFailed();
                }
            }
        });
    }
}