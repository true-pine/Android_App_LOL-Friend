package com.capston.lolfriend.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.capston.lolfriend.R;
import com.capston.lolfriend.etc.CustomProgressDialog;

//미완성
public class ProfileInfoActivity extends AppCompatActivity {

    CustomProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_info);

        mDialog = new CustomProgressDialog(ProfileInfoActivity.this);

        String userUid = getIntent().getStringExtra("uid");
    }

    class ProfileInfoActivityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profileinfo, parent, false);
            return new ProfileInfoActivityViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }

        public class ProfileInfoActivityViewHolder extends RecyclerView.ViewHolder {

            public ProfileInfoActivityViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
