package com.capston.lolfriend.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.capston.lolfriend.R;
import com.capston.lolfriend.etc.PreferenceManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.capston.lolfriend.fragment.ChatFragment;
import com.capston.lolfriend.fragment.HomeFragment;
import com.capston.lolfriend.fragment.MoreFragment;
import com.capston.lolfriend.fragment.PeopleFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Fragment homeFragment;
    private Fragment peopleFragment;
    private Fragment chatFragment;
    private Fragment moreFragment;
    private String uid;
    private Map<String, Object> stateMap;
    private TextView title;
    private long time = 0;
    public InterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bnv = findViewById(R.id.mainActivity_bnv);
        Toolbar tb = findViewById(R.id.mainActivity_toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        title = findViewById(R.id.mainActivity_toolbar_title);

        homeFragment = new HomeFragment();
        peopleFragment = new PeopleFragment();
        chatFragment = new ChatFragment();
        moreFragment = new MoreFragment();

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        stateMap = new HashMap<>();

        if (getIntent().getStringExtra("request") != null && getIntent().getStringExtra("request").equals("changeInfo")) {
            bnv.setSelectedItemId(R.id.more);
            getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_layout_frame, moreFragment).commit();
            startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
        } else {
            bnv.setSelectedItemId(R.id.home);
            getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_layout_frame, homeFragment).commit();
        }

        bnv.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment fragment = null;

                switch (menuItem.getItemId()) {
                    case R.id.home:
                        fragment = homeFragment;
                        break;
                    case R.id.people:
                        fragment = peopleFragment;
                        break;
                    case R.id.chat:
                        fragment = chatFragment;
                        break;
                    case R.id.more:
                        fragment = moreFragment;
                        break;
                }
                getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_layout_frame, fragment).commit();
                return true;
            }
        });
        passPushTokenToServer();

        interstitialAd = new InterstitialAd(MainActivity.this);
        interstitialAd.setAdUnitId("ca-app-pub-6457311703084676~2277355657");   //교체 : ca-app-pub-3940256099942544/1033173712
        interstitialAd.loadAd(new AdRequest.Builder().build());
    }

    @Override
    protected void onStart() {
        super.onStart();

        long lastLoginTime = System.currentTimeMillis();

        stateMap.put("login_state", true);
        stateMap.put("last_time", lastLoginTime);

        FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(stateMap);
    }

    @Override
    protected void onStop() {
        super.onStop();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            long lastLoginTime = System.currentTimeMillis();

            stateMap.put("login_state", false);
            stateMap.put("last_time", lastLoginTime);

            FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(stateMap);
        }
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - time >= 2000) {
            time = System.currentTimeMillis();
            Toast.makeText(MainActivity.this, "버튼을 한번 더 누르면 종료합니다.", Toast.LENGTH_SHORT).show();
        } else if (System.currentTimeMillis() - time < 2000) {
            finish();
        }
    }

    void passPushTokenToServer() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }
                final String token = task.getResult().getToken();

                Map<String, Object> map = new HashMap<>();
                map.put("pushToken", token);

                FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(map);
            }
        });
    }

    public void setActionBarTitle(String text) {
        title.setText(text);
    }
}
