package com.akondi.homemarket.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.interfaces.ItemClickListener;
import com.akondi.homemarket.model.Category;
import com.akondi.homemarket.model.Token;
import com.akondi.homemarket.viewholder.MenuViewHolder;
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
import java.util.HashMap;
import java.util.Map;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;
    private NavigationView navigationView;

    FirebaseDatabase database;
    DatabaseReference category;
    FirebaseRecyclerAdapter mFirebaseAdapter;
    TextView txtFullName;
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;
    SwipeRefreshLayout swipeRefreshLayout;

    Intent foodListIntent;

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
        loadMenu();
        updateToken();
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
                loadMenu();
            }
        });
        //Default for the first time
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
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
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_menu.setLayoutManager(layoutManager);
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
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                Intent cartIntent = new Intent(Home.this, Cart.class);
                startActivity(cartIntent);
            }
        });
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
            new DownloadFilesTask().doInBackground();
        } else {
            Toast.makeText(this, "Please check your connection!", Toast.LENGTH_SHORT).show();
        }
    }

    private void buildCategoryItemAdapter() {

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
                        .resize(400, 200)
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

        mFirebaseAdapter.notifyDataSetChanged();
        recycler_menu.setAdapter(mFirebaseAdapter);
        mFirebaseAdapter.startListening();
        swipeRefreshLayout.setRefreshing(false);
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
    public void onStart() {
        super.onStart();
        if (mFirebaseAdapter != null) mFirebaseAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mFirebaseAdapter != null) mFirebaseAdapter.stopListening();
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            loadMenu();
            mFirebaseAdapter.startListening();
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
