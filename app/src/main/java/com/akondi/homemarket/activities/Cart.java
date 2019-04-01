package com.akondi.homemarket.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.common.Config;
import com.akondi.homemarket.databases.Database;
import com.akondi.homemarket.interfaces.OnQuantityChangedListener;
import com.akondi.homemarket.model.*;
import com.akondi.homemarket.remote.APIService;
import com.akondi.homemarket.remote.IGoogleService;
import com.akondi.homemarket.viewholder.CartAdapter;
import com.facebook.common.logging.LoggingDelegate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.dynamic.IFragmentWrapper;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.*;
import com.paypal.android.sdk.payments.*;

import org.json.JSONArray;
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

public class Cart extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

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

    Place shippingAddress;

    //Location
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    private static final int UPDATE_INTERVAL = 5000;
    private static final int FASTEST_INTERVAL = 3000;
    private static final int DISPLACEMENT = 10;
    private static final int LOCATION_REQUEST_CODE = 9999;
    private static final int PLAY_SERVICES_REQEST = 9997;

    IGoogleService mGoogleMapService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        checkLocationPermission();
        initGoogleMapService();
        setupFirebase();
        initViews();
        loadListFood();
        initService();
        initPayPal();
    }

    private void initGoogleMapService() {
        mGoogleMapService = Common.getGoogleMapApi();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, LOCATION_REQUEST_CODE);
        } else {
            //If have play services on device
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
            }
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //If have play services on device
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                    }
                }
            }
            break;
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)) {
                GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, PLAY_SERVICES_REQEST).show();
            } else {
                Toast.makeText(mContext, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
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
//        final PlaceAutocompleteFragment edtAddress = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
//        //Hide search icon before fragment
//        edtAddress.getView().findViewById(R.id.place_autocomplete_search_button).setVisibility(View.GONE);
//        ((EditText) edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
//                .setHint("Enter your address");
//        ((EditText) edtAddress.getView().findViewById(R.id.place_autocomplete_search_input))
//                .setTextSize(14);
//        //Get address from Place Autocomplete
//        edtAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(Place place) {
//                shippingAddress = place;
//            }
//
//            @Override
//            public void onError(Status status) {
//                Log.e("ERROR", "onError: " + status.getStatusMessage());
//            }
//        });

        final EditText edtComment = (EditText) order_address_comment.findViewById(R.id.edtComment);
        final RadioButton rbtnShipToAddress = (RadioButton) order_address_comment.findViewById(R.id.rbtnShipToAddress);
        final RadioButton rbtnHomeAddress = (RadioButton) order_address_comment.findViewById(R.id.rbtnHomeAddress);

        rbtnShipToAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mGoogleMapService.getAddressName(
                            String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&sensor=false&key=%s",
                                    lastLocation.getLatitude(),
                                    lastLocation.getLongitude(),
                                    "AIzaSyA_VqZHeCoru8nH2UciuV00g90iNHt_PNg")
                    )
                            .enqueue(new Callback<String>() {
                                @Override
                                public void onResponse(Call<String> call, Response<String> response) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(response.body().toString());
                                        JSONArray resultArray = jsonObject.getJSONArray("results");
                                        JSONObject firstObject = resultArray.getJSONObject(0);
                                        address = firstObject.getString("formatted_address");
                                        edtAddress.setText(address);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(Call<String> call, Throwable t) {
                                    Toast.makeText(mContext, t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        rbtnHomeAddress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Common.currentUser.getHomeAddress() != null ||
                            !TextUtils.isEmpty(Common.currentUser.getHomeAddress())) {
                        address = Common.currentUser.getHomeAddress();
                        edtAddress.setText(address);
                    } else {
                        Toast.makeText(mContext, "Home Address is empty please update it", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        alertDialog.setView(order_address_comment);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Add check condition here
                // If user select address from Place Fragment, just use it
                // If user select Ship to This Address, get Address from location and use it
                // If user select Home Address , get HomeAddress from Profile and use it
                if (!rbtnShipToAddress.isChecked() && !rbtnHomeAddress.isChecked()) {
                    //if both radio buttons are not selected
                    if (shippingAddress != null) {
                        //address = shippingAddress.getAddress().toString();
                        address = edtAddress.getText().toString();
                    } else {
                        Toast.makeText(mContext, "Please enter an address or select option address", Toast.LENGTH_SHORT).show();
                        //removeFragment();
                        //return;
                    }
                }


                //address = shippingAddress.getAddress().toString();
                address = edtAddress.getText().toString();
                comment = edtComment.getText().toString();

                String formatAmount = txtTotalPrice.getText().toString()
                        .replace("$", "")
                        .replace(",", "");

                if (TextUtils.isEmpty(address)) {
                    Toast.makeText(mContext, "Please enter an address or select option address", Toast.LENGTH_SHORT).show();

                    //removeFragment();
                    //return;
                } else {
                    setupPayPalPayment(formatAmount);
                    //removeFragment();
                }
            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //removeFragment();
            }
        });
        alertDialog.show();
    }

    private void removeFragment() {
//        getFragmentManager().beginTransaction()
//                .remove(getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment))
//                .commit();
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
                /*String.format("%s,%s", shippingAddress.getLatLng().latitude, shippingAddress.getLatLng().longitude),*/
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
        cartAdapter = new CartAdapter(cart, this, new OnQuantityChangedListener() {

            @Override
            public void onQuantityChanged() {
                loadListFood();
            }
        });
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                lastLocation = location;
                if (lastLocation != null) {
                    Log.d("LOCATION", "onSuccess: Your location: " +
                            lastLocation.getLatitude() + "," +
                            lastLocation.getLongitude());
                } else {
                    Log.d("Location", "Could not get your location");
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                lastLocation = location;
                displayLocation();
//                if (mCurrLocationMarker != null) {
//                    mCurrLocationMarker.remove();
//                }
//
//                //Place current location marker
//                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                MarkerOptions markerOptions = new MarkerOptions();
//                markerOptions.position(latLng);
//                markerOptions.title("Current Position");
//                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
//                mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
//
//                //move map camera
//                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
            }
        }
    };
}
