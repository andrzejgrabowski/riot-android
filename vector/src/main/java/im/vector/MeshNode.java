package im.vector;

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
    public double lat;
    public double lng;
    public String data;
    public String IP;
    public String ID;
    public String name;
    public boolean visibleOnMap;
}
