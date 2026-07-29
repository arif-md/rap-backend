package x.y.z.backend.domain.model;

/**
 * The KIE Server container/process identifiers actually used for a specific process
 * instance, resolved from JBPM.PROCESSINSTANCELOG rather than the static ProcessInfo
 * containerAlias - this stays correct even when multiple container versions are deployed
 * and a given instance is running on an older one.
 */
public class ProcessInstanceInfo {

    private Long processInstanceId;
    private String containerId;
    private String processId;

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }
}
