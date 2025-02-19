package com.wws.imageplatformback;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@EnableAsync
@MapperScan("com.wws.imageplatformback.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)//全局获取代理对象
public class ImagePlatformBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImagePlatformBackApplication.class, args);
    }

}
