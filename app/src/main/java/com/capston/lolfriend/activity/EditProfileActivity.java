package com.capston.lolfriend.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.capston.lolfriend.R;
import com.capston.lolfriend.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private UserModel myUserData;
    private ImageView iv_profile;
    private TextView tv_id;
    private TextView tv_nickName;
    private TextView tv_tier1;
    private TextView tv_tier2;
    private TextView tv_description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar tb = findViewById(R.id.editProfileActivity_toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_left_black_24dp);

        iv_profile = findViewById(R.id.editProfileActivity_iv_profile);
        tv_id = findViewById(R.id.editProfileActivity_tv_id);
        tv_nickName = findViewById(R.id.editProfileActivity_tv_nickname);
        tv_tier1 = findViewById(R.id.editProfileActivity_tv_tier_1);
        tv_tier2 = findViewById(R.id.editProfileActivity_tv_tier_2);
        tv_description = findViewById(R.id.editProfileActivity_tv_description);
        TextView tv_editPassword = findViewById(R.id.editProfileActivity_tv_edit_password);
        TextView tv_editGameInfo = findViewById(R.id.editProfileActivity_tv_edit_gameinfo);
        TextView tv_editDescription = findViewById(R.id.editProfileActivity_tv_edit_description);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myUserData = dataSnapshot.getValue(UserModel.class);

                //소환사 아이콘 이미지 설정
                Glide.with(EditProfileActivity.this)
                        .load(myUserData.profileIconURL)
                        .apply(new RequestOptions().circleCrop())
                        .into(iv_profile);
                //아이디 설정
                String id = myUserData.email;
                int index = id.indexOf("@");
                id = id.substring(0, index);
                tv_id.setText(id);
                tv_nickName.setText(myUserData.nickname);
                //랭크 설정
                String tier1 = myUserData.solo_tier;
                String tier2 = myUserData.free_tier;
                if (tier1.length() == 1) {
                    tv_tier1.setText("UnRanked");
                } else {
                    tv_tier1.setText(tier1);
                }
                if (tier2.length() == 1) {
                    tv_tier2.setText("UnRanked");
                } else {
                    tv_tier2.setText(tier2);
                }
                //내 소개 설정
                if (myUserData.description != null) {
                    tv_description.setText(myUserData.description);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClickEditPasswordView(View view) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_password, null);
        final EditText curPassword = dialogView.findViewById(R.id.dialogEditPassword_password_current);
        final EditText newPassword = dialogView.findViewById(R.id.dialogEditPassword_password_new);
        final EditText checkPassword = dialogView.findViewById(R.id.dialogEditPassword_password_check);

        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setView(dialogView)
                .setPositiveButton("변경", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (curPassword.getText().length() < 6 || newPassword.getText().length() < 6 || checkPassword.getText().length() < 6) {
                            Toast.makeText(EditProfileActivity.this, "최소 6자리 이상 입력하세요", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String cp = curPassword.getText().toString();
                        final String np = newPassword.getText().toString();
                        String ckp = checkPassword.getText().toString();

                        if (!cp.equals(np) && np.equals(ckp)) {
                            AuthCredential credential = EmailAuthProvider.getCredential(myUserData.email, cp);
                            FirebaseAuth.getInstance().getCurrentUser().reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        FirebaseAuth.getInstance().getCurrentUser().updatePassword(np).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()) {
                                                    Toast.makeText(EditProfileActivity.this, "변경되었습니다", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } else {
                                        Toast.makeText(EditProfileActivity.this, "현재 비밀번호가 맞지 않습니다", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(EditProfileActivity.this, "비밀번호를 다시 확인해주세요", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).create().show();
    }

    public void onClickEditGameInfoView(View view) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_game_info, null);
        final EditText nickname = dialogView.findViewById(R.id.dialogEditGameInfo_edittext);
        Button check = dialogView.findViewById(R.id.dialogEditGameInfo_button);
        final ImageView profileImage = dialogView.findViewById(R.id.dialogEditGameInfo_imageView);
        final TextView tier1 = dialogView.findViewById(R.id.dialogEditGameInfo_tier1);
        final TextView tier2 = dialogView.findViewById(R.id.dialogEditGameInfo_tier2);

        final UserModel tempModel = myUserData;

        final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch(msg.what) {
                    case 0:
                        Glide.with(EditProfileActivity.this)
                                .load(tempModel.profileIconURL)
                                .apply(new RequestOptions().circleCrop())
                                .into(profileImage);

                        if(tempModel.solo_tier.length() == 1) {
                            tier1.setText("개인랭크 : UnRanked");
                        } else {
                            tier1.setText("개인랭크 : " + tempModel.solo_tier);
                        }

                        if(tempModel.free_tier.length() == 1) {
                            tier2.setText("자유랭크 : UnRanked");
                        } else {
                            tier2.setText("자유랭크 : " + tempModel.free_tier);
                        }
                        break;
                    case 1:
                        Toast.makeText(EditProfileActivity.this, "소환사를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nickname.getText().length() == 0) {
                    return;
                }

                final String newNickname = nickname.getText().toString();
                FirebaseDatabase.getInstance().getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel snapShotModel;
                                String dbNickname;
                                String inputNickname;

                                for(DataSnapshot user : dataSnapshot.getChildren()) {
                                    snapShotModel = user.getValue(UserModel.class);
                                    dbNickname = snapShotModel.nickname;
                                    inputNickname = newNickname;
                                    dbNickname = dbNickname.replaceAll(" ", "");
                                    inputNickname = inputNickname.replaceAll(" ", "");

                                    if(dbNickname.equals(inputNickname)) {
                                        Toast.makeText(EditProfileActivity.this, "이미 가입된 닉네임입니다", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }

                                {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String url = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" +
                                                    newNickname +
                                                    "?api_key=" + getString(R.string.api_key);
                                            url = url.replaceAll(" ", "");

                                            String json = getJsonByURL(url);
                                            if (json != null) {
                                                try {
                                                    JSONObject jsonObject = new JSONObject(json);
                                                    //소환사 ID
                                                    String summonerId = jsonObject.getString("id");
                                                    //프로필아이콘 ID 및 URL
                                                    int profileIconId = jsonObject.getInt("profileIconId");
                                                    String profileIconURL = getString(R.string.asset_url) + profileIconId + ".png";

                                                    //솔랭, 자유랭 정보
                                                    String leagueInfo = getJsonByURL("https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" +
                                                            summonerId + "?api_key=" +
                                                            getString(R.string.api_key));

                                                    String tier1 = "";
                                                    String rank1 = "";
                                                    String tier2 = "";
                                                    String rank2 = "";
                                                    if (leagueInfo != null) {
                                                        JSONArray jsonArray = new JSONArray(leagueInfo);
                                                        for (int i = 0; i < jsonArray.length(); i++) {
                                                            JSONObject temp = jsonArray.getJSONObject(i);
                                                            switch (temp.getString("queueType")) {
                                                                case "RANKED_SOLO_5x5":
                                                                    tier1 = temp.getString("tier");
                                                                    rank1 = temp.getString("rank");
                                                                    break;
                                                                case "RANKED_FLEX_SR":
                                                                    tier2 = temp.getString("tier");
                                                                    rank2 = temp.getString("rank");
                                                                    break;
                                                            }
                                                        }
                                                    }

                                                    String finalTier1 = tier1 + " " + rank1;
                                                    String finalTier2 = tier2 + " " + rank2;

                                                    tempModel.nickname = newNickname;
                                                    tempModel.summonerId = summonerId;
                                                    tempModel.profileIconURL = profileIconURL;
                                                    tempModel.solo_tier = finalTier1;
                                                    tempModel.free_tier = finalTier2;

                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                                handler.sendEmptyMessage(0);
                                            } else {
                                                handler.sendEmptyMessage(1);
                                            }
                                        }
                                    }).start();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setView(dialogView)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(nickname.getText().length() > 3) {
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            Map<String, Object> map = new HashMap<>();
                            map.put("nickname", tempModel.nickname);
                            map.put("summonerId", tempModel.summonerId);
                            map.put("profileIconURL", tempModel.profileIconURL);
                            map.put("solo_tier", tempModel.solo_tier);
                            map.put("free_tier", tempModel.free_tier);
                            FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(EditProfileActivity.this, "변경되었습니다", Toast.LENGTH_SHORT).show();
                                        recreate();
                                    }
                                }
                            });
                        }
                    }
                }).create().show();
    }

    public void onClickEditDescription(View view) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_description, null);
        final EditText et_description = dialogView.findViewById(R.id.dialogEditDescription_edittext);

        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setView(dialogView)
                .setPositiveButton("변경", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(et_description.getText().length() == 0) {
                            Toast.makeText(EditProfileActivity.this, "최소 한 자리 이상 입력하세요", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final String text = et_description.getText().toString();
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        FirebaseDatabase.getInstance().getReference().child("users/" + uid + "/description").setValue(text).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()) {
                                    tv_description.setText(text);
                                    Toast.makeText(EditProfileActivity.this, "변경되었습니다", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }).create().show();
    }

    @Nullable
    private String getJsonByURL(String url) {
        InputStream is = null;
        try {
            is = new URL(url).openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String str;
            StringBuffer buffer = new StringBuffer();
            while ((str = br.readLine()) != null) {
                buffer.append(str);
            }
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
