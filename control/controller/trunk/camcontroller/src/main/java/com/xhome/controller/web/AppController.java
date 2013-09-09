package com.xhome.controller.web;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.xhome.common.util.ObjectMapperManager;
import com.xhome.comtroller.model.Cmd;
import com.xhome.comtroller.model.Result;
import com.xhome.comtroller.model.RotateCmd;
import com.xhome.controller.service.AppService;

/**
 * 
 * @author shahu
 * 
 */
@Controller
@RequestMapping("/ops")
public class AppController {
	private static Log log = LogFactory.getLog(AppController.class);

	@Autowired
	@Qualifier("appService")
	private AppService appService;

	@RequestMapping("/test")
	public void test(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("Content-Type", "application/json");
		response.setHeader("Cache-Control", "max-age=-1");
		ObjectMapper mapper = ObjectMapperManager.getObjectMapper();
		Result result = new Result();
		result.setErrorCode(Result.RC_OK);
		result.setMessage("server is ok");
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write(mapper.writeValueAsString(result));
			writer.close();
		} catch (Exception e) {
			log.error("write output string catch exception", e);
		}
	}

	@RequestMapping("/control")
	public void control(HttpServletRequest request, HttpServletResponse response) {
		// 1. Get the parameter
		String uid = request.getParameter("uid");
		String cid = request.getParameter("cid");
		String command = request.getParameter("cmd");

		boolean isok = false;

		ObjectMapper mapper = ObjectMapperManager.getObjectMapper();
		Result result = new Result();
		Cmd cmd = new RotateCmd();
		try {
			int code = Integer.parseInt(command);
			cmd.setCode(code);
			isok = true;
		} catch (Exception e) {
			result.setErrorCode(Result.RC_FAILURE);
			result.setMessage("parameter error!");
			log.error("write output string catch exception", e);
		}

		// 2. Process the logic
		if (isok) {
			try {
				appService.control(uid, cid, cmd);
				result.setErrorCode(Result.RC_OK);
				result.setMessage("send command sucess!");
			} catch (Exception e) {
				result.setErrorCode(Result.RC_FAILURE);
				result.setMessage("send command failed!");
				log.error("write output string catch exception", e);
			}
		}

		// 3. return the json result

		response.setHeader("Content-Type", "application/json");
		response.setHeader("Cache-Control", "max-age=-1");
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write(mapper.writeValueAsString(result));
			writer.close();
		} catch (Exception e) {
			log.error("write output string catch exception", e);
		}

	}

}
