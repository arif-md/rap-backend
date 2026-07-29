package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * MyBatis Mapper interface for RAP.WORKFLOW_APP_ASSOC, the association between an
 * application and the jBPM process instance(s) started for it. Rows are written by
 * the processes module's ProcessEventListener (x.y.z.process.listener) when a
 * process starts; this backend only reads it.
 */
@Mapper
@Repository
public interface WorkflowAppAssocMapper {

    /**
     * Find the most recently associated, active (non-deleted) jBPM process instance
     * id for an application, or null if none has been started yet.
     */
    Long findActiveProcessInstanceId(@Param("applicationId") Long applicationId);

    /**
     * Reverse lookup: find the application id associated with a jBPM process instance,
     * or null if this process instance wasn't started through the application submission
     * flow (e.g. seed/test data).
     */
    Long findApplicationIdByProcessInstanceId(@Param("processInstanceId") Long processInstanceId);
}
