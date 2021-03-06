/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.silverware.microservices.annotations.hystrix.basic;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies the name of a thread pool used to execute calls (in Hystrix commands).
 * If not present, the name of the called microservice is used as a thread pool name.
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface ThreadPool {

   int DEFAULT_SIZE = 10;

   /**
    * Defines the name of the thread pool used to execute Hystrix commands.
    *
    * @return the name of the thread pool
    */
   String name() default "";

   /**
    * Defines the size of the thread pool used to execute Hystrix commands.
    *
    * @return number of threads in the thread pool
    */
   int size() default DEFAULT_SIZE;

}
