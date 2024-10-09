package com.datastat.aop;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.datastat.dao.RedisDao;
import com.datastat.result.ResultData;
import com.datastat.util.ClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class LimitRequestAspect {
    /**
     * 请求间隔时间的配置值.
     */
    @Value("${requestInterval:60000}")
    private String requestInterval;
    /**
     * Redis操作的DAO实例.
     */
    @Autowired
    private RedisDao redisDao;
    /**
     * 请求限制的配置值.
     */
    private final ConcurrentHashMap<String, CallMark> callMarkMap = new ConcurrentHashMap<>();
    /**
     * ObjectMapper实例，用于将对象转换为JSON字符串.
     */
    private ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 切点定义，匹配带有{@link LimitRequest}注解的方法.
     * @param limitRequest 限制请求注解
     */
    @Pointcut("@annotation(limitRequest)")
    public void exudeService(LimitRequest limitRequest) { }

    /**
      * 在执行指定方法之前，检查请求是否超过限制.
      *
      * @param joinPoint 连接点，用于获取方法签名和参数
      * @param limitRequest 限制请求对象，包含请求限制的相关信息
      * @return 返回方法执行结果，如果请求超过限制，则返回错误结果
      * @throws Throwable 抛出异常，如果方法执行过程中出现异常
      */
    @Around(value = "exudeService(limitRequest)", argNames = "joinPoint,limitRequest")
    public Object before(ProceedingJoinPoint joinPoint, LimitRequest limitRequest) throws Throwable {
        if (!isAllowed(joinPoint.getSignature().getName(), limitRequest)) {
            ResultData resultData = ResultData.fail(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests");
            return objectMapper.writeValueAsString(resultData);
        }

        return joinPoint.proceed();
    }

    /**
     * 检查给定的方法名称是否在指定的时间窗口内超过了限制请求的次数.
     *
     * @param methodName 方法名称
     * @param limitRequest 限制请求的配置
     * @return 如果超过了限制，则返回false；否则，返回true
     */
    public boolean isAllowed(String methodName, LimitRequest limitRequest) {
        Duration timeWindow = Duration.ofSeconds(limitRequest.callTime());
        Instant now = Instant.now();
        if (callMarkMap.containsKey(methodName)) {
            CallMark callMark = callMarkMap.get(methodName);
            if (Duration.between(callMark.getLastCallTime(), now).compareTo(timeWindow) > 0) {
                callMark.setLastCallTime(now);
                callMark.setCallCount(0);
            }

            if (callMark.getCallCount() < limitRequest.callCount()) {
                callMark.setCallCount(callMark.getCallCount() + 1);
                callMarkMap.put(methodName, callMark);
                return true;
            }
            return false;
        } else {
            CallMark callMark = new CallMark();
            callMark.setLastCallTime(now);
            callMark.setCallCount(1);
            callMarkMap.put(methodName, callMark);
            return true;
        }
    }
    /**
     * 限制IP地址访问频率的切面方法.
     * 当一个IP地址在指定的时间内的访问次数超过限制时，返回一个错误响应
     *
     * @param joinPoint 切点
     * @return 执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(RateLimit)")
    public Object limitIpAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            throw new RuntimeException("Not a web application");
        }

        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String ip = ClientUtil.getClientIpAddress(request);
        String key = "ip_access_record" + ip;
        String lastAccessTime = (String) redisDao.get(key);
        if (lastAccessTime != null
            && System.currentTimeMillis() - Long.valueOf(lastAccessTime) < Long.parseLong(requestInterval)) {
            ResultData resultData = ResultData.fail(HttpStatus.TOO_MANY_REQUESTS.value(),
              "Submit too frequently, please try again later");
            return objectMapper.writeValueAsString(resultData);
        }
        redisDao.set(key,  Long.toString(System.currentTimeMillis()), Long.parseLong(requestInterval));
        return joinPoint.proceed();
    }

}
