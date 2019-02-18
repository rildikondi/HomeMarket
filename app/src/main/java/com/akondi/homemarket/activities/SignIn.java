package com.akondi.homemarket.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.model.User;
import com.google.firebase.database.*;
import io.paperdb.Paper;

public class SignIn extends AppCompatActivity {

    private RelativeLayout rl_main;
    private EditText edtPhone, edtPassword;
    private Button btnSignIn;
    private ProgressDialog mDialog;
    private String phone;
    private String Password;
    private com.rey.material.widget.CheckBox checkboxRemember;
    private TextView txtForgotPwd;

    FirebaseDatabase database;
    DatabaseReference table_user;
    ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_sign_in);

        findWidgets();
    }

    private void findWidgets() {
        rl_main = findViewById(R.id.rl_main);
        edtPhone = (EditText) findViewById(R.id.edtPhone);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.isConnectedToInternet(SignIn.this))
                    authenticateUser();
                else
                    Toast.makeText(SignIn.this, "Please check your connection!", Toast.LENGTH_SHORT).show();
            }
        });
        checkboxRemember = findViewById(R.id.checkboxRemember);
        txtForgotPwd = (TextView) findViewById(R.id.txtForgotPwd);
        txtForgotPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPwdDialog();
            }
        });
        Paper.init(this);
    }

    private void showForgotPwdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle("Forgot Password");
        builder.setMessage("Enter your secure code");

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.forgot_password_layout, null);

        builder.setView(view);
        builder.setIcon(R.drawable.ic_security_black_24dp);

        final EditText edtPhone = (EditText) view.findViewById(R.id.edtPhone);
        final EditText edtSecureCode = (EditText) view.findViewById(R.id.edtSecureCode);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Check if user available
                database = FirebaseDatabase.getInstance();
                table_user = database.getReference("User");
                table_user.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.child(edtPhone.getText().toString())
                                .getValue(User.class);

                        if (user.getSecureCode().equals(edtSecureCode.getText().toString()))
                            Toast.makeText(SignIn.this, "Your password : " + user.getPassword(), Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(SignIn.this, "Wrong secure code !", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

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

    private void authenticateUser() {
        database = FirebaseDatabase.getInstance();
        table_user = database.getReference("User");

        phone = edtPhone.getText().toString();
        Password = edtPassword.getText().toString();

        mDialog = new ProgressDialog(SignIn.this);
        mDialog.setMessage("Please wait...");
        mDialog.show();

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                analiseData(dataSnapshot, valueEventListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        table_user.addListenerForSingleValueEvent(valueEventListener);
    }

    private void analiseData(DataSnapshot dataSnapshot, ValueEventListener valueEventListener) {
        mDialog.dismiss();

        //check if the User exists
        if (dataSnapshot.child(phone).exists()) {
            // Get User information
            User user = dataSnapshot.child(phone).getValue(User.class);
            user.setPhone(phone);
            if (user.getPassword().equals(Password)) {
                Toast.makeText(SignIn.this, "Sign in successfully.", Toast.LENGTH_SHORT).show();
                Intent homeIntent = new Intent(this, Home.class);
                Common.currentUser = user;
                if (checkboxRemember.isChecked()) {
                    Paper.book().write(Common.USER_KEY, phone);
                    Paper.book().write(Common.PWD_KEY, Password);
                }
                startActivity(homeIntent);
                finish();
                table_user.removeEventListener(valueEventListener);
            } else {
                Toast.makeText(SignIn.this, "Wrong Password!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(SignIn.this, "User not exists in database!", Toast.LENGTH_SHORT).show();
        }
    }
}
