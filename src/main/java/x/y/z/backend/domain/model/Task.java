package x.y.z.backend.domain.model;

import java.time.LocalDateTime;

/**
 * Task POJO - Domain model representing a workflow task entity.
 * This is used for the "Action Needed" tab in the dashboard.
 * Plain Java object without JPA annotations, used with MyBatis.
 */
public class Task {
    
    private Long id;
    private String function;
    private String task;
    private String applicationNumber;
    private String applicationName;
    private String issuingOffice;
    private String type;
    private String status;
    private String assignedTo;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Default constructor
    public Task() {
    }

    // Constructor for creating new tasks
    public Task(String function, String task, String applicationNumber, 
                String applicationName, String issuingOffice, String type) {
        this.function = function;
        this.task = task;
        this.applicationNumber = applicationNumber;
        this.applicationName = applicationName;
        this.issuingOffice = issuingOffice;
        this.type = type;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
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

    public String getIssuingOffice() {
        return issuingOffice;
    }

    public void setIssuingOffice(String issuingOffice) {
        this.issuingOffice = issuingOffice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", function='" + function + '\'' +
                ", task='" + task + '\'' +
                ", applicationNumber='" + applicationNumber + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", issuingOffice='" + issuingOffice + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
