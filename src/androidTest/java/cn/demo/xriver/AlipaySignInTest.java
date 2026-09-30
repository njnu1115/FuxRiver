package cn.demo.xriver;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/*
项目决策过程可参见 https://chat.z.ai/s/6c1e5d8e-7247-4c3e-99d3-878d7861554e
*/

/**
 * @startuml
 * [*] --> DETERMIN : Launch
 * HOME --> DETERMIN : startOver() via DEEPLINK
 * START --> HOLDOVER : randomClick()\nset timeout
 * HOLDOVER --> HOLDOVER : [3s] check popups / done markers
 * HOLDOVER --> DETERMIN : timeout reached\n(pressBack)
 * DETERMIN --> HOLDOVER : HOLDOVER strings found
 * DETERMIN --> START : START strings found
 * DETERMIN --> HOME : HOME strings found
 * DETERMIN --> PRESSBACK : nothing found
 * PRESSBACK --> DETERMIN : hold 10s, pressBack(), counter++
 * PRESSBACK --> [*] : counter reach 1000
 * @enduml
 */
@RunWith(AndroidJUnit4.class)
public class AlipaySignInTest {

    private static final String TAG = "FuxRiver19890604";
    private static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
    private static final String DEEP_LINK_URL =
            "alipays://platformapi/startapp?appId=68687805&url=https%3A%2F%2Frender.alipay.com%2Fp%2Fyuyan%2F180020380000000023%2Fpoint-sign-in.html";
    private static final long WAIT_TIMEOUT = 1000;
    private static final String DEVICE_NAME = android.os.Build.DEVICE;
    private static final int GO_FINISH_Y = DEVICE_NAME.equals("umi") ? 1600 : 2100;

    /* ================= 状态机参数 ================= */
    private static final long RUN_BUDGET_MS = 60 * 60 * 1000L;   // 唯一退出条件
    private static final long HOLDOVER_DWELL_MS           = 18000;    // HOLDOVER 等待
    private static final long PRESSBACK_HOLD_MS = 1000;   // PRESSBACK 停留 10s
    private static final long SETTLE_MS         = 1500;    // DETERMIN 观察前的稳定等待
    private static final Pattern TASK_FILTER = Pattern.compile("\\+3积分|\\+5积分|5分钟|玩一玩");

    /* ================= 状态判定字符串 ================= */
    private static final Pattern ANY_CN_TEXT = Pattern.compile(".*[\\\\u4e00-\\\\u9fa5].*");
    private static final String[] START_MARKERS = {
            "已完成", "连签", "已领取", "兑好物", "今天","明天", "后天", 
            "恭喜完成今日", "福利任务", "继续做任务赚积分吧"};
    private static final String[] HOME_MARKERS = {
            "扫一扫", "收付款", "卡包", "出行", "我的", "支付宝", "微信", "设置"};
	private static final Pattern HOLDOVER_PATTERN = Pattern.compile("看一看5分钟得奖励|剩余 [1-9]\\d* 秒");

    /* ================= 状态机 ================= */
    private enum State { DETERMIN, HOME, START, HOLDOVER, PRESSBACK }

    private UiDevice device;
    private Context context;
    private boolean scrollFlag = false;
    private State state = State.DETERMIN;   // [*] --> DETERMIN : Launch
    private List<UiObject2> lastDump;       // DETERMIN 的 dump，传给 START 复用
    private final Random rnd = new Random();

    @Before
    public void setUp() throws Exception {
        device = UiDevice.getInstance(getInstrumentation());
        context = getInstrumentation().getContext();
        device.setOrientationNatural();
        device.waitForWindowUpdate(ALIPAY_PACKAGE, WAIT_TIMEOUT);
    }

    @Test
    public void testAlipaySignIn() throws Exception {
        long t0 = System.currentTimeMillis();
        long budgetEnd = t0 + RUN_BUDGET_MS;

        while (System.currentTimeMillis() < budgetEnd) {
            logger("== state :=" + state );
            switch (state) {
                case DETERMIN:  determine();       break;
                case HOME:      startOver();       break;
                case START:     doStart(lastDump); break;
                case HOLDOVER:  doHoldover();      break;
                case PRESSBACK: doPressBack();     break;
            }
        }
        logger("======== Finished ========");
    }

    /** DETERMIN：整页 dump 一次，分类；dump 通过 lastDump 传给 START */
    private void determine() throws Exception {
        Thread.sleep(SETTLE_MS);
        lastDump = device.findObjects(By.clazz("android.widget.TextView"));   // 全流程唯一一次整页 dump
        // lastDump = device.findObjects(By.text(ANY_CN_TEXT));
        String screen = String.join("", extractAndLogTexts(lastDump));

        if (hitCount(screen, START_MARKERS) > 3)     { state = State.START;    return; }
        if (HOLDOVER_PATTERN.matcher(screen).find()) { state = State.HOLDOVER; return; }
        if (hitCount(screen, HOME_MARKERS) >= 2)     { state = State.HOME;     return; }
        state = State.PRESSBACK;   // nothing found
    }

