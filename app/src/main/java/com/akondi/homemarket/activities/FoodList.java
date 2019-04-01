package com.akondi.homemarket.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.databases.Database;
import com.akondi.homemarket.interfaces.ItemClickListener;
import com.akondi.homemarket.model.Food;
import com.akondi.homemarket.model.Order;
import com.akondi.homemarket.viewholder.FoodViewHolder;
import com.facebook.CallbackManager;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;

public class FoodList extends AppCompatActivity {

    private static final String TAG = "FoodList";
    private RecyclerView recycler_food;
    private RecyclerView.LayoutManager layoutManager;

    private FirebaseDatabase database;
    private FirebaseRecyclerAdapter mFirebaseAdapter;
    private DatabaseReference foodList;

    private String categoryId;

    //Favorites
    Database localDB;

    //Search Functionality
    private FirebaseRecyclerAdapter searchAdapter;
    private List<String> suggestList = new ArrayList<>();
    private SearchView searchView;

    //Facebook Share
    CallbackManager callbackManager;
    ShareDialog shareDialog;

    private SwipeRefreshLayout swipeRefreshLayout;

    //Create Target from Picasso
    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            //Create Photo from Bitmap
            SharePhoto photo = new SharePhoto.Builder()
                    .setBitmap(bitmap)
                    .build();
            if (ShareDialog.canShow(SharePhotoContent.class)) {
                SharePhotoContent content = new SharePhotoContent.Builder()
                        .addPhoto(photo)
                        .build();
                shareDialog.show(content);
            }
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        setupFirebase();
        setupRecyclerView();
        getCategoryId();
        setupFirebaseAdapter();
        loadListFood();
        setupSwiping();
        setupSearchBar();
        initLocalDatabase();
        initFacebook();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mFirebaseAdapter != null) mFirebaseAdapter.startListening();
        if (searchAdapter != null) searchAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mFirebaseAdapter != null) mFirebaseAdapter.stopListening();
        if (searchAdapter != null) searchAdapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFirebaseAdapter != null) mFirebaseAdapter.startListening();
        if (searchAdapter != null) searchAdapter.startListening();
    }

    private void startAdapter() {

        mFirebaseAdapter.notifyDataSetChanged();
        recycler_food.setAdapter(mFirebaseAdapter);

        //Animation
        recycler_food.getAdapter().notifyDataSetChanged();
        recycler_food.scheduleLayoutAnimation();

        mFirebaseAdapter.startListening();
    }

    private void setupFirebaseAdapter() {
        FirebaseRecyclerOptions<Food> options =
                new FirebaseRecyclerOptions.Builder<Food>()
                        .setQuery(foodList.orderByChild("menuId").equalTo(categoryId), Food.class)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.food_item, viewGroup, false);

                return new FoodViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder holder, final int position, @NonNull final Food model) {
                Typeface face = Typeface.createFromAsset(getAssets(), "fonts/restaurant_font.otf");
                holder.food_name.setTypeface(face);
                holder.food_name.setText(model.getName());
                holder.food_price.setTypeface(face);
                holder.food_price.setText(String.format("$ %s", model.getPrice()));

                Picasso.get().load(model.getImage()).resize(400, 200).into(holder.food_image);

                //Add Favorites
                if (localDB.isFavorite(mFirebaseAdapter.getRef(position).getKey()))
                    holder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);

                //Click to change state of Favorites
                holder.fav_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!localDB.isFavorite(mFirebaseAdapter.getRef(holder.getAdapterPosition()).getKey())) {
                            localDB.addToFavorites(mFirebaseAdapter.getRef(holder.getAdapterPosition()).getKey());
                            holder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, model.getName() + " was added to Favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            localDB.removeFromFavorites(mFirebaseAdapter.getRef(position).getKey());
                            holder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(FoodList.this, model.getName() + " was removed from Favorites", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                holder.btn_share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Picasso.get()
                                .load(model.getImage())
                                .into(target);
                    }
                });

                holder.btn_quick_cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Database(FoodList.this).addToCart(new Order(
                                mFirebaseAdapter.getRef(position).getKey(),
                                model.getName(),
                                "1",
                                model.getPrice(),
                                model.getDiscount(),
                                model.getImage()
                        ));

                        Toast.makeText(FoodList.this, "Added to Cart", Toast.LENGTH_SHORT).show();
                    }
                });

                final Food clickItem = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(FoodList.this, clickItem.getName(), Toast.LENGTH_SHORT).show();
                        Intent foodDetailsIntent = new Intent(FoodList.this, FoodDetails.class);
                        //Send Food Id to FoodDetails Activity
                        foodDetailsIntent.putExtra("FoodId", mFirebaseAdapter.getRef(position).getKey());
                        startActivity(foodDetailsIntent);
                    }
                });
            }
        };
    }

    private void setupSwiping() {
        swipeRefreshLayout = findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark
        );
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadListFood();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        //Default for the first time
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                loadListFood();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initFacebook() {
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
    }

    private void initLocalDatabase() {
        localDB = new Database(this);
    }

    private void setupSearchBar() {
        //Search
        searchView = (SearchView) findViewById(R.id.searchBar);
        //searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Enter your food");

        // Get the search close button image view
        ImageView closeButton = (ImageView) searchView.findViewById(R.id.search_close_btn);

        // Set on click listener
        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //recycler_food.setAdapter(mFirebaseAdapter);

                //Find EditText view
                EditText et = (EditText) findViewById(R.id.search_src_text);

                //Clear the text from EditText view
                et.setText("");

                //Clear query
                searchView.setQuery("", false);
                //Collapse the action view
                searchView.onActionViewCollapsed();
                //Collapse the search widget
                searchView.onActionViewCollapsed();

                loadListFood();
            }
        });

        setupSearchActionListener();
    }

    private void setupSearchActionListener() {

        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSuggest();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: start search called");
                startSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: called");
                if (newText.equals("")) {
                    loadListFood();
                } else {
                    startSearch(newText);
                }
                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                recycler_food.setAdapter(mFirebaseAdapter);
                return true;
            }
        });

