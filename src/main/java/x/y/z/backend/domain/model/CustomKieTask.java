package x.y.z.backend.domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomKieTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> potentialOwners = new ArrayList<String>();
    private Date lastModificationDate;
    private String lastModificationUser;
    private Map<String,Object> inputdata;
    private Map<String,Object> outputdata;
    private String processName;
    private String processInstanceDescription;
    private Map<String, Object> processVariables;
    private Map<String, Object> data;
    private Long parentProcessInstanceId;
    private String parentProcessName;
    private String parentProcessCorrelationKey;
	private Long taskId;
	private String status;
	private Date activationTime;
	private String name;
	private String description;
	private Integer priority;
	private String actualOwnerId;
    private String actualOwnerEmail;
	private String createdBy;
	private String deploymentId;
	private String processId;
	private Long processInstanceId;
	private Date createdOn;
	private Date dueDate;
	private String formName;
	private Long workItemId;
	private Date slaDueDate;
	private Integer slaCompliance;
    private String subject;
    private String correlationKey;
    private Integer processType;
    private String applicationNumber;
    private String applicationName;
    private String universityName;

    public CustomKieTask() {

    }

    public CustomKieTask(Long taskId, String status,
                                Date activationTime, String name, String description,
                                Integer priority, String actualOwnerId, String createdBy,
                                String deploymentId, String processId, Long processInstanceId,
                                Date createdOn, Date dueDate) {
        this(taskId, status, activationTime, name, description, priority, actualOwnerId, createdBy, deploymentId, processId, processInstanceId, createdOn, dueDate, null);

    }

	public CustomKieTask(Long taskId, String status, Date activationTime, String name, String description,
								Integer priority, String actualOwnerId, String createdBy, String deploymentId,
								String processId, Long processInstanceId, Date createdOn, Date dueDate, Long workItemId) {
		this(taskId, status, activationTime, name, description, priority, actualOwnerId, createdBy, deploymentId,
             processId, processInstanceId, createdOn, dueDate, workItemId,null, null, null, null);
	}
	
	
	public CustomKieTask(Long taskId, String status, Date activationTime, String name, String description,
                                Integer priority, String actualOwnerId, String createdBy, String deploymentId,
                                String processId, Long processInstanceId, Date createdOn, Date dueDate, Long workItemId, String formName, String subject, String correlationKey, Integer processType) {
        this(taskId, status, activationTime, name, description, priority, actualOwnerId, createdBy, deploymentId,
             processId, processInstanceId, createdOn, dueDate, workItemId, formName, subject, correlationKey, processType, null, null);
    }
	
	
    public CustomKieTask(Long taskId, String status, Date activationTime, String name,
                                String description,
                                Integer priority, String actualOwnerId, String createdBy, String deploymentId,
                                String processId, Long processInstanceId, Date createdOn, Date dueDate, Long workItemId, String formName, String subject, String correlationKey, Integer processType,
                                Date slaDueDate, Integer slaCompliance) {
        this.taskId = taskId;
        this.status = status;
        this.activationTime = activationTime;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.actualOwnerId = actualOwnerId;
        this.createdBy = createdBy;
        this.deploymentId = deploymentId;
        this.processId = processId;
        this.processInstanceId = processInstanceId;
        this.createdOn = createdOn;
        this.dueDate = dueDate;
        this.workItemId = workItemId;
        this.formName = formName;
        this.subject = subject;
        this.correlationKey = correlationKey;
        this.processType = processType;
        this.slaDueDate = slaDueDate;
        this.slaCompliance = slaCompliance;
    }

    public CustomKieTask(Long taskId, String name, String description, Integer priority, Date dueDate, String formName) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.formName = formName;
    }

    public CustomKieTask(Long taskId, String status, String actualOwnerId, 
            String name, String description, Integer priority, String createdBy, String processId,
            Long processInstanceId, Date createdOn, String formName, 
            String deploymentId, Date dueDate) {
        this.taskId = taskId;
        this.status = status;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.actualOwnerId = actualOwnerId;
        this.createdBy = createdBy;
        this.deploymentId = deploymentId;
        this.processId = processId;
        this.processInstanceId = processInstanceId;
        this.createdOn = createdOn;
        this.formName = formName;
        this.deploymentId = deploymentId;
        this.dueDate = dueDate;
    }

    public CustomKieTask(Long taskId, String name, String description, String formName,
                                            String subject, String actualOwnerId, String potOwner, 
                                            String correlationKey, Date createdOn, String createdBy, 
                                            Date expirationDate, Date lastModificationDate, String lastModificationUser,
                                            Integer priority, String status, Long processInstanceId, 
                                            String processId, String deploymentId, String processInstanceDescription) {
        this(taskId, status, actualOwnerId, name, description, priority, createdBy, processId, processInstanceId, createdOn, formName, deploymentId, expirationDate);
        this.potentialOwners.add(potOwner);
        this.correlationKey = correlationKey;
        this.lastModificationDate = lastModificationDate;
        this.lastModificationUser = lastModificationUser;
        this.subject = subject;
        this.processInstanceDescription = processInstanceDescription;
        this.processVariables = new HashMap<>();
    }
    
    public CustomKieTask(String actualOwnerId, String createdBy,
                                            Date createdOn,Date expirationDate,
                                            Long taskId, String name, String description,
                                            Integer priority, Long processInstanceId,
                                            String processId, String status,
                                            String potOwner, String formName,
                                            String correlationKey, String subject,
                                            String deploymentId, String processInstanceDescription) {
              this(taskId, status, actualOwnerId, name, description, priority, createdBy, processId, processInstanceId, createdOn, formName, deploymentId,expirationDate);
              this.potentialOwners.add(potOwner);
              this.correlationKey = correlationKey;
              this.processInstanceDescription = processInstanceDescription;
              this.subject = subject;
              this.processVariables = new HashMap<>();
    }


    public CustomKieTask(
                            String applicationNumber,
                            String applicationName,
                            String universityName,
                            String actualOwnerEmail, 
                            String actualOwnerId, 
                            String createdBy,
                            Date createdOn,
                            Date expirationDate,
                            Long taskId, 
                            String name, 
                            String description,
                            Integer priority, 
                            Long processInstanceId,
                            String processId, 
                            String status,
                            String potOwner, 
                            String formName,
                            String correlationKey, 
                            String subject,
                            String deploymentId, 
                            String processInstanceDescription,
                            Long parentProcessInstanceId,
                            String parentProcessName,
                            String parentProcessCorrelationKey,
                            String processName) {
              this(actualOwnerId, createdBy, createdOn, expirationDate, taskId, name, description, priority, processInstanceId, processId, status, potOwner, formName, correlationKey, subject, deploymentId, processInstanceDescription);              
              this.applicationNumber = applicationNumber;
              this.applicationName = applicationName;
              this.universityName = universityName;
              this.actualOwnerEmail = actualOwnerEmail;
              this.potentialOwners.add(potOwner);
              this.correlationKey = correlationKey;
              this.processInstanceDescription = processInstanceDescription;
              this.subject = subject;
              this.processVariables = new HashMap<>();
              this.processName = processName;
              this.parentProcessInstanceId = parentProcessInstanceId;
              this.parentProcessName = parentProcessName;
              this.parentProcessCorrelationKey = parentProcessCorrelationKey;
    }

    public CustomKieTask(
                            String actualOwnerId, 
                            String createdBy,
                            Date createdOn,
                            Date expirationDate,
                            Long taskId, 
                            String name, 
                            String description,
                            Integer priority, 
                            Long processInstanceId,
                            String processId, 
                            String status,
                            String potOwner, 
                            String formName,
                            String correlationKey, 
                            String subject,
                            String deploymentId, 
                            String processInstanceDescription,
                            Long parentProcessInstanceId,
                            String parentProcessName,
                            String parentProcessCorrelationKey,
                            String processName) {
              this(actualOwnerId, createdBy, createdOn, expirationDate, taskId, name, description, priority, processInstanceId, processId, status, potOwner, formName, correlationKey, subject, deploymentId, processInstanceDescription);              
              this.potentialOwners.add(potOwner);
              this.correlationKey = correlationKey;
              this.processInstanceDescription = processInstanceDescription;
              this.subject = subject;
              this.processVariables = new HashMap<>();
              this.processName = processName;
              this.parentProcessInstanceId = parentProcessInstanceId;
              this.parentProcessName = parentProcessName;
              this.parentProcessCorrelationKey = parentProcessCorrelationKey;
    }
    
	public Long getTaskId() {
		
		return this.taskId;
	}

	public String getStatus() {
		
		return this.status;
	}

	public Date getActivationTime() {
		
		return this.activationTime;
	}

	public String getName() {
		
		return this.name;
	}

	public String getDescription() {
		
		return this.description;
	}

	public Integer getPriority() {
		
		return this.priority;
	}

	public String getCreatedBy() {
		
		return this.createdBy;
	}

	public Date getCreatedOn() {
		
		return this.createdOn;
	}

	public Date getDueDate() {
		
		return this.dueDate;
	}

	public Long getProcessInstanceId() {
		
		return this.processInstanceId;
	}

	public String getProcessId() {
		
		return this.processId;
	}

    public String getActualOwnerId() {
        return actualOwnerId;
    }

    public String getActualOwnerEmail() {
        return actualOwnerEmail;
    }

	public String getDeploymentId() {
		
		return this.deploymentId;
	}
	
    public String getFormName() {
        return formName;
    }

	public Long getWorkItemId() {
		return workItemId;
	}

	public Date getSlaDueDate() {
		return slaDueDate;
	}

	public Integer getSlaCompliance() {
		return slaCompliance;
	}

	public void setFormName(String formName) {
        this.formName = formName;
    }
	
    public void setName(String name) {
        this.name = name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

	public void setSlaDueDate(Date slaDueDate) {
		this.slaDueDate = slaDueDate;
	}

	public void setSlaCompliance(Integer slaCompliance) {
		this.slaCompliance = slaCompliance;
	}

	public String getSubject() {
	    return subject;
	}
	
	public void setSubject(String subject) {
        this.subject = subject;
	}

	public String getCorrelationKey() {
	    return correlationKey;
	}

	public void setCorrelationKey(String correlationKey) {
	    this.correlationKey = correlationKey;
	}

	public Integer getProcessType() {
	    return processType;
    }

	public void setProcessType(Integer processType) {
	    this.processType = processType;
	}

    public Long getParentProcessInstanceId() {
        return parentProcessInstanceId;
    }

    public void setParentProcessInstanceId(Long parentProcessInstanceId) {
        this.parentProcessInstanceId = parentProcessInstanceId;
    }

    public String getParentProcessName() {
        return parentProcessName;
    }

    public void setParentProcessName(String parentProcessName) {
        this.parentProcessName = parentProcessName;
    }

    public String getParentProcessCorrelationKey() {
        return parentProcessCorrelationKey;
    }

    public void setParentProcessCorrelationKey(String parentProcessCorrelationKey) {
        this.parentProcessCorrelationKey = parentProcessCorrelationKey;
    }

    public List<String> getPotentialOwners() {
        
        return this.potentialOwners;
    }
    

    public Date getLastModificationDate() {
        
        return this.lastModificationDate;
    }
        
    public String getLastModificationUser() {
        return this.lastModificationUser;
    }

    public void setPotentialOwners(List<String> potOwners) {
        this.potentialOwners = potOwners;
    }
    
    public void addPotOwner(String potOwners) {
        this.potentialOwners.add(potOwners);
    }
    
    public void setLastModificationDate(Date lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }
        
    public void setLastModificationUser(String lastModificationUser) {
        this.lastModificationUser = lastModificationUser;
    }

    public Map<String,Object> getInputdata() {
        return inputdata;
    }

    public void setInputdata(Map<String,Object> inputdata) {
        this.inputdata = inputdata;
    }

    public Map<String,Object> getOutputdata() {
        return outputdata;
    }

    public void setOutputdata(Map<String,Object> outputdata) {
        this.outputdata = outputdata;
    }
    
    public void addInputdata(String variable, Object variableValue) {
        if (this.inputdata == null) {
            this.inputdata = new HashMap<String, Object>();
        }
        this.inputdata.put(variable, variableValue);
    }
    
    public void addOutputdata(String variable, Object variableValue) {
        if (this.outputdata == null) {
            this.outputdata = new HashMap<String, Object>();
        }
        this.outputdata.put(variable, variableValue);
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessInstanceDescription() {
        return processInstanceDescription;
    }

    public void setProcessInstanceDescription(String processInstanceDescription) {
        this.processInstanceDescription = processInstanceDescription;
    }

    public void addProcessVariable(String variable, Object variableValue) {
        if (this.processVariables == null) {
            this.processVariables = new HashMap<>();
        }
        this.processVariables.put(variable, variableValue);
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

    public void setProcessVariables(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
    }

    public void addExtraData(String variable, Object variableValue) {
        if (this.data == null) {
            this.data = new HashMap<>();
        }
        this.data.put(variable, variableValue);
    }

    public void setExtraData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getExtraData() {
        return data;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }
    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }
    public String getApplicationName() {
        return applicationName;
    }
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    public String getUniversityName() {
        return universityName;
    }
    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }
    
	@Override
	public String toString() {
	    return "CustomKieTask [taskId=" + taskId + ", parentProcessInstanceId=" + parentProcessInstanceId + ", parentProcessName=" + parentProcessName + ", parentProcessCorrelationKey=" + parentProcessCorrelationKey +", processName=" + processName + ", status=" + status + ", activationTime=" + activationTime + ", name=" + name + ", description=" + description + ", priority=" + priority + ", actualOwnerId=" +
	            actualOwnerId + ", createdBy=" + createdBy + ", deploymentId=" + deploymentId + ", processId=" + processId + ", processInstanceId=" + processInstanceId + ", createdOn=" + createdOn + ", dueDate=" + dueDate +
	            ", formName=" + formName + ", workItemId=" + workItemId + ", slaDueDate=" + slaDueDate + ", slaCompliance=" + slaCompliance + ", subject=" + subject + ", correlationKey=" + correlationKey +
	            ", processType=" + processType + "]";
	}

}
