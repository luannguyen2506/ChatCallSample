package com.stringee.kit.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.stringee.apptoappcallsample.R;
import com.stringee.kit.ui.activity.ConversationActivity;
import com.stringee.kit.ui.activity.ImageFullScreenActivity;
import com.stringee.kit.ui.commons.Common;
import com.stringee.kit.ui.commons.ImageLoader;
import com.stringee.kit.ui.commons.ImageUtils;
import com.stringee.kit.ui.commons.MediaPlayerManager;
import com.stringee.kit.ui.commons.utils.AlphaNumberColorUtil;
import com.stringee.kit.ui.commons.utils.FileUtils;
import com.stringee.kit.ui.commons.utils.LocationUtils;
import com.stringee.kit.ui.commons.utils.PermissionsUtils;
import com.stringee.kit.ui.commons.utils.StringeePermissions;
import com.stringee.kit.ui.commons.utils.Utils;
import com.stringee.kit.ui.contact.STContactData;
import com.stringee.kit.ui.contact.STContactParser;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.view.View.GONE;

public class MessageAdapter extends RecyclerView.Adapter {

    private List<Message> messageList;
    private Context mContext;
    private Drawable sentIcon;
    private Drawable deliveredIcon;
    private Drawable pendingIcon;
    private Drawable readIcon;
    private ImageLoader loadImage;
    private FileUtils fileUtils;
    private View view;
    private Handler mHandler = new Handler();
    private int screenWidth;
    private int screenHeight;

