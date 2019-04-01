package com.akondi.homemarket.adapter;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.akondi.homemarket.R;
import com.akondi.homemarket.interfaces.OnPageClickListener;
import com.akondi.homemarket.model.Banner;
import com.squareup.picasso.Picasso;

import java.util.List;


public class MyPagerAdapter extends PagerAdapter {

    private List<Banner> foodList;
    private LayoutInflater layoutInflater;
    private Context context;
    private OnPageClickListener onPageClickListener;

    public MyPagerAdapter(List<Banner> foodList, Context context ,OnPageClickListener onPageClickListener) {
        this.foodList = foodList;
        this.context = context;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.onPageClickListener = onPageClickListener;
    }

    public void setFoodList(List<Banner> foodList) {
        this.foodList = foodList;
    }

    @Override
    public int getCount() {
        return foodList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        //inflating different layout for each position
        //View view = layoutInflater.inflate(layouts[position], container, false);
        //inflating the same layout

//        LayoutInflater inflater = (LayoutInflater) container.getContext()
//                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = layoutInflater.inflate(R.layout.slide_layout, container, false);

        ImageView image = (ImageView) view.findViewById(R.id.imageView);

        Picasso.get()
                .load(foodList.get(position).getImage())
                .fit()
                .centerCrop()
                .into(image);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPageClickListener.onClick(position);
            }
        });

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        container.removeView(view);
    }
}
