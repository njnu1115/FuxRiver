package cn.demo.xriver;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.StaleObjectException;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.app.Instrumentation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AlipaySignInTest {

    private static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
    private static final String ALIPAY_SIGN_IN_ACTIVITY = "com.eg.android.AlipayGphone/com.alipay.mobile.nebulax.xriver.activity.XRiverActivity";
    private static final String DEEP_LINK_URL = "alipays://platformapi/startapp?appId=68687805&url=https%3A%2F%2Frender.alipay.com%2Fp%2Fyuyan%2F180020380000000023%2Fpoint-sign-in.html";
    private static final int LOOP_COUNT = 64;
    private static final long WAIT_TIMEOUT = 1000; // ms
    private static final String DEVICE_NAME = android.os.Build.DEVICE;
    private static final int GO_FINISH_Y = DEVICE_NAME.equals("umi") ? 1600 : DEVICE_NAME.equals("ruben") ? 1900 : 1900;
    private static final String[] clickTexts = {
            "一键核算用电成本",
            "合理规划用电开销",
            "打卡签到领奖励",
            "打卡记录每天好心情",
            "智能算电省钱有道",
            "浏览爱租相机3秒",
            "浏览租机猩3秒",
            "浏览机汤租机3秒",
            "浏览网商贷15秒",
            "用电省钱精准算费",
            "电费明细精准呈现",
            "看5秒视频领积分",
            "观看15秒视频领积分",
            "逛15秒安全知识",
            "逛15秒支付有礼领红包",
            "逛15秒芝麻租赁频道",
            "逛15秒芝麻租赁首页",
            "逛一逛乐游记",
            "逛一逛余额宝摇钱树",
            "逛一逛小米钱包APP",
            "逛一逛摇红包",
            "逛一逛支付宝运动路",
            "逛一逛每日惊喜不断",
            "逛一逛福气鱼塘",
            "逛一逛签到领红包",
            "逛一逛芝麻信用",
            "逛一逛芭芭农场",
            "逛一逛蚂蚁新村",
            "逛一逛蚂蚁森林",
            "逛一逛话费活动",
            "逛一逛领取优惠",
            "逛一逛领奖励",
            "逛一逛高德打车小程序",
            "逛双11会场",
            "逛热卖好货15秒",
            "逛蚂蚁庄园喂小鸡",
            "逛飞猪一日游景点门票",
            "集鸿运金抢兑红包"
    };
    private static final String[] scrollTexts = {
            "滑动浏览优品会场15秒",
            "逛热卖好货15秒",
            "逛15秒精选超值好物",
            "滑动浏览15秒红包会场"
    };
    private static final String[] awayBackTexts = {
            "逛一逛淘宝消消乐",
            "逛一逛淘宝视频",
            "逛一逛淘宝斗地主",
            "逛一逛淘金币频道",
            "逛一逛淘宝芭芭农场",
            "逛一逛天猫APP",
            "逛中国移动领流量",
            "逛百度天天领现金",
            "逛百度极速版领钱",
            "逛美团刷视频领现金",
    };

    private UiDevice device;
    private Context context;

    @Before
    public void setUp() throws Exception {
        device = UiDevice.getInstance(getInstrumentation());
        context = getInstrumentation().getContext();
        device.setOrientationNatural();

        device.waitForWindowUpdate(ALIPAY_PACKAGE, WAIT_TIMEOUT);
    }

    private boolean seekAndClick(String text) throws Exception {
        Thread.sleep(WAIT_TIMEOUT);
        UiObject2 obj = device.findObject(By.text(text));
        if (obj != null) {
            logger("Found and clicked: " + text);
            obj.click();
            Thread.sleep(WAIT_TIMEOUT);
            return true;
        } else {
            logger("Not found or not clickable: " + text);
            Thread.sleep(WAIT_TIMEOUT);
            return false;
        }
    }

    private void justChange() throws Exception {
        int screenHeight = device.getDisplayHeight();
        int screenWidth = device.getDisplayWidth();
        int centerX = screenWidth / 2;
        boolean isClicked = false;
        for (int j = 0; j < 4; j++) {
            UiObject2 obj = device.findObject(By.text("换一换"));
            if (obj != null) {
                obj.click();
                isClicked = true;
                // swipe to top and click "赚更多积分"
                device.swipe(540, 300, 540, 1500, 50);
                Thread.sleep(WAIT_TIMEOUT);
                seekAndClick("赚更多积分");
                break;
            } else {
                device.swipe(centerX, screenHeight * 3 / 4, centerX, screenHeight * 2 / 4, 200);
            }
        }
        if (!isClicked) {
            // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // context.startActivity(intent);
        }
    }

    private void ScrollTask(UiObject2 obj) throws Exception {
        obj.click();
        Thread.sleep(WAIT_TIMEOUT);
        device.click(561, GO_FINISH_Y);
        for (int j = 0; j < 9; j++) {
            Thread.sleep(WAIT_TIMEOUT);
            UiObject2 riskobj = device.findObject(By.text("账号风险检测"));
            if (riskobj != null) {
                device.pressBack();
                continue;
            }
            Thread.sleep(WAIT_TIMEOUT);
            device.swipe(561, 1000, 498, 800, 64);
            Thread.sleep(WAIT_TIMEOUT);
        }
        device.pressBack();
    }

    private void ClickTask(UiObject2 obj) throws Exception {
        obj.click();
        Thread.sleep(WAIT_TIMEOUT);
        device.click(561, GO_FINISH_Y);
        Thread.sleep(18000);
        device.pressBack();
    }

    private void ShortTVTask(UiObject2 obj) throws Exception {
        obj.click();
        Thread.sleep(WAIT_TIMEOUT);
        device.click(561, GO_FINISH_Y);
        Thread.sleep(WAIT_TIMEOUT);
        // pause the play by a single tap at the center of the screen
        int centerX = device.getDisplayWidth() / 2;
        int centerY = device.getDisplayHeight() / 2;
        device.click(centerX, centerY);
        // then double tap every 25 seconds to prevent screen lock
        for (int j = 0; j < 14; j++) {
            Thread.sleep(25000);
            device.click(centerX, centerY);
            Thread.sleep(500);
            device.click(centerX, centerY);
        }
        Thread.sleep(WAIT_TIMEOUT);
        device.pressBack();
    }

    @Test
    public void testAlipaySignIn() throws Exception {

        ArrayList<String> blacklist = new ArrayList<>();
        for (int i = 0; i < LOOP_COUNT; i++) {
            logger("Loop iteration: " + (i + 1));
            boolean isClicked = false;
            seekAndClick("赚更多积分");
            List<UiObject2> objs = device.findObjects(By.clazz("android.widget.TextView"));
            if (objs != null && !objs.isEmpty()) {
                for (UiObject2 obj : objs) {
                    try {
                        String text = obj.getText();
                        if (text == null || text.isEmpty())
                            continue;
                        for (String black : blacklist) {
                            if (text.equals(black)) {
                                continue;
                            }
                        }

                        for (String scrollText : scrollTexts) {
                            if (text.contains(scrollText)) {
                                ScrollTask(obj);
                                isClicked = true;
                                break;
                            }
                        }
                        if (isClicked)
                            break;

                        for (String clickText : clickTexts) {
                            if (text.equals(clickText)) {
                                ClickTask(obj);
                                isClicked = true;
                                break;
                            }
                        }
                        if (isClicked)
                            break;

                        if (text.endsWith("5分钟")) {
                            ShortTVTask(obj);
                            isClicked = true;
                            break;
                        }
                        if (isClicked)
                            break;

                        if (DEVICE_NAME.equals("umi")) {
                            for (String awayBackText : awayBackTexts) {
                                if (text.contains(awayBackText)) {
                                    obj.click();
                                    isClicked = true;
                                    Thread.sleep(WAIT_TIMEOUT);
                                    // blacklist.add(text);
                                    device.click(561, GO_FINISH_Y);
                                    Thread.sleep(8888);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                    break;
                                }
                            }
                            if (isClicked)
                                break;
                        }

                    } catch (StaleObjectException e) {
                        logger("StaleObjectException, skipping...");
                        continue;
                    }
                }
            }

            if (isClicked)
                continue;
            logger("No task found, checking for 换一换...");
            justChange();
        }
    }

    /**
     * 日志输出
     */
    private void logger(String msg) {
        System.out.println("[AlipaySignIn113] " + msg);
    }
}
