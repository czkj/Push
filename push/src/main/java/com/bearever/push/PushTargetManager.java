package com.bearever.push;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.bearever.push.handle.PushReceiverHandleManager;
import com.bearever.push.model.PushTarget;
import com.bearever.push.model.ReceiverInfo;
import com.bearever.push.target.BasePushTargetInit;
import com.bearever.push.target.huawei.HuaweiInit;
import com.bearever.push.target.jiguang.JPushInit;
import com.bearever.push.target.meizu.MeizuInit;
import com.bearever.push.target.oppo.OppoInit;
import com.bearever.push.target.xiaomi.XiaomiInit;
import com.huawei.android.hms.agent.HMSAgent;
import com.huawei.android.hms.agent.common.handler.ConnectHandler;

/**
 * 初始化推送服务的管家，根据设备判断初始化哪个平台的推送服务；
 * *********
 * 如果你需要对接收到的信息进行处理，请实现 handle/impl里面的类：
 * HandleReceiverAlias----------------别名设置之后执行
 * HandleReceiverMessage--------------接收到消息之后执行，不会主动显示通知
 * HandleReceiverNotification---------接收到消息之后执行，主动显示通知
 * HandleReceiverNotificationOpened---用户点击了通知之后执行
 * *********
 * Created by luoming on 2018/5/28.
 */

public class PushTargetManager {
    private static final String TAG = "PushTargetManager";
    private static PushTargetManager instance;
    private PushTarget mTarget = PushTarget.JPUSH; //当前的推送平台
    private BasePushTargetInit mPushTarget; //当前选择的推送方式

    //设备厂商名
    private static String HUAWEI = "HUAWEI"; //华为
    private static String XIAOMI = "XIAOMI"; //小米
    private static String OPPO = "OPPO"; //oppo
    private static String MEIZU = "MEIZU"; //魅族

    public static PushTargetManager getInstance() {
        if (instance == null) {
            synchronized (PushTargetManager.class) {
                if (instance == null) {
                    instance = new PushTargetManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Application context) {
        String mobile_brand = android.os.Build.MANUFACTURER;
        mobile_brand = mobile_brand.toUpperCase();
        //根据设备厂商选择推送平台
        //小米的使用小米推送，华为使用华为推送...其他的使用极光推送
        if (XIAOMI.equals(mobile_brand)) {
            this.mTarget = PushTarget.XIAOMI;
            mPushTarget = new XiaomiInit(context);
        } else if (HUAWEI.equals(mobile_brand)) {
            this.mTarget = PushTarget.HUAWEI;
            mPushTarget = new HuaweiInit(context);
        } else if (OPPO.equals(mobile_brand)) {
            this.mTarget = PushTarget.OPPO;
            mPushTarget = new OppoInit(context);
        } else if (MEIZU.equals(mobile_brand)) {
            this.mTarget = PushTarget.MEIZU;
            mPushTarget = new MeizuInit(context);
        } else {
            this.mTarget = PushTarget.JPUSH;
            mPushTarget = new JPushInit(context);
        }
    }

    /**
     * 初始化华为推送
     *
     * @param activity
     */
    public void initHuaweiPush(Activity activity) {
        if (getPushTarget() != PushTarget.HUAWEI) {
            return;
        }
        // TODO: 2018/9/30 失败重试
        HMSAgent.connect(activity, new ConnectHandler() {
            @Override
            public void onConnect(int rst) {
                if (rst == HMSAgent.AgentResultCode.HMSAGENT_SUCCESS) {
                    //连接成功就获取token和设置打开推送等
                    HMSAgent.Push.getPushState(null);
                    HMSAgent.Push.getToken(null);
                }
            }
        });
    }

    /**
     * 设置别名，华为和魅族不可用，需要通过@link HandleReceiverAlias 的 handle方法回调取得
     *
     * @param context
     * @param alias   别名
     */
    public void setAliasNotWithHuaweiMeizu(Context context, String alias) {
        if (mPushTarget == null) {
            throw new NullPointerException("请先执行init()，然后在设置别名");
        }
        mPushTarget.setAlias(context, alias);
    }

    /**
     * 添加推送监听
     *
     * @param key
     * @param listener
     */
    public void addPushReceiverListener(String key, OnPushReceiverListener listener) {
        PushReceiverHandleManager.getInstance().addPushReceiverListener(key, listener);
    }

    /**
     * 移除一个推送监听
     *
     * @param key
     */
    public void removePushReceiverListener(String key) {
        PushReceiverHandleManager.getInstance().removePushReceiverListener(key);
    }

    /**
     * 清空推送监听
     */
    public void clearPushReceiverListener() {
        PushReceiverHandleManager.getInstance().clearPushReceiverListener();

    }

    /**
     * 获取当前选择的推送平台
     *
     * @return
     */
    public PushTarget getPushTarget() {
        return this.mTarget;
    }

    public interface OnPushReceiverListener {
        /**
         * 注册成功回调
         *
         * @param info
         */
        void onRegister(ReceiverInfo info);

        /**
         * 别名设置完成回调
         *
         * @param info
         */
        void onAlias(ReceiverInfo info);

        /**
         * 收到穿透消息的回调
         *
         * @param info
         */
        void onMessage(ReceiverInfo info);

        /**
         * 收到通知的回调
         *
         * @param info
         */
        void onNotification(ReceiverInfo info);

        /**
         * 点击通知的回调
         *
         * @param info
         */
        void onOpened(ReceiverInfo info);
    }
}
