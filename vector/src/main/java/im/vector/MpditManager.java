package im.vector;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;



import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.connection.BluetoothAdapterManager;
import com.gotenna.sdk.connection.BluetoothAdapterManager.BluetoothStatus;
import com.gotenna.sdk.connection.GTConnectionError;
import com.gotenna.sdk.connection.GTConnectionManager;
import com.gotenna.sdk.connection.GTConnectionState;
import com.gotenna.sdk.data.GTCommand;
import com.gotenna.sdk.data.GTCommandCenter;
import com.gotenna.sdk.data.GTDeviceType;
import com.gotenna.sdk.data.GTError;
import com.gotenna.sdk.data.GTErrorListener;
import com.gotenna.sdk.data.GTResponse;
import com.gotenna.sdk.data.frequencies.FrequencySlot;
import com.gotenna.sdk.data.frequencies.FrequencySlot.Bandwidth;
import com.gotenna.sdk.data.frequencies.GTFrequencyChannel;
import com.gotenna.sdk.data.user.UserDataStore;
import com.gotenna.sdk.exceptions.GTInvalidFrequencyChannelException;
import com.gotenna.sdk.frequency.PowerLevel;
import com.gotenna.sdk.frequency.SetFrequencySlotInfoInteractor;

import com.gotenna.sdk.responses.SystemInfoResponseData;


/*
KOMUNIKACJA POMIĘDZY SIECIAMI MESH:

1. Wiadomości przesyłane z UBIQUITY do GOTENNA poprzez bramkę pakietem UDP
wiadomości te sa przesyłane gdy jest połaczenie tylko z siecią Ubiquity poprzez bramkę.
Wiadomości są przechowywane w: mGoTennaMessagesToSendByGateway
są wysyłąne w funkcji: sendGoTennaMessagesByGateWay() wywoływanej w funkcji: run() wątku
nowa wiadomość do wektora mGoTennaMessagesToSendByGateway jest dodawana w funkcji: goTennaSendTextMessage()
formatowanie: TXT \t GID \t MESSAGE_ID \t MESSAGE_TEXT


2. Wiadomości UDP odbierane z bramki przez sieć UBIQUITY
Wiadomości odczytywane są w funkcji: run()
wiadomość posiada nagłówek: TXT
wiadomość jest przetwarzana w funkcji: AddOrModifyMeshNodeTxtDataFromUdp(data,ip)
formatowanie: TXT \t GID \t SENDER_NAME \t MESSAGE_ID \t MESSAGE_TEXT


3. Wiadomości wysyłane przez bramkę GOTENNA do odbiorcy w sieci UBIQUITY
formatowanie: GTW \t IP \t SENDER_NAME \t MESSAGE_TEXT
???

4. Wiadomości odebrane z bramki GOTENNA przesłane przez użytkownika sieci UBIQUITY
???
 */

public class MpditManager implements LocationListener, Runnable, GTConnectionManager.GTConnectionListener, GoTennaIncomingMessagesManager.IncomingMessageListener {

    // goTenna
    private static final String GOTENNA_APP_TOKEN = "RgIJCQMNEEQVQxlBUAsbHxwBQldHUlgDB0NSAxdRHx4LAwtZRFgLVw4DR1gcXgQE";
    private static final boolean GOTENNA_WILL_ENCRYPT_MESSAGES = true; // Can optionally encrypt messages using SDK
    private UserDataStore userDataStore = null;
    private BluetoothAdapterManager bluetoothAdapterManager = null;
    private GTConnectionManager gtConnectionManager = null;
    private GTCommandCenter gtCommandCenter = null;
    private SetFrequencySlotInfoInteractor setFrequencySlotInfoInteractor = null;
    public boolean goTennaNeedInit = true;
    public boolean goTennaNeedConnect = true;
    public boolean goTennaHasPreviousConnectionData = false;
    public GoTennaTechnicalMessageListener goTennaTechnicalMessageListener = null;
    public GoTennaMessageListener goTennaMessageListener = null;
    private GotennaSendMessageInteractor mGotennaSendMessageInteractor = null;

    public double[] mGoTennaControlChannel = new double[3];
    public double[] mGoTennaDataChannel = new double[13];
    public double mGoTennaPower = 4;
    public double mGoTennaBandwidth = 11.8;
    public String mGoTennaUserName = "username";
    public long mGoTennaGID = 999666;
    public long mGoTennaMpditGID = 666999;

    public String mGotennaChatUserGID = "?";
    public String mGotennaChatUserName = "?";

