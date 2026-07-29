package x.y.z.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.server.api.model.instance.TaskSummary;
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

    public DashboardTaskService(ApplicationSubmissionHandler applicationSubmissionHandler) {
        this.applicationSubmissionHandler = applicationSubmissionHandler;
    }

    public PageResponse<TaskForm> getMyActiveTasks(Long userId, int page, int size) {
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
    }

    private List<TaskForm> toTaskForms(List<TaskSummary> source) {
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
