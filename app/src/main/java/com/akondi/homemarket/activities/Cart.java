package com.akondi.homemarket.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.common.Config;
import com.akondi.homemarket.databases.Database;
import com.akondi.homemarket.model.*;
import com.akondi.homemarket.remote.APIService;
import com.akondi.homemarket.viewholder.CartAdapter;
import com.google.firebase.database.*;
import com.paypal.android.sdk.payments.*;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Cart extends AppCompatActivity {

    private static final int PAYPAL_REQUEST_CODE = 9999;
    private Context mContext = Cart.this;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;

    private FirebaseDatabase database;
    private DatabaseReference requests;

    private TextView txtTotalPrice;
    private Button btn_place;

    private List<Order> cart = new ArrayList<>();
    private CartAdapter cartAdapter;

    private APIService mService;

    //PayPal payment
    static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX) // using SANDBOX for testing change to real for release
            .clientId(Config.PAYPAL_CLIENT_ID);

    String address, comment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        setupFirebase();
        initViews();
        loadListFood();
        initService();
        initPayPal();
    }

    private void initPayPal() {
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(intent);
    }

    private void initService() {
        mService = Common.getFCMService();
    }

    private void setupFirebase() {
        //Firebase
        database = FirebaseDatabase.getInstance();
        requests = database.getReference("Requests");
    }

    private void initViews() {
        //init
        recyclerView = (RecyclerView) findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        txtTotalPrice = findViewById(R.id.total);
        btn_place = findViewById(R.id.btnPlaceOrder);

        btn_place.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!txtTotalPrice.getText().toString().equals("$0.00"))
                    showAlertDialog();
                else
                    Toast.makeText(mContext, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext, R.style.MyDialogTheme);
        alertDialog.setTitle("One more step!");
        alertDialog.setMessage("Enter your address: ");

        LayoutInflater inflater = this.getLayoutInflater();
        View order_address_comment = inflater.inflate(R.layout.order_address_comment, null);

        final EditText edtAddress = (EditText) order_address_comment.findViewById(R.id.edtAddress);
        final EditText edtComment = (EditText) order_address_comment.findViewById(R.id.edtComment);

        alertDialog.setView(order_address_comment);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Show PayPal to payment

                address = edtAddress.getText().toString();
                comment = edtComment.getText().toString();

                String formatAmount = txtTotalPrice.getText().toString()
                        .replace("$", "")
                        .replace(",", "");

                setupPayPalPayment(formatAmount);
            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void setupPayPalPayment(String formatAmount) {
        PayPalPayment payPalPayment = new PayPalPayment(new BigDecimal(formatAmount),
                "EUR",
                "HomeMarket App Order",
                PayPalPayment.PAYMENT_INTENT_SALE);
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment);
        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                PaymentConfirmation confirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirmation != null) {
                    try {
                        String paymentDetail = confirmation.toJSONObject().toString(4);
                        JSONObject jsonObject = new JSONObject(paymentDetail);
                        String paymentStatus = jsonObject.getJSONObject("response").getString("state");
                        sendRequestToFirebase(address, comment, paymentStatus);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(mContext, "Payment canceled !", Toast.LENGTH_SHORT).show();
            } else if (requestCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Toast.makeText(mContext, "Invalid Payment !", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendRequestToFirebase(String address, String comment, String paymentStatus) {
        // Create new Request
        Request request = new Request(
                Common.currentUser.getPhone(),
                Common.currentUser.getName(),
                address,
                txtTotalPrice.getText().toString(),
                "0",
                comment,
                paymentStatus,
                cart
        );
        //Submit to Firebase
        //We will use System.CurrentMilli for key
        String order_number = String.valueOf(System.currentTimeMillis());
        requests.child(order_number)
                .setValue(request);
        //Delete cart from locale database
        new Database(mContext).cleanCart();
        sendNotificationOrder(order_number);
    }

    private void sendNotificationOrder(final String order_number) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");
        //Get all nodes with isServerToken == true
        Query data = tokens.orderByChild("isServerToken").equalTo(true);
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                    Token serverToken = postSnapShot.getValue(Token.class);

                    //Create raw payload to send
                    Notification notification = new Notification("AK Dev", "You have new order: " + order_number);
                    Sender content = new Sender(serverToken.getToken(), notification);

                    mService.sendNotification(content)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200) {
                                        if (response.body().success == 1) {
                                            Toast.makeText(mContext, "Thank you, Order Placed!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(mContext, "Failed !", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {
                                    Log.d("Error", t.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadListFood() {
        cart = new Database(this).getCarts();
        cartAdapter = new CartAdapter(cart, this);
        cartAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(cartAdapter);

        //calculate total price;
        int total = 0;
        for (Order order : cart) {
            int price = (Integer.parseInt(order.getPrice()));
            int quantity = (Integer.parseInt(order.getQuantity()));
            total += price * quantity;
        }
        Locale locale = new Locale("en", "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
        txtTotalPrice.setText(fmt.format(total));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());
        return true;
    }

    private void deleteCart(int position) {
        // We will remove item at List<Order> by position
        cart.remove(position);
        //After that, we will delete all old data from SQLite
        new Database(this).cleanCart();
        //And final, we will update new data from List<Order> to SQLite
        for (Order item : cart)
            new Database(this).addToCart(item);
        //Refresh
        loadListFood();
    }
}
