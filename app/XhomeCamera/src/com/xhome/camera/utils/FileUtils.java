package com.xhome.camera.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;

public class FileUtils {
    public boolean saveContentToSDCard(String fileName, String content)
    throws IOException {
        boolean isExternalStorageAvailable = false; // SD卡可读写的标志位
        FileOutputStream fileOutputStream = null; // FileOutputStream对象

        // 创建File对象，以SD卡所在的路径作为文件存储路径
        File file = new File(Environment.getExternalStorageDirectory(),
                             fileName);

        // 判断SD卡是否可读写
        if(Environment.MEDIA_MOUNTED.equals(Environment
                                            .getExternalStorageState())) {
            isExternalStorageAvailable = true;
            fileOutputStream = new FileOutputStream(file); // 创建FileOutputStream对象
            fileOutputStream.write(content.getBytes()); // 向FileOutputStream对象中写入数据

            if(fileOutputStream != null) {  // 关闭FileOutputStream对象
                fileOutputStream.close();
            }
        }

        return isExternalStorageAvailable;
    }

    public void saveContentToLocal(Context mContext, String fileName,
                                   String content) throws IOException {
        FileOutputStream fileOutputStream = mContext.openFileOutput(fileName,
                                            Context.MODE_APPEND);
        fileOutputStream.write(content.getBytes());

        if(fileOutputStream != null) {
            fileOutputStream.close();
        }
    }
}
