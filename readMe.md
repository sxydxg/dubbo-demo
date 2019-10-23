# **丁溪贵dubbo学习总结--于2019/10/23** 

dubbo测试版本 2.6.2  - >(Jun 05, 2018) 

## 1.启动时检查(在spring容器启动时即使没有服务也不会报错，只有在调用时才检查)

```
<dubbo:reference  check="false" /> (优先级更高)
or
<dubbo:consumer check="false" />
```



# 2.集群容错（当调用一个服务出错，处理方案）

* **failover cluster** (**默认**) 失败自动切换，当出现失败，重试其它服务器 [[1\]](http://dubbo.apache.org/zh-cn/docs/user/demos/fault-tolerent-strategy.html#fn1)。通常用于读操作，但重试会带来更长延迟。可通过 `retries="2"` 来设置重试次数(不含第一次)。

  ```
  设置重试次数
  <dubbo:service retries="2" />  
  
  <dubbo:reference retries="2" />
  
  <dubbo:reference>
      <dubbo:method name="xxxx" retries="2" />
  </dubbo:reference>
  
  ```

* **failfast cluster**  快速失败，只发起一次调用，失败立即报错。通常用于非幂等性的写操作，比如新增记录
* **failsafe cluster**  失败安全，出现异常时，直接忽略。通常用于写入审计日志等操作
* **failback cluster**  失败自动恢复，后台记录失败请求，定时重发。通常用于消息通知操作
* **forking cluster**    并行调用多个服务器，只要一个成功即返回。通常用于实时性要求较高的读操作，但需要浪费更多服务资源。可通过 `forks="2"` 来设置最大并行数
* **boradcast cluster**  广播调用所有提供者，逐个调用，任意一台报错则报错 [[2\]](http://dubbo.apache.org/zh-cn/docs/user/demos/fault-tolerent-strategy.html#fn2)。通常用于通知所有提供者更新缓存或日志等本地资源信息。

```xml
如何配置集群容错方案？
<dubbo:service cluster="failsafe" />
或
<dubbo:reference cluster="failsafe" />

```

# 3.负载均衡

在集群负载均衡时，Dubbo 提供了多种均衡策略，默认为 `random` 随机调用

* **Random LoadBalance (默认)**
  * 随机，按权重设置随机概率
  * 在一个截面上碰撞的概率高，但调用量越大分布越均匀，而且按概率使用权重后也比较均匀，有利于动态调整提供者权重
* **RoundRobin LoadBalance**
  * 轮询，按公约后的权重设置轮询比率
  * 存在慢的提供者累积请求的问题，比如：第二台机器很慢，但没挂，当请求调到第二台时就卡在那，久而久之，所有请求都卡在调到第二台上

```
比如A,B,C台机器的权重比例分别是  10:30:60
每次10次调用情况大概是：(如果莫一台老是没执行会有补偿机制)
  a,b,b,b,c,c,c,c,c,c

```

* **LeastActive LoadBalance**
  * 最少活跃调用  （每次服务调用都会计算一个调用的差值指标）
  * 意思是优先调用响应速度最快的服务
* **ConsistentHash LoadBalance**
  * 一致性hash ，相同参数的请求总是发到同一提供者
  * 当某一台提供者挂时，原本发往该提供者的请求，基于虚拟节点，平摊到其它提供者，不会引起剧烈变动
  * 缺省只对第一个参数 Hash，如果要修改，请配置 `<dubbo:parameter key="hash.arguments" value="0,1" />`，注意该标签一般嵌套在`<dubbo:method > ' 中
  * 缺省用 160 份虚拟节点，如果要修改，请配置 `<dubbo:parameter key="hash.nodes" value="320" />`

```xml
如何配置集群环境中的负载均衡呢？

服务端服务级别:
<dubbo:service interface="..." loadbalance="roundrobin" />

客户端服务级别:
<dubbo:reference interface="..." loadbalance="roundrobin" />

服务端方法级别:
<dubbo:service interface="...">
    <dubbo:method name="..." loadbalance="roundrobin"/>
</dubbo:service>

客户端方法级别:
<dubbo:reference interface="...">
    <dubbo:method name="..." loadbalance="roundrobin"/>
</dubbo:reference>

```

# 4.多协议

dubbo远程rpc调用同时支持多种通信协议

* dubbo 协议
  * 采用nio复用单一长连接，并使用线程池处理并发请求，减少握手和加大并发效率，性能较好（官方推荐）
  * 适用与高并发，数据量较小的环境
*  rmi（Remote Method Invocation）协议：jdk自带的能力，可与元素rmi进行交互，基于tcp协议
* hessian协议：可与原始的hessian相互操作，基于http协议

```xml
可以同时使用多种协议，配置如下

 1.不同服务不同协议
	<!-- 多协议配置 -->
    <dubbo:protocol name="dubbo" port="20880" />
    <dubbo:protocol name="rmi" port="1099" />
    <!-- 使用dubbo协议暴露服务 -->
    <dubbo:service interface="xxxx1" version="1.0.0" ref="xx1" protocol="dubbo" />
    <!-- 使用rmi协议暴露服务 -->
    <dubbo:service interface="xxxxx2" version="1.0.0" ref="xx2" protocol="rmi" /> 
    
 2.多协议暴露服务（服务端会根据服务端的请求自动使用相应协议）
 <!-- 多协议配置 -->
    <dubbo:protocol name="dubbo" port="20880" />
    <dubbo:protocol name="hessian" port="8080" />
    <!-- 使用多个协议暴露服务 -->
    <dubbo:service id="xxx" interface="xxxx" version="1.0.0" protocol="dubbo,hessian" />

```

## 5.多注册中心

Dubbo 支持同一服务向多注册中心同时注册，或者不同服务分别注册到不同的注册中心上去，甚至可以同时引用注册在不同注册中心上的同名服务。另外，注册中心是支持自定义扩展的

* zookeeper
* redis 
* 服务提供者和消费者直连
* 采用组播的方法（multicast）
* simple (使用一个dubbo程序作为注册中心，不支持集群)

```xml
多注册中心使用案列：
	注意 id是自定义注册中心引用bean
	1.一个服务注册到多个注册中心
	<dubbo:registry id="hangzhouRegistry" address="10.20.141.150:9090" />
    <dubbo:registry id="qingdaoRegistry" address="10.20.141.151:9010" default="false" />
    <!-- 向多个注册中心注册 -->
    <dubbo:service interface="xxx" version="1.0.0" ref="xxx" 
    registry="hangzhouRegistry,qingdaoRegistry" />
	
	2.不同服务使用不同注册
	<dubbo:registry id="chinaRegistry" address="10.20.141.150:9090" />
    <dubbo:registry id="intlRegistry" address="10.20.154.177:9010" default="false" />
    <!-- 向中文站注册中心注册 -->
    <dubbo:service interface="xxx" version="1.0.0" 
    ref="xxx" registry="chinaRegistry" />
    <!-- 向国际站注册中心注册 -->
    <dubbo:service interface="xx" version="1.0.0" 
    ref="xx" registry="intlRegistry" />
    
    3.相同服务注册中心使用 , 进行分割
	<dubbo:registry protocol="zookeeper" address="10.20.154.177:9010,10.20.154.177:9011" 	
                    default="false" />

```

5.本地存根

远程服务后，客户端通常只剩下接口，而实现全在服务器端，但提供方有些时候想在客户端也执行部分逻辑。

```xml
spring 配置
<dubbo:service interface="Eat" stub="true" />
或
<dubbo:service interface="Eat" stub="Eat_Stub" />
疑问：为什么是service，不应该是reference吗？
我个人是使用reference端的。

```

实现原理: dubbo会自动通过构造函数的方式将代理对象注入其中

```java
// 该类的实现类有provider实现
public interface Eat(){
	void eat();
}
// 希望在调用eat()前线洗手
public class Eat_Stub implements Eat{
    private Eat proxy ;
    public Eat_Stub Eat proxy){
        this.proxy = proxy ;
    }
    public void eat(){
        //在消费端调用
        System.out.println("先洗手哦！");
        proxy.eat();
    }
}
```





