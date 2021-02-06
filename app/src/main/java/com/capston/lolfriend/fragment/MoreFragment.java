package com.capston.lolfriend.fragment;

import android.content.DialogInterface;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.capston.lolfriend.R;
import com.capston.lolfriend.activity.EditProfileActivity;
import com.capston.lolfriend.activity.LoginActivity;
import com.capston.lolfriend.activity.MainActivity;
import com.capston.lolfriend.activity.NoticeActivity;
import com.capston.lolfriend.activity.RequestActivity;
import com.capston.lolfriend.etc.CustomProgressDialog;
import com.capston.lolfriend.etc.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class MoreFragment extends Fragment {

    private CustomProgressDialog mDialog;

    public MoreFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        MainActivity activity = (MainActivity)getActivity();
        if(activity != null) {
            activity.setActionBarTitle("더보기");
        }

        RelativeLayout notice = view.findViewById(R.id.moreFragment_tv_notice);
        RelativeLayout request = view.findViewById(R.id.moreFragment_tv_request);
        RelativeLayout editProfile = view.findViewById(R.id.moreFragment_tv_editProfile);
        RelativeLayout logOut = view.findViewById(R.id.moreFragment_tv_logout);
        RelativeLayout resign = view.findViewById(R.id.moreFragment_tv_resign);
        mDialog = new CustomProgressDialog(getActivity());

        //공지사항 리스너
        notice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), NoticeActivity.class));
            }
        });

        //문의하기 리스너
        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), RequestActivity.class));
            }
        });

        //프로필 수정 리스너
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), EditProfileActivity.class));
            }
        });

        //로그아웃 리스너
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("확인").setMessage("로그아웃 하시겠습니까?")
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("pushToken").setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()) {
                                            //로그아웃 상태 처리
                                            Map<String, Object> stateMap = new HashMap<>();
                                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                            if (user != null) {
                                                long lastLoginTime = System.currentTimeMillis();

                                                stateMap.put("login_state", false);
                                                stateMap.put("last_time", lastLoginTime);

                                                FirebaseDatabase.getInstance().getReference().child("users").child(uid).updateChildren(stateMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            //로그아웃
                                                            FirebaseAuth.getInstance().signOut();
                                                            //광고 설정값 초기화
                                                            PreferenceManager.setInt(getActivity(), "ad_display_count", 0);
                                                            PreferenceManager.setBoolean(getActivity(), "ad_display_state", true);
                                                            //로그인화면 실행
                                                            startActivity(new Intent(getActivity(), LoginActivity.class));
                                                            getActivity().finish();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create().show();
            }
        });

        //회원탈퇴 리스너
        resign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickResignView();
            }
        });

        return view;
    }

    private void onClickResignView() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_resign, null);
        final EditText password = dialogView.findViewById(R.id.dialogResign_et);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView)
                .setPositiveButton("탈퇴", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (password.getText().length() == 0) {
                            Toast.makeText(getActivity(), "비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mDialog.show();

                        //재로그인
                        AuthCredential credential = EmailAuthProvider.getCredential(FirebaseAuth.getInstance().getCurrentUser().getEmail(), password.getText().toString());
                        FirebaseAuth.getInstance().getCurrentUser().reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()) {
                                    //데이터 삭제
                                    final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/" + uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            for(DataSnapshot room : dataSnapshot.getChildren()) {
                                                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(room.getKey()).child("users").child(uid).setValue(null);
                                            }
                                            FirebaseDatabase.getInstance().getReference().child("users").child(uid).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        //계정 삭제
                                                        FirebaseAuth.getInstance().getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()) {
                                                                    //광고 설정값 초기화
                                                                    PreferenceManager.setInt(getActivity(), "ad_display_count", 0);
                                                                    PreferenceManager.setBoolean(getActivity(), "ad_display_state", true);

                                                                    mDialog.dismiss();
                                                                    Toast.makeText(getActivity(), "이용해주셔서 감사합니다", Toast.LENGTH_SHORT).show();
                                                                    startActivity(new Intent(getActivity(), LoginActivity.class));
                                                                    getActivity().finish();
                                                                }
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                } else {
                                    mDialog.dismiss();
                                    Toast.makeText(getActivity(), "비밀번호가 맞지 않습니다", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }).create().show();
    }
}
