package org.protobj.mock.gui.load.impl;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.guangyu.cd003.projects.ucenter.util.EccEncryptUtil;
import com.guangyu.cd003.projects.ucenter.util.PemUtil;
import com.guangyu.cd003.projects.ucenter.util.RsaEncryptUtil;
import com.guangyu.cd003.projects.ucenter.vo.UserToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.MockCmdParseLoad;
import com.guangyu.cd003.projects.mock.MockHandlerAnalysis;
import com.guangyu.cd003.projects.mock.cfg.ConvertExcel;
import com.guangyu.cd003.projects.mock.common.BlendedData;
import com.guangyu.cd003.projects.mock.common.ConstMockUI;
import com.guangyu.cd003.projects.mock.common.MockAccount;
import com.guangyu.cd003.projects.mock.common.PropertiesUitl;
import com.guangyu.cd003.projects.mock.gui.bo.MGCConnect;
import com.guangyu.cd003.projects.mock.gui.bo.MGCContext;
import com.guangyu.cd003.projects.mock.gui.bo.MGCLobby;
import com.guangyu.cd003.projects.mock.gui.load.IMGCIniLoad;
import com.guangyu.cd003.projects.mock.gui.load.proc.AnalysisLogErrorInfo;
import com.guangyu.cd003.projects.mock.ui.MockDialog;
import com.guangyu.cd003.projects.mock.ui.MockMenu;
import com.guangyu.cd003.projects.mock.ui.MockMenuItem;
import com.guangyu.cd003.projects.mock.ui.MockUIContext;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.common.StringUtil;
import com.pv.framework.gs.core.util.RandomUtils;

import cn.gyyx.framework.game.HttpsConfigCenter;

