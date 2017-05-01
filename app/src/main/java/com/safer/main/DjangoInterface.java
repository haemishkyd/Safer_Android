package com.safer.main;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by haemish on 2017/01/31.
 */

public class DjangoInterface
{
    private Operator tcpMessageOperator;
    private OnMessageReceived mMessageListener = null;

    public DjangoInterface(OnMessageReceived listener)
    {
        mMessageListener = listener;
    }

    public void SetOperatorData(Operator passedOperatorData)
    {
        tcpMessageOperator = passedOperatorData;
    }

    public void run()
    {
        String response = "";
        String message = "";
        URL url;

        try
        {
            if (tcpMessageOperator.CurrentlyLoggedIn)
            {
                LatLng tempCoord = null;
                int callCompleteFlag;
                /**********************************************************************
                 /* Assign the position based on what is populated
                 /**********************************************************************/
                if (tcpMessageOperator.CustomPosition != null)
                {
                    tempCoord = tcpMessageOperator.CustomPosition;
                }
                else
                {
                    tempCoord = tcpMessageOperator.CurrentPosition;
                }
                /**********************************************************************
                 /* Assign the call complete based on some logic
                 /**********************************************************************/
                if (tcpMessageOperator.ThisAgentHasBeenRequested == 1)
                {
                    callCompleteFlag = 0;
                }
                else if (tcpMessageOperator.ThisAgentHasBeenRequested == 0)
                {
                    callCompleteFlag = 1;
                }
                else
                {
                    callCompleteFlag = tcpMessageOperator.ThisAgentHasBeenRequested;
                }

                /**********************************************************************/
                if (tcpMessageOperator.RequestResponderDetails == true)
                {
                    message = "<get_agent_data><agent_id>" + String.valueOf(tcpMessageOperator.RespondingAgent) + "</agent_id></get_agent_data>";
                    url = new URL("http://www.sa-fer.com:80/appinterface/?getagentdata=" + message);
                    tcpMessageOperator.RequestResponderDetails = false;
                }
                else
                {
                    if (tcpMessageOperator.CurrentRole == tcpMessageOperator.ROLE_USER)
                    {
                        message = "<user_data><LatData>" + String.valueOf(tempCoord.latitude) + "</LatData><LongData>" + String.valueOf(tempCoord.longitude) + "</LongData><UserId>" + String.valueOf(tcpMessageOperator.OperatorId) + "</UserId><CallFlag>" + String.valueOf(tcpMessageOperator.CallActionFlag) + "</CallFlag><TypeRequested>" + tcpMessageOperator.Agent_Type_Requested + "</TypeRequested></user_data>";
                        url = new URL("http://www.sa-fer.com:8001/appinterface/?userdata=" + message);
                    }
                    else
                    {
                        message = "<agent_data><LatData>" + String.valueOf(tempCoord.latitude) + "</LatData><LongData>" + String.valueOf(tempCoord.longitude) + "</LongData><AgentId>" + String.valueOf(tcpMessageOperator.OperatorId) + "</AgentId><OnlineFlag>" + String.valueOf(tcpMessageOperator.OnlineFlag) + "</OnlineFlag><Acknowledge>" + String.valueOf(tcpMessageOperator.CallAcknowledgedFlag) + "</Acknowledge><CallComplete>" + String.valueOf(callCompleteFlag) + "</CallComplete></agent_data>";
                        url = new URL("http://www.sa-fer.com:8001/appinterface/?agentdata=" + message);
                    }
                }
            }
            else
            {
                message = "<login_data><username>" + tcpMessageOperator.Username + "</username><password>" + tcpMessageOperator.Password + "</password></login_data>";
                url = new URL("http://www.sa-fer.com:8001/appinterface/?logindata=" + message);
            }

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");

            int responseCode = urlConnection.getResponseCode();
            response = "";
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                while ((line = br.readLine()) != null)
                {
                    response += line;
                }
                if (response != null && mMessageListener != null)
                {
                    //call the method messageReceived from MyActivity class
                    mMessageListener.messageReceived(response);
                }
            }
            else
            {
                response = "";
            }
        } catch (MalformedURLException e)
        {
            Log.d("MaformedURL", "Malformed URL");
        } catch (IOException e)
        {
            Log.d("IOException", "IO Exception");
        }
    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived
    {
        public void messageReceived(String message);
    }
}
