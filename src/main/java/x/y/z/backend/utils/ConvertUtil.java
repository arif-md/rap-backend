package x.y.z.backend.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import x.y.z.backend.domain.model.Constants;
import x.y.z.backend.domain.model.TaskStatus;

public class ConvertUtil {
	
  private static final Logger log = LoggerFactory.getLogger(ConvertUtil.class);
	
   public static Integer toInteger(String str) {
	   Integer result = null;
	   if(str !=null && !str.trim().isEmpty()){
		   try{
		    result = Integer.parseInt(str);
		   }catch (Exception e){
			   //Ignore the exception and return null for invalid values}
		   }
	   }	
	   return result;
   }
   
   public static Boolean getBoolean(Character ch){
	   if(ch == null){
		 return null;
	   }
	   Boolean result = null;
	   if(ch == Constants.YES){
		   result = true;
	   }else if(ch == Constants.NO){
		   result = false;
	   }
	   return result;
   }
   
   public static Boolean getBoolean(String str){
	   if(str == null){
		 return null;
	   }
	   Boolean result = null;
	   if(str.trim().equalsIgnoreCase(Constants.YES_STR)){
		   result = Boolean.valueOf(true);
	   }else if(str.trim().equalsIgnoreCase(Constants.NO_STR)){
		   result = Boolean.valueOf(false);
	   }
	   return result;
   }   
   
   public static Character getBoolean(Boolean val){
	   if(val == null){
		   return null;
	   }
	   return val ? Constants.YES : Constants.NO;
   }
   
   public static String convertToStringChar(Boolean val){
	   if(val == null){
		   return null;
	   }
	   return val ? Constants.YES_STR : Constants.NO_STR;
   }
   
   public static Date getDate(String date){
   	Date result = null;
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
		try{
   		result = sdf.parse(date);    			
		}catch(Exception e){
			log.debug("Exception while parsing date");
		}    	
   	return result;
   }
   
   public static String getDate(Date date){
   	String result = null;
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
		try{
   		result = sdf.format(date);   			
		}catch(Exception e){
			log.debug("Exception while parsing date");
		}    	
   	return result;
   }
   
   //Assumption : Given date is in the same time zone as system default time zone.
   public static LocalDate getLocalDate(Date date){
	   if(date == null ) {
		   return null;
	   }	   
	   LocalDate result = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	   return result;
   }
   
   
	public static String getRaptorTaskStatus(String status){
		String result = null;
    	switch(status){
    	case "Ready":
    		result = TaskStatus.READY.getDescription();// "Not Assigned";
    		break;
    	case "InProgress":
    		result =  TaskStatus.INPROGRESS.getDescription(); //"Active"
    		break;  
    	case "Reserved":
    		result = TaskStatus.RESERVED.getDescription(); // "Pending";
    		break;
    	case "Exited":
    		result = TaskStatus.EXITED.getDescription();// "Aborted";
    		break;    	
    	case "Created":
    		result = TaskStatus.CREATED.getDescription();
    		break;    	
    	case "Suspended":
    		result = TaskStatus.SUSPENDED.getDescription();
    		break;    	    		
    	case "Completed":
    		result = TaskStatus.COMPLETED.getDescription();
    		break;    	    		
    	case "Failed":
    		result = TaskStatus.FAILED.getDescription();
    		break;    	    		
    	case "Error":
    		result = TaskStatus.ERROR.getDescription();
    		break;    	    		
    	case "Obsolete":
    		result = TaskStatus.OBSOLETE.getDescription();
    		break;    	
    	}		
    	return result;
	}	
	public static int getFiscalYear(Date date) {
		log.debug("Received date for fiscal year calculation = {}", date);
		Calendar calendarDate = Calendar.getInstance();
		calendarDate.setTime(date);
		int month = calendarDate.get(Calendar.MONTH);
		int advance = (month < Constants.FIRST_FISCAL_MONTH) ? 0 : 1;
		int year = calendarDate.get(Calendar.YEAR) + advance;
		return year;
	}   
}
