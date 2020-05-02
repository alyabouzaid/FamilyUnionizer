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
    private GoogleMap mMap;
    private LatLng sydney = new LatLng(-8.579892, 116.095239);
    private MapFragment mapFragment;


    //location
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationCallback locationCallback;
    // document
    final DocumentReference mDocRef = FirebaseFirestore.getInstance().document("location/data");


    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.


    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    public static final String TAG = MainActivity.class.getSimpleName();


    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private String mUid;
    private String mUidFamily;

    final DocumentReference mDocRefChatRoom = FirebaseFirestore.getInstance().document("chatroom/data");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        setContentView(R.layout.activity_maps);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);


        mUid = getIntent().getStringExtra("EXTRA_SESSION_ID");
        mUidFamily = getIntent().getStringExtra("EXTRA_SESSION_FAMILY_ID");


        //////////////////////////////////////
        // Location
        //////////////////////////////////////
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getLocationPermission();
        getDeviceLocation();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    mLastKnownLocation = location;

                    mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                final Map<String, Object> mapUsers = documentSnapshot.getData();
                                ArrayList<Double> location = new ArrayList<>();
                                location.add(mLastKnownLocation.getLatitude());
                                location.add(mLastKnownLocation.getLongitude());
                                String mUsername = getIntent().getStringExtra("EXTRA_SESSION_ID");

                                mapUsers.put(mUsername, location);
                                Log.d(TAG, "loooop" + mLastKnownLocation + "elwa");

                                mMap.clear();
                                //isUserInFamily();
                              mDocRef.update(mUsername,location);


                                mDocRefChatRoom.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {


                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        if (documentSnapshot.exists()) {
                                            Log.d(TAG, "loooop" + "amber");

                                            Map<String, Object> map = documentSnapshot.getData();
                                            ArrayList<String> arrayList;


                                            for (Map.Entry<String, Object> entry : mapUsers.entrySet()) {

                                                ArrayList<Double> longitudeLantitude = (ArrayList<Double>) entry.getValue();
                                                arrayList = (ArrayList<String>) map.get(entry.getKey());

                                                Log.v(String.valueOf(longitudeLantitude.get(0)), "hhhhh");
                                                Log.v(mUidFamily, "hhhhh");

                                                if (arrayList != null) {
                                                    if (arrayList.get(1).equals(mUidFamily)) {
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
//                                for (Map.Entry<String, Object> entry : map.entrySet()) {
//
//                                    ArrayList<Double> longitudeLantitude = (ArrayList<Double>) entry.getValue();
//
//
//                                    if (entry.getKey() ==) {
//
//                                    }
//
                                mDocRef.update(mUsername, location);
//
//                                    if (longitudeLantitude.get(0) != null && longitudeLantitude.get(1) != null) {
//                                        mMap.addMarker(new MarkerOptions()
//                                                .title(entry.getKey())
//                                                .position(new LatLng(longitudeLantitude.get(0), longitudeLantitude.get(1)))
//                                                .snippet("No places found, because location permission is disabled.")
//                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
//                                    }
//
//                                }


                            }
                        }
                    });


                }
            }

            ;
        };


    }


    private void isUserInFamily() {


    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
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

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
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
//                            mMap.moveCamera(CameraUpdateFactory
//                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
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

        // Enable the zoom controls for the map
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // pick current location


        mDocRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> map = documentSnapshot.getData();
                    ArrayList<Double> location = new ArrayList<>();
                    location.add(mLastKnownLocation.getLatitude());
                    location.add(mLastKnownLocation.getLongitude());

                    map.put(mUid, location);
                    Log.d(TAG, "loooop");


                    mDocRef.set(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Document has been saved");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Document was not saved", e);

                        }
                    });

                }
            }
        });


        mDocRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (documentSnapshot.exists()) {
                    final Map<String, Object> mapUsers = documentSnapshot.getData();

                    mMap.clear();

                    mDocRefChatRoom.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {


                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                Log.d(TAG, "loooop" + "amber");

                                Map<String, Object> map = documentSnapshot.getData();
                                ArrayList<String> arrayList;


                                for (Map.Entry<String, Object> entry : mapUsers.entrySet()) {

                                    ArrayList<Double> longitudeLantitude = (ArrayList<Double>) entry.getValue();
                                    arrayList = (ArrayList<String>) map.get(entry.getKey());

                                    Log.v(String.valueOf(longitudeLantitude.get(0)), "hhhhh");
                                    Log.v(mUidFamily, "hhhhh");

                                    if (arrayList != null) {
                                        if (arrayList.get(1).equals(mUidFamily)) {
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



//                    for (Map.Entry<String, Object> entry : map.entrySet()) {
//
//                        ArrayList<Double> longitudeLantitude = (ArrayList<Double>) entry.getValue();
//
//
//                        if (longitudeLantitude.get(0) != null && longitudeLantitude.get(1) != null) {
//                            mMap.addMarker(new MarkerOptions()
//                                    .title(entry.getKey())
//                                    .position(new LatLng(longitudeLantitude.get(0), longitudeLantitude.get(1)))
//                                    .snippet("No places found, because location permission is disabled.")
//                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
//                        }
//
//                    }
                }
            }
        });


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


