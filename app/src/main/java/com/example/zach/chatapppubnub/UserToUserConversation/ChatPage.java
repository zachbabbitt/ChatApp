package com.example.zach.chatapppubnub.UserToUserConversation;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zach.chatapppubnub.KeyFile;
import com.example.zach.chatapppubnub.R;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ChatPage extends AppCompatActivity {

    private Context mContext; //Context of this activity
    private ConversationAdapter mAdapter; //Adapter for the conversation list view
    private static int mUserID;
    private PubNub pubnub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_page);

        mContext = this;


        //Generates random number as userID.
        //Temporary until Firebase integration is added
        //Could have duplicates, needed for prototyping
        //TODO: make userID not Random
        //TODO: Store userID in device storage
        Random rand = new Random();
        mUserID = 0;


        InitializeConversationListView();


        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setSubscribeKey(KeyFile.subscribehKey);
        pnConfiguration.setPublishKey(KeyFile.publishKey);
        pnConfiguration.setUuid(Integer.toString(mUserID));
        pnConfiguration.setSecure(true);

        pubnub = new PubNub(pnConfiguration);

        Button sendButton = (Button) findViewById(R.id.sendButton);
        final EditText messageHolderET = (EditText) findViewById(R.id.messageHolder);


        final String time = "";


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageHolderET.getText().toString();
                Messages mess = new Messages(message, mUserID);
                sendMessage(mess);
            }
        });

        PubNub pubnub= new PubNub(pnConfiguration);

        pubnub.subscribe().channels(Arrays.asList("first_channel")).execute();

        pubnub.addListener(new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
                    Messages data = new Messages("Sample Text", mUserID);
                    pubnub.publish().channel("first_channel").message(data).async(new PNCallback<PNPublishResult>() {
                        @Override
                        public void onResponse(PNPublishResult result, PNStatus status) {
                            System.out.println("onResponse called");
                            Toast.makeText(mContext, result.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                System.out.println("message called" + message.toString());
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {
                System.out.println("presence called");
            }
        });


    }

    private void sendMessage(Messages message) {
        pubnub.publish()
                .message(message)
                .channel("first_channel")
                .shouldStore(true)
                .usePOST(true)
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        if (status.isError()) {
                            // something bad happened.
                            System.out.println("error happened while publishing: " + status.toString());
                        } else {
                            System.out.println("publish worked! timetoken: " + result.getTimetoken());
                        }
                    }
                });
    }

    /**
     * Instantiates the Conversation List View in the activity_chat_page.xml file
     */
    private void InitializeConversationListView(){
        Messages[] messages = new Messages[3];
        messages[0] = new Messages("Message Text 0", mUserID);
        messages[1] = new Messages("Message Text 1",5);
        messages[2] = new Messages("Message Text 2",1);

        mAdapter = new ConversationAdapter(mContext, R.layout.single_message_from_user_layout, messages);



        ListView lv = (ListView) findViewById(R.id.conversation_list_view);
        lv.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        mAdapter.addAll(messages);
    }

    //Storage for individual messages
    private class Messages{
        private String text;
        private int userID;
        public Messages(String text, int userID){
            this.text = text;
            this.userID = userID;
        }
        @Override
        public String toString(){
            return text + "," + userID;
        }
    }


    /**
     * Adapter to populate the conversation listview located in activity_chat_page.xml
     * Needed since we want to use two different layout formats for user and partner messages
     * Formats changed by setting the X scale to -1 if partner message
     * X scale is 1 if user message.
     *
     * Pubnub and Firebase store messages to the Messages array, PubNub is the tool for instant
     * communication, Firebase is to persist the messages so they can be seen across app boot ups
     *
     * At scale, would be cheaper to have a first party solution, I don't feel like coding that for this
     *
     */
    private class ConversationAdapter extends ArrayAdapter<Messages> {

        private Context mContext;
        private int layoutID;
        private Messages[] messages;
        private ConversationAdapter(Context mContext, int layoutID, Messages[] messages){
            super(mContext, layoutID);
            this.mContext = mContext;
            this.layoutID = layoutID;
            this.messages = messages;
        }

        /**
         *  Builds the view to contain the message and timestamp
         * @param position index of the array
         * @param convertView
         * @param parent
         * @return the inflated view to be added to the ListView
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            if(convertView == null){
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(layoutID, parent, false);
            }

            //Grab current message
            Messages currMessage = messages[position];

            //Set text from current message to the single_message_from_user_layout file
            TextView messageView = (TextView) convertView.findViewById(R.id.message);
            TextView timeView = (TextView) convertView.findViewById(R.id.messageTimeStamp);

            messageView.setText(currMessage.text);
//            timeView.setText(currMessage.time);


            //Flip side of screen if from user or from partner
            if(currMessage.userID != mUserID){
                RelativeLayout rl = (RelativeLayout) convertView.findViewById(R.id.RLToFlip);
                RelativeLayout backgroundColor = (RelativeLayout) convertView.findViewById(R.id.backgroundColor);
                rl.setScaleX(-1);
                messageView.setScaleX(-1);
                timeView.setScaleX(-1);

                backgroundColor.setBackgroundColor(Color.YELLOW);


            }
            return convertView;

        }
    }
}
