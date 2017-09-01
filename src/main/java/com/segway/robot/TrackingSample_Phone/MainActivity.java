package com.segway.robot.TrackingSample_Phone;

/**
 * Created by Yi.Zhang on 2017/04/26.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.mobile.sdk.connectivity.BufferMessage;
import com.segway.robot.mobile.sdk.connectivity.MobileException;
import com.segway.robot.mobile.sdk.connectivity.MobileMessageRouter;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.LinkedList;

public class MainActivity extends Activity {
    private static final String TAG = "TrackingActivity_Phone";
    private Draw mDraw;
    private FrameLayout mFrameLayout;
    private EditText mEditText;
    private TextView mTextView;
    private Button mResetButton;
    private Button mSendButton;
    private Button mStopButton;
    private Button mScaleButton;
    private String mRobotIP;
    private MobileMessageRouter mMobileMessageRouter = null;
    private MessageConnection mMessageConnection = null;
    private LinkedList<PointF> mPointList;
    private float pixelToMeter = 0.01f;

    class Point3D {
        public Point3D(int width, int height, float density){
            this.width = width;
            this.height = height;
            this.density = density;
        }
        public int width;
        public int height;
        public float density;
    }

    // called when service bind success or failed, register MessageConnectionListener in onBind
    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind: ");
            try {
                mMobileMessageRouter.register(mMessageConnectionListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbind(String reason) {
            Log.e(TAG, "onUnbind: " + reason);
        }
    };

    // called when connection created, set ConnectionStateListener and MessageListener in onConnectionCreated
    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new MessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            Log.d(TAG, "onConnectionCreated: " + connection.getName());
            //get the MessageConnection instance
            mMessageConnection = connection;
            try {
                mMessageConnection.setListeners(mConnectionStateListener, mMessageListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    // called when connection state change
    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            //connection between mobile application and robot application is opened.
            //Now can send messages to each other.
            Log.d(TAG, "onOpened: " + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enableButtons();
                    Toast.makeText(getApplicationContext(), "connected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            //connection closed with error
            Log.e(TAG, "onClosed: " + error + ";name=" + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    disableButtons();
                    Toast.makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    // called when message received/sent/sentError
    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageReceived(final Message message) {
            byte[] bytes = (byte[]) message.getContent();
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            boolean dataIgnored = buffer.getInt()==1? true:false;
            Log.d(TAG, "onMessageReceived: data ignored=" + dataIgnored + ";timestamp=" + message.getTimestamp());
            if(dataIgnored) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Robot Ignore Data", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Robot Start Tracking", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void onMessageSentError(Message message, String error) {
            //the message  that is sent failed
            Log.d(TAG, "Message send error");
        }

        @Override
        public void onMessageSent(Message message) {
            //the message  that is sent successfully
            Log.d(TAG, "onMessageSent: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // find view
        findView();

        // init canvas
        initCanvas();

        // disable buttons
        disableButtons();

        // show scale hint
        showScale();
    }

    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.btnReset:
                // reset paint to empty
                mDraw.resetPaint();
            break;
            case R.id.btnSend:
                // send point list to Robot
                mPointList = mDraw.getPointList();
                byte[] messageByte = packFile();
                if (mMessageConnection != null) {
                    try {
                        //message sent is BufferMessage, used a txt file to test sending BufferMessage
                        mMessageConnection.sendMessage(new BufferMessage(messageByte));
                    } catch (MobileException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.btnBind:
                // init connection to Robot
                initConnection();
                break;
            case R.id.btnStop:
                // send STOP instruction to robot
                stopRobot();
                break;
            case R.id.btnScale:
                // modify area scale
                final EditText et = new EditText(this);
                new AlertDialog.Builder(this)
                        .setTitle("Input width")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(et)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                float fWidth = Float.parseFloat(et.getText().toString());
                                pixelToMeter = fWidth/(float)mDraw.getCanvasWidth();
                                showScale();
                                initCanvas();
                            }
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
                break;
        }
    }

    // find view
    private void findView() {
        mFrameLayout = (FrameLayout) findViewById(R.id.flMap);
        mEditText = (EditText) findViewById(R.id.etIP);
        mTextView = (TextView) findViewById(R.id.tvScale);
        mResetButton = (Button) findViewById(R.id.btnReset);
        mSendButton = (Button) findViewById(R.id.btnSend);
        mStopButton = (Button) findViewById(R.id.btnStop);
        mScaleButton = (Button) findViewById(R.id.btnScale);
    }

    // init canvas for drawing
    private void initCanvas() {
        Point3D pt = getWindowSize();
        int gridWidthInPixel = (int)(1/pixelToMeter);
        mDraw = new Draw(MainActivity.this, pt.width, pt.height, pt.density, gridWidthInPixel);
        mFrameLayout.addView(mDraw);
    }

    // init connection to Robot
    private void initConnection() {
        // get the MobileMessageRouter instance
        mMobileMessageRouter = MobileMessageRouter.getInstance();

        // you can read the IP from the robot app.
        mRobotIP = mEditText.getText().toString();
        try {
            mMobileMessageRouter.setConnectionIp(mRobotIP);

            // bind the connection service in robot
            mMobileMessageRouter.bindService(this, mBindStateListener);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Connection init FAILED", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Connection init FAILED", e);
        }
    }

    // pack file to byte[]
    private byte[] packFile() {
        ByteBuffer buffer = ByteBuffer.allocate(mPointList.size() * 2 * 4 + 4);
        // protocol: the first 4 bytes is indicator of data or STOP message
        // 1 represent tracking data, 0 represent STOP message
        buffer.putInt(1);
        for(PointF pf : mPointList) {
            //System.out.println(pf.x + " " + pf.y);
            Log.d(TAG, "Send " + pixelToMeter * pf.x + "< >" + pixelToMeter * pf.y);
            buffer.putFloat(pixelToMeter * pf.x);
            buffer.putFloat(pixelToMeter * pf.y);
        }
        buffer.flip();
        byte[] messageByte = buffer.array();
        return messageByte;
    }

    // send STOP message to robot
    public void stopRobot() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        // protocol: the first 4 bytes is indicator of data or STOP message
        // 1 represent tracking data, 0 represent STOP message
        buffer.putInt(0);
        byte[] messageByte = buffer.array();
        try {
            mMessageConnection.sendMessage(new BufferMessage(messageByte));
        } catch(Exception e) {
            Log.e(TAG, "send STOP message failed", e);
        }
    }

    // show area width and height
    private void showScale() {
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        mTextView.setText(decimalFormat.format(pixelToMeter * mDraw.getCanvasWidth()) + "X" + decimalFormat.format(pixelToMeter * mDraw.getCanvasHeight()) + "m");
    }

    // get window size
    private Point3D getWindowSize() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;
        int height = metric.heightPixels;
        float density = metric.density;
        return new Point3D(width, height, density);
    }

    // enable buttons
    private void enableButtons() {
        mResetButton.setEnabled(true);
        mSendButton.setEnabled(true);
        mStopButton.setEnabled(true);
    }

    // disable buttons
    private void disableButtons() {
        mResetButton.setEnabled(false);
        mSendButton.setEnabled(false);
        mStopButton.setEnabled(false);
    }
}
