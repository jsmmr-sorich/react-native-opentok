package com.rnopentok;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.SubscriberKit.SubscriberAudioStats;
import com.opentok.android.SubscriberKit.SubscriberVideoStats;


public class RNOpenTokSubscriberView extends RNOpenTokView implements SubscriberKit.SubscriberListener, SubscriberKit.VideoStatsListener, SubscriberKit.AudioStatsListener {
    private Subscriber mSubscriber;
    private Boolean mAudioEnabled;
    private Boolean mVideoEnabled;

    public RNOpenTokSubscriberView(ThemedReactContext context) {
        super(context);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        RNOpenTokSessionManager.getSessionManager().setSubscriberListener(mSessionId, this);
    }

    public void setAudio(Boolean enabled) {
        if (mSubscriber != null) {
            mSubscriber.setSubscribeToAudio(enabled);
        }

        mAudioEnabled = enabled;
    }

    public void setVideo(Boolean enabled) {
        if (mSubscriber != null) {
            mSubscriber.setSubscribeToVideo(enabled);
        }

        mVideoEnabled = enabled;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RNOpenTokSessionManager.getSessionManager().removeSubscriberListener(mSessionId);
    }

    private void startSubscribing(Stream stream) {
        mSubscriber = new Subscriber(getContext(), stream);
        mSubscriber.setSubscriberListener(this);
        mSubscriber.setVideoStatsListener(this);
        mSubscriber.setAudioStatsListener(this);
        mSubscriber.setSubscribeToAudio(mAudioEnabled);
        mSubscriber.setSubscribeToVideo(mVideoEnabled);

        mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL);

        Session session = RNOpenTokSessionManager.getSessionManager().getSession(mSessionId);
        session.subscribe(mSubscriber);

        attachSubscriberView();
    }

    private void attachSubscriberView() {
        addView(mSubscriber.getView(), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        requestLayout();
    }

    private void cleanUpSubscriber() {
        if ( mSubscriber != null ) {
            removeView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber = null;
        }
    }

    public void onStreamReceived(Session session, Stream stream) {
        if (mSubscriber == null) {
            startSubscribing(stream);
            sendEvent(Events.EVENT_SUBSCRIBE_START, Arguments.createMap());
        }
    }

    public void onStreamDropped(Session session, Stream stream) {
        sendEvent(Events.EVENT_SUBSCRIBE_STOP, Arguments.createMap());
    }

    /** Subscribe listener **/

    @Override
    public void onConnected(SubscriberKit subscriberKit) {}

    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        cleanUpSubscriber();
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        WritableMap payload = Arguments.createMap();
        payload.putString("connectionId", opentokError.toString());

        sendEvent(Events.EVENT_SUBSCRIBE_ERROR, payload);

        cleanUpSubscriber();
    }

    @Override
    public void onVideoStats(SubscriberKit subscriber, SubscriberKit.SubscriberVideoStats stats) {
        WritableMap payload = Arguments.createMap();
        payload.putInt("videoPacketsReceived", stats.videoPacketsLost);
        payload.putInt("videoPacketsLost", stats.videoPacketsLost);
        payload.putInt("videoBytesReceived", stats.videoBytesReceived);
        payload.putDouble("timestamp", stats.timeStamp);

        sendEvent(Events.EVENT_SUBSCRIBE_VIDEO_NETWORK_STATS_UPDATE, payload);
    }

    @Override
    public void onAudioStats(SubscriberKit subscriber, SubscriberKit.SubscriberAudioStats stats) {
        WritableMap payload = Arguments.createMap();
        payload.putInt("audioPacketsReceived", stats.audioPacketsLost);
        payload.putInt("audioPacketsLost", stats.audioPacketsLost);
        payload.putInt("audioBytesReceived", stats.audioBytesReceived);
        payload.putDouble("timestamp", stats.timeStamp);

        sendEvent(Events.EVENT_SUBSCRIBE_AUDIO_NETWORK_STATS_UPDATE, payload);
    }

}
