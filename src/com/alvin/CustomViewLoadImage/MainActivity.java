package com.alvin.CustomViewLoadImage;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity implements CustomViewLoadImage.OnLoadMoreListener {
    private CustomViewLoadImage customViewLoadImage;
    private List<String> imageUrls;
    private int currentPage;
    private static final int NUM_PRE_PAGE = 9;
    private ImageDownloadHelper imageDownloadHelper;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        customViewLoadImage = (CustomViewLoadImage) findViewById(R.id.water_fall);
        customViewLoadImage.setOnLoadMoreListener(this);

        imageDownloadHelper = new ImageDownloadHelper();
        imageUrls = new ArrayList<String>();
        readFile();
    }

    public void readFile() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("imageUrl.txt")));
            String line = null;
            while ((line = br.readLine()) != null) {
                imageUrls.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImages();
    }

    public void loadImages() {
        int startIndex = currentPage * NUM_PRE_PAGE;
        int endIndex = startIndex + NUM_PRE_PAGE;
        if (startIndex < imageUrls.size()) {
            if (endIndex > imageUrls.size()) {
                endIndex = imageUrls.size() - 1;
            }
            List<String> list = imageUrls.subList(startIndex, endIndex);
            for (String url : list) {
                byte[] imgNameBytes = imageDownloadHelper.md5(url.getBytes());//  利用 md5 算法 获取唯一的 图片名字
                String imageName = imageDownloadHelper.toHex(imgNameBytes)+".png";
                String imgDir = SDCardHelper.getSDCardPrivateCacheDir(this) + File.separator + imageName;
                File file = new File(imgDir);
                if (file.exists()) {
                   //二次采样,参数2 按比例缩放 1/ (sampleSize*sampleSize)
                    Bitmap bitmap = ImageDownloadHelper.createThumbnail(imgDir,5);
                    if (bitmap != null) {
                        customViewLoadImage.addImage(bitmap);
                    } else {
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
                        customViewLoadImage.addImage(bitmap);
                    }
                } else {
                    ImageView imageview = new ImageView(this);
                    imageDownloadHelper.myDownloadImage(this, url, imageview, new ImageDownloadHelper.OnImageDownloadListener() {
                        @Override
                        public void onImageDownload(Bitmap bitmap, String imgUrl) {
                            customViewLoadImage.addImage(bitmap);
                        }
                    });
                }
            }
            currentPage++;
        }
    }

    @Override
    public void onTop() {

    }

    @Override
    public void onBottom() {
        loadImages();
    }
}
