package io.stalk.android.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.xpush.chat.common.Constants;
import io.xpush.chat.core.CallbackEvent;
import io.xpush.chat.core.XPushCore;
import io.xpush.chat.models.XPushSession;
import io.stalk.android.R;
import io.stalk.android.activities.EditNickNameActivity;
import io.stalk.android.activities.EditStatusMessageActivity;
import io.xpush.chat.network.StringRequest;

public class ProfileFragment extends Fragment {

    private String TAG = ProfileFragment.class.getSimpleName();
    private Context mActivity;

    private XPushSession mSession;
    private JSONObject mJsonUserData;

    @Bind(R.id.nickname_button)
    View mNicknameButton;

    @Bind(R.id.status_message_button)
    View mStatusMessageButton;

    @Bind(R.id.tvUserId)
    TextView mTvUserId;

    @Bind(R.id.imageBox)
    View mImageBox;

    @Bind(R.id.thumbnail)
    SimpleDraweeView mThumbnail;

    @Bind(R.id.nickname)
    TextView mTvNickname;

    @Bind(R.id.status_message)
    TextView mTvStatusMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mSession = XPushCore.getInstance().getXpushSession();

        Log.d(TAG, mSession.toString());
        mJsonUserData = mSession.getUserData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, view);

        if( mSession.getImage() != null ) {
            mThumbnail.setImageURI(Uri.parse(mSession.getImage()));
        }

        if( null != mSession.getName() ) {
            mTvNickname.setText(mSession.getName());
        }

        if( null != mSession.getMessage() ) {
            mTvStatusMessage.setText(mSession.getMessage());
        }

        if( null != mSession.getId() ){
            mTvUserId.setText( mSession.getEmail() );
        }

        return view;
    }

    @OnClick(R.id.nickname_button)
    public void editNickName() {
        Intent localIntent = new Intent(mActivity, EditNickNameActivity.class);
        getActivity().startActivityForResult(localIntent, Constants.REQUEST_EDIT_NICKNAME);
    }

    @OnClick(R.id.status_message_button)
    public void editStatusMessage() {
        Intent localIntent = new Intent(mActivity, EditStatusMessageActivity.class);
        getActivity().startActivityForResult(localIntent, Constants.REQUEST_EDIT_STATUS_MESSAGE);
    }

    @OnClick(R.id.imageBox)
    public void openGallery(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        getActivity().startActivityForResult(Intent.createChooser(intent, "Select file to use profile"), Constants.REQUEST_EDIT_IMAGE);
    }

    public void setImage(Uri uri){
        UploadImageTask imageUpload = new UploadImageTask(uri);
        imageUpload.execute();
    }

    public void setNickName(String name){
        try {
            if (mJsonUserData != null) {
                mJsonUserData.put("NM", name);
                updateProfile();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setStatusMessage(String message){
        try {
            if( mJsonUserData != null ) {
                mJsonUserData.put("MG", message);
                updateProfile();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateProfile() {
        Log.d(TAG, "Login");

        final Map<String,String> params = new HashMap<String, String>();
        String url = getString(R.string.stalk_front_url);

        try {
            params.put("image", mJsonUserData.getString("I"));
            params.put("name", mJsonUserData.getString("NM"));
        } catch ( Exception e ){
            e.printStackTrace();
        }

        url = url + "/api/auths/"+mSession.getId();

        StringRequest request = new StringRequest(url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, response.toString());
                            if( response.has("uid") && mSession.getId().equalsIgnoreCase(response.getString("uid")) ){
                                XPushCore.getInstance().getXpushSession().setImage( params.get("image") );
                                XPushCore.getInstance().getXpushSession().setName( params.get("name") );
                                XPushCore.getInstance().setXpushSession(XPushCore.getInstance().getXpushSession());

                            } else {
                                Log.d(TAG, "update error ======================");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "update error ======================");
                        error.printStackTrace();
                    }
                }
        );

        RequestQueue queue = Volley.newRequestQueue(mActivity);
        queue.add(request);
    }

    private class UploadImageTask extends AsyncTask<Void, Void, String> {
        Uri mUri;

        public UploadImageTask(Uri uri) {
            this.mUri = uri;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String downloadUrl = XPushCore.getInstance().uploadImage(mUri);
            try {
                mJsonUserData.put("I", downloadUrl);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return downloadUrl;
        }

        @Override
        protected  void onPostExecute(final String imageUrl){
            super.onPostExecute(imageUrl);
            if( imageUrl != null ){
                updateProfile();
            }
        }
    }
}