package com.capston.lolfriend.etc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.capston.lolfriend.R;
import com.capston.lolfriend.model.UserModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DialogRoomInfoAdapter extends RecyclerView.Adapter<DialogRoomInfoAdapter.ViewHolder> {

    private List<UserModel> data;
    private Context context;
    private Map<String, Integer> tiers;

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_usericon;
        TextView tv_username;
        ImageView iv_solotier;
        ImageView iv_freetier;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_usericon = itemView.findViewById(R.id.itemRoomInfo_iv_usericon);
            tv_username = itemView.findViewById(R.id.itemRoomInfo_tv_username);
            iv_solotier = itemView.findViewById(R.id.itemRoomInfo_iv_solotier);
            iv_freetier = itemView.findViewById(R.id.itemRoomInfo_iv_freetier);
        }
    }

    public DialogRoomInfoAdapter(List<UserModel> data, Context context) {
        this.data = data;
        this.context = context;

        tiers = new HashMap<>();
        tiers.put("UNRANKED", R.drawable.tier_unranked);
        tiers.put("IRON", R.drawable.tier_iron);
        tiers.put("BRONZE", R.drawable.tier_bronze);
        tiers.put("SILVER", R.drawable.tier_silver);
        tiers.put("GOLD", R.drawable.tier_gold);
        tiers.put("PLATINUM", R.drawable.tier_platinum);
        tiers.put("DIAMOND", R.drawable.tier_diamond);
        tiers.put("MASTER", R.drawable.tier_master);
        tiers.put("GRANDMASTER", R.drawable.tier_grandmaster);
        tiers.put("CHALLENGER", R.drawable.tier_challenger);
    }

    @NonNull
    @Override
    public DialogRoomInfoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roominfo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DialogRoomInfoAdapter.ViewHolder holder, int position) {
        //usericon binding
        Glide.with(context)
                .load(data.get(position).profileIconURL)
                .apply(new RequestOptions().circleCrop())
                .into(holder.iv_usericon);
        //username binding
        String email = data.get(position).email;
        email = email.substring(0, email.indexOf("@"));
        String text = email + "\n(" + data.get(position).nickname + ")";
        holder.tv_username.setText(text);
        //solotier binding
        String db_solotier = data.get(position).solo_tier;
        String solotier;
        if(db_solotier.length() == 1) {
            solotier = "UNRANKED";
        } else {
            int end = db_solotier.indexOf(" ");
            solotier = db_solotier.substring(0, end);
        }
        holder.iv_solotier.setImageResource(tiers.get(solotier));
        //freetier binding
        String db_freetier = data.get(position).free_tier;
        String freetier;
        if(db_freetier.length() == 1) {
            freetier = "UNRANKED";
        } else {
            int end = db_freetier.indexOf(" ");
            freetier = db_freetier.substring(0, end);
        }
        holder.iv_freetier.setImageResource(tiers.get(freetier));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
