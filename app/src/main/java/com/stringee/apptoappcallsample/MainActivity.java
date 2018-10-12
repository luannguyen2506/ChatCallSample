package com.stringee.apptoappcallsample;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.stringee.StringeeClient;
import com.stringee.apptoappcallsample.utils.Utils;
import com.stringee.call.StringeeCall;
import com.stringee.exception.StringeeError;
import com.stringee.kit.ui.activity.ConversationActivity;
import com.stringee.kit.ui.commons.Common;
import com.stringee.kit.ui.commons.Constant;
import com.stringee.kit.ui.commons.Notify;
import com.stringee.kit.ui.commons.PrefUtils;
import com.stringee.kit.ui.notification.NotificationService;
import com.stringee.listener.StatusListener;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.messaging.listeners.ChatClientListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ChatClientListener {

    private String to;
    public static Map<String, StringeeCall> callsMap = new HashMap<>();

    private String userId = "stringee2";
    private String name = "Stringee 2";

    private EditText etTo;
    private TextView tvUserId;
    private ProgressDialog progressDialog;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private final String PREF_NAME = "com.stringee.onetoonecallsample";
    private final String IS_TOKEN_REGISTERED = "is_token_registered";
    private final String TOKEN = "token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserId = (TextView) findViewById(R.id.tv_userid);

        Button btnVoiceCall = (Button) findViewById(R.id.btn_voice_call);
        btnVoiceCall.setOnClickListener(this);
        Button btnVideoCall = (Button) findViewById(R.id.btn_video_call);
        btnVideoCall.setOnClickListener(this);

        Button btnChat = (Button) findViewById(R.id.btn_chat);
        btnChat.setOnClickListener(this);
        Button btnConversations = (Button) findViewById(R.id.btn_conversations);
        btnConversations.setOnClickListener(this);

        etTo = (EditText) findViewById(R.id.et_to);

        Button btnUnregister = (Button) findViewById(R.id.btn_unregister);
        btnUnregister.setOnClickListener(this);

        progressDialog = ProgressDialog.show(this, "", "Connecting...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        initAndConnectStringee();
    }

    public void initAndConnectStringee() {
        Common.client = new StringeeClient(this);
        Common.client.setConnectionListener(new StringeeConnectionListener() {
            @Override
            public void onConnectionConnected(final StringeeClient stringeeClient, boolean isReconnecting) {
                PrefUtils.putString(Constant.USER_NAME, userId);
                PrefUtils.putString(Constant.NAME, name);
                boolean isTokenRegistered = sharedPreferences.getBoolean(IS_TOKEN_REGISTERED, false);
                if (!isTokenRegistered) {
                    final String token = FirebaseInstanceId.getInstance().getToken();
                    Common.client.registerPushToken(token, new StatusListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("Stringee", "Register push token successfully.");
                            editor.putBoolean(IS_TOKEN_REGISTERED, true);
                            editor.putString(TOKEN, token);
                            editor.commit();
                        }

                        @Override
                        public void onError(StringeeError error) {
                            Log.d("Stringee", "Unregister push token: " + error.getMessage());
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        tvUserId.setText("Connected as: " + stringeeClient.getUserId());
                        Utils.reportMessage(MainActivity.this, "StringeeClient is connected.");
                    }
                });
            }

            @Override
            public void onConnectionDisconnected(StringeeClient stringeeClient, boolean isReconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "StringeeClient disconnected.");
                    }
                });
            }

            @Override
            public void onIncomingCall(final StringeeCall stringeeCall) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callsMap.put(stringeeCall.getCallId(), stringeeCall);
                        Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                        intent.putExtra("call_id", stringeeCall.getCallId());
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onConnectionError(StringeeClient stringeeClient, final StringeeError stringeeError) {
                Log.d("Stringee", "StringeeClient fails to connect: " + stringeeError.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "StringeeClient fails to connect: " + stringeeError.getMessage());
                    }
                });
            }

            @Override
            public void onRequestNewToken(StringeeClient stringeeClient) {
                // Get new token here and connect to Stringe server
            }

            @Override
            public void onCustomMessage(String s, JSONObject jsonObject) {

            }
        });
        Common.client.setChatClientListener(this);
        getTokenAndConnect(this, userId, name);
    }

    public static void getTokenAndConnect(final Context context, final String userId, final String name) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String finalName = name;
                if (name != null) {
                    finalName = name.replace(" ", "%20");
                }
                String url = "https://v1.stringee.com/samples_and_docs/access_token/gen_access_token_stringeetestdev.php?userId=" + userId + "&displayName=" + finalName + "&avatarUrl=";
                RequestQueue queue = Volley.newRequestQueue(context);
                StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String token = jsonObject.getString("access_token");
                            Common.client.connect(token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(request);
            }
        });
        thread.start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_voice_call:
                to = etTo.getText().toString();
                if (to.trim().length() > 0) {
                    if (Common.client.isConnected()) {
                        Intent intent = new Intent(this, OutgoingCallActivity.class);
                        intent.putExtra("from", Common.client.getUserId());
                        intent.putExtra("to", to);
                        intent.putExtra("is_video_call", false);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(this, "Stringee session not connected");
                    }
                }
                break;
            case R.id.btn_video_call:
                to = etTo.getText().toString();
                if (to.trim().length() > 0) {
                    if (Common.client.isConnected()) {
                        Intent intent = new Intent(this, OutgoingCallActivity.class);
                        intent.putExtra("from", Common.client.getUserId());
                        intent.putExtra("to", to);
                        intent.putExtra("is_video_call", true);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(this, "Stringee session not connected");
                    }
                }
                break;
            case R.id.btn_unregister:
                Common.client.unregisterPushToken(sharedPreferences.getString(TOKEN, ""), new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Stringee", "Unregister push token successfully.");
                        editor.remove(IS_TOKEN_REGISTERED);
                        editor.remove(TOKEN);
                        editor.commit();
                    }
                });
                break;
            case R.id.btn_conversations:
                Intent intent = new Intent(this, ConversationActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_chat:
                final String editTextValue = etTo.getText().toString().trim();
                if (TextUtils.isEmpty(editTextValue) || etTo.getText().toString().trim().length() == 0) {
                    Toast.makeText(this, R.string.empty_user_id_info, Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog = ProgressDialog.show(this, "", "Executing...");
                progressDialog.setCancelable(true);
                progressDialog.show();
                String[] pars = editTextValue.split(",");
                List<User> participants = new ArrayList<>();
                for (int i = 0; i < pars.length; i++) {
                    User identity = new User(pars[i]);
                    participants.add(identity);
                }
                Common.client.createConversation(participants, new CallbackListener<Conversation>() {
                    @Override
                    public void onSuccess(final Conversation conv) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                                intent.putExtra("conversation", conv);
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onError(final StringeeError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Utils.reportMessage(MainActivity.this, error.getMessage());
                            }
                        });
                    }
                });
                break;
        }
    }

    @Override
    public void onConversationAdded(final Conversation conversation) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Message message = conversation.getLastReceivedMessage(MainActivity.this);
                if (message != null) {
                    notifyMessage(message);
                }

                Intent intent = new Intent(Notify.CONVERSATION_ADDED.getValue());
                intent.putExtra("conversation", conversation);
                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            }
        });
    }

    @Override
    public void onConversationUpdated(final Conversation conversation) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Message message = conversation.getLastReceivedMessage(MainActivity.this);
                if (message != null) {
                    notifyMessage(message);
                }

                Intent intent = new Intent(Notify.CONVERSATION_UPDATED.getValue());
                intent.putExtra("conversation", conversation);
                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            }
        });
    }

    @Override
    public void onConversationDeleted(Conversation conversation) {

    }

    private void notifyMessage(Message message) {
        if (!(Common.isChatting && Common.currentConvId != null && Common.currentConvId.equals(message.getConversationId())) && message.getMsgType() == Message.MESSAGE_TYPE_RECEIVE) {
            User user = Common.client.getUser(message.getAuthor());
            Conversation conversation = Common.client.getConversationBySid(message.getConversationId());
            NotificationService.showNotification(this, message, conversation, user);
        }
    }
}
