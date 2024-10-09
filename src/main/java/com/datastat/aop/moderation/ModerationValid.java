package com.datastat.aop.moderation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.datastat.dao.RedisDao;
import com.datastat.util.ModerationUtil;
import java.lang.annotation.RetentionPolicy;


import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * 用于检查字符串是否符合规则的注解.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ModerationValid.List.class)
@Constraint(validatedBy = ModerationValid.ModerationValidator.class)
public @interface ModerationValid {
    /**
     * 错误消息，当字符串不符合规则时显示.
     *
     * @return 错误消息
     */
    String message() default "Parameter contains sensitive words";
    /**
     * 验证组，用于指定在哪个验证组中使用此验证器.
     *
     * @return 验证组
     */
    Class<?>[] groups() default {};
    /**
     * 载荷，用于将验证信息传递给验证器.
     *
     * @return 载荷
     */
    Class<? extends Payload>[] payload() default {};

    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        /**
         * 获取`ModerationValid`注解的数组.
         *
         * @return `ModerationValid`注解的数组
         */
      ModerationValid[] value();
    }

  /**
   * 实现{@link ConstraintValidator}接口，用于检查字符串是否符合规则.
   */
    class ModerationValidator implements ConstraintValidator<ModerationValid, String> {
        /**
         * Redis数据访问对象，用于与Redis数据库进行交互.
         */
        @Autowired
        private RedisDao redisDao;

        /**
         * 环境对象，用于获取应用程序配置的属性.
         */
        @Autowired
        private Environment env;

        /**
         * 检查给定的字符串是否有效.
         *
         * @param value 要检查的字符串
         * @param context 约束验证上下文
         * @return 如果字符串有效，则返回true；否则返回false
         */
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            String token = (String) redisDao.get("nps_moderation_token");
            if (token == null) {
                token = ModerationUtil.getHuaweiCloudToken(env.getProperty("moderation.body.format"),
                        env.getProperty("moderation.user.name"), env.getProperty("moderation.user.password"),
                        env.getProperty("moderation.domain.name"), env.getProperty("moderation.token.endpoint"));
                redisDao.set("nps_moderation_token", token, 36000L);
            }
            if (ModerationUtil.moderation(env.getProperty("moderation.url"), value, token)) {
              return true;
            }
            return false;
        }

    }
}
