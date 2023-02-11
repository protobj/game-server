package org.protobj.mock.common;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class PropertiesUitl {
	public static String splitSymbol = "\r\n";
	public static String splitsymbol2 = "\n";
	public static Map<String, String> stringToMap(String str){
		if(StringUtils.isEmpty(str)) {
			return Collections.emptyMap();
		}
		List<String> splist = new ArrayList<>();
		int nextIdx = 0;
		while(nextIdx > -1){
		     if((nextIdx = str.indexOf(splitsymbol2)) != -1) {
                splist.add(str.substring(0,nextIdx));
                str = str.substring(nextIdx + splitsymbol2.length());
            }else if((nextIdx = str.indexOf(splitSymbol)) != -1){
				splist.add(str.substring(0,nextIdx));
				str = str.substring(nextIdx + splitSymbol.length());
			}else if(StringUtils.isNotEmpty(str)){
				splist.add(str);
				break;
			}
		}
		return splist.stream().filter((nstr)->!nstr.trim().startsWith("#")).collect(Collectors.toMap((key)->key.substring(0, key.indexOf("=")), (value)->value.substring(value.indexOf("=")+1)));
	}
	
	public static String mapToString(Map<String, String> map) {
		StringBuilder sb = new StringBuilder();
		map.forEach((k,v)->{
			if(k.startsWith("#")) {
				sb.append(k).append(v).append(splitSymbol);
			}else {
				sb.append(k).append("=").append(v).append(splitSymbol);
			}
		});
		return sb.toString();
	}
	
	public static BuildString buildString() {
		return new BuildString();
	}
	
	public static class BuildString{
		private StringBuilder sb = new StringBuilder();
		
		public BuildString add(String key,String value) {
			sb.append(key).append("=").append(value).append(splitSymbol);
			return this;
		}
		
		public BuildString desc(String v) {
			sb.append("#").append(v).append(splitSymbol);
			return this;
		}
		
		public String build() {
			return sb.toString();
		}
		
	}
}
