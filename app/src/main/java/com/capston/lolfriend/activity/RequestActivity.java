package com.capston.lolfriend.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.capston.lolfriend.R;
import com.capston.lolfriend.etc.CustomProgressDialog;
import com.capston.lolfriend.etc.GMailSender;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

public class RequestActivity extends AppCompatActivity {

    private CustomProgressDialog mDialog;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        Toolbar tb = findViewById(R.id.requestActivity_toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_chevron_left_black_24dp);

        mDialog = new CustomProgressDialog(RequestActivity.this);

        final EditText subject = findViewById(R.id.requestActivity_et_subject);
        final EditText content = findViewById(R.id.requestActivity_et_content);
        Button send = findViewById(R.id.requestActivity_btn_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(subject.getText().toString().length() == 0 || content.getText().toString().length() == 0) {
                    Toast.makeText(RequestActivity.this, "모든 내용은 필수 기재입니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                mDialog.show();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String email = user.getEmail();
                sendMail(subject.getText().toString(), "보낸유저 : " + email + "\n\n" + content.getText().toString());
            }
        });

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                boolean value = false;
                switch (msg.what) {
                    case 0:     //성공
                        value = true;
                        mDialog.dismiss();
                        Toast.makeText(RequestActivity.this, "메일을 성공적으로 보냈습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                    case 1:     //실패1
                        value = false;
                        mDialog.dismiss();
                        Toast.makeText(RequestActivity.this, "이메일 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:     //실패2
                        value = false;
                        mDialog.dismiss();
                        Toast.makeText(RequestActivity.this, "인터넷 연결을 확인해주세요", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:     //실패3
                        value = false;
                        mDialog.dismiss();
                        break;
                }
                return value;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    void sendMail(final String subject, final String content) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String user = "true.pine55@gmail.com";
                String password = "Forest..1029";
                String sendTo = "true_pine5@naver.com";

                try {
                    GMailSender gMailSender = new GMailSender(user, password);
                    gMailSender.sendMail(subject, content, sendTo);
                    handler.sendEmptyMessage(0);
                } catch (SendFailedException e) {
                    handler.sendEmptyMessage(1);
                } catch (MessagingException e) {
                    handler.sendEmptyMessage(2);
                } catch (Exception e) {
                    handler.sendEmptyMessage(3);
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
