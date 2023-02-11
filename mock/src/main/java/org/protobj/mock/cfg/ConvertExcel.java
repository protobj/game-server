package org.protobj.mock.cfg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.guangyu.cd003.projects.common.cfg.ExcelCfg;
import com.guangyu.cd003.projects.common.cfg.ExcelCfgCell;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import com.guangyu.cd003.projects.mock.common.ConstMockUI;
import com.guangyu.cd003.projects.mock.ui.support.TextShowUtil;
import com.pv.common.utilities.cfg.csv.BaseCfg;
import com.pv.common.utilities.common.CommonUtil;

public class ConvertExcel {
	static final Logger logger = TextShowUtil.creLogger(ConvertExcel.class);
	static final Map<String, String> typeMapping = CommonUtil.createSimpleMap();
	static final Map<String, String> luaTypeMapping = CommonUtil.createSimpleMap();
	static {
		typeMapping.put("Byte", "byte");
		typeMapping.put("Short", "short");
		typeMapping.put("Integer", "int");
		typeMapping.put("Long", "long");
		typeMapping.put("Float", "short");
		typeMapping.put("Double", "int");
		typeMapping.put("Boolean", "boolean");
		typeMapping.put("String", "str");
		
		luaTypeMapping.put("byte", "num");
		luaTypeMapping.put("short", "num");
		luaTypeMapping.put("int", "num");
		luaTypeMapping.put("long", "num");
		luaTypeMapping.put("short", "num");
		luaTypeMapping.put("int", "num");
		luaTypeMapping.put("str", "str");
		luaTypeMapping.put("[]", "fun");
		luaTypeMapping.put("boolean", "bool");
		luaTypeMapping.put("json", "json");
		luaTypeMapping.put("id", "signTypeCid");
	}
	public static void comvertExcel() {
		String pkg = "com.guangyu.cd003.projects.gs.module.";
		Set<Class<?>> typesAnnotatedWith = new Reflections(new ConfigurationBuilder()
                .forPackages(pkg)
                .addScanners(Scanners.TypesAnnotated.filterResultsBy((str)->{ 
                	return str.contains(ExcelCfg.class.getName());}))
        ).getTypesAnnotatedWith(ExcelCfg.class);
		typesAnnotatedWith.forEach((clz)->{
			ExcelData data = new ExcelData();
			String fName = clz.getSimpleName();
			char[] charArray = fName.toCharArray();
			charArray[0] += 32;
			fName = String.valueOf(charArray);
			if(fName.endsWith("Cfg")) {
				data.setName(fName.substring(0, fName.length() - 3)+".xlsx");
			}else {
				data.setName(fName+".xlsx");
			}
			
			ExcelSheel sheel = data.creSheel();
			
			Class<?> loopClz = clz;
			List<Class<?>> clzLis = CommonUtil.createList();
			while(!loopClz.equals(Object.class)) {
				clzLis.add(loopClz);
				loopClz = loopClz.getSuperclass();
			}
			Class<?>[] array = clzLis.toArray(new Class<?>[clzLis.size()]);
			ArrayUtils.reverse(array);
			List<String> cell = CommonUtil.createList();
			for (int i = 0; i < array.length; i++) {
				Class<?> clzd = array[i];
				Field[] declaredFields = clzd.getDeclaredFields();
				for (int j = 0; j < declaredFields.length; j++) {
					Field fd = declaredFields[j];
					ExcelCfgCell annotation = fd.getAnnotation(ExcelCfgCell.class);
					if(annotation == null) {
						if(clzd.equals(BaseCfg.class)) {
							String type = fd.getType().getSimpleName();
							String desc = fd.getName();
							cell.add(DocmentRoot.build(desc, fd.getName(), type,false));
						}
						continue;
					}
					String type = annotation.fieldType().trim();
					if(StringUtils.isEmpty(type)) {
						type = fd.getType().getSimpleName();
					}
					String desc = annotation.desc();
					if(StringUtils.isEmpty(desc)) {
						desc = fd.getName();
					}
					cell.add(DocmentRoot.build(desc, fd.getName(), type,annotation.isClient()));
				}
			}
			if(!cell.isEmpty()) {
				sheel.addRowValue(cell);
				saveExcel(data);
			}
		});
	}
	//<服务器，客户端>
	private static Pair<String,String> getType(String sourceType) {
		String clientType = "";
		if(sourceType.endsWith("[]")) {
			String stype = sourceType.substring(0, sourceType.lastIndexOf("["));
			if(typeMapping.containsKey(stype)) {
				stype = typeMapping.get(stype);
			}
			sourceType = stype + "s";
			clientType = luaTypeMapping.get("[]");
		}else {
			boolean isJson = sourceType.indexOf("json_") > -1;
			if(typeMapping.containsKey(sourceType)) {
				sourceType = typeMapping.get(sourceType);
			}
			if(isJson) {
				clientType = luaTypeMapping.get("json");
			}else {
				clientType = luaTypeMapping.get(sourceType);
			}
		}
		return Pair.of(sourceType,clientType);
	}


