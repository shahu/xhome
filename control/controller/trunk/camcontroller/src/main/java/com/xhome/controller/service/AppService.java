package com.xhome.controller.service;

import java.util.List;

import com.xhome.comtroller.model.Camera;
import com.xhome.comtroller.model.Cmd;

/**
 * 
 * @author shahu
 *
 */
public interface AppService {
	public void control(String uid, String cid, Cmd cmd);
	public List<Camera> listCams(String uid);
	public void addCam(Camera camera);
	
}
