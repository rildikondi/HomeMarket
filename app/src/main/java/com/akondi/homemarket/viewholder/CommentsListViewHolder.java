package com.akondi.homemarket.viewholder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.akondi.homemarket.R;

public class CommentsListViewHolder extends RecyclerView.ViewHolder {

    public TextView txtUserPhone, txtComment;
    public RatingBar ratingBar;

    public CommentsListViewHolder(@NonNull View itemView) {
        super(itemView);
        txtComment = (TextView) itemView.findViewById(R.id.txtComment);
        txtUserPhone = (TextView) itemView.findViewById(R.id.txtUserPhone);
        ratingBar = (RatingBar) itemView.findViewById(R.id.ratingBar);
    }
}
