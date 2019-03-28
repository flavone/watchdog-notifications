package com.cfpamf.jenkins;

/**
 * @author flavone
 * @date 2019/03/27
 */
public interface WatchDogService {
    /**
     * 发送消息
     * @param result
     */
    void send(Boolean result);
}
