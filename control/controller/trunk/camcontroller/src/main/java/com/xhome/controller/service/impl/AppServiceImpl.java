package com.xhome.controller.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.xhome.comtroller.model.Camera;
import com.xhome.comtroller.model.Cmd;
import com.xhome.controller.service.AppService;

/**
 * 
 * @author shahu
 *
 */
@Service
@Qualifier("appService")
public class AppServiceImpl implements AppService {

	@Override
	public void control(String uid, String cid, Cmd cmd) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Camera> listCams(String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCam(Camera camera) {
		// TODO Auto-generated method stub

	}

}
