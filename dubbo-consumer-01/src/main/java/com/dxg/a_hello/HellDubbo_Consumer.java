package com.dxg.a_hello;

import com.dxg.service.SayHello;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @auther 丁溪贵
 * @date 2019/10/23
 */
public class HellDubbo_Consumer {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("consumer01.xml");
        SayHello bean = ac.getBean(SayHello.class);
        // 远程调用hello方法
        System.out.println(bean.hello());
    }
}
