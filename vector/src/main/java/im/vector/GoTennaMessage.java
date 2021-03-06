package im.vector;






        import android.util.Log;

        import com.gotenna.sdk.exceptions.GTDataMissingException;
        import com.gotenna.sdk.data.messages.GTBaseMessageData;
        import com.gotenna.sdk.data.messages.GTTextOnlyMessageData;
        //import com.gotenna.sdk.sample.managers.ContactsManager;

        import java.util.Date;

/**
 * A model class that represents a sent or received message.
 *
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */

public class GoTennaMessage
{
    //==============================================================================================
    // Class Properties
    //==============================================================================================

    private static final String LOG_TAG = "Message";

    private long senderGID;
    private long receiverGID;
    private Date sentDate;
    public String time;
    public String text;
    public String text_only;
    private MessageStatus messageStatus;
    private String detailInfo;
    private int hopCount;
    public boolean fromHost = true;
    public boolean willDisplayAckConfirmation = true;
    public int messageID;
    public long milisSend = 0;
    public long milisReceived = 0;

    public enum MessageStatus
    {
        SENDING,
        SENT_SUCCESSFULLY,
        ERROR_SENDING
    }

    //==============================================================================================
    // Constructor
    //==============================================================================================

    public GoTennaMessage(long senderGID, long receiverGID, Date sentDate, String text, MessageStatus messageStatus, String detailInfo)
    {
        this.senderGID = senderGID;
        this.receiverGID = receiverGID;
        this.sentDate = sentDate;
        this.text = text;
        this.text_only = text;
        this.messageStatus = messageStatus;
        this.detailInfo = detailInfo;

        this.milisSend = System.currentTimeMillis();
        this.milisReceived = this.milisSend;
    }

    //==============================================================================================
    // Class Instance Methods
    //==============================================================================================

    public long getSenderGID()
    {
        return senderGID;
    }

    public long getReceiverGID()
    {
        return receiverGID;
    }

    public Date getSentDate()
    {
        return sentDate;
    }

    public String getText()
    {
        return text;
    }

    public MessageStatus getMessageStatus()
    {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus)
    {
        this.messageStatus = messageStatus;
    }

    public String getDetailInfo()
    {
        return detailInfo;
    }

    public byte[] toBytes()
    {
        // Use the goTenna SDK's helper classes to format the text data
        // in a way that is easily parsable
        GTTextOnlyMessageData gtTextOnlyMessageData = null;

        try
        {
            gtTextOnlyMessageData = new GTTextOnlyMessageData(text);
        }
        catch (GTDataMissingException e)
        {
            Log.w(LOG_TAG, e);
        }

        if (gtTextOnlyMessageData == null)
        {
            return null;
        }

        return gtTextOnlyMessageData.serializeToBytes();
    }

    public void setHopCount(int hopCount)
    {
        this.hopCount = hopCount;
    }

    public int getHopCount()
    {
        return hopCount;
    }

    //==============================================================================================
    // Static Helper Methods
    //==============================================================================================

    public static GoTennaMessage createReceivedMessage(long senderGID, long receiverGID, String text)
    {
        return new GoTennaMessage(senderGID, receiverGID, new Date(), text, MessageStatus.SENT_SUCCESSFULLY, null);
    }

    public static GoTennaMessage createReadyToSendMessage(long senderGID, long receiverGID, String text)
    {
        return new GoTennaMessage(senderGID, receiverGID, new Date(), text, MessageStatus.SENDING, null);
    }

    public static GoTennaMessage createMessageFromData(GTTextOnlyMessageData gtTextOnlyMessageData)
    {
        GoTennaMessage gm = new GoTennaMessage(gtTextOnlyMessageData.getSenderGID(),
                gtTextOnlyMessageData.getRecipientGID(),
                gtTextOnlyMessageData.getMessageSentDate(),
                gtTextOnlyMessageData.getText(),
                MessageStatus.SENT_SUCCESSFULLY,
                getDetailInfo(gtTextOnlyMessageData));
        gm.setHopCount( gtTextOnlyMessageData.getHopCount() );
        return  gm;
    }

    private static String getDetailInfo(GTBaseMessageData gtBaseMessageData)
    {
        /*
        Contact contact = ContactsManager.getInstance().findContactWithGid(gtBaseMessageData.getSenderGID());
        String senderInitials = gtBaseMessageData.getSenderInitials();

        if (contact != null)
        {
            return contact.getName();
        }
        else if (senderInitials != null)
        {
            return senderInitials;
        }
        else
        {
            return Long.toString(gtBaseMessageData.getSenderGID());
        }

         */

        return "empty";
    }
}
