package com.safer.main;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by haemish on 2016/08/07.
 */
public class AgentPos
{
    private int AgentID;
    private double AgentLat;
    private double AgentLong;
    private double AgentGoToLat;
    private double AgentGoToLong;
    private boolean Responding;
    private int UserThatCalled;
    private int CalledAgentRole;

    public int GetAgentID()
    {
        return AgentID;
    }

    public LatLng GetAgentPos()
    {
        LatLng returnPos = new LatLng(AgentLat,AgentLong);
        return returnPos;
    }

    public LatLng GetAgentGoToPos()
    {
        LatLng returnPos = new LatLng(AgentGoToLat,AgentGoToLong);
        return returnPos;
    }

    public boolean GetRespondingState()
    {
        return Responding;
    }

    public int GetUserThatCalled()
    {
        return UserThatCalled;
    }

    public int GetCalledAgentRole()
    {
        return CalledAgentRole;
    }

    public void SetAgentID(int agent_id)
    {
        AgentID = agent_id;
    }

    public void SetAgentLat(double lat)
    {
        AgentLat = lat;
    }

    public void SetAgentLong(double lon)
    {
        AgentLong = lon;
    }

    public void SetAgentGoToLat(double lat)
    {
        AgentGoToLat = lat;
    }

    public void SetAgentGoToLong(double lon)
    {
        AgentGoToLong = lon;
    }

    public void SetRespondingState(boolean state)
    {
        Responding = state;
    }

    public void SetUserThatCalled(int user_id)
    {
        UserThatCalled = user_id;
    }

    public void SetCalledAgentRole(int role)
    {
        CalledAgentRole = role;
    }
}
