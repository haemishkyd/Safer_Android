package com.safer.main;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener
{
    private GoogleMap mMap;
    Handler h = new Handler();
    int delay = 1000;
    private DjangoInterface mDjangoClient;
    private double latitude_data = -26.11846;
    private double longitude_data = 28.00108;
    LatLng localPosition;
    private LocationManager locationManager;
    private String provider;

    private Menu localMenuReference;
    private ProgressDialog pDialog;
    private SaferDatabase appInfoStore;
    private Operator appOperator;
    private List<Marker> mapMarkers;
    private Marker  myPosition;
    private Polyline mapPolyline;
    private boolean FlagToRequestResponderDetails;
    private long AgentDetailsRetryTimer;
    List<AgentPos> ShieldPlacement = null;
    SeekBar mySeekbar;

    public void publishNotification(int whichOne)
    {
        switch (whichOne)
        {
            case 0:
            {
                String messageToNotify;
                long[] vibrate = {0, 100, 200, 300};
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
                mBuilder.setSmallIcon(R.drawable.shield);
                mBuilder.setContentTitle("You Have Been Called!");

                mBuilder.setContentText("You have a user call!");
                mBuilder.setSound(Uri.parse("android.resource://" + this.getPackageName() + "/" + R.raw.been_called));
                mBuilder.setVibrate(vibrate);
                messageToNotify = "A user (" + Integer.toString(appOperator.AgentCalledToWhichUser) + ") has requested your service at Lat:" + Double.toString(appOperator.AgentCalledToWhere.latitude) + " Long: " + Double.toString(appOperator.AgentCalledToWhere.longitude);
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageToNotify));
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                // notificationID allows you to update the notification later on.
                mNotificationManager.notify(1111, mBuilder.build());
            }
            break;
        }
    }

    public void regularUIFunctions()
    {
        MenuItem loginButton = localMenuReference.findItem(R.id.menu_user);
        if (appOperator.CurrentlyLoggedIn)
        {
            loginButton.setTitle(R.string.menu_logout);
            if (appOperator.CurrentRole == Operator.ROLE_USER)
            {
                setTitle("User: " + appOperator.RealName+" "+ appOperator.RealSurname);
                if (mySeekbar != null)
                {
                    mySeekbar.setEnabled(true);
                }
                if (localMenuReference != null)
                {
                    MenuItem itemOnline = localMenuReference.findItem(R.id.menu_online);
                    itemOnline.setVisible(false);
                }
            }
            if ((appOperator.CurrentRole == appOperator.ROLE_SECURITY_AGENT) ||
                (appOperator.CurrentRole == appOperator.ROLE_AMBULANCE_AGENT) ||
                (appOperator.CurrentRole == appOperator.ROLE_TOWTRUCK_AGENT))
            {
                setTitle("Agent: " + appOperator.RealName+" "+ appOperator.RealSurname);
                if (mySeekbar != null)
                {
                    if (appOperator.CurrentRole == appOperator.ROLE_AMBULANCE_AGENT)
                        mySeekbar.setProgress(R.integer.ambulance_value);
                    if (appOperator.CurrentRole == appOperator.ROLE_SECURITY_AGENT)
                        mySeekbar.setProgress(R.integer.security_value);
                    if (appOperator.CurrentRole == appOperator.ROLE_TOWTRUCK_AGENT)
                        mySeekbar.setProgress(R.integer.towtruck_value);
                    mySeekbar.setEnabled(false);
                }
                if (localMenuReference != null)
                {
                    MenuItem itemOnline = localMenuReference.findItem(R.id.menu_online);
                    itemOnline.setVisible(true);
                }
            }
        }
        else
        {
            loginButton.setTitle(R.string.menu_login);
            setTitle("Please Log In..");
        }
    }

    public void acknowledgeDialog()
    {
        // custom dialog
        final Dialog ack_dialog = new Dialog(this);
        ack_dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        ack_dialog.setContentView(R.layout.acknowldge_layout);

        TextView currentInfo= (TextView)ack_dialog.findViewById(R.id.instructions_2);
        currentInfo.setText("A user (" + Integer.toString(appOperator.AgentCalledToWhichUser) + ") has requested your service at Lat:" + Double.toString(appOperator.AgentCalledToWhere.latitude) + " Long: " + Double.toString(appOperator.AgentCalledToWhere.longitude));
        Button ackButton_user = (Button) ack_dialog.findViewById(R.id.acknowledgecall);
        // if button is clicked, close the custom dialog
        ackButton_user.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                appOperator.CallAcknowledgedFlag=1;
                ack_dialog.dismiss();
            }
        });
        ack_dialog.show();
    }

    public void acknowledgeResponderDialog()
    {
        // custom dialog
        final Dialog ack_dialog = new Dialog(this);
        ack_dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        ack_dialog.setContentView(R.layout.acknowldge_layout);

        TextView currentInfo= (TextView)ack_dialog.findViewById(R.id.instructions_2);
        currentInfo.setText("Agent: "+appOperator.ResponderRealName+" "+appOperator.ResponderRealSurname+" from "+appOperator.ResponderCompany+". Registration: "+appOperator.ResponderRegistration);
        Button ackButton_user = (Button) ack_dialog.findViewById(R.id.acknowledgecall);
        ackButton_user.setText("OK");
        // if button is clicked, close the custom dialog
        ackButton_user.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ack_dialog.dismiss();
            }
        });
        ack_dialog.show();
    }

    public void runShieldPlacement()
    {
        if (ShieldPlacement != null)
        {
            int shield_idx;
            appOperator.RespondingAgent = 0;
            /**********************************************************************
             * Clear all the markers for the agents currently open.
             **********************************************************************/
            for (Marker tempMarker:mapMarkers)
            {
                tempMarker.remove();
            }
            mapMarkers.clear();
            /**********************************************************************
             * We go through all of the agents and their positions as they have
             * been returned to us!!
             **********************************************************************/
            for (shield_idx = 0; shield_idx < ShieldPlacement.size(); shield_idx++)
            {
                int shield_choice;
                if (appOperator.OperatorId != ShieldPlacement.get(shield_idx).GetAgentID())
                {
                    /**********************************************************************
                     * If the agent is not set to responding simply show where they are.
                     **********************************************************************/
                    if (!ShieldPlacement.get(shield_idx).GetRespondingState())
                    {
                        shield_choice = R.drawable.security_shield;
                        if (ShieldPlacement.get(shield_idx).GetCalledAgentRole() == appOperator.ROLE_SECURITY_AGENT)
                        {
                            shield_choice = R.drawable.security_shield;
                        }
                        else if (ShieldPlacement.get(shield_idx).GetCalledAgentRole() == appOperator.ROLE_AMBULANCE_AGENT)
                        {
                            shield_choice = R.drawable.ambulance_shield;
                        }
                        else if (ShieldPlacement.get(shield_idx).GetCalledAgentRole() == appOperator.ROLE_TOWTRUCK_AGENT)
                        {
                            shield_choice = R.drawable.towtruck_shield;
                        }
                        mapMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(ShieldPlacement.get(shield_idx).GetAgentPos())
                                .anchor(0.5f, 0.5f)
                                .title("Shield " + Integer.toString(ShieldPlacement.get(shield_idx).GetAgentID()))
                                .icon(BitmapDescriptorFactory.fromResource(shield_choice))
                        ));
                    }
                    /**********************************************************************
                     * If the agent is responding lets have a look at it.
                     **********************************************************************/
                    else if (ShieldPlacement.get(shield_idx).GetRespondingState())
                    {
                        /**********************************************************************
                         * Is the agent responding to me (assuming I am a user)
                         **********************************************************************/
                        if (ShieldPlacement.get(shield_idx).GetUserThatCalled() == appOperator.OperatorId)
                        {
                            if (appOperator.CurrentRole == appOperator.ROLE_USER)
                            {
                                shield_choice = R.drawable.security_shield_r;
                                if (ShieldPlacement.get(shield_idx).GetCalledAgentRole() == appOperator.ROLE_SECURITY_AGENT)
                                {
                                    shield_choice = R.drawable.security_shield_r;
                                }
                                else if (ShieldPlacement.get(shield_idx).GetCalledAgentRole() == appOperator.ROLE_AMBULANCE_AGENT)
                                {
                                    shield_choice = R.drawable.ambulance_shield_r;
                                }
                                else if (ShieldPlacement.get(shield_idx).GetCalledAgentRole() == appOperator.ROLE_TOWTRUCK_AGENT)
                                {
                                    shield_choice = R.drawable.towtruck_shield_r;
                                }
                                appOperator.RespondingAgent = ShieldPlacement.get(shield_idx).GetAgentID();
                                pDialog.dismiss();
                                if (FlagToRequestResponderDetails)
                                {
                                    appOperator.RequestResponderDetails = true;
                                    FlagToRequestResponderDetails = false;
                                    AgentDetailsRetryTimer = System.currentTimeMillis();
                                }
                                mapMarkers.add(mMap.addMarker(new MarkerOptions()
                                        .position(ShieldPlacement.get(shield_idx).GetAgentPos())
                                        .anchor(0.5f, 0.5f)
                                        .title("Shield " + Integer.toString(ShieldPlacement.get(shield_idx).GetAgentID()))
                                        .icon(BitmapDescriptorFactory.fromResource(shield_choice))
                                ));
                                            /* Kill my call flag */
                                appOperator.CallActionFlag = 0;
                            }
                        }
                    }
                    /**********************************************************************
                     * If the user that called this one is NOT me and this agent in in a
                     * responding state then it is assigned to someone else so we don't
                     * display it.
                     **********************************************************************/
                }

            }
            /**********************************************************************
             * Logic if there is no one responding.
             **********************************************************************/
            if (appOperator.CurrentRole == appOperator.ROLE_USER)
            {
                if (appOperator.RespondingAgent == 0)
                {
                    appOperator.ResponderRealName = "";
                    appOperator.ResponderRealSurname = "";
                    appOperator.ResponderCompany = "";
                    appOperator.ResponderRegistration = "";
                }
                else if (((System.currentTimeMillis() - AgentDetailsRetryTimer) > 10000) && (appOperator.ResponderRealName.isEmpty()))
                {
                    FlagToRequestResponderDetails = true;
                }
            }

            if ((appOperator.CurrentRole == appOperator.ROLE_SECURITY_AGENT) ||
                    (appOperator.CurrentRole == appOperator.ROLE_AMBULANCE_AGENT) ||
                    (appOperator.CurrentRole == appOperator.ROLE_TOWTRUCK_AGENT))
            {
                /**********************************************************************
                 * We go through all of the agents and their positions as they have
                 * been returned to us!! One of them will be us.
                 **********************************************************************/
                for (shield_idx = 0; shield_idx < ShieldPlacement.size(); shield_idx++)
                {
                    /**************************************************
                     * If this agent is me and is set to responding   *
                     **************************************************/
                    if ((ShieldPlacement.get(shield_idx).GetAgentID() == appOperator.OperatorId) && (ShieldPlacement.get(shield_idx).GetRespondingState()))
                    {
                        if (appOperator.CallAnsweredFlag == 0)
                        {
                            appOperator.AgentCalledToWhichUser = ShieldPlacement.get(shield_idx).GetUserThatCalled();
                            appOperator.AgentCalledToWhere = ShieldPlacement.get(shield_idx).GetAgentGoToPos();
                            if ((appOperator.ThisAgentHasBeenRequested == 0)||(appOperator.ThisAgentHasBeenRequested == 255))
                            {
                                acknowledgeDialog();
                            }
                            appOperator.ThisAgentHasBeenRequested = 1;
                            // Getting URL to the Google Directions API
                            String url = getDirectionsUrl(ShieldPlacement.get(shield_idx).GetAgentPos(), appOperator.AgentCalledToWhere);

                            DownloadTask downloadTask = new DownloadTask();

                            // Start downloading json data from Google Directions API
                            downloadTask.execute(url);
                            publishNotification(0);
                            mapMarkers.add(mMap.addMarker(new MarkerOptions()
                                    .position(ShieldPlacement.get(shield_idx).GetAgentGoToPos())
                                    .title("Final Destination")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination))));
                        }
                    }
                    /*************************************************
                     * If this one is me but is not set to responding *
                     **************************************************/
                    if ((ShieldPlacement.get(shield_idx).GetAgentID() == appOperator.OperatorId) && (!ShieldPlacement.get(shield_idx).GetRespondingState()))
                    {
                        appOperator.ThisAgentHasBeenRequested = 255;
                        appOperator.CallAnsweredFlag = 0;
                        appOperator.CallAcknowledgedFlag = 0;
                    }
                }
            }
        }
    }
    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException
    {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try
        {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null)
            {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e)
        {
            Log.d("Exception", e.toString());
        } finally
        {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest)
    {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters+"&key=AIzaSyAymo0GnJrn_hs3pO58TNjIifBx-8aQAmM";

        return url;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        localMenuReference = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.menu_user:
                if (appOperator.CurrentlyLoggedIn)
                {
                    appInfoStore.removeUserData();
                    appOperator.CurrentlyLoggedIn = false;
                    appOperator.OperatorId = 0;
                    appOperator.Username = "";
                    appOperator.Password = "";
                }
                else
                {
                    // custom dialog
                    final Dialog dialog_user = new Dialog(this);
                    dialog_user.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                    dialog_user.setContentView(R.layout.agent_id_enter);

                    Button dialogButton_user = (Button) dialog_user.findViewById(R.id.dialogButtonOK);
                    // if button is clicked, close the custom dialog
                    dialogButton_user.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            String username;
                            String password;
                            EditText TempEditText = (EditText) dialog_user.findViewById(R.id.usernameinput);
                            username = TempEditText.getText().toString();
                            TempEditText = (EditText) dialog_user.findViewById(R.id.passwordinput);
                            password = TempEditText.getText().toString();
                            appInfoStore.addUserDetail(username, password);
                            appOperator.Username = username;
                            appOperator.Password = password;
                            dialog_user.dismiss();
                        }
                    });
                    dialog_user.show();
                }
                return true;
            case R.id.menu_online:
                if (appOperator.OnlineFlag == 1)
                {
                    item.setTitle("Go Online");
                    appOperator.OnlineFlag = 0;
                }
                else
                {
                    appOperator.OnlineFlag = 1;
                    item.setTitle("Go Offline");
                }
                return true;
            case R.id.menu_clear:
                appOperator.CustomPosition = null;
                return true;
            case R.id.menu_about:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        /* Create the operator object for the app */
        appOperator = new Operator();
        mapMarkers = new ArrayList<Marker>();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        localPosition = new LatLng(-34, 151);
        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the location provider -> use
        // default
        FlagToRequestResponderDetails = true;
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        appInfoStore = new SaferDatabase(MapsActivity.this);
        appInfoStore.getReadableDatabase();

        if (!appInfoStore.isThereAUser())
        {
            appOperator.CurrentlyLoggedIn = false;
        }
        else
        {
            appOperator.CurrentlyLoggedIn = true;
            appInfoStore.getCurrentLoggedInUserDetails(appOperator);
        }

        /***************************************************************************
         *                          SEEK BAR STUFF
         ***************************************************************************/
        mySeekbar = (SeekBar) findViewById(R.id.seekBar);
        //set button actions
        mySeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if (seekBar.getProgress() < 50)
                {
                    appOperator.Agent_Type_Requested = appOperator.REQ_AMBULANCE_AGENT;
                    seekBar.setProgress(0);
                }
                if ((seekBar.getProgress()>=50) && (seekBar.getProgress()<150))
                {
                    appOperator.Agent_Type_Requested = appOperator.REQ_SECURITY_AGENT;
                    seekBar.setProgress(100);
                }
                if (seekBar.getProgress()>=150)
                {
                    appOperator.Agent_Type_Requested = appOperator.REQ_TOWTRUCK_AGENT;
                    seekBar.setProgress(200);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser)
            {

            }
        });


        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if (location != null)
        {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
        }
        else
        {

        }

        /***************************************************************************
         *                          PERIODIC TASK
         ***************************************************************************/
        h.postDelayed(new Runnable()
        {
            public void run()
            {
                if ((mDjangoClient != null) && (localPosition != null))
                {
                    latitude_data = localPosition.latitude;
                    longitude_data = localPosition.longitude;
                    LatLng carmarker = new LatLng(latitude_data, longitude_data);

                    if (myPosition != null)
                    {
                        myPosition.remove();
                    }
                    if (appOperator.ThisAgentHasBeenRequested != 1)
                    {
                        if (mapPolyline != null)
                        {
                            mapPolyline.remove();
                        }
                    }
                    /**********************************************************************
                     * As an agent or user mark my current position.
                     **********************************************************************/
                    if ((appOperator.CurrentRole == appOperator.ROLE_SECURITY_AGENT) ||
                        (appOperator.CurrentRole == appOperator.ROLE_AMBULANCE_AGENT) ||
                        (appOperator.CurrentRole == appOperator.ROLE_TOWTRUCK_AGENT))
                    {
                        if (appOperator.ThisAgentHasBeenRequested == 1)
                        {
                            myPosition = mMap.addMarker(new MarkerOptions()
                                    .position(carmarker).title("Your Position").snippet("You have been called!!!")
                                    .anchor(0.5f, 0.5f)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.shield_respond))
                            );
                        }
                        else
                        {
                            myPosition = mMap.addMarker(new MarkerOptions()
                                    .position(carmarker).title("Your Position").snippet("")
                                    .anchor(0.5f, 0.5f)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.shield))
                            );
                        }

                    }
                    else if (appOperator.CurrentRole == appOperator.ROLE_USER)
                    {
                        myPosition = mMap.addMarker(new MarkerOptions()
                                .position(carmarker).title("You").snippet("Click For Help")
                                .anchor(0.5f, 0.5f)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.shield))
                        );
                    }
                    myPosition.showInfoWindow();

                    if (!appOperator.MapHasBeenMoved)
                    {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carmarker, 16.0f));
                    }
                    /**********************************************************************
                     * We have manually marked a place to go.
                     **********************************************************************/
                    if (appOperator.CustomPosition != null)
                    {
                        mapMarkers.add(mMap.addMarker(new MarkerOptions()
                                .position(appOperator.CustomPosition)
                                .title("Custom location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
                        );
                    }
                    regularUIFunctions();
                }

                new connectTask().execute("");

                h.postDelayed(this, delay);
            }
        }, delay);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        mMap.setOnMarkerClickListener(this);

        mMap.setOnMapLongClickListener(this);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                Context context = getApplicationContext(); //or getActivity(), YourActivity.this, etc.

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

    }

    @Override
    public void onProviderEnabled(String provider)
    {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    /* Request updates at startup */
    @Override
    protected void onResume()
    {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause()
    {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        localPosition = new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        // TODO Auto-generated method stub

    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker)
    {
        boolean return_value;

        if (marker.equals(myPosition))
        {
                    /* If we're a user */
            if (appOperator.CurrentRole == Operator.ROLE_USER)
            {
                FlagToRequestResponderDetails = true;
                appOperator.CallActionFlag = 1;
                pDialog = new ProgressDialog(MapsActivity.this);
                pDialog.setMessage("Finding Your Safer Agent..");
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
            }
            else /* If we're an agent */
            {
                if (appOperator.ThisAgentHasBeenRequested == 1)
                {
                    appOperator.ThisAgentHasBeenRequested = 0;
                    appOperator.CallAnsweredFlag = 1;
                }
            }
            return_value = true;
        }
        else
        {
            return_value = false;
        }
        return return_value;
    }

    @Override
    public void onMapLongClick(LatLng point)
    {
        appOperator.CustomPosition = point;
    }

    /***************************************************************************
     *                     CLASS: HANDLES THE COMMUNICATIONS
     ***************************************************************************/
    public class connectTask extends AsyncTask<String, String, DjangoInterface>
    {
        @Override
        protected DjangoInterface doInBackground(String... message)
        {
            mDjangoClient = new DjangoInterface(new DjangoInterface.OnMessageReceived()
            {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message)
                {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            /**************************************************
             *      Transfer of data to the TCP object.       *
             **************************************************/
            mDjangoClient.SetOperatorData(appOperator);

            if (appOperator.CurrentlyLoggedIn)
            {
                /* Create current position object */
                appOperator.CurrentPosition = new LatLng(latitude_data, longitude_data);
            }
            mDjangoClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            super.onProgressUpdate(values);
            XMLPullParseHandler parser = new XMLPullParseHandler(appOperator);

            InputStream stream = new ByteArrayInputStream(values[0].getBytes());
            if (ShieldPlacement != null)
            {
                ShieldPlacement.clear();
            }
            ShieldPlacement = parser.parse(stream);

            if (parser.MessageFrom == parser.AGENT_DATA_RESPONSE)
            {
                acknowledgeResponderDialog();
            }
            if (parser.MessageFrom == parser.LOGIN_PASSED_MESSAGE_TYPE)
            {
                appInfoStore.updateUserDetail(appOperator.RealName, appOperator.RealSurname, appOperator.CurrentRole, appOperator.OperatorId, appOperator.Username);
                appOperator.CurrentlyLoggedIn = true;
            }

            if (parser.MessageFrom == parser.AGENT_DATA_FAILED)
            {
                Log.e("MapsActivity","--agent data retrieval failed");
            }
            if (ShieldPlacement.size() > 0)
            {
                runShieldPlacement();
            }
            else
            {
                Log.e("MapsActivity", "--response incorrect");
            }
        }
    }

    /***************************************************************************
     *                   CLASS: DOWNLOADS GOOGLE DIRECTIONS
     ***************************************************************************/
    private class DownloadTask extends AsyncTask<String, Void, String>
    {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url)
        {

            // For storing data from web service
            String data = "";

            try
            {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e)
            {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /***************************************************************************
     *                  CLASS: PARSES GOOGLE DIRECTIONS (JSON)
     ***************************************************************************/
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>>
    {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData)
        {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try
            {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result)
        {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++)
            {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++)
                {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(4);
                lineOptions.color(Color.BLUE);

            }

            if (mapPolyline != null)
            {
                mapPolyline.remove();
            }
            if (lineOptions != null)
            {
                // Drawing polyline in the Google Map for the i-th route
                mapPolyline = mMap.addPolyline(lineOptions);
            }
        }
    }
}

