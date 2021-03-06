package cn.rongcloud.radio.helper;

import android.text.TextUtils;
import android.util.Log;

import com.basis.net.oklib.OkApi;
import com.basis.net.oklib.OkParams;
import com.basis.net.oklib.WrapperCallBack;
import com.basis.net.oklib.wrapper.Wrapper;
import com.basis.utils.KToast;
import com.basis.utils.Logger;
import com.basis.utils.UIKit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.rongcloud.config.UserManager;
import cn.rongcloud.music.MusicControlManager;
import cn.rongcloud.radioroom.IRCRadioRoomEngine;
import cn.rongcloud.radioroom.RCRadioRoomEngine;
import cn.rongcloud.radioroom.callback.RCRadioRoomCallback;
import cn.rongcloud.radioroom.room.RCRadioEventListener;
import cn.rongcloud.radioroom.utils.JsonUtils;
import cn.rongcloud.roomkit.api.VRApi;
import cn.rongcloud.roomkit.manager.AllBroadcastManager;
import cn.rongcloud.roomkit.message.RCAllBroadcastMessage;
import cn.rongcloud.roomkit.message.RCChatroomAdmin;
import cn.rongcloud.roomkit.message.RCChatroomBarrage;
import cn.rongcloud.roomkit.message.RCChatroomEnter;
import cn.rongcloud.roomkit.message.RCChatroomGift;
import cn.rongcloud.roomkit.message.RCChatroomGiftAll;
import cn.rongcloud.roomkit.message.RCChatroomKickOut;
import cn.rongcloud.roomkit.message.RCChatroomLeave;
import cn.rongcloud.roomkit.message.RCChatroomLocationMessage;
import cn.rongcloud.roomkit.message.RCChatroomVoice;
import cn.rongcloud.roomkit.message.RCFollowMsg;
import cn.rongcloud.roomkit.message.RCRRCloseMessage;
import cn.rongcloud.roomkit.ui.miniroom.MiniRoomManager;
import cn.rongcloud.roomkit.ui.miniroom.OnCloseMiniRoomListener;
import cn.rongcloud.roomkit.ui.miniroom.OnMiniRoomListener;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;

/**
 * @author gyn
 * @date 2021/10/13
 * <p>
 * ?????????????????????????????????????????????????????????????????????
 */
public class RadioEventHelper implements IRadioEventHelper, RCRadioEventListener, OnCloseMiniRoomListener {

    // ???????????????????????????
    private boolean isSendDefaultMessage = false;

    public static RadioEventHelper getInstance() {
        return Holder.INSTANCE;
    }

    private List<RadioRoomListener> listeners = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();
    private OnMiniRoomListener onMiniRoomListener;
    private String roomId;
    // ??????????????????
    private boolean isInSeat = false;
    // ????????????
    private boolean isSuspend = false;
    // ????????????
    private boolean isMute = false;

    public void setSendDefaultMessage(boolean sendDefaultMessage) {
        isSendDefaultMessage = sendDefaultMessage;
    }

    public String getRoomId() {
        return roomId;
    }

    public boolean isInSeat() {
        return isInSeat;
    }

    public void setInSeat(boolean inSeat) {
        isInSeat = inSeat;
    }

    public boolean isSuspend() {
        return isSuspend;
    }

