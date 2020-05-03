/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.familyunionizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    public static final String ANONYMOUS = "anonymous";
    public static final int RC_SIGN_IN = 1;



    //views
    private EditText mMessageEditText;
    private Button mSendButton;
    private TextView mTextBox;
    private TextView mTextBoxMyFamily;
    private Switch onOffSwitch;
    private TextView mTextBoxFamilyId;


    //user data
    private String mUsername;
    private String mUid;
    private String mUidFamily;


    //firebase
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    final DocumentReference mDocRefChatRoom = FirebaseFirestore.getInstance().document("chatroom/data");


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        InitializeFirebaseRealtimeDatabseAndRemoteAndAuthentication();

        GetReferencesToRealtimeDatabase();

        InitializeReferencesToViews();

        FamilyHeadSwitchViewsAndStateActivation();

        Authentication();

        // RemoteConfig();



    }

    private void GetReferencesToRealtimeDatabase() {
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");

    }

    private void InitializeFirebaseRealtimeDatabseAndRemoteAndAuthentication() {
        // instantiate realtime database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        // instantiate remote config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    private void InitializeReferencesToViews() {
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mTextBox = (TextView) findViewById(R.id.textBoxUserId);
        mTextBoxMyFamily = (TextView) findViewById(R.id.myFamilyBox);
        onOffSwitch = (Switch) findViewById(R.id.enable_switch);
        mTextBoxFamilyId = (TextView) findViewById(R.id.BoxmyFamilyId);
    }



    private void Authentication() {
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    onSignedInInitialize(user.getDisplayName(), user.getEmail());

                } else {
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

//    private void RemoteConfig() {
//        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
//                .setDeveloperModeEnabled(BuildConfig.DEBUG)
//                .build();
//
//        mFirebaseRemoteConfig.setConfigSettings(configSettings);
//
//        Map<String, Object> defaultConfigMap = new HashMap<>();
//        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
//        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
//        fetchConfig();
//    }

    private void FamilyHeadSwitchViewsAndStateActivation() {
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    mSendButton.setVisibility(View.INVISIBLE);
                    mMessageEditText.setVisibility(View.INVISIBLE);
                    mTextBoxMyFamily.setVisibility(View.INVISIBLE);
                    mTextBoxFamilyId.setVisibility(View.INVISIBLE);
                    mUidFamily = mUid;
                    initializingUserFamilyChatRoom(false);

                } else {
                    mSendButton.setVisibility(View.VISIBLE);
                    mMessageEditText.setVisibility(View.VISIBLE);
                    mTextBoxMyFamily.setVisibility(View.VISIBLE);
                    mTextBoxFamilyId.setVisibility(View.VISIBLE);
                    initializingUserFamilyChatRoom(true);

                }

            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();

            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
    }

    private void onSignedInInitialize(String username, String email) {
        mUsername = username;
        mUid = email.split(".com")[0];
        mTextBox.setText(mUid);
        initializingUserFamilyChatRoom(true);

    }

    private void initializingUserFamilyChatRoom(final boolean getTrueSetFalse) {

        mDocRefChatRoom.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {

                    GetAndSetFamilyId(documentSnapshot,getTrueSetFalse);

                }
            }
        });


    }

    private void GetAndSetFamilyId(DocumentSnapshot documentSnapshot,boolean getTrueSetFalse) {

        Map<String, Object> map = documentSnapshot.getData();
        ArrayList<String> arrayListCurrentUser;
        ArrayList<String> arrayListFamilyUser;


        arrayListCurrentUser = (ArrayList<String>) map.get(mUid);
        arrayListFamilyUser = (ArrayList<String>) map.get(mUidFamily);


        if (arrayListCurrentUser != null) {

            if(getTrueSetFalse) {

                if (!arrayListCurrentUser.get(1).equals("-1")) {
                    mUidFamily = arrayListCurrentUser.get(1);
                    mTextBoxMyFamily.setText(arrayListCurrentUser.get(1));
                } else {
                    mTextBoxMyFamily.setText("No Family Chat Room Yet");
                }
            } else {

                if(arrayListFamilyUser != null){
                    ArrayList<String> userInfo = new ArrayList<>();
                    userInfo.add(mUsername);
                    userInfo.add(mUidFamily);
                    mDocRefChatRoom.update(mUid, userInfo);
                } else {
                    mTextBoxMyFamily.setText("Wrong Family Name");
                }

            }

        } else {

            ArrayList<String> userInfo = new ArrayList<>();
            userInfo.add(mUsername);
            userInfo.add("-1");
            mDocRefChatRoom.update(mUid, userInfo);
            mTextBoxMyFamily.setText("No Family Chat Room Yet");
        }
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        detachDatabaseReadListener();
    }


    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

//    public void fetchConfig() {
//        long cacheExpiration = 3600; // 1 hour in seconds
//        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
//        // server. This should not be used in release builds.
//        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
//            cacheExpiration = 0;
//        }
//        mFirebaseRemoteConfig.fetch(cacheExpiration)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        // Make the fetched config available
//                        // via FirebaseRemoteConfig get<type> calls, e.g., getLong, getString.
//                        mFirebaseRemoteConfig.activateFetched();
//
//                        // Update the EditText length limit with
//                        // the newly retrieved values from Remote Config.
//                        applyRetrievedLengthLimit();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // An error occurred when fetching the config.
//                        Log.w(TAG, "Error fetching config", e);
//
//                        // Update the EditText length limit with
//                        // the newly retrieved values from Remote Config.
//                        applyRetrievedLengthLimit();
//                    }
//                });
//    }
//
//    /**
//     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
//     * cached values.
//     */
//    private void applyRetrievedLengthLimit() {
//        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
//        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
//        Log.d(TAG, FRIENDLY_MSG_LENGTH_KEY + " = " + friendly_msg_length);
//    }



    public void onFamilyIdButtonClicked(View view) {
        mUidFamily = mMessageEditText.getText().toString();
        initializingUserFamilyChatRoom(false);
        // Clear input box
        if (mMessageEditText.length() > 0) {
            mMessageEditText.getText().clear();
        }
    }


    public void onMyFamilyChatButtonClicked(View view) {
        Intent intent = new Intent(this, MyChatActivity.class);
        intent.putExtra("EXTRA_SESSION_USER_NAME", mUsername);
        intent.putExtra("EXTRA_SESSION_ID", mUidFamily);
        startActivity(intent);
    }



    public void onAddPlaceButtonClicked(View view) {
        final Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("EXTRA_SESSION_USER_NAME", mUsername);
        intent.putExtra("EXTRA_SESSION_FAMILY_ID", mUidFamily);
        intent.putExtra("EXTRA_SESSION_ID", mUid);

        // Nothing done if no family head
        mDocRefChatRoom.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> map = documentSnapshot.getData();
                    ArrayList<String> arrayList;
                    arrayList = (ArrayList<String>) map.get(mUidFamily);

                    if (arrayList != null) {
                        if (!arrayList.get(1).equals("-1")) {
                            startActivity(intent);

                        }
                    }

                }
            }
        });
    }

}
