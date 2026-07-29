package x.y.z.backend.domain.model;

import java.util.Calendar;

public interface Constants {

    public static final String CORR_KEY_RAPTOR = "raptorApplicationKey";
    public static final String CORR_KEY_PROP_APPNO = "applicationNumber";

    public static final String PROCESS_ID_FORMAT = "%s_v%s";

    public static final String UNIVERSITY_ID = "universityId";
    public static final String APPLICANT_ID = "applicantId";
    public static final String APPLICATION_ID = "applicationId";

    public static final String DOMAIN_APPLICATION_STATUS = "APPLICATION_STATUS_CD";
    public static final String CD_APPLICATION_STATUS_PENDING = "PENDING_SUBMIT";

	public static final char YES = 'Y';
	public static final char NO = 'N';
	public static final String YES_STR = "Y";
	public static final String NO_STR = "N";

    public static final String DATE_AND_TIME_FORMAT = "MM/dd/yyyy HH:mm:ss";
    public static final String DATE_FORMAT = "MM/dd/yyyy";
	public static final String DATETIME_FORMAT_ISO_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final int FIRST_FISCAL_MONTH  = Calendar.OCTOBER;
    public static final String OUTPUT_ACTORID = "out_actorId";

}
