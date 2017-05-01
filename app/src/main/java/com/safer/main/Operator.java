package com.safer.main;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by haemish on 2016/11/15.
 */

public class Operator
{
    public static final int ROLE_USER=0;
    public static final int ROLE_SECURITY_AGENT=1;
    public static final int ROLE_AMBULANCE_AGENT=2;
    public static final int ROLE_TOWTRUCK_AGENT=3;
    public static final int REQ_SECURITY_AGENT=1;
    public static final int REQ_AMBULANCE_AGENT=2;
    public static final int REQ_TOWTRUCK_AGENT=3;


    /* User and session information */
    public boolean CurrentlyLoggedIn;
    public String Username;
    public String Password;
    public String RealName;
    public String RealSurname;
    public int OperatorId;
    public int CurrentRole; /* This can be ROLE_USER or ROLE_SECURITY_AGENT,ROLE_AMBULANCE_AGENT,ROLE_TOWTRUCK_AGENT */

    /* If this object is a normal user */
    public int CallActionFlag = 0; /* User has requested a call */
    public boolean MapHasBeenMoved = false;
    public LatLng CurrentPosition = null;
    public LatLng CustomPosition = null;
    public int Agent_Type_Requested = REQ_SECURITY_AGENT;
    public int RespondingAgent;
    public boolean RequestResponderDetails = false;
    public String ResponderRealName;
    public String ResponderRealSurname;
    public String ResponderCompany;
    public String ResponderRegistration;

    /* If this object is an agent user */
    public int OnlineFlag = 1; /* Agent is currently online */
    public int ThisAgentHasBeenRequested = 0;
    public int CallAcknowledgedFlag = 0;
    public int CallAnsweredFlag = 0;
    public int AgentCalledToWhichUser = 0;
    public LatLng AgentCalledToWhere;
    public String Company;
}
