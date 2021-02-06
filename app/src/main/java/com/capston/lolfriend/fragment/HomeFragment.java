package com.capston.lolfriend.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.capston.lolfriend.activity.MainActivity;
import com.capston.lolfriend.etc.DialogRoomInfoAdapter;
import com.capston.lolfriend.etc.PopupActivity;
import com.capston.lolfriend.R;
import com.capston.lolfriend.activity.MessageActivity;
import com.capston.lolfriend.model.ChatModel;
import com.capston.lolfriend.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private Map<String, Integer> tiers;

    private List<ChatModel> rooms;
    private String uid;

    private RecyclerView recyclerView;

    public HomeFragment() {
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

        rooms = new ArrayList<>();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference().child("chatrooms").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ChatModel chatModel;
                for (DataSnapshot room : dataSnapshot.getChildren()) {
                    chatModel = room.getValue(ChatModel.class);
                    if (chatModel.info.roomType.equals("singular") || chatModel.users.containsKey(uid)) {
                        continue;
                    }
                    rooms.add(chatModel);
                }
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        FloatingActionButton fab = view.findViewById(R.id.homeFragment_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PopupActivity.class);
                startActivity(intent);
            }
        });

        recyclerView = view.findViewById(R.id.homeFragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new HomeFragmentAdapter());

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.setActionBarTitle("채팅방");
        }

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).users.containsKey(uid)) {
                rooms.remove(i);
            }
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.homefragment_toolbar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.homeFragment_toolbar_action_refresh) {
            Toast.makeText(getActivity(), "갱신되었습니다", Toast.LENGTH_SHORT).show();

            FirebaseDatabase.getInstance().getReference().child("chatrooms").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    ChatModel chatModel;

                    rooms.clear();
                    for (DataSnapshot room : dataSnapshot.getChildren()) {
                        chatModel = room.getValue(ChatModel.class);
                        if (chatModel.info.roomType.equals("singular") || chatModel.users.containsKey(uid)) {
                            continue;
                        }
                        rooms.add(chatModel);
                    }
                    recyclerView.getAdapter().notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    void onClickRoomItem(final Map<String, Boolean> map, final ChatModel room, final int position) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_room_info, null);
        TextView tv_roomname = dialogView.findViewById(R.id.dialogRoomInfo_roomname);
        final RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRoomInfo_recyclerview);

        final Map<String, Boolean> userMap = map;
        final List<UserModel> users = new ArrayList<>();

        tv_roomname.setText(room.info.roomName);

        FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot user : dataSnapshot.getChildren()) {
                    Boolean tmp = userMap.get(user.getKey());
                    if (tmp != null && tmp) {
                        users.add(user.getValue(UserModel.class));
                    }
                }
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                recyclerView.setAdapter(new DialogRoomInfoAdapter(users, getActivity()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setView(dialogView)
                .setPositiveButton("참여하기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseDatabase.getInstance().getReference()
                                .child("chatrooms").child(room.info.roomId).child("users")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                                        int userNumber = map.size();
                                        int maxUserNumber = Integer.parseInt(room.info.gameType.substring(0, 1));
                                        if (userNumber < maxUserNumber) {
                                            map.put(uid, true);
                                            FirebaseDatabase.getInstance().getReference()
                                                    .child("chatrooms").child(room.info.roomId).child("users").updateChildren(map)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            rooms.get(position).users.put(uid, true);

                                                            Intent intent = new Intent(getActivity(), MessageActivity.class);
                                                            intent.putExtra("roomId", room.info.roomId);
                                                            intent.putExtra("roomType", "plural");
                                                            intent.putExtra("roomName", room.info.roomName);
                                                            startActivity(intent);
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(getActivity(), "인원이 초과되었습니다", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                    }
                }).create().show();
    }

    class HomeFragmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public HomeFragmentAdapter() {

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);

            return new HomeFragmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            ((HomeFragmentViewHolder) holder).imageView.setImageResource(tiers.get(rooms.get(position).info.roomTier));
            ((HomeFragmentViewHolder) holder).roomName.setText(rooms.get(position).info.roomName);
            ((HomeFragmentViewHolder) holder).description.setText(rooms.get(position).info.roomDescription);
            ((HomeFragmentViewHolder) holder).gameType.setText(rooms.get(position).info.gameType);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    onClickRoomItem(rooms.get(position).users,rooms.get(position), position);
                    /*final ChatModel curRoom = rooms.get(position);
                    FirebaseDatabase.getInstance().getReference()
                            .child("chatrooms").child(curRoom.info.roomId).child("users")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                            int userNumber = map.size();
                            int maxUserNumber = Integer.parseInt(curRoom.info.gameType.substring(0, 1));
                            if(userNumber < maxUserNumber) {
                                map.put(uid, true);
                                FirebaseDatabase.getInstance().getReference()
                                        .child("chatrooms").child(curRoom.info.roomId).child("users").updateChildren(map)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                rooms.get(position).users.put(uid, true);

                                                Intent intent = new Intent(v.getContext(), MessageActivity.class);
                                                intent.putExtra("roomId", curRoom.info.roomId);
                                                intent.putExtra("roomType", "plural");
                                                intent.putExtra("roomName", curRoom.info.roomName);
                                                startActivity(intent);
                                            }
                                        });
                            } else {
                                Toast.makeText(v.getContext(), "인원이 초과되었습니다", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });*/
                }
            });
        }

        @Override
        public int getItemCount() {
            return rooms.size();
        }

        private class HomeFragmentViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView roomName;
            public TextView description;
            public TextView gameType;

            public HomeFragmentViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.roomItem_imageview);
                roomName = view.findViewById(R.id.roomItem_tv_name);
                description = view.findViewById(R.id.roomItem_tv_description);
                gameType = view.findViewById(R.id.roomItem_tv_gameType);
            }
        }
    }
}