public class MgcLoadMenu implements IMGCIniLoad {
	static final Logger logger = TextShowUtil.creLogger(MgcLoadMenu.class);
	MGCLobby lobby;
	@Override
	public boolean initLoad(MGCLobby lobby) {
		this.lobby = lobby;
		MockUIContext context = lobby.context;
		MockCmdParseLoad cmdParseLoad = lobby.cmdParseLoad;
		MockHandlerAnalysis analysis = lobby.analysis;
		MGCContext mockContext = lobby.mockContext;
		MockMenu opMenu = new MockMenu("操作", context);
        opMenu.addItem(new MockMenuItem("测试配置中心"), (menu) -> {
        	menu.runTask(()->{
        		testXXlConfig();
        	});
        }).addItem(new MockMenuItem("测试TLS"), (menu) -> {
        	menu.runTask(()->{
        		testTLS();
        	});
        })
        .addItem(new MockMenuItem("刷新Handler"), (menu) -> {
            menu.runTask(() -> {
                logger.info("======开始刷新 handler 配置=====");
                lobby.analysis.loadHandler();
                lobby.initLoad(IMGCIniLoad.Load_Action);
            });
        }).addItem(new MockMenuItem("loadCmd"), (e) -> {
            e.getMockUIContext().clearButs();
            cmdParseLoad.loadCmd();
            lobby.initLoad(IMGCIniLoad.Load_CmdBtn);
        }).addItem(new MockMenuItem("登录"), this::login);

//		MockMenu boxCmd = new MockMenu("Cmd",context);
//		opMenu.add(boxCmd);
//		
//		boxCmd.addItem(new MockMenuItem("loadCmd") , (e)->{
//			e.getMockUIContext().clearButs();
//			cmdParseLoad.loadCmd();
//			loadCmdBtn();
//		})
//		.addItem(new MockMenuItem("openCmd"), (e)->{
//			Path cmdCfgPath = cmdParseLoad.getCmdCfgPath();
//			File file = cmdCfgPath.toFile();
//			if(file.exists()) {
//				try {
////					Runtime.getRuntime().exec("explorer /e,/select,"+file.getAbsolutePath());
//					Runtime.getRuntime().exec("rundll32 url.dll FileProtocolHandler file://"+file.getAbsolutePath());
//				} catch (IOException ex) {
//					logger.error("open file error!",ex);
//				}
//			}
//		});

        opMenu.addToUI();

        MockMenu other = new MockMenu("设置", context);
        other
		/*		.addItem(new MockMenuItem("添加handler模块"), (m) -> {
            String module = JOptionPane.showInputDialog("handler模块包");
            logger.info("新加入模块==>{}", module);
            if (StringUtil.isNotEmpty(module)) {
                analysis.getCreatorModules().add(module);
                analysis.save();
            }
        }).addItem(new MockMenuItem("删除handler模块"), (m) -> {
            String[] selects = new String[analysis.getCreatorModules().size()];
            selects = analysis.getCreatorModules().toArray(selects);
            Arrays.sort(selects);
            String module = (String) JOptionPane.showInputDialog(null, "请选择", "module", JOptionPane.PLAIN_MESSAGE, null, selects, selects[0]);
            if (StringUtil.isNotEmpty(module)) {
                logger.info("删除模块==>{}", module);
                analysis.getCreatorModules().remove(module);
                analysis.save();
            }
        })*/
				.addItem(new MockMenuItem("解密用户"),m->{
			String showtxt = PropertiesUitl.buildString()
					.desc("只需要Token，不需要登录服地址")
					.add("Token","").build();
			String accountName =MockDialog.showDialog(m.getText(),showtxt);
			if (StringUtil.isNotEmpty(accountName)) {
				logger.info("==解密用户名==");
				Map<String, String> stringToMap = PropertiesUitl.stringToMap(accountName);
				String token = stringToMap.get("Token");
				if (StringUtil.isEmpty(token)) {
					logger.info("token not find!!!!");
					return;
				}
				logger.info("decode token:{}",token);
				UserToken decrypt = UserToken.decrypt(token);
				logger.info("deCode:{}",decrypt);
			}
		}).addItem(new MockMenuItem("修改用户"), (m) -> {
    		String showtxt = PropertiesUitl.buildString()
    		.desc("用户")
    		.add("account", analysis.getAccount().getUid()+"")
    		.desc("serverId")
    		.add("serverId", analysis.getAccount().getServerId()+"").build();
    		String accountName =MockDialog.showDialog(m.getText(),showtxt);
            if (StringUtil.isNotEmpty(accountName)) {
                logger.info("==修改用户名==");
                logger.info(accountName);
                MGCConnect mockConnect = mockContext.getConnectMap().get(analysis.getAccount().getUid());
                try {
                	Map<String, String> stringToMap = PropertiesUitl.stringToMap(accountName);
                    analysis.getAccount().setServerId(stringToMap.getOrDefault("serverId", analysis.getAccount().getServerId()).trim());
                    analysis.getAccount().setUid(stringToMap.getOrDefault("account", analysis.getAccount().getUid()).trim());
                    if (mockConnect != null) {
                        try {
                            mockConnect.close();
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("", e);
                    return;
                }
                analysis.save();
            }
        })
        .addItem(new MockMenuItem("GateWays"), (m)->{
        	StringBuilder sb = new StringBuilder();
        	String[] gateWay = analysis.getAccount().getGateWay();
        	for (int i = 0; i < gateWay.length; i++) {
        		sb.append(gateWay[i])
        		.append(",");
			}
        	String gateWays = JOptionPane.showInputDialog("GateWays:",sb.toString());
        	if (StringUtil.isNotEmpty(gateWays)) {
        		 logger.info("修改GateWay==>{}：{}", sb.toString(), gateWays);
        		try {
        			analysis.getAccount().setGateWay(gateWays.split(","));
        		} catch (Exception e) {
                    logger.error("", e);
                    return;
                }
        		analysis.save();
        	}
        })
        .addItem(new MockMenuItem("openCmd"), (e) -> {
            Path cmdCfgPath = cmdParseLoad.getCmdCfgPath();
            File file = cmdCfgPath.toFile();
            if (file.exists()) {
                try {
//					Runtime.getRuntime().exec("explorer /e,/select,"+file.getAbsolutePath());
                    Runtime.getRuntime().exec("rundll32 url.dll FileProtocolHandler file://" + file.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("open file error!", ex);
                }
            }
        });

        other.addToUI();

        MockMenu gameCfg = new MockMenu("游戏配置操作", context);
        gameCfg.addItem(new MockMenuItem("转换Excel"), (e) -> {
            logger.info("=====开始执行配置表转换=====");
            e.runTask(() -> {
                ConvertExcel.comvertExcel();
            });

        }).addItem(new MockMenuItem("OpenExcel目录"), (e) -> {
            try {
                Runtime.getRuntime().exec("cmd /c start explorer " + ConstMockUI.excelPath);
            } catch (IOException e1) {
                logger.error("", e1);
            }
        })
        ;

        gameCfg.addToUI();
        
        MockMenu tools = new MockMenu("工具", context);
        tools.addItem(new MockMenuItem("错误日志提取配置"), (e)->{
        	BlendedData blendedData = analysis.getBlendedData();
        	String showtxt = PropertiesUitl.buildString()
            .desc("解析错误日志属性字段：文件大小 默认10M")
        	.add("ana_Log_line_FileSize", blendedData.getAna_Log_line_FileSize()+"")
        	.desc("解析错误日志属性字段：一段完整异常的开始关键字 如：[2022-06")
        	.add("ana_Log_line_startStr", blendedData.getAna_Log_line_startStr())
    		.desc("解析错误日志属性字段：异常关键字 如：java.lang,ERROR,Exception")
    		.add("ana_Log_line_errorSigns", blendedData.getAna_Log_line_errorSigns()).build();
    		String errorCfg = MockDialog.showDialog(e.getText(),showtxt);
            if (StringUtil.isNotEmpty(errorCfg)) {
                logger.info("upd Conf:{}",errorCfg);
                try {
                	Map<String, String> stringToMap = PropertiesUitl.stringToMap(errorCfg);
                	blendedData.setAna_Log_line_FileSize(NumberUtils.toInt(stringToMap.getOrDefault("ana_Log_line_FileSize", "10485760")));
                    blendedData.setAna_Log_line_startStr(stringToMap.getOrDefault("ana_Log_line_startStr", "["));
                    blendedData.setAna_Log_line_errorSigns(stringToMap.getOrDefault("ana_Log_line_errorSigns", "java.lang,ERROR,Exception"));
                } catch (Exception ex) {
                    logger.error("", ex);
                    return;
                }
                analysis.save();
            }
        	
        });
        
        tools.addItem(new MockMenuItem("错误日志提取"), (e)->{
        	new Thread(()->{
    			MgcLoadMenu.this.errorParse(e);
    		}).start();
        });
        
        tools.addToUI();
        mockContext.listenDisConnection((conn) -> {
    		lobby.scheduledExecutorService.schedule(() -> {
                login(null);
            }, 2, TimeUnit.SECONDS);
        });

		initKeys();
		return false;
	}

	/**
	 * 客户端 服务器账号加密key
	 */
	private void initKeys(){
		logger.info("load keys start!");
		String path0 = ConstMockUI.resourcesPath+File.separator+"keys/rsa_private_key.pem";
		String path1 = ConstMockUI.resourcesPath+File.separator+"keys/ecc_private_key.pem";

		try {
			readRsaPrivateKey(path0);
			readEccPrivateKey(path1);
			logger.info("load keys suc!");
		}catch (Exception e){
			logger.error("initKeys",e);
		}
	}

	private void readRsaPrivateKey(String filePath){
		Security.addProvider(new BouncyCastleProvider());
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
			InputStream inputStream = Files.newInputStream(Paths.get(filePath));
			PemReader pemReader = new PemReader(new InputStreamReader(inputStream));
			PemObject privatePem = pemReader.readPemObject();
			PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privatePem.getContent());
			RSAPrivateKey rsaPrivate =  (RSAPrivateKey) factory.generatePrivate(privateSpec);
			RsaEncryptUtil.prepareKey(RsaEncryptUtil.CipherType.SERVER, null, rsaPrivate);
		}catch (Exception e){
			logger.error("init RSA Key",e);
		}
	}

	private void readEccPrivateKey(String filePath) {
		Security.addProvider(new BouncyCastleProvider());
		try {
			KeyFactory factory = KeyFactory.getInstance("EC", "BC");
			InputStream inputStream = Files.newInputStream(Paths.get(filePath));
			PemReader pemReader = new PemReader(new InputStreamReader(inputStream));
			PemObject privatePem = pemReader.readPemObject();
			PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privatePem.getContent());
			PrivateKey privateKey =  factory.generatePrivate(privateSpec);
			EccEncryptUtil.prepareKey(null, privateKey);
		} catch (Exception e) {
			logger.error("init ECC Key",e);
		}
	}
	@Override
	public int loadType() {
		return IMGCIniLoad.Load_Menu;
	}
	
