package com.akondi.homemarket.viewholder;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.akondi.homemarket.R;
import com.akondi.homemarket.common.Common;
import com.akondi.homemarket.databases.Database;
import com.akondi.homemarket.interfaces.ItemClickListener;
import com.akondi.homemarket.interfaces.OnQuantityChangedListener;
import com.akondi.homemarket.model.Order;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class CartViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnCreateContextMenuListener {

    public ImageView cart_image;
    public TextView txt_cart_name, txt_price;
    public ElegantNumberButton btn_quantity;

    private ItemClickListener itemClickListener;

    public void setTxt_cart_name(TextView txt_cart_name) {
        this.txt_cart_name = txt_cart_name;
    }

    public CartViewHolder(@NonNull View itemView) {
        super(itemView);
        cart_image = (ImageView) itemView.findViewById(R.id.cart_image);
        txt_cart_name = (TextView) itemView.findViewById(R.id.cart_item_name);
        txt_price = (TextView) itemView.findViewById(R.id.cart_item_price);
        btn_quantity = (ElegantNumberButton) itemView.findViewById(R.id.btn_quantity);
        itemView.setOnClickListener(this);
        itemView.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Select action");
        menu.add(0, 0, getAdapterPosition(), Common.DELETE);
    }
}

public class CartAdapter extends RecyclerView.Adapter<CartViewHolder> {

    private List<Order> listData = new ArrayList<>();
    private Context context;
    private OnQuantityChangedListener listener;

    public CartAdapter(List<Order> listData, Context context, OnQuantityChangedListener listener) {
        this.listData = listData;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.cart_layout, viewGroup, false);
        return new CartViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final CartViewHolder cartViewHolder, final int i) {

        final String quantity = listData.get(i).getQuantity();
        final String priceOfProduct = listData.get(i).getPrice();
        String amount = calculateAmount(priceOfProduct, quantity);

        Picasso.get().load(listData.get(i).getImage()).resize(70, 70).centerCrop().into(cartViewHolder.cart_image);

        String productName = listData.get(i).getProductName();
//        TextDrawable drawable = TextDrawable.builder()
//                .buildRound(quantity, Color.RED);
//        cartViewHolder.cart_item_count.setImageDrawable(drawable);
        cartViewHolder.btn_quantity.setNumber(quantity);
        cartViewHolder.btn_quantity.setOnValueChangeListener(new ElegantNumberButton.OnValueChangeListener() {
            @Override
            public void onValueChange(ElegantNumberButton view, int oldValue, int newValue) {
                Order order = listData.get(i);
                order.setQuantity(String.valueOf(newValue));
                new Database(context).updateCart(order);
                String newAmount = calculateAmount(priceOfProduct, String.valueOf(newValue));
                cartViewHolder.txt_price.setText(newAmount);
                listener.onQuantityChanged();
            }
        });

        cartViewHolder.txt_price.setText(amount);
        cartViewHolder.txt_cart_name.setText(productName);
    }

    private String calculateAmount(String price, String quantity) {
        Locale locale = new Locale("en", "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
        return fmt.format(Integer.parseInt(price) * Integer.parseInt(quantity));
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }
}
