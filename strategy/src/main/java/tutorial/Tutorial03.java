package tutorial;

import cqt.goai.run.main.RunTask;
import cqt.goai.run.notice.NoticeType;

import java.util.Arrays;

/**
 * 教程2：本教程你将了解 如何通知 和 持久化信息。
 */

public class Tutorial03 extends RunTask {

    @Override
    protected void init() {
        notice.noticeLow("low notice");
        notice.noticeHigh("high notice");
        notice.noticeHigh("high notice email", NoticeType.Email);
        notice.noticeHigh("high notice telegram", NoticeType.Telegram);

        // 1 全局存储和读取信息
        global("test1", 123);
        int value1 = global("test1");

        // 2 实时显示信息
        // 显示字符串
        show("show string");
        // 显示表格
        show("show table", Arrays.asList("标题1", "标题2"),
                Arrays.asList("行11", "行12"),
                Arrays.asList("行21", "行22"));

        // 3 存储收益，显示收益曲线
        profit(1234);

    }

}