    public void setSuspend(boolean suspend) {
        isSuspend = suspend;
    }

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean mute) {
        isMute = mute;
    }

    @Override
    public void unRegister() {
        this.roomId = null;
        listeners.clear();
        messages.clear();
        isInSeat = false;
        isSuspend = false;
        isMute = false;
        isSendDefaultMessage = false;
        onMiniRoomListener = null;
        MusicControlManager.getInstance().release();
    }

    @Override
    public void register(String roomId) {
        if (!TextUtils.equals(roomId, this.roomId)) {
            this.roomId = roomId;
            RCRadioRoomEngine.getInstance().setRadioEventListener(this);
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        if (message.getConversationType() != Conversation.ConversationType.CHATROOM) {
            return;
        }
        MessageContent content = message.getContent();
        Logger.d("==============onMessageReceived: " + content.getClass() + JsonUtils.toJson(content));
        // ?????????????????????
        if (content instanceof RCAllBroadcastMessage) {
            AllBroadcastManager.getInstance().addMessage((RCAllBroadcastMessage) content);
            return;
        }
        // ????????????
        if (isShowingMessage(message)) {
            messages.add(message);
        }

        if (content instanceof RCChatroomKickOut) {
            // ??????????????????????????????????????????
            String targetId = ((RCChatroomKickOut) content).getTargetId();
            if (TextUtils.equals(targetId, UserManager.get().getUserId())) {
                // ??????????????????????????????
                if (MiniRoomManager.getInstance().isShowing()) {
                    leaveRoom(new LeaveRoomCallback() {
                        @Override
                        public void leaveFinish() {
                            KToast.show("?????????????????????");
                            MiniRoomManager.getInstance().close();
                        }
                    });
                }
            }
        } else if (content instanceof RCRRCloseMessage) {
            // ????????????????????????
            if (MiniRoomManager.getInstance().isShowing()) {
                KToast.show("???????????????????????????");
            }
        }

        for (RCRadioEventListener l : listeners) {
            l.onMessageReceived(message);
        }
    }

    @Override
    public void addRadioEventListener(RadioRoomListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Logger.d("==============addRadioEventListener:messages-" + messages.size() + " listener size:" + listeners.size());
            if (!messages.isEmpty()) {
                listener.onLoadMessageHistory(messages);
            }
        }
    }

    public void setMiniRoomListener(OnMiniRoomListener onMiniRoomListener) {
        this.onMiniRoomListener = onMiniRoomListener;
    }

    @Override
    public void removeRadioEventListener(RadioRoomListener listener) {
        listeners.remove(listener);
        Logger.d("==============RadioEventHelper:removeRadioEventListener");
    }

    @Override
    public boolean isInRoom() {
        return !TextUtils.isEmpty(roomId);
    }

    @Override
    public void sendMessage(MessageContent messageContent) {
        if (TextUtils.isEmpty(roomId)) {
            Logger.e("roomId is empty, please register");
            return;
        }
        // ???????????????????????????
        if (messageContent instanceof RCChatroomLocationMessage) {
            Message message = new Message();
            message.setConversationType(Conversation.ConversationType.CHATROOM);
            message.setContent(messageContent);
            onMessageReceived(message);
            return;
        }
        RongCoreClient.getInstance().sendMessage(Conversation.ConversationType.CHATROOM, roomId, messageContent, "", "", new IRongCoreCallback.ISendMessageCallback() {
            @Override
            public void onAttached(Message message) {
            }

            @Override
            public void onSuccess(Message message) {
                onMessageReceived(message);
                Logger.d("=============sendChatRoomMessage:success");
            }

            @Override
            public void onError(Message message, IRongCoreEnum.CoreErrorCode coreErrorCode) {
                if (messageContent instanceof RCChatroomBarrage || messageContent instanceof RCChatroomVoice) {
                    ToastUtils.s(UIKit.getContext(), "????????????");
                }
                Logger.e("=============" + coreErrorCode.code + ":" + coreErrorCode.msg);
            }
        });
    }

    @Override
    public void onRadioRoomKVUpdate(IRCRadioRoomEngine.UpdateKey updateKey, String s) {
        switch (updateKey) {
            case RC_NOTICE:
                if (isSendDefaultMessage) {
                    sendNoticeModifyMessage();
                }
                break;
            case RC_SUSPEND:
                setSuspend(TextUtils.equals(s, "1"));
                break;
            case RC_SEATING:
                setInSeat(TextUtils.equals(s, "1"));
                break;
            case RC_SILENT:
                setMute(TextUtils.equals(s, "1"));
                break;
            case RC_SPEAKING:
                if (onMiniRoomListener != null) {
                    onMiniRoomListener.onSpeak(TextUtils.equals(s, "1"));
                }
                break;
        }
        for (RCRadioEventListener l : listeners) {
            l.onRadioRoomKVUpdate(updateKey, s);
        }
    }

    public boolean isShowingMessage(Message message) {
        MessageContent content = message.getContent();
        if (content instanceof RCChatroomBarrage || content instanceof RCChatroomEnter
                || content instanceof RCChatroomKickOut || content instanceof RCChatroomGiftAll
                || content instanceof RCChatroomGift || content instanceof RCChatroomAdmin
                || content instanceof RCChatroomLocationMessage || content instanceof RCFollowMsg
                || content instanceof RCChatroomVoice || content instanceof TextMessage) {
            return true;
        }
        return false;
    }

    /**
     * ?????????????????????
     */
    private void sendNoticeModifyMessage() {
        RCChatroomLocationMessage tips = new RCChatroomLocationMessage();
        tips.setContent("????????????????????????");
        sendMessage(tips);
    }

    @Override
    public void onCloseMiniRoom(CloseResult closeResult) {
        onMiniRoomListener = null;
        // need leave room
        leaveRoom(new LeaveRoomCallback() {
            @Override
            public void leaveFinish() {
                changeUserRoom("");
                unRegister();
                if (closeResult != null) {
                    closeResult.onClose();
                }
            }
        });
    }

    /**
     * ??????????????????
     */
    private void changeUserRoom(String roomId) {
        HashMap<String, Object> params = new OkParams()
                .add("roomId", roomId)
                .build();
        OkApi.get(VRApi.USER_ROOM_CHANGE, params, new WrapperCallBack() {
            @Override
            public void onResult(Wrapper result) {
                if (result.ok()) {
                    Log.e("TAG", "changeUserRoom: " + result.getBody());
                }
            }
        });
    }

    /**
     * ???????????????????????????
     */
    public void switchRoom() {
        // ?????????????????????
        RCChatroomLeave leave = new RCChatroomLeave();
        leave.setUserId(UserManager.get().getUserId());
        leave.setUserName(UserManager.get().getUserName());
        sendMessage(leave);
        // ????????????
        removeListener();
    }

    /**
     * ????????????
     *
     * @param leaveRoomCallback
     */
    public void leaveRoom(LeaveRoomCallback leaveRoomCallback) {
        // ????????????????????????????????????????????????
        switchRoom();
        // ?????????????????????
        RCRadioRoomEngine.getInstance().leaveRoom(new RCRadioRoomCallback() {
            @Override
            public void onSuccess() {
                changeUserRoom("");
                Logger.d("==============leaveRoom onSuccess");
                if (leaveRoomCallback != null) {
                    leaveRoomCallback.leaveFinish();
                }
            }

            @Override
            public void onError(int code, String message) {
                Logger.e("==============leaveRoom onError");
                changeUserRoom("");
                if (leaveRoomCallback != null) {
                    leaveRoomCallback.leaveFinish();
                }
            }
        });
    }

    /**
     * ????????????
     *
     * @param closeRoomCallback
     */
    public void closeRoom(String roomId, CloseRoomCallback closeRoomCallback) {
        MusicControlManager.getInstance().release();
        // ???????????????????????????
        sendMessage(new RCRRCloseMessage());
        // ????????????
        leaveRoom(() -> {
            // ????????????
            deleteRoom(roomId, closeRoomCallback);
        });
    }

    /**
     * ??????????????????
     */
    private void deleteRoom(String roomId, CloseRoomCallback closeRoomCallback) {
        // ?????????????????????????????????????????????
        OkApi.get(VRApi.deleteRoom(roomId), null, new WrapperCallBack() {
            @Override
            public void onResult(Wrapper result) {
                if (closeRoomCallback != null) {
                    closeRoomCallback.onSuccess();
                }
            }

            @Override
            public void onError(int code, String msg) {
                super.onError(code, msg);
                if (closeRoomCallback != null) {
                    closeRoomCallback.onSuccess();
                }
            }
        });
    }

    private void removeListener() {
        unRegister();
    }

    public interface CloseRoomCallback {
        void onSuccess();
    }

    public interface LeaveRoomCallback {
        void leaveFinish();
    }

    private static class Holder {
        static final RadioEventHelper INSTANCE = new RadioEventHelper();
    }
}
