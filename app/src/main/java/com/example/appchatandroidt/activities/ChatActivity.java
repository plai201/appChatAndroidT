package com.example.appchatandroidt.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.LogTime;
import com.example.appchatandroidt.R;
import com.example.appchatandroidt.adapter.ChatAdapter;
import com.example.appchatandroidt.models.Conversions;
import com.example.appchatandroidt.models.Message;
import com.example.appchatandroidt.models.User;
import com.example.appchatandroidt.utilities.Constants;
import com.example.appchatandroidt.utilities.PreferenceManager;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.makeramen.roundedimageview.RoundedImageView;

import org.checkerframework.checker.units.qual.A;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends BaseActivity {
    private List<Message> chatMessages;
    private ChatAdapter chatAdapter;
    ProgressBar progressBar;
    RecyclerView recyclerView;
    EditText inputSms;
    AppCompatImageView btnSend;
    AppCompatImageView btnSendImage;
    RoundedImageView imageProFile;
    TextView textViewName,txtAvailability;
    String otherUserId;
    DatabaseReference mUserRef, smsRef,conversionRef;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    String otherUserName, otherUserProFileUrl, OtherUserStatus;
    String userName,userImageUrl;

    FirebaseRecyclerOptions<Message> options;
    FirebaseRecyclerAdapter<Message, ChatViewHolder> adapter;
    Uri uri;
    private final static int PICK_IMAGE =1;
    String URL = "https://fcm.googleapis.com/fcm/send";
    RequestQueue requestQueue;
    private List<Conversions> conversions;
    private String conversionId = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        progressBar = findViewById(R.id.progressBarChat);
        otherUserId = getIntent().getStringExtra("OtherUserId");



        requestQueue = Volley.newRequestQueue(this);
        recyclerView = findViewById(R.id.chatRecyclerView);
        inputSms = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.iconSendMess);
        btnSendImage = findViewById(R.id.iconSendImage);
        imageProFile = findViewById(R.id.imageProfileChat);
        textViewName = findViewById(R.id.txtNameChat);
        txtAvailability = findViewById(R.id.txtAvailability);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mUserRef = FirebaseDatabase.getInstance().getReference().child("users");
        smsRef = FirebaseDatabase.getInstance().getReference().child("message");
        conversionRef = FirebaseDatabase.getInstance().getReference().child("conversions");
        loadUsers();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessages();
            }
        });
        loadMyProfile();
        checkForConversion();


       loadMessages();


        btnSendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });

    }
    private void init(){
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                otherUserProFileUrl,
                otherUserId,
                this
        );
        recyclerView.setAdapter(chatAdapter);
    }

    private void loadMyProfile(){
        mUserRef.child(mUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    userName = snapshot.child("name").getValue().toString();
                    userImageUrl = snapshot.child("avatarUrl").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this,""+error.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            uri = data.getData();
            if (uri != null) {
                String url = uri.toString();
                Intent intent = new Intent(ChatActivity.this,SendImageActivity.class);
                intent.putExtra("u",url);
                intent.putExtra("n",otherUserName);
                intent.putExtra("ruid",otherUserId);
                intent.putExtra("suid",mUser.getUid());
                startActivity(intent);            } else {
                // Xử lý khi uri bị null
                Log.e("ChatActivity", "uri is null");
            }
        }else {
            Toast.makeText(this,"Không có file được chọn",Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        options = new FirebaseRecyclerOptions.Builder<Message>()
                .setQuery(smsRef.child(mUser.getUid()).child(otherUserId), Message.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Message, ChatViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ChatViewHolder chatViewHolder, int i, @NonNull Message message) {

                if (message.isSentByCurrentUser()) {
                    if (message.isImageMessage()) {
                        // Tin nhắn gửi là tin nhắn hình ảnh
                        chatViewHolder.bindSentMessage(message.getSms(), "iv", message.getDatetime());
                    } else {
                        // Tin nhắn gửi là tin nhắn văn bản
                        chatViewHolder.bindSentMessage(message.getSms(), "text",message.getDatetime());
                    }
                } else {
                    if (message.isImageMessage()) {
                        // Tin nhắn nhận là tin nhắn hình ảnh
                        chatViewHolder.bindReceivedMessage(message.getSms(), otherUserProFileUrl, "iv",message.getDatetime());
                    } else {
                        // Tin nhắn nhận là tin nhắn văn bản
                        chatViewHolder.bindReceivedMessage(message.getSms(), otherUserProFileUrl, "text",message.getDatetime());

                    }
                }
            }

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view;
                if (viewType == 0) { // gửi tin nhắn
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_sent_message, parent, false);
                } else { // nhận tin nhắn
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_received_message, parent, false);
                }
                return new ChatViewHolder(view);
            }

            @Override
            public int getItemViewType(int position) {
                // Xác định loại tin nhắn (gửi hoặc nhận) tại vị trí
                Message message = getItem(position);
                if (message.isSentByCurrentUser()) {
                    return 0; // Tin nhắn gửi
                } else {
                    return 1; // Tin nhắn nhận
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                int itemCount = adapter.getItemCount();
                if (itemCount > 0) {
                    recyclerView.scrollToPosition(itemCount - 1);
                }
            }
        };

        adapter.startListening();
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void sendMessages() {
        String sms = inputSms.getText().toString();
        if (sms.isEmpty()) {
            Toast.makeText(this, "Nhập tin nhắn", Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("sms", sms);
            hashMap.put("status", "unseen");
            hashMap.put("userId", mUser.getUid());
            hashMap.put("senderId", mUser.getUid());
            hashMap.put("receiverId", otherUserId);
            hashMap.put("datetime", getReadableDateTime(new Date()));
            smsRef.child(otherUserId).child(mUser.getUid()).push().updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        smsRef.child(mUser.getUid()).child(otherUserId).push().updateChildren(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    checkForConversion();
                                    Log.d("CheckForConversion",""+conversionId);
                                    if (conversionId != null){
                                        updateConversion(sms);
                                    }else {
                                        HashMap<String, Object> conversationMap = new HashMap<>();
                                        conversationMap.put("senderId", mUser.getUid());
                                        conversationMap.put("receiverId", otherUserId);
                                        conversationMap.put("lastMessage", sms);
                                        conversationMap.put("senderName", userName);
                                        conversationMap.put("receiverName", otherUserName);
                                        conversationMap.put("receiverImage", otherUserProFileUrl);
                                        conversationMap.put("senderImage", userImageUrl);
                                        conversationMap.put("timestamp", new Date());
                                        addConversion(conversationMap);
                                    }

                                    sendNotification(sms);
                                    inputSms.setText(null);
                                    Toast.makeText(ChatActivity.this, "Đã gửi tin nhắn", Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
                    }
                }
            });
        }
    }
    private void sendNotification(String sms){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to","/topics/"+otherUserId);
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("title","Tin nhắn mới từ "+ userName);
            jsonObject1.put("body",sms);

            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("userId",mUser.getUid());
            jsonObject2.put("type","sms");


            jsonObject.put("notification",jsonObject1);
            jsonObject.put("data",jsonObject2);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,URL, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {

                }
            }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> map = new HashMap<>();
                    map.put("content-type","application/json");
                    map.put("authorization","key=AAAAZKy0RfY:APA91bE3c5YkP5LdtApoWBFAhRgZ2XhB_yd_qHHyIVcjWPSebqJ0-u3H0f3ps0zu2elVw51Sa4rr8aL1GXH5Vlshy0q11OtegmDx8-ZqBy2TGmD1crAGpGhtYLHVLnTJPAjmKFSdrNhi");
                    return map;
                }
            };
            requestQueue.add(request);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    private void loadUsers() {
        mUserRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                        otherUserName = snapshot.child("name").getValue(String.class);
                        otherUserProFileUrl = snapshot.child("avatarUrl").getValue(String.class);
                        OtherUserStatus = snapshot.child("status").getValue(String.class);
                        long lastActive = (long) snapshot.child("lastActive").getValue();

                        textViewName.setText(otherUserName);
                        if (Long.valueOf(lastActive) !=null){
                            if (OtherUserStatus.equalsIgnoreCase("offline")){
                                txtAvailability.setBackgroundResource(R.color.warning);
                                txtAvailability.setVisibility(View.VISIBLE);
                                String timeAgo = calculateTimeSinceLastActive(lastActive);
                                txtAvailability.setText(timeAgo);
                            }else {
                                txtAvailability.setBackgroundResource(R.color.accent);
                                txtAvailability.setText("online");
                                txtAvailability.setVisibility(View.VISIBLE);

                            }
                        }


                    // Hiển thị ảnh đại diện của người dùng
                        if (otherUserProFileUrl != null && !otherUserProFileUrl.isEmpty()) {
                            Glide.with(ChatActivity.this)
                                    .load(otherUserProFileUrl)
                                    .into(imageProFile);
                        } else {
                            // Nếu không có ảnh đại diện, sử dụng ảnh mặc định
                            Glide.with(ChatActivity.this)
                                    .load(R.drawable.default_avatar)
                                    .into(imageProFile);
                        }


                } else {
                    // Xử lý khi DataSnapshot không tồn tại
                    Log.e("ChatActivity", "Không có dữ liệu cho otherUserId: " + otherUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Đã xảy ra lỗi: " + error.getMessage());
                Toast.makeText(ChatActivity.this, "Đã xảy ra lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd,yyyy - hh:mm a", Locale.getDefault()).format(date);
    }
    private void listenMessages() {
        Query senderChatRef = smsRef
                .child(otherUserId)
                .child(mUser.getUid())
                .orderByChild("senderId")
                .equalTo(otherUserId);
        Query receiverChatRef = smsRef
                .child(mUser.getUid())
                .child(otherUserId)
                .orderByChild("receiverId")
                .equalTo(otherUserId);
        senderChatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                int count = chatMessages.size();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message chatmessage = dataSnapshot.getValue(Message.class);
                    chatMessages.add(chatmessage);
                }
//                Collections.sort(conversions, (o1, o2) -> o1.dataObject.compareTo(o2.dataObject));

                if (count == 0) {
                    // Nếu đây là tin nhắn đầu tiên, cần thông báo toàn bộ dữ liệu đã thay đổi
                    chatAdapter.notifyDataSetChanged();
                } else {
                    // Nếu không phải tin nhắn đầu tiên, thông báo vị trí bắt đầu và kết thúc tin nhắn mới được thêm vào
                    chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());

                    // Di chuyển RecyclerView đến vị trí tin nhắn mới nhất
                    recyclerView.smoothScrollToPosition(conversions.size() - 1);
                }
                recyclerView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                if (conversionId == null) {
                    checkForConversion();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        receiverChatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                int count = chatMessages.size();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message chatmessage = new Message();
                    chatmessage.setUserId(dataSnapshot.child("senderId").getValue(String.class));
                    chatmessage.setSms(dataSnapshot.child("sms").getValue(String.class));
                    chatMessages.add(chatmessage);

                }


