package x.y.z.backend.domain.model;

import x.y.z.backend.utils.KieClient;

public enum ProcessInfo {

	NEW_SRP_APPLICATION(ProcessInfo.REC_APPLICATION_PROCESS, "SRP Application", ProcessInfo.REC_CONTAINER),
	REC_LETTER(ProcessInfo.REC_LETTER_PROCESS, "SRP Letter", ProcessInfo.REC_CONTAINER);

	
	//Recreation container and all process definitions inside that container.
    public static final String REC_CONTAINER = "raptor-recreation-process";
	public static final String REC_APPLICATION_PROCESS = "recreation-application";
	public static final String REC_LETTER_PROCESS = "recreation-letter";
	
	//Paleontology container and all process definitions inside that container.	

	//Science container and all process definitions inside that container.	

	public static final String CKEY_PTRN_REC_LETTER = "{0}-letter-{1,number}";
	
	private String name;
	private String description;
	private String containerId;
	   
	private ProcessInfo(String name, String description, String containerId){
		this.name = name;
		this.description = description;
		this.containerId = containerId;
	}
	
	public String getName(){
	   return name;
	}
	public static String getNameFromId(String processId){
		if(processId == null) {
			return null;
		}else if(processId.startsWith(REC_APPLICATION_PROCESS)){
			return REC_APPLICATION_PROCESS;
		}
		return null;
	}
	public String getDescription() {
		return description;
	}
	public String getContainerId() {
		return containerId;
	}
	public String getCorrelationKeyPattern() {
       switch(this) {
	       case REC_LETTER:
	    	    return CKEY_PTRN_REC_LETTER;
	       default:
	    	    return null;	    	   
	   }
	}

	public String getProcessId() {
	   String latestVer = null;

	   switch(this) {
	       case NEW_SRP_APPLICATION:
	    	    latestVer = KieClient.latestPidVerRecNewApp;
	    	    break;
	       default:
	    	   return null;
       }       
       
	    return  latestVer !=null && ! latestVer.isEmpty() ?
	    		String.format(Constants.PROCESS_ID_FORMAT, this.name, latestVer) : this.name;	    	   
       
	}
	
	public static ProcessInfo getProcessInfo(String pidOrPname) {
		
		if(pidOrPname.startsWith(NEW_SRP_APPLICATION.getName())) {
			return NEW_SRP_APPLICATION;
		}
		else{
			return null;
		}		
	}	
		
}
