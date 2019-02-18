package com.akondi.homemarket.activities;

import android.app.ProgressDialog;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.*;

public class SignUp extends AppCompatActivity {

    private RelativeLayout rl_main;
    private EditText edtPhone, edtName, edtPassword, edtSecureCode;
    private String phone, Name, Password, secureCode;
    private Button btnSignUp;
    private ProgressDialog mDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        findWidgets();
    }

    private void findWidgets() {
        rl_main = findViewById(R.id.rl_main);
        edtPhone = (EditText) findViewById(R.id.edtPhone);
        edtName = (EditText) findViewById(R.id.edtName);
        edtPassword = (EditText) findViewById(R.id.edtPassword);
        edtSecureCode = (EditText) findViewById(R.id.edtSecureCode);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Common.isConnectedToInternet(SignUp.this))
                    registerUser();
                else
                    Toast.makeText(SignUp.this, "Please check your connection!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference table_user = database.getReference("User");

        phone = edtPhone.getText().toString();
        Name = edtName.getText().toString();
        Password = edtPassword.getText().toString();
        secureCode = edtSecureCode.getText().toString();

        mDialog = new ProgressDialog(SignUp.this);
        mDialog.setMessage("Please wait...");
        mDialog.show();

        table_user.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                analiseData(dataSnapshot, table_user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void analiseData(DataSnapshot dataSnapshot, DatabaseReference table_user) {
        mDialog.dismiss();

        //check if the User exists
        if (dataSnapshot.child(phone).exists()) {
            Toast.makeText(SignUp.this, "User already exists in database!", Toast.LENGTH_SHORT).show();
        } else {

            // Get User information
            User user = new User(Name, Password, secureCode);
            table_user.child(phone).setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Toast.makeText(SignUp.this, "User successfully registered!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(SignUp.this, "Registration failed!" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
