package com.capston.lolfriend.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.capston.lolfriend.etc.CustomProgressDialog;
import com.capston.lolfriend.R;
import com.capston.lolfriend.etc.NetworkManager;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private EditText id;
    private EditText password;
    private Button login;
    private Button signup;

    private CustomProgressDialog dialog;
    private Handler handler;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initVariable();
    }

    void initVariable() {
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {
                if (id.getText().length() == 0) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        dialog.show();

                        checkNickname(user);
                    }
                } else {
                    final FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        final String email = user.getEmail();
                        FirebaseDatabase.getInstance().getReference().child("users").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot key : dataSnapshot.getChildren()) {
                                    String otherEmail = key.child("email").getValue(String.class);
                                    if (otherEmail.equals(email) && key.child("pushToken").exists()) {
                                        firebaseAuth.signOut();
                                        dialog.dismiss();
                                        Toast.makeText(LoginActivity.this, "다른 기기에서 이용 중 입니다", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                                checkNickname(user);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }
        };

        id = findViewById(R.id.loginActivity_et_id);
        password = findViewById(R.id.loginActivity_et_password);
        login = findViewById(R.id.loginActivity_btn_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginEvent();
            }
        });
        signup = findViewById(R.id.loginActivity_btn_signup);
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });

        dialog = new CustomProgressDialog(LoginActivity.this);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                boolean value = false;
                switch (msg.what) {
                    case 0:     //오류
                        Toast.makeText(LoginActivity.this, "소환사를 찾을 수 없습니다. 소환사 정보를 변경해주세요", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("request", "changeInfo");
                        startActivity(intent);
                        dialog.dismiss();
                        finish();
                        value = false;
                        break;
                    case 1:     //정상
                        Toast.makeText(LoginActivity.this, "접속되었습니다", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        dialog.dismiss();
                        finish();
                        value = true;
                        break;
                }
                return value;
            }
        });

        uid = null;
    }

    void checkNickname(FirebaseUser user) {
        uid = user.getUid();
        FirebaseDatabase.getInstance().getReference().child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String nickname = null;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if (child.getKey().equals("nickname")) {
                        nickname = child.getValue(String.class);
                        break;
                    }
                }
                findSummonerName(nickname);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        MobileAds.initialize(LoginActivity.this, "ca-app-pub-6457311703084676~2277355657");
    }

    void findSummonerName(final String nickname) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" +
                        nickname + "?api_key=" + getString(R.string.api_key);
                url = url.replaceAll(" ", "");

                String json = getJsonByURL(url);
                //소환사 json 정보 검사
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

                        Map<String, Object> map = new HashMap<>();
                        map.put("summonerId", summonerId);
                        map.put("profileIconURL", profileIconURL);
                        map.put("solo_tier", finalTier1);
                        map.put("free_tier", finalTier2);

                        FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (!task.isSuccessful()) {
                                    return;
                                }
                                handler.sendEmptyMessage(1);
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    handler.sendEmptyMessage(0);
                }
            }
        }).start();
    }

    void loginEvent() {
        //인터넷 연결 상태 검사
        if (NetworkManager.checkNetworkState(LoginActivity.this)) {
            if (id.getText().length() != 0 && password.getText().length() != 0) {
                dialog.show();

                String email = id.getText().toString() + getString(R.string.domain);
                firebaseAuth.signInWithEmailAndPassword(email, password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //로그인 실패시
                        if (!task.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        } else {
            Toast.makeText(LoginActivity.this, "인터넷 연결을 확인하세요", Toast.LENGTH_SHORT).show();
        }


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

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }
}