	public void login(MockMenuItem but) {
		MGCContext mockContext = lobby.mockContext;
		MockHandlerAnalysis analysis = lobby.analysis;
    	MockAccount account = analysis.getAccount();
    	MGCConnect mockConnect = new MGCConnect(mockContext, account.getUid(),account.getServerId(), analysis);
    	mockContext.connect(mockConnect, account.getGateWay()[RandomUtils.nextInt(account.getGateWay().length)], (state) -> {
            if (state == 0) {
            	lobby.context.updateTitle("Client User : " + account.getUid() + "(" + account.getServerId() + ")");
            } else {
            	lobby.scheduledExecutorService.schedule(() -> {
            		login(but);
                }, 2, TimeUnit.SECONDS);
            }
        });
    }
	
	/**
	 * 错误日志分析
	 */
	private void errorParse(MockMenuItem e) {
		File selectFile = openFilePanl(e.getMockUIContext().getFrameUI());
		if(selectFile == null)return;
		new AnalysisLogErrorInfo(selectFile, lobby.analysis.getBlendedData()).start();
	}
	
	
	
	private File openFilePanl(Component parent) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("."));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("log(*.log,*.txt)", "log","txt"));
		int showOpenDialog = chooser.showOpenDialog(parent);
		if(showOpenDialog == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			return file;
		}
		return null;
	}
	
	private void testXXlConfig() {
		String showtxt = PropertiesUitl.buildString()
		.desc("xxlConfig地址")
		.add("config.center.url", "127.0.0.1:10443")
		.desc("accessToken由xxl提供")
		.add("config.center.accessToken", "123456")
		.desc("项目名字")
		.add("config.center.find.project", "appslg")
		.desc("环境")
		.add("config.center.find.env", "test")
		.desc("查询的key")
		.add("config.center.find.key", "zk102181.properties").build();
		String result = MockDialog.showDialog("测试参数",showtxt);
		logger.info("====config====");
		Map<String, String> stringToMap = PropertiesUitl.stringToMap(result);
		stringToMap.forEach((dk,value)->{
			System.setProperty(dk, value);
			logger.info("{}={}",dk,value);
		});
		if(!checkPri(System.getProperty("config.center.url"))) {
			return;
		}
		String project = System.getProperty("config.center.find.project");
		if(!checkPri(project)) {
			return;
		}
		String env = System.getProperty("config.center.find.env");
		if(!checkPri(env)) {
			return;
		}
		String key = System.getProperty("config.center.find.key");
		if(!checkPri(key)) {
			return;
		}
		HttpsConfigCenter httpsConfigCenter = new HttpsConfigCenter();
        httpsConfigCenter.updateConfigCenterAddress(project);
        String configContent = httpsConfigCenter.getConfig(env, key);
		logger.info("configCenter rvc:{}",configContent);
	}
	
	private boolean checkPri(String key) {
		if(StringUtils.isEmpty(key)) {
			return false;
		}
		return true;
	}
	private void testTLS() {
		logger.info("===开始检查开启的TLS协议===");
		SSLContext context;
		try {
			context = SSLContext.getInstance("TLS");
			 context.init(null, null, null);
		    SSLSocketFactory factory = (SSLSocketFactory) context.getSocketFactory();
		    SSLSocket socket = (SSLSocket) factory.createSocket();

		    String[] protocols = socket.getSupportedProtocols();

		    logger.info("Supported Protocols: " + protocols.length);
		    for (int i = 0; i < protocols.length; i++) {
		    	logger.info("{}" , protocols[i]);
		    }

		    protocols = socket.getEnabledProtocols();

		    logger.info("Enabled Protocols: " + protocols.length);
		    for (int i = 0; i < protocols.length; i++) {
		    	logger.info("{}", protocols[i]);
		    }
		} catch (Exception e) {
			logger.error("",e);
		}
	   
	}
}