//        searchView.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
//            @Override
//            public void onSearchStateChanged(boolean enabled) {
//                //When Search Bar is closed
//                //Restore original adapter
//                if (!enabled)
//                    recycler_food.setAdapter(mFirebaseAdapter);
//            }
//
//            @Override
//            public void onSearchConfirmed(CharSequence text) {
//                //When search finish
//                //Show result on search adapter
//                startSearch(text);
//            }
//
//            @Override
//            public void onButtonClicked(int buttonCode) {
//
//            }
//        });
    }

    private void setupTextChangeListener() {
//        searchView.addTextChangeListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                // When user type their text, we will change suggest list
//                List<String> suggest = new ArrayList<>();
//                for (String search : suggestList) {
//                    if (search.toLowerCase().contains(searchView.getText().toLowerCase()))
//                        suggest.add(search);
//                }
//                searchView.setLastSuggestions(suggest);
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });
    }

    private void getCategoryId() {
        // Get intent from Home
        if (getIntent() != null) {
            categoryId = getIntent().getStringExtra("CategoryId");
        }
    }

    private void setupRecyclerView() {
        recycler_food = (RecyclerView) findViewById(R.id.recycler_food);
        //recycler_food.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_food.setLayoutManager(layoutManager);
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                recycler_food.getContext(),
                R.anim.layout_slide_from_left
        );
        recycler_food.setLayoutAnimation(controller);
    }

    private void setupFirebase() {
        //Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference("Foods");
    }

    private void startSearch(CharSequence text) {
        Query query = foodList.orderByChild("name").startAt(text.toString().toUpperCase());

        FirebaseRecyclerOptions<Food> options =
                new FirebaseRecyclerOptions.Builder<Food>()
                        .setQuery(query, Food.class).build();

        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.food_item, viewGroup, false);

                return new FoodViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder holder, final int position, @NonNull final Food model) {
                Typeface face = Typeface.createFromAsset(getAssets(), "fonts/restaurant_font.otf");
                holder.food_name.setTypeface(face);
                holder.food_name.setText(model.getName());
                holder.food_price.setTypeface(face);
                holder.food_price.setText(String.format("$ %s", model.getPrice()));
                Log.d(TAG, "onBindViewHolder: " + model.getName());
                Picasso.get().load(model.getImage()).resize(400, 200).into(holder.food_image);

                //Add Favorites
                if (localDB.isFavorite(searchAdapter.getRef(position).getKey()))
                    holder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);

                //Click to change state of Favorites
                holder.fav_image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!localDB.isFavorite(searchAdapter.getRef(holder.getAdapterPosition()).getKey())) {
                            localDB.addToFavorites(searchAdapter.getRef(holder.getAdapterPosition()).getKey());
                            holder.fav_image.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, model.getName() + " was added to Favorites", Toast.LENGTH_SHORT).show();
                        } else {
                            localDB.removeFromFavorites(searchAdapter.getRef(position).getKey());
                            holder.fav_image.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(FoodList.this, model.getName() + " was removed from Favorites", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                holder.btn_share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Picasso.get()
                                .load(model.getImage())
                                .into(target);
                    }
                });

                holder.btn_quick_cart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Database(FoodList.this).addToCart(new Order(
                                mFirebaseAdapter.getRef(position).getKey(),
                                model.getName(),
                                "1",
                                model.getPrice(),
                                model.getDiscount(),
                                model.getImage()
                        ));

                            Toast.makeText(FoodList.this, "Added to Cart", Toast.LENGTH_SHORT).show();
                    }
                });

                final Food clickItem = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Toast.makeText(FoodList.this, clickItem.getName(), Toast.LENGTH_SHORT).show();
                        Intent foodDetailsIntent = new Intent(FoodList.this, FoodDetails.class);
                        //Send Food Id to FoodDetails Activity
                        foodDetailsIntent.putExtra("FoodId", searchAdapter.getRef(position).getKey());
                        startActivity(foodDetailsIntent);
                    }
                });
            }
        };

        searchAdapter.notifyDataSetChanged();
        recycler_food.setAdapter(searchAdapter);
        searchAdapter.startListening();
    }

    private void loadSuggest() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
            recycler_food.setAdapter(searchAdapter);
            searchAdapter.startListening();
        }
    }

    private void loadListFood() {
        if (!categoryId.isEmpty()) {
            if (Common.isConnectedToInternet(FoodList.this))
                startAdapter();
            else
                Toast.makeText(FoodList.this, "Please check your internet connection!", Toast.LENGTH_SHORT).show();
        }
    }
}