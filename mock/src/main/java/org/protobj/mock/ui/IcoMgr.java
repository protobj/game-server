package org.protobj.mock.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.guangyu.cd003.projects.mock.common.ConstMockUI;

public class IcoMgr {
	
	public static BufferedImage FRAME_ICO;
	static{
		try {
			FRAME_ICO = ImageIO.read(new File(ConstMockUI.resourcesPath+"/m8.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