    /**
     * START：直接在 DETERMIN 传进来的 dump 中过滤包含 {@link #TASK_FILTER} 的 TextView，
     * 随机挑一个文本，按文本重新查一次（避免列表滚过拿旧坐标），点击，进入 HOLDOVER。
     */
    private void doStart(List<UiObject2> dump) throws Exception {
        List<String> candidates = new ArrayList<>();
        if (dump != null) {
            for (UiObject2 obj : dump) {
                String text = safeText(obj);
                if (text != null && TASK_FILTER.matcher(text).find()) {
                    candidates.add(text);
                }
            }
        }


        if (candidates.isEmpty()) {
            logger("START: no task  -> 换一换 or 赚更多积分");
            List<UiObject2> refresh = device.findObjects(By.text(Pattern.compile("换一换|赚更多积分")));
            if (!refresh.isEmpty()) {
                refresh.get(0).click(); Thread.sleep(WAIT_TIMEOUT); 
            }else{ // 如果换一换 和 赚更多积分都没有，就往上滑一点点
                device.swipe(498, 999, 502, 666, 20);
            }
            state = State.DETERMIN;      // 回 DETERMIN 重新 dump
            return;                      // ★ 关键：不落入下面的任务点击流程
        }
        int randomIndex = rnd.nextInt(candidates.size());
        String pick = candidates.get(rnd.nextInt(candidates.size()));
        logger("candidate 选举完成 (共 " + candidates.size() + " 条): 「" + String.join("」「", candidates) + "」" + "picked: >>>>" + pick);
        UiObject2 obj = device.findObject(By.text(pick));
        if (obj == null) { logger("pick vanished -> PRESSBACK"); state = State.PRESSBACK; return; }
        if (pick.contains("+5秒")){scrollFlag = true;}

        obj.click();                           // ── randomClick ──
        Thread.sleep(2000);
        device.click(561, GO_FINISH_Y);        // "去完成"
        state = State.HOLDOVER;
    }

    /**
     * HOLDOVER：等到超时；弹窗压掉；完成标记提前走。
     * 已删除 scroll 分支 —— 不再按文本区分滑动任务，所有任务统一等待。
     */
    private void doHoldover() throws Exception {
        if(scrollFlag){
            for (int i = 0; i < 6; i++) {
                device.swipe(498, 999, 502, 666, 20);
                Thread.sleep(3333); 
            }
            scrollFlag = false;
        }
        else{Thread.sleep(HOLDOVER_DWELL_MS);}
        state = State.DETERMIN;
        // 无 scroll，统一等待
    }

    /** PRESSBACK：hold 10s → pressBack → counter++ → DETERMIN */
	private void doPressBack() throws Exception {
		Thread.sleep(PRESSBACK_HOLD_MS);
		String currentPkg = device.getCurrentPackageName();

		// 需要连续按两次返回的包名列表
		boolean needDoubleBack = 
				"com.taobao.taobao".equals(currentPkg)          // 淘宝
				|| "com.baidu.searchbox".equals(currentPkg)    // 百度App
				|| "com.baidu.searchbox.lite".equals(currentPkg)//百度极速版
				|| "com.taobao.etao".equals(currentPkg)        // 一淘
				|| "com.sankuai.meituan".equals(currentPkg)    // 美团
				|| "com.kuaishou.nebula".equals(currentPkg);   // 快手

		device.pressBack();
		logger("PRESSBACK:" + ", currentPkg=" + currentPkg);

		if (needDoubleBack) {
			Thread.sleep(300); // 两次返回之间间隔，按需调整
			device.pressBack();
			logger("PRESSBACK: Double back for target app." );
		}
		state = State.DETERMIN;
	}

    /** HOME：deeplink 回起点 → DETERMIN（进入 HOME 本身就说明不在起点，无需再判断） */
    private void startOver() throws Exception {
        logger("HOME: startOver via DEEPLINK");
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Thread.sleep(2500);          // 给 H5 加载时间，避免 HOME↔DETERMIN 快速互跳
        state = State.DETERMIN;
    }

    /* ================= helpers ================= */

    private static String safeText(UiObject2 obj) {
        try { return obj.getText(); } catch (Exception e) { return null; }
    }

    private static int hitCount(String screen, String[] markers) {
        int n = 0;
        for (String m : markers) if (screen.contains(m)) n++;
        return n;
    }

    private void logger(String msg) { Log.d(TAG, msg); }

    private List<String> extractAndLogTexts(List<UiObject2> objs) {
        List<String> textList = new ArrayList<>();
        if (objs == null || objs.isEmpty()) { logger("未找到任何匹配的节点。"); return textList; }
        for (UiObject2 obj : objs) {
            try {
                String text = obj.getText();
                if (text != null && !text.trim().isEmpty() && text.matches(".*[\\u4e00-\\u9fa5].*")) {
                    textList.add(text);
                }
            } catch (Exception e) { /* 忽略节点失效 */ }
        }
        if (textList.isEmpty()) {
            logger("提取完成，当前屏幕未发现包含中文的文本。");
        } else {
            logger("提取完成 (共 " + textList.size() + " 条): 「" + String.join("」「", textList) + "」");
        }
        return textList;
    }
}
