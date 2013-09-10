
package com.xhome.camera.activity;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.xhome.camera.R;
import com.xhome.camera.model.Constants;
import com.xhome.camera.utils.Constants.Extra;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.VideoView;

public class CameraShowActivity extends Activity {
    Button playButton;

    VideoView videoView;

    EditText rtspUrl;

    String[] imageUrls;

    DisplayImageOptions options;

    protected ImageLoader imageLoader = ImageLoader.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_show);

        videoView = (VideoView)findViewById(R.id.rtsp_player);
        //PlayRtspStream("rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp");
        Intent intent = getIntent();
        String cid = intent.getStringExtra("cid");
        PlayRtspStream(this.getString(R.string.mtue_stream, cid));
        imageUrls = Constants.IMAGES;

        options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.ic_stub)
        .showImageForEmptyUri(R.drawable.ic_empty).showImageOnFail(R.drawable.ic_error)
        .cacheInMemory(true).cacheOnDisc(true).bitmapConfig(Bitmap.Config.RGB_565).build();

        Gallery gallery = (Gallery)findViewById(R.id.gallery);
        gallery.setAdapter(new ImageGalleryAdapter());
        gallery.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startImagePagerActivity(position);
            }
        });
    }

    private void PlayRtspStream(String rtspUrl) {
        videoView.setVideoURI(Uri.parse(rtspUrl));
        videoView.requestFocus();
        videoView.start();
    }

    private void startImagePagerActivity(int position) {
        Intent intent = new Intent(this, ImagePagerActivity.class);
        intent.putExtra(Extra.IMAGES, imageUrls);
        intent.putExtra(Extra.IMAGE_POSITION, position);
        startActivity(intent);
    }

    private class ImageGalleryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return imageUrls.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = (ImageView)convertView;

            if(imageView == null) {
                imageView = (ImageView)getLayoutInflater().inflate(R.layout.item_gallery_image,
                            parent, false);
            }

            String url = imageUrls[position];
            imageLoader.displayImage(url, imageView, options);
            return imageView;
        }
    }
}
