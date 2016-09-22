package com.safer.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
//import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener
{

    private GoogleMap mMap;
    Handler h = new Handler();
    int delay = 1000;
    private TCPClient mTcpClient;
    private double latitude_data = -26.11846;
    private double longitude_data = 28.00108;
    LatLng localPosition;
    private LocationManager locationManager;
    private String provider;
    public int CurrentRole=0; /* Role User (0) or Agent (1) */
    public int AgentId=1;   /* Agent ID 1 or 2 */
    public int UserId = 1;
    public int call_action_flag=0;
    List<AgentPos> ShieldPlacement = null;
    Button button_left;
    Button button_right;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_user:
                CurrentRole = 0;
                mTcpClient.setCurrentRole(CurrentRole);
                button_left.setText("Call Agent");
                button_right.setText("Place Holder");
                AgentId = 0; //this means I am a user
                setTitle("SAfer User");
                return true;
            case R.id.menu_agent:
                CurrentRole = 1;
                mTcpClient.setCurrentRole(CurrentRole);
                button_left.setText("Accept Call");
                button_right.setText("Go Online");
                AgentId = 1; //default to agent 1
                setTitle("SAfer Agent "+Integer.toString(AgentId));
                return true;
            case R.id.menu_agent_id:
                // custom dialog
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.agent_id_enter);
                dialog.setTitle("Enter Your Agent ID");

                // set the custom dialog components - text, image and button
                TextView text = (TextView) dialog.findViewById(R.id.text);
                text.setText("Please enter your Agent ID:");

                Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
                // if button is clicked, close the custom dialog
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText agentIDText=(EditText)dialog.findViewById(R.id.editText);
                        AgentId = Integer.parseInt(agentIDText.getText().toString());
                        setTitle("SAfer Agent "+Integer.toString(AgentId));
                        dialog.dismiss();
                    }
                });
                dialog.show();
                return true;
            case R.id.menu_user_id:
                // custom dialog
                final Dialog dialog_user = new Dialog(this);
                dialog_user.setContentView(R.layout.agent_id_enter);
                dialog_user.setTitle("Enter Your User ID");

                // set the custom dialog components - text, image and button
                TextView text_user = (TextView) dialog_user.findViewById(R.id.text);
                text_user.setText("Please enter your User ID:");

                Button dialogButton_user = (Button) dialog_user.findViewById(R.id.dialogButtonOK);
                // if button is clicked, close the custom dialog
                dialogButton_user.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText agentIDText=(EditText)dialog_user.findViewById(R.id.editText);
                        UserId = Integer.parseInt(agentIDText.getText().toString());
                        setTitle("SAfer User "+Integer.toString(UserId));
                        dialog_user.dismiss();
                    }
                });
                dialog_user.show();
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

        button_left = (Button)findViewById(R.id.button_left);
        button_right = (Button)findViewById(R.id.button_right);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        localPosition = new LatLng(-34, 151);
        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the locatioin provider -> use
        // default
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);

        //set button actions
        button_left.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (CurrentRole == 0)
                {
                    call_action_flag = 1;
                }
            }
        });

        // API 23: we have to check if ACCESS_FINE_LOCATION and/or ACCESS_COARSE_LOCATION permission are granted
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

            Location location = locationManager.getLastKnownLocation(provider);

            // Initialize the location fields
            if (location != null)
            {
                System.out.println("Provider " + provider + " has been selected.");
                onLocationChanged(location);
            } else
            {

            }
        }

        h.postDelayed(new Runnable(){
            public void run(){
                if ((mTcpClient != null) && (localPosition != null))
                {
                    latitude_data = localPosition.latitude;
                    longitude_data = localPosition.longitude;
                    LatLng carmarker = new LatLng(latitude_data, longitude_data);
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(carmarker).title("My Location"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(carmarker,16.0f));
                    if (ShieldPlacement != null)
                    {
                        int shield_idx=0;
                        for (shield_idx = 0; shield_idx < ShieldPlacement.size(); shield_idx++)
                        {
                            if (ShieldPlacement.get(shield_idx).GetRespondingState())
                            {
                                mMap.addMarker(new MarkerOptions()
                                        .position(ShieldPlacement.get(shield_idx).GetAgentPos())
                                        .title("Shield " + Integer.toString(ShieldPlacement.get(shield_idx).GetAgentID()))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.shield_respond))
                                );
                            }
                            else
                            {
                                mMap.addMarker(new MarkerOptions()
                                        .position(ShieldPlacement.get(shield_idx).GetAgentPos())
                                        .title("Shield " + Integer.toString(ShieldPlacement.get(shield_idx).GetAgentID()))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.shield))
                                );
                            }
                        }
                    }
                }
                new connectTask().execute("");

                if ((ShieldPlacement != null)&&(CurrentRole == 1))
                {
                    int shield_idx = 0;
                    for (shield_idx = 0; shield_idx < ShieldPlacement.size(); shield_idx++)
                    {
                        if (ShieldPlacement.get(shield_idx).GetRespondingState() && (ShieldPlacement.get(shield_idx).GetAgentID() == AgentId))
                        {
                            Toast.makeText(getBaseContext(), "You have been called!!",Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                h.postDelayed(this, delay);
            }
        }, delay);
    }

    public class connectTask extends AsyncTask<String,String,TCPClient>
    {
        @Override
        protected TCPClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            LatLng carmarker = new LatLng(latitude_data, longitude_data);
            mTcpClient.setCurrentRole(CurrentRole);
            if (CurrentRole == 0)
            {
                mTcpClient.setCoordinates(carmarker, UserId);
            }
            else
            {
                mTcpClient.setCoordinates(carmarker, AgentId);
            }
            mTcpClient.setCallAction(call_action_flag);
            call_action_flag = 0;
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            XMLPullParseHandler parser = new XMLPullParseHandler();

            InputStream stream = new ByteArrayInputStream(values[0].getBytes());
            if (ShieldPlacement != null)
            {
                ShieldPlacement.clear();
            }
            ShieldPlacement = parser.parse(stream);
        }
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
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationManager.requestLocationUpdates(provider, 400, 1, this);
        }
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();
        //if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        //        || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        localPosition = new LatLng(location.getLatitude(),location.getLongitude());
        String longitude = "Longitude: " + location.getLongitude();
        Log.e("Long: ", longitude);
        String latitude = "Latitude: " + location.getLatitude();
        Log.e("Lat: ", latitude);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

}

