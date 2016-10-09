package com.safer.main;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by haemish on 2016/08/07.
 */
public class XMLPullParseHandler
{
    private static final int NO_MESSAGE_TYPE = 719;
    private static final int USER_MESSAGE_TYPE = 694;
    private static final int AGENT_MESSAGE_TYPE = 179;
    List<AgentPos> agent_pos_list;
    private AgentPos agent_pos;
    private int MessageFrom = NO_MESSAGE_TYPE;
    private String text;

    public XMLPullParseHandler()
    {
        agent_pos_list = new ArrayList<AgentPos>();
    }

    public List<AgentPos> getEmployees()
    {
        return agent_pos_list;
    }

    public List<AgentPos> parse(InputStream is)
    {
        XmlPullParserFactory factory = null;
        XmlPullParser parser = null;
        try
        {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();

            parser.setInput(is, null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                String tagname = parser.getName();
                switch (eventType)
                {
                    case XmlPullParser.START_TAG:
                        if (tagname.equalsIgnoreCase("agent_data"))
                        {
                            MessageFrom = AGENT_MESSAGE_TYPE;
                        } else if (tagname.equalsIgnoreCase("user_data"))
                        {
                            MessageFrom = USER_MESSAGE_TYPE;
                        } else if (tagname.equalsIgnoreCase("agent_pos"))
                        {
                            agent_pos = new AgentPos();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagname.equalsIgnoreCase("agent_pos"))
                        {
                            // add employee object to list
                            agent_pos_list.add(agent_pos);
                        } else if (tagname.equalsIgnoreCase("agent_id"))
                        {
                            agent_pos.SetAgentID(Integer.valueOf(text));
                        } else if (tagname.equalsIgnoreCase("latitude"))
                        {
                            agent_pos.SetAgentLat(Double.valueOf(text));
                        } else if (tagname.equalsIgnoreCase("longitude"))
                        {
                            agent_pos.SetAgentLong(Double.valueOf(text));
                        } else if (tagname.equalsIgnoreCase("responding_status"))
                        {
                            if (text.equals("1"))
                            {
                                agent_pos.SetRespondingState(true);
                            } else
                            {
                                agent_pos.SetRespondingState(false);
                            }
                        } else if (tagname.equalsIgnoreCase("responding_to_user"))
                        {
                            agent_pos.SetUserThatCalled(Integer.valueOf(text));

                        } else if (tagname.equalsIgnoreCase("agent_data"))
                        {

                        } else if (tagname.equalsIgnoreCase("user_data"))
                        {

                        }
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return agent_pos_list;
    }
}
