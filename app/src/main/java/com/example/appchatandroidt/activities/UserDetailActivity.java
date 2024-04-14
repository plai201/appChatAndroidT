package com.example.appchatandroidt.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.appchatandroidt.R;
import com.example.appchatandroidt.models.Friends;
import com.example.appchatandroidt.models.User;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class UserDetailActivity extends AppCompatActivity {

    private String userId;
    DatabaseReference mUserRef,mRef;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseRecyclerOptions<Friends>options;
    FirebaseRecyclerAdapter<Friends, FriendViewHolder>adapter;
    private RecyclerView recyclerView;

    private EditText textName, textEmail, textPhone,txtPass;
    private Spinner spinner;
    private ImageView imageUser;
    private ImageButton btnEdit, btnDelete;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        userId = getIntent().getStringExtra("userIdD");

        textName = findViewById(R.id.textName2);
        textEmail = findViewById(R.id.textEmail2);
        textPhone = findViewById(R.id.textPhone2);
        spinner = findViewById(R.id.textRole2);
        txtPass = findViewById(R.id.txtPassword2);
        imageUser = findViewById(R.id.imageUser);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        recyclerView = findViewById(R.id.friend_recycler_view_detail);

        String[] roles = {"Admin", "Người dùng"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = roles[position];
                Toast.makeText(getApplicationContext(), "Selected Role: " + selectedRole, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do something when nothing is selected
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mRef = FirebaseDatabase.getInstance().getReference().child("friends");

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mUserRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        getUserDetails();
        loadFriends("");

    }
    private void loadFriends(String s){
        Query query = mRef.child(userId).orderByChild("name").startAt(s).endAt(s+"\uf8ff");
        options = new FirebaseRecyclerOptions.Builder<Friends>().setQuery(query, Friends.class).build();

        adapter= new FirebaseRecyclerAdapter<Friends, FriendViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FriendViewHolder friendViewHolder, int i, @NonNull Friends friends) {
                if(friends.getAvatarUrl()!= null && !friends.getAvatarUrl().isEmpty()){
                    Glide.with(friendViewHolder.itemView.getContext())
                            .load(friends.getAvatarUrl())
                            .apply(RequestOptions.circleCropTransform())
                            .into(friendViewHolder.proFileImage);
                } else {
                    // Nếu không có ảnh đại diện, sử dụng ảnh mặc định
                    Glide.with(friendViewHolder.itemView.getContext())
                            .load(R.drawable.default_avatar)
                            .apply(RequestOptions.circleCropTransform())
                            .into(friendViewHolder.proFileImage);
                }
                friendViewHolder.name.setText(friends.getName());
                friendViewHolder.sdt.setText(friends.getSdt());

                final int position = i;
                friendViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
//                        Intent intent = new Intent(UserDetailActivity.this, UserDetailActivity.class);
//                        intent.putExtra("userId",getRef(position).getKey().toString());
//                        startActivity(intent);
                    }
                });

            }

            @NonNull
            @Override
            public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_view_friend,parent,false);
                return new  FriendViewHolder(view);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);
     }
     private void getUserDetails() {
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        // Hiển thị thông tin người dùng
                        textName.setText(user.getName());
                        textEmail.setText(user.getEmail());
                        textPhone.setText(user.getSdt());
//                        if ( user.getRole() == 0){
//                            textRole.setText("Người dùng");
//                        }else {
//                            textRole.setText("Admin");
//
//                        }

                        // Hiển thị hình ảnh người dùng (sử dụng Picasso hoặc Glide)
                        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            Glide.with(getApplicationContext())
                                    .load(user.getAvatarUrl())
                                    .into(imageUser);                        } else {
                            // Nếu không có hình ảnh, sử dụng hình ảnh mặc định
                            imageUser.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        Toast.makeText(UserDetailActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(UserDetailActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserDetailActivity.this, "Đã xảy ra lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}