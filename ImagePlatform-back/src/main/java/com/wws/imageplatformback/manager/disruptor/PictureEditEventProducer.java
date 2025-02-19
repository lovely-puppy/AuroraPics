package com.wws.imageplatformback.manager.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.wws.imageplatformback.manager.websocket.model.PictureEditRequestMessage;
import com.wws.imageplatformback.model.entity.User;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件生产者
 */
@Component
@Slf4j
public class PictureEditEventProducer {

  @Resource Disruptor<PictureEditEvent> pictureEditEventDisruptor;

  public void publishEvent(
      PictureEditRequestMessage pictureEditRequestMessage,
      WebSocketSession session,
      User user,
      Long pictureId) {
    RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
    // 获取可以生成的位置
    long next = ringBuffer.next();
    PictureEditEvent pictureEditEvent = ringBuffer.get(next);
    pictureEditEvent.setSession(session);
    pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
    pictureEditEvent.setUser(user);
    pictureEditEvent.setPictureId(pictureId);
    // 发布事件
    ringBuffer.publish(next);
  }

  /** 优雅停机 */
  @PreDestroy
  public void close() {
    pictureEditEventDisruptor.shutdown();
  }
}
