package com.xhome.camera.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.xhome.camera.R;
import com.xhome.camera.http.AsyncHttpClient;
import com.xhome.camera.http.AsyncHttpResponseHandler;
import com.xhome.camera.model.Constants;
import com.xhome.camera.model.Constants.Extra;
import com.xhome.camera.utils.StringUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;
import android.widget.VideoView;

public class CameraShowActivity extends Activity {

    private static final String TAG = CameraShowActivity.class.getSimpleName();

    private Button playButton;

    private VideoView videoView;

    EditText rtspUrl;

    String[] imageUrls;

    DisplayImageOptions options;

    private SeekBar mSeekBar;

    private Handler mHandler;

    private ProgressDialog mProgressDialog;

    protected ImageLoader imageLoader = ImageLoader.getInstance();

    private String cid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_show);

        initView();
        // PlayRtspStream("rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp");
        Intent intent = getIntent();
        cid = intent.getStringExtra("cid");
        PlayRtspStream(StringUtils.generateStreamUrl(cid));
        imageUrls = Constants.IMAGES;

        options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.ic_stub)
        .showImageForEmptyUri(R.drawable.ic_empty).showImageOnFail(R.drawable.ic_error)
        .cacheInMemory(true).cacheOnDisc(true).bitmapConfig(Bitmap.Config.RGB_565).build();

        Gallery gallery = (Gallery) findViewById(R.id.gallery);
        gallery.setAdapter(new ImageGalleryAdapter());
        gallery.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startImagePagerActivity(position);
            }
        });
    }

    private long getTodayTimestamp() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        GregorianCalendar date = new GregorianCalendar(year, month, day, 0, 0, 0);
        return date.getTimeInMillis() / 1000;
    }

    private void initView() {
        videoView = (VideoView) findViewById(R.id.video_play);
        mSeekBar = (SeekBar) findViewById(R.id.play_progress);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mSeekBar.setMax(Constants.ONE_DAY);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO 请求最新的定点播放URL
                currentProgress = seekBar.getProgress();
                long time = getTodayTimestamp() + currentProgress;
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

            }
        });
        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 1000);
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

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mSeekBar.setProgress(currentProgress++);
        }
    };

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
        mHandler.removeCallbacks(mRunnable);
        videoView.stopPlayback();
        videoView = null;
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
            ImageView imageView = (ImageView) convertView;

            if(imageView == null) {
                imageView = (ImageView) getLayoutInflater().inflate(R.layout.item_gallery_image,
                            parent, false);
            }

            String url = imageUrls[position];
            imageLoader.displayImage(url, imageView, options);
            return imageView;
        }
    }
}
