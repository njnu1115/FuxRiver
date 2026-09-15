package cn.demo.xriver;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.StaleObjectException;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
public class AlipaySignInTest {

    private static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
    private static final String ALIPAY_SIGN_IN_ACTIVITY = "com.eg.android.AlipayGphone/com.alipay.mobile.nebulax.xriver.activity.XRiverActivity";
    private static final String DEEP_LINK_URL = "alipays://platformapi/startapp?appId=68687805&url=https%3A%2F%2Frender.alipay.com%2Fp%2Fyuyan%2F180020380000000023%2Fpoint-sign-in.html";
    private static final int LOOP_COUNT = 64;
    private static final long WAIT_TIMEOUT = 1000; // ms
    private static final String DEVICE_NAME = android.os.Build.DEVICE;
    private static final int GO_FINISH_Y = DEVICE_NAME.equals("umi") ? 1600 : DEVICE_NAME.equals("rubens") ? 2085 : 2085;

    // ============================================================
    // Timing configuration
    //  - BUFFER_RATIO:           每个任务在 base time 之上固定加 20% buffer
    //  - SCROLL_INTERVAL_MS:     UnifiedTask 等待期间每隔 ~3s scroll 一次
    //  - DEFAULT_TASK_TIME_MS:   taskTimes 中未显式配置时的兜底
    //  - SHORT_TV_*:             ShortTVTask 5 分钟 + buffer（沿用）
    //  - PLAY_TASK_WAIT_MS:      「玩一玩」任务等待时长（沿用 200s）
    //  - AWAY_BACK_WAIT_MS:      awayBack 类任务点 Go 之后的等待（沿用 ~8.9s）
    // ============================================================
    private static final double BUFFER_RATIO = 0.20;
    private static final long SCROLL_INTERVAL_MS = 3_000L;
    private static final long DEFAULT_TASK_TIME_MS = 30_000L;
    private static final long SHORT_TV_WAIT_MS = 5 * 60 * 1000L;
    private static final long SHORT_TV_BUFFER_MS = 30 * 1000L;
    private static final long SHORT_TV_POLL_INTERVAL_MS = 5_000L;
    private static final long PLAY_TASK_WAIT_MS = 200_000L;
    private static final long AWAY_BACK_WAIT_MS = 8_888L;

    /** press back 试图回到目标 Activity 的最大次数，超过即告警 */
    private static final int MAX_BACK_ATTEMPTS = 10;

