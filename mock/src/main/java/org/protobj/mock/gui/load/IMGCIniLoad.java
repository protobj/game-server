package org.protobj.mock.gui.load;

import com.guangyu.cd003.projects.mock.gui.bo.MGCLobby;

public interface IMGCIniLoad {
	public static final int Load_Action = 1;
	public static final int Load_Menu = 2;
	public static final int Load_CmdBtn = 3;
	
	
	
	
	public boolean initLoad(MGCLobby lobby);
	public int loadType();
}
