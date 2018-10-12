package com.stringee.kit.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.stringee.apptoappcallsample.R;
import com.stringee.kit.ui.adapter.ConversationAdapter;
import com.stringee.kit.ui.commons.Common;
import com.stringee.kit.ui.commons.Constant;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.listeners.CallbackListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConversationListFragment extends Fragment {

    private List<Conversation> conversationList = new ArrayList<>();
    private ConversationAdapter adapter;

    private RecyclerView conversationListView;
    private LinearLayoutManager linearLayoutManager;
    private ProgressBar prLoading;

    private boolean isLoading;
    private boolean isLast;
    private boolean isTouched;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.conversations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);

        prLoading = view.findViewById(R.id.prLoading);
        conversationListView = view.findViewById(R.id.conversationList);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        conversationListView.setLayoutManager(linearLayoutManager);
        conversationListView.setHasFixedSize(true);
        adapter = new ConversationAdapter(getActivity(), conversationList);
        conversationListView.setAdapter(adapter);

        conversationListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                isTouched = true;
                return false;
            }
        });

        conversationListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && !isLoading && !isLast && isTouched) {
                    prLoading.setVisibility(View.VISIBLE);
                    long lastUpdate = conversationList.get(conversationList.size() - 1).getLastTimeNewMsg();
                    Common.client.getConversationsBefore(lastUpdate, Constant.CONVERSATIONS_COUNT, new CallbackListener<List<Conversation>>() {
                        @Override
                        public void onSuccess(final List<Conversation> conversations) {
                            isLoading = false;
                            prLoading.setVisibility(View.GONE);
                            if (conversations.size() < Constant.CONVERSATIONS_COUNT) {
                                isLast = true;
                            }
                            if (conversations.size() > 0 && getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        merge(conversations);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        Common.client.getLastConversations(Constant.CONVERSATIONS_COUNT, new CallbackListener<List<Conversation>>() {
            @Override
            public void onSuccess(final List<Conversation> conversations) {
                if (conversations.size() > 0) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                merge(conversations);
                                adapter.notifyDataSetChanged();
                                // Get latest conversations from server
                                getLatestConversations();

                            }
                        });
                    }
                } else {
                    getLatestConversations();
                }
            }
        });

        return view;
    }

    private void getLatestConversations() {
        Common.client.getConversationsAfter(0, Constant.CONVERSATIONS_COUNT, new CallbackListener<List<Conversation>>() {
            @Override
            public void onSuccess(final List<Conversation> conversations) {
                if (conversations.size() < Constant.CONVERSATIONS_COUNT) {
                    isLast = true;
                }
                if (conversations.size() > 0 && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            merge(conversations);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    private void merge(List<Conversation> conversations) {
        conversationList.addAll(conversations);
        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                String convId = conversation.getSId();
                String convId2 = t1.getSId();
                if (convId.equals(convId2)) {
                    long lastUpdate = conversation.getLastTimeNewMsg();
                    long lastUpdate2 = t1.getLastTimeNewMsg();
                    if (lastUpdate > lastUpdate2) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    return convId.compareTo(convId2);
                }
            }
        });

        for (int i = conversationList.size() - 1; i >= 0; i--) {
            if (i > 0) {
                String convId = conversationList.get(i).getSId();
                String convId2 = conversationList.get(i - 1).getSId();
                if (convId != null && convId2 != null && convId.equals(convId2)) {
                    conversationList.remove(i);
                }
            }
        }

        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                if (conversation.getLastTimeNewMsg() > t1.getLastTimeNewMsg()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
    }

    public void onAddConversation(Conversation conversation) {
        // Update conversation list
        conversationList.add(conversation);
        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                if (conversation.getLastTimeNewMsg() > t1.getLastTimeNewMsg()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        adapter.notifyDataSetChanged();
    }

    public void onUpdateConversation(Conversation conversation) {
        for (int i = conversationList.size() - 1; i >= 0; i--) {
            Conversation conversation1 = conversationList.get(i);
            if (conversation.getSId().equals(conversation1.getSId())) {
                conversationList.set(i, conversation);
                break;
            }
        }
        Collections.sort(conversationList, new Comparator<Conversation>() {
            @Override
            public int compare(Conversation conversation, Conversation t1) {
                if (conversation.getLastTimeNewMsg() > t1.getLastTimeNewMsg()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        adapter.notifyDataSetChanged();
    }
}
