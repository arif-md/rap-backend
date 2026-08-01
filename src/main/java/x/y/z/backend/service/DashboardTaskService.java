package x.y.z.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.api.util.QueryFilterSpecBuilder;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.springframework.stereotype.Service;

import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.handler.ApplicationSubmissionHandler;
import x.y.z.backend.domain.model.ProcessInfo;
import x.y.z.backend.domain.model.TaskForm;
import x.y.z.backend.domain.model.TaskStatus;
import x.y.z.backend.utils.ConvertUtil;
import x.y.z.backend.utils.KieClient;
import x.y.z.backend.domain.model.Constants;
import x.y.z.backend.domain.model.CustomKieTask;

import static x.y.z.backend.domain.model.Constants.GROUP_ID_FORMAT;

/**
 * Reads the current user's in-progress jBPM tasks for the dashboard's "Action Needed" tab.
 * This talks to the jBPM process service via KieClient/UserTaskServicesClient and is
 * separate from ProcessService/WorkflowController, which serve a local RAP.task table
 * (see backend/CLAUDE.md - the two integration points look alike but don't overlap).
 */
@Service
public class DashboardTaskService {

    // Temporary: resolves TaskForm.applicationId via RAP.WORKFLOW_APP_ASSOC until this is
    // replaced with a jBPM custom/advanced query (see TODO task).
    private final ApplicationSubmissionHandler applicationSubmissionHandler;
    public static final String QueryName_findTasksAssignedAsPotentialOwnerByGroup = "findTasksAssignedAsPotentialOwnerByGroup";

    public DashboardTaskService(ApplicationSubmissionHandler applicationSubmissionHandler) {
        this.applicationSubmissionHandler = applicationSubmissionHandler;
    }

    /*public PageResponse<TaskForm> getMyActiveTasks(Long userId, int page, int size) {
        UserTaskServicesClient client = KieClient.getUserTaskServicesClient();
        List<String> statuses = new ArrayList<>();
        statuses.add(TaskStatus.INPROGRESS.value());

        // UserTaskServicesClient has no total-count API, so fetch one extra record beyond
        // the page size to detect whether a next page exists, and derive an estimated
        // totalElements from that instead.
        List<TaskSummary> fetched = client.findTasksAssignedAsPotentialOwner(
                userId.toString(), statuses, page, size + 1);

        System.out.println("Total rows fetched ======================================== "+fetched.size());
        boolean hasNext = fetched.size() > size;
        List<TaskSummary> pageContent = hasNext ? fetched.subList(0, size) : fetched;
        long totalElements = (long) page * size + pageContent.size() + (hasNext ? 1 : 0);

        List<TaskForm> content = toTaskForms(pageContent);
        return new PageResponse<>(content, page, size, totalElements);
    }*/

    public PageResponse<TaskForm> myUniversityTasks(Long officeid, int page, int size) {

        List<String> groupIds = new ArrayList<String>();
        groupIds.add(String.format(GROUP_ID_FORMAT, "INTERNAL_USER", officeid));

        List<?> fetched = findTasksAssignedAsPotentialOwnerByGroup(groupIds, page, size + 1);

        System.out.println("Total rows fetched ======================================== "+fetched.size());
        boolean hasNext = fetched.size() > size;
        List<?> pageContent = hasNext ? fetched.subList(0, size) : fetched;
        long totalElements = (long) page * size + pageContent.size() + (hasNext ? 1 : 0);

        List<TaskForm> content = toTaskForms(pageContent);
        return new PageResponse<>(content, page, size, totalElements);
    }

    public PageResponse<TaskForm> myActiveTasks(Long userId, int page, int size) {

        List<String> userIds = new ArrayList<String>();
        userIds.add(String.valueOf(userId));

        List<String> statuses = new ArrayList<>();
        statuses.add(TaskStatus.INPROGRESS.value());

        List<?> fetched = findTasksAssignedAsActualOwner(userIds, statuses, page, size + 1);

        System.out.println("Total rows fetched ======================================== "+fetched.size());
        boolean hasNext = fetched.size() > size;
        List<?> pageContent = hasNext ? fetched.subList(0, size) : fetched;
        long totalElements = (long) page * size + pageContent.size() + (hasNext ? 1 : 0);

        List<TaskForm> content = toTaskForms(pageContent);
        return new PageResponse<>(content, page, size, totalElements);
    }


