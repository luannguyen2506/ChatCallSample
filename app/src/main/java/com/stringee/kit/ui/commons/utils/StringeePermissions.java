package com.stringee.kit.ui.commons.utils;

import android.Manifest;
import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.LinearLayout;

import com.stringee.apptoappcallsample.R;
import com.stringee.kit.ui.activity.StringeeLocationActivity;

/**
 * Created by sunil on 22/1/16.
 */
public class StringeePermissions {
    private LinearLayout snackBarLayout;
    private Activity activity;

    public StringeePermissions(Activity activity, LinearLayout linearLayout) {
        this.activity = activity;
        this.snackBarLayout = linearLayout;
    }

    public StringeePermissions(Activity activity) {
        this(activity, null);
    }

    public void requestStoragePermissions() {
        if (PermissionsUtils.shouldShowRequestForStoragePermission(activity)) {
            showSnackBar(R.string.storage_permission, PermissionsUtils.PERMISSIONS_STORAGE, PermissionsUtils.REQUEST_STORAGE);
        } else {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSIONS_STORAGE, PermissionsUtils.REQUEST_STORAGE);
        }
    }

    public void requestLocationPermissions() {
        if (PermissionsUtils.shouldShowRequestForLocationPermission(activity)) {
            showSnackBar(R.string.location_permission, PermissionsUtils.PERMISSIONS_LOCATION, PermissionsUtils.REQUEST_LOCATION);
        } else {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSIONS_LOCATION, PermissionsUtils.REQUEST_LOCATION);
        }
    }

    public void requestAudio() {
        if (PermissionsUtils.shouldShowRequestForLocationPermission(activity)) {
            showSnackBar(R.string.record_audio, PermissionsUtils.PERMISSIONS_LOCATION, PermissionsUtils.REQUEST_AUDIO_RECORD);
        } else {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSIONS_RECORD_AUDIO, PermissionsUtils.REQUEST_AUDIO_RECORD);
        }
    }

    public void requestCameraPermission() {
        if (PermissionsUtils.shouldShowRequestForCameraPermission(activity)) {
            showSnackBar(R.string.phone_camera_permission, PermissionsUtils.PERMISSION_CAMERA, PermissionsUtils.REQUEST_CAMERA);
        } else {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_CAMERA, PermissionsUtils.REQUEST_CAMERA);
        }
    }

    public void requestContactPermission() {
        if (PermissionsUtils.shouldShowRequestForContactPermission(activity)) {
            showSnackBar(R.string.contact_permission, PermissionsUtils.PERMISSION_CONTACT, PermissionsUtils.REQUEST_CONTACT);
        } else {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_CONTACT, PermissionsUtils.REQUEST_CONTACT);
        }
    }

    public void requestCameraAndRecordPermission() {
        if (PermissionsUtils.shouldShowRequestForVideoPermission(activity)) {
            showSnackBar(!PermissionsUtils.checkPermissionForCameraAndMicrophone(activity) ? R.string.camera_audio_permission : !PermissionsUtils.isAudioRecordingPermissionGranted(activity) ? R.string.record_audio : !PermissionsUtils.isCameraPermissionGranted(activity) ? R.string.phone_camera_permission : R.string.camera_audio_permission, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PermissionsUtils.REQUEST_CAMERA_AUDIO);
        } else {
            PermissionsUtils.requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PermissionsUtils.REQUEST_CAMERA_AUDIO);
        }
    }

    public void checkRuntimePermissionForLocationActivity() {
        if (PermissionsUtils.checkSelfPermissionForLocation(activity)) {
            requestLocationPermissions();
        } else {
            ((StringeeLocationActivity) activity).processingLocation();
        }
    }

    public void showSnackBar(int resId, final String[] permissions, final int requestCode) {
        if (snackBarLayout != null) {
            Snackbar.make(snackBarLayout, resId,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            PermissionsUtils.requestPermissions(activity, permissions, requestCode);
                        }
                    }).show();
        }
    }
}
