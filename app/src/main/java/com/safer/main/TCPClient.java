package com.safer.main;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient
{

    private String serverMessage;
    public static final String SERVERIP = "www.sa-fer.com"; //your computer IP address
    public static final int SERVERPORT = 9998;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    /* This is all the data to send */
    private double m_LatitudeData;
    private double m_LongitudeData;
    private int m_AgentId;
    private int m_UserId;
    private int m_CallFlag = 0;
    private int m_CurrentRole = 255;
    private int m_OnlineFlag = 0;

    PrintWriter out;
    BufferedReader in;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(OnMessageReceived listener)
    {
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */
    public void sendMessage(String message)
    {
        if (out != null && !out.checkError())
        {
            out.println(message);
            out.flush();
        } else
        {
            Log.e("Error", "Error");
        }
    }

    public void stopClient()
    {
        mRun = false;
    }

    public void setCoordinates(LatLng current_coord, int agent_no)
    {
        m_LatitudeData = current_coord.latitude;
        m_LongitudeData = current_coord.longitude;
        if (m_CurrentRole == 0)
        {
            m_UserId = agent_no;
        } else
        {
            m_AgentId = agent_no;
        }
    }

    public void setCallAction(int flag)
    {
        if (m_CurrentRole == 0)
        {
            m_CallFlag = flag;
        }
        else
        {
            m_OnlineFlag = flag;
        }
    }

    public void setCurrentRole(int role)
    {
        m_CurrentRole = role;
    }

    public void run()
    {

        try
        {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVERIP);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVERPORT);

            try
            {
                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                String message;
                if (m_CurrentRole == 0)
                {
                    message = "<user_data><LatData>" + String.valueOf(m_LatitudeData) + "</LatData><LongData>" + String.valueOf(m_LongitudeData) + "</LongData><UserId>" + String.valueOf(m_UserId) + "</UserId><CallFlag>" + String.valueOf(m_CallFlag) + "</CallFlag></user_data>";
                } else
                {
                    message = "<agent_data><LatData>" + String.valueOf(m_LatitudeData) + "</LatData><LongData>" + String.valueOf(m_LongitudeData) + "</LongData><AgentId>" + String.valueOf(m_AgentId) + "</AgentId><OnlineFlag>" + String.valueOf(m_OnlineFlag) + "</OnlineFlag></agent_data>";
                }

                this.sendMessage(message);
                mRun = true;

                Log.e("TCP Client", "C: Sent.");

                Log.e("TCP Client", "C: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //in this while the client listens for the messages sent by the server
                while (mRun)
                {
                    serverMessage = in.readLine();

                    if (serverMessage != null && mMessageListener != null)
                    {
                        //call the method messageReceived from MyActivity class
                        mMessageListener.messageReceived(serverMessage);
                        mRun = false;
                    }
                    serverMessage = null;
                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");

            } catch (Exception e)
            {

                Log.e("TCP", "S: Error", e);

            } finally
            {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e)
        {
            Log.e("TCP", "C: Error", e);
        }


    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived
    {
        public void messageReceived(String message);
    }
}