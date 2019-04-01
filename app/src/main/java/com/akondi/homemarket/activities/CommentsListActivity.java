package com.akondi.homemarket.activities;

import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DrawableUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.akondi.homemarket.R;
import com.akondi.homemarket.model.Rating;
import com.akondi.homemarket.viewholder.CommentsListViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import static com.akondi.homemarket.common.Common.INTENT_FOOD_ID;

public class CommentsListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference ratingTbl;
    FirebaseRecyclerAdapter<Rating, CommentsListViewHolder> adapter;

    SwipeRefreshLayout swipeRefreshLayout;

    String foodId = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments_list);
        getFoodId();
        initFirebase();
        setupRecyclerView();
        setupSwipeRefreshLayout();
        loadCommentsFirstTime();
    }

    private void loadCommentsFirstTime() {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                loadComments();
            }
        });
    }

    private void loadComments() {
        if (!foodId.isEmpty() && foodId != null) {
            setupFirebaseAdapter();
        }
    }

    private void setupFirebaseAdapter() {
        Query query = ratingTbl.orderByChild("foodId").equalTo(foodId);

        FirebaseRecyclerOptions<Rating> options = new FirebaseRecyclerOptions.Builder<Rating>()
                .setQuery(query, Rating.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Rating, CommentsListViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CommentsListViewHolder holder, int position, @NonNull Rating model) {
                holder.ratingBar.setRating(Float.parseFloat(model.getRateValue()));
                holder.txtComment.setText(model.getComment());
                holder.txtUserPhone.setText(model.getUserPhone());
            }

            @NonNull
            @Override
            public CommentsListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.comment_layout, viewGroup, false);
                return new CommentsListViewHolder(view);
            }
        };
        adapter.startListening();
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void getFoodId() {
        if (getIntent() != null) {
            foodId = getIntent().getStringExtra(INTENT_FOOD_ID);
        }
    }

    private void setupSwipeRefreshLayout() {
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadComments();
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.comments_recycler);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void initFirebase() {
        database = FirebaseDatabase.getInstance();
        ratingTbl = database.getReference("Rating");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
