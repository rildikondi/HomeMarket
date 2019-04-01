package com.akondi.homemarket;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.akondi.homemarket.activities.Home;
import com.akondi.homemarket.activities.SignIn;
import com.akondi.homemarket.activities.SignUp;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.model.User;
import com.facebook.FacebookSdk;
import com.google.firebase.database.*;
import io.paperdb.Paper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity {

    private Context mContext = MainActivity.this;
    private static final String TAG = "MainActivity";
    private Button btnSignIn, btnSignUp;
    private TextView txtSlogan;
    private RelativeLayout rl_main;
    private ProgressDialog mDialog;

    private String phone;
    private String pwd;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findWidgets();
        initFacebook();
        printKeyHash();
    }

    private void initFacebook() {
        FacebookSdk.sdkInitialize(getApplicationContext());
    }

    private void printKeyHash() {
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo info = getPackageManager().getPackageInfo("com.akondi.homemarket",
                        PackageManager.GET_SIGNING_CERTIFICATES);

                SigningInfo signingInfo = info.signingInfo;
                if (signingInfo.hasMultipleSigners()) {
                    for (Signature signature : signingInfo.getApkContentsSigners()) {
                        MessageDigest md = MessageDigest.getInstance("SHA");
                        md.update(signature.toByteArray());
                        Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                    }
                } else {
                    for (Signature signature : signingInfo.getSigningCertificateHistory()) {
                        MessageDigest md = MessageDigest.getInstance("SHA");
                        md.update(signature.toByteArray());
                        Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                    }
                }
            }else {
                PackageInfo info = getPackageManager().getPackageInfo(
                        "com.akondi.homemarket",
                        PackageManager.GET_SIGNATURES);
                for (Signature signature : info.signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void findWidgets() {
        rl_main = findViewById(R.id.rl_main);

        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        txtSlogan = (TextView) findViewById(R.id.txtSlogan);
        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/NABILA.TTF");
        txtSlogan.setTypeface(face);
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIn = new Intent(mContext, SignIn.class);
                startActivity(signIn);

            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUp = new Intent(mContext, SignUp.class);
                startActivity(signUp);
            }
        });

        //Init Paper
        Paper.init(this);
        //Check remember
        phone = Paper.book().read(Common.USER_KEY);
        pwd = Paper.book().read(Common.PWD_KEY);
        if (phone != null && pwd != null) {
            if (!phone.isEmpty() && !pwd.isEmpty()) {
                login();
            }
        }
    }

    private void login() {
        if (Common.isConnectedToInternet(MainActivity.this))
            authenticateUser();
        else
            Toast.makeText(MainActivity.this, "Please check your connection!", Toast.LENGTH_SHORT).show();
    }

    private void authenticateUser() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("User");

        mDialog = new ProgressDialog(MainActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.show();

        table_user.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                analiseData(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void analiseData(DataSnapshot dataSnapshot) {
        mDialog.dismiss();

        //check if the User exists
        if (dataSnapshot.child(phone).exists()) {
            // Get User information
            User user = dataSnapshot.child(phone).getValue(User.class);
            user.setPhone(phone);
            if (user.getPassword().equals(pwd)) {
                Toast.makeText(MainActivity.this, "Sign in successfully.", Toast.LENGTH_SHORT).show();
                Intent homeIntent = new Intent(this, Home.class);
                Common.currentUser = user;
                startActivity(homeIntent);
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Wrong Password!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "User not exists in database!", Toast.LENGTH_SHORT).show();
        }
    }
}
