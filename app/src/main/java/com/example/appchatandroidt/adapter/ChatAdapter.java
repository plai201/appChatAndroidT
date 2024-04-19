package com.example.appchatandroidt.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appchatandroidt.R;
import com.example.appchatandroidt.models.Message;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Message> chatMessages;
    private String receiverProfileImage;
    private final String senderId;
    public static final int VIEW_TYPE_SEND = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    private static Context context;

    public ChatAdapter(List<Message> chatMessages, String receiverProfileImage, String senderId,Context context) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SEND){
            View view = LayoutInflater.from(context).inflate(R.layout.item_container_sent_message, parent, false);
            return new SentMessageViewHolder(view);

        }else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_container_received_message, parent, false);
            return new ReceiverMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position) == VIEW_TYPE_SEND){
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        }else {
            ((ReceiverMessageViewHolder)holder).setData(chatMessages.get(position),receiverProfileImage);
        }

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).isSentByCurrentUser()){
            return VIEW_TYPE_SEND;
        }else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder{
        private TextView textMessageSent,textViewDatetimeT, textViewDatetimeR;
        private ImageView imageSend;



        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessageSent = itemView.findViewById(R.id.txtMessageSent);
            imageSend = itemView.findViewById(R.id.imageSending);
            textViewDatetimeT = itemView.findViewById(R.id.txtDataTimeS);
            textViewDatetimeR = itemView.findViewById(R.id.txtDataTimeImageS);
        }
        public void setData(Message message) {
            if (!message.isImageMessage()){
                textMessageSent.setVisibility(View.VISIBLE);
                textViewDatetimeT.setVisibility(View.VISIBLE);
                textViewDatetimeT.setText(message.getDatetime());
                textMessageSent.setText(message.getSms());
            } else{
                imageSend.setVisibility(View.VISIBLE);
                textMessageSent.setVisibility(View.GONE);
                textViewDatetimeT.setVisibility(View.GONE);
                textViewDatetimeR.setVisibility(View.VISIBLE);
                textViewDatetimeR.setText(message.getSms());
                Glide.with(itemView.getContext())
                        .load(message.getSms())
                        .into(imageSend);
            }
        }
//        void setData(Message chatMessage){
//            if (type.equals("text")){
//                textMessageSent.setVisibility(View.VISIBLE);
//                textViewDatetimeT.setVisibility(View.VISIBLE);
//                textViewDatetimeT.setText(datetime);
//                textMessageSent.setText(message);
//            } else if (type.equals("iv")) {
//                imageSend.setVisibility(View.VISIBLE);
//                textMessageSent.setVisibility(View.GONE);
//                textViewDatetimeT.setVisibility(View.GONE);
//                textViewDatetimeR.setVisibility(View.VISIBLE);
//                textViewDatetimeR.setText(datetime);
//                Glide.with(itemView.getContext())
//                        .load(message)
//                        .into(imageSend);
//            }
//        }



    }
    static class ReceiverMessageViewHolder extends  RecyclerView.ViewHolder{
        private TextView textMessageReceived, textViewDatetimeRT, getTextViewDatetimeRR;;
        private RoundedImageView imageProfileReceivedUrl;
        private ImageView imageReceive;
        public ReceiverMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDatetimeRT = itemView.findViewById(R.id.txtDataTimeR);
            getTextViewDatetimeRR = itemView.findViewById(R.id.txtDataTimeImageR);
            textMessageReceived = itemView.findViewById(R.id.txtMessageReceived);
            imageProfileReceivedUrl = itemView.findViewById(R.id.imageProfileReceived);
            imageReceive = itemView.findViewById(R.id.imageReceive);
        }
//        void setData(Message chatMessage){
//            if (type.equals("text")){
//                textMessageSent.setVisibility(View.VISIBLE);
//                textViewDatetimeT.setVisibility(View.VISIBLE);
//                textViewDatetimeT.setText(datetime);
//                textMessageSent.setText(message);
//            } else if (type.equals("iv")) {
//                imageSend.setVisibility(View.VISIBLE);
//                textMessageSent.setVisibility(View.GONE);
//                textViewDatetimeT.setVisibility(View.GONE);
//                textViewDatetimeR.setVisibility(View.VISIBLE);
//                textViewDatetimeR.setText(datetime);
//                Glide.with(itemView.getContext())
//                        .load(message)
//                        .into(imageSend);
//            }
//        }
        public void setData(Message message, String imageUrl) {
            // Kiểm tra và hiển thị ảnh đại diện
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .into(imageProfileReceivedUrl);

            } else {
                // Nếu không có ảnh đại diện, sử dụng ảnh mặc định
                imageProfileReceivedUrl.setImageResource(R.drawable.default_avatar);
            }
            if (!message.isImageMessage()){
                textMessageReceived.setVisibility(View.VISIBLE);
                textViewDatetimeRT.setVisibility(View.VISIBLE);
                textViewDatetimeRT.setText(message.getDatetime());
                textMessageReceived.setText(message.getSms());
            } else {
                imageReceive.setVisibility(View.VISIBLE);
                textMessageReceived.setVisibility(View.GONE);
                textViewDatetimeRT.setVisibility(View.GONE);
                getTextViewDatetimeRR.setVisibility(View.VISIBLE);
                getTextViewDatetimeRR.setText(message.getSms());
                Glide.with(itemView.getContext())
                        .load(message)
                        .into(imageReceive);
            }
        }
    }

}
