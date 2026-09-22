package cn.demo.xriver;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.StaleObjectException;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
public class AlipaySignInTest {

	private static final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
//	private static final String ALIPAY_SIGN_IN_ACTIVITY = "com.eg.android.AlipayGphone/com.alipay.mobile.nebulax.xriver.activity.XRiverActivity";
	private static final String DEEP_LINK_URL = "alipays://platformapi/startapp?appId=68687805&url=https%3A%2F%2Frender.alipay.com%2Fp%2Fyuyan%2F180020380000000023%2Fpoint-sign-in.html";
	private static final int LOOP_COUNT = 64;
	private static final long WAIT_TIMEOUT = 1000; // ms
	private static final String DEVICE_NAME = android.os.Build.DEVICE;
	private static final int GO_FINISH_Y = DEVICE_NAME.equals("umi") ? 1600 : 2085;
	private static final String[] clickTexts = {
			"一键核算用电成本",
			"从支付宝首页访问会员",
			"合理规划用电开销",
			"天天签到赢奖励",
			"打卡签到领奖励",
			"打卡记录每天好心情",
			"智能算电省钱有道",
			"浏览机汤租机3秒",
			"浏览爱租相机3秒",
			"浏览租机猩3秒",
			"浏览网商贷15秒",
			"用电省钱精准算费",
			"电费明细精准呈现",
			"看5秒视频领积分",
			"逛15秒安全知识",
			"逛15秒支付有礼领红包",
			"逛15秒芝麻租赁频道",
			"逛15秒芝麻租赁首页",
			"逛一逛乐游记",
			"逛一逛里程币兑红包",
			"来余额宝攒钱节领红包",
			"逛一逛滴滴出行活动",
			"逛一逛余额宝",
			"逛一逛余额宝摇钱树",
			"逛一逛余额宝攒钱节",
			"逛一逛摇红包",
			"逛一逛支付宝运动路线",
			"逛一逛支付有礼",
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
			"逛大额账单",
			"逛我的快递包裹游历",
			"逛支付有礼每日攒红包",
			"逛热卖好货15秒",
			"每日浇水领真绿植",
			"逛蚂蚁庄园喂小鸡",
			"逛退款账单",
			"逛飞猪一日游景点门票",
			"集鸿运金抢兑红包"
	};
	private static final String[] scrollTexts = {
			"滑动浏览优品会场15秒",
			"逛热卖好货15秒",
			"逛15秒精选超值好物",
			"逛一逛国补好货会场",
			"滑动浏览15秒红包会场"
	};
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
			startOver();
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

		// ========== 智能轮询：替代原来的 Thread.sleep(330000) ==========
		long startTime = System.currentTimeMillis();
		long timeoutMs = 12 * 60 * 1000;   // 兜底超时
		long intervalMs = 20 * 1000;       // 检查间隔
		boolean taskCompleted = false;

		logger("ShortTV: 开始智能轮询，最长等待 24分钟...");

		while (System.currentTimeMillis() - startTime < timeoutMs) {
			Thread.sleep(intervalMs);

			// 1. 获取当前屏幕所有中文文本（复用你已有的方法）
			List<UiObject2> textViews = device.findObjects(By.clazz("android.widget.TextView"));
			List<String> chineseTexts = extractAndLogTexts(textViews);
			String screenText = String.join("", chineseTexts);

			long elapsedSec = (System.currentTimeMillis() - startTime) / 1000;
			logger("ShortTV: 已等待 " + elapsedSec + " 秒");

			if (screenText.contains("已获得奖励")) {
				device.pressBack();
				logger("ShortTV: 已获得奖励，点击返回<一层...");
			} else if (screenText.contains("账号风险监测") || screenText.contains("登录") ) {
				device.pressBack();
			} else if (screenText.contains("通用任务悬浮球") ||  screenText.contains("看短剧5分钟得15积分") || screenText.contains("看一看5分钟得奖励")) {
				logger("ShortTV: 积分计时进行中继续等待...");
			} else if (isBackToStartPage()){
				logger("Already returned to point page, end of ShortTVTask....");
				return;
			} else {
				// 情况 D：其他未知状态 → 记录日志，继续等
				device.pressBack();
				logger("ShortTV: 未匹配到已知状态，点击返回<一层");
			}
		}
		// 3. 超时兜底
		if (!taskCompleted) {
			logger("ShortTV: 达到超时，强制结束。");
		}

