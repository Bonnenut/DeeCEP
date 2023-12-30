package sase.stream;

import java.util.List;

/**
 * 自定义数据结构 EventGroup，用于存储按时间戳分组的事件和事件类型的重复次数。
 */
public class EventGroup {
    // 事件列表
    private List<Event> events;
    // 事件类型的重复次数
    private int eventTypeCount;

    /**
     * 构造函数，初始化 EventGroup 对象。
     *
     * @param events         事件列表
     * @param eventTypeCount 事件类型的重复次数
     */
    public EventGroup(List<Event> events, int eventTypeCount) {
        this.events = events;
        this.eventTypeCount = eventTypeCount;
    }

    /**
     * 获取事件列表。
     *
     * @return 事件列表
     */
    public List<Event> getEvents() {
        return events;
    }

    /**
     * 获取事件类型的重复次数。
     *
     * @return 事件类型的重复次数
     */
    public int getEventTypeCount() {
        return eventTypeCount;
    }
}



