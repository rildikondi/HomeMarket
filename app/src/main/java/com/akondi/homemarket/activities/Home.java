package com.akondi.homemarket.activities;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akondi.homemarket.R;
import com.akondi.homemarket.adapter.MyPagerAdapter;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.custom.FixedSpeedScroller;
import com.akondi.homemarket.databases.Database;
import com.akondi.homemarket.interfaces.ItemClickListener;
import com.akondi.homemarket.interfaces.OnPageClickListener;
import com.akondi.homemarket.model.Banner;
import com.akondi.homemarket.model.Category;
import com.akondi.homemarket.model.Token;
import com.akondi.homemarket.tasks.IdleTimeListener;
import com.akondi.homemarket.tasks.IdleTimer;
import com.akondi.homemarket.viewholder.MenuViewHolder;
import com.andremion.counterfab.CounterFab;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.*;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.squareup.picasso.Picasso;

import io.paperdb.Paper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;
    private NavigationView navigationView;

    FirebaseDatabase database;
    DatabaseReference category;
    FirebaseRecyclerAdapter mFirebaseAdapter;
    TextView txtFullName, textCartItemCount;
    CounterFab fab;
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;
    SwipeRefreshLayout swipeRefreshLayout;

    Intent foodListIntent;

    //Slider
    List<Banner> foodList;
    ViewPager viewPager;
    private TextView foodName;
    private LinearLayout dots_layout;
    private ImageView[] dots;

    private MyPagerAdapter pagerAdapter;

    int refreshingItem = -1;
    private boolean hasSlided = false;

    private IdleTimer idleTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupToolbar();
        setupFirebase();
        setupFloatingButton();
        setupDrawerLayout();
        setupNavigationView();
        setupHeader();
        setupSwiping();
        setupRecyclerView();
        setupFirebaseAdapter();
        loadMenu();
        updateToken();
        setupSlider();
        getSliderData();
    }

    private void getSliderData() {
        DatabaseReference banners = database.getReference("Banner");
        banners.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                foodList.clear();
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                    Banner banner = postSnapShot.getValue(Banner.class);
                    foodList.add(banner);
                }
                setupBeginViewPager();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setupBeginViewPager() {
        Animation animation = AnimationUtils.loadAnimation(
                this,
                R.anim.item_animation_fall_down
        );
        animation.setDuration(1150);
        foodName.startAnimation(animation);
        pagerAdapter.notifyDataSetChanged();
        createDots(viewPager.getCurrentItem());
        foodName.setText(foodList.get(viewPager.getCurrentItem()).getName());
        foodName.setVisibility(View.VISIBLE);
    }

    private void setupSlider() {
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        dots_layout = (LinearLayout) findViewById(R.id.dotsLayout);
        foodName = (TextView) findViewById(R.id.foodName);
        foodList = new ArrayList<>();

        pagerAdapter = new MyPagerAdapter(foodList, this, new OnPageClickListener() {
            @Override
            public void onClick(int position) {
                restartIdleTimer();
                Intent intent = new Intent(Home.this, FoodDetails.class);
                intent.putExtra("FoodId", foodList.get(position).getId());
                startActivity(intent);
            }
        });

        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                if (v > 0 && v < 1) {
                    foodName.setVisibility(View.GONE);
                    hasSlided = true;
                    refreshingItem = -1;

                } else {
                    Animation animation = AnimationUtils.loadAnimation(
                            Home.this,
                            R.anim.slide_up
                    );
                    if (refreshingItem != i && hasSlided) {
                        foodName.setVisibility(View.VISIBLE);
                        foodName.startAnimation(animation);
                    }
                }
            }

            @Override
            public void onPageSelected(int i) {
                createDots(i);
                checkPosition(i);
                foodName.setText(foodList.get(i).getName());
                foodName.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

//        try {
//            Field mScroller;
//            Interpolator sInterpolator = new AccelerateInterpolator();
//            mScroller = ViewPager.class.getDeclaredField("mScroller");
//            mScroller.setAccessible(true);
//            FixedSpeedScroller scroller = new FixedSpeedScroller(viewPager.getContext(), sInterpolator);
//            mScroller.set(viewPager, scroller);
//        } catch (NoSuchFieldException e) {
//        } catch (IllegalArgumentException e) {
//        } catch (IllegalAccessException e) {
//        }

        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                viewPager.getContext(),
                R.anim.layout_fall_down
        );
        viewPager.setLayoutAnimation(controller);
    }

    private void createDots(int current_position) {
        if (dots_layout != null)
            dots_layout.removeAllViews();

        dots = new ImageView[foodList.size()];

        for (int i = 0; i < foodList.size(); i++) {
            dots[i] = new ImageView(this);
            if (i == current_position)
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.active_dot));
            else
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.default_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 0, 4, 0);

            dots_layout.addView(dots[i], params);
        }

    }

    private void checkPosition(int i) {

    }

    @Override
    public void onStart() {
        super.onStart();
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.startListening();
        }
        restartIdleTimer();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopIdleTimer();
    }

    private void stopIdleTimer() {
        if (idleTimer != null) {
            idleTimer.interrupt();
        }
    }

    private void restartIdleTimer() {
        stopIdleTimer();
        idleTimer = new IdleTimer(5, new IdleTimeListener() {
            @Override
            public void onTimePassed() {

                goToNextSlide();
                restartIdleTimer();
            }
        });
        idleTimer.start();
    }


    private void goToNextSlide() {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable(){
            @Override
            public void run() {
                Animation fade_in = AnimationUtils.loadAnimation(
                        Home.this,
                        R.anim.fade_in
                );

                Animation fade_out = AnimationUtils.loadAnimation(
                        Home.this,
                        R.anim.fade_out
                );
                viewPager.setAnimation(fade_out);
                pagerAdapter.notifyDataSetChanged();


                int position = viewPager.getCurrentItem();
                if ( position < pagerAdapter.getCount()-1) {
                    viewPager.setCurrentItem(position + 1);

                    viewPager.setAnimation(fade_in);
                    pagerAdapter.notifyDataSetChanged();

                }
                else  {
                    viewPager.setCurrentItem(0);
                    viewPager.setAnimation(fade_in);
                    pagerAdapter.notifyDataSetChanged();
                }

            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mFirebaseAdapter != null) mFirebaseAdapter.stopListening();
        stopIdleTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.startListening();
        }
        fab.setCount(new Database(this).getCountCart());
        setupBadge();
    }

    private void setupFirebaseAdapter() {
        FirebaseRecyclerOptions<Category> options
                = new FirebaseRecyclerOptions.Builder<Category>()
                .setQuery(category, Category.class)
                .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(options) {
            @NonNull
            @Override
            public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.menu_item, viewGroup, false);

                return new MenuViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull MenuViewHolder holder, int position, @NonNull Category model) {
                holder.txtMenuName.setText(model.getName());
                Picasso.get()
                        .load(model.getImage())
                        .resize(400, 400)
                        .centerCrop()
                        .into(holder.imageView);


                final Category clickItem = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        goToFoodList(position);
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
                refreshingItem = viewPager.getCurrentItem();
                setupBeginViewPager();
                viewPager.scheduleLayoutAnimation();
                loadMenu();
            }
        });
        //Default for the first time
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                //setupBeginViewPager();
                loadMenu();
            }
        });
    }

    private void updateToken() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(Home.this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                updateTokenToFirebase(newToken);
            }
        });
    }

    private void updateTokenToFirebase(String s) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");
        Token token = new Token(s, false);
        tokens.child(Common.currentUser.getPhone()).setValue(token);
    }

    private void setupListenOrderService() {
        // Logic changed will be not used
        // Register Service
        //Intent service = new Intent(Home.this, ListenOrder.class);
        //startService(service);
    }

    private void setupRecyclerView() {
        recycler_menu = (RecyclerView) findViewById(R.id.recycler_menu);
//        recycler_menu.setHasFixedSize(true);
//        layoutManager = new LinearLayoutManager(this);
//        recycler_menu.setLayoutManager(layoutManager);
        recycler_menu.setLayoutManager(new GridLayoutManager(this, 2));
        LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(
                recycler_menu.getContext(),
                R.anim.layout_fall_down
        );
        recycler_menu.setLayoutAnimation(controller);
    }

    private void setupHeader() {
        //set Name for User
        View headerView = navigationView.getHeaderView(0);
        txtFullName = headerView.findViewById(R.id.txtFullName);
        txtFullName.setText(Common.currentUser.getName());
    }

    private void setupNavigationView() {
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupDrawerLayout() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupFloatingButton() {
        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab = (CounterFab) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                Intent cartIntent = new Intent(Home.this, Cart.class);
                startActivity(cartIntent);
            }
        });
        fab.setCount(new Database(this).getCountCart());
    }

    private void setupFirebase() {
        //init Firebase
        database = FirebaseDatabase.getInstance();
        category = database.getReference("Category");
        Paper.init(this);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Menu");
        setSupportActionBar(toolbar);
    }

    private void loadMenu() {
        if (Common.isConnectedToInternet(Home.this)) {
            //new DownloadFilesTask().doInBackground();
            buildCategoryItemAdapter();
        } else {
            Toast.makeText(this, "Please check your connection!", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildCategoryItemAdapter() {
        mFirebaseAdapter.notifyDataSetChanged();
        recycler_menu.setAdapter(mFirebaseAdapter);
        swipeRefreshLayout.setRefreshing(false);

        //Animation
        recycler_menu.getAdapter().notifyDataSetChanged();
        recycler_menu.scheduleLayoutAnimation();

        mFirebaseAdapter.startListening();
    }

    private class DownloadFilesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            buildCategoryItemAdapter();
            return null;
        }
    }

    private void goToFoodList(int position) {
        foodListIntent = new Intent(Home.this, FoodList.class);
        //new GettingIdTask(position).execute();
        foodListIntent.putExtra("CategoryId", mFirebaseAdapter.getRef(position).getKey());
        startActivity(foodListIntent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);

        final MenuItem menuItem = menu.findItem(R.id.toolbar_cart);

        View actionView = menuItem.getActionView();
        textCartItemCount = (TextView) actionView.findViewById(R.id.cart_badge);

        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(menuItem);
            }
        });

        setupBadge();

        return true;
    }

    private void setupBadge() {

        if (textCartItemCount != null) {
            if (new Database(this).getCountCart() == 0) {
                if (textCartItemCount.getVisibility() != View.GONE) {
                    textCartItemCount.setVisibility(View.GONE);
                }
            } else {
                textCartItemCount.setText(String.valueOf(new Database(this).getCountCart()));
                if (textCartItemCount.getVisibility() != View.VISIBLE) {
                    textCartItemCount.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

//        if (id == R.id.refresh) {
//            loadMenu();
//            mFirebaseAdapter.startListening();
//        }

        if (id == R.id.toolbar_cart) {
            goToCart();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_menu) {
            // Handle the camera action
        } else if (id == R.id.nav_home_address) {
            showHomeAddressDialog();
        } else if (id == R.id.nav_cart) {
            goToCart();
        } else if (id == R.id.nav_orders) {
            goToOrders();
        } else if (id == R.id.nav_log_out) {
            logout();
        } else if (id == R.id.nav_change_pwd) {
            showChangePasswordDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showHomeAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle("Change Home Address");
        builder.setMessage("Please fill your home address");

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.home_address_layout, null);

        builder.setView(view);
        builder.setIcon(R.drawable.ic_home_black_24dp);

        final EditText edtHomeAddress = (EditText) view.findViewById(R.id.edtHomeAddress);

        builder.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                Common.currentUser.setHomeAddress(edtHomeAddress.getText().toString());

                FirebaseDatabase.getInstance().getReference("User")
                        .child(Common.currentUser.getPhone())
                        .setValue(Common.currentUser)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(Home.this, "Home Address updated successfully", Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle("Change Password");
        builder.setMessage("Please fill all the information");

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.change_password_layout, null);

        builder.setView(view);
        builder.setIcon(R.drawable.ic_security_black_24dp);

        final EditText edtPassword = (EditText) view.findViewById(R.id.edtPassword);
        final EditText edtNewPassword = (EditText) view.findViewById(R.id.edtNewPassword);
        final EditText edtRepeatPassword = (EditText) view.findViewById(R.id.edtRepeatPassword);

        builder.setPositiveButton("CHANGE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                final ProgressDialog progressDialog = new ProgressDialog(Home.this);
                progressDialog.show();

                if (edtPassword.getText().toString().equals(Common.currentUser.getPassword())) {
                    //Check new password and repeat password
                    if (edtNewPassword.getText().toString().equals(edtRepeatPassword.getText().toString())) {
                        Map<String, Object> passwordUpdate = new HashMap<>();
                        passwordUpdate.put("password", edtNewPassword.getText().toString());

                        //Make update
                        DatabaseReference user = FirebaseDatabase.getInstance().getReference("User");
                        user.child(Common.currentUser.getPhone())
                                .updateChildren(passwordUpdate)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        progressDialog.dismiss();
                                        Toast.makeText(Home.this, "Password was updated !", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(Home.this, "Password failed to change ! " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(Home.this, "New Password doesn't match !", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(Home.this, "Wrong old password", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void logout() {
        Paper.book().destroy();
        Intent signIn = new Intent(Home.this, SignIn.class);
        signIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(signIn);
    }

    private void goToOrders() {
        Intent orderStatusIntent = new Intent(this, OrderStatus.class);
        startActivity(orderStatusIntent);
    }

    private void goToCart() {
        Intent cartIntent = new Intent(this, Cart.class);
        startActivity(cartIntent);
    }

    private class GettingIdTask extends AsyncTask<Void, Void, Void> {

        private int position;
        private String CategoryId;

        public GettingIdTask(int position) {
            this.position = position;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            CategoryId = mFirebaseAdapter.getRef(position).getKey();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            foodListIntent.putExtra("CategoryId", CategoryId);
            startActivity(foodListIntent);
        }
    }
}
