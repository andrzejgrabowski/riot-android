package im.vector;

import java.util.Vector;

/*
        w paczce przesyłamy:
        packetCount
        Lat
        Lng
        ID (matrix or goTenna)
        displayedName
        networkType (U - ubiquity; G - goTenna)
        */
public class MeshNode
{
    public static class GotennaMessage
    {
        public String text;
        public boolean fromHost = true;
        public boolean receiptConfiermed = false;
    }

    public double lat;
    public double lng;
    public String data;
    public String IP;
    public String ID;
    public String name;
    public boolean visibleOnMap;
    public Vector<GotennaMessage> messages = new Vector<GotennaMessage>();
}
