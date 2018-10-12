package com.stringee.apptoappcallsample;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.stringee.kit.ui.commons.Common;
import com.stringee.listener.StatusListener;

/**
 * Created by luannguyen on 2/7/2018.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        // Register the token to Stringee Server
        if (Common.client != null && Common.client.isConnected()) {
            Common.client.registerPushToken(refreshedToken, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        } else {
            // Handle your code here
        }
    }
}
