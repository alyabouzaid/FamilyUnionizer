package com.familyunionizer;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
//import com.google.android.libraries.places.api.Places;
//import com.google.android.libraries.places.api.model.Place;
//import com.google.android.libraries.places.widget.Autocomplete;
//import com.google.android.libraries.places.widget.AutocompleteActivity;
//import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
//import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String TAG = MapActivity.class.getSimpleName();


    //map
    private GoogleMap mMap;
    private MapFragment mapFragment;


    //location
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationCallback locationCallback;

    // Firebase documents
    final DocumentReference mDocRefLocation = FirebaseFirestore.getInstance().document("location/data");
    final DocumentReference mDocRefChatRoom = FirebaseFirestore.getInstance().document("chatroom/data");


    //user data
    private String mUid;
    private String mUidFamily;
    private String mUsername;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        GetUserIdAndNameFromPreviousActivity();

        GetMapItemFromLayoutAndGetMapAsync();

        GettingLocationServicesOnThisActivity();

        GettingLocationPermisionInCaseItWasntThere();

        GetDeviceLocation();

        GettingLocationCallBackFunctionInitialization();

    }

    private void GettingLocationCallBackFunctionInitialization() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    mLastKnownLocation = location;

                    UpdatingTheCurrentUserLocationToMap();

                    mDocRefLocation.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {

                                final Map<String, Object> MapOfUserLocations = documentSnapshot.getData();

                                GettingNamesOfUsersAndPlottingThemAccodingToTheirLocation(MapOfUserLocations);

                            }
                        }
                    });


                }
            }

            ;
        };
    }

    private void UpdatingTheCurrentUserLocationToMap() {
        ArrayList<Double> location = new ArrayList<>();
        location.add(mLastKnownLocation.getLatitude());
        location.add(mLastKnownLocation.getLongitude());
        mDocRefLocation.update(mUsername,location);
    }

    private void GettingNamesOfUsersAndPlottingThemAccodingToTheirLocation(final Map<String, Object> mapOfUserLocations) {
        mMap.clear();
        mDocRefChatRoom.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> MapOfUserChatRooms = documentSnapshot.getData();
                    ArrayList<String> ArrayOfChatRoomForASpecificUser;

                    for (Map.Entry<String, Object> entry : mapOfUserLocations.entrySet()) {

                        ArrayList<Double> longitudeLantitude = (ArrayList<Double>) entry.getValue();
                        ArrayOfChatRoomForASpecificUser = (ArrayList<String>) MapOfUserChatRooms.get(entry.getKey());
                        if (ArrayOfChatRoomForASpecificUser != null) {
                            if (ArrayOfChatRoomForASpecificUser.get(1).equals(mUidFamily)) {
                                if (longitudeLantitude.get(0) != null && longitudeLantitude.get(1) != null) {
                                    mMap.addMarker(new MarkerOptions()
                                            .title(entry.getKey())
                                            .position(new LatLng(longitudeLantitude.get(0), longitudeLantitude.get(1)))
                                            .snippet("No places found, because location permission is disabled.")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                }
                            }
                        }

                    }

                }
            }
        });
    }

    private void GettingLocationServicesOnThisActivity() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void GetMapItemFromLayoutAndGetMapAsync() {
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    private void GetUserIdAndNameFromPreviousActivity() {
        mUid = getIntent().getStringExtra("EXTRA_SESSION_ID");
        mUidFamily = getIntent().getStringExtra("EXTRA_SESSION_FAMILY_ID");
        mUsername = getIntent().getStringExtra("EXTRA_SESSION_ID");

    }


    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */

    private void GettingLocationPermisionInCaseItWasntThere() {
        mLocationPermissionGranted = false;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
    private void GetDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            mLastKnownLocation = task.getResult();
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();

    }

    private void startLocationUpdates() {

        LocationRequest request = new LocationRequest()
                .setFastestInterval(1500)
                .setInterval(6000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationProviderClient.requestLocationUpdates(request,
                locationCallback,
                Looper.getMainLooper());
    }
}


