package com.baijiahulian.live.ui.rightmenu;


import com.baijiahulian.live.ui.activity.LiveRoomRouterListener;
import com.baijiahulian.live.ui.utils.RxUtils;
import com.baijiahulian.livecore.context.LPConstants;
import com.baijiahulian.livecore.listener.OnSpeakApplyCountDownListener;
import com.baijiahulian.livecore.models.LPSpeakInviteModel;
import com.baijiahulian.livecore.models.imodels.IMediaControlModel;
import com.baijiahulian.livecore.models.imodels.IMediaModel;
import com.baijiahulian.livecore.utils.LPErrorPrintSubscriber;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static com.baijiahulian.live.ui.utils.Precondition.checkNotNull;

/**
 * Created by Shubo on 2017/2/15.
 */

public class RightMenuPresenter implements RightMenuContract.Presenter {

    private LiveRoomRouterListener liveRoomRouterListener;
    private RightMenuContract.View view;
    private LPConstants.LPUserType currentUserType;
    private Subscription subscriptionOfMediaControl, subscriptionOfMediaPublishDeny, subscriptionOfSpeakApplyDeny, subscriptionOfClassEnd, subscriptionOfSpeakApplyResponse,
            subscriptionOfSpeakInvite, subscriptionOfClassStart, subscriptionOfStudentDrawingAuth;
    private int speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
    private boolean isDrawing = false;
    private boolean isGetDrawingAuth = false;
    private boolean isWaitingRecordOpen = false;

    public RightMenuPresenter(RightMenuContract.View view) {
        this.view = view;
    }

    @Override
    public void visitOnlineUser() {
        liveRoomRouterListener.navigateToUserList();
    }

    @Override
    public void changeDrawing() {
        if (!isDrawing && !liveRoomRouterListener.canStudentDraw()) {
            view.showCantDraw();
            return;
        }
        if (!liveRoomRouterListener.isTeacherOrAssistant() && !liveRoomRouterListener.getLiveRoom().isClassStarted()) {
            view.showCantDrawCauseClassNotStart();
            return;
        }
        isDrawing = !isDrawing;
        liveRoomRouterListener.navigateToPPTDrawing(isDrawing);
        view.showDrawingStatus(isDrawing);
    }

    @Override
    public void managePPT() {
        if (currentUserType == LPConstants.LPUserType.Teacher
                || currentUserType == LPConstants.LPUserType.Assistant) {
            liveRoomRouterListener.navigateToPPTWareHouse();
        }
    }

    public int getSpeakApplyStatus() {
        return speakApplyStatus;
    }

    @Override
    public void speakApply() {
        checkNotNull(liveRoomRouterListener);

        if (!liveRoomRouterListener.getLiveRoom().isClassStarted()) {
            view.showHandUpError();
            return;
        }

        if (speakApplyStatus == RightMenuContract.STUDENT_SPEAK_APPLY_NONE) {
            if (liveRoomRouterListener.getLiveRoom().getForbidRaiseHandStatus()) {
                view.showHandUpForbid();
                return;
            }

            if (liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().isSpeakersFull()){
                speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().cancelSpeakApply();
                view.showSpeakClosedByServer();
                return;
            }

            liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().requestSpeakApply(new OnSpeakApplyCountDownListener() {
                @Override
                public void onTimeOut() {
                    speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                    liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().cancelSpeakApply();
                    view.showSpeakApplyCanceled();
                    view.showHandUpTimeout();
                }

                @Override
                public void onTimeCountDown(int counter, int timeOut) {
                    view.showSpeakApplyCountDown(timeOut - counter, timeOut);
                }
            });
            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_APPLYING;
            view.showWaitingTeacherAgree();
        } else if (speakApplyStatus == RightMenuContract.STUDENT_SPEAK_APPLY_APPLYING) {
            // 取消发言请求
            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
            liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().cancelSpeakApply();
            view.showSpeakApplyCanceled();
        } else if (speakApplyStatus == RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
            // 取消发言
            cancelStudentSpeaking();
        }
    }

    private void cancelStudentSpeaking(){
        speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
        liveRoomRouterListener.disableSpeakerMode();
        view.showSpeakApplyCanceled();
        if (isDrawing) {
            // 如果画笔打开 关闭画笔模式
            changeDrawing();
        }
    }

