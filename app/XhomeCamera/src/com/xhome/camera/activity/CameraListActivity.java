package com.xhome.camera.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.xhome.camera.R;
import com.xhome.camera.model.Constants;
import com.xhome.camera.utils.FileUtils;
import com.xhome.camera.widget.RefreshListView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CameraListActivity extends CustomTitleActivity {

    private static final String TAG = CameraListActivity.class.getSimpleName();
    private RefreshListView refreshListView;

    String[] imageUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_list);
        init();
    }

    private void init() {
        refreshListView = (RefreshListView) findViewById(R.id.list_camera);
        List<String> list = getCameraList();
        CameraAdapter cameraAdapter = new CameraAdapter(list);
    }

    private List<String> getCameraList() {
        List<String> cameraList = new ArrayList<String>();

        try {
            cameraList = FileUtils.readFileFromData(this,
                                                    Constants.FILE_NAME);

        } catch(IOException e) {
            Log.e(TAG, "" + e);
        }

        return cameraList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    class CameraAdapter extends BaseAdapter {
        private List<String> items;

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
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getItem(int)
         */
        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getItemId(int)
         */
        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see android.widget.Adapter#getView(int, android.view.View,
         * android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }

}
