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
import android.location.Location;
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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final String FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length";
    public static final String FAKE_LOCATION = "6666";
    Map<String, Object> map;


    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    private MessageAdapter mMessageAdapter;


    //mybox
    ArrayAdapter<String> adapter;
    private ListView mLocationListView;
    ArrayList<String> listItems = new ArrayList<String>();

    //location
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationCallback locationCallback;

    //map
    private MapFragment mapFragment;
    private GoogleMap mMap;
    Map<String, Object> mapTransfer;


    private ListView mMessageListView;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private TextView mTextBox;
    private TextView mTextBoxMyFamily;
    private Switch onOffSwitch;
    private TextView mTextBoxFamilyId;


    private String mUsername;
    private String mUid;
    private String mUidFamily;

    private boolean mFamilyHeadState;


    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    final DocumentReference mDocRefChatRoom = FirebaseFirestore.getInstance().document("chatroom/data");
    private CollectionReference mColRefChatRoom;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        // instantiate realtime database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        // instantiate storage database
        mFirebaseStorage = FirebaseStorage.getInstance();
        // instantiate remote config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        // instantiate auth
        mFirebaseAuth = FirebaseAuth.getInstance();


        //getting the ref of the real time database title,which happens to be messages here
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        //getting the ref of the storage database title, which happes to be chat_photos here
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");


        // Initialize references to views

        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mTextBox = (TextView) findViewById(R.id.textBoxUserId);
        mTextBoxMyFamily = (TextView) findViewById(R.id.myFamilyBox);
        onOffSwitch = (Switch) findViewById(R.id.enable_switch);
        mTextBoxFamilyId = (TextView) findViewById(R.id.BoxmyFamilyId);


        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    mSendButton.setVisibility(View.INVISIBLE);
                    mMessageEditText.setVisibility(View.INVISIBLE);
                    mTextBoxMyFamily.setVisibility(View.INVISIBLE);
                    mTextBoxFamilyId.setVisibility(View.INVISIBLE);

                    mFamilyHeadState = true;
                    onFamilyIdButtonClicked(mTextBoxFamilyId);

                } else {
                    mSendButton.setVisibility(View.VISIBLE);
                    mMessageEditText.setVisibility(View.VISIBLE);
                    mTextBoxMyFamily.setVisibility(View.VISIBLE);
                    mTextBoxFamilyId.setVisibility(View.VISIBLE);

                    mFamilyHeadState = false;


                }

            }

        });


        ///////////////////////////////////////
        //Auth and remote stuff
        ///////////////////////////////////////

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // user is signed in
//                    Toast.makeText(MainActivity.this, "You're now signed in. Welcome to FriendlyChat.", Toast.LENGTH_SHORT).show();
                    onSignedInInitialize(user.getDisplayName(), user.getUid());

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

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();

        mFirebaseRemoteConfig.setConfigSettings(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        fetchConfig();


        //////////////////////////////////////
        // Location
        //////////////////////////////////////
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);


        ////

        //initializingUserFamilyChatRoom();

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
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();

            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        //       startLocationUpdates();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
    }

    private void onSignedInInitialize(String username, String uid) {
        mUsername = username;
        mUid = uid;
        //set user Id
        mTextBox.setText(mUid);
        // attachLocationSuccessListener()
        Log.v("muid", mUid);

        initializingUserFamilyChatRoom();

    }

    private void initializingUserFamilyChatRoom() {

        mDocRefChatRoom.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {


            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {

                    Map<String, Object> map = documentSnapshot.getData();
                    ArrayList<String> arrayList;

                    arrayList = (ArrayList<String>) map.get(mUid);

                    if (arrayList != null) {

                        if (!arrayList.get(1).equals("-1")) {
                            mUidFamily = arrayList.get(1);
                            mTextBoxMyFamily.setText(arrayList.get(1));
                        } else {
                            ArrayList<String> userInfo = new ArrayList<>();
                            userInfo.add(mUsername);
                            userInfo.add("-1");
                            mUidFamily = arrayList.get(1);
                            mDocRefChatRoom.update(mUid, userInfo);
                            mTextBoxMyFamily.setText("No Family Chat Room Yet");
                        }
                    }

                }
            }
        });


    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
//        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }


    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available
                        // via FirebaseRemoteConfig get<type> calls, e.g., getLong, getString.
                        mFirebaseRemoteConfig.activateFetched();

                        // Update the EditText length limit with
                        // the newly retrieved values from Remote Config.
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // An error occurred when fetching the config.
                        Log.w(TAG, "Error fetching config", e);

                        // Update the EditText length limit with
                        // the newly retrieved values from Remote Config.
                        applyRetrievedLengthLimit();
                    }
                });
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, FRIENDLY_MSG_LENGTH_KEY + " = " + friendly_msg_length);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Enable the zoom controls for the map
        mMap.getUiSettings().setZoomControlsEnabled(true);

    }


    public void onFamilyIdButtonClicked(View view) {

        if (mFamilyHeadState) {
            mUidFamily = mUid;
        } else {
            mUidFamily = mMessageEditText.getText().toString();
        }

        mDocRefChatRoom.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {

            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {

                    Map<String, Object> map = documentSnapshot.getData();
                    ArrayList<String> arrayList;

                    arrayList = (ArrayList<String>) map.get(mUidFamily);

                    if (arrayList != null) {
                        ArrayList<String> userInfo = new ArrayList<>();
                        userInfo.add(mUsername);
                        userInfo.add(mUidFamily);
                        mUidFamily = arrayList.get(1);
                        mDocRefChatRoom.update(mUid, userInfo);
                        initializingUserFamilyChatRoom();

                    } else {


                        ArrayList<String> userInfo = new ArrayList<>();
                        userInfo.add(mUsername);
                        userInfo.add("-1");
                        mDocRefChatRoom.update(mUid, userInfo);
                        mTextBoxMyFamily.setText("Wrong Id");
                    }

                }
            }
        });


        // Clear input box

        if (mMessageEditText.length() > 0) {
            mMessageEditText.getText().clear();
        }


    }


    public void onMyChatButtonClicked(View view) {
        Intent intent = new Intent(this, MyChatActivity.class);
        intent.putExtra("EXTRA_SESSION_USER_NAME", mUsername);
        intent.putExtra("EXTRA_SESSION_ID", mUid);

        startActivity(intent);
    }


    public void onMyFamilyChatButtonClicked(View view) {
        final Intent intent = new Intent(this, FamilyChatActivity.class);
        intent.putExtra("EXTRA_SESSION_USER_NAME", mUsername);
        intent.putExtra("EXTRA_SESSION_ID", mUidFamily);

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

    public void onAddPlaceButtonClicked(View view) {
        final Intent intent = new Intent(this, MapActivity.class);
        //intent.putExtra("EXTRA_SESSION_ID", (HashMap) mapTransfer);
        intent.putExtra("EXTRA_SESSION_USER_NAME", mUsername);
        intent.putExtra("EXTRA_SESSION_FAMILY_ID", mUidFamily);
        intent.putExtra("EXTRA_SESSION_ID", mUid);

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