    @Override
    public void onSpeakInvite(int confirm) {
        if (liveRoomRouterListener.getLiveRoom().getGroupId() != 0) return;
        liveRoomRouterListener.getLiveRoom().sendSpeakInvite(confirm);
        if (confirm == 1) {
            //接受
            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING;
            liveRoomRouterListener.getLiveRoom().getRecorder().publish();
            if (!liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached())
                liveRoomRouterListener.attachLocalAudio();
            if (liveRoomRouterListener.getLiveRoom().getAutoOpenCameraStatus()) {
                isWaitingRecordOpen = true;
                Observable.timer(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
//                        .filter(new Func1<Long, Boolean>() {
//                            @Override
//                            public Boolean call(Long aLong) {
//                                return !liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached();
//                            }
//                        })
                        .subscribe(new LPErrorPrintSubscriber<Long>() {
                            @Override
                            public void call(Long aLong) {
                                if (!liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached())
                                    liveRoomRouterListener.attachLocalVideo();
                                isWaitingRecordOpen = false;
                            }
                        });
            }
            view.showForceSpeak(isEnableDrawing());
            liveRoomRouterListener.enableSpeakerMode();
        }
    }

    @Override
    public boolean isWaitingRecordOpen() {
        return isWaitingRecordOpen;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.liveRoomRouterListener = liveRoomRouterListener;
    }

    private boolean isEnableDrawing(){
        return isGetDrawingAuth || liveRoomRouterListener.getLiveRoom().getPartnerConfig().liveDisableGrantStudentBrush == 1;
    }

    @Override
    public void subscribe() {
        checkNotNull(liveRoomRouterListener);
        currentUserType = liveRoomRouterListener.getLiveRoom().getCurrentUser().getType();

        if (liveRoomRouterListener.isTeacherOrAssistant()) {
            view.showTeacherRightMenu();
        } else {
            view.showStudentRightMenu();
            if (liveRoomRouterListener.getLiveRoom().getPartnerConfig().liveHideUserList == 1) {
                view.hideUserList();
            }
            if (liveRoomRouterListener.getLiveRoom().getGroupId() != 0){
                view.hideSpeakApply();
            }
        }

        if (!liveRoomRouterListener.isTeacherOrAssistant()) {
            // 学生

            subscriptionOfMediaPublishDeny = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM()
                    .getObservableOfMediaDeny()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                        @Override
                        public void call(IMediaModel iMediaModel) {
                            if (!liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached() && !liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached())
                                view.showForceSpeakDenyByServer();
                            if(liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached()){
                                liveRoomRouterListener.getLiveRoom().getRecorder().detachAudio();
                            }
                            if(liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached()){
                                liveRoomRouterListener.getLiveRoom().getRecorder().detachVideo();
                                liveRoomRouterListener.detachLocalVideo();
                            }
//                            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
//                            liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().cancelSpeakApply();

                            if (liveRoomRouterListener.getLiveRoom().getRoomType() != LPConstants.LPRoomType.Multi)
                                view.showAutoSpeak(isEnableDrawing());
                        }
                    });
            subscriptionOfSpeakApplyDeny = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().getObservableOfSpeakApplyDeny()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                        @Override
                        public void call(IMediaModel iMediaModel) {
                            // 结束发言模式
                            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                            liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().cancelSpeakApply();
                            view.showSpeakClosedByServer();
                        }
                    });

            subscriptionOfMediaControl = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM()
                    .getObservableOfMediaControl()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaControlModel>() {
                        @Override
                        public void call(final IMediaControlModel iMediaControlModel) {
                            if (iMediaControlModel.isApplyAgreed()) {
                                // 强制发言
                                if (speakApplyStatus == RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
                                    //已经在发言了
                                    if (iMediaControlModel.isAudioOn() && !liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached()) {
                                        liveRoomRouterListener.getLiveRoom().getRecorder().attachAudio();
                                    } else if (!iMediaControlModel.isAudioOn() && liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached()) {
                                        liveRoomRouterListener.getLiveRoom().getRecorder().detachAudio();
                                    }
                                    if (iMediaControlModel.isVideoOn() && !liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached()) {
                                        liveRoomRouterListener.attachLocalVideo();
                                    } else if (!iMediaControlModel.isVideoOn() && liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached()) {
                                        liveRoomRouterListener.detachLocalVideo();
                                    }

                                } else {
                                    speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING;
                                    liveRoomRouterListener.enableSpeakerMode();
                                    view.showForceSpeak(isEnableDrawing());
                                    liveRoomRouterListener.showForceSpeakDlg(iMediaControlModel);
                                }
                            } else {
                                // 结束发言模式
                                speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                                if (liveRoomRouterListener.getLiveRoom().getRoomType() == LPConstants.LPRoomType.Multi) {
                                    liveRoomRouterListener.disableSpeakerMode();
                                    if (isDrawing) {
                                        // 如果画笔打开 关闭画笔模式
                                        changeDrawing();
                                    }
                                } else {
                                    liveRoomRouterListener.detachLocalVideo();
                                    if (liveRoomRouterListener.getLiveRoom().getRecorder().isPublishing())
                                        liveRoomRouterListener.getLiveRoom().getRecorder().stopPublishing();
                                }

                                if (!iMediaControlModel.getSenderUserId().equals(liveRoomRouterListener.getLiveRoom().getCurrentUser().getUserId())) {
                                    // 不是自己结束发言的
                                    view.showSpeakClosedByTeacher(liveRoomRouterListener.getLiveRoom().getRoomType() == LPConstants.LPRoomType.SmallGroup);
                                }
                            }
                            if (!iMediaControlModel.isAudioOn() && !iMediaControlModel.isVideoOn()
                                    && liveRoomRouterListener.getLiveRoom().getRoomType() == LPConstants.LPRoomType.Multi ){
                                cancelStudentSpeaking();
                            }
                        }
                    });

            subscriptionOfSpeakApplyResponse = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().getObservableOfSpeakResponse()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaControlModel>() {
                        @Override
                        public void call(IMediaControlModel iMediaControlModel) {
                            if (!iMediaControlModel.getUser().getUserId()
                                    .equals(liveRoomRouterListener.getLiveRoom().getCurrentUser().getUserId()))
                                return;
                            // 请求发言的用户自己
                            if (iMediaControlModel.isApplyAgreed()) {
                                // 进入发言模式
                                liveRoomRouterListener.getLiveRoom().getRecorder().publish();
//                                liveRoomRouterListener.getLiveRoom().getRecorder().attachVideo();
                                liveRoomRouterListener.attachLocalAudio();
                                view.showSpeakApplyAgreed(isEnableDrawing());
                                speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING;
                                liveRoomRouterListener.enableSpeakerMode();
                                if (liveRoomRouterListener.getLiveRoom().getAutoOpenCameraStatus()) {
                                    isWaitingRecordOpen = true;
                                    Observable.timer(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new LPErrorPrintSubscriber<Long>() {
                                                @Override
                                                public void call(Long aLong) {
                                                        liveRoomRouterListener.attachLocalVideo();
                                                        isWaitingRecordOpen = false;
                                                }
                                            });
                                }
                            } else {
                                speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                                if (!iMediaControlModel.getSenderUserId().equals(liveRoomRouterListener.getLiveRoom().getCurrentUser().getUserId())) {
                                    // 不是自己结束发言的
                                    view.showSpeakApplyDisagreed();
                                }
                            }
                        }
                    });

            subscriptionOfStudentDrawingAuth = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().getPublishSubjectOfStudentDrawingAuth()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            if (aBoolean){
                                if (isGetDrawingAuth) return;
                                view.showPPTDrawBtn();
                                isGetDrawingAuth = true;
                            } else{
                                if (!isGetDrawingAuth) return;
                                view.hidePPTDrawBtn();
                                isGetDrawingAuth = false;
                                liveRoomRouterListener.navigateToPPTDrawing(false);
                                isDrawing = false;
                                view.showDrawingStatus(false);
                            }
                        }
                    });


        } else if (liveRoomRouterListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Assistant) {
            // 助教
            subscriptionOfMediaPublishDeny = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM()
                    .getObservableOfMediaDeny()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                        @Override
                        public void call(IMediaModel iMediaModel) {
                            if (!liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached() && !liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached())
                                view.showForceSpeakDenyByServer();
                            if(liveRoomRouterListener.getLiveRoom().getRecorder().isAudioAttached()){
                                liveRoomRouterListener.getLiveRoom().getRecorder().detachAudio();
                            }
                            if(liveRoomRouterListener.getLiveRoom().getRecorder().isVideoAttached()){
                                liveRoomRouterListener.getLiveRoom().getRecorder().detachVideo();
                                liveRoomRouterListener.detachLocalVideo();
                            }

                            if (liveRoomRouterListener.getLiveRoom().getRoomType() != LPConstants.LPRoomType.Multi)
                                view.showAutoSpeak(isEnableDrawing());
                        }
                    });
            subscriptionOfSpeakApplyDeny = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().getObservableOfSpeakApplyDeny()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                        @Override
                        public void call(IMediaModel iMediaModel) {
                            // 结束发言模式
                            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                            liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().cancelSpeakApply();
                            view.showSpeakClosedByServer();
                        }
                    });

            subscriptionOfMediaControl = liveRoomRouterListener.getLiveRoom().getSpeakQueueVM()
                    .getObservableOfMediaControl()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaControlModel>() {
                        @Override
                        public void call(IMediaControlModel iMediaControlModel) {
                            if (!iMediaControlModel.isApplyAgreed()) {
                                // 结束发言模式
                                liveRoomRouterListener.disableSpeakerMode();
                                if (isDrawing) changeDrawing();
                            }
                        }
                    });

        }

        subscriptionOfClassEnd = liveRoomRouterListener.getLiveRoom().getObservableOfClassEnd()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        if (speakApplyStatus == RightMenuContract.STUDENT_SPEAK_APPLY_APPLYING) {
                            // 取消发言请求
                            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                            liveRoomRouterListener.getLiveRoom().getSpeakQueueVM().cancelSpeakApply();
                            view.showSpeakApplyCanceled();
                        } else if (speakApplyStatus == RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING) {
                            // 取消发言
                            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_NONE;
                            liveRoomRouterListener.disableSpeakerMode();
                            view.showSpeakApplyCanceled();
                            if (isDrawing) {
                                // 如果画笔打开 关闭画笔模式
                                liveRoomRouterListener.navigateToPPTDrawing(false);
                                isDrawing = !isDrawing;
                                view.showDrawingStatus(isDrawing);
                            }
                        }
                        isGetDrawingAuth = false;
                    }
                });

        subscriptionOfClassStart = liveRoomRouterListener.getLiveRoom().getObservableOfClassStart()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        if (liveRoomRouterListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Student &&
                                liveRoomRouterListener.getLiveRoom().getRoomType() != LPConstants.LPRoomType.Multi) {
                            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING;
                        }
                    }
                });

        if (liveRoomRouterListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Student &&
                liveRoomRouterListener.getLiveRoom().getRoomType() != LPConstants.LPRoomType.Multi) {
            view.showAutoSpeak(isEnableDrawing());
            speakApplyStatus = RightMenuContract.STUDENT_SPEAK_APPLY_SPEAKING;
        }

        //邀请发言
        subscriptionOfSpeakInvite = liveRoomRouterListener.getLiveRoom().getObservableOfSpeakInvite()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<LPSpeakInviteModel>() {
                    @Override
                    public void call(LPSpeakInviteModel lpSpeakInviteModel) {
                        if (liveRoomRouterListener.getLiveRoom().getCurrentUser().getUserId().equals(lpSpeakInviteModel.to)) {
                            liveRoomRouterListener.showSpeakInviteDlg(lpSpeakInviteModel.invite);
                        }
                    }
                });

//        liveRoomRouterListener.getLiveRoom().getGroupId()
    }

    @Override
    public void unSubscribe() {
        RxUtils.unSubscribe(subscriptionOfMediaControl);
        RxUtils.unSubscribe(subscriptionOfSpeakApplyResponse);
        RxUtils.unSubscribe(subscriptionOfClassEnd);
        RxUtils.unSubscribe(subscriptionOfSpeakInvite);
        RxUtils.unSubscribe(subscriptionOfClassStart);
        RxUtils.unSubscribe(subscriptionOfSpeakApplyDeny);
        RxUtils.unSubscribe(subscriptionOfMediaPublishDeny);
        RxUtils.unSubscribe(subscriptionOfStudentDrawingAuth);
    }

    @Override
    public void destroy() {
        liveRoomRouterListener = null;
        view = null;
    }
}