    // ============================================================
    // 任务表 —— 合并原 clickTexts + scrollTexts，每个任务配独立 base time（毫秒）
    // 匹配时按 key 长度降序迭代（避免 "逛一逛余额宝" 误匹配 "逛一逛余额宝摇钱树"）
    // ============================================================
    private static final Map<String, Long> taskTimes = new LinkedHashMap<>();
    static {
        // —— 3 秒类 ——
        taskTimes.put("浏览机汤租机3秒", 3_000L);
        taskTimes.put("浏览爱租相机3秒", 3_000L);
        taskTimes.put("浏览租机猩3秒", 3_000L);
        // —— 5 秒类 ——
        taskTimes.put("看5秒视频领积分", 5_000L);
        // —— 15 秒类 ——
        taskTimes.put("浏览网商贷15秒", 15_000L);
        taskTimes.put("观看15秒视频领积分", 15_000L);
        taskTimes.put("逛15秒安全知识", 15_000L);
        taskTimes.put("逛15秒支付有礼领红包", 15_000L);
        taskTimes.put("逛15秒芝麻租赁频道", 15_000L);
        taskTimes.put("逛15秒芝麻租赁首页", 15_000L);
        taskTimes.put("逛热卖好货15秒", 15_000L);
        taskTimes.put("逛15秒精选超值好物", 15_000L);
        taskTimes.put("滑动浏览优品会场15秒", 15_000L);
        taskTimes.put("滑动浏览15秒红包会场", 15_000L);
        // —— 默认 30 秒类（无明确秒数的） ——
        taskTimes.put("一键核算用电成本", DEFAULT_TASK_TIME_MS);
        taskTimes.put("从支付宝首页访问会员", DEFAULT_TASK_TIME_MS);
        taskTimes.put("合理规划用电开销", DEFAULT_TASK_TIME_MS);
        taskTimes.put("天天签到赢奖励", DEFAULT_TASK_TIME_MS);
        taskTimes.put("打卡签到领奖励", DEFAULT_TASK_TIME_MS);
        taskTimes.put("打卡记录每天好心情", DEFAULT_TASK_TIME_MS);
        taskTimes.put("智能算电省钱有道", DEFAULT_TASK_TIME_MS);
        taskTimes.put("用电省钱精准算费", DEFAULT_TASK_TIME_MS);
        taskTimes.put("电费明细精准呈现", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛乐游记", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛里程币兑红包", DEFAULT_TASK_TIME_MS);
        taskTimes.put("来余额宝攒钱节领红包", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛滴滴出行活动", DEFAULT_TASK_TIME_MS);
        // 注意：长的 key 排在前面，依赖 sortedTaskEntries 按 length desc 迭代
        taskTimes.put("逛一逛余额宝摇钱树", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛余额宝攒钱节", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛余额宝", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛摇红包", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛支付宝运动路线", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛支付有礼", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛每日惊喜不断", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛福气鱼塘", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛签到领红包", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛芝麻信用", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛芭芭农场", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛蚂蚁新村", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛蚂蚁森林", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛话费活动", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛领取优惠", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛领奖励", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛高德打车小程序", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛一逛国补好货会场", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛双11会场", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛大额账单", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛我的快递包裹游历", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛支付有礼每日攒红包", DEFAULT_TASK_TIME_MS);
        taskTimes.put("每日浇水领真绿植", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛蚂蚁庄园喂小鸡", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛退款账单", DEFAULT_TASK_TIME_MS);
        taskTimes.put("逛飞猪一日游景点门票", DEFAULT_TASK_TIME_MS);
        taskTimes.put("集鸿运金抢兑红包", DEFAULT_TASK_TIME_MS);
    }

    /** awayBack 类任务：跳到外部 App，需要靠深链拉回，不进 taskTimes 表 */
    private static final String[] awayBackTexts = {
            "逛一逛游戏中心",
            "逛一逛淘宝斗地主",
            "逛一逛淘宝消消乐",
            "逛一逛淘宝芭芭农场",
            "逛一逛淘宝视频",
            "逛一逛淘金币频道",
            "逛一逛小米钱包APP",
            "逛一逛大众点评",
            "逛淘宝签到领现金"
    };

    /** dumpsys ActivityRecord 行解析：ActivityRecord{hash u<uid> <component> t<taskid>} */
    private static final Pattern ACTIVITY_RECORD_PATTERN =
            Pattern.compile("ActivityRecord\\{\\S+ u\\d+ (\\S+) t\\d+");

    private final Random rand = new Random();
    private UiDevice device;
    private Context context;

    /** 按 key 长度降序排列的任务 entry 列表，匹配时优先长的，避免子串误匹配 */
    private List<Map.Entry<String, Long>> sortedTaskEntries;

    @Before
    public void setUp() throws Exception {
        device = UiDevice.getInstance(getInstrumentation());
        context = getInstrumentation().getContext();
        device.setOrientationNatural();

        sortedTaskEntries = new ArrayList<>(taskTimes.entrySet());
        sortedTaskEntries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        device.waitForWindowUpdate(ALIPAY_PACKAGE, WAIT_TIMEOUT);
    }

    // ============================================================
    // Random helpers —— 所有点击/滑动/睡眠都加抖动，规避机器人检测
    // ============================================================

    /**
     * 在 UiObject2 可见区域的中央 50% 区域内随机取点点击，
     * 避免每次都精确命中几何中心。
     */
    private void randomClick(UiObject2 obj) {
        Rect r = obj.getVisibleBounds();
        int halfW = Math.max(1, r.width() / 4);
        int halfH = Math.max(1, r.height() / 4);
        int x = r.centerX() + rand.nextInt(halfW * 2) - halfW;
        int y = r.centerY() + rand.nextInt(halfH * 2) - halfH;
        device.click(x, y);
    }

    /** 在指定坐标附近 ±10px 内随机抖动后点击 */
    private void randomTap(int x, int y) {
        int jx = rand.nextInt(21) - 10;
        int jy = rand.nextInt(21) - 10;
        device.click(x + jx, y + jy);
    }

    /** swipe 起止点都加 ±20px 抖动，steps 加 0..15 抖动，最小 10 */
    private void randomSwipe(int x1, int y1, int x2, int y2, int steps) {
        int jx1 = rand.nextInt(41) - 20;
        int jy1 = rand.nextInt(41) - 20;
        int jx2 = rand.nextInt(41) - 20;
        int jy2 = rand.nextInt(41) - 20;
        int jstep = rand.nextInt(16);
        device.swipe(x1 + jx1, y1 + jy1, x2 + jx2, y2 + jy2, Math.max(10, steps + jstep));
    }

    /** 睡眠 baseMs + 0..20% 的抖动 */
    private void randomSleep(long baseMs) throws InterruptedException {
        long jitter = (long) (baseMs * 0.20 * rand.nextDouble());
        Thread.sleep(baseMs + jitter);
    }

    // ============================================================
    // Activity stack helpers —— 状态机核心
    // ============================================================

    /**
     * 取当前栈顶 Activity 的 component name（pkg/.Act）。
     * 解析失败时返回原始 mResumedActivity 行字符串。
     */
    private String getCurrentTopActivity() throws Exception {
        String output = device.executeShellCommand("dumpsys activity activities");
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("mResumedActivity")) {
                String comp = extractComponentName(trimmed);
                return (comp != null) ? comp : trimmed;
            }
        }
        return "";
    }

