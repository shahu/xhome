package com.xhome.camera.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    public static final String CAMERA_NAME_KEY = "camera_name";

    public static final String CAMERA_ID_KEY = "camera_id";

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

    public static void saveContentToLocal(Context context, String fileName, String content)
    throws IOException {
        FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_APPEND);
        fileOutputStream.write(content.getBytes());
        fileOutputStream.write("\r\n".getBytes());

        if(fileOutputStream != null) {
            fileOutputStream.close();
        }
    }

    /**
     * Write the specified data to an specified file.
     *
     * @param file
     *            The file to write into.
     * @param data
     *            The data to write. May be null.
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

    public static List<String> readFileFromData(Context context, String fileName)
    throws IOException {
        List<String> list = new ArrayList<String>();
        FileInputStream fin = null;
        InputStreamReader in = null;
        BufferedReader br = null;

        try {
            fin = context.openFileInput(fileName);
            in = new InputStreamReader(fin, "utf-8");
            br = new BufferedReader(in);
            String line = "";

            while(null != (line = br.readLine())) {
                list.add(line);
            }

        } catch(Exception e) {
            Log.e(TAG, "" + e);

        } finally {
            if(null != fin) {
                fin.close();
            }

            if(null != in) {
                in.close();
            }

            if(null != br) {
                br.close();
            }
        }

        return list;
    }

    public static boolean isFileExists(String fileName) {
        if(fileName == null || (fileName = fileName.trim()).equals("")) {
            return false;
        }

        File file = new File(fileName);
        return isFileExists(file);
    }

    public static boolean isFileExists(File file) {
        if(file == null) {
            return false;
        }

        return file.exists();
    }

    public static boolean isDirectory(File file) {
        if(file == null) {
            return false;
        }

        return file.isDirectory();
    }

    public static boolean isDirectory(String fileName) {
        if(fileName == null || (fileName = fileName.trim()).equals("")) {
            return false;
        }

        File file = new File(fileName);
        return isDirectory(file);
    }

    public static boolean createOrExistsFolder(File file) {
        if(file == null) {
            return false;
        }

        boolean result = false;

        if(isFileExists(file) && isDirectory(file)) {
            // 如果file存在且是文件夹，返回true
            return true;
        }

        // 如果文件夹不存在，创建文件夹
        if(file.mkdirs()) {
            // 创建成功返回true
            result = true;

        } else {
            // 创建失败返回false
            result = false;
        }

        return result;
    }

    public static boolean createOrExistsFolder(String fileName) {
        if(fileName == null || (fileName = fileName.trim()).equals("")) {
            return false;
        }

        File file = new File(fileName);
        return createOrExistsFolder(file);
    }

    public static boolean createOrExistsFile(String fileName) {
        if(fileName == null || (fileName = fileName.trim()).equals("")) {
            return false;
        }

        File file = new File(fileName);

        return createOrExistsFile(file);
    }

    public static boolean createOrExistsFile(File file) {
        if(file == null) {
            return false;
        }

        boolean result = false;

        if(isFileExists(file) && isFile(file)) {
            // 判断文件是否存在且为文件，如果存在结果为true
            return true;
        }

        // 如果文件不存在，创建文件
        // 先创建文件夹，否则不会成功
        File parentFile = file.getParentFile();

        if(!createOrExistsFolder(parentFile)) {
            // 如果父文件夹创建不成功，返回false
            return false;
        }

        try {
            if(file.createNewFile()) {
                // 创建成功返回true
                result = true;

            } else {
                // 创建失败返回false
                result = false;
            }

        } catch(IOException e) {
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    public static boolean isFile(File file) {
        if(file == null) {
            return false;
        }

        return file.isFile();
    }

    public static boolean isFile(String fileName) {
        if(fileName == null || (fileName = fileName.trim()).equals("")) {
            return false;
        }

        File file = new File(fileName);
        return isFile(file);
    }

    private static void deleteFilesByDirectory(File directory) {
        if(null != directory && directory.exists() && directory.isDirectory()) {
            for(File item : directory.listFiles()) {
                item.delete();
            }
        }
    }

    public static void cleanLocalFiles(Context context) {
        deleteFilesByDirectory(context.getFilesDir());
    }
}