	// 创建excel
	private static void saveExcel(ExcelData data) {
		logger.info("start Save excel:{}", data.getName());
		File pFile = new File(ConstMockUI.excelPath);
		if(!pFile.exists()) {
			pFile.mkdirs();
		}
		Path path = Paths.get(ConstMockUI.excelPath, data.getName());
		File file = path.toFile();
		if(file.exists()) {
			logger.info("start Save excel:{} 已存在！", data.getName());
			return;
		}
		try(XSSFWorkbook xssBook = new XSSFWorkbook();OutputStream out = new FileOutputStream(file);) {
			for (int i = 0; i < data.getSheels().size(); i++) {
				ExcelSheel sheelData = data.getSheels().get(i);
				XSSFSheet sheetAt = null;
				if (StringUtils.isNotEmpty(sheelData.getName())) {
					sheetAt = xssBook.createSheet(sheelData.getName());
				} else {
					sheetAt = xssBook.createSheet();
				}
				for (int c = 0; c < sheelData.getDataLs().size(); c++) {
					XSSFRow createRow = sheetAt.createRow(c);
					List<String> rowData = sheelData.getDataLs().get(c);
					for (int r = 0; r < rowData.size(); r++) {
						XSSFCell createCell = createRow.createCell(r, CellType.STRING);
						createCell.setCellValue(rowData.get(r));
					}
				}

			}
			xssBook.write(out);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("save handler!", e);
		}
	}

	static class ExcelData {
		private String name;
		private List<ExcelSheel> sheels =CommonUtil.createList();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<ExcelSheel> getSheels() {
			return sheels;
		}

		public void setSheels(List<ExcelSheel> sheels) {
			this.sheels = sheels;
		}
		
		public ExcelSheel creSheel() {
			return creSheel(null);
		}
		
		public ExcelSheel creSheel(String name) {
			ExcelSheel sheel = new ExcelSheel();
			sheel.setName(name);
			sheels.add(sheel);
			return sheel;
		}
	}

	static class ExcelSheel {
		private String name;
		private List<List<String>> dataLs = CommonUtil.createList();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		public List<List<String>> getDataLs() {
			return dataLs;
		}

		public void setDataLs(List<List<String>> dataLs) {
			this.dataLs = dataLs;
		}
		
		public void addRowValue(String... rows) {
			dataLs.add(Arrays.asList(rows));
		}
		
		public void addRowValue(List<String> rows) {
			dataLs.add(rows);
		}
		
		public void addRowValueRoot(List<DocmentRoot> roots){
			List<String> rows = CommonUtil.createList();
			for (DocmentRoot rootLoop : roots) {
				rows.add(rootLoop.toCellValue());
			}
			addRowValue(rows);
		}
		
		public void addRowValueRoot(DocmentRoot... roots){
			List<String> rows = CommonUtil.createList();
			for (DocmentRoot rootLoop : roots) {
				rows.add(rootLoop.toCellValue());
			}
			addRowValue(rows);
		}
	}

	static class DocmentRoot {
		private String desc;
		private String fieldName;
		private String fieldType;
		private String cellValue;
		
		public DocmentRoot(String desc, String fieldName, String fieldType,boolean client) {
			this.desc = desc;
			this.fieldName = fieldName;
			this.fieldType = fieldType;
			Pair<String, String> fCsvType = getType(fieldType);
			StringBuilder sb = new StringBuilder();
			if(StringUtils.isNotEmpty(desc)) {
				sb.append(desc);
			}
			sb.append("{");
			sb.append(fieldName);
			sb.append(",");
			sb.append(fCsvType.getLeft());
			sb.append("}");
			if(fieldName.equals("id")) {
				sb.append("("+luaTypeMapping.get(fieldName)+")");
			}
			if(client) {
				sb.append("(");
				sb.append(fCsvType.getRight());
				sb.append(",");
				sb.append(fieldName);
				sb.append(")");
			}
			this.cellValue = sb.toString();
		}
		
		public static String build(String desc, String fieldName, String fieldType,boolean client) {
			DocmentRoot root = new DocmentRoot(desc, fieldName, fieldType,client);
			return root.toCellValue();
		}
		
		public static String build(String fieldName, String fieldType,boolean client) {
			DocmentRoot root = new DocmentRoot("", fieldName, fieldType,client);
			return root.toCellValue();
		}
		
		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public String getFieldType() {
			return fieldType;
		}

		public void setFieldType(String fieldType) {
			this.fieldType = fieldType;
		}
		
		public String toCellValue() {
			return cellValue;
		}
	}
}
