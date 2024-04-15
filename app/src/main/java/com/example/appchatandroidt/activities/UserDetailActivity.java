package com.example.appchatandroidt.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserDetailActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

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
    private final static int PICK_IMAGE = 1;
    private Uri imageUri;
    private Bitmap selectedBitmap;
    private String avatarUrl;
    private String[] roles = {"Người dùng", "Admin"};

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



        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mRef = FirebaseDatabase.getInstance().getReference().child("friends");

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mUserRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        getUserDetails();
        loadRole();
        loadFriends("");
        imageUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUser2();
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });
    }
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa người dùng này?");

        // Nút Xác nhận
        builder.setPositiveButton("Xác nhận", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Gọi hàm xóa người dùng khi người dùng xác nhận
                deleteUser();
            }
        });

        // Nút Hủy
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Không làm gì khi người dùng chọn hủy
                dialog.dismiss();
            }
        });

        // Hiển thị AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteUser() {

        mUserRef.removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Người dùng đã được xóa", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi xóa người dùng. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadRole(){
        spinner.setOnItemSelectedListener(this);

        ArrayAdapter ad
                = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                roles);

        // set simple layout resource file
        // for each item of spinner
        ad.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item);

        // Set the ArrayAdapter (ad) data on the
        // Spinner which binds data to spinner
        spinner.setAdapter(ad);
    }

    private int getIndexForRole(int userRole) {
        // Vai trò 0 và 1 tương ứng với vị trí trong mảng roles
        if (userRole == 0) {
            return 0; // Người dùng
        } else if (userRole == 1) {
            return 1; // Admin
        }
        return -1;
    }



    private void updateUser(String userId) {


        // Lấy dữ liệu hiện tại từ Firebase
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String currentName = dataSnapshot.child("name").getValue(String.class);
                    String currentEmail = dataSnapshot.child("email").getValue(String.class);
                    String currentPhone = dataSnapshot.child("sdt").getValue(String.class);
                    String currentAvatarUrl = dataSnapshot.child("avatarUrl").getValue(String.class);

                    // So sánh và cập nhật Tên (Name)
                    String newName = textName.getText().toString();
                    if (!newName.equals(currentName)) {
                        mUserRef.child("name").setValue(newName)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "Tên người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật tên. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }

                    // So sánh và cập nhật Email
                    String newEmail = textEmail.getText().toString();
                    if (!newEmail.equals(currentEmail)) {
                        mUserRef.child("email").setValue(newEmail)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "Email người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật email. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }

                    // So sánh và cập nhật Số điện thoại (Phone)
                    String newPhone = textPhone.getText().toString();
                    if (!newPhone.equals(currentPhone)) {
                        mUserRef.child("sdt").setValue(newPhone)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "Số điện thoại người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật số điện thoại. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }

                    // So sánh và cập nhật Avatar URL
                    if (avatarUrl != null && !avatarUrl.equals(currentAvatarUrl)) {
                        mUserRef.child("avatarUrl").setValue(avatarUrl)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "URL avatar người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật URL avatar. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Xử lý khi có lỗi xảy ra
                Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi lấy dữ liệu từ Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateUser2() {
        if(!txtPass.getText().toString().isEmpty()){
            String newPassword = txtPass.getText().toString();

            mUser.updatePassword(newPassword)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Mật khẩu đã được set lại thành công
                                Toast.makeText(getApplicationContext(), "Mật khẩu đã được set lại thành công", Toast.LENGTH_SHORT).show();
                            } else {
                                // Đã xảy ra lỗi khi set lại mật khẩu
                                Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi set lại mật khẩu. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                // In ra lỗi chi tiết
                                Exception exception = task.getException();
                                if (exception != null) {
                                    Log.e("FirebaseAuth", "Error updating password: " + exception.getMessage());
                                }
                            }
                        }
                    });

        }
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        if (selectedBitmap != null) {
            uploadImageToFirebaseStorage(storageRef, userId);
        }
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String sdt = snapshot.child("sdt").getValue(String.class);
                    if (!name.equals(textName.getText().toString())){
                        mUserRef.child("name").setValue(textName.getText().toString())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "Tên người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật tên. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                    if (!email.equals(textEmail.getText().toString())){
                        mUserRef.child("email").setValue(textEmail.getText().toString())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "Email người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật tên. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                    if (!sdt.equals(textPhone.getText().toString())){
                        mUserRef.child("sdt").setValue(textPhone.getText().toString())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "Số điện thoại người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật tên. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


//        mUserRef.child("sdt").setValue(textPhone.getText().toString())
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        if (task.isSuccessful()) {
//                            Toast.makeText(getApplicationContext(), "Số điện thoại người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật số điện thoại. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//        mUserRef.child("avatarUrl").setValue(avatarUrl)
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        if (task.isSuccessful()) {
//                            Toast.makeText(getApplicationContext(), "URL avatar người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật URL avatar. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//        mUserRef.child("email").setValue(textEmail.getText().toString())
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        if (task.isSuccessful()) {
//                            Toast.makeText(getApplicationContext(), "Email người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
//                        } else {
//                            Toast.makeText(getApplicationContext(), "Có lỗi xảy ra khi cập nhật email. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
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
                        Intent intent = new Intent(UserDetailActivity.this, UserDetailActivity.class);
                        intent.putExtra("userId",getRef(position).getKey().toString());
                        startActivity(intent);
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
                        spinner.setSelection(getIndexForRole(user.getRole()));

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
    private void uploadImageToFirebaseStorage(StorageReference storageRef, String userId) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();


        StorageReference imageRef = storageRef.child("avatars/" + userId + ".jpg");
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnFailureListener(e -> {
            Log.e("UPLOAD_IMAGE_ERROR", "Error uploading image: " + e.getMessage());
            Toast.makeText(UserDetailActivity.this, "Lỗi khi tải ảnh lên. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
        }).addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                avatarUrl = uri.toString();
                mUserRef.child("avatarUrl").setValue(avatarUrl)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // Cập nhật thành công
                                    Toast.makeText(getApplicationContext(), "Ảnh người dùng đã được cập nhật", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Xử lý khi cập nhật không thành công
                                    Toast.makeText(getApplicationContext(), "Có lỗi xảy ra. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            });
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imageUser.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mUserRef.child("role").setValue(position).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Cập nhật thành công
                    Toast.makeText(getApplicationContext(), "Role đã được cập nhật", Toast.LENGTH_SHORT).show();
                } else {
                    // Xử lý khi cập nhật không thành công
                    Toast.makeText(getApplicationContext(), "Có lỗi xảy ra. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}