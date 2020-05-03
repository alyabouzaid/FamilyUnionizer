package com.familyunionizer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class MyChatActivity extends AppCompatActivity {

    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;


    private static final int RC_PHOTO_PICKER = 2;

    private MessageAdapter mMessageAdapter;



    private ListView mMessageListView;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;
    private String mUid;



    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_chat);

        GetUserIdAndNameFromPreviousActivity();

        InitializeFirebase();

        InitializeReferenceViews();

        GetReferencesToRealtimeDatabaseAndPhotos();

        InitializeListViewAndAdapter();

        InitializeProgressbar();

        OnClickListenerForPhotoPicker();

        MessageTextboxListenerToSetSendButtonEnabledAndLimitTextSize();

        OnClickListenerForSendButton();

        ActivateRealtimeFetching();


    }

    private void OnClickListenerForSendButton() {
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(friendlyMessage);

                if (mMessageEditText.length() > 0) {
                    mMessageEditText.getText().clear();
                }
            }
        });
    }

    private void MessageTextboxListenerToSetSendButtonEnabledAndLimitTextSize() {
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
    }

    private void OnClickListenerForPhotoPicker() {
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);


            }
        });
    }

    private void InitializeProgressbar() {
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    private void InitializeListViewAndAdapter() {
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);
    }

    private void GetReferencesToRealtimeDatabaseAndPhotos() {
        //getting the ref of the real time database title,which happens to be messages here
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(mUid);
        //getting the ref of the storage database title, which happes to be chat_photos here
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");
    }

    private void GetUserIdAndNameFromPreviousActivity() {
        mUsername = getIntent().getStringExtra("EXTRA_SESSION_USER_NAME");
        mUid = getIntent().getStringExtra("EXTRA_SESSION_ID");
    }

    private void InitializeFirebase() {
        // instantiate realtime database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        // instantiate storage database
        mFirebaseStorage = FirebaseStorage.getInstance();
    }

    private void InitializeReferenceViews() {
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {

            Uri selectedImageUri = data.getData();

            // Get a reference to store file at chat_photos/<FILENAME>
            final StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri downloadPhotoUrl) {
                                    //Now play with downloadPhotoUrl
                                    //Store data into Firebase Realtime Database
                                    FriendlyMessage friendlyMessage = new FriendlyMessage
                                            (null, mUsername, downloadPhotoUrl.toString());
                                    mMessagesDatabaseReference.push().setValue(friendlyMessage);
                                }
                            });


                        }
                    });


        }
    }



    // adding stuff from the realtime database...
    // it starts with the device and keeps getting called whenever there is something added.
    // just like the listeners in the on create, they stay there.

    private void ActivateRealtimeFetching() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }


}
