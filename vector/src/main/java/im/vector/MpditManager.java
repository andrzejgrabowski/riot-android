package im.vector;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
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
import java.util.Vector;

;

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
    public String mDisplayedName = "u28";
    public String mNetworkType = "U";

    public String mLastPacket = "?";

    public String mLastExceptionMessage = "?";
    public String mDeviceIP = "127.0.0.1";


    //przechowywanie danych o innych uzytkownikach
    private Vector<String> mListeners = new Vector<String>();
    private Vector<MeshNode> mNodesUbiquity = new Vector<MeshNode>();
    private Vector<MeshNode> mNodesGotenna = new Vector<MeshNode>();

    // co mBroadcastDelay sekund wysyłane są dane do wszystkich użytkowników sieci
    int mBroadcastDelay = 30;


    // konstruktor
    public MpditManager(){

    }

    public void create()
    {
        try {



            //mSocket.setBroadcast(true);
            //mSocketSend.setBroadcast(true);
        } catch (Exception e) {
            mLastExceptionMessage = e.getMessage();
        }



        mLastExceptionMessage = mDeviceIP;
        /*
        try {
            mDeviceIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            mLastExceptionMessage = mDeviceIP + e.getMessage();


        }*/

        //mListeners.add(mDeviceIP);//"10.3.2.8");//"10.3.2.9");//mDeviceIP);
    }


    public void sendGpsData() {
        sendUdp = true;
    }


    public void sendBroadcast(DatagramSocket SocketSend)
    {
        //String[] separated = currentString.split(":");
        //separated[1] = separated[1].trim();
        String[] s = mDeviceIP.split("\\.");
        if(s.length > 2) {
            String a = s[0] + "." + s[1] + "." + s[2] + ".";

            for (int i = 0; i < 255; i++) {
                try {

                    String message = String.format("%d\t%f\t%f\t", mPacketCount, mLat, mLng) + mID + "\t" + mDisplayedName + "\t" + mNetworkType;
                    byte[] buf = message.getBytes();

                    String address = a + Integer.toString(i);

                    mLastExceptionMessage = address;

                    InetAddress server = InetAddress.getByName(address);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, server, mPort);
                    SocketSend.send(packet);
                } catch (Exception e) {
                    //mLastExceptionMessage = e.getMessage();
                }

            }
        } else mLastExceptionMessage = "wrong device IP " + s.toString();
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
        for(int i=0; i<mListeners.size(); i++)
        {
            try {
                //DatagramSocket SocketSend = new DatagramSocket();
                //SocketSend.setBroadcast(true);

                String message = String.format("%d\t%f\t%f\t",mPacketCount,mLat,mLng) + mID + "\t" + mDisplayedName + "\t" + mNetworkType;
                s = "1";
                byte[] buf = message.getBytes();
                s = "2";
                InetAddress server = InetAddress.getByName(mListeners.get(i));//10.3.2.8");
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

    public void AddMeshNodeData(String data, String ip)
    {
        String[] s = data.split("\t");
        //sprawdzamy typ sieci [5]
        if(s.length < 6)    return;

         /*
        w paczce przesyłamy:
        0 packetCount
        1 Lat
        2 Lng
        3 ID (matrix or goTenna)
        4 displayedName
        5 networkType (U - ubiquity; G - goTenna)
        */



        if(s[5].compareTo("U") == 0)
        {
            // UBIQUITY
            boolean newnode = true;

            for(int i = 0; i<mNodesUbiquity.size(); i++)
            {
                if(ip.compareTo(mNodesUbiquity.get(i).IP) == 0) {
                        newnode = false;
                    mNodesUbiquity.get(i).data = data;
                    mNodesUbiquity.get(i).lat = Double.valueOf(s[1]);
                    mNodesUbiquity.get(i).lng = Double.valueOf(s[2]);
                }
            }

            if(newnode)
            {
                MeshNode node = new MeshNode();
                node.data = data;
                node.lat = Double.valueOf(s[1]);
                node.lng = Double.valueOf(s[2]);
                node.ID = s[3];
                node.name = s[4];
                node.IP = ip;

                mNodesUbiquity.add(node);
            }
        }

        if(s[5].compareTo("G") == 0)
        {
            // GOTENNA
            boolean newnode = true;
            String id = s[3];

            for(int i = 0; i<mNodesGotenna.size(); i++)
            {
                if(id.compareTo(mNodesGotenna.get(i).ID) == 0) {
                    newnode = false;
                    mNodesGotenna.get(i).data = data;
                    mNodesGotenna.get(i).lat = Double.valueOf(s[1]);
                    mNodesGotenna.get(i).lng = Double.valueOf(s[2]);
                }
            }

            if(newnode)
            {
                MeshNode node = new MeshNode();
                node.data = data;
                node.lat = Double.valueOf(s[1]);
                node.lng = Double.valueOf(s[2]);
                node.ID = s[3];
                node.name = s[4];
                node.IP = ip;

                mNodesGotenna.add(node);
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
            mSocketSend = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            mLastPacket = "unable to create socket" + e.getMessage() + e.getCause();
        }

        try {
            //mSocket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int iter =0;
        long startTime = System.currentTimeMillis();


        mLastPacket = "listening...";
        while (mWork) {

            iter++;
            if(System.currentTimeMillis() - startTime > mBroadcastDelay*1000)
            {
                startTime = System.currentTimeMillis();
                sendBroadcast(mSocket);
            }


            if(sendUdp)
                sendUdpData(mSocket);

            // listen
            try {

                if(mSocket != null) {
                    byte[] message = new byte[500];
                    DatagramPacket packet = new DatagramPacket(message, message.length);//, InetAddress.getByName("10.3.2.8"), mPort);
                    //packet.setPort(mPort);

                    //Log.i("UDP client: ", "about to wait to receive");
                    mSocket.setSoTimeout(1000);
                    mSocket.receive(packet);
                    String data = new String(message, 0, packet.getLength());
                    String ip = packet.getAddress().getHostAddress();
                    // dodajemy dane do tablic
                    AddMeshNodeData(data,ip);
                    mLastPacket = data + String.format("  %d %d",iter,mListeners.size());

                    // sprawdzamy czy to jest nowy adres IP
                    boolean contains = false;
                    for(int i=0; i<mListeners.size(); i++)
                    {
                        if(ip.compareTo(mListeners.get(i)) == 0)
                            contains = true;
                    }
                    if(!contains)
                        mListeners.add(ip);



                } else {
                    Thread.sleep(500);
                    mLastPacket = String.format("null socket  %d",iter);
                }
            } catch (Exception e) {
                //Log.e(" UDP client has IOException", "error: ", e);
                //run = false;
                //udpSocket.close();
                mLastPacket = String.format("run %d %d",iter,mListeners.size()) + e.getMessage() + e.getCause();
            }
        }

    }
}
