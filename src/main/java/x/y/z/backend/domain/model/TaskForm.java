package x.y.z.backend.domain.model;

import java.util.Date;

import jakarta.validation.constraints.Min;

public class TaskForm extends Task {

    protected String formType;
    private Boolean isUpdated;
    protected Long id;

    // RAP.application.id linked to this task's process instance via RAP.WORKFLOW_APP_ASSOC,
    // or null if no application is associated (e.g. a process started outside the submission flow).
    private Long applicationId;

    protected Long universityId;
    protected String universityName;

    //@Size(min = 1, max = 50)
    //private String applicationNumber;
    protected String applicationCode;
    private Date startDate;
    private Date endDate;
    private Date submitDate;
    private String signedBy;
    private String actionReasonVar;
    private String actionReason;
    private Double indirectRate;
    private Date estimatedDecisionDate;

    public TaskForm() {
        this.formType = "TaskForm";
    }

    public TaskForm(Long processInstanceId, String containerId, Long id) {
        super(processInstanceId, containerId, id);
    }

    public void shallowCopy(TaskForm taskForm) {
        //super.shallowCopy(taskForm);
        this.formType = taskForm.formType;
        this.isUpdated = taskForm.isUpdated;
        this.id = taskForm.id;
        this.universityId = taskForm.universityId;
        //this.applicationId = taskForm.applicationId;
        //this.applicationNumber = taskForm.applicationNumber;
        this.startDate = taskForm.startDate;
        this.endDate = taskForm.endDate;
        this.submitDate = taskForm.submitDate;
        this.signedBy = taskForm.signedBy;
        this.actionReasonVar = taskForm.actionReasonVar;
        this.actionReason = taskForm.actionReason;
        this.indirectRate = taskForm.indirectRate;
        this.estimatedDecisionDate = taskForm.estimatedDecisionDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getUniversityId() {
        return universityId;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }

    public Boolean getIsUpdated() {
        return isUpdated;
    }

    public void setIsUpdated(Boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    /*public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }*/

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    public String getSignedBy() {
        return signedBy;
    }

    public void setSignedBy(String signedBy) {
        this.signedBy = signedBy;
    }

    public String getActionReasonVar() {
        return actionReasonVar;
    }

    public void setActionReasonVar(String actionReasonVar) {
        this.actionReasonVar = actionReasonVar;
    }

    public String getActionReason() {
        return actionReason;
    }

    public void setActionReason(String actionReason) {
        this.actionReason = actionReason;
    }

    public Double getIndirectRate() {
        return indirectRate;
    }

    public void setIndirectRate(Double indirectRate) {
        this.indirectRate = indirectRate;
    }

    public Date getEstimatedDecisionDate() {
        return estimatedDecisionDate;
    }

    public void setEstimatedDecisionDate(Date estimatedDecisionDate) {
        this.estimatedDecisionDate = estimatedDecisionDate;
    }
    public String getUniversityName() {
        return universityName;
    }
    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }
}
