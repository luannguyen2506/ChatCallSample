package com.stringee.kit.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stringee.apptoappcallsample.R;
import com.stringee.kit.ui.activity.ConversationActivity;
import com.stringee.kit.ui.commons.Common;
import com.stringee.kit.ui.commons.utils.AlphaNumberColorUtil;
import com.stringee.kit.ui.commons.utils.Utils;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationAdapter extends RecyclerView.Adapter {

    private LayoutInflater mInflater;
    private List<Conversation> conversationList;
    private Context context;

    public ConversationAdapter(Context context, List<Conversation> conversations) {
        this.context = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        conversationList = conversations;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mInflater == null) {
            return null;
        }

        View v = mInflater.inflate(R.layout.conversation_row, parent, false);
        return new ConversationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ConversationViewHolder viewHolder = (ConversationViewHolder) holder;
        final Conversation conversation = conversationList.get(position);
        String convName = conversation.getName();
        if (convName == null || convName.length() == 0) {
            convName = "";
            List<User> participants = conversation.getParticipants();
            for (int j = 0; j < participants.size(); j++) {
                String userId = participants.get(j).getUserId();
                if (!conversation.isGroup() && Common.client.getUserId() != null && Common.client.getUserId().equals(userId)) {
                    continue;
                } else {
                    String name = participants.get(j).getName();
                    if (name == null || name.trim().length() == 0) {
                        name = participants.get(j).getUserId();
                    }
                    convName = convName + name + ",";
                }
            }
            if (convName.length() > 0) {
                convName = convName.substring(0, convName.length() - 1);
            } else {
                convName = "Unknown";
            }
        }
        String text = conversation.getText();
        switch (conversation.getLastMsgType()) {
            case Message.TYPE_TEXT:
                viewHolder.attachmentImageView.setVisibility(View.GONE);
                text = conversation.getText();
                break;
            case Message.TYPE_CREATE_GROUP:
                viewHolder.attachmentImageView.setVisibility(View.GONE);
                text = conversation.getCreator() + " created the group.";
                break;
            case Message.TYPE_LOCATION:
                viewHolder.attachmentImageView.setVisibility(View.VISIBLE);
                viewHolder.attachmentImageView.setImageResource(R.drawable.stringee_ic_location_icon);
                break;
            case Message.TYPE_AUDIO:
            case Message.TYPE_FILE:
            case Message.TYPE_PHOTO:
            case Message.TYPE_VIDEO:
                viewHolder.attachmentImageView.setVisibility(View.VISIBLE);
                viewHolder.attachmentImageView.setImageResource(R.drawable.stringee_ic_action_attachment);
                break;
            case Message.TYPE_CONTACT:
                viewHolder.attachmentImageView.setVisibility(View.VISIBLE);
                viewHolder.attachmentImageView.setImageResource(R.drawable.stringee_ic_contact_icon);
                break;
        }
        String datetime = Utils.getFormattedDateAndTime(conversation.getLastTimeNewMsg());
//        String url = "http://goo.gl/gEgYUd";

        if (conversation.isGroup()) {
            viewHolder.alphabeticTextView.setVisibility(View.GONE);
            viewHolder.avatarImageView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.alphabeticTextView.setVisibility(View.VISIBLE);
            viewHolder.avatarImageView.setVisibility(View.GONE);
            char firstLetter = convName.toUpperCase().charAt(0);
            Character colorKey = AlphaNumberColorUtil.alphabetBackgroundColorMap.containsKey(firstLetter) ? firstLetter : null;
            GradientDrawable bgShape = (GradientDrawable) viewHolder.alphabeticTextView.getBackground();
            bgShape.setColor(context.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(colorKey)));
            viewHolder.alphabeticTextView.setText(String.valueOf(firstLetter));
        }

        viewHolder.titleTextView.setText(convName);
        viewHolder.subTitleTextView.setText(text);
        viewHolder.timeTextView.setText(datetime);
//        Glide.with(context).load(url).into(viewHolder.avatarImageView);

        int totalUnread = conversation.getTotalUnread();
        if (totalUnread > 0) {
            viewHolder.unReadTextView.setVisibility(View.VISIBLE);
            viewHolder.unReadTextView.setText(String.valueOf(totalUnread));
        } else {
            viewHolder.unReadTextView.setVisibility(View.GONE);
        }

        viewHolder.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ConversationActivity.class);
                intent.putExtra("conversation", conversation);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rootView;
        TextView titleTextView, subTitleTextView, timeTextView, alphabeticTextView, unReadTextView;
        ImageView attachmentImageView;
        CircleImageView avatarImageView;

        public ConversationViewHolder(View view) {
            super(view);

            rootView = view.findViewById(R.id.rootView);
            avatarImageView = (CircleImageView) view.findViewById(R.id.avatarImage);
            titleTextView = (TextView) view.findViewById(R.id.title);
            subTitleTextView = (TextView) view.findViewById(R.id.subTitle);
            timeTextView = (TextView) view.findViewById(R.id.datetime);
            alphabeticTextView = (TextView) view.findViewById(R.id.alphabeticImage);
            unReadTextView = (TextView) view.findViewById(R.id.totalUnread);
            attachmentImageView = view.findViewById(R.id.attachmentIcon);
        }
    }
}
