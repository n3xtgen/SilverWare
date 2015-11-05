/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
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
package io.silverware.microservices.providers.cdi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import io.silverware.microservices.annotations.Microservice;
import io.silverware.microservices.annotations.MicroserviceReference;
import io.silverware.microservices.providers.cdi.builtin.Configuration;
import io.silverware.microservices.providers.cdi.builtin.CurrentContext;
import io.silverware.microservices.providers.cdi.builtin.Storage;
import io.silverware.microservices.providers.cdi.internal.MicroservicesInitEvent;
import io.silverware.microservices.util.BootUtil;

/**
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class CdiMicroserviceProviderServicesTest {

   private static final Logger log = LogManager.getLogger(CdiMicroserviceProviderServicesTest.class);

   private static final Semaphore semaphore = new Semaphore(0);

   @Test
   public void testStorageService() throws Exception {
      final BootUtil bootUtil = new BootUtil();
      bootUtil.getContext().getProperties().put("config.test", "jojo");
      final Thread platform = bootUtil.getMicroservicePlatform(this.getClass().getPackage().getName());
      platform.start();

      BeanManager beanManager = null;
      while (beanManager == null) {
         beanManager = (BeanManager) bootUtil.getContext().getProperties().get(CdiMicroserviceProvider.BEAN_MANAGER);
         Thread.sleep(200);
      }

      Assert.assertTrue(semaphore.tryAcquire(1, TimeUnit.MINUTES), "Timed-out while waiting for test completion.");

      Assert.assertEquals(bootUtil.getContext().getProperties().get("here"), "jojo");

      platform.interrupt();
      platform.join();
   }

   @Microservice
   public static class TestStorageMicroservice {

      @Inject
      @MicroserviceReference
      private Storage storage;

      @Inject
      @MicroserviceReference
      private Configuration config;

      @Inject
      @MicroserviceReference
      private CurrentContext context;

      public void eventObserver(@Observes MicroservicesStartedEvent event) {
         storage.put("test", config.getProperty("config.test"));
         context.getContext().getProperties().put("here", storage.get("test"));
         semaphore.release();
      }

   }
}