	// CustomKieTask.class is passed only to satisfy the query() signature - kie-server-client's
	// QueryServicesClientImpl.getResultTypeList() only recognizes a hardcoded whitelist of
	// built-in KIE types (TaskSummary, ProcessInstance, etc.) and falls back to Object.class for
	// any custom QueryResultMapper result type, so this always deserializes as raw
	// List<LinkedHashMap>, never List<CustomKieTask>, regardless of the class literal passed
	// here. toTaskForms2 reads those maps directly instead of casting to CustomKieTask.
	private List<?> findTasksAssignedAsPotentialOwnerByGroup(List<String> groupIds, int pageNumber, int pageSize){

		QueryServicesClient queryClient = KieClient.getQueryServicesClient();

		QueryFilterSpec spec = new QueryFilterSpecBuilder().in("potowner", groupIds).oderBy("createdOn", false).get();
		//spec.setOrderBy(orderBy);
		return queryClient.query(QueryName_findTasksAssignedAsPotentialOwnerByGroup, "CustomKieTaskMapper", spec, pageNumber, pageSize, CustomKieTask.class);
	}

	private List<?> findTasksAssignedAsActualOwner(List<String> userIds, List<String> statuses, int pageNumber, int pageSize){

		QueryServicesClient queryClient = KieClient.getQueryServicesClient();

		QueryFilterSpec spec = new QueryFilterSpecBuilder().
            in("ACTUALOWNER", userIds).
            in("STATUS", statuses).
            oderBy("createdOn", false).get();
		//spec.setOrderBy(orderBy);
		return queryClient.query(QueryName_findTasksAssignedAsPotentialOwnerByGroup, "CustomKieTaskMapper", spec, pageNumber, pageSize, CustomKieTask.class);
	}

    /*private List<TaskForm> toTaskForms(List<TaskSummary> source) {
        List<TaskForm> result = new ArrayList<>();
        for (TaskSummary summary : source) {
            TaskForm task = new TaskForm();
            task.setId(summary.getId());
            task.setProcessInstanceId(summary.getProcessInstanceId());
            task.setStatus(ConvertUtil.getRaptorTaskStatus(summary.getStatus()));
            task.setProcessName(ProcessInfo.getNameFromId(summary.getProcessId()));
            task.setName(summary.getName());
            task.setContainerId(summary.getContainerId());
            task.setAssignee(summary.getActualOwner());
            task.setApplicationId(
                    applicationSubmissionHandler.findApplicationIdByProcessInstanceId(summary.getProcessInstanceId()));
            // Function/task/applicationNumber/applicationName/issuingOffice/type require
            // joining against application data - only Task.name is populated for now.
            result.add(task);
        }
        return result;
    }*/

	// source elements are raw LinkedHashMap<String,Object> (see findTasksAssignedAsPotentialOwnerByGroup)
	// keyed by CustomKieTask's @XmlElement names, which the server's JSON marshaller also uses
	// (kie-server-api's JSONMarshaller applies JAXB annotation introspection) - e.g. "task-id",
	// "task-name", "task-container-id". CustomKieTask itself is not reconstructed here since most
	// of its fields (taskId, status, actualOwner, deploymentId, processInstanceId...) have no
	// setters, only constructors, making it awkward to populate from a generic map.
	private List<TaskForm> toTaskForms(List<?> source){
		List<TaskForm> result = new ArrayList<TaskForm>();
		for(Object item : source){
			@SuppressWarnings("unchecked")
			Map<String, Object> summary = (Map<String, Object>) item;
			TaskForm task = new TaskForm();
    		task.setStatus(ConvertUtil.getRaptorTaskStatus((String) summary.get("status")));
    		task.setId(toLong(summary.get("taskId")));
    		task.setName((String) summary.get("name"));
            task.setApplicationNumber((String) summary.get("applicationNumber"));
            task.setApplicationName((String) summary.get("applicationName"));
            task.setUniversityName((String) summary.get("universityName"));
    		task.setContainerId((String) summary.get("deploymentId"));//containerId => deploymentId
			String prrocessName = (String) summary.get("processName");
			String parentProcessName = (String) summary.get("parentProcessName");
			//If this task belongs to a child process, then populate parent process details. UI only cares about parent workflow details.
			//Parent process details to populate : 1) Parent Process Name 2) Parent Process Correlation key.
    		task.setProcessName(parentProcessName != null ? parentProcessName : prrocessName);
    		task.setProcessInstanceId(toLong(summary.get("processInstanceId")));
    		task.setAssigneeId((String) summary.get("actualOwnerId"));
    		task.setAssignee((String) summary.get("actualOwnerEmail"));
    		task.setApplicationId(
    				applicationSubmissionHandler.findApplicationIdByProcessInstanceId(task.getProcessInstanceId()));
    		result.add(task);
		}
        return result;
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    /**
     * Complete a jBPM human task on behalf of the given user, e.g. from the dashboard's
     * "Action Needed" tab. The task must already be started/owned by that user (its status
     * must be InProgress) or jBPM will reject the completion.
     */
    public void completeTask(String containerId, Long taskId, Long userId) {
        UserTaskServicesClient client = KieClient.getUserTaskServicesClient();
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.OUTPUT_ACTORID, userId);
        client.completeTask(containerId, taskId, userId.toString(), params);
    }
}
