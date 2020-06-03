package im.vector;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;

public class MpditManager implements LocationListener, Runnable {

    public double mLat = 52.20;
    public double mLng = 21.05;

    private DatagramSocket mSocket = null;
    private DatagramSocket mSocketSend = null;

    private int mPort = 6666;
    private boolean mWork = true;
    private boolean sendUdp = false;
    //private Thread mThreadSocket = null;
    //private int mPortSend = 6385;

    public int mPacketCount = 0;
    public String mID = "28";
    public String mDisplayedName = "user28";
    public String mNetworkType = "U";

    public String mLastPacket = "?";

    public String mLastExceptionMessage = "?";


    // konstruktor
    public MpditManager(){

    }

    public void create()
    {
        try {

            mSocketSend = new DatagramSocket();

            //mSocket.setBroadcast(true);
            //mSocketSend.setBroadcast(true);
        } catch (Exception e) {
            mLastExceptionMessage = e.getMessage();
        }
    }


    public void sendGpsData() {
        sendUdp = true;
    }

    public void sendUdpData(DatagramSocket SocketSend)
    {
        sendUdp = false;

        // TO DO !!!
        // co 10 s robimy broadcast do wszystkich komputerów w sieci lokalnej

        /*
        w paczce przesyłamy:
        packetCount
        Lat
        Lng
        ID (matrix or goTenna)
        displayedName
        networkType (U - ubiquity; G - goTenna)
        */

        mPacketCount++;
        String s = "0";

        // send
        {
            try {
                //DatagramSocket SocketSend = new DatagramSocket();
                //SocketSend.setBroadcast(true);

                String message = String.format("%d\t%f\t%f\t",mPacketCount,mLat,mLng) + mID + "\t" + mDisplayedName + "\t" + mNetworkType;
                s = "1";
                byte[] buf = message.getBytes();
                s = "2";
                InetAddress server = InetAddress.getByName("10.3.2.8");
                s = "3";
                DatagramPacket packet = new DatagramPacket(buf, buf.length, server, mPort);
                s = "3";
                if(packet == null)
                    s = "null packet";
                if(SocketSend == null)
                    s = "null socket";
                SocketSend.send(packet);
                s = "4";
            }
            catch (PortUnreachableException e)    {
                mLastExceptionMessage = "1_SGD: " + s + " " + e.getMessage() + e.getCause();
            }
            catch (IllegalBlockingModeException e)    {
                mLastExceptionMessage = "2_SGD: " + s + " " + e.getMessage() + e.getCause();
            }
            catch (IllegalArgumentException e)    {
                mLastExceptionMessage = "3_SGD: " + s + " " + e.getMessage() + e.getCause();
            }
            catch (SecurityException e)    {
                mLastExceptionMessage = "4_SGD: " + s + " " + e.getMessage() + e.getCause();
            }
            catch (IOException e)    {
                mLastExceptionMessage = "5_SGD: " + s + " " + e.getMessage() + e.getCause();
            }
            catch (Exception e)    {
                mLastExceptionMessage = "6_SGD: " + s + " " + e.getMessage() + e.getCause();
            }


        }
    }

    @Override
    public void onLocationChanged(Location loc)
    {
        mLat = loc.getLatitude();
        mLng = loc.getLongitude();

        try {
            sendGpsData();
        } catch (Exception e) {
            //e.printStackTrace();
            mLastExceptionMessage = "olc: " + e.getMessage();
        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider)
    {

    }

    @Override
    public void run() {


        try {
            mSocket = new DatagramSocket(mPort);
        } catch (SocketException e) {
            e.printStackTrace();
            mLastPacket = "unable to create socket" + e.getMessage() + e.getCause();
        }

        int iter =0;

        mLastPacket = "listening...";
        while (mWork) {

            iter++;

            if(sendUdp)
                sendUdpData(mSocket);

            // listen
            try {

                if(mSocket != null) {
                    byte[] message = new byte[500];
                    DatagramPacket packet = new DatagramPacket(message, message.length);
                    //Log.i("UDP client: ", "about to wait to receive");
                    mSocket.setSoTimeout(1000);
                    mSocket.receive(packet);
                    mLastPacket = new String(message, 0, packet.getLength());
                    mLastPacket += String.format("  %d",iter);
                    //Log.d("Received text", text);
                } else {
                    Thread.sleep(500);
                    mLastPacket = String.format("null socket  %d",iter);
                }
            } catch (Exception e) {
                //Log.e(" UDP client has IOException", "error: ", e);
                //run = false;
                //udpSocket.close();
                mLastPacket = String.format("run %d ",iter) + e.getMessage() + e.getCause();
            }
        }

    }
}
