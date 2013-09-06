package com.xhome.camera.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    public boolean saveContentToSDCard(String fileName, String content) throws IOException {
        boolean isExternalStorageAvailable = false; // SD卡可读写的标志位
        FileOutputStream fileOutputStream = null; // FileOutputStream对象

        // 创建File对象，以SD卡所在的路径作为文件存储路径
        File file = new File(Environment.getExternalStorageDirectory(), fileName);

        // 判断SD卡是否可读写
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            isExternalStorageAvailable = true;
            fileOutputStream = new FileOutputStream(file); // 创建FileOutputStream对象
            fileOutputStream.write(content.getBytes()); // 向FileOutputStream对象中写入数据

            if(fileOutputStream != null) {  // 关闭FileOutputStream对象
                fileOutputStream.close();
            }
        }

        return isExternalStorageAvailable;
    }

    public void saveContentToLocal(Context context, String fileName, String content)
    throws IOException {
        FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_APPEND);
        fileOutputStream.write(content.getBytes());

        if(fileOutputStream != null) {
            fileOutputStream.close();
        }
    }

    /**
     * Write the specified data to an specified file.
     *
     * @param file The file to write into.
     * @param data The data to write. May be null.
     * @throws IOException
     */
    public static final void writeDataToFile(Context context, final File file, byte[] data)
    throws IOException {
        if(null == file) {
            throw new IllegalArgumentException("file may not be null.");
        }

        if(null == data) {
            data = new byte[0];
        }

        final File dir = file.getParentFile();

        if(dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        FileOutputStream fos = null;

        try {
            fos = context.openFileOutput(file.getName(), Context.MODE_APPEND);
            fos.write(data);

        } catch(final Exception e) {
            Log.d(TAG, "" + e);

        } finally {
            if(null != fos) {
                fos.close();
            }
        }
    }

    public void saveAppend(Context context, String fileNameStr, String fileContentStr)
    throws IOException {
        // 追加操作模式:不覆盖源文件，但是同样其它应用无法访问该文件
        FileOutputStream fos = context.openFileOutput(fileNameStr, Context.MODE_APPEND);
        fos.write(fileContentStr.getBytes());
        fos.close();
    }
}
