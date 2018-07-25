package com.wr.qt.nmediademo;


public class FormatHelper {

	/**
	 * return a format time of 00:00
	 * @param milliseconds
	 * @return
	 */
	public static String formatDuration(int milliseconds){
		int seconds = milliseconds / 1000;
		int secondPart = seconds % 60;
		int minutePart = seconds / 60%60;
		int hourPart=seconds/3600;
//		return (minutePart >= 10 ? minutePart : "0" + minutePart) + ":" + (secondPart >= 10 ? secondPart : "0" + secondPart);
		return (hourPart>=10?hourPart:"0"+hourPart)+":"+(minutePart >= 10 ? minutePart : "0" + minutePart) + ":" + (secondPart >= 10 ? secondPart : "0" + secondPart);
	}
	
	public static String formatTitle(String title, int length){
		int len = title.length() < length ? title.length():length;
		String subString = title.substring(0, len);
		if(len < title.length()){
			subString += "...";
		}
		return subString;
	}
}
