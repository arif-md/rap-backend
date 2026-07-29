package x.y.z.backend.domain.model;

public enum TaskStatus {

	INPROGRESS("InProgress","Active"),
	READY("Ready","Not Assigned"),
	RESERVED("Reserved","Pending"),
	EXITED("Exited","Aborted"),
	CREATED("Created","Created"),
	SUSPENDED("Suspended","Suspended"),
	COMPLETED("Completed","Completed"),
	FAILED("Failed","Failed"),
	ERROR("Error","Error"),
	OBSOLETE("Obsolete","Obsolete");
     
	private String id;
	private String description;
	   
	private TaskStatus(String id, String description){
		this.id = id;
		this.description = description;
	}
	
	public String value(){
	   return id;
	}
	public String getDescription() {
		return description;
	}
	
	public static TaskStatus getTaskStatus(String id){
		if(INPROGRESS.value().equals(id)) {
			return INPROGRESS;
		}else if(READY.value().equals(id)) {
			return READY;
		}else if(RESERVED.value().equals(id)) {
			return RESERVED;
		}else if(EXITED.value().equals(id)) {
			return EXITED;
		}else if(CREATED.value().equals(id)) {
			return CREATED;
		}else if(SUSPENDED.value().equals(id)) {
			return SUSPENDED;
		}else if(COMPLETED.value().equals(id)) {
			return COMPLETED;
		}else if(FAILED.value().equals(id)) {
			return FAILED;
		}else if(ERROR.value().equals(id)) {
			return ERROR;
		}else if(OBSOLETE.value().equals(id)) {
			return OBSOLETE;
		}		
		
		
		else{
			return null;
		}		
	}	
	
}
