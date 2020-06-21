package im.vector;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gotenna.sdk.data.GTCommandCenter;
import com.gotenna.sdk.data.GTError;
import com.gotenna.sdk.data.GTErrorListener;
import com.gotenna.sdk.data.GTResponse;
import com.gotenna.sdk.data.GTSendCommandResponseListener;
import com.gotenna.sdk.data.GTSendMessageResponse;
import com.gotenna.sdk.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GotennaSendMessageInteractor
{
    //==============================================================================================
    // Class Properties
    //==============================================================================================

    private static final String TAG = "SendMessageInteractor";
    private static final int MESSAGE_RESEND_DELAY_MILLISECONDS = 5000;
    private final GTCommandCenter gtCommandCenter;
    private final Handler messageResendHandler;
    private final List<SendMessageItem> messageQueue;
    private boolean isSending;

    //==============================================================================================
    // Constructor
    //==============================================================================================

    public GotennaSendMessageInteractor()
    {
        gtCommandCenter = GTCommandCenter.getInstance();
        messageResendHandler = new Handler(Looper.getMainLooper());
        messageQueue = new ArrayList<>();
    }

    //==============================================================================================
    // Class Instance Methods
    //==============================================================================================

    public void sendBroadcastMessage(@NonNull final GoTennaMessage message, @NonNull final SendGoTennaMessageListener sendMessageListener)
    {
        SendMessageItem sendMessageItem = new SendMessageItem();
        sendMessageItem.message = message;
        sendMessageItem.sendMessageListener = sendMessageListener;
        sendMessageItem.isBroadcast = true;

        messageQueue.add(sendMessageItem);
        attemptToSendMessage();

    }

    public void sendMessage(@NonNull final GoTennaMessage message, final boolean willEncrypt, @NonNull final SendGoTennaMessageListener sendMessageListener)
    {
        SendMessageItem sendMessageItem = new SendMessageItem();
        sendMessageItem.message = message;
        sendMessageItem.willEncrypt = willEncrypt;
        sendMessageItem.sendMessageListener = sendMessageListener;
        sendMessageItem.isBroadcast = false;

        messageQueue.add(sendMessageItem);
        attemptToSendMessage();
    }

    private void attemptToSendMessage()
    {
        if (!isSending && !messageQueue.isEmpty())
        {
            messageResendHandler.removeCallbacks(attemptToSendRunnable);
            isSending = true;
            SendMessageItem sendMessageItem = messageQueue.get(0);

            if (sendMessageItem.isBroadcast)
            {
                sendBroadcast(sendMessageItem);
            }
            else
            {
                sendMessage(sendMessageItem);
            }
        }
    }

    private void markMessageAsSentAndSendNext(SendMessageItem sendMessageItem)
    {
        messageQueue.remove(sendMessageItem);
        isSending = false;
        attemptToSendMessage();
    }

    private void sendBroadcast(final SendMessageItem sendMessageItem)
    {
        gtCommandCenter.sendBroadcastMessage(sendMessageItem.message.toBytes(), new GTSendCommandResponseListener()
        {
            @Override
            public void onSendResponse(GTSendMessageResponse response)
            {
                // All broadcasts only travel 1 hop max, so you can ignore this number
                int hopCount = response.getHopCount();
                sendMessageItem.message.setHopCount(hopCount);

                if (response.getResponseCode() == GTResponse.GTCommandResponseCode.POSITIVE)
                {
                    sendMessageItem.message.setMessageStatus(GoTennaMessage.MessageStatus.SENT_SUCCESSFULLY);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }
                else
                {
                    sendMessageItem.message.setMessageStatus(GoTennaMessage.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }

                markMessageAsSentAndSendNext(sendMessageItem);
            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                if (error.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED)
                {
                    Log.w(TAG, String.format(Locale.US, "Limit szybkości przesyłania danych został przekroczony. Ponowne wysłanie wiadomości za %d sekund", MESSAGE_RESEND_DELAY_MILLISECONDS / TimeUtils.MILLISECONDS_PER_SECOND));
                    attemptToResendWithDelay();
                }
                else
                {
                    Log.w(TAG, error.toString());
                    sendMessageItem.message.setMessageStatus(GoTennaMessage.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();

                    markMessageAsSentAndSendNext(sendMessageItem);
                }
            }
        });
    }

    private void sendMessage(final SendMessageItem sendMessageItem)
    {
        gtCommandCenter.sendMessage(sendMessageItem.message.toBytes(), sendMessageItem.message.getReceiverGID(), new GTSendCommandResponseListener()
        {
            @Override
            public void onSendResponse(GTSendMessageResponse response)
            {
                // For goTenna Mesh, responses will have a hop count to indicate how many hops the unit took to reach its destination
                // A direct A -> B transaction is 1 hop, A -> B -> C is 2 hops, etc...
                // If you are using the V1 goTenna, the hop count will always be 0.
                // The goTenna Mesh only supports up to 3 hops currently.
                int hopCount = response.getHopCount();
                sendMessageItem.message.setHopCount(hopCount);

                if (response.getResponseCode() == GTResponse.GTCommandResponseCode.POSITIVE)
                {
                    sendMessageItem.message.setMessageStatus(GoTennaMessage.MessageStatus.SENT_SUCCESSFULLY);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }
                else
                {
                    sendMessageItem.message.setMessageStatus(GoTennaMessage.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }

                markMessageAsSentAndSendNext(sendMessageItem);
            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                if (error.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED)
                {
                    Log.w(TAG, String.format(Locale.US, "Data rate limit was exceeded. Resending message in %d seconds", MESSAGE_RESEND_DELAY_MILLISECONDS / TimeUtils.MILLISECONDS_PER_SECOND));
                    attemptToResendWithDelay();
                }
                else
                {
                    Log.w(TAG, error.toString());

                    sendMessageItem.message.setMessageStatus(GoTennaMessage.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                    markMessageAsSentAndSendNext(sendMessageItem);
                }
            }
        }, sendMessageItem.willEncrypt);
    }

    private void attemptToResendWithDelay()
    {
        isSending = false;
        messageResendHandler.removeCallbacks(attemptToSendRunnable);
        messageResendHandler.postDelayed(attemptToSendRunnable, MESSAGE_RESEND_DELAY_MILLISECONDS);
    }

    private final Runnable attemptToSendRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            attemptToSendMessage();
        }
    };

    //==============================================================================================
    // SendMessageItem Class
    //==============================================================================================

    private class SendMessageItem
    {
        GoTennaMessage message;
        boolean willEncrypt;
        SendGoTennaMessageListener sendMessageListener;
        boolean isBroadcast;
    }

    //==============================================================================================
    // SendMessageListener
    //==============================================================================================

    public interface SendGoTennaMessageListener
    {
        void onMessageResponseReceived();
    }
}
