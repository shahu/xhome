package com.xhome.comtroller.model;

public class RotateCmd extends Cmd{
	public static final int LEFT = 0;
	public static final int TOP = 1;
	public static final int RIGHT = 2;
	public static final int DOWN = 4;
	
	public RotateCmd(){
		
	}
	
	public RotateCmd(int code){
		this.code = code;
	}
}