    /**
     * Byte limit on text to make sure we don't go over goTenna's payload limit.
     * Made the limit here lower than the actual limit since text messages get inflated
     * by a few bytes when serialized later.
     */
    public static final int GOTENNA_MESSAGE_BYTE_LIMIT = 200;//227;



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
    public String mLastExceptionGoTennaMessage = "?";
    public String mGoTennaLastMessage = "?";
    public String mGoTennaStatus = "?";
    public boolean mGoTennaHasNewMessage = false;

    public String mDeviceIP = "127.0.0.1";
    public String mGatewayIP = "127.0.0.1";

    // przechowywanie danych dotyczących wiadomości wysyłanych przez bramkę UDP do sieci gotenna
    private Vector<GoTennaMessage> mGoTennaMessagesToSendByGateway = new Vector<GoTennaMessage>();
    // do każdej wiadomości dodwany jest ID, serwer potem potwierdza odebranie danej wiadomości
    int mLastGotennaMessageIdSentByGateway = 0;

    //przechowywanie danych o innych uzytkownikach
    private Vector<String> mListeners = new Vector<String>();
    private Vector<MeshNode> mNodesUbiquity = new Vector<MeshNode>();
    private Vector<MeshNode> mNodesGotenna = new Vector<MeshNode>();
    private Vector<MeshNode> mNodesMpdit = new Vector<MeshNode>();

    // co mBroadcastDelay sekund wysyłane są dane do wszystkich użytkowników sieci
    int mBroadcastDelay = 30;
    int mGoTennaBroadcastDelay = 60;




    // konstruktor
    public MpditManager(){



    }


    public void goTennaAddMessage(String message)
    {
        mGoTennaLastMessage = message;
        mGoTennaHasNewMessage = true;

        if(goTennaTechnicalMessageListener != null)
            goTennaTechnicalMessageListener.onNewGotennaTechnicalMessage();
    }

    public boolean goTennaInit(Context context)
    {
        // goTenna
        try {
            GoTenna.setApplicationToken(context, GOTENNA_APP_TOKEN);
            GoTennaIncomingMessagesManager.getInstance().startListening();
            GoTennaIncomingMessagesManager.getInstance().addIncomingMessageListener(this);

            //userDataStore = UserDataStore.getInstance();
            bluetoothAdapterManager = BluetoothAdapterManager.getInstance();
            gtConnectionManager = GTConnectionManager.getInstance();
            gtCommandCenter = GTCommandCenter.getInstance();
            setFrequencySlotInfoInteractor = new SetFrequencySlotInfoInteractor();

            gtConnectionManager.addGtConnectionListener(this);

            mGotennaSendMessageInteractor = new GotennaSendMessageInteractor();
        } catch (Exception e) {
            mLastExceptionGoTennaMessage = e.getMessage();
            return false;
        }

        mLastExceptionGoTennaMessage = "inicjalizacja powiodła się";
        goTennaNeedInit = false;

        return true;
    }


    private void goTennaSendSetGidCommand(String username, long gid)
    {
        // The UserDataStore automatically saves the user's basic info after setGoTennaGID is called
        gtCommandCenter.setGoTennaGID(gid, username, new GTCommand.GTCommandResponseListener()
        {
            @Override
            public void onResponse(GTResponse response)
            {

                if (response.getResponseCode() == GTResponse.GTCommandResponseCode.POSITIVE)
                {
                    //view.showSetGidSuccessMessage();
                    //goTennaAddMessage("Konfiguracja GID zakończyła się powodzeniem!");
                }
                else
                {
                    //view.showSetGidFailureMessage();
                    goTennaAddMessage("Błąd konfiguracji GID");
                }
            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                //if (view != null)
                {
                    //view.showSetGidFailureMessage();
                    goTennaAddMessage("Błąd konfiguracji GID");
                }
            }
        });
    }

    public boolean goTennaTest()
    {
        // Send an echo command to the goTenna to flash the LED light
        gtCommandCenter.sendEchoCommand(new GTCommand.GTCommandResponseListener()
        {
            @Override
            public void onResponse(GTResponse response)
            {
                switch (response.getResponseCode())
                {
                    case POSITIVE:
                        //view.showEchoSuccessMessage();
                        goTennaAddMessage("Test goTenna zakończony pomyślnie!");
                        break;
                    case NEGATIVE:
                        //view.showEchoNackMessage();
                        goTennaAddMessage("Błąd testu goTenna (NACK)");
                        break;
                    case ERROR:
                        //view.showEchoErrorMessage();
                        goTennaAddMessage("Błąd testu goTenna");
                        break;
                }
            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                //view.showEchoErrorMessage();
                goTennaAddMessage("Błąd testu goTenna");
            }
        });
        return true;
    }

