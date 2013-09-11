/*******************************************************************************
 *
 *
 *    xhome-camera
 *
 *    StringUtils
 *    TODO File description or class description.
 *
 *    @author: Evilsylvana
 *    @since:  2013-9-7
 *    @version: 1.0
 *
 ******************************************************************************/
package com.xhome.camera.utils;

import com.xhome.camera.model.Constants;

/**
 * StringUtils of xhome-camera.
 *
 * @author Evilsylvana
 *
 */

public class StringUtils {
    public static String getCameraName(String item) {
        return item.split(Constants.SEPARATE)[0];
    }

    public static String getCameraUrl(String item) {
        return item.split(Constants.SEPARATE)[1];
    }

    // "%s/image/%d/%d/%s/%d.%s"
    public static String generateScreenUrl(String cid, long timestamp) {
        return String.format(Constants.SCREEN_URL, Constants.DOMAIN, Constants.DEFAULT_WITH,
                             Constants.DEFAULT_HEIGHT, cid, timestamp, Constants.DEFAULT_SCREEN_FORMAT);
    }

    //"%s/live/5/%d/%s.%s"
    public static String generateStreamUrl(String cid) {
        return String.format(Constants.STREAM_URL, Constants.DOMAIN, Constants.DEFAULT_PLAY_DELAY , cid, Constants.DEFAULT_STREAM_FORMAT);
    }

    public static String generateStreamUrl(String cid, long delay) {
        if(0 == delay || delay <= Constants.DEFAULT_PLAY_DELAY) {
            return generateStreamUrl(cid);
        }

        return String.format(Constants.STREAM_URL, Constants.DOMAIN, delay , cid, Constants.DEFAULT_STREAM_FORMAT);
    }
}
