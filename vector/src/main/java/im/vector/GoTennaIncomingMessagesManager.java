package im.vector;


        import com.gotenna.sdk.data.GTCommandCenter;
        import com.gotenna.sdk.data.messages.GTBaseMessageData;
        import com.gotenna.sdk.data.messages.GTGroupCreationMessageData;
        import com.gotenna.sdk.data.messages.GTMessageData;
        import com.gotenna.sdk.data.messages.GTTextOnlyMessageData;

        import java.util.ArrayList;

/**
 * A singleton that manages listening for incoming messages from the SDK and parses them into
 * usable data classes.
 */
public class GoTennaIncomingMessagesManager implements GTCommandCenter.GTMessageListener
{
    //==============================================================================================
    // Class Properties
    //==============================================================================================

    private final ArrayList<IncomingMessageListener> incomingMessageListeners;

    //==============================================================================================
    // Singleton Methods
    //==============================================================================================

    private GoTennaIncomingMessagesManager()
    {
        incomingMessageListeners = new ArrayList<>();
    }

    private static class SingletonHelper
    {
        private static final GoTennaIncomingMessagesManager INSTANCE = new GoTennaIncomingMessagesManager();
    }

    public static GoTennaIncomingMessagesManager getInstance()
    {
        return SingletonHelper.INSTANCE;
    }

    //==============================================================================================
    // Class Instance Methods
    //==============================================================================================

    public void startListening()
    {
        GTCommandCenter.getInstance().setMessageListener(this);
    }

    public void addIncomingMessageListener(IncomingMessageListener incomingMessageListener)
    {
        synchronized (incomingMessageListeners)
        {
            if (incomingMessageListener != null)
            {
                incomingMessageListeners.remove(incomingMessageListener);
                incomingMessageListeners.add(incomingMessageListener);
            }
        }
    }

    public void removeIncomingMessageListener(IncomingMessageListener incomingMessageListener)
    {
        synchronized (incomingMessageListeners)
        {
            if (incomingMessageListener != null)
            {
                incomingMessageListeners.remove(incomingMessageListener);
            }
        }
    }

    private void notifyIncomingMessage(final GoTennaMessage incomingMessage)
    {
        synchronized (incomingMessageListeners)
        {
            for (IncomingMessageListener incomingMessageListener : incomingMessageListeners)
            {
                incomingMessageListener.onIncomingMessage(incomingMessage);
            }
        }
    }

    private void showGroupInvitationToast(long groupGID)
    {
        /*
        Context context = MyApplication.getAppContext();
        String message = context.getString(R.string.invited_to_group_toast_text, groupGID);

        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        */

    }

    //==============================================================================================
    // GTMessageListener Implementation
    //==============================================================================================

    @Override
    public void onIncomingMessage(GTMessageData messageData)
    {
        // We do not send any custom formatted messages in this app,
        // but if you wanted to send out messages with your own format, this is where
        // you would receive those incoming messages.
    }

    @Override
    public void onIncomingMessage(GTBaseMessageData gtBaseMessageData)
    {
        // This is where you would receive incoming messages that the SDK automatically knows how to parse
        // such as GTTextOnlyMessageData among the many other MessageData classes.
        if (gtBaseMessageData instanceof GTTextOnlyMessageData)
        {
            // Somebody sent us a message, try to parse it
            GTTextOnlyMessageData gtTextOnlyMessageData = (GTTextOnlyMessageData) gtBaseMessageData;
            GoTennaMessage incomingMessage = GoTennaMessage.createMessageFromData(gtTextOnlyMessageData);
            notifyIncomingMessage(incomingMessage);
        }
        else if (gtBaseMessageData instanceof GTGroupCreationMessageData)
        {
            // Somebody invited us to a group!
            GTGroupCreationMessageData gtGroupCreationMessageData = (GTGroupCreationMessageData) gtBaseMessageData;
            showGroupInvitationToast(gtGroupCreationMessageData.getGroupGID());
        }
    }

    //==============================================================================================
    // IncomingMessageListener Interface
    //==============================================================================================

    public interface IncomingMessageListener
    {
        void onIncomingMessage(GoTennaMessage incomingMessage);
    }
}
