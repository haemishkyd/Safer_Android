# server.py
import Queue
import socket
import threading
import calendar
import time
import MySQLdb
import xml.etree.ElementTree as ET
from math import radians, cos, sin, asin, sqrt

#from dns.rdatatype import NULL
def logdiagdata(logstring):
    print('{}-{}-{} {}:{}'.format(time.strftime("%d"),time.strftime("%m"),time.strftime("%Y"),time.strftime("%H:%M:%S"),logstring))

def haversine(lon1, lat1, lon2, lat2):
    """
    Calculate the great circle distance between two points
    on the earth (specified in decimal degrees)
    """
    # convert decimal degrees to radians
    lon1, lat1, lon2, lat2 = map(radians, [lon1, lat1, lon2, lat2])

    # haversine formula
    dlon = lon2 - lon1
    dlat = lat2 - lat1
    a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
    c = 2 * asin(sqrt(a))
    r = 6371 # Radius of earth in kilometers. Use 3956 for miles
    return c * r

def build_response():
    #We need these to be the global ones since these have been set up to access the database
    global db
    global cur
    #fetch all the current locations of agents
    query_string=("SELECT * from agent_current_info")
    cur.execute(query_string)
    agent_data = cur.fetchall ()
    db.commit()
    #----------------------------------------
    #fetch all open calls
    query_string=("SELECT * from agent_calls")
    cur.execute(query_string)
    agent_calls = cur.fetchall ()
    db.commit()
    # print the rows
    #xml_string_build = "<user_data>"
    #response_call_flag = '0'
    #for row in agent_data :
    #    response_call_flag = '0'
    #    for row_inner in agent_calls:
    #        #logdiagdata("--"+str(row[1])+":"+str(row_inner[1])+" "+str(row_inner[4]))
    #        if ((row[1] == row_inner[1]) and (str(row_inner[4])=='1')):
    #            response_call_flag = '1'
    #            logdiagdata("--Set Response")
    #    xml_string_build += "<agent_pos><agent_id>"+str(row[1])+"</agent_id><latitude>"+str(row[2])+"</latitude><longitude>"+str(row[3])+"</longitude><responding_status>"+response_call_flag+"</responding_status><responding_to_user>1</responding_to_user></agent_pos>"
    #xml_string_build += "</user_data>"
    xml_string_build = "<user_data>"
    response_call_flag = '0'
    for row in agent_data :
        logdiagdata("--Set Response")
        xml_string_build += "<agent_pos><agent_id>"+str(row[1])+"</agent_id><latitude>"+str(row[2])+"</latitude><longitude>"+str(row[3])+"</longitude><responding_status>"+response_call_flag+"</responding_status><responding_to_user>1</responding_to_user></agent_pos>"
    xml_string_build += "</user_data>"

    return xml_string_build

logdiagdata("Waiting for startup to be complete...")
time.sleep(2)
logdiagdata("Startup complete!")
#CREATION OF INTERNAL SOCKET TO LISTEN FOR ACTIONS
# create a server socket object
serverport = 9998
try:
    serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
except socket.error:
    logdiagdata("Failed to create server socket")
    sys.exit()
# get local machine name
host = socket.gethostname()
# bind to the port
serversocket.bind(('0.0.0.0', serverport))
# queue up to 5 requests
serversocket.listen(5)

#create a connection to the database
db = MySQLdb.connect(host="localhost",user="root",passwd="empyrean69",db="SAfer")
cur = db.cursor()

server_run = True
closest_agent = 0
message_type = None

while server_run:
    # establish a connection
    clientsocket,addr = serversocket.accept()

    data = clientsocket.recv(1024)
    if data:
        if (str(data) != "ping"):
            if (len(str(data))>10):
                logdiagdata(str(data))
                e=ET.fromstring(str(data))
                #*******************************    
                #CHECK THE USER MESSAGES
                #*******************************
                #message_type = e.find('user_data')
                message_type = e.tag
                print(message_type)
                if (message_type == 'user_data'):
                    lat_string=e.find('LatData').text
                    long_string=e.find('LongData').text
                    user_id = e.find('UserId').text
                    call_flag = e.find('CallFlag').text
                    logdiagdata("<<User:"+user_id+" Lat:"+lat_string+" Long:"+long_string+" Call Flag:"+call_flag+">>")
                    logdiagdata("--Update User Pos")
                    if (call_flag == '1'):                        
                        #################################################################
                        #                   FIND THE CLOSEST AGENT
                        #################################################################
                        #find the closest guy
                        closest_agent = 1
                        #Right now this is just assigned to agent one - we would run a query for this
                        logdiagdata("--Log Call")
                        #################################################################
                        #                   AGENT TABLES UPDATE
                        #################################################################
                        #Insert into the database all of the calls and where the agent must go
                        query_string=("INSERT INTO agent_calls (agent_id,lat_data,long_data,call_flag,user_that_called) VALUES (%s,%s,%s,%s,%s)")
                        data_string=(closest_agent,lat_string,long_string,'1',user_id)
                        cur.execute(query_string,data_string)
                        db.commit()
                        #Insert into the database all of the calls and where the agent must go
                        query_string=("INSERT INTO user_requests (user_id,lat_data,long_data,call_flag,responder_id) VALUES (%s,%s,%s,%s,%s)")
                        data_string=(user_id,lat_string,long_string,'1',closest_agent)
                        cur.execute(query_string,data_string)
                        db.commit()                                                
                        #################################################################
                        #                   USER TABLES UPDATE
                        #################################################################    
                        #Insert into the database the fact that this user has requested a call out
                        query_string=("UPDATE user_list SET call_requested=%s WHERE user_id=%s")
                        data_string=('1',user_id)
                        cur.execute(query_string,data_string)
                        db.commit()
                        
                    else:
                        logdiagdata("--No Call")

                    response = build_response()                    
                    logdiagdata("--Send Complete")
                #*******************************    
                #NOW CHECK THE AGENT MESSAGES
                #*******************************
                #message_type = e.find('agent_data')
                message_type = e.tag
                print(message_type)
                if (message_type == 'agent_data'):
                    logdiagdata("--Agent Data Received")
                    lat_string=e.find('LatData').text
                    long_string=e.find('LongData').text
                    agent_id = e.find('AgentId').text
                    online_flag = e.find('OnlineFlag').text
                    #################################################################
                    #             UPDATE THE CURRENT AGENTS POSITION
                    ################################################################# 
                    query_string=("UPDATE agent_current_info SET lat_data=%s,long_data=%s,online_flag=%s WHERE sec_user_id=%s")
                    data_string=(lat_string,long_string,online_flag,agent_id)
                    cur.execute(query_string,data_string)
                    db.commit()

                    response = build_response()

                    logdiagdata("--Send Complete")
                clientsocket.send(response)
            else:
                logdiagdata("--Junk received")
        else:
            clientsocket.send("Ahoy!")
            logdiagdata("Ping Request")
        clientsocket.close()

logdiagdata("--End Program")
serversocket.close()
