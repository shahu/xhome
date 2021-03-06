package com.xhome.camera.activity;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.xhome.camera.R;
import com.xhome.camera.model.Constants;
import com.xhome.camera.utils.FileUtils;
import com.xhome.camera.utils.StringUtils;
import com.xhome.camera.view.RefreshListView;
import com.xhome.camera.view.RefreshListView.IXListViewListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class CameraListActivity extends Activity implements IXListViewListener {

    private static final String TAG = CameraListActivity.class.getSimpleName();

    private RefreshListView refreshListView;

    DisplayImageOptions options;

    protected ImageLoader imageLoader = ImageLoader.getInstance();

    private Button btnAdd;

    private TextView txtTitle;

    private EditText cidEditText;

    private CameraAdapter cameraAdapter;

    private AlertDialog alertDialog;

    private AlertDialog.Builder builder;

    private Handler handler;

    private SimpleDateFormat mSimpleDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.camera_list);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.camera_list_title);
        init();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        options = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.ic_launcher)
        .showImageForEmptyUri(R.drawable.ic_launcher)
        .showImageOnFail(R.drawable.ic_launcher).cacheInMemory(true).cacheOnDisc(true)
        .displayer(new RoundedBitmapDisplayer(20)).build();
    }

    private void init() {
        initRefreshListView();
        btnAdd = (Button) findViewById(R.id.btn_add);
        txtTitle = (TextView) findViewById(R.id.txt_title);
        txtTitle.setText(R.string.camera_list);
        List<String> list = getCameraList();
        cameraAdapter = new CameraAdapter(list);
        refreshListView.setAdapter(cameraAdapter);
        initDialog();
        btnAdd.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                alertDialog.show();
            }
        });
        handler = new Handler();
        mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    }

    private void initRefreshListView() {
        refreshListView = (RefreshListView) findViewById(R.id.list_camera);
        refreshListView.setCacheColorHint(Color.TRANSPARENT);
        refreshListView.setDivider(getResources().getDrawable(R.drawable.line));
        refreshListView.setPullLoadEnable(false);
        refreshListView.setXListViewListener(this);
        refreshListView.setVerticalScrollBarEnabled(false);
        refreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Intent intent = new Intent(CameraListActivity.this, CameraShowActivity.class);
                String cid = ((TextView) arg0.findViewById(R.id.text)).getText().toString();
                intent.putExtra("cid", cid);
                startActivity(intent);
            }
        });
    }

    private void initDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.add_camera,
                                             (ViewGroup) findViewById(R.id.dialog));
        cidEditText = (EditText) layout.findViewById(R.id.cidEditText);
        builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setView(layout);
        builder.setPositiveButton("确定", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // String item = cidEditText.getText() + Constants.SEPARATE +
                // Constants.IMAGES[0];
                String cid = cidEditText.getText().toString();
                long timestamp = new Date().getTime() / 1000;
                String screenUrl = StringUtils.generateScreenUrl(cid, timestamp);
                Log.d(TAG, "screen_url:" + screenUrl);
                String item = cid + Constants.SEPARATE + screenUrl;

                try {
                    FileUtils
                    .saveContentToLocal(CameraListActivity.this, Constants.FILE_NAME, item);

                } catch(IOException e) {
                    Log.e(TAG, "" + e);
                }

                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog = builder.create();
        alertDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                List<String> items = getCameraList();
                cameraAdapter.items = items;
                cameraAdapter.notifyDataSetChanged();
            }
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "activity onResume");
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        AnimateFirstDisplayListener.displayedImages.clear();
    }

    private List<String> getCameraList() {
        List<String> cameraList = new ArrayList<String>();

        try {
            cameraList = FileUtils.readFileFromData(this, Constants.FILE_NAME);

        } catch(IOException e) {
            Log.e(TAG, "" + e);
        }

        Log.d(TAG, "list size:" + cameraList.size());
        return cameraList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                FileUtils.cleanLocalFiles(this);
                reloadCameraList();
                break;

            default:
                break;
        }

        return false;
    }

    class CameraAdapter extends BaseAdapter {
        public List<String> items;

        private final ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

        private class ViewHolder {
            public TextView text;

            public ImageView image;
        }

        public CameraAdapter(List<String> items) {
            this.items = items;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getCount()
         */
        @Override
        public int getCount() {
            return items.size();
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getItem(int)
         */
        @Override
        public Object getItem(int position) {
            return position;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getItemId(int)
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getView(int, android.view.View,
         * android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;

            if(null == convertView) {
                view = getLayoutInflater().inflate(R.layout.camera_list_item, null);
                holder = new ViewHolder();
                holder.text = (TextView) view.findViewById(R.id.text);
                holder.image = (ImageView) view.findViewById(R.id.image);
                view.setTag(holder);

            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.text.setText(StringUtils.getCameraName(items.get(position)));
            String url = StringUtils.getCameraUrl(items.get(position));
            Log.d(TAG, "imageUrl:" + url);
            imageLoader.displayImage(url, holder.image, options, animateFirstListener);
            return view;
        }
    }

    private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {

        static final List<String> displayedImages = Collections
                .synchronizedList(new LinkedList<String>());

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if(loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);

                if(firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }

    private void onLoad() {
        refreshListView.stopRefresh();
        refreshListView.stopLoadMore();
        refreshListView.setRefreshTime(this.getString(R.string.app_list_header_refresh_last_update,
                                       mSimpleDateFormat.format(new Date())));
    }

    private void reloadCameraList() {
        List<String> items = getCameraList();

        if(0 == items.size()) {
            return;
        }

        FileUtils.cleanLocalFiles(this);
        List<String> newItems = new ArrayList<String>();

        for(String item : items) {
            String cid = item.split(Constants.SEPARATE)[0];
            String screen = StringUtils.generateScreenUrl(cid, System.currentTimeMillis() / 1000);
            String newItem = cid + Constants.SEPARATE + screen;
            newItems.add(newItem);

            try {
                FileUtils.saveContentToLocal(CameraListActivity.this, Constants.FILE_NAME, newItem);

            } catch(IOException e) {
                Log.e(TAG, "" + e);
            }
        }

        cameraAdapter.items = newItems;
        cameraAdapter.notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.xhome.camera.view.XListView.IXListViewListener#onRefresh()
     */
    @Override
    public void onRefresh() {
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                reloadCameraList();
                onLoad();
            }
        }, 2000);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.xhome.camera.view.XListView.IXListViewListener#onLoadMore()
     */
    @Override
    public void onLoadMore() {
    }

}
