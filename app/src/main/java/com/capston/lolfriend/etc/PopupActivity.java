package com.capston.lolfriend.etc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.capston.lolfriend.R;
import com.capston.lolfriend.activity.MessageActivity;
import com.capston.lolfriend.model.ChatModel;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class PopupActivity extends Activity {

    private Spinner player;
    private Spinner tier;
    private List<String> gameTypeList;
    private List<String> tierList;
    private ArrayAdapter<String> playerAdapter;
    private ArrayAdapter<String> tierAdapter;
    private EditText name;
    private EditText description;
    private Button button;
    private CustomProgressDialog dialog;
    private InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int width = (int)(dm.widthPixels * 0.9);
        getWindow().getAttributes().width = width;

        player = findViewById(R.id.popupActivity_spinner_player);
        tier = findViewById(R.id.popupActivity_spinner_tier);
        name = findViewById(R.id.popupActivity_edittext_name);
        description = findViewById(R.id.popupActivity_edittext_description);
        button = findViewById(R.id.popupActivity_button);
        dialog = new CustomProgressDialog(PopupActivity.this);

        //게임 종류 리스트 초기화
        {
            gameTypeList = new ArrayList<>();

            gameTypeList.add("3인 자유랭크");
            gameTypeList.add("5인 자유랭크");

            playerAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, gameTypeList);
        }

        //티어 종류 리스트 초기화
        {
            tierList = new ArrayList<>();

            tierList.add("IRON");
            tierList.add("BRONZE");
            tierList.add("SILVER");
            tierList.add("GOLD");
            tierList.add("PLATINUM");
            tierList.add("DIAMOND");
            tierList.add("MASTER");
            tierList.add("GRANDMASTER");
            tierList.add("CHALLENGER");

            tierAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, tierList);
        }

        player.setAdapter(playerAdapter);
        tier.setAdapter(tierAdapter);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(name.getText().length() == 0) {
                    Toast.makeText(PopupActivity.this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                int adCount = PreferenceManager.getInt(PopupActivity.this, "ad_display_count");
                PreferenceManager.setInt(PopupActivity.this, "ad_display_count", ++adCount);
                if(adCount > 9) {
                    PreferenceManager.setInt(PopupActivity.this, "ad_display_count", 0);
                    PreferenceManager.setBoolean(PopupActivity.this, "ad_display_state", true);
                }

                boolean isShowAd = PreferenceManager.getBoolean(PopupActivity.this, "ad_display_state");
                if(isShowAd && interstitialAd.isLoaded()) {
                    PreferenceManager.setBoolean(PopupActivity.this, "ad_display_state", false);
                    interstitialAd.show();
                } else {
                    dialog.show();

                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    final ChatModel chatModel = new ChatModel();
                    chatModel.users.put(uid, true);

                    final String roomId = FirebaseDatabase.getInstance().getReference().child("chatrooms").push().getKey();

                    chatModel.info = new ChatModel.Info();
                    chatModel.info.roomName = name.getText().toString();
                    chatModel.info.gameType = player.getSelectedItem().toString();
                    chatModel.info.roomTier = tier.getSelectedItem().toString();
                    chatModel.info.roomDescription = description.getText().toString();
                    chatModel.info.roomId = roomId;
                    chatModel.info.roomType = "plural";

                    FirebaseDatabase.getInstance().getReference()
                            .child("chatrooms").child(roomId).setValue(chatModel)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(PopupActivity.this, MessageActivity.class);
                                    intent.putExtra("roomId", roomId);
                                    intent.putExtra("roomType", "plural");
                                    intent.putExtra("roomName", chatModel.info.roomName);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                }
            }
        });

        interstitialAd = new InterstitialAd(PopupActivity.this);
        interstitialAd.setAdUnitId("ca-app-pub-6457311703084676~2277355657");   //교체 : ca-app-pub-3940256099942544/1033173712
        interstitialAd.loadAd(new AdRequest.Builder().build());
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                interstitialAd.loadAd(new AdRequest.Builder().build());

                dialog.show();

                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                final ChatModel chatModel = new ChatModel();
                chatModel.users.put(uid, true);

                final String roomId = FirebaseDatabase.getInstance().getReference().child("chatrooms").push().getKey();

                chatModel.info = new ChatModel.Info();
                chatModel.info.roomName = name.getText().toString();
                chatModel.info.gameType = player.getSelectedItem().toString();
                chatModel.info.roomTier = tier.getSelectedItem().toString();
                chatModel.info.roomDescription = description.getText().toString();
                chatModel.info.roomId = roomId;
                chatModel.info.roomType = "plural";

                FirebaseDatabase.getInstance().getReference()
                        .child("chatrooms").child(roomId).setValue(chatModel)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                dialog.dismiss();
                                Intent intent = new Intent(PopupActivity.this, MessageActivity.class);
                                intent.putExtra("roomId", roomId);
                                intent.putExtra("roomType", "plural");
                                intent.putExtra("roomName", chatModel.info.roomName);
                                startActivity(intent);
                                finish();
                            }
                        });
            }
        });
    }
}
