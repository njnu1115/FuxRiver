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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * @startuml
 * [*] --> DETERMIN : Launch
 * HOME --> DETERMIN : startOver() via DEEPLINK
 * START --> HOLDOVER : pickAndClick()\nset scrollFlag\nset timeout
 * HOLDOVER --> HOLDOVER : [3s] scrollFlag=true\nscroll()
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
    private static final String DEEP_LINK_URL = "alipays://platformapi/startapp?appId=68687805&url=https%3A%2F%2Frender.alipay.com%2Fp%2Fyuyan%2F180020380000000023%2Fpoint-sign-in.html";
    private static final long WAIT_TIMEOUT = 1000;
    private static final String DEVICE_NAME = android.os.Build.DEVICE;
    private static final int GO_FINISH_Y = DEVICE_NAME.equals("umi") ? 1600 : 2085;

    /* ================= 状态机参数 ================= */
    private static final long TICK_MS           = 3000;    // HOLDOVER 心跳
    private static final long PRESSBACK_HOLD_MS = 10000;   // PRESSBACK 停留 10s
    private static final long RESCUE_HOLD_MS    = 15000;   // DETERMIN 直入 HOLDOVER（无任务上下文）的兜底时长
    private static final long SETTLE_MS         = 1500;    // DETERMIN 观察前的稳定等待
    private static final int  MAX_PRESSBACK     = 1000;    // counter 上限 → 结束
    private static final int  MAX_TASKS         = 64;      // 任务数上限
    private static final int  MAX_EMPTY_START   = 5;       // 连续捡不到任务的空轮上限

    /* ================= 状态判定字符串 ================= */
    private static final String[] START_MARKERS = {
            "赚更多积分", "我已连签", "连签奖励", "兑好物",
            "恭喜完成今日", "福利任务", "继续做任务赚积分吧"};
    private static final String[] HOME_MARKERS = {
            "扫一扫", "收付款", "卡包", "出行", "我的"};
    private static final String[] HOLDOVER_MARKERS = {      // DETERMIN 认定"还在任务页"
            "已获得奖励", "账号风险检测", "账号风险监测", "通用任务悬浮球"};
    private static final String[] DONE_MARKERS  = {"已获得奖励"};                    // 提前完成
    private static final String[] POPUP_MARKERS = {"账号风险检测", "账号风险监测", "登录"}; // 弹窗 → back 压掉

    /* ================= 任务文案 ================= */
    private static final String[] clickTexts = {
            "一键核算用电成本", "从支付宝首页访问会员", "合理规划用电开销",
            "天天签到赢奖励", "打卡签到领奖励", "打卡记录每天好心情",
            "智能算电省钱有道", "浏览机汤租机3秒", "浏览爱租相机3秒",
            "浏览租机猩3秒", "浏览网商贷15秒", "用电省钱精准算费",
            "电费明细精准呈现", "看5秒视频领积分", "逛15秒安全知识",
            "逛15秒支付有礼领红包", "逛15秒芝麻租赁频道", "逛15秒芝麻租赁首页",
            "逛一逛乐游记", "逛一逛里程币兑红包", "来余额宝攒钱节领红包",
            "逛一逛滴滴出行活动", "逛一逛余额宝", "逛一逛余额宝摇钱树",
            "逛一逛余额宝攒钱节", "逛一逛摇红包", "逛一逛支付宝运动路线",
            "逛一逛支付有礼", "逛一逛每日惊喜不断", "逛一逛福气鱼塘",
            "逛一逛签到领红包", "逛一逛芝麻信用", "逛一逛芭芭农场",
            "逛一逛蚂蚁新村", "逛一逛蚂蚁森林", "逛一逛话费活动",
            "逛一逛领取优惠", "逛一逛领奖励", "逛一逛高德打车小程序",
            "逛双11会场", "逛大额账单", "逛我的快递包裹游历",
            "逛支付有礼每日攒红包", "逛热卖好货15秒", "每日浇水领真绿植",
            "逛蚂蚁庄园喂小鸡", "逛退款账单", "逛飞猪一日游景点门票",
            "集鸿运金抢兑红包"
    };
    private static final String[] scrollTexts = {
            "滑动浏览优品会场15秒", "逛热卖好货15秒", "逛15秒精选超值好物",
            "逛一逛国补好货会场", "滑动浏览15秒红包会场"
    };
    private static final String[] awayBackTexts = {
            "逛一逛游戏中心", "逛一逛淘宝斗地主", "逛一逛淘宝消消乐",
            "逛一逛淘宝芭芭农场", "逛一逛淘宝视频", "逛一逛淘金币频道",
            "逛一逛小米钱包APP", "逛一逛大众点评", "逛淘宝签到领现金"
    };

    /* ================= 任务表：全部 CONTAINS，差异只剩 holdMs 和 scrollFlag ================= */
    private static class Group {
        final String[] texts;
        final long holdMs;
        final boolean scroll;
        Group(String[] texts, long holdMs, boolean scroll) {
            this.texts = texts; this.holdMs = holdMs; this.scroll = scroll;
        }
        boolean matches(String text) {
            for (String t : texts) if (text.contains(t)) return true;
            return false;
        }
    }

    /** 顺序即优先级：scrollTexts 的 "逛热卖好货15秒" 同时也在 clickTexts 里，放前面保证按滑动处理 */
    private static final Group[] GROUPS = {
            new Group(scrollTexts,            21_000,    true),   // 滑动类：边等边滑（≈原 ScrollTask 9 次滑动）
            new Group(clickTexts,             18_000,    false),  // 普通点击类
            new Group(new String[]{"5分钟"},  12*60_000, false),  // 短剧类
            new Group(new String[]{"玩一玩"}, 200_000,   false),  // 小游戏
            new Group(awayBackTexts,          8_888,     false),  // 跳出去再回来类
    };

    /* ================= 状态机 ================= */
    private enum State { DETERMIN, HOME, START, HOLDOVER, PRESSBACK }

    private UiDevice device;
    private Context context;

    private State state = State.DETERMIN;   // [*] --> DETERMIN : Launch
    private List<UiObject2> lastDump;       // DETERMIN 的 dump，传给 START 复用
    private Group task;                     // 当前任务（HOLDOVER 的参数来源）
    private long deadline;                  // HOLDOVER 截止时间
    private int pressBackCount;             // 图中的 counter
    private int taskCount;
    private int emptyStart;

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
        while (pressBackCount < MAX_PRESSBACK && taskCount < MAX_TASKS && emptyStart < MAX_EMPTY_START) {
            logger("== state=" + state + " tasks=" + taskCount + " backs=" + pressBackCount);
            switch (state) {
                case DETERMIN:  determine();       break;
                case HOME:      startOver();       break;   // --> DETERMIN
                case START:     doStart(lastDump); break;
                case HOLDOVER:  doHoldover();      break;
                case PRESSBACK: doPressBack();     break;
            }
        }
        logger("Finished: tasks=" + taskCount + ", counter=" + pressBackCount
                + ", " + (System.currentTimeMillis() - t0) / 1000 + "s");
    }

    /** DETERMIN：整页 dump 一次，分类；dump 通过 lastDump 传给 START */
    private void determine() throws Exception {
        Thread.sleep(SETTLE_MS);
        lastDump = device.findObjects(By.clazz("android.widget.TextView"));   // 全流程唯一一次整页 dump
        String screen = String.join("", extractAndLogTexts(lastDump));

        if (hitCount(screen, START_MARKERS) > 3)   { state = State.START;    return; }
        if (containsAny(screen, HOLDOVER_MARKERS)) { state = State.HOLDOVER; return; }
        if (hitCount(screen, HOME_MARKERS) >= 2)   { state = State.HOME;     return; }
        // 兜底：不在支付宝前台（冷启动/进程被杀）→ 直接 deeplink，避免空转 back
        if (!ALIPAY_PACKAGE.equals(device.getCurrentPackageName())) {
            logger("not in alipay (" + device.getCurrentPackageName() + ") -> HOME");
            state = State.HOME;
            return;
        }
        state = State.PRESSBACK;   // nothing found
    }

    /** START：直接用 DETERMIN 传进来的 dump 挑任务，不再重复 dump */
    private void doStart(List<UiObject2> dump) throws Exception {
        String pick = null;
        Group g = null;
        if (dump != null) {
            for (UiObject2 obj : dump) {
                String text = safeText(obj);
                if (text == null || text.isEmpty()) continue;
                g = matchGroup(text);
                if (g != null) { pick = text; break; }
            }
        }
        if (pick == null) {                    // 无任务可捡 → 退出去刷新
            emptyStart++;
            logger("START: no task (" + emptyStart + "/" + MAX_EMPTY_START + ") -> PRESSBACK");
            state = State.PRESSBACK;
            return;
        }
        logger("pick: 「" + pick + "」 hold=" + g.holdMs + "ms scroll=" + g.scroll);

        seekAndClick("赚更多积分");             // 原有习惯动作：回任务区

        // 锚点点击后列表可能滚过，只按文本补一次单点查询，避免拿旧坐标误点
        UiObject2 obj = device.findObject(By.text(pick));
        if (obj == null) { logger("pick vanished -> PRESSBACK"); state = State.PRESSBACK; return; }

        obj.click();                           // ── pickAndClick ──
        Thread.sleep(WAIT_TIMEOUT);
        device.click(561, GO_FINISH_Y);        // "去完成"

        task = g;
        deadline = System.currentTimeMillis() + g.holdMs;   // set timeout
        taskCount++;
        emptyStart = 0;
        state = State.HOLDOVER;
    }

    /** HOLDOVER：等到超时；scrollFlag=true 每 3s 滑一次；弹窗压掉；完成标记提前走 */
    private void doHoldover() throws Exception {
        if (task == null) deadline = System.currentTimeMillis() + RESCUE_HOLD_MS;  // DETERMIN 直入
        if (System.currentTimeMillis() >= deadline) {                              // timeout reached
            logger("HOLDOVER: timeout -> pressBack -> DETERMIN");
            device.pressBack();
            task = null;
            state = State.DETERMIN;
            return;
        }

        Thread.sleep(TICK_MS);
        String screen = screenText();

        if (containsAny(screen, DONE_MARKERS)) {           // 提前完成，省时间
            logger("HOLDOVER: 已获得奖励 -> pressBack -> DETERMIN");
            device.pressBack();
            task = null;
            state = State.DETERMIN;
            return;
        }
        if (containsAny(screen, POPUP_MARKERS)) {          // 风控/登录弹窗 → back 压掉
            device.pressBack();
            return;
        }
        if (task != null && task.scroll) {                 // scrollFlag=true → scroll()
            device.swipe(561, 1000, 498, 800, 64);
        }
    }

    /** PRESSBACK：hold 10s → pressBack → counter++ → DETERMIN */
    private void doPressBack() throws Exception {
        Thread.sleep(PRESSBACK_HOLD_MS);
        device.pressBack();
        pressBackCount++;
        logger("PRESSBACK: counter=" + pressBackCount);
        state = State.DETERMIN;
    }

    /** HOME：deeplink 回起点 → DETERMIN（进入 HOME 本身就说明不在起点，无需再判断） */
    private void startOver() throws Exception {
        logger("HOME: startOver via DEEPLINK");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        for (int i = 0; i < 3 && !seekAndClick("赚更多积分"); i++) { }   // H5 加载慢，重试锚点
        state = State.DETERMIN;
    }

    /* ================= helpers ================= */

    private static Group matchGroup(String text) {
        for (Group g : GROUPS) if (g.matches(text)) return g;
        return null;
    }

    private boolean seekAndClick(String text) throws Exception {
        Thread.sleep(WAIT_TIMEOUT);
        UiObject2 obj = device.findObject(By.text(text));
        if (obj == null) { logger("Not found: " + text); return false; }
        logger("Found and clicked: " + text);
        obj.click();
        Thread.sleep(WAIT_TIMEOUT);
        return true;
    }

    private String screenText() {
        return String.join("", extractAndLogTexts(device.findObjects(By.clazz("android.widget.TextView"))));
    }

    private static String safeText(UiObject2 obj) {
        try { return obj.getText(); } catch (Exception e) { return null; }
    }

    private static int hitCount(String screen, String[] markers) {
        int n = 0;
        for (String m : markers) if (screen.contains(m)) n++;
        return n;
    }

    private static boolean containsAny(String screen, String[] markers) {
        for (String m : markers) if (screen.contains(m)) return true;
        return false;
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
