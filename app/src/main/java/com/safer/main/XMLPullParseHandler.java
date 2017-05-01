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
    public static final int NO_MESSAGE_TYPE = 719;
    public static final int USER_MESSAGE_TYPE = 694;
    public static final int AGENT_MESSAGE_TYPE = 179;
    public static final int LOGIN_PASSED_MESSAGE_TYPE = 579;
    public static final int LOGIN_FAILED_MESSAGE_TYPE = 815;
    public static final int AGENT_DATA_FAILED = 574;
    public static final int AGENT_DATA_RESPONSE = 758;
    private AgentPos agent_pos;
    private String text;
    private Operator parserOperator;

    List<AgentPos> agent_pos_list;
    public int MessageFrom = NO_MESSAGE_TYPE;

    public XMLPullParseHandler(Operator passOperator)
    {
        agent_pos_list = new ArrayList<AgentPos>();
        parserOperator = passOperator;
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
                        /* Check the main message header - what are we dealing with */
                        if (tagname.equalsIgnoreCase("agent_data"))
                        {
                            MessageFrom = AGENT_MESSAGE_TYPE;
                        }
                        else if (tagname.equalsIgnoreCase("user_data"))
                        {
                            MessageFrom = USER_MESSAGE_TYPE;
                        }
                        else if (tagname.equalsIgnoreCase("login_data"))
                        {
                            MessageFrom = LOGIN_PASSED_MESSAGE_TYPE;
                        }
                        else if (tagname.equalsIgnoreCase("agent_details"))
                        {
                            MessageFrom = AGENT_DATA_RESPONSE;
                        }
                        /* If this is a agent position we need to create a new object */
                        if (tagname.equalsIgnoreCase("agent_pos") && (MessageFrom == USER_MESSAGE_TYPE))
                        {
                            agent_pos = new AgentPos();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (MessageFrom == AGENT_DATA_RESPONSE)
                        {
                            if (tagname.equalsIgnoreCase("agent_name"))
                            {
                                parserOperator.ResponderRealName = text;
                            }
                            if (tagname.equalsIgnoreCase("agent_surname"))
                            {
                                parserOperator.ResponderRealSurname = text;
                            }
                            if (tagname.equalsIgnoreCase("agent_company"))
                            {
                                parserOperator.ResponderCompany = text;
                            }
                            if (tagname.equalsIgnoreCase("agent_registration"))
                            {
                                parserOperator.ResponderRegistration = text;
                            }
                            else if (tagname.equalsIgnoreCase("error"))
                            {
                                MessageFrom = AGENT_DATA_FAILED;
                            }
                            else if (tagname.equalsIgnoreCase("agent_details"))
                            {
                            }
                        }
                        if (MessageFrom == AGENT_MESSAGE_TYPE)
                        {
                            if (tagname.equalsIgnoreCase("agent_data"))
                            {
                            }
                        }
                        if (MessageFrom == USER_MESSAGE_TYPE)
                        {
                            if (tagname.equalsIgnoreCase("agent_pos"))
                            {
                                // add employee object to list
                                agent_pos_list.add(agent_pos);
                            }
                            else if (tagname.equalsIgnoreCase("agent_id"))
                            {
                                agent_pos.SetAgentID(Integer.valueOf(text));
                            }
                            else if (tagname.equalsIgnoreCase("latitude"))
                            {
                                agent_pos.SetAgentLat(Double.valueOf(text));
                            }
                            else if (tagname.equalsIgnoreCase("longitude"))
                            {
                                agent_pos.SetAgentLong(Double.valueOf(text));
                            }
                            else if (tagname.equalsIgnoreCase("responding_status"))
                            {
                                if (text.equals("1"))
                                {
                                    agent_pos.SetRespondingState(true);
                                }
                                else
                                {
                                    agent_pos.SetRespondingState(false);
                                }
                            }
                            else if (tagname.equalsIgnoreCase("respond_to_latitude"))
                            {
                                agent_pos.SetAgentGoToLat(Double.valueOf(text));
                            }
                            else if (tagname.equalsIgnoreCase("responding_operator_role"))
                            {
                                agent_pos.SetCalledAgentRole(Integer.valueOf(text));
                            }
                            else if (tagname.equalsIgnoreCase("respond_to_longitude"))
                            {
                                agent_pos.SetAgentGoToLong(Double.valueOf(text));
                            }
                            else if (tagname.equalsIgnoreCase("responding_to_user"))
                            {
                                agent_pos.SetUserThatCalled(Integer.valueOf(text));
                            }
                            else if (tagname.equalsIgnoreCase("user_data"))
                            {
                            }
                        }
                        if (MessageFrom == LOGIN_PASSED_MESSAGE_TYPE)
                        {
                            if (tagname.equalsIgnoreCase("operator_id"))
                            {
                                parserOperator.OperatorId = Integer.valueOf(text);
                            }
                            else if (tagname.equalsIgnoreCase("name"))
                            {
                                parserOperator.RealName = text;
                            }
                            else if (tagname.equalsIgnoreCase("surname"))
                            {
                                parserOperator.RealSurname = text;
                            }
                            else if (tagname.equalsIgnoreCase("user_agent_type"))
                            {
                                parserOperator.CurrentRole = Integer.valueOf(text);
                            }
                            else if (tagname.equalsIgnoreCase("company"))
                            {
                                parserOperator.Company = text;
                            }
                            else if (tagname.equalsIgnoreCase("error"))
                            {
                                MessageFrom = LOGIN_FAILED_MESSAGE_TYPE;
                            }
                            else if (tagname.equalsIgnoreCase("login_data"))
                            {
                            }
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
