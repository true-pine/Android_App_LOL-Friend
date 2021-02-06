package com.capston.lolfriend.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.capston.lolfriend.R;
import com.capston.lolfriend.etc.CustomProgressDialog;
import com.capston.lolfriend.model.ChatModel;
import com.capston.lolfriend.model.NotificationModel;
import com.capston.lolfriend.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {

    private String chatRoomId;
    private String chatRoomType;
    private String uid;
    private String nickname;
    private String profileUrl;
    private String chatRoomName;

    private Button button;
    private EditText editText;
    private RecyclerView recyclerView;
    private List<ChatModel.Comment> comments;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private Map<String, UserModel> userMap = new HashMap<>();
    private CustomProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        chatRoomId = getIntent().getStringExtra("roomId");
        chatRoomType = getIntent().getStringExtra("roomType");
        chatRoomName = getIntent().getStringExtra("roomName");

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Toolbar tb = findViewById(R.id.messageActivity_toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_left_black_24dp);

        TextView tv = findViewById(R.id.messageActivity_toolbar_title);
        tv.setText(chatRoomName);


        //닉네임 & 프로필url 대입
        FirebaseDatabase.getInstance().getReference()
                .child("users").child(uid).orderByKey()
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot key : dataSnapshot.getChildren()) {
                            switch (key.getKey()) {
                                case "nickname":
                                    nickname = key.getValue(String.class);
                                case "profileIconURL":
                                    profileUrl = key.getValue(String.class);
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        //채팅방의 유저 정보 대입
        FirebaseDatabase.getInstance().getReference()
                .child("chatrooms").child(chatRoomId).child("users")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        userMap.clear();
                        if (chatRoomType.equals("singular") && dataSnapshot.getChildrenCount() <= 1L) {
                            button.setEnabled(false);
                            editText.setEnabled(false);
                            return;
                        }
                        Map<String, Boolean> map = (Map<String, Boolean>) dataSnapshot.getValue();
                        if(map != null) {
                            for (String userId : map.keySet()) {
                                FirebaseDatabase.getInstance().getReference().child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                        userMap.put(userModel.uid, userModel);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        button = findViewById(R.id.messageActivity_button);
        editText = findViewById(R.id.messageActivity_edittext);
        recyclerView = findViewById(R.id.messageActivity_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
        recyclerView.setAdapter(new MessageActivityAdapter());

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.getText().length() > 0) {
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = editText.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP;
                    comment.nickname = nickname;
                    comment.profileUrl = profileUrl;
                    FirebaseDatabase.getInstance().getReference()
                            .child("chatrooms").child(chatRoomId).child("comments").push().setValue(comment)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    for (String user : userMap.keySet()) {
                                        if (user.equals(uid)) {
                                            continue;
                                        }
                                        String token = userMap.get(user).pushToken;
                                        sendFcm(token);
                                    }
                                    editText.setText("");

                                }
                            });
                }
            }
        });

        mDialog = new CustomProgressDialog(MessageActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRoomType.equals("singular") && comments.size() == 0) {
            FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomId).setValue(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.messageactivity_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishAndRemoveTask();
                break;
            case R.id.messageActivity_toolbar_action_exit:
                AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                builder.setTitle("확인").setMessage("나가신 채팅방은 복구할 수 없습니다.\n채팅방을 나가시겠습니까?")
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDialog.show();

                                FirebaseDatabase.getInstance().getReference().child("chatrooms/" + chatRoomId + "/users/" + uid).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        FirebaseDatabase.getInstance().getReference().child("chatrooms/" + chatRoomId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(!dataSnapshot.child("users").exists()) {
                                                    FirebaseDatabase.getInstance().getReference().child("chatrooms/" + chatRoomId).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            mDialog.dismiss();
                                                            finish();
                                                        }
                                                    });
                                                } else {
                                                    mDialog.dismiss();
                                                    finishAndRemoveTask();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                });
                            }
                        })
                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create().show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void sendFcm(String token) {
        Gson gson = new Gson();

        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = token;
        //notificationModel.notification.title = chatRoomName;
        //notificationModel.notification.text = nickname + " : " + editText.getText().toString();
        notificationModel.data.title = chatRoomName;
        notificationModel.data.text = nickname + " : " + editText.getText().toString();
        notificationModel.data.chatRoomType = chatRoomType;
        notificationModel.data.chatRoomId = chatRoomId;
        notificationModel.data.chatRoomName = chatRoomName;

        RequestBody requestBody = RequestBody.create(gson.toJson(notificationModel), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .addHeader("Authorization", "key=AAAAvapBaA0:APA91bEO_-Ia_3jeXH9gX-O1pyFGezf9zgQbuQx2D3KtNhmHPrdpKtWu4GTi50OS_Y4R8herZvaRfxvHrw77o6Ljg3_RmNUnjZZ6HNg2w_sIa01ZT-5zg5_073kO9HJTiQXAiRgdhg6v")
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

            }
        });
    }

    class MessageActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public MessageActivityAdapter() {
            comments = new ArrayList<>();

            FirebaseDatabase.getInstance().getReference()
                    .child("chatrooms").child(chatRoomId).child("comments")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            comments.clear();
                            for (DataSnapshot comment : dataSnapshot.getChildren()) {
                                comments.add(comment.getValue(ChatModel.Comment.class));
                            }

                            //notifyDataSetChanged();

                            recyclerView.scrollToPosition(comments.size() - 1);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new MessageActivityViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MessageActivityViewHolder viewHolder = ((MessageActivityViewHolder) holder);

            //내가 보낸 메세지
            if (comments.get(position).uid.equals(uid)) {
                viewHolder.imageView_profile.setVisibility(View.GONE);
                viewHolder.textView_name.setVisibility(View.GONE);

                viewHolder.textView_message.setText(comments.get(position).message);
                viewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);

                long unixTime = (long) comments.get(position).timestamp;
                Date date = new Date(unixTime);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                String time = simpleDateFormat.format(date);
                viewHolder.textView_time.setText(time);

                ConstraintLayout.LayoutParams layoutParams_message = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                layoutParams_message.topToTop = R.id.messageItem_constraintLayout_message;
                layoutParams_message.endToEnd = R.id.messageItem_constraintLayout_message;
                viewHolder.textView_message.setLayoutParams(layoutParams_message);

                ConstraintLayout.LayoutParams layoutParams_time = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                layoutParams_time.endToStart = R.id.messageItem_textView_message;
                layoutParams_time.bottomToBottom = R.id.messageItem_textView_message;
                viewHolder.textView_time.setLayoutParams(layoutParams_time);
            } else {
                //상대방이 보낸 메세지
                viewHolder.imageView_profile.setVisibility(View.VISIBLE);
                viewHolder.textView_name.setVisibility(View.VISIBLE);

                Glide.with(holder.itemView.getContext())
                        .load(comments.get(position).profileUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(viewHolder.imageView_profile);
                viewHolder.textView_name.setText(comments.get(position).nickname);
                viewHolder.textView_message.setText(comments.get(position).message);
                viewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);

                long unixTime = (long) comments.get(position).timestamp;
                Date date = new Date(unixTime);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                String time = simpleDateFormat.format(date);
                viewHolder.textView_time.setText(time);

                ConstraintLayout.LayoutParams layoutParams_message = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                layoutParams_message.topToTop = R.id.messageItem_constraintLayout_message;
                layoutParams_message.startToStart = R.id.messageItem_constraintLayout_message;
                viewHolder.textView_message.setLayoutParams(layoutParams_message);

                ConstraintLayout.LayoutParams layoutParams_time = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                layoutParams_time.startToEnd = R.id.messageItem_textView_message;
                layoutParams_time.bottomToBottom = R.id.messageItem_textView_message;
                viewHolder.textView_time.setLayoutParams(layoutParams_time);
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class MessageActivityViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView_profile;
            public TextView textView_name;
            public ConstraintLayout constraintLayout_message;
            public TextView textView_message;
            public TextView textView_time;

            public MessageActivityViewHolder(View view) {
                super(view);
                imageView_profile = view.findViewById(R.id.messageItem_imageView_profile);
                textView_name = view.findViewById(R.id.messageItem_textView_name);
                constraintLayout_message = view.findViewById(R.id.messageItem_constraintLayout_message);
                textView_message = view.findViewById(R.id.messageItem_textView_message);
                textView_time = view.findViewById(R.id.messageItem_textView_time);
            }
        }
    }
}
