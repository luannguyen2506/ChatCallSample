package com.stringee.kit.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.stringee.apptoappcallsample.R;
import com.stringee.exception.StringeeError;
import com.stringee.kit.ui.activity.ConversationActivity;
import com.stringee.kit.ui.activity.StringeeLocationActivity;
import com.stringee.kit.ui.adapter.MessageAdapter;
import com.stringee.kit.ui.adapter.StringeeMultimediaPopupAdapter;
import com.stringee.kit.ui.commons.Common;
import com.stringee.kit.ui.commons.Constant;
import com.stringee.kit.ui.commons.PrefUtils;
import com.stringee.kit.ui.commons.utils.FileUtils;
import com.stringee.kit.ui.commons.utils.Utils;
import com.stringee.kit.ui.notification.NotificationService;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ChatFragment extends Fragment implements View.OnClickListener {

    private Conversation conversation;
    private GridView attachGridView;
    private EditText messageEditText;
    private ImageButton attachButton;
    private ImageButton sendMessageButton;
    private List<Message> messages = new ArrayList<>();
    private RecyclerView messagesRecyclerView;
    private MessageAdapter adapter;
    private ImageButton recordButton;
    private LinearLayoutManager linearLayoutManager;
    private ProgressBar prLoading;
    private boolean isLoading;
    private boolean isTop = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
            conversation = (Conversation) args.getSerializable("conversation");
        }
        if (conversation == null) {
            return;
        }

        conversation.addListener((ConversationActivity) getActivity());
        Common.currentConvId = conversation.getSId();
        String title = conversation.getName();
        if (title == null || title.length() == 0) {
            title = "";
            List<User> pars = conversation.getParticipants();
            for (int i = 0; i < pars.size(); i++) {
                String name = pars.get(i).getName();
                if (name == null || name.trim().length() == 0) {
                    name = pars.get(i).getUserId();
                }
                if (conversation.isGroup()) {
                    title = title + name + ",";
                } else {
                    String userId = pars.get(i).getUserId();
                    if (!userId.equals(PrefUtils.getString(Constant.USER_NAME, ""))) {
                        title = title + name + ",";
                    }
                }
            }
            if (title.length() > 0) {
                title = title.substring(0, title.length() - 1);
            } else {
                title = "Unknown";
            }
        }
        ActionBar actionBar = ((ConversationActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        prLoading = view.findViewById(R.id.prLoading);

        attachButton = view.findViewById(R.id.attachButton);
        attachButton.setOnClickListener(this);

        recordButton = view.findViewById(R.id.recordButton);
        recordButton.setOnClickListener(this);

        sendMessageButton = view.findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(this);

        attachGridView = view.findViewById(R.id.attachGridView);
        prepareAttachmentData();

        messageEditText = view.findViewById(R.id.messageEditText);
        messageEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (attachGridView.getVisibility() == View.VISIBLE) {
                    attachGridView.setVisibility(View.GONE);
                }
                messageEditText.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    }
                }, 500);
            }
        });
        messageEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    if (attachGridView.getVisibility() == View.VISIBLE) {
                        attachGridView.setVisibility(View.GONE);
                    }

                    messageEditText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            messagesRecyclerView.scrollToPosition(messages.size() - 1);
                        }
                    }, 500);
                }
            }
        });
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().trim().length() > 0) {
                    handleSendAndRecordButtonView(true);
                } else {
                    handleSendAndRecordButtonView(false);
                }
            }
        });

        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        messagesRecyclerView.setLayoutManager(linearLayoutManager);
        messagesRecyclerView.setHasFixedSize(true);

        adapter = new MessageAdapter(getActivity(), messages);
        messagesRecyclerView.setAdapter(adapter);

        messagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(-1) && !isLoading && !isTop) {
                    isLoading = true;
                    prLoading.setVisibility(View.VISIBLE);
                    // Scroll to the top
                    long seq = 0;
                    for (int i = 0; i < messages.size(); i++) {
                        Message message = messages.get(i);
                        if (message.getType() == Message.TYPE_TEMP_DATE) {
                            continue;
                        } else {
                            seq = message.getSequence();
                            break;
                        }
                    }
                    conversation.getMessagesBefore(Common.client, seq, Constant.MESSAGES_COUNT, new CallbackListener<List<Message>>() {
                        @Override
                        public void onSuccess(final List<Message> lstMessages) {
                            if (getActivity() == null) {
                                return;
                            }
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (lstMessages.size() < Constant.MESSAGES_COUNT) {
                                        isTop = true;
                                    }
                                    prLoading.setVisibility(View.GONE);
                                    if (lstMessages.size() > 0) {
                                        addTempDate(lstMessages);
                                        for (int i = 0; i < messages.size(); i++) {
                                            if (messages.get(0).getType() == Message.TYPE_TEMP_DATE) {
                                                messages.remove(0);
                                            } else {
                                                break;
                                            }
                                        }
                                        long dayDiff = Utils.daysBetween(new Date(lstMessages.get(lstMessages.size() - 1).getCreatedAt()), new Date(messages.get(0).getCreatedAt()));
                                        if (dayDiff >= 1) {
                                            Message tempMessage = new Message();
                                            tempMessage.setType(Message.TYPE_TEMP_DATE);
                                            tempMessage.setCreatedAt(messages.get(0).getCreatedAt());
                                            lstMessages.add(tempMessage);
                                        }
                                        messages.addAll(0, lstMessages);
                                        adapter.notifyDataSetChanged();
                                        messagesRecyclerView.scrollToPosition(lstMessages.size());
                                    }
                                    isLoading = false;
                                }
                            });
                        }
                    });
                }
            }
        });

        getMessages();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Common.isChatting = true;
        if (conversation != null) {
            NotificationService.cancelNotification(getActivity(), conversation.getId());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        attachGridView.setVisibility(View.GONE);
        Common.isChatting = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.attachButton:
                ((ConversationActivity) getActivity()).hideKeyboard(attachButton);
                attachGridView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (attachGridView.getVisibility() == View.VISIBLE) {
                            attachGridView.setVisibility(View.GONE);
                        } else {
                            attachGridView.setVisibility(View.VISIBLE);
                            messagesRecyclerView.scrollToPosition(messages.size() - 1);
                        }
                    }
                }, 200);
                break;
            case R.id.sendMessageButton:
                String text = messageEditText.getText().toString().trim();
                if (text.length() > 0) {
                    Message message = new Message(text);
                    conversation.sendMessage(Common.client, message, new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(StringeeError error) {

                        }
                    });
                    messageEditText.setText("");
                }
                break;
            case R.id.recordButton:
                ((ConversationActivity) getActivity()).processAudioAction((ConversationActivity) getActivity());
                break;
        }
    }

    private void getMessages() {
        prLoading.setVisibility(View.VISIBLE);
        // Get last messages
        conversation.getLastMessages(Common.client, Constant.MESSAGES_COUNT, new CallbackListener<List<Message>>() {
            @Override
            public void onSuccess(final List<Message> messageList) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        prLoading.setVisibility(View.GONE);
                        messages.addAll(messageList);
                        addTempDate(messages);
                        adapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                        readMessages(messages);

                        conversation.getMessagesBefore(Common.client, Long.MAX_VALUE, Constant.MESSAGES_COUNT, new CallbackListener<List<Message>>() {
                            @Override
                            public void onSuccess(final List<Message> messages1) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (getActivity() == null) {
                                            return;
                                        }
                                        if (messages1.size() > 0) {
                                            if (messages.size() > 0 && messages1.get(messages1.size() - 1).getSId().equals(messages.get(messages.size() - 1).getSId())) {
                                                return;
                                            }

                                            if (messages.size() == 0) {
                                                messages.addAll(messages1);
                                            } else {
                                                String lstMsgId = messages.get(messages.size() - 1).getSId();
                                                int index = -1;
                                                for (int i = 0; i < messages1.size() - 1; i++) {
                                                    if (messages1.get(i).getSId().equals(lstMsgId)) {
                                                        index = i + 1;
                                                        break;
                                                    }
                                                }

                                                if (index < 0) {
                                                    messages.clear();
                                                    messages.addAll(messages1);
                                                } else {
                                                    for (int j = index; j < messages1.size(); j++) {
                                                        messages.add(messages1.get(j));
                                                    }
                                                }
                                            }

                                            addTempDate(messages);
                                            adapter.notifyDataSetChanged();
                                            messagesRecyclerView.scrollToPosition(messages.size() - 1);
                                            readMessages(messages);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void prepareAttachmentData() {
        String[] allValues = getResources().getStringArray(R.array.multimediaOptions_without_price_text);
        String[] allIcons = getResources().getStringArray(R.array.multimediaOptionIcons_without_price);

        StringeeMultimediaPopupAdapter adapter = new StringeeMultimediaPopupAdapter(getContext(), Arrays.asList(allIcons), Arrays.asList(allValues));
        attachGridView.setAdapter(adapter);
        attachGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                attachGridView.setVisibility(View.GONE);
                ((ConversationActivity) getActivity()).setAttachType(i);
                switch (i) {
                    case 0:
                        Intent intent = new Intent(getActivity(), StringeeLocationActivity.class);
                        getActivity().startActivityForResult(intent, ConversationActivity.REQUEST_CODE_LOCATION);
                        break;
                    case 1:
                        ((ConversationActivity) getActivity()).processCameraAction(getActivity());
                        break;
                    case 2:
                        ((ConversationActivity) getActivity()).processFileAction(getActivity());
                        break;
                    case 3:
                        ((ConversationActivity) getActivity()).processAudioAction((ConversationActivity) getActivity());
                        break;
                    case 4:
                        ((ConversationActivity) getActivity()).processVideoAction(getActivity());
                        break;
                    case 5:
                        ((ConversationActivity) getActivity()).processContactAction(getActivity());
                        break;
                }
            }
        });
    }

    private List<Message> addTempDate(List<Message> messageList) {
        for (int i = messageList.size() - 1; i > 0; i--) {
            Message message1 = messageList.get(i);
            Message message2 = messageList.get(i - 1);
            if (message1.getType() == Message.TYPE_TEMP_DATE || message2.getType() == Message.TYPE_TEMP_DATE) {
                continue;
            }

            long dayDifference = Utils.daysBetween(new Date(message2.getCreatedAt()), new Date(message1.getCreatedAt()));
            if (dayDifference >= 1) {
                Message tempMessage = new Message();
                tempMessage.setType(Message.TYPE_TEMP_DATE);
                tempMessage.setCreatedAt(message1.getCreatedAt());
                messageList.add(i, tempMessage);
            }
        }

        if (messageList.get(0).getType() != Message.TYPE_TEMP_DATE) {
            Message firstTempMessage = new Message();
            firstTempMessage.setType(Message.TYPE_TEMP_DATE);
            firstTempMessage.setCreatedAt(messageList.get(0).getCreatedAt());
            messageList.add(0, firstTempMessage);
        }
        return messageList;
    }

    public void onAddMessage(final Message message) {
        messages.add(message);
        adapter.notifyDataSetChanged();
        messagesRecyclerView.scrollToPosition(messages.size() - 1);
        if (Common.isChatting && Common.currentConvId != null && Common.currentConvId.equals(message.getConversationId()) && message.getMsgType() == Message.MESSAGE_TYPE_RECEIVE) {
            message.markAsRead(Common.client, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }

    public void onUpdateMessage(final Message message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = messages.size() - 1; i >= 0; i--) {
                    Message message1 = messages.get(i);
                    if (message1.getMsgType() == Message.MESSAGE_TYPE_SEND && message.getSequence() >= message1.getSequence()) {
                        if (message.getState().getValue() > message1.getState().getValue()) {
                            message1.setState(message.getState());
                        } else {
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
        });
    }

    public void handleSendAndRecordButtonView(boolean isSendButtonVisible) {
        sendMessageButton.setVisibility(isSendButtonVisible ? View.VISIBLE : View.GONE);
        recordButton.setVisibility(isSendButtonVisible ? View.GONE : View.VISIBLE);
    }

    public void sendLocation(double latitude, double longitude) {
        Message message = new Message(Message.TYPE_LOCATION);
        message.setLatitude(latitude);
        message.setLongitude(longitude);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendContact(Uri contactData) {
        try {
            String vCardStr = FileUtils.vCard(contactData, getActivity());
            Message message = new Message(Message.TYPE_CONTACT);
            message.setContact(vCardStr);
            conversation.sendMessage(Common.client, message, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        } catch (Exception e) {
            Log.e("Exception::", "Exception", e);
        }
    }

    public void sendPhoto(String filePath) {
        Message message = new Message(Message.TYPE_PHOTO);
        message.setFilePath(filePath);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendAudio(String filePath, long duration) {
        Message message = new Message(Message.TYPE_AUDIO);
        message.setFilePath(filePath);
        message.setDuration(duration);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendVideo(File mediaFile) {
        Message message = new Message(Message.TYPE_VIDEO);
        message.setFilePath(mediaFile.getAbsolutePath());
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void sendFile(String filePath) {
        Message message = new Message(Message.TYPE_FILE);
        message.setFilePath(filePath);
        conversation.sendMessage(Common.client, message, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    private void readMessages(List<Message> lstMessages) {
        if (lstMessages.size() == 0) {
            return;
        }
        Message lastMsg = messages.get(messages.size() - 1);
        lastMsg.markAsRead(Common.client, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }
}