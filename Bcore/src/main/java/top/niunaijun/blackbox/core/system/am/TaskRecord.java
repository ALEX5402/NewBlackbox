package top.niunaijun.blackbox.core.system.am;

import android.content.Intent;

import java.util.LinkedList;
import java.util.List;

/**
 * updated by alex5402 on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class TaskRecord {
    public int id;
    public int userId;
    public String taskAffinity;
    public Intent rootIntent;
    public final List<ActivityRecord> activities = new LinkedList<>();

    public TaskRecord(int id, int userId, String taskAffinity) {
        this.id = id;
        this.userId = userId;
        this.taskAffinity = taskAffinity;
    }

    public boolean needNewTask() {
        for (ActivityRecord activity : activities) {
            if (!activity.finished) {
                return false;
            }
        }
        return true;
    }

    public void addTopActivity(ActivityRecord record) {
        activities.add(record);
    }

    public void removeActivity(ActivityRecord record) {
        activities.remove(record);
    }

    public ActivityRecord getTopActivityRecord() {
        for (int i = activities.size() - 1; i >= 0; i--) {
            ActivityRecord activityRecord = activities.get(i);
            if (!activityRecord.finished) {
                return activityRecord;
            }
        }
        return null;
    }
}
