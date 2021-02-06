package com.capston.lolfriend.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.capston.lolfriend.R;
import com.capston.lolfriend.activity.MainActivity;
import com.capston.lolfriend.activity.MessageActivity;
import com.capston.lolfriend.model.ChatModel;
import com.capston.lolfriend.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private List<ChatModel> chats;
    private String uid;
    private Map<String, Integer> tiers;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);

    private RecyclerView recyclerView;

    private enum MODE {
        SELLECT, DISPLAY
    }

    private MODE viewMode;

    private Map<String, Boolean> checkedChat;

    public ChatFragment() {
        chats = new ArrayList<>();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tiers = new HashMap<>();
        tiers.put("IRON", R.drawable.tier_iron);
        tiers.put("BRONZE", R.drawable.tier_bronze);
        tiers.put("SILVER", R.drawable.tier_silver);
        tiers.put("GOLD", R.drawable.tier_gold);
        tiers.put("PLATINUM", R.drawable.tier_platinum);
        tiers.put("DIAMOND", R.drawable.tier_diamond);
        tiers.put("MASTER", R.drawable.tier_master);
        tiers.put("GRANDMASTER", R.drawable.tier_grandmaster);
        tiers.put("CHALLENGER", R.drawable.tier_challenger);

        viewMode = MODE.DISPLAY;

        checkedChat = new HashMap<>();

        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chats.clear();
                for (DataSnapshot path1 : dataSnapshot.getChildren()) {
                    ChatModel chatModel = path1.getValue(ChatModel.class);
                    if (chatModel.users.containsKey(uid)) {
                        if(chatModel.info.roomType.equals("singular") && chatModel.comments.size() == 0) {
                            continue;
                        }
                        chats.add(chatModel);
                    }
                }
                //채팅방 리스트 정렬
                Collections.sort(chats, new Comparator<ChatModel>() {
                    @Override
                    public int compare(ChatModel o1, ChatModel o2) {
                        Map<String, ChatModel.Comment> comments_o1 = new TreeMap<>(Collections.reverseOrder());
                        Map<String, ChatModel.Comment> comments_o2 = new TreeMap<>(Collections.reverseOrder());
                        comments_o1.putAll(o1.comments);
                        comments_o2.putAll(o2.comments);

                        Long o1_time = 0L;
                        Long o2_time = 0L;

                        String lastMessage_o1;
                        String lastMessage_o2;

                        if (comments_o1.size() == 0 && comments_o2.size() == 0) {
                            return o1_time.compareTo(o2_time);
                        } else if (comments_o1.size() == 0) {
                            lastMessage_o2 = (String) comments_o2.keySet().toArray()[0];
                            o2_time = (long) o2.comments.get(lastMessage_o2).timestamp;

                            return o1_time.compareTo(o2_time);
                        } else if (comments_o2.size() == 0) {
                            lastMessage_o1 = (String) comments_o1.keySet().toArray()[0];
                            o1_time = (long) o1.comments.get(lastMessage_o1).timestamp;

                            return o1_time.compareTo(o2_time);
                        } else {
                            lastMessage_o1 = (String) comments_o1.keySet().toArray()[0];
                            lastMessage_o2 = (String) comments_o2.keySet().toArray()[0];

                            o1_time = (long) o1.comments.get(lastMessage_o1).timestamp;
                            o2_time = (long) o2.comments.get(lastMessage_o2).timestamp;

                            return o1_time.compareTo(o2_time);
                        }
                    }
                });
                Collections.reverse(chats);

                if (recyclerView != null) {
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.chatFragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new ChatFragmentAdapter());

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.setActionBarTitle("채팅");
        }

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.chatfragment_toolbar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.chatFragment_toolbar_action_delete) {
            if (viewMode == MODE.DISPLAY) {
                //아이콘 변경
                item.setIcon(R.drawable.ic_check_black_24dp);
                //프래그먼트의 모드를 선택모드로 바꾼다.
                viewMode = MODE.SELLECT;
                //체크된 채팅방의 방id를 담는 해시맵을 비운다.
                checkedChat.clear();
                recyclerView.getAdapter().notifyDataSetChanged();
                Toast.makeText(getActivity(), "편집모드", Toast.LENGTH_SHORT).show();
            } else {
                //아이콘 변경
                item.setIcon(R.drawable.ic_delete_forever_black_24dp);
                viewMode = MODE.DISPLAY;

                if(checkedChat.size() > 0) {
                    for (String key : checkedChat.keySet()) {
                        if(checkedChat.get(key)) {
                            final String newkey = key;
                            FirebaseDatabase.getInstance().getReference().child("chatrooms/" + key + "/users/" + uid).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    FirebaseDatabase.getInstance().getReference().child("chatrooms/" + newkey).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(!dataSnapshot.child("users").exists()) {
                                                FirebaseDatabase.getInstance().getReference().child("chatrooms/" + newkey).setValue(null);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            });
                        }
                    }
                }
                recyclerView.getAdapter().notifyDataSetChanged();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private class ChatFragmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public ChatFragmentAdapter() {

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);

            return new ChatFragmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
            final ChatModel curChat = chats.get(position);
            final ChatFragmentViewHolder viewHolder = (ChatFragmentViewHolder) holder;

            //이미지와 방 이름 설정
            switch (curChat.info.roomType) {
                case "plural":
                    //그룹채팅
                    viewHolder.imageView.setImageResource(tiers.get(curChat.info.roomTier));
                    viewHolder.name.setText(curChat.info.roomName);
                    break;
                case "singular":
                    //개인채팅
                    String destinationUid = null;
                    for (String user : curChat.users.keySet()) {
                        if (!uid.equals(user)) {
                            destinationUid = user;
                        }
                    }
                    if(destinationUid != null) {
                        FirebaseDatabase.getInstance().getReference().child("users/" + destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                Glide.with(holder.itemView.getContext()).load(userModel.profileIconURL).into(viewHolder.imageView);
                                String id = userModel.email;
                                id = id.substring(0, id.indexOf("@"));
                                String text = id + "\n(" + userModel.nickname + ")";
                                viewHolder.name.setText(text);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                    break;
            }
            //마지막 메세지 & 시간 설정
            Map<String, ChatModel.Comment> comments = new TreeMap<>(Collections.reverseOrder());
            comments.putAll(curChat.comments);
            if (comments.size() > 0) {
                String lastMessageKey = (String) comments.keySet().toArray()[0];
                viewHolder.lastMessage.setText(curChat.comments.get(lastMessageKey).message);

                long unixTime = (long) curChat.comments.get(lastMessageKey).timestamp;
                Date date = new Date(unixTime);
                String time = simpleDateFormat.format(date);
                viewHolder.time.setText(time);
            } else {
                viewHolder.lastMessage.setText("");
                viewHolder.time.setText("");
            }
            //클릭 이벤트 설정
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (viewMode == MODE.DISPLAY) {
                        Intent intent = new Intent(v.getContext(), MessageActivity.class);
                        intent.putExtra("roomId", curChat.info.roomId);
                        intent.putExtra("roomType", curChat.info.roomType);
                        intent.putExtra("roomName", viewHolder.name.getText().toString());
                        startActivity(intent);
                    } else {
                        viewHolder.checkBox.setChecked(!viewHolder.checkBox.isChecked());
                        checkedChat.put(curChat.info.roomId, viewHolder.checkBox.isChecked());
                    }
                }
            });
            //체크박스 바인드
            viewHolder.checkBox.setChecked(false);
            if (viewMode == MODE.DISPLAY) {
                viewHolder.checkBox.setVisibility(View.GONE);
            } else {
                for(String roomId : checkedChat.keySet()) {
                    if(curChat.info.roomId.equals(roomId)) {
                        viewHolder.checkBox.setChecked(checkedChat.get(roomId));
                    }
                }
                viewHolder.checkBox.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        private class ChatFragmentViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView name;
            public TextView time;
            public TextView lastMessage;
            public CheckBox checkBox;

            public ChatFragmentViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.chatItem_imageview);
                name = view.findViewById(R.id.chatItem_tv_chatname);
                time = view.findViewById(R.id.chatItem_tv_lasttime);
                lastMessage = view.findViewById(R.id.chatItem_tv_lastmessage);
                checkBox = view.findViewById(R.id.chatItem_checkbox);
            }
        }
    }
}