    public boolean goTennaGetSystemInfo() {
        GTCommandCenter.getInstance().sendGetSystemInfo(new GTCommandCenter.GTSystemInfoResponseListener()
        {
            @Override
            public void onResponse(SystemInfoResponseData systemInfoResponseData)
            {
                // This is where you could retrieve info such at the goTenna's battery level and current firmware version
                //view.showSystemInfo(systemInfoResponseData);
                goTennaAddMessage("Status urządzenia odebrany");

                int batteryLevel = systemInfoResponseData.getBatteryLevelAsPercentage();
                String info = String.format("Stan baterii: %d %%",batteryLevel);

                String antena = systemInfoResponseData.getAntennaQuality().toString();
                info += "\n Antena: " + antena;

                info += "\n Firmware: " + systemInfoResponseData.getFirmwareVersion().toString();

                info += "\n Nr seryjny: " + systemInfoResponseData.getGoTennaSerialNumber();

                info += "\n Ostatni błąd: " + mLastExceptionGoTennaMessage;

                mGoTennaStatus = info;

                if(goTennaTechnicalMessageListener != null)
                    goTennaTechnicalMessageListener.onNewGotennaStatusMessage();

            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                //view.showSystemInfoErrorMessage();
                goTennaAddMessage("Błąd pobierania statusu urządzenia");
            }
        });

        return true;
    }


    public boolean goTennaDisconnect() {
        // There is another method you can use, GTConnectionManager.getInstance().disconnectWithRetry();
        // That method will disconnect us from the current goTenna and immediately start scanning for another goTenna
        // Chances are we will re-connect to the goTenna we were just connected to, but it is helpful for clearing up
        // potential connection issues or performing other business logic.
        gtConnectionManager.disconnect();

        goTennaNeedConnect = true;

        return true;
    }

    public boolean goTennaConnectPrevious() {

        goTennaCheckBluetoothStatus();

        return true;
    }

    public boolean goTennaConnectNew() {

        // Clear old connected Bluetooth MAC address of previous goTenna we were connected to
        gtConnectionManager.clearConnectedGotennaAddress();

        goTennaCheckBluetoothStatus();

        return true;
    }



