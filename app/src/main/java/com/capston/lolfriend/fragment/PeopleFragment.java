package com.capston.lolfriend.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.capston.lolfriend.R;
import com.capston.lolfriend.activity.MainActivity;
import com.capston.lolfriend.activity.MessageActivity;
import com.capston.lolfriend.activity.ProfileInfoActivity;
import com.capston.lolfriend.etc.PreferenceManager;
import com.capston.lolfriend.etc.TimeString;
import com.capston.lolfriend.model.ChatModel;
import com.capston.lolfriend.model.UserModel;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PeopleFragment extends Fragment {

    private String uid;
    private String destinationUid;
    private String roomId;
    private List<UserModel> userModels;

    private RecyclerView recyclerView;

    private InterstitialAd interstitialAd;

    public PeopleFragment() {
        userModels = new ArrayList<>();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserModel userModel = snapshot.getValue(UserModel.class);
                    if(!userModel.uid.equals(uid)) {
                        userModels.add(userModel);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);

        recyclerView = view.findViewById(R.id.peopleFragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        recyclerView.setAdapter(new PeopleFragmentAdapter());

        MainActivity activity = (MainActivity)getActivity();
        if(activity != null) {
            activity.setActionBarTitle("유저");
        }

        setHasOptionsMenu(true);

        interstitialAd = ((MainActivity)getActivity()).interstitialAd;

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.peoplefragment_toolbar_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.peopleFragment_toolbar_action_refresh) {
            Toast.makeText(getActivity(), "갱신되었습니다", Toast.LENGTH_SHORT).show();

            FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserModel userModel;

                    userModels.clear();
                    for(DataSnapshot user : dataSnapshot.getChildren()) {
                        userModel = user.getValue(UserModel.class);
                        if(!userModel.uid.equals(uid)) {
                            userModels.add(userModel);
                        }
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

    void onClickUserItem(final UserModel user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_info, null);
        ImageView iv_usericon = dialogView.findViewById(R.id.dialogUserInfo_iv_usericon);
        TextView tv_username = dialogView.findViewById(R.id.dialogUserInfo_tv_username);
        TextView tv_usertier = dialogView.findViewById(R.id.dialogUserInfo_tv_usertier);
        TextView tv_userdescription = dialogView.findViewById(R.id.dialogUserInfo_tv_userdescription);

        //usericon binding
        Glide.with(getActivity())
                .load(user.profileIconURL)
                .apply(new RequestOptions().circleCrop())
                .into(iv_usericon);
        //username binding
        String id = user.email;
        id = id.substring(0, id.indexOf("@"));
        String text = id + "(" + user.nickname + ")";
        tv_username.setText(text);
        //usertier binding
        String tierText;
        String A;
        String B;
        if(user.solo_tier.length() == 1) {
            A = "UnRanked";
        } else {
            A = user.solo_tier;
        }
        if(user.free_tier.length() == 1) {
            B = "UnRanked";
        } else {
            B = user.free_tier;
        }
        tierText = A + "\n" + B;
        tv_usertier.setText(tierText);
        //userdescription binding
        String descriptiontext = user.description;
        tv_userdescription.setText(descriptiontext);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setView(dialogView)
                .setPositiveButton("대화하기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        destinationUid = user.uid;
                        roomId = null;

                        FirebaseDatabase.getInstance().getReference()
                                .child("chatrooms").orderByChild("users/" + uid).equalTo(true)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for(DataSnapshot chatroom : dataSnapshot.getChildren()) {
                                            ChatModel chatModel = chatroom.getValue(ChatModel.class);
                                            if(chatModel.users.containsKey(destinationUid)) {
                                                roomId = chatroom.getKey();
                                                break;
                                            }
                                        }

                                        if(roomId == null) {
                                            int adCount = PreferenceManager.getInt(getActivity(), "ad_display_count");
                                            PreferenceManager.setInt(getActivity(), "ad_display_count", ++adCount);
                                            if(adCount > 9) {
                                                PreferenceManager.setInt(getActivity(), "ad_display_count", 0);
                                                PreferenceManager.setBoolean(getActivity(), "ad_display_state", true);
                                            }

                                            boolean isShowAd = PreferenceManager.getBoolean(getActivity(), "ad_display_state");
                                            if(isShowAd && interstitialAd.isLoaded()) {
                                                AdListener adListener = new AdListener() {
                                                    @Override
                                                    public void onAdClosed() {
                                                        interstitialAd.loadAd(new AdRequest.Builder().build());

                                                        roomId = FirebaseDatabase.getInstance().getReference().child("chatrooms").push().getKey();

                                                        ChatModel chatModel = new ChatModel();
                                                        chatModel.users.put(uid, true);
                                                        chatModel.users.put(destinationUid, true);
                                                        chatModel.info = new ChatModel.Info();
                                                        chatModel.info.roomId = roomId;
                                                        chatModel.info.roomType = "singular";

                                                        FirebaseDatabase.getInstance().getReference()
                                                                .child("chatrooms").child(roomId).setValue(chatModel)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        Intent intent = new Intent(getActivity(), MessageActivity.class);
                                                                        intent.putExtra("roomId", roomId);
                                                                        intent.putExtra("roomType", "singular");

                                                                        String email = user.email;
                                                                        int end = email.indexOf("@");
                                                                        email = email.substring(0, end);
                                                                        String text = email + "(" + user.nickname + ")";

                                                                        intent.putExtra("roomName", text);
                                                                        startActivity(intent);
                                                                    }
                                                                });
                                                    }
                                                };
                                                interstitialAd.setAdListener(adListener);

                                                PreferenceManager.setBoolean(getActivity(), "ad_display_state", false);
                                                interstitialAd.show();
                                            } else {
                                                roomId = FirebaseDatabase.getInstance().getReference().child("chatrooms").push().getKey();

                                                ChatModel chatModel = new ChatModel();
                                                chatModel.users.put(uid, true);
                                                chatModel.users.put(destinationUid, true);
                                                chatModel.info = new ChatModel.Info();
                                                chatModel.info.roomId = roomId;
                                                chatModel.info.roomType = "singular";

                                                FirebaseDatabase.getInstance().getReference()
                                                        .child("chatrooms").child(roomId).setValue(chatModel)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Intent intent = new Intent(getActivity(), MessageActivity.class);
                                                                intent.putExtra("roomId", roomId);
                                                                intent.putExtra("roomType", "singular");

                                                                String email = user.email;
                                                                int end = email.indexOf("@");
                                                                email = email.substring(0, end);
                                                                String text = email + "(" + user.nickname + ")";

                                                                intent.putExtra("roomName", text);
                                                                startActivity(intent);
                                                            }
                                                        });
                                            }
                                        } else {
                                            Intent intent = new Intent(getActivity(), MessageActivity.class);
                                            intent.putExtra("roomId", roomId);
                                            intent.putExtra("roomType", "singular");

                                            String email = user.email;
                                            int end = email.indexOf("@");
                                            email = email.substring(0, end);
                                            String text = email + "(" + user.nickname + ")";

                                            intent.putExtra("roomName", text);
                                            startActivity(intent);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                    }
                })
                .setNeutralButton("자세히보기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(getActivity(), ProfileInfoActivity.class);
                        intent.putExtra("uid", user.uid);
                        startActivity(intent);
                    }
                }).create().show();
    }

    class PeopleFragmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        public PeopleFragmentAdapter() {

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_people, parent, false);

            return new PeopleFragmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

            final PeopleFragmentViewHolder curHolder = (PeopleFragmentViewHolder)holder;

            Glide.with(holder.itemView.getContext()).load(userModels.get(position).profileIconURL).into(curHolder.imageView);

            String id = userModels.get(position).email;
            id = id.substring(0, id.indexOf("@"));
            String text = id + "\n(" + userModels.get(position).nickname + ")";
            curHolder.nickname.setText(text);

            if(userModels.get(position).login_state) {
                curHolder.lasttime.setText("접속중");
            } else {
                long last_time = (long) userModels.get(position).last_time;
                String msg = TimeString.formatTimeString(last_time);
                curHolder.lasttime.setText(msg);
            }

            curHolder.description.setText(userModels.get(position).description);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    onClickUserItem(userModels.get(position));
                }
            });
        }

        @Override
        public int getItemCount() {
            return userModels.size();
        }

        private class PeopleFragmentViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView nickname;
            public TextView lasttime;
            public TextView description;

            public PeopleFragmentViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.peopleItem_iv);
                nickname = view.findViewById(R.id.peopleItem_tv_nickname);
                lasttime = view.findViewById(R.id.peopleItem_tv_lasttime);
                description = view.findViewById(R.id.peopleItem_tv_description);
            }
        }
    }
}
