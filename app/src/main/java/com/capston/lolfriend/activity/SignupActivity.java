package com.capston.lolfriend.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.capston.lolfriend.etc.CustomProgressDialog;
import com.capston.lolfriend.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import com.capston.lolfriend.model.UserModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class SignupActivity extends AppCompatActivity {

    private EditText nickname;
    private EditText id;
    private EditText password;
    private Button signup;
    private CustomProgressDialog dialog;
    private UserModel userModel;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //뷰 초기화
        {
            nickname = findViewById(R.id.signupActivity_et_nickname);
            id = findViewById(R.id.signupActivity_et_id);
            password = findViewById(R.id.signupActivity_et_password);
            signup = findViewById(R.id.signupActivity_btn_signup);

            dialog = new CustomProgressDialog(SignupActivity.this);

            userModel = new UserModel();

            //핸들러 기능 구현
            {
                handler = new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(@NonNull Message msg) {
                        switch (msg.what) {
                            case 0: //오류발생
                                dialog.dismiss();
                                Toast.makeText(SignupActivity.this, "소환사를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                                break;
                            case 1: //작업실행
                                final String email = id.getText().toString() + getString(R.string.domain);
                                FirebaseAuth.getInstance()
                                        .createUserWithEmailAndPassword(email, password.getText().toString())
                                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                try {
                                                    String uid = task.getResult().getUser().getUid();

                                                    userModel.nickname = nickname.getText().toString();
                                                    userModel.email = email;
                                                    userModel.uid = uid;
                                                    userModel.description = " ";

                                                    FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(userModel)
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    dialog.dismiss();
                                                                    SignupActivity.this.finish();
                                                                }
                                                            });
                                                } catch (RuntimeExecutionException e) {
                                                    dialog.dismiss();
                                                    Toast.makeText(SignupActivity.this, "중복된 아이디 입니다", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                break;
                        }
                        return true;
                    }
                });
            }
        }

        //리스너 설정
        {
            signup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (nickname.getText().length() == 0 || id.getText().length() == 0 || password.getText().length() == 0) {
                        return;
                    }

                    dialog.show();

                    //롤 닉네임 체크 부분
                    {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String url = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" +
                                        nickname.getText().toString() +
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

                                        userModel.summonerId = summonerId;
                                        userModel.profileIconURL = profileIconURL;
                                        userModel.solo_tier = finalTier1;
                                        userModel.free_tier = finalTier2;

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    handler.sendEmptyMessage(1);
                                } else {
                                    handler.sendEmptyMessage(0);
                                }
                            }
                        }).start();
                    }
                }
            });
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
}
