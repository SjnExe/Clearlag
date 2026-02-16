package me.minebuilders.clearlag.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import me.minebuilders.clearlag.config.ConfigValueType;

/** Created by TCP on 2/3/2016. */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigValue {

  /**
   * Path.
   *
   * @return path
   */
  String path() default "";

  /**
   * Value type.
   *
   * @return value type
   */
  ConfigValueType valueType() default ConfigValueType.PRIMITIVE;
}
