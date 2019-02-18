package com.akondi.homemarket.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.databases.Database;
import com.akondi.homemarket.model.Food;
import com.akondi.homemarket.model.Order;
import com.akondi.homemarket.model.Rating;
import com.akondi.homemarket.model.User;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;
import com.stepstone.apprating.AppRatingDialog;
import com.stepstone.apprating.listener.RatingDialogListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class FoodDetails extends AppCompatActivity {

    private TextView food_name, food_price, food_description;
    private ImageView food_image;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private FloatingActionButton btnCart, btnRating;
    private ElegantNumberButton numberButton;
    private RatingBar ratingBar;

    private FirebaseDatabase database;
    private DatabaseReference foods;
    private DatabaseReference ratingTbl;

    private Food currentFood;
    private String foodId = "";
    private TextView ratingDescription;

    private int starClicked = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_details);

        setupFirebase();
        initViews();
        getFoodId();
    }

    private void getFoodId() {
        //Get Food Id form Intent
        if (getIntent() != null)
            foodId = getIntent().getStringExtra("FoodId");
        if (!foodId.isEmpty()) {
            if (Common.isConnectedToInternet(FoodDetails.this)) {
                getDetailsFood(foodId);
                getRatingFood(foodId);
            } else
                Toast.makeText(this, "Please check internet connection!", Toast.LENGTH_SHORT).show();
        }
    }

    private void getRatingFood(String foodId) {
        Query foodRating = ratingTbl.orderByChild("foodId").equalTo(foodId);
        foodRating.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0, sum = 0;
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Rating item = postSnapshot.getValue(Rating.class);
                    sum += Integer.parseInt(item.getRateValue());
                    count++;
                }
                if (count != 0) {
                    int average = sum / count;
                    ratingBar.setRating(average);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initViews() {
        // init views
        numberButton = (ElegantNumberButton) findViewById(R.id.number_button);
        btnCart = (FloatingActionButton) findViewById(R.id.btnCart);
        btnCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Database(FoodDetails.this).addToCart(new Order(
                        foodId,
                        currentFood.getName(),
                        numberButton.getNumber(),
                        currentFood.getPrice(),
                        currentFood.getDiscount()
                ));

                Toast.makeText(FoodDetails.this, "Added to Cart", Toast.LENGTH_SHORT).show();
            }
        });
        btnRating = (FloatingActionButton) findViewById(R.id.btn_rating);
        btnRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRatingDialog();
            }
        });
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);

        food_description = (TextView) findViewById(R.id.food_description);
        food_name = (TextView) findViewById(R.id.food_name);
        food_price = (TextView) findViewById(R.id.food_price);
        food_image = (ImageView) findViewById(R.id.img_food);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing);
        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.ExpandedAppbar);
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.CollapsedAppbar);
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rating, null);
        builder.setView(view);

        setupDialogWidgets(view);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                uploadRatingToFirebase();
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void uploadRatingToFirebase() {
        // Get rating and upload to Firebase
        final Rating rating = new Rating(
                Common.currentUser.getPhone(),
                foodId,
                String.valueOf(starClicked),
                ratingDescription.getText().toString()
        );
        ratingTbl.child(Common.currentUser.getPhone()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(Common.currentUser.getPhone()).exists()) {
                    //remove old value
                    ratingTbl.child(Common.currentUser.getPhone()).removeValue();
                    //update new value
                    ratingTbl.child(Common.currentUser.getPhone()).setValue(rating);
                } else {
                    //Update new value
                    ratingTbl.child(Common.currentUser.getPhone()).setValue(rating);
                }
                Toast.makeText(FoodDetails.this, "Thank you for your rating !!!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupDialogWidgets(View view) {
        final EditText userReview = (EditText) view.findViewById(R.id.userReview);
        final ImageView star1 = (ImageView) view.findViewById(R.id.star1);
        final ImageView star2 = (ImageView) view.findViewById(R.id.star2);
        final ImageView star3 = (ImageView) view.findViewById(R.id.star3);
        final ImageView star4 = (ImageView) view.findViewById(R.id.star4);
        final ImageView star5 = (ImageView) view.findViewById(R.id.star5);
        ratingDescription = (TextView) view.findViewById(R.id.ratingDescription);

        star1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                starClicked = 1;
                ratingDescription.setText("Very Bad");
                star1.setImageResource(R.drawable.ic_star_green_24dp);
                star2.setImageResource(R.drawable.ic_star_border_black_24dp);
                star3.setImageResource(R.drawable.ic_star_border_black_24dp);
                star4.setImageResource(R.drawable.ic_star_border_black_24dp);
                star5.setImageResource(R.drawable.ic_star_border_black_24dp);

            }
        });

        star2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                starClicked = 2;
                ratingDescription.setText("Not Good");
                star1.setImageResource(R.drawable.ic_star_green_24dp);
                star2.setImageResource(R.drawable.ic_star_green_24dp);
                star3.setImageResource(R.drawable.ic_star_border_black_24dp);
                star4.setImageResource(R.drawable.ic_star_border_black_24dp);
                star5.setImageResource(R.drawable.ic_star_border_black_24dp);

            }
        });

        star3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                starClicked = 3;
                ratingDescription.setText("Quite Ok");
                star1.setImageResource(R.drawable.ic_star_green_24dp);
                star2.setImageResource(R.drawable.ic_star_green_24dp);
                star3.setImageResource(R.drawable.ic_star_green_24dp);
                star4.setImageResource(R.drawable.ic_star_border_black_24dp);
                star5.setImageResource(R.drawable.ic_star_border_black_24dp);

            }
        });

        star4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                starClicked = 4;
                ratingDescription.setText("Very Good");
                star1.setImageResource(R.drawable.ic_star_green_24dp);
                star2.setImageResource(R.drawable.ic_star_green_24dp);
                star3.setImageResource(R.drawable.ic_star_green_24dp);
                star4.setImageResource(R.drawable.ic_star_green_24dp);
                star5.setImageResource(R.drawable.ic_star_border_black_24dp);

            }
        });

        star5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                starClicked = 5;
                ratingDescription.setText("Excellent");
                star1.setImageResource(R.drawable.ic_star_green_24dp);
                star2.setImageResource(R.drawable.ic_star_green_24dp);
                star3.setImageResource(R.drawable.ic_star_green_24dp);
                star4.setImageResource(R.drawable.ic_star_green_24dp);
                star5.setImageResource(R.drawable.ic_star_green_24dp);
            }
        });

    }

    private void setupFirebase() {
        database = FirebaseDatabase.getInstance();
        foods = database.getReference("Foods");
        ratingTbl = database.getReference("Rating");
    }

    private void getDetailsFood(String foodId) {
        foods.child(foodId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentFood = dataSnapshot.getValue(Food.class);
                Picasso.get().load(currentFood.getImage()).into(food_image);
                collapsingToolbarLayout.setTitle(currentFood.getName());
                food_price.setText(currentFood.getPrice());
                food_name.setText(currentFood.getName());
                food_description.setText(currentFood.getDescription());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