    private void goTennaCheckBluetoothStatus()
    {
        BluetoothStatus bluetoothStatus = bluetoothAdapterManager.getBluetoothStatus();

        switch (bluetoothStatus)
        {
            case SUPPORTED_AND_ENABLED:
                //view.showGotennaDeviceTypeSelectionDialog();
                try
                {
                    gtConnectionManager.scanAndConnect(GTDeviceType.PRO);
                    //view.showScanningInProgressDialog();
                    //view.startTimeoutCountdown();
                    goTennaAddMessage("Rozpoczęcie skanowania w poszukiwaniu urządzenia");
                }
                catch (UnsupportedOperationException e)
                {
                    //view.showUnsupportedDeviceWarning(e.getLocalizedMessage());
                    goTennaAddMessage("Błąd skanowania goTenna: " + e.getLocalizedMessage());
                }
                break;
            case SUPPORTED_NOT_ENABLED:
                //view.requestEnableBluetooth();
                goTennaAddMessage("Moduł bluetooth nie jest włączony!");
                break;
            case NOT_SUPPORTED:
                //view.showBluetoothNotSupportedMessage();
                goTennaAddMessage("Brak wsparcia dla komunikacji typu bluetooth");
                break;
        }
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

    public Vector<MeshNode> getUbiquityNodes()
    {
        return mNodesUbiquity;
    }

    public Vector<MeshNode> getGotennaNodes()
    {
        return mNodesGotenna;
    }

    public Vector<MeshNode> getMpditNodes()
    {
        return mNodesMpdit;
    }

    public Vector<GoTennaMessage> getGotennaMessages(String gid)
    {
        for(int i = 0 ; i < mNodesGotenna.size(); i++ ) {
            MeshNode node = mNodesGotenna.get(i);
            if(gid.compareTo(node.ID) == 0)
                return node.messages;
        }

        return null;
    }



    public void sendGpsGataByUdpBroadcast(DatagramSocket SocketSend)
    {
        //String[] separated = currentString.split(":");
        //separated[1] = separated[1].trim();
        String[] s = mDeviceIP.split("\\.");
        if(s.length > 2) {
            String a = s[0] + "." + s[1] + "." + s[2] + ".";

            for (int i = 0; i < 255; i++) {
                try {

                    //String message = String.format("%d\t%f\t%f\t", mPacketCount, mLat, mLng) + mID + "\t" + mDisplayedName + "\t" + mNetworkType;
                    String message = String.format("GPS\t%f\t%f\t", mLat, mLng) + mID + "\t" + mDisplayedName + "\t" + mNetworkType;
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



    public void sendGoTennaMessagesByGateWay(DatagramSocket SocketSend)
    {
        for (GoTennaMessage m : mGoTennaMessagesToSendByGateway) {
            if(m.getMessageStatus() == GoTennaMessage.MessageStatus.SENDING) {
                try {
                    byte[] buf = m.getText().getBytes();
                    InetAddress server = InetAddress.getByName(mGatewayIP);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, server, mPort);
                    SocketSend.send(packet);

                    m.setMessageStatus(GoTennaMessage.MessageStatus.SENT_SUCCESSFULLY);
                } catch (Exception e) {}
            }
        }
        //TO DO !!!
    }

    public void sendGpsDataByUdp(DatagramSocket SocketSend)
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

                //String message = String.format("%d\t%f\t%f\t",mPacketCount,mLat,mLng) + mID + "\t" + mDisplayedName + "\t" + mNetworkType;
                String message = String.format("GPS\t%f\t%f\t",mLat,mLng) + mID + "\t" + mDisplayedName + "\t" + mNetworkType;
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

    public boolean goTennaRemoveNode(String gid)
    {
        for(int i = 0; i<mNodesGotenna.size(); i++)
        {
            if(gid.compareTo(mNodesGotenna.get(i).ID) == 0) {

                mNodesGotenna.remove(i);
                return true;
            }
        }
        return false;
    }

    public void goTennaAddUpdateNode(String id, String name)
    {
        for(int i = 0; i<mNodesGotenna.size(); i++)
        {
            if(id.compareTo(mNodesGotenna.get(i).ID) == 0) {
                String newName = String.format("%s",name);
                mNodesGotenna.get(i).name = newName;
                return;
            }
        }

        {
            MeshNode node = new MeshNode();
            node.data = "?";
            node.lat = 52.20;
            node.lng = 21.05;
            node.ID = id;
            node.name = name;
            node.IP = "127.0.0.1";
            node.visibleOnMap = false;

            mNodesGotenna.add(node);
        }
    }


    // otrzymaliśmy wiadomość z sieci goTenna przekazaną przez bramkę
    public void AddOrModifyMeshNodeTxtDataFromUdp(String data, String ip)
    {
        // format wiadomości
        // TXT \t GID \t SENDER_NAME \t MESSAGE_ID \t MESSAGE_TEXT
        //String data = fulldata.substring(3);
        String[] s = data.split("\t");
        if(s.length < 5)    return;

        // GOTENNA
        String id = s[1];
        String sender_name = s[2];
        String message_id = s[3];
        String message_text = s[4];

        {
            boolean newnode = true;
            MeshNode node = null;

            for (int i = 0; i < mNodesGotenna.size(); i++) {
                if (id.compareTo(mNodesGotenna.get(i).ID) == 0) {
                    newnode = false;
                    node = mNodesGotenna.get(i);
                    //mNodesGotenna.get(i).data = data;
                }
            }

            if (newnode) {
                node = new MeshNode();
                node.data = data;
                node.ID = id;
                node.name = sender_name;
                node.IP = ip;
                node.visibleOnMap = false;

                mNodesGotenna.add(node);
            }

            if(null != node) {
                GoTennaMessage m = GoTennaMessage.createReceivedMessage(Long.parseLong(id), mGoTennaGID, message_text);
                m.fromHost = false;
                m.messageID = Integer.parseInt(message_id);
                Date currentTime = Calendar.getInstance().getTime();
                m.time = currentTime.toString();
                node.messages.add(m);
            }
        }

        if (goTennaMessageListener != null)     goTennaMessageListener.onIncomingMessage(sender_name,message_text);
    }
    public void AddOrModifyMeshNodeGpsDataFromUdp(String data, String ip)
    {
        String[] s = data.split("\t");
        //sprawdzamy typ sieci [5]
        if(s.length < 6)    return;

        /*
        w paczce przesyłamy:
        0 packetCount/message type
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
                    mNodesUbiquity.get(i).ID = s[3];
                    mNodesUbiquity.get(i).name = s[4];
                    mNodesUbiquity.get(i).visibleOnMap = true;
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
                node.visibleOnMap = true;

                mNodesUbiquity.add(node);
            }
        }

        if(s[5].compareTo("G") == 0)
        {
            // GOTENNA
            String id = s[3];
            int i = 0;

            // sprawdzamy czy to MPDIT
            boolean dataFromMpdit = false;
            for (i = 0; i < mNodesMpdit.size(); i++) {
                if (id.compareTo(mNodesMpdit.get(i).ID) == 0) {
                    dataFromMpdit = true;
                    mNodesMpdit.get(i).data = data;
                    mNodesMpdit.get(i).lat = Double.valueOf(s[1]);
                    mNodesMpdit.get(i).lng = Double.valueOf(s[2]);
                    mNodesMpdit.get(i).name = s[4];
                    mNodesMpdit.get(i).visibleOnMap = true;
                }
            }


            //if(!dataFromMpdit)
            {
                boolean newnode = true;


                for (i = 0; i < mNodesGotenna.size(); i++) {
                    if (id.compareTo(mNodesGotenna.get(i).ID) == 0) {
                        newnode = false;
                        mNodesGotenna.get(i).data = data;
                        mNodesGotenna.get(i).lat = Double.valueOf(s[1]);
                        mNodesGotenna.get(i).lng = Double.valueOf(s[2]);
                        //mNodesGotenna.get(i).ID = s[3];
                        mNodesGotenna.get(i).name = s[4];
                        if(dataFromMpdit)
                            mNodesGotenna.get(i).visibleOnMap = false;
                        else
                            mNodesGotenna.get(i).visibleOnMap = false;
                    }
                }

                if (newnode) {
                    MeshNode node = new MeshNode();
                    node.data = data;
                    node.lat = Double.valueOf(s[1]);
                    node.lng = Double.valueOf(s[2]);
                    node.ID = s[3];
                    node.name = s[4];
                    node.IP = ip;
                    if(dataFromMpdit)
                        node.visibleOnMap = false;
                    else
                        node.visibleOnMap = true;

                    mNodesGotenna.add(node);
                }
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
        long startTimeGoTenna = System.currentTimeMillis();


        mLastPacket = "listening...";
        while (mWork) {

            iter++;
            // wysylamy dane o GPS przez gniazda do wszystkich w sieci
            if(System.currentTimeMillis() - startTime > mBroadcastDelay*1000) {
                startTime = System.currentTimeMillis();
                sendGpsGataByUdpBroadcast(mSocket);
            }

            // wysylamy dane przez siec goTenna do wszystkich uzytkownikow
            if(System.currentTimeMillis() - startTimeGoTenna > mGoTennaBroadcastDelay*1000) {
                startTimeGoTenna = System.currentTimeMillis();
                if(isGotennaConected())
                    goTennaSendGPS();
            }


            // wysyłamy dane GPS do znanych użytkowników, ta funkcja jest wywoływana gdy zmieniły się wskazania z GPS
            if(sendUdp)
                sendGpsDataByUdp(mSocket);

            // wysyłamy do bramki wiadomości kierowane do sieci goTenna
            // TO DO !!!
            sendGoTennaMessagesByGateWay(mSocket);

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

                    //analizujemy nagłówek
                    String header = data.substring(0,3);

                    if(header.compareTo("GTW") == 0) {
                        // pakiet od bramki, to możemy pozyskać adres IP bramki
                         mGatewayIP = ip;
                    }

                    if(header.compareTo("GPS") == 0) {
                        // nowe dane GPS, dodajemy dane do tablic
                        AddOrModifyMeshNodeGpsDataFromUdp(data,ip);
                    }

                    if(header.compareTo("TXT") == 0) {
                        // wiadomosc tekstowa, dodajemy dane do tabli
                        // tutaj dostajemy od bramki GateWayMPDIT wiadomości od sieci MESH goTenna
                        AddOrModifyMeshNodeTxtDataFromUdp(data,ip);
                    }

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

    private void goTennaSendGPS() {
        // petla po wszystkich
        for (MeshNode node : mNodesGotenna) {
            String messageText = String.format("GPS%f\t%f",mLat,mLng);

            GoTennaMessage messageToSend = GoTennaMessage.createReadyToSendMessage( mGoTennaGID,
                    Long.parseLong(node.ID),
                    messageText);

            mGotennaSendMessageInteractor.sendMessage(messageToSend, GOTENNA_WILL_ENCRYPT_MESSAGES,
                    new GotennaSendMessageInteractor.SendGoTennaMessageListener()
                    {
                        @Override
                        public void onMessageResponseReceived()
                        {
                            if (goTennaMessageListener != null)
                            {
                                //view.showMessages(createMessageViewModels());
                                //goTennaMessageListener.onMessageResponseReceived();
                            }
                        }
                    });
        }
    }


    //==============================================================================================
    // Gotenna: Private and public Class Instance Methods
    //==============================================================================================

    public void goTennaUpdateConnectedParameters()
    {
        if(goTennaNeedConnect)  return;
        onGotennaConnected();
    }

    private void onGotennaConnected()
    {
        FrequencySlot frequencySlot = new FrequencySlot();

        /*
        public double[] mGoTennaControlChannel = new double[3];
    public double[] mGoTennaDataChannel = new double[13];
    public double mGoTennaPower = 11.8;
    public double mGoTennaBandwidth = 4;
    public String mGoTennaUserName = "username";
    public long mGoTennaGID = 999666;
    public long mGoTennaMpditGID = 666999;
         */

        frequencySlot.setPowerLevel(PowerLevel.ONE_HALF);

        if(1.0 == mGoTennaPower)    frequencySlot.setPowerLevel(PowerLevel.ONE);
        if(2.0 == mGoTennaPower)    frequencySlot.setPowerLevel(PowerLevel.TWO);
        if(4.0 == mGoTennaPower)    frequencySlot.setPowerLevel(PowerLevel.FOUR);
        if(5.0 == mGoTennaPower)    frequencySlot.setPowerLevel(PowerLevel.FIVE);

        frequencySlot.setBandwidth(Bandwidth._11_80_kHZ);
        if(4.84 == mGoTennaBandwidth)    frequencySlot.setBandwidth(Bandwidth._4_84_kHZ);
        if(7.28 == mGoTennaBandwidth)    frequencySlot.setBandwidth(Bandwidth._7_28_kHZ);


        try
        {
            // There is a default set of frequencies that a slot get populated with if you do not know what to use
            List<GTFrequencyChannel> frequencyChannels = new ArrayList<>();
            //frequencyChannels.add(new GTFrequencyChannel(150000000, true));
            //frequencyChannels.add(new GTFrequencyChannel(151000000, false));



            for(int i = 0; i < mGoTennaControlChannel.length; i++)
                frequencyChannels.add(new GTFrequencyChannel(mGoTennaControlChannel[i], true));
            for(int i = 0; i < mGoTennaDataChannel.length; i++)
                frequencyChannels.add(new GTFrequencyChannel(mGoTennaDataChannel[i], false));

            frequencySlot.setFrequencyChannels(frequencyChannels);
        }
        catch (GTInvalidFrequencyChannelException e)
        {
            e.printStackTrace();
            goTennaAddMessage("Nie prawidłowa konfiguracja częstotliwości goTenna");
            mLastExceptionGoTennaMessage = "Nie prawidłowa konfiguracja częstotliwości goTenna";
        }

        setFrequencySlotInfoInteractor.setFrequencySlotInfoOnGotenna(frequencySlot, new SetFrequencySlotInfoInteractor.SetFrequencySlotInfoListener()
        {
            @Override
            public void onInfoStateChanged(@NonNull SetFrequencySlotInfoInteractor.SetInfoState setInfoState)
            {

                switch (setInfoState)
                {
                    case NON_IDLE_STATE_ERROR:
                        goTennaAddMessage("Błąd konfiguracji: urządzenie zajęte");
                        mLastExceptionGoTennaMessage = "Błąd konfiguracji: urządzenie zajęte";
                        break;
                    case NOT_CONNECTED_ERROR:
                        goTennaAddMessage("Błąd konfiguracji: brak połączenia");
                        mLastExceptionGoTennaMessage = "Błąd konfiguracji: brak połączenia";
                        break;
                    case SET_POWER_LEVEL_ERROR:
                        goTennaAddMessage("Błąd konfiguracji mocy");
                        mLastExceptionGoTennaMessage = "Błąd konfiguracji mocy";
                        break;
                    case SET_BANDWIDTH_BITRATE_ERROR:
                        goTennaAddMessage("Błąd konfiguracji szerokości pasma");
                        mLastExceptionGoTennaMessage = "Błąd konfiguracji szerokości pasma";
                        break;
                    case SET_FREQUENCIES_ERROR:
                        //view.showErrorSettingFrequenciesWarning();
                        goTennaAddMessage("Błąd konfiguracji częstotliwości");
                        mLastExceptionGoTennaMessage = "Błąd konfiguracji częstotliwości";
                        break;
                    case SET_ALL_SUCCESS:
                        //view.showSdkOptionsScreen();
                        goTennaAddMessage("Konfiguracja częstotliwości zakończyła się powodzeniem");
                        break;
                }
            }
        });


        goTennaSendSetGidCommand(mGoTennaUserName, mGoTennaGID);

        goTennaNeedConnect = false;

        if(goTennaTechnicalMessageListener != null)
            goTennaTechnicalMessageListener.onGotennaTechnicalMessageConnected();

        /*view.stopTimeoutCountdown();
        view.dismissScanningProgressDialog();

        GTDeviceType deviceType = GTConnectionManager.getInstance().getDeviceType();

        switch (deviceType)
        {
            case V1:
                view.showSdkOptionsScreen();
                break;
            case MESH:
                findAndSetMeshLocation();
                break;
            case PRO:
                setProFrequencies();
                break;
        }

         */
    }

    public void AddFirstMpditNode(String mpditGID) {
        if(mNodesMpdit.size() > 0)  return;
        MeshNode node = new MeshNode();
        node.name = "MPDIT";
        node.ID = mpditGID;
        node.lat = 52.20;
        node.lng = 21.05;
        node.IP = "127.0.0.1";
        node.data = "?";
        node.visibleOnMap = false;

        mNodesMpdit.add(node);
        mNodesGotenna.add(node);
    }

    public void goTennaSetChatUser(String id, String name) {
        mGotennaChatUserGID = id;
        mGotennaChatUserName = name;
    }

    public boolean isGotennaConected() {
        if(null == gtConnectionManager)     return false;
        return gtConnectionManager.isConnected();
    }

    private void goTennaIncominMessageGPS(GoTennaMessage incomingMessage) {
        MeshNode sender = null;
        String gid = Long.toString(incomingMessage.getSenderGID());
        int n = -1;
        for(int i = 0 ; i < mNodesGotenna.size(); i++ ) {
            MeshNode node = mNodesGotenna.get(i);
            if (gid.compareTo(node.ID) == 0) {
                n = i;
                sender = node;
                break;
            }
        }

        if(null == sender) {
            MeshNode node = new MeshNode();
            node.name = "???";
            node.ID = gid;
            node.lat = 52.20;
            node.lng = 21.05;
            node.IP = "127.0.0.1";
            node.data = "?";
            node.visibleOnMap = false;
            mNodesGotenna.add(node);
            sender = node;
        }

        sender.visibleOnMap = true;
        sender.data = incomingMessage.text;
        // parsujemy wiadomosc
        // TO DO !!!

        String data = incomingMessage.text.substring(3);
        String[] s = data.split("\t");
        if(s.length < 2)    return;
        /*
        w paczce przesyłamy:
        0 Lat
        1 Lng
        */

        try {
            sender.lat = Double.parseDouble(s[0]);
        } catch (Exception e) {}

        try {
            sender.lng = Double.parseDouble(s[1]);
        } catch (Exception e) {}




    }

    private void goTennaIncominMessageText(GoTennaMessage incomingMessage) {
        MeshNode sender = null;
        incomingMessage.fromHost = false;
        Date currentTime = Calendar.getInstance().getTime();
        incomingMessage.time = currentTime.toString();

        String gid = Long.toString(incomingMessage.getSenderGID());
        int n = -1;
        for(int i = 0 ; i < mNodesGotenna.size(); i++ ) {
            MeshNode node = mNodesGotenna.get(i);
            if (gid.compareTo(node.ID) == 0) {
                n = i;
                sender = node;
                break;
            }
        }

        if(null == sender) {
            MeshNode node = new MeshNode();
            node.name = "???";
            node.ID = gid;
            node.lat = 52.20;
            node.lng = 21.05;
            node.IP = "127.0.0.1";
            node.data = "?";
            node.visibleOnMap = false;
            mNodesGotenna.add(node);

            sender = node;
            n = mNodesGotenna.size() - 1;
        }

        sender.messages.add(incomingMessage);

        if (goTennaMessageListener != null)     goTennaMessageListener.onIncomingMessage(sender.name,incomingMessage.text.substring(3));
    }

    @Override
    public void onIncomingMessage(GoTennaMessage incomingMessage) {

        // sprawdzamy rodzaj wiadomosci
        if(incomingMessage.text.length() < 3)
            return;



        String header = incomingMessage.text.substring(0,3);
        //incomingMessage.text = incomingMessage.text.substring(3);
        //if (goTennaMessageListener != null)     goTennaMessageListener.onIncomingMessage(header, incomingMessage.text.substring(3));

        if(header.compareTo("TXT") == 0) {
            goTennaIncominMessageText(incomingMessage);
            return;
        }

        if(header.compareTo("GPS") == 0) {
            goTennaIncominMessageGPS(incomingMessage);
            return;
        }

    }

    public void goTennaSendTextMessage(String gid, String messageText)
    {
        if(messageText.length() >= GOTENNA_MESSAGE_BYTE_LIMIT) {
            // informacja o tym, że wiadomość jest zadługa
            return;
        }
        Date currentTime = Calendar.getInstance().getTime();


        if(!isGotennaConected()) {
            GoTennaMessage messageToSend = GoTennaMessage.createReadyToSendMessage( mGoTennaGID,
                    Long.parseLong(gid),
                    messageText);
            messageToSend.time = currentTime.toString();
            messageToSend.messageID = mLastGotennaMessageIdSentByGateway;
            // formatowanie TXT \t GID \t MESSAGE_ID \t MESSAGE_TEXT
            messageToSend.text = String.format("TXT\t%d\t%d\t%s",gid,messageToSend.messageID,messageText);
            mGoTennaMessagesToSendByGateway.add(messageToSend);
            mLastGotennaMessageIdSentByGateway++;

            return;
        }

        if(null == mGotennaSendMessageInteractor) return;

        for(int i = 0 ; i < mNodesGotenna.size(); i++ ) {
            MeshNode node = mNodesGotenna.get(i);
            if(gid.compareTo(node.ID) == 0) {



                GoTennaMessage messageToSend = GoTennaMessage.createReadyToSendMessage( mGoTennaGID,
                        Long.parseLong(gid),
                        messageText);

                messageToSend.text = "TXT"+messageText;


                messageToSend.time = currentTime.toString();
                node.messages.add(messageToSend);

                //messages.add(messageToSend);
                //view.showMessages(createMessageViewModels());

                mGotennaSendMessageInteractor.sendMessage(messageToSend, GOTENNA_WILL_ENCRYPT_MESSAGES,
                        new GotennaSendMessageInteractor.SendGoTennaMessageListener()
                        {
                            @Override
                            public void onMessageResponseReceived()
                            {
                                if (goTennaMessageListener != null)
                                {
                                    //view.showMessages(createMessageViewModels());
                                    goTennaMessageListener.onMessageResponseReceived();
                                }
                            }
                        });

                return;
            }

        }
    }

    //==============================================================================================
    // GTConnectionListener Implementation
    //==============================================================================================

    // goTenna
    @Override
    public void onConnectionStateUpdated(@NonNull GTConnectionState gtConnectionState) {
        goTennaAddMessage("Stan połączenia zmieniony");

        switch (gtConnectionState)
        {
            case CONNECTED:
                goTennaAddMessage("Gotenna: jest połączenie");
                onGotennaConnected();
                break;
            case DISCONNECTED:
                goTennaAddMessage("Gotenna: połączenie zerwane");
                break;
            case SCANNING:
                goTennaAddMessage("Gotenna: skanowanie");
                break;
            case CONNECTING:
                goTennaAddMessage("Gotenna: łączenie");
                break;

        }
    }

    // goTenna
    @Override
    public void onConnectionError(@NonNull GTConnectionState gtConnectionState, @NonNull GTConnectionError gtConnectionError) {
        //view.stopTimeoutCountdown();
        //view.dismissScanningProgressDialog();
        goTennaAddMessage("Nierozpoznany błąd połączenia");

        switch (gtConnectionError.getErrorState())
        {
            case X_UPGRADE_CHECK_FAILED:
                /*
                    This error gets passed when we failed to check if the device is goTenna X. This
                    could happen due to connectivity issues with the device or error checking if the
                    device has been remotely upgraded.
                 */
                //view.showXCheckError();
                break;
            case NOT_X_DEVICE_ERROR:
                /*
                    This device is confirmed not to be a goTenna X device. Using error.getDetailString()
                    you can pull the serial number of the connected device.
                 */
                //view.showNotXDeviceWarning(error.getDetailString());
                break;
        }
    }




    //==============================================================================================
    // Listener Interface
    //==============================================================================================

    public interface GoTennaTechnicalMessageListener
    {
        void onNewGotennaTechnicalMessage();
        void onGotennaTechnicalMessageConnected();
        void onNewGotennaStatusMessage();
    }

    //==============================================================================================
    // Listener Interface
    //==============================================================================================

    public interface GoTennaMessageListener
    {
        void onMessageResponseReceived();
        void onIncomingMessage(String sender, String text);
    }
}