//                Collections.sort(conversions, (o1, o2) -> o1.dataObject.compareTo(o2.dataObject));

                if (count == 0) {
                    // Nếu đây là tin nhắn đầu tiên, cần thông báo toàn bộ dữ liệu đã thay đổi
                    chatAdapter.notifyDataSetChanged();
                } else {
                    // Nếu không phải tin nhắn đầu tiên, thông báo vị trí bắt đầu và kết thúc tin nhắn mới được thêm vào
                    chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());

                    // Di chuyển RecyclerView đến vị trí tin nhắn mới nhất
                    recyclerView.smoothScrollToPosition(conversions.size() - 1);
                }
                // Hiển thị RecyclerView nếu đã ẩn
                recyclerView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                if (conversionId == null) {
                    //checkForConversion();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void addConversion(HashMap<String, Object> conversion) {
        DatabaseReference conversionsRef = FirebaseDatabase.getInstance().getReference("conversions");

        String newConversionKey = conversionsRef.push().getKey(); // Tạo một key mới cho conversion

        conversionsRef.child(newConversionKey).setValue(conversion)
                .addOnSuccessListener(aVoid -> {
                    // Lưu conversionId để sử dụng sau này
                    conversionId = newConversionKey;
                })
                .addOnFailureListener(e -> {
                    // Xử lý khi thêm conversion thất bại
                });
    }
    private void updateConversion(String message) {
        if (conversionId != null && !conversionId.isEmpty()) {
            DatabaseReference conversionRef = FirebaseDatabase.getInstance().getReference("conversions").child(conversionId);

            Map<String, Object> updateData = new HashMap<>();
            updateData.put(Constants.KEY_LAST_MESSAGE, message);
            updateData.put(Constants.KEY_TIMESTAMP, new Date());

            conversionRef.updateChildren(updateData)
                    .addOnSuccessListener(aVoid -> {
                        // Cập nhật conversion thành công
                    })
                    .addOnFailureListener(e -> {
                        // Xử lý khi cập nhật conversion thất bại
                    });
        } else {
            // Xử lý khi conversionId không hợp lệ
        }
    }




    private void checkForConversion() {
        checkForConversionRemotely(mUser.getUid(),otherUserId);
        checkForConversionRemotely(otherUserId,mUser.getUid());
     }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        Query query = conversionRef.orderByChild("senderId").equalTo(senderId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("receiverId").getValue().toString().equals(receiverId)) {
                        conversionId = snapshot.getKey();
                    }
                }
                // Xử lý trường hợp không tìm thấy dữ liệu phù hợp
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Xử lý khi có lỗi xảy ra
            }
        });
    }





}
