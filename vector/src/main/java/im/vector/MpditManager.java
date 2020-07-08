package im.vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

import im.vector.util.PreferencesManager;


/*
EIADMOS TEKSTOWA DO KONSOLI MPDIT PRZEZ SIEC GOTENNA
komunikat jest wysyłany do brami, ale jest sformatowany następująco:
formatowanie: TXT \t IP \t SENDER_NAME \t MESSAGE_TEXT

KOMUNIKACJA POMIĘDZY SIECIAMI MESH:

1. Wiadomości przesyłane z UBIQUITY do GOTENNA poprzez bramkę pakietem UDP
wiadomości te sa przesyłane gdy jest połaczenie tylko z siecią Ubiquity poprzez bramkę.
Wiadomości są przechowywane w: mGoTennaMessagesToSendByGateway
są wysyłąne w funkcji: sendGoTennaMessagesByGateWay() wywoływanej w funkcji: run() wątku
nowa wiadomość do wektora mGoTennaMessagesToSendByGateway jest dodawana w funkcji: goTennaSendTextMessage()
formatowanie: TXT \t GID \t SENDER_NAME \t MESSAGE_ID \t MESSAGE_TEXT


2. Wiadomości UDP odbierane z bramki przez sieć UBIQUITY
Wiadomości odczytywane są w funkcji: run()
wiadomość posiada nagłówek: TXT
wiadomość jest przetwarzana w funkcji: AddOrModifyMeshNodeTxtDataFromUdp(data,ip)
formatowanie: TXT \t GID \t SENDER_NAME \t MESSAGE_ID \t MESSAGE_TEXT


3. Wiadomości wysyłane przez bramkę GOTENNA do odbiorcy w sieci UBIQUITY
formatowanie: GTW \t IP \t SENDER_NAME \t MESSAGE_TEXT
???

4. Wiadomości odebrane z bramki GOTENNA przesłane przez użytkownika sieci UBIQUITY
formatowanie: GTW \t IP \t SENDER_NAME \t MESSAGE_TEXT
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
    public static final int CHAT_MODE_GOTENNA = 0;
    public static final int CHAT_MODE_GOTENNA_UBIQUITY = 1;
    public int mGotennaChatUserMode = CHAT_MODE_GOTENNA;

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
    public String mID = "unknown!!!";
    public String mDisplayedName = "unknown!!!";
    public String mNetworkType = "U";

    public String mLastPacket = "?";
    public String mLastPacketError = "?";

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

    // zmian liczby węzłów
    public NodesListener mNodesListener = null;



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

    public void goTennaUpdateDataFromSharedPreferences(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        String gid = sp.getString(PreferencesManager.GOTENNA_SETTINGS_GID_PREFERENCE_KEY, "999666");
        String name = sp.getString(PreferencesManager.GOTENNA_SETTINGS_NAME_PREFERENCE_KEY, "999666");
        String gidMpdit = sp.getString(PreferencesManager.GOTENNA_SETTINGS_MPDIT_PREFERENCE_KEY, "999666");

        String control1 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_1_PREFERENCE_KEY, "145");
        String control2 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_2_PREFERENCE_KEY, "146");
        String control3 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_CONTROL_CHANNEL_3_PREFERENCE_KEY, "147");

        String data1 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_1_PREFERENCE_KEY, "148");
        String data2 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_2_PREFERENCE_KEY, "149");
        String data3 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_3_PREFERENCE_KEY, "151");
        String data4 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_4_PREFERENCE_KEY, "152");
        String data5 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_5_PREFERENCE_KEY, "153");
        String data6 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_6_PREFERENCE_KEY, "154");
        String data7 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_7_PREFERENCE_KEY, "155");
        String data8 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_8_PREFERENCE_KEY, "156");
        String data9 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_9_PREFERENCE_KEY, "157");
        String data10 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_10_PREFERENCE_KEY, "158");
        String data11 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_11_PREFERENCE_KEY, "159");
        String data12 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_12_PREFERENCE_KEY, "160");
        String data13 = sp.getString(PreferencesManager.GOTENNA_SETTINGS_DATA_CHANNEL_13_PREFERENCE_KEY, "161");


        //Toast.makeText(getActivity(), "GID: " + gid, Toast.LENGTH_SHORT).show();

        String power = sp.getString(PreferencesManager.GOTENNA_SETTINGS_POWER_PREFERENCE_KEY, "4");
        String bandwidth = sp.getString(PreferencesManager.GOTENNA_SETTINGS_BANDWIDTH_PREFERENCE_KEY, "4");

        try{
            mGoTennaGID = Long.parseLong(gid);
            mGoTennaUserName = name;
            mGoTennaMpditGID = Long.parseLong(gidMpdit);

            mGoTennaControlChannel[0] = Double.parseDouble(control1);
            mGoTennaControlChannel[1] = Double.parseDouble(control2);
            mGoTennaControlChannel[2] = Double.parseDouble(control3);

            mGoTennaDataChannel[0] = Double.parseDouble(data1);
            mGoTennaDataChannel[1] = Double.parseDouble(data2);
            mGoTennaDataChannel[2] = Double.parseDouble(data3);
            mGoTennaDataChannel[3] = Double.parseDouble(data4);
            mGoTennaDataChannel[4] = Double.parseDouble(data5);
            mGoTennaDataChannel[5] = Double.parseDouble(data6);
            mGoTennaDataChannel[6] = Double.parseDouble(data7);
            mGoTennaDataChannel[7] = Double.parseDouble(data8);
            mGoTennaDataChannel[8] = Double.parseDouble(data9);
            mGoTennaDataChannel[9] = Double.parseDouble(data10);
            mGoTennaDataChannel[10] = Double.parseDouble(data11);
            mGoTennaDataChannel[11] = Double.parseDouble(data12);
            mGoTennaDataChannel[12] = Double.parseDouble(data13);

            mGoTennaPower = Double.parseDouble(power);
            mGoTennaBandwidth = Double.parseDouble(bandwidth);
        } catch (Exception e) {
            //Toast.makeText(getActivity(), "Bład parametrów pracy urządzenia:  " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    // automatyczne łączenie z gotenna: uruchomienie funkcji połącz ponownie
    public void goTennaAutoConnect(Context context) {
        if(goTennaInit(context)) {
            goTennaConnectPrevious();
        }
    }

    public boolean goTennaInit(Context context)
    {
        if(!goTennaNeedInit) return true;

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

    private void addMpditNode(MeshNode node) {
        mNodesMpdit.add(node);
        if(null != mNodesListener)
            mNodesListener.onNodesCountChanged();
    }

    private void addUbiquityNode(MeshNode node) {
        mNodesUbiquity.add(node);
        if(null != mNodesListener)
            mNodesListener.onNodesCountChanged();
    }

    private void addGotennaNode(MeshNode node) {
        mNodesGotenna.add(node);
        if(null != mNodesListener)
            mNodesListener.onNodesCountChanged();
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
        switch(mGotennaChatUserMode) {
            case CHAT_MODE_GOTENNA:
            for (int i = 0; i < mNodesGotenna.size(); i++) {
                MeshNode node = mNodesGotenna.get(i);
                if (gid.compareTo(node.ID) == 0)
                    return node.messages;
            }
            break;

            case CHAT_MODE_GOTENNA_UBIQUITY:
                for (int i = 0; i < mNodesUbiquity.size(); i++) {
                    MeshNode node = mNodesUbiquity.get(i);
                    if (gid.compareTo(node.IP) == 0)
                        return node.messages;
                }
                break;
        }
        return null;
    }



    public void sendGpsGataByUdpBroadcast(DatagramSocket SocketSend)
    {
        if(mID.compareTo("unknown!!!") == 0)
            return;

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
                    // sami do siebie nie wysyłamy
                    if(address.compareTo(mDeviceIP) != 0)
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
        if(mID.compareTo("unknown!!!") == 0)
            return;

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
                if(mListeners.get(i).compareTo(mDeviceIP) != 0)
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

            addGotennaNode(node);//mNodesGotenna.add(node);
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

                addGotennaNode(node);//mNodesGotenna.add(node);
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
    public String AddOrModifyMeshNodeGpsDataFromUdp(String data, String ip)
    {
        String errorCode = "X1";
        String[] s = data.split("\t");
        //sprawdzamy typ sieci [5]
        if(s.length < 6)    return errorCode;

        /*
        w paczce przesyłamy:
        0 packetCount/message type
        1 Lat
        2 Lng
        3 ID (matrix or goTenna)
        4 displayedName
        5 networkType (U - ubiquity; G - goTenna (przez bramkę); M - pojazd MPDIT)
        */

        double lat = -1.0;
        double lng = -1.0;
        boolean godLL = true;

        try {
            lat = Double.parseDouble(s[1]);
        } catch (Exception e) {

            try {
                s[1] = s[1].replace(',','.');
                lat = Double.parseDouble(s[1]);
            } catch (Exception ee) { godLL = false; }
        }


        try {
            lng = Double.parseDouble(s[2]);
        } catch (Exception e) {
            try {
                s[2] = s[2].replace(',','.');
                lng = Double.parseDouble(s[2]);
            } catch (Exception ee) { godLL = false; }
        }

        mLastPacketError = s[1] + " - " + s[2] + " - LAT: " + lat + " LNG: " + lng;


        try {


            errorCode = "U1";
            if (s[5].compareTo("U") == 0) {
                // UBIQUITY
                errorCode = "U2";
                if (mDeviceIP.compareTo(ip) != 0) {
                    errorCode = "U3";
                    // jezeli nie jest to wiadomość od nas to ją analizujemy
                    boolean newnode = true;

                    for (int i = 0; i < mNodesUbiquity.size(); i++) {
                        errorCode = "U4";
                        if (ip.compareTo(mNodesUbiquity.get(i).IP) == 0) {
                            errorCode = "U5";
                            newnode = false;
                            mNodesUbiquity.get(i).data = data;
                            errorCode = "U6";
                            mNodesUbiquity.get(i).lat = lat;
                            errorCode = "U7";
                            mNodesUbiquity.get(i).lng = lng;
                            errorCode = "U8";
                            mNodesUbiquity.get(i).ID = s[3];
                            errorCode = "U9";
                            mNodesUbiquity.get(i).name = s[4];
                            errorCode = "U10";
                            mNodesUbiquity.get(i).visibleOnMap = godLL;
                            errorCode = "U11";
                        }
                    }

                    if (newnode) {
                        errorCode = "U12";
                        MeshNode node = new MeshNode();
                        errorCode = "U13";
                        node.data = data;
                        errorCode = "U14";
                        node.lat = lat;
                        errorCode = "U15";
                        node.lng = lng;
                        errorCode = "U16";
                        node.ID = s[3];
                        errorCode = "U17";
                        node.name = s[4];
                        errorCode = "U18";
                        node.IP = ip;
                        errorCode = "U19";
                        node.visibleOnMap = godLL;

                        // do listy nie dodajemy też bramki
                        errorCode = "U21";
                        if (mGatewayIP.compareTo(ip) != 0) {
                            errorCode = "U22";
                            addUbiquityNode(node);//mNodesUbiquity.add(node);
                        }
                    }
                }
            }

            errorCode = "M1";
            if (s[5].compareTo("M") == 0) {
                // MPDIT pojazd
                String id = s[3];
                int i = 0;

                // sprawdzamy czy to MPDIT
                boolean dataFromMpdit = false;
                for (i = 0; i < mNodesMpdit.size(); i++) {
                    if (id.compareTo(mNodesMpdit.get(i).ID) == 0) {
                        dataFromMpdit = true;
                        mNodesMpdit.get(i).data = data;
                        mNodesMpdit.get(i).lat = lat;
                        mNodesMpdit.get(i).lng = lng;
                        mNodesMpdit.get(i).name = s[4];
                        mNodesMpdit.get(i).visibleOnMap = godLL;
                    }
                }
            }

            errorCode = "G1";
            if (s[5].compareTo("G") == 0) {
                mGatewayIP = ip;
                // GOTENNA
                String id = s[3];
                int i = 0;

                // sprawdzamy czy to MPDIT
                boolean dataFromMpdit = false;
                for (i = 0; i < mNodesMpdit.size(); i++) {
                    if (id.compareTo(mNodesMpdit.get(i).ID) == 0) {
                        dataFromMpdit = true;
                        mNodesMpdit.get(i).data = data;
                        mNodesMpdit.get(i).lat = lat;
                        mNodesMpdit.get(i).lng = lng;
                        mNodesMpdit.get(i).name = s[4];
                        mNodesMpdit.get(i).visibleOnMap = godLL;
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
                            if (dataFromMpdit)
                                mNodesGotenna.get(i).visibleOnMap = false;
                            else
                                mNodesGotenna.get(i).visibleOnMap = false;
                        }
                    }

                    if (newnode) {
                        MeshNode node = new MeshNode();
                        node.data = data;
                        node.lat = lat;
                        node.lng = lng;
                        node.ID = s[3];
                        node.name = s[4];
                        node.IP = ip;
                        if (dataFromMpdit)
                            node.visibleOnMap = false;
                        else
                            node.visibleOnMap = true;

                        addGotennaNode(node);//mNodesGotenna.add(node);
                    }
                }
            }

            //errorCode = "OK! ";

        } catch (Exception e) {
            //String errorCode = "X1";
            mLastPacketError = errorCode + " : " + e.toString();
        }

        return errorCode;
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

        int iter = 0;
        int iterAfter = 0;
        long startTime = System.currentTimeMillis();
        long startTimeGoTenna = System.currentTimeMillis();


        mLastPacket = "listening...";
        while (mWork) {

            // wysylamy dane o GPS przez gniazda do wszystkich w sieci
            if(System.currentTimeMillis() - startTime > mBroadcastDelay*1000) {
                startTime = System.currentTimeMillis();
                sendGpsGataByUdpBroadcast(mSocket);
            }

            // wysylamy dane przez siec goTenna do wszystkich uzytkownikow
            if(System.currentTimeMillis() - startTimeGoTenna > mGoTennaBroadcastDelay*1000) {
                startTimeGoTenna = System.currentTimeMillis();
                if(isGotennaConnected())
                    goTennaSendGPS();
            }


            // wysyłamy dane GPS do znanych użytkowników, ta funkcja jest wywoływana gdy zmieniły się wskazania z GPS
            if(sendUdp)
                sendGpsDataByUdp(mSocket);

            // wysyłamy do bramki wiadomości kierowane do sieci goTenna
            // TO DO !!!
            sendGoTennaMessagesByGateWay(mSocket);

            String errorCode = "A ";

            // listen
            try {

                mSocket.setSoTimeout(1000);
                if(mSocket != null) {


                    iter++;
                    errorCode = "A ";

                    byte[] message = new byte[2500];
                    DatagramPacket packet = new DatagramPacket(message, message.length);//, InetAddress.getByName("10.3.2.8"), mPort);
                    errorCode = "B ";
                    //packet.setPort(mPort);

                    //Log.i("UDP client: ", "about to wait to receive");
                    mSocket.receive(packet);
                    errorCode = "C ";
                    iterAfter++;

                    String ip = packet.getAddress().getHostAddress();
                    errorCode = "D ";

                    // sprawdzamy czy to jest nowy adres IP
                    boolean contains = false;
                    for(int i=0; i<mListeners.size(); i++)
                    {
                        if(ip.compareTo(mListeners.get(i)) == 0)
                            contains = true;
                    }
                    errorCode = "E ";
                    if(!contains && ip.compareTo(mDeviceIP) != 0) {
                        errorCode = "F ";
                        mListeners.add(ip);
                    }
                    errorCode = "G ";

                    String data = new String(message, 0, packet.getLength());
                    errorCode = "H ";
                    mLastPacket = data + String.format("  %d %d %d",iter, iterAfter, mListeners.size());


                    errorCode = "I ";
                    //analizujemy nagłówek
                    if(data.length() >= 3) {
                        errorCode = "J ";
                        String header = data.substring(0, 3);

                        if (header.compareTo("GTW") == 0) {
                            errorCode = "K ";
                            // pakiet od bramki, to możemy pozyskać adres IP bramki
                            mGatewayIP = ip;
                        }

                        if (header.compareTo("GPS") == 0) {
                            errorCode = "L ";
                            // nowe dane GPS, dodajemy dane do tablic
                            // adres IP bramki aktualizujemy jezli sa to dane z sieci gotenna (NetworkType: 'G')
                            errorCode = AddOrModifyMeshNodeGpsDataFromUdp(data, ip);
                        }

                        if (header.compareTo("TXT") == 0) {
                            errorCode = "Z ";
                            // wiadomosc tekstowa, dodajemy dane do tabeli
                            // tutaj dostajemy od bramki GateWayMPDIT wiadomości od sieci MESH goTenna
                            mGatewayIP = ip;
                            AddOrModifyMeshNodeTxtDataFromUdp(data, ip);
                        }
                    }



                    mLastPacket = "!OK!: " + mLastPacket;



                } else {
                    Thread.sleep(500);
                    mLastPacketError = String.format("null socket  %d",iter);
                }
            }
            catch (SocketTimeoutException e) {

            }
            catch (Exception e) {
                //Log.e(" UDP client has IOException", "error: ", e);
                //run = false;
                //udpSocket.close();
               mLastPacketError =  errorCode + String.format("error: %d %d %d",iter, iterAfter, mListeners.size()) + e.getCause()+ e.getMessage();
            }
        }

    }

    private void goTennaSendGPS() {
        // petla po wszystkich
        for (MeshNode node : mNodesGotenna) {
            String messageText = String.format("GPS%f\t%f\t%s",mLat,mLng,mDisplayedName);

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

        addMpditNode(node);//mNodesMpdit.add(node);
        addGotennaNode(node);//mNodesGotenna.add(node);
    }

    public void goTennaSetChatUser(String id, String name, int mode) {
        mGotennaChatUserGID = id;
        mGotennaChatUserName = name;
        mGotennaChatUserMode = mode;
    }

    public boolean isGotennaConnected() {
        if(null == gtConnectionManager)     return false;
        return gtConnectionManager.isConnected();
    }

    private void goTennaIncominMessageGPSfromGateway(GoTennaMessage incomingMessage) {
        // parsujemy wiadomosc
        String data = incomingMessage.text.substring(3);
        String[] s = data.split("\t");

        MeshNode sender = null;
        String ip = s[2];
        String name = s[3];

        // nie analizujemy informacji które pochodzą od nas w przypadku urządzenie obecnego jednoczęśnie w obu sieciach
        if(mDeviceIP.compareTo(ip) == 0)
            return;

        int n = -1;
        for(int i = 0 ; i < mNodesUbiquity.size(); i++ ) {
            MeshNode node = mNodesUbiquity.get(i);
            if (ip.compareTo(node.IP) == 0) {
                n = i;
                sender = node;
                break;
            }
        }

        if(null == sender) {
            MeshNode node = new MeshNode();
            node.name = name;
            node.ID = "?";
            node.lat = 52.20;
            node.lng = 21.05;
            node.IP = ip;
            node.data = "?";
            addUbiquityNode(node);//mNodesUbiquity.add(node);
            sender = node;
        }

        sender.visibleOnMap = true;

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

    private void goTennaIncominMessageGPS(GoTennaMessage incomingMessage) {

        // parsujemy wiadomosc
        String data = incomingMessage.text.substring(3);
        String[] s = data.split("\t");
        if(s.length < 2)    return;

        if(s.length > 2)
        {
            // paczka z Ubiquity przesłąna przez bramkę
            goTennaIncominMessageGPSfromGateway(incomingMessage);
            return;
        }

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
            addGotennaNode(node);//mNodesGotenna.add(node);
            sender = node;
        }

        sender.visibleOnMap = true;
        //sender.data = incomingMessage.text;


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
            addGotennaNode(node);//mNodesGotenna.add(node);

            sender = node;
            n = mNodesGotenna.size() - 1;
        }

        sender.messages.add(incomingMessage);

        if (goTennaMessageListener != null)     goTennaMessageListener.onIncomingMessage(sender.name,incomingMessage.text.substring(3));
    }


    private void goTennaIncominMessageTextByGateway(GoTennaMessage incomingMessage) {
        MeshNode sender = null;
        incomingMessage.fromHost = false;
        Date currentTime = Calendar.getInstance().getTime();
        incomingMessage.time = currentTime.toString();

        // analizujemy wiadomosc po odcieciu naglowka
        String m = incomingMessage.text.substring(3);
        String[] tab = m.split("\t");
        String IP = tab[0];
        String name = tab[1];
        String messageTekst = tab[2];

        incomingMessage.text = "GTW"+messageTekst;
        incomingMessage.text_only = messageTekst;

        String gid = Long.toString(incomingMessage.getSenderGID());
        int n = -1;
        for(int i = 0 ; i < mNodesUbiquity.size(); i++ ) {
            MeshNode node = mNodesUbiquity.get(i);
            if (IP.compareTo(node.IP) == 0) {
                n = i;
                sender = node;
                break;
            }
        }

        if(null == sender) {
            MeshNode node = new MeshNode();
            node.name = name;
            node.ID = gid;
            node.lat = 52.20;
            node.lng = 21.05;
            node.IP = IP;
            node.data = "?";
            node.visibleOnMap = false;
            addUbiquityNode(node);//mNodesUbiquity.add(node);

            sender = node;
            n = mNodesUbiquity.size() - 1;
        }

        sender.messages.add(incomingMessage);

        if (goTennaMessageListener != null)     goTennaMessageListener.onIncomingMessage(sender.name,messageTekst);
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

        if(header.compareTo("GTW") == 0) {
            goTennaIncominMessageTextByGateway(incomingMessage);
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


        // BRAMKA GOTENNA
        // przysałnie przez bramkę gotenna wiadomości do uzytkownika ubiquity
        if(CHAT_MODE_GOTENNA_UBIQUITY == mGotennaChatUserMode)
            if(isGotennaConnected()) {
                // GTW IP \t SENDER_NAME \t MESSAGE_TEXT
                // teraz gid to IP
                if(null == mGotennaSendMessageInteractor) return;

                for(int i = 0 ; i < mNodesUbiquity.size(); i++ ) {
                    MeshNode node = mNodesUbiquity.get(i);
                    if(gid.compareTo(node.IP) == 0) {
                        GoTennaMessage messageToSend = GoTennaMessage.createReadyToSendMessage( mGoTennaGID, mGoTennaMpditGID, messageText);

                        messageToSend.text = "GTW"+node.IP+"\t"+ mDisplayedName +"\t"+messageText;
                        messageToSend.time = currentTime.toString();
                        node.messages.add(messageToSend);

                        mGotennaSendMessageInteractor.sendMessage(messageToSend, GOTENNA_WILL_ENCRYPT_MESSAGES,
                                new GotennaSendMessageInteractor.SendGoTennaMessageListener() {
                                    @Override
                                    public void onMessageResponseReceived()   {
                                        if (goTennaMessageListener != null) {
                                            goTennaMessageListener.onMessageResponseReceived();
                                            //messageToSend.milisReceived = System.currentTimeMillis();
                                        }
                                    }
                                });
                        return;
                    }

                }

            return;
        }


        // BRAMKA UDP
        // przesyłanie do użytkownika gotenna wiadmości
        if(!isGotennaConnected()) {
            GoTennaMessage messageToSend = GoTennaMessage.createReadyToSendMessage( mGoTennaGID,
                    Long.parseLong(gid),
                    messageText);
            messageToSend.time = currentTime.toString();
            messageToSend.messageID = mLastGotennaMessageIdSentByGateway;
            // formatowanie TXT \t GID \t SENDER_NAME \t MESSAGE_ID \t MESSAGE_TEXT
            messageToSend.text = String.format("TXT\t%d\t%s\t%d\t%s",gid,mDisplayedName,messageToSend.messageID,messageText);
            mGoTennaMessagesToSendByGateway.add(messageToSend);
            mLastGotennaMessageIdSentByGateway++;

            return;
        }


        // BEZPOSREDNIO - bez bramki
        // przesyłanie bezposrednio wiadomości do uzytkownika gotenna (z pominięciem bramki)
        if(null == mGotennaSendMessageInteractor) return;

        for(int i = 0 ; i < mNodesGotenna.size(); i++ ) {
            MeshNode node = mNodesGotenna.get(i);
            if(gid.compareTo(node.ID) == 0) {
                GoTennaMessage messageToSend = GoTennaMessage.createReadyToSendMessage( mGoTennaGID, Long.parseLong(gid), messageText);

                messageToSend.text = "TXT"+messageText;
                messageToSend.time = currentTime.toString();
                node.messages.add(messageToSend);

                mGotennaSendMessageInteractor.sendMessage(messageToSend, GOTENNA_WILL_ENCRYPT_MESSAGES,
                        new GotennaSendMessageInteractor.SendGoTennaMessageListener() {
                            @Override
                            public void onMessageResponseReceived()   {
                                if (goTennaMessageListener != null) {
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

    //==============================================================================================
    // Listener Interface
    //==============================================================================================

    public interface NodesListener
    {
        void onNodesCountChanged();
    }
}