		// 4. 最终返回
		device.pressBack();
		logger("ShortTV: 任务结束，已按返回。");
	}

	@Test
	public void testAlipaySignIn() throws Exception {

		for (int i = 0; i < LOOP_COUNT; i++) {
			logger("Loop iteration: " + (i + 1));
			boolean isClicked = false;
			seekAndClick("赚更多积分");
			List<UiObject2> objs = device.findObjects(By.clazz("android.widget.TextView"));
			List<String> allTexts = extractAndLogTexts(objs);

			if (!objs.isEmpty()) {
				for (UiObject2 obj : objs) {
					try {
						String text = obj.getText();
						if (text == null || text.isEmpty())
							continue;

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
							if (text.equals(clickText) || text.endsWith("3秒") || text.equals("+3积分")) {
								logger("Found and clicked: " + clickText);
								ClickTask(obj);
								isClicked = true;
								break;
							}
						}
						if (isClicked)
							break;

						if (text.startsWith("玩一玩")) {
							logger("Found and clicked: " + text);
							obj.click();
							isClicked = true;
							Thread.sleep(WAIT_TIMEOUT);
							device.click(561, GO_FINISH_Y);
							Thread.sleep(200000);
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(intent);
							break;
						}
						if (isClicked)
							break;

						if (text.endsWith("5分钟")) {
							logger("Found and clicked: " + text);
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
                    }
				}
			}

			if (isClicked)
				continue;
			logger("No task found, checking for 换一换...");
			justChange();
		}
	}

	private void startOver() throws Exception {
		logger("Starting over...");
		if (isBackToStartPage() == false) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK_URL));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
			Thread.sleep(WAIT_TIMEOUT);
			seekAndClick("赚更多积分");
		} else {
			logger("Already in start page, no need to startover");
		}
	}

	private void logger(String msg) {
		Log.d("FuxRiver19890604", msg);
	}

	private List<String> extractAndLogTexts(List<UiObject2> objs) {
		List<String> textList = new ArrayList<>();
		
		if (objs == null || objs.isEmpty()) {
			logger("未找到任何匹配的节点。");
			return textList;
		}

		// 1. 遍历并过滤：仅保留包含中文的字符串
		for (UiObject2 obj : objs) {
			try {
				String text = obj.getText();
				// 判空 + 正则匹配：[\u4e00-\u9fa5] 匹配基本中文字符
				if (text != null && !text.trim().isEmpty() && text.matches(".*[\\u4e00-\\u9fa5].*")) {
					textList.add(text);
				}
			} catch (Exception e) {
				// 忽略节点失效 (StaleObjectException) 等异常，防止脚本崩溃
			}
		}
		
		// 2. 一口气打印结果
		if (textList.isEmpty()) {
			logger("提取完成，当前屏幕未发现包含中文的文本。");
		} else {
			// 核心拼接逻辑：将 List 转换为 「文本1」「文本2」「文本3」 的格式
			String joinedTexts = "「" + String.join("」「", textList) + "」";
			logger("提取完成 (共 " + textList.size() + " 条): " + joinedTexts);
		}
		
		return textList;
	}

	/**
	 * 判断当前是否回到了起点页面。
	 * 依据：检查屏幕上是否出现了特定的特征字符串，如果命中数量 > 3（即至少4个），则认为是起点。
	 *
	 * @return true 表示回到了起点页面，false 表示没有
	 */
	private boolean isBackToStartPage() {
		// 1. 定义特征字符串
		String[] markers = {
			"赚更多积分", 
			"我已连签", 
			"连签奖励", 
			"兑好物", 
			"恭喜完成今日", 
			"福利任务", 
			"继续做任务赚积分吧"
		};

		// 2. 一次性获取当前屏幕所有 TextView 的文本（避免多次查询 UI 树）
		StringBuilder screenTextBuilder = new StringBuilder();
		List<UiObject2> textViews = device.findObjects(By.clazz("android.widget.TextView"));

		for (UiObject2 obj : textViews) {
			try {
				String text = obj.getText();
				if (text != null) {
					screenTextBuilder.append(text);
				}
			} catch (Exception e) {
				// 忽略节点失效异常 (StaleObjectException)
			}
		}
		String screenText = screenTextBuilder.toString();

		// 3. 统计命中了多少个特征字符串
		int matchCount = 0;
		for (String marker : markers) {
			if (screenText.contains(marker)) {
				matchCount++;
				// 优化：如果已经满足条件（>3），直接提前退出循环
				if (matchCount > 3) {
					break; 
				}
			}
		}

		// 4. 打印日志并返回结果
		logger("起点页面特征匹配数: " + matchCount + " / " + markers.length);
		
		// 需求是“超过3个”，即 >= 4
		return matchCount > 3;
	}
}