    /**
     * 取当前 Activity stack 中所有 Activity 的 component name（顺序不一定严格按栈深，
     * 但 contains 判断足够用）。
     */
    private List<String> getActivityStack() throws Exception {
        String output = device.executeShellCommand("dumpsys activity activities");
        List<String> stack = new ArrayList<>();
        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("* ActivityRecord{")) {
                String comp = extractComponentName(trimmed);
                if (comp != null) {
                    stack.add(comp);
                }
            }
        }
        return stack;
    }

    private String extractComponentName(String line) {
        Matcher m = ACTIVITY_RECORD_PATTERN.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * 状态机：一路 press back，直到 top activity == targetActivity。
     *  - 如果 target 不在 stack 里：ERROR 日志后退出（说明已经回不去了，多半是目标 Activity 被销毁）
     *  - 如果到达 MAX_BACK_ATTEMPTS 仍未回到 target：WARN 日志后退出
     *  - targetActivity 为空：直接 pressBack 一次（兼容老行为）
     */
    private void pressBackUntil(String targetActivity, String taskName) throws Exception {
        if (targetActivity == null || targetActivity.isEmpty()) {
            logger(taskName + ": no target activity recorded, single pressBack fallback");
            device.pressBack();
            return;
        }

        // 已经在目标上，啥都不用做
        if (getCurrentTopActivity().equals(targetActivity)) {
            logger(taskName + ": already at target activity: " + targetActivity);
            return;
        }

        for (int i = 0; i < MAX_BACK_ATTEMPTS; i++) {
            List<String> stack = getActivityStack();
            if (!stack.contains(targetActivity)) {
                logger("ERROR [" + taskName + "]: target activity NOT in stack: "
                        + targetActivity + ", current top: " + getCurrentTopActivity()
                        + ", stack size: " + stack.size());
                return;
            }
            String current = getCurrentTopActivity();
            logger(taskName + ": pressing back (" + (i + 1) + "/" + MAX_BACK_ATTEMPTS
                    + "), current=" + current + ", target=" + targetActivity);
            device.pressBack();
            randomSleep(WAIT_TIMEOUT);

            if (getCurrentTopActivity().equals(targetActivity)) {
                logger(taskName + ": reached target activity: " + targetActivity);
                return;
            }
        }
        logger("WARN [" + taskName + "]: max back attempts reached, current top: "
                + getCurrentTopActivity());
    }

    // ============================================================
    // Generic helpers
    // ============================================================

    private boolean seekAndClick(String text) throws Exception {
        randomSleep(WAIT_TIMEOUT);
        UiObject2 obj = device.findObject(By.text(text));
        if (obj != null) {
            logger("Found and clicked: " + text);
            randomClick(obj);
            randomSleep(WAIT_TIMEOUT);
            return true;
        } else {
            logger("Not found or not clickable: " + text);
            randomSleep(WAIT_TIMEOUT);
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
                randomClick(obj);
                isClicked = true;
                // swipe to top and click "赚更多积分"
                randomSwipe(540, 300, 540, 1500, 50);
                randomSleep(WAIT_TIMEOUT);
                seekAndClick("赚更多积分");
                break;
            } else {
                randomSwipe(centerX, screenHeight * 3 / 4, centerX, screenHeight * 2 / 4, 200);
            }
        }
        if (!isClicked) {
            // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // context.startActivity(intent);
        }
    }

    // ============================================================
    // Task executors —— 每个任务都遵循 record → click → wait → return 模式
    // ============================================================

    /**
     * 统一任务执行器（取代原 ClickTask + ScrollTask）：
     *  1) 记录点击前的 Activity（即签到页 XRiverActivity）
     *  2) 随机点击 obj → 等 ~1s → 随机点击 Go 按钮
     *  3) 在 baseTime × (1 + BUFFER_RATIO) 的时间窗内：
     *     - 每 ~3s 检测一次"账号风险检测"弹窗，出现就 press back
     *     - 每 ~3s 随机 swipe 一次，模拟人类浏览
     *  4) press back 回到目标 Activity
     *
     * 即便原任务是"点击型"，等待期间也持续滑动，符合用户"统一按 Scroll 风格"的要求。
     */
    private void UnifiedTask(UiObject2 obj, long baseTimeMs, String taskName) throws Exception {
        String beforeActivity = getCurrentTopActivity();
        long totalMs = (long) (baseTimeMs * (1.0 + BUFFER_RATIO));
        logger(taskName + ": before=" + beforeActivity
                + ", base=" + baseTimeMs + "ms, with+20%buffer=" + totalMs + "ms");

        randomClick(obj);
        randomSleep(WAIT_TIMEOUT);
        randomTap(561, GO_FINISH_Y);

        int scrollCount = 0;
        long endTime = System.currentTimeMillis() + totalMs;
        while (System.currentTimeMillis() < endTime) {
            randomSleep(SCROLL_INTERVAL_MS);

            // 风控弹窗检测
            UiObject2 riskobj = device.findObject(By.text("账号风险检测"));
            if (riskobj != null) {
                logger(taskName + ": 账号风险检测 dialog detected, pressing back");
                device.pressBack();
                continue;
            }

            // 随机滑动 —— 模拟人类浏览，每次起点/终点/步长都抖动
            int h = device.getDisplayHeight();
            int w = device.getDisplayWidth();
            int cx = w / 2 + rand.nextInt(121) - 60;
            int y1 = h * 2 / 3 + rand.nextInt(80) - 40;
            int y2 = h / 3 + rand.nextInt(80) - 40;
            randomSwipe(cx, y1, cx + rand.nextInt(61) - 30, y2, 50);
            scrollCount++;
        }
        logger(taskName + ": wait finished, performed " + scrollCount + " random scrolls");
        pressBackUntil(beforeActivity, taskName);
    }

    /**
     * 短剧/长视频类任务（5 分钟）：
     *  1) 记录点击前的 Activity（即签到页 XRiverActivity），作为最终返回目标
     *  2) 点击 → 点 Go → 等待 Activity 切换稳定后，记录当前 top 为 targetActivity
     *  3) 在 SHORT_TV_WAIT_MS + SHORT_TV_BUFFER_MS 时间窗内轮询：
     *     - 如果 top != targetActivity，说明 target 被压下去了
     *     - 若 target 仍在 stack 中：press back 直到它回到 top
     *     - 若 target 已不在 stack 中：ERROR 日志，并尝试重发深链兜底
     *  4) 等待结束后 press back 回到签到页
     */
    private void ShortTVTask(UiObject2 obj) throws Exception {
        String homeActivity = getCurrentTopActivity();
        logger("ShortTVTask: home activity = " + homeActivity);

        randomClick(obj);
        randomSleep(WAIT_TIMEOUT);
        randomTap(561, GO_FINISH_Y);
        // 等目标 Activity 切换到位
        randomSleep(WAIT_TIMEOUT * 3);

        String targetActivity = getCurrentTopActivity();
        logger("ShortTVTask: target (post-click) activity = " + targetActivity);

        long totalWaitMs = SHORT_TV_WAIT_MS + SHORT_TV_BUFFER_MS;
        long endTime = System.currentTimeMillis() + totalWaitMs;

        while (System.currentTimeMillis() < endTime) {
            randomSleep(SHORT_TV_POLL_INTERVAL_MS);
            String current = getCurrentTopActivity();
            if (current.equals(targetActivity)) {
                continue;
            }

            // top 变了 —— target 被压下去了，或 target 没了
            List<String> stack = getActivityStack();
            if (!stack.contains(targetActivity)) {
                logger("ERROR [ShortTVTask]: target activity LOST during wait: "
                        + targetActivity + ", current top: " + current);
                // 兜底：重发深链回到签到页，重置 target 为签到页
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                randomSleep(WAIT_TIMEOUT * 3);
                targetActivity = getCurrentTopActivity();
                logger("ShortTVTask: re-launched deep link, new target = " + targetActivity);
                continue;
            }

            // target 仍在栈中，press back 直到它回到 top
            logger("ShortTVTask: top changed to " + current + ", target=" + targetActivity
                    + " still in stack, pressing back to restore");
            for (int i = 0; i < MAX_BACK_ATTEMPTS; i++) {
                if (getCurrentTopActivity().equals(targetActivity)) {
                    break;
                }
                device.pressBack();
                randomSleep(WAIT_TIMEOUT);
            }
            if (getCurrentTopActivity().equals(targetActivity)) {
                logger("ShortTVTask: target restored to top");
            } else {
                logger("WARN [ShortTVTask]: failed to restore target after "
                        + MAX_BACK_ATTEMPTS + " backs, current top: " + getCurrentTopActivity());
            }
        }

        // 5 分钟 + buffer 等待结束，回到签到页
        pressBackUntil(homeActivity, "ShortTVTask");
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
                        // 注：原代码的 blacklist 检测是 no-op（continue 仅跳过内层 for），
                        // 此处保留原行为，后续如需真正黑名单请改成标签 + break。
                        for (String black : blacklist) {
                            if (text.equals(black)) {
                                continue;
                            }
                        }

                        // —— 统一任务表匹配（按 key 长度降序，避免子串误匹配） ——
                        for (Map.Entry<String, Long> entry : sortedTaskEntries) {
                            if (text.contains(entry.getKey())) {
                                UnifiedTask(obj, entry.getValue(),
                                        "Task[" + entry.getKey() + "]");
                                isClicked = true;
                                break;
                            }
                        }
                        if (isClicked)
                            break;

                        if (text.startsWith("玩一玩")) {
                            String beforeActivity = getCurrentTopActivity();
                            logger("PlayTask: before activity = " + beforeActivity);
                            randomClick(obj);
                            isClicked = true;
                            randomSleep(WAIT_TIMEOUT);
                            randomTap(561, GO_FINISH_Y);
                            Thread.sleep(PLAY_TASK_WAIT_MS);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            randomSleep(WAIT_TIMEOUT * 3);
                            pressBackUntil(beforeActivity, "PlayTask");
                            break;
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
                                    String beforeActivity = getCurrentTopActivity();
                                    logger("AwayBackTask: before activity = " + beforeActivity);
                                    randomClick(obj);
                                    isClicked = true;
                                    randomSleep(WAIT_TIMEOUT);
                                    // blacklist.add(text);
                                    randomTap(561, GO_FINISH_Y);
                                    Thread.sleep(AWAY_BACK_WAIT_MS);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                    randomSleep(WAIT_TIMEOUT * 3);
                                    pressBackUntil(beforeActivity, "AwayBackTask");
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
