package com.xhome.camera.activity;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.xhome.camera.R;
import com.xhome.camera.http.AsyncHttpClient;
import com.xhome.camera.http.AsyncHttpResponseHandler;
import com.xhome.camera.model.Constants;
import com.xhome.camera.model.Constants.Extra;
import com.xhome.camera.utils.StringUtils;

import android.R.integer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class CameraShowActivity extends Activity {

    private static final String TAG = CameraShowActivity.class.getSimpleName();

    private Button playButton;

    private VideoView videoView;

    EditText rtspUrl;

    DisplayImageOptions options;

    private SeekBar mSeekBar;

    private ProgressDialog mProgressDialog;

    protected ImageLoader imageLoader = ImageLoader.getInstance();

    private String cid;

    private TextView mTextView;

    private long initProgress = 0;

    private final int[] calendar = new int[3];

    private final List<String> images = new ArrayList<String>();

    private final int step = 30 * 60;

    private Gallery gallery;

    private ImageGalleryAdapter imageGalleryAdapter;

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日");

    private long todayTimeStamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_show);

        Intent intent = getIntent();
        cid = intent.getStringExtra("cid");
        initData();
        initView();
        // PlayRtspStream("rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp");
        PlayRtspStream(StringUtils.generateStreamUrl(cid));

        options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.ic_stub)
        .showImageForEmptyUri(R.drawable.ic_empty).showImageOnFail(R.drawable.ic_error)
        .cacheInMemory(true).cacheOnDisc(true).bitmapConfig(Bitmap.Config.RGB_565).build();

    }

    private void getCurrentProgress() {

    }

    private void initView() {
        videoView = (VideoView) findViewById(R.id.video_play);
        mSeekBar = (SeekBar) findViewById(R.id.play_progress);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mSeekBar.setMax(Constants.ONE_DAY);
        Log.d(TAG, "progress:" + initProgress + " maxProgress:" + mSeekBar.getMax());
        mSeekBar.setProgress((int) initProgress);
        mTextView = (TextView) findViewById(R.id.date_txt);
        mTextView.setText(formatter.format(new Date()));
        gallery = (Gallery) findViewById(R.id.gallery);
        imageGalleryAdapter = new ImageGalleryAdapter(images);
        gallery.setAdapter(imageGalleryAdapter);
        gallery.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // startImagePagerActivity(position);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO 请求最新的定点播放URL
                currentProgress = seekBar.getProgress();
                // int imageIndx = currentProgress / step;
                // gallery.setSelection(imageIndx);

                long time = todayTimeStamp + currentProgress;
                Log.d(TAG, "seek stop currentProgress:" + currentProgress);

                if((System.currentTimeMillis() / 1000 - time) > Constants.DEFAULT_PLAY_DELAY) {
                    AsyncHttpClient client = new AsyncHttpClient();
                    String streamUrl = StringUtils.generateStreamUrl(cid,
                                       System.currentTimeMillis() / 1000 - time);
                    Log.d(TAG, "streamUrl:" + streamUrl);
                    switchPlayStream(streamUrl);
                    client.get(streamUrl, new CameraHttpResponse());
                }

                Toast.makeText(CameraShowActivity.this, "seek stop", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Toast.makeText(CameraShowActivity.this, "seek start", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int index = progress / step + 1;

                Log.d(TAG, "image index:" + index);

                if(index < images.size()) {
                    gallery.setSelection(index);
                }
            }
        });
    }

    private void initData() {
        Calendar c = Calendar.getInstance();
        calendar[0] = c.get(Calendar.YEAR);
        calendar[1] = c.get(Calendar.MONTH);
        calendar[2] = c.get(Calendar.DAY_OF_MONTH);
        GregorianCalendar date = new GregorianCalendar(calendar[0], calendar[1], calendar[2], 0, 0,
                0);
        todayTimeStamp = date.getTimeInMillis() / 1000;
        getImages();
    }

    private void getImages() {
        long position = todayTimeStamp;
        long now = System.currentTimeMillis() / 1000;
        images.clear();

        while(position <= now) {
            images.add(StringUtils.generateScreenUrl(cid, position));
            position += step;
        }

        initProgress = now - todayTimeStamp;

        if(null != imageGalleryAdapter) {
            imageGalleryAdapter.items = images;
            imageGalleryAdapter.notifyDataSetChanged();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    class CameraHttpResponse extends AsyncHttpResponseHandler {

        /*
         * (non-Javadoc)
         *
         * @see com.xhome.camera.http.AsyncHttpResponseHandler#onStart()
         */
        @Override
        public void onStart() {
            // loadLinearLayout.setVisibility(View.VISIBLE);
            // loadProgressBar.setVisibility(View.VISIBLE);
            // loadTextView.setVisibility(View.VISIBLE);
            mProgressDialog.show();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.xhome.camera.http.AsyncHttpResponseHandler#onSuccess(java.lang
         * .String)
         */
        @Override
        public void onSuccess(String content) {
            // TODO
            // loadLinearLayout.setVisibility(View.GONE);
            // loadProgressBar.setVisibility(View.GONE);
            Log.d(TAG, "content:" + content);
            mProgressDialog.dismiss();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.xhome.camera.http.AsyncHttpResponseHandler#onFailure(java.lang
         * .Throwable, java.lang.String)
         */
        @Override
        public void onFailure(Throwable error, String content) {
            // loadProgressBar.setVisibility(View.INVISIBLE);
            // loadTextView.setText(R.string.app_load_fail);
            mProgressDialog.dismiss();
            Toast.makeText(CameraShowActivity.this, "load fail", Toast.LENGTH_LONG).show();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * com.xhome.camera.http.AsyncHttpResponseHandler#onFailure(java.lang
         * .Throwable)
         */
        @Override
        public void onFailure(Throwable error) {
            // TODO Auto-generated method stub
            mProgressDialog.dismiss();
            Toast.makeText(CameraShowActivity.this, "load fail", Toast.LENGTH_LONG).show();
        }

    }

    private int currentProgress = 0;

    private void PlayRtspStream(String rtspUrl) {
        videoView.setVideoURI(Uri.parse(rtspUrl));
        videoView.requestFocus();
        videoView.start();
    }

    private void switchPlayStream(String newUrl) {
        videoView.pause();
        videoView.setVideoURI(Uri.parse(newUrl));
        videoView.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.stopPlayback();
        videoView = null;
    }

    private void startImagePagerActivity(int position) {
        Intent intent = new Intent(this, ImagePagerActivity.class);
        // intent.putExtra(Extra.IMAGES, images);
        intent.putExtra(Extra.IMAGE_POSITION, position);
        startActivity(intent);
    }

    private class ImageGalleryAdapter extends BaseAdapter {
        public List<String> items;

        public ImageGalleryAdapter(List<String> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
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
            ImageView imageView = (ImageView) convertView;

            if(imageView == null) {
                imageView = (ImageView) getLayoutInflater().inflate(R.layout.item_gallery_image,
                            parent, false);
            }

            String url = items.get(position);
            imageLoader.displayImage(url, imageView, options);
            return imageView;
        }
    }
}
