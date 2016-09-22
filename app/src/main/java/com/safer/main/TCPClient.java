package com.safer.main;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient {

    private String serverMessage;
    public static final String SERVERIP = "www.sa-fer.com"; //your computer IP address
    public static final int SERVERPORT = 9998;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    private double latitude_data;
    private double longitude_data;
    private int agent_id;
    private int user_id;
    private int call_flag=0;
    private int current_role=255;

    PrintWriter out;
    BufferedReader in;

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
        else
        {
            Log.e("Error","Error");
        }
    }

    public void stopClient(){
        mRun = false;
    }

    public void setCoordinates(LatLng current_coord,int agent_no)
    {
        latitude_data = current_coord.latitude;
        longitude_data = current_coord.longitude;
        if (current_role == 0)
        {
            user_id = agent_no;
        }
        else
        {
            agent_id = agent_no;
        }
    }

    public void setCallAction(int flag)
    {
        call_flag = flag;
    }

    public void setCurrentRole(int role)
    {
        current_role = role;
    }

    public void run() {

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
                if (current_role == 0)
                {
                    message = "<user_data><LatData>" + String.valueOf(latitude_data) + "</LatData><LongData>" + String.valueOf(longitude_data) + "</LongData><UserId>"+String.valueOf(user_id)+"</UserId><CallFlag>"+String.valueOf(call_flag)+"</CallFlag></user_data>";
                }
                else
                {
                    message = "<agent_data><LatData>" + String.valueOf(latitude_data) + "</LatData><LongData>" + String.valueOf(longitude_data) + "</LongData><AgentId>"+String.valueOf(agent_id)+"</AgentId><AcceptFlag>"+String.valueOf(call_flag)+"</AcceptFlag></agent_data>";
                }

                this.sendMessage(message);
                mRun = true;

                Log.e("TCP Client", "C: Sent.");

                Log.e("TCP Client", "C: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //in this while the client listens for the messages sent by the server
                while (mRun) {
                    serverMessage = in.readLine();

                    if (serverMessage != null && mMessageListener != null) {
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
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}