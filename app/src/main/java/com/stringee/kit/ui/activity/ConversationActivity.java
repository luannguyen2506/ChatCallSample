package com.stringee.kit.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.stringee.apptoappcallsample.R;
import com.stringee.kit.ui.commons.Notify;
import com.stringee.kit.ui.commons.utils.FileUtils;
import com.stringee.kit.ui.commons.utils.PermissionsUtils;
import com.stringee.kit.ui.commons.utils.StringeePermissions;
import com.stringee.kit.ui.commons.utils.Utils;
import com.stringee.kit.ui.fragment.AudioMessageFragment;
import com.stringee.kit.ui.fragment.ChatFragment;
import com.stringee.kit.ui.fragment.ConversationListFragment;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.listeners.ConversationListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ConversationActivity extends AppCompatActivity implements ConversationListener {

    RelativeLayout childFragmentLayout;
    protected ActionBar mActionBar;
    private ConversationListFragment conversationListFragment;
    private ChatFragment chatFragment;
    private InputMethodManager inputMethodManager;
    private Bundle extras;
    public Snackbar snackbar;
    public static LinearLayout layout;
    private int attachType;
    private File mediaFile;

    public static final int REQUEST_CODE_LOCATION = 1;
    public static final int REQUEST_CODE_TAKE_PHOTO = 2;
    public static final int REQUEST_CODE_FILE = 3;
    public static final int REQUEST_CODE_CAPTURE_VIDEO = 4;
    public static final int REQUEST_CODE_CONTACT_SHARE = 5;

    public static final String PACKAGE_NAME = "com.package.name";

    private BroadcastReceiver convAddedReceiver;
    private BroadcastReceiver convUpdatedReceiver;

    public ConversationActivity() {

    }

    private void addFragment(FragmentActivity fragmentActivity, Fragment fragmentToAdd, String fragmentTag) {
        FragmentManager supportFragmentManager = fragmentActivity.getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        fragmentTransaction.replace(R.id.layout_child_activity, fragmentToAdd,
                fragmentTag);

        fragmentTransaction.addToBackStack(fragmentTag);
        fragmentTransaction.commitAllowingStateLoss();
        supportFragmentManager.executePendingTransactions();
    }

    public Fragment getFragmentByTag(FragmentActivity activity, String tag) {
        FragmentManager supportFragmentManager = activity.getSupportFragmentManager();

        if (supportFragmentManager.getBackStackEntryCount() == 0) {
            return null;
        }
        return supportFragmentManager.findFragmentByTag(tag);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_conversation);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(R.string.conversations);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);

        extras = getIntent().getExtras();

        childFragmentLayout = (RelativeLayout) findViewById(R.id.layout_child_activity);
        layout = findViewById(R.id.footerAd);

        if (extras != null) {
            chatFragment = new ChatFragment();
            chatFragment.setArguments(extras);
            addFragment(this, chatFragment, "ChatFragment");
        } else if (savedInstanceState != null) {
            chatFragment = new ChatFragment();
            chatFragment.setArguments(savedInstanceState);
            addFragment(this, chatFragment, "ChatFragment");
        } else {
            conversationListFragment = new ConversationListFragment();
            addFragment(this, conversationListFragment, "ConversationFragment");
        }

        registerReceivers();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (extras != null) {
            outState.putSerializable("conversation", extras.getSerializable("conversation"));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    private void showActionBar() {
        mActionBar.setDisplayShowTitleEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        View v = getCurrentFocus();
        if (v != null) {
            hideKeyboard(v);
        }
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getExtras() != null) {
            chatFragment = new ChatFragment();
            chatFragment.setArguments(intent.getExtras());
            addFragment(this, chatFragment, "ChatFragment");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            ChatFragment fragment = (ChatFragment) getFragmentByTag(ConversationActivity.this, "ChatFragment");
            if (fragment != null) {
                switch (requestCode) {
                    case REQUEST_CODE_LOCATION:
                        double latitude = data.getDoubleExtra("latitude", 0);
                        double longitude = data.getDoubleExtra("longitude", 0);
                        fragment.sendLocation(latitude, longitude);
                        break;
                    case REQUEST_CODE_TAKE_PHOTO:
                        if (mediaFile != null) {
                            fragment.sendPhoto(mediaFile.getAbsolutePath());
                        }
                        break;
                    case REQUEST_CODE_CAPTURE_VIDEO:
                        if (mediaFile != null) {
                            fragment.sendVideo(mediaFile);
                        }
                        break;
                    case REQUEST_CODE_CONTACT_SHARE:
                        fragment.sendContact(data.getData());
                        break;
                    case REQUEST_CODE_FILE:
                        String path = data.getStringExtra("path");
                        fragment.sendFile(path);
                        break;
                }
            }
        }
    }

    public void hideKeyboard(View v) {
        if (inputMethodManager.isActive()) {
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void setAttachType(int attachType) {
        this.attachType = attachType;
    }

    @Override
    public void onMessageAdded(final Message message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatFragment fragment = (ChatFragment) getFragmentByTag(ConversationActivity.this, "ChatFragment");
                if (fragment != null) {
                    fragment.onAddMessage(message);
                }
            }
        });
    }

    @Override
    public void onMessageUpdated(Message message) {
        ChatFragment fragment = (ChatFragment) getFragmentByTag(this, "ChatFragment");
        if (fragment != null) {
            fragment.onUpdateMessage(message);
        }
    }

    @Override
    public void onMessageDeleted(Message message) {

    }

    public void showSnackBar(int resId) {
        try {
            snackbar = Snackbar.make(layout, resId,
                    Snackbar.LENGTH_SHORT);
            snackbar.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionsUtils.REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.phone_camera_permission_granted);
                captureImage(this);
            } else {
                showSnackBar(R.string.phone_camera_permission_not_granted);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CAMERA_AUDIO) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.phone_camera_and_audio_permission_granted);
                captureVideo(this);
            } else {
                showSnackBar(R.string.audio_or_camera_permission_not_granted);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CONTACT) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.contact_permission_granted);
                Intent contactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                contactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                startActivityForResult(contactIntent, REQUEST_CODE_CONTACT_SHARE);
            } else {
                showSnackBar(R.string.contact_permission_not_granted);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_AUDIO_RECORD) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackBar(R.string.record_audio_permission_granted);
                FragmentManager supportFragmentManager = getSupportFragmentManager();
                DialogFragment fragment = AudioMessageFragment.newInstance();
                FragmentTransaction fragmentTransaction = supportFragmentManager
                        .beginTransaction().add(fragment, "AudioMessageFragment");

                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commitAllowingStateLoss();
            } else {
                showSnackBar(R.string.record_audio_permission_not_granted);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_STORAGE) {
            switch (attachType) {
                case 1:
                    processCamera(this);
                    break;
                case 2:
                    Intent intent = new Intent(this, SelectFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_FILE);
                    break;
                case 3:
                    processAudio(this);
                    break;
                case 4:
                    processVideo(this);
                    break;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void processCamera(Activity activity) {
        if (PermissionsUtils.isCameraPermissionGranted(activity)) {
            captureImage(activity);
        } else {
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                new StringeePermissions(activity, layout).requestCameraPermission();
            } else {
                captureImage(activity);
            }
        }
    }

    public void processCameraAction(final Activity activity) {
        if (PermissionsUtils.isStoragePermissionGranted(activity)) {
            processCamera(activity);
        } else {
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                new StringeePermissions(activity, layout).requestStoragePermissions();
            } else {
                processCamera(activity);
            }
        }
    }

    private void processVideo(Activity activity) {
        try {
            if (PermissionsUtils.isCameraPermissionGranted(activity) && PermissionsUtils.isAudioRecordingPermissionGranted(activity)) {
                captureVideo(activity);
            } else {
                if (Utils.hasMarshmallow() && PermissionsUtils.checkPermissionForCameraAndMicrophone(activity)) {
                    new StringeePermissions(activity, layout).requestCameraAndRecordPermission();
                } else {
                    captureVideo(activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processVideoAction(Activity activity) {
        if (PermissionsUtils.isStoragePermissionGranted(activity)) {
            processVideo(activity);
        } else {
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                new StringeePermissions(activity, layout).requestStoragePermissions();
            } else {
                processVideo(activity);
            }
        }
    }

    private void captureImage(Activity activity) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + ".jpeg";

            mediaFile = FileUtils.getFilePath(imageFileName, activity.getApplicationContext(), "image/jpeg");

            Uri capturedImageUri = FileProvider.getUriForFile(activity, Utils.getMetaDataValue(activity, PACKAGE_NAME) + ".provider", mediaFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ClipData clip =
                        ClipData.newUri(activity.getContentResolver(), "a Photo", capturedImageUri);

                cameraIntent.setClipData(clip);
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            } else {
                List<ResolveInfo> resInfoList = activity.getPackageManager()
                        .queryIntentActivities(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    activity.grantUriPermission(packageName, capturedImageUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    activity.grantUriPermission(packageName, capturedImageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }


            if (cameraIntent.resolveActivity(activity.getApplicationContext().getPackageManager()) != null) {
                if (mediaFile != null) {
                    activity.startActivityForResult(cameraIntent, ConversationActivity.REQUEST_CODE_TAKE_PHOTO);
                }
            }
        } catch (Exception e) {
            Log.e("Stringee", e.getMessage());
            e.printStackTrace();
        }
    }


    private void captureVideo(Activity activity) {
        try {
            Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "VID_" + timeStamp + "_" + ".mp4";

            mediaFile = FileUtils.getFilePath(imageFileName, activity.getApplicationContext(), "video/mp4");

            Uri videoFileUri = FileProvider.getUriForFile(activity, Utils.getMetaDataValue(activity, PACKAGE_NAME) + ".provider", mediaFile);

            videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoFileUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                videoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ClipData clip =
                        ClipData.newUri(activity.getContentResolver(), "a Video", videoFileUri);

                videoIntent.setClipData(clip);
                videoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            } else {
                List<ResolveInfo> resInfoList =
                        activity.getPackageManager()
                                .queryIntentActivities(videoIntent, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    activity.grantUriPermission(packageName, videoFileUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    activity.grantUriPermission(packageName, videoFileUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);

                }
            }

            if (videoIntent.resolveActivity(activity.getApplicationContext().getPackageManager()) != null) {
                if (mediaFile != null) {
                    videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                    activity.startActivityForResult(videoIntent, REQUEST_CODE_CAPTURE_VIDEO);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processAudio(AppCompatActivity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfPermissionForAudioRecording(activity)) {
            new StringeePermissions(activity, layout).requestAudio();
        } else if (PermissionsUtils.isAudioRecordingPermissionGranted(activity)) {
            FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
            DialogFragment fragment = AudioMessageFragment.newInstance();
            FragmentTransaction fragmentTransaction = supportFragmentManager
                    .beginTransaction().add(fragment, "AudioMessageFragment");
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    public void processAudioAction(AppCompatActivity activity) {
        if (PermissionsUtils.isStoragePermissionGranted(activity)) {
            processAudio(activity);
        } else {
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                new StringeePermissions(activity, layout).requestStoragePermissions();
            } else {
                processAudio(activity);
            }
        }
    }

    public void processContactAction(Activity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForContactPermission(activity)) {
            new StringeePermissions(activity).requestContactPermission();
        } else {
            Intent contactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            contactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            activity.startActivityForResult(contactIntent, REQUEST_CODE_CONTACT_SHARE);
        }
    }

    public void processFileAction(Activity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission(activity)) {
            new StringeePermissions(activity).requestStoragePermissions();
        } else {
            Intent intent = new Intent(this, SelectFileActivity.class);
            activity.startActivityForResult(intent, REQUEST_CODE_FILE);
        }
    }

    private void registerReceivers() {
        IntentFilter filter1 = new IntentFilter(Notify.CONVERSATION_ADDED.getValue());
        convAddedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Conversation conversation = (Conversation) intent.getSerializableExtra("conversation");
                ConversationListFragment fragment = (ConversationListFragment) getFragmentByTag(ConversationActivity.this, "ConversationFragment");
                if (fragment != null) {
                    fragment.onAddConversation(conversation);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(convAddedReceiver, filter1);

        IntentFilter filter2 = new IntentFilter(Notify.CONVERSATION_UPDATED.getValue());
        convUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Conversation conversation = (Conversation) intent.getSerializableExtra("conversation");
                ConversationListFragment fragment = (ConversationListFragment) getFragmentByTag(ConversationActivity.this, "ConversationFragment");
                if (fragment != null) {
                    fragment.onUpdateConversation(conversation);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(convUpdatedReceiver, filter2);
    }

    public void unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(convAddedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(convUpdatedReceiver);
    }
}