    public MessageAdapter(Context context, List<Message> data) {
        mContext = context;
        messageList = data;

        sentIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_sent);
        deliveredIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_delivered);
        pendingIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_pending);
        readIcon = context.getResources().getDrawable(R.drawable.stringee_ic_action_message_read);

        fileUtils = new FileUtils();
        loadImage = new ImageLoader(context, ImageUtils.getLargestScreenDimension((Activity) context)) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return fileUtils.loadMessageImage(mContext, (String) data);
            }
        };
        loadImage.setImageFadeIn(false);
        loadImage.addImageCache(((FragmentActivity) context).getSupportFragmentManager(), 0.1f);

        Display dm = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenWidth = dm.getWidth();
        screenHeight = dm.getHeight();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mInflater == null) {
            return null;
        }

        if (viewType == 1) {
            View v1 = mInflater.inflate(R.layout.stringee_received_message_list_view, parent, false);
            return new MessageHolder(v1);
        } else if (viewType == 2) {
            View v2 = mInflater.inflate(R.layout.stringee_conversation_custom_message, parent, false);
            return new MessageHolder2(v2);
        } else if (viewType == 3) {
            View v3 = mInflater.inflate(R.layout.stringee_date_layout, parent, false);
            return new MessageHolder3(v3);
        }
        view = mInflater.inflate(R.layout.stringee_sent_message_list_view, parent, false);
        return new MessageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int itemViewType = getItemViewType(position);
        final Message message = messageList.get(position);

        if (itemViewType == 2) {
            MessageHolder2 messageHolder = (MessageHolder2) holder;
            messageHolder.customMessageTextView.setText(message.getText());
        } else if (itemViewType == 3) {
            MessageHolder3 messageHolder3 = (MessageHolder3) holder;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy");
            SimpleDateFormat simpleDateFormatDay = new SimpleDateFormat("EEEE");
            Date date = new Date(message.getCreatedAt());

            if (Utils.isSameDay(message.getCreatedAt())) {
                messageHolder3.dayTextView.setVisibility(View.VISIBLE);
                messageHolder3.timeTextView.setVisibility(GONE);
                messageHolder3.dayTextView.setText(R.string.today);
            } else {
                messageHolder3.dayTextView.setVisibility(View.VISIBLE);
                messageHolder3.timeTextView.setVisibility(View.VISIBLE);
                messageHolder3.dayTextView.setText(simpleDateFormatDay.format(date));
                messageHolder3.timeTextView.setText(simpleDateFormat.format(date));
            }
        } else {
            final MessageHolder messageHolder = (MessageHolder) holder;
            messageHolder.timeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));
            String text = message.getText();
            int type = message.getType();
            int msgType = message.getMsgType();

            switch (type) {
                case Message.TYPE_TEXT:
                    messageHolder.messageTextView.setVisibility(View.VISIBLE);
                    messageHolder.chatLocation.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.messageTextView.setText(text);
                    messageHolder.timeTextView.setTextColor(Color.parseColor("#a1aab1"));
                    messageHolder.timeTextView.setBackgroundColor(Color.TRANSPARENT);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setBackgroundResource(R.drawable.stringee_sent_message);
                    }
                    messageHolder.timeTextView.setVisibility(View.VISIBLE);
                    break;
                case Message.TYPE_LOCATION:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocation.setLayoutParams(new RelativeLayout.LayoutParams((int) (0.65 * screenWidth), ViewGroup.LayoutParams.WRAP_CONTENT));
                    messageHolder.chatLocation.setVisibility(View.VISIBLE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.timeTextView.setTextColor(Color.WHITE);
                    messageHolder.timeTextView.setBackgroundResource(R.drawable.time_bg);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                    messageHolder.timeTextView.setVisibility(View.VISIBLE);

                    loadImage.setImageFadeIn(false);
                    //Default image while loading image.
                    messageHolder.mapImageView.setVisibility(View.VISIBLE);
                    loadImage.setLoadingImage(R.drawable.stringee_map_offline_thumbnail);
                    loadImage.loadImage(LocationUtils.loadStaticMap(message), messageHolder.mapImageView);

                    messageHolder.chatLocation.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String uri = "geo:" + message.getLatitude() + "," + message.getLongitude();
                            Uri gmmIntentUri = Uri.parse(uri);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");
                            ((AppCompatActivity) mContext).startActivity(mapIntent);
                        }
                    });
                    break;
                case Message.TYPE_CONTACT:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                    messageHolder.timeTextView.setVisibility(View.GONE);
                    messageHolder.contactTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));

                    setupContactShareView(message, messageHolder);
                    break;
                case Message.TYPE_PHOTO:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.videoIconImageView.setVisibility(View.GONE);
                    messageHolder.timeTextView.setTextColor(Color.WHITE);
                    messageHolder.timeTextView.setBackgroundResource(R.drawable.time_bg);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                    messageHolder.timeTextView.setVisibility(View.VISIBLE);

                    float ratio = message.getImageRatio();
                    int width;
                    int height;
                    Display dm = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    if (ratio > 1) {
                        width = (int) (dm.getWidth() * 0.65);
                        height = (int) (width / ratio);
                    } else {
                        height = (int) (dm.getHeight() * 0.5);
                        width = (int) (ratio * height);
                    }

                    messageHolder.previewImageView.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
                    String filepath = message.getFilePath();
                    if (filepath != null && filepath.trim().length() > 0) {
                        Glide.with(mContext).load(Uri.parse("file://" + filepath)).apply(new RequestOptions().override(width, height))
                                .into(messageHolder.previewImageView);
                    } else {
                        Glide.with(mContext).load(message.getFileUrl()).apply(new RequestOptions().override(width, height)).into(messageHolder.previewImageView);
                    }

                    messageHolder.previewImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(mContext, ImageFullScreenActivity.class);
                            intent.putExtra("message", message);
                            mContext.startActivity(intent);
                        }
                    });
                    break;

                case Message.TYPE_VIDEO:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.VISIBLE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.GONE);
                    messageHolder.videoIconImageView.setVisibility(View.VISIBLE);
                    messageHolder.timeTextView.setTextColor(Color.WHITE);
                    messageHolder.timeTextView.setBackgroundResource(R.drawable.time_bg);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                    messageHolder.timeTextView.setVisibility(View.VISIBLE);

                    float ratio1 = message.getImageRatio();
                    int width1;
                    int height1;
                    Display dm1 = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    if (ratio1 > 1) {
                        width1 = (int) (dm1.getWidth() * 0.65);
                        height1 = (int) (width1 / ratio1);
                    } else {
                        height1 = (int) (dm1.getHeight() * 0.5);
                        width1 = (int) (ratio1 * height1);
                    }

                    messageHolder.previewImageView.setLayoutParams(new RelativeLayout.LayoutParams(width1, height1));
                    String thumbnail = message.getFilePath();
                    if (thumbnail != null && thumbnail.trim().length() > 0) {
                        Glide.with(mContext).load(Uri.parse("file://" + thumbnail)).apply(new RequestOptions().override(width1, height1)).into(messageHolder.previewImageView);
                    } else {
                        Glide.with(mContext).load(message.getFileUrl()).apply(new RequestOptions().override(width1, height1)).into(messageHolder.previewImageView);
                    }
                    final String videoFilePath = message.getFilePath();
                    if (videoFilePath != null && videoFilePath.trim().length() > 0) {
                        messageHolder.videoIconImageView.setImageResource(R.drawable.stringee_video_play_icon);
                    } else {
                        messageHolder.videoIconImageView.setImageResource(R.drawable.stringee_ic_action_download);
                    }
                    messageHolder.videoIconImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (videoFilePath != null && videoFilePath.trim().length() > 0) {
                                if (Build.VERSION.SDK_INT >= 24) {
                                    File videoFile = new File(videoFilePath);
                                    Uri fileUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", videoFile);
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(fileUri, "video/*");
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    mContext.startActivity(intent);
                                } else {
                                    Uri uri = Uri.parse(videoFilePath);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    intent.setDataAndType(uri, "video/*");
                                    mContext.startActivity(intent);
                                }
                            } else {
                                if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
                                    new StringeePermissions((ConversationActivity) mContext, ConversationActivity.layout).requestStoragePermissions();
                                } else {
                                    messageHolder.progressBar.setVisibility(View.VISIBLE);
                                    messageHolder.videoIconImageView.setVisibility(View.GONE);
                                    Common.client.downloadAttachment(mContext, message, new StatusListener() {
                                        @Override
                                        public void onSuccess() {
                                            messageHolder.videoIconImageView.setVisibility(View.VISIBLE);
                                            messageHolder.videoIconImageView.setImageResource(R.drawable.stringee_video_play_icon);
                                            messageHolder.progressBar.setVisibility(View.GONE);
                                        }
                                    });
                                }
                            }
                        }
                    });
                    break;
                case Message.TYPE_AUDIO:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.VISIBLE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                    messageHolder.timeTextView.setVisibility(View.GONE);
                    messageHolder.fileNameTextView.setVisibility(View.GONE);
                    messageHolder.audioSeekBar.setVisibility(View.VISIBLE);
                    messageHolder.audioTimeTextView.setVisibility(View.VISIBLE);
                    messageHolder.audioTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));
                    messageHolder.playImageView.setImageResource(R.drawable.ic_play_circle_outline);
                    messageHolder.fileNameTextView.setVisibility(View.GONE);
                    messageHolder.fileTimeTextView.setVisibility(View.GONE);
                    if (msgType == Message.MESSAGE_TYPE_SEND) {
                        messageHolder.playImageView.setColorFilter(0xff62727d);
                        messageHolder.downloadImageView.setColorFilter(0xff000000);
                        messageHolder.downloadLayout.setBackgroundResource(R.color.sent_message_text_color);
                    }
                    messageHolder.durationTextView.setVisibility(View.VISIBLE);
                    messageHolder.durationTextView.setText(Utils.getAudioTime(message.getDuration()));
                    String audioFilePath = message.getFilePath();
                    if (audioFilePath == null || audioFilePath.trim().length() == 0) {
                        messageHolder.downloadLayout.setVisibility(View.VISIBLE);
                        messageHolder.playImageView.setVisibility(View.GONE);
                    } else {
                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                        messageHolder.downloadLayout.setVisibility(View.GONE);
                    }

                    messageHolder.downloadLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
                                new StringeePermissions((ConversationActivity) mContext, ConversationActivity.layout).requestStoragePermissions();
                            } else {
                                messageHolder.audioProgressBar.setVisibility(View.VISIBLE);
                                messageHolder.downloadImageView.setVisibility(View.GONE);
                                Common.client.downloadAttachment(mContext, message, new StatusListener() {
                                    @Override
                                    public void onSuccess() {
                                        messageHolder.audioProgressBar.setVisibility(View.GONE);
                                        messageHolder.downloadLayout.setVisibility(View.GONE);
                                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    });

                    messageHolder.playImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            playAudio(message, messageHolder);
                        }
                    });
                    break;
                case Message.TYPE_FILE:
                    messageHolder.messageTextView.setVisibility(View.GONE);
                    messageHolder.chatLocationLayout.setVisibility(View.GONE);
                    messageHolder.mainContactShareLayout.setVisibility(View.GONE);
                    messageHolder.attachmentPreviewLayout.setVisibility(View.GONE);
                    messageHolder.attachmentAudioLayout.setVisibility(View.VISIBLE);
                    if (messageHolder.messageLayout != null) {
                        messageHolder.messageLayout.setBackgroundColor(Color.TRANSPARENT);
                    }
                    messageHolder.audioSeekBar.setVisibility(View.GONE);
                    messageHolder.timeTextView.setVisibility(View.GONE);
                    messageHolder.audioTimeTextView.setVisibility(View.VISIBLE);
                    messageHolder.playImageView.setImageResource(R.drawable.ic_documentreceive);
                    messageHolder.fileNameTextView.setVisibility(View.VISIBLE);
                    messageHolder.fileTimeTextView.setVisibility(View.VISIBLE);

                    messageHolder.fileTimeTextView.setText(Utils.getFormattedDate(message.getCreatedAt()));

                    if (msgType == Message.MESSAGE_TYPE_SEND) {
                        messageHolder.downloadImageView.setColorFilter(0xff000000);
                        messageHolder.downloadLayout.setBackgroundResource(R.color.sent_message_text_color);
                    }

                    String path = message.getFilePath();
                    if (path == null || path.trim().length() == 0) {
                        messageHolder.downloadLayout.setVisibility(View.VISIBLE);
                        messageHolder.playImageView.setVisibility(View.GONE);
                        path = message.getFileUrl();
                    } else {
                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                        messageHolder.downloadLayout.setVisibility(View.GONE);
                    }

                    messageHolder.durationTextView.setVisibility(View.GONE);
                    messageHolder.fileNameTextView.setVisibility(View.VISIBLE);

                    if (path != null) {
                        String[] parts = path.split("/");
                        if (parts.length > 0) {
                            String fileName = parts[parts.length - 1];
                            messageHolder.fileNameTextView.setText(fileName);
                        }
                    }

                    messageHolder.downloadLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
                                new StringeePermissions((ConversationActivity) mContext, ConversationActivity.layout).requestStoragePermissions();
                            } else {
                                messageHolder.audioProgressBar.setVisibility(View.VISIBLE);
                                messageHolder.downloadImageView.setVisibility(View.GONE);
                                Common.client.downloadAttachment(mContext, message, new StatusListener() {
                                    @Override
                                    public void onSuccess() {
                                        messageHolder.audioProgressBar.setVisibility(View.GONE);
                                        messageHolder.downloadLayout.setVisibility(View.GONE);
                                        messageHolder.playImageView.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }
                    });

                    messageHolder.playImageView.setOnClickListener(null);

                    messageHolder.attachmentAudioLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String localPath = message.getFilePath();
                            if (localPath != null && localPath.length() > 0) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.fromFile(new File(localPath)));
                                Intent viewIntent = Intent.createChooser(intent, "Choose an application");
                                mContext.startActivity(viewIntent);
                            }
                        }
                    });

                    break;
            }

            if (message.getMsgType() == Message.MESSAGE_TYPE_SEND) {
                Drawable statusIcon = pendingIcon;
                if (message.getState() == Message.State.SENT) {
                    statusIcon = sentIcon;
                } else if (message.getState() == Message.State.DELIVERED) {
                    statusIcon = deliveredIcon;
                } else if (message.getState() == Message.State.READ) {
                    statusIcon = readIcon;
                }
                if (type == Message.TYPE_AUDIO) {
                    messageHolder.audioTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);

                }
                if (type == Message.TYPE_FILE) {
                    messageHolder.fileTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);

                }
                if (type == Message.TYPE_CONTACT) {
                    messageHolder.contactTimeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
                }
                messageHolder.timeTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, statusIcon, null);
            } else {
                displayImage(message, messageHolder.contactImageView, messageHolder.alphabeticTextView);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);
        int type = message.getType();
        int msgType = message.getMsgType();
        if (type == Message.TYPE_CREATE_GROUP) {
            return 2;
        } else if (type == Message.TYPE_TEMP_DATE) {
            return 3;
        }
        if (msgType == Message.MESSAGE_TYPE_SEND) {
            return 0;
        }
        if (msgType == Message.MESSAGE_TYPE_RECEIVE) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private Message getItem(int position) {
        return messageList.get(position);
    }

    class MessageHolder extends RecyclerView.ViewHolder {

        TextView messageTextView, timeTextView, alphabeticTextView, shareContactName, shareContactNo, shareEmailContact, durationTextView, audioTimeTextView, contactTimeTextView, fileNameTextView, fileTimeTextView;
        RelativeLayout chatLocation;
        ImageView contactImageView, mapImageView, shareContactImage, previewImageView, playImageView, videoIconImageView, downloadImageView;
        Button addContactButton;
        LinearLayout mainContactShareLayout;
        RelativeLayout chatLocationLayout;
        RelativeLayout attachmentPreviewLayout, attachmentAudioLayout, downloadLayout;
        ProgressBar progressBar, audioProgressBar;
        SeekBar audioSeekBar;
        LinearLayout messageLayout;

        public MessageHolder(View itemView) {
            super(itemView);

            messageLayout = itemView.findViewById(R.id.messageLayout);
            messageTextView = itemView.findViewById(R.id.message);
            messageTextView.setMaxWidth((int) (0.65 * screenWidth));
            timeTextView = itemView.findViewById(R.id.createdAtTime);
            alphabeticTextView = itemView.findViewById(R.id.alphabeticImage);
            contactImageView = itemView.findViewById(R.id.contactImage);
            chatLocation = itemView.findViewById(R.id.chat_location);
            mapImageView = itemView.findViewById(R.id.static_mapview);
            mainContactShareLayout = itemView.findViewById(R.id.contact_share_layout);
            chatLocationLayout = itemView.findViewById(R.id.chat_location);
            attachmentPreviewLayout = itemView.findViewById(R.id.attachment_preview_layout);
            attachmentAudioLayout = itemView.findViewById(R.id.attach_audio_layout);


            shareContactImage = mainContactShareLayout.findViewById(R.id.contact_share_image);
            shareContactName = mainContactShareLayout.findViewById(R.id.contact_share_tv_name);
            shareContactNo = mainContactShareLayout.findViewById(R.id.contact_share_tv_no);
            shareEmailContact = mainContactShareLayout.findViewById(R.id.contact_share_emailId);
            addContactButton = mainContactShareLayout.findViewById(R.id.contact_share_add_btn);
            contactTimeTextView = mainContactShareLayout.findViewById(R.id.contactTime);

            previewImageView = attachmentPreviewLayout.findViewById(R.id.preview);
            videoIconImageView = attachmentPreviewLayout.findViewById(R.id.video_icon);
            progressBar = attachmentPreviewLayout.findViewById(R.id.progressBar);

            playImageView = attachmentAudioLayout.findViewById(R.id.playImageView);
            durationTextView = attachmentAudioLayout.findViewById(R.id.durationTextView);
            audioSeekBar = attachmentAudioLayout.findViewById(R.id.audioSeekbar);
            downloadLayout = attachmentAudioLayout.findViewById(R.id.downloadLayout);
            audioProgressBar = attachmentAudioLayout.findViewById(R.id.audioProgressBar);
            downloadImageView = attachmentAudioLayout.findViewById(R.id.downloadImageView);
            audioTimeTextView = attachmentAudioLayout.findViewById(R.id.audioTime);
            fileNameTextView = attachmentAudioLayout.findViewById(R.id.fileNameTextView);
            fileTimeTextView = attachmentAudioLayout.findViewById(R.id.fileTime);

        }
    }

    class MessageHolder2 extends RecyclerView.ViewHolder {
        TextView customMessageTextView;

        public MessageHolder2(View itemView) {
            super(itemView);
            customMessageTextView = itemView.findViewById(R.id.customMessage);
        }
    }

    class MessageHolder3 extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView dayTextView;

        public MessageHolder3(View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            dayTextView = itemView.findViewById(R.id.dayTextView);
        }
    }

    public void displayImage(Message message, ImageView contactImage, TextView alphabeticTextView) {
        if (alphabeticTextView != null) {
            alphabeticTextView.setVisibility(View.VISIBLE);
            String sender = message.getAuthor();
            char firstLetter = 0;
            firstLetter = sender.charAt(0);
            alphabeticTextView.setText(String.valueOf(firstLetter));

            Character colorKey = AlphaNumberColorUtil.alphabetBackgroundColorMap.containsKey(firstLetter) ? firstLetter : null;
            GradientDrawable bgShape = (GradientDrawable) alphabeticTextView.getBackground();
            bgShape.setColor(mContext.getResources().getColor(AlphaNumberColorUtil.alphabetBackgroundColorMap.get(colorKey)));
        }
    }

    private void setupContactShareView(final Message message, MessageHolder messageHolder) {
        messageHolder.mainContactShareLayout.setLayoutParams(new RelativeLayout.LayoutParams((int) (0.65 * screenWidth), ViewGroup.LayoutParams.WRAP_CONTENT));
        int resId;
        if (message.getMsgType() == Message.MESSAGE_TYPE_SEND) {
            resId = mContext.getResources().getColor(R.color.sent_message_text_color);
        } else {
            resId = mContext.getResources().getColor(R.color.message_text_color);
        }
        messageHolder.shareContactName.setTextColor(resId);
        messageHolder.shareContactNo.setTextColor(resId);
        messageHolder.shareEmailContact.setTextColor(resId);
        messageHolder.addContactButton.setTextColor(resId);
        STContactParser parser = new STContactParser();
        try {
            STContactData data = parser.parseCVFContactData(message.getContact());
            messageHolder.shareContactName.setText(data.getName());

            if (data.getAvatar() != null) {
                messageHolder.shareContactImage.setImageBitmap(data.getAvatar());
            }
            if (!TextUtils.isEmpty(data.getPhone())) {
                messageHolder.shareContactNo.setText(data.getPhone());
            } else {
                messageHolder.shareContactNo.setVisibility(View.GONE);
            }
            if (data.getEmail() != null) {
                messageHolder.shareEmailContact.setText(data.getEmail());
            } else {
                messageHolder.shareEmailContact.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        messageHolder.addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission((ConversationActivity) mContext)) {
                    new StringeePermissions((ConversationActivity) mContext, ConversationActivity.layout).requestStoragePermissions();
                } else {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "CONTACT_" + timeStamp + "_" + ".vcf";
                    File outputFile = FileUtils.getFilePath(imageFileName, mContext.getApplicationContext(), "text/x-vcard");
                    byte[] buf = message.getContact().trim().getBytes();
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsoluteFile());
                        fileOutputStream.write(buf);
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri outputUri = null;
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (Build.VERSION.SDK_INT >= 24) {
                        outputUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", outputFile);
                    } else {
                        outputUri = Uri.fromFile(outputFile);
                    }
                    if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                        intent.setDataAndType(outputUri, "text/x-vcard");
                        mContext.startActivity(intent);
                    }
                }
            }
        });

    }


    public void playAudio(Message message, MessageHolder messageHolder) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".provider", new File(message.getFilePath()));
        } else {
            uri = Uri.parse(message.getContact());
        }

        MediaPlayerManager.getInstance(mContext).play(uri, message, messageHolder.playImageView, messageHolder.audioSeekBar, messageHolder.durationTextView);
        int state = MediaPlayerManager.getInstance(mContext).getAudioState(message.getSId());
        messageHolder.playImageView.setVisibility(View.VISIBLE);
        messageHolder.durationTextView.setText("00:00");
        if (state == 1) {
            messageHolder.playImageView.setImageResource(R.drawable.ic_pause_circle_outline);
        } else {
            messageHolder.playImageView.setImageResource(R.drawable.ic_play_circle_outline);
        }

        updateApplozicSeekBar(message, messageHolder);
    }

    private void updateApplozicSeekBar(final Message message, final MessageHolder messageHolder) {
        MediaPlayer mediaplayer = MediaPlayerManager.getInstance(mContext).getMediaPlayer(message.getSId());
        if (mediaplayer == null) {
            messageHolder.audioSeekBar.setProgress(0);
        } else if (mediaplayer.isPlaying()) {
            messageHolder.audioSeekBar.setMax(mediaplayer.getDuration());
            messageHolder.audioSeekBar.setProgress(mediaplayer.getCurrentPosition());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateApplozicSeekBar(message, messageHolder);
                }
            };
            mHandler.postDelayed(runnable, 500);
        } else {
            messageHolder.audioSeekBar.setMax(mediaplayer.getDuration());
            messageHolder.audioSeekBar.setProgress(mediaplayer.getCurrentPosition());
        }
    }
}
