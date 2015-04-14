package com.alvin.CustomViewLoadImage;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ${Alvin} on 2015/4/7.
 */
public class CustomViewLoadImage extends ScrollView{
    private LinearLayout waterPool;
    private int columnNum;
    private List<LinearLayout> columns;
    private OnLoadMoreListener onLoadMoreListener;

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public CustomViewLoadImage(Context context) {
        this(context, null);
    }

    public CustomViewLoadImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomViewLoadImage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context,attrs);
    }
    public void init(Context context, AttributeSet attrs){
        columnNum = 3;
        waterPool = new LinearLayout(context);
        waterPool.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        waterPool.setLayoutParams(lp);

        columns = new LinkedList<LinearLayout>();
        for (int i = 0; i < columnNum; i++) {
            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1);
            column.setLayoutParams(layoutParams);
            columns.add(column);
            waterPool.addView(column);
        }
        addView(waterPool);
    }
    private int imageCount;
    public void addImage(Bitmap bitmap){
        if(bitmap !=null){
            ImageView imageView = new ImageView(getContext());
            imageView.setImageBitmap(bitmap);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            imageView.setLayoutParams(lp);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            LinearLayout linearLayout = columns.get(imageCount % columnNum);
            linearLayout.addView(imageView);
            imageCount++;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean bol = false;
        int action = ev.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                bol = true;
                break;
            case MotionEvent.ACTION_UP:
                int scrollY = getScrollY();
                int height = getHeight();
                int measuredHeight = waterPool.getMeasuredHeight();
                if(scrollY+height>=measuredHeight){
                    //TODO 已经加载到最底端了
                    if(onLoadMoreListener != null){
                        onLoadMoreListener.onBottom();
                    }
                }else if(scrollY==0){
                    if(onLoadMoreListener != null){
                        onLoadMoreListener.onTop();
                    }
                }
                break;
            default:
                bol = super.onTouchEvent(ev);
                break;
        }
        return bol;
    }

    public interface OnLoadMoreListener{
        void onTop();
        void onBottom();
    }
}
