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
package io.silverware.microservices.providers.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.silverware.microservices.annotations.Microservice;
import io.silverware.microservices.annotations.MicroserviceReference;
import io.silverware.microservices.providers.cdi.CdiMicroserviceProvider;
import io.silverware.microservices.silver.HttpServerSilverService;
import io.silverware.microservices.util.BootUtil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Radek Koubsky (radek.koubsky@gmail.com)
 */
public class HttpServerMicroserviceProviderRestTest {
   private static final Logger log = LogManager.getLogger(HttpServerMicroserviceProviderRestTest.class);
   private Map<String, Object> platformProperties;
   private Client client;
   private Thread platform;

   @BeforeClass
   public void setUpPlatforn() throws InterruptedException {
      final BootUtil bootUtil = new BootUtil();
      this.platformProperties = bootUtil.getContext().getProperties();
      this.platformProperties.put(HttpServerSilverService.HTTP_SERVER_PORT, 8282);

      this.platform = bootUtil.getMicroservicePlatform(
            this.getClass().getPackage().getName(),
            CdiMicroserviceProvider.class.getPackage().getName());
      this.platform.start();

      while (bootUtil.getContext().getProperties().get(CdiMicroserviceProvider.BEAN_MANAGER) == null) {
         Thread.sleep(200);
      }
      this.client = ClientBuilder.newClient();
   }

   @AfterClass
   public void tearDown() throws InterruptedException {
      this.client.close();
      this.platform.interrupt();
      this.platform.join();
   }

   @Test
   public void lookupMicroserviceForRESTTest() throws Exception {
      assertThat(
            this.client
                  .target(baseURI(this.platformProperties) + "/helloservice/hello")
                  .request(MediaType.TEXT_PLAIN)
                  .get()
                  .readEntity(String.class)).as(
            "Rest microservice should return 'Hello from " + MicroserviceA.class.getName() + "'").isEqualTo(
            "Hello from " + MicroserviceA.class.getName());
   }

   @Test
   public void microserviceQueryParamsTest() throws Exception {
      assertThat(
            this.client
                  .target(baseURI(this.platformProperties) + "/helloservice/test_query_params?name=Radek&age=25")
                  .request(MediaType.TEXT_PLAIN)
                  .get()
                  .readEntity(String.class)).as("Rest microservice should return 'Radek;25'").isEqualTo("Radek;25");
   }

   @Test
   public void microserviceDefaultQueryParamsTest() throws Exception {
      assertThat(
            this.client
                  .target(baseURI(this.platformProperties) + "/helloservice/test_query_params")
                  .request(MediaType.TEXT_PLAIN)
                  .get()
                  .readEntity(String.class)).as("Rest microservice should return 'John;25'").isEqualTo("John;21");
   }

   @Test
   public void microservicePathParamTest() throws Exception {
      assertThat(
            this.client
                  .target(baseURI(this.platformProperties) + "/helloservice/5")
                  .request(MediaType.TEXT_PLAIN)
                  .get()
                  .readEntity(String.class)).as("Rest microservice should return '5' as path param.").isEqualTo("5");
   }

   @Test
   public void microserviceJsonTest() throws Exception {
      final Person person = new Person("John", "Collins", 30);
      assertThat(
            this.client
                  .target(baseURI(this.platformProperties) + "/helloservice/test_json")
                  .request(MediaType.APPLICATION_JSON)
                  .post(Entity.json(person))
                  .readEntity(Person.class)).as("Rest microservice should return:" + person).isEqualTo(person);
   }

   @Test
   public void microserviceXmlTest() throws Exception {
      final Person person = new Person("John", "Collins", 30);
      assertThat(
            this.client
                  .target(baseURI(this.platformProperties) + "/helloservice/test_xml")
                  .request(MediaType.APPLICATION_XML)
                  .post(Entity.xml(person))
                  .readEntity(Person.class)).as("Rest microservice should return:" + person).isEqualTo(person);
   }

   /**
    * @param platformProperties
    * @return base URI
    */
   private String baseURI(final Map<String, Object> platformProperties) {
      return "http://" + platformProperties.get(HttpServerSilverService.HTTP_SERVER_ADDRESS) + ":" + platformProperties
            .get(HttpServerSilverService.HTTP_SERVER_PORT) + platformProperties
            .get(HttpServerSilverService.HTTP_SERVER_REST_CONTEXT_PATH) + "/" + platformProperties
            .get(HttpServerSilverService.HTTP_SERVER_REST_SERVLET_MAPPING_PREFIX);
   }

   @Path("helloservice")
   @Microservice
   @Dependent
   public static class MyRestService {
      @Inject
      @MicroserviceReference
      private MicroserviceA msA;

      @GET
      @Produces(MediaType.TEXT_PLAIN)
      @Path("hello")
      public Response sayHello() {
         log.info("Redirecting to " + MicroserviceA.class.getName());
         return Response.ok(this.msA.hello()).build();
      }

      @GET
      @Path("test_query_params")
      @Produces(MediaType.TEXT_PLAIN)
      public Response printQueryParams(@QueryParam("name") @DefaultValue("John") final String name,
            @QueryParam("age") @DefaultValue("21") final int age) {
         return Response.ok(name + ";" + age).build();
      }

      @GET
      @Path("/{id}")
      @Produces(MediaType.TEXT_PLAIN)
      public Response pathParamTest(@PathParam("id") final long id) {
         return Response.ok().entity(id).build();
      }

      @POST
      @Path("/test_json")
      @Produces(MediaType.APPLICATION_JSON)
      @Consumes(MediaType.APPLICATION_JSON)
      public Response jsonTest(final Person person) {
         return Response.ok().entity(person).build();
      }

      @POST
      @Path("/test_xml")
      @Produces(MediaType.APPLICATION_XML)
      @Consumes(MediaType.APPLICATION_XML)
      public Response xmlTest(final Person person) {
         return Response.ok().entity(person).build();
      }

   }

   @Microservice
   public static class MicroserviceA {
      public String hello() {
         return "Hello from " + this.getClass().getName();
      };
   }

   @XmlRootElement
   public static class Person {
      @XmlElement(name = "name")
      private String name;
      @XmlElement(name = "surname")
      private String surname;
      @XmlElement(name = "age")
      private int age;

      public Person() {

      }

      /**
       * Ctor.
       *
       * @param name
       * @param surname
       * @param age
       */
      @JsonCreator
      public Person(@JsonProperty("name") final String name, @JsonProperty("surname") final String surname,
            @JsonProperty("age") final int age) {
         super();
         this.name = name;
         this.surname = surname;
         this.age = age;
      }

      public String getName() {
         return this.name;
      }

      public String getSurname() {
         return this.surname;
      }

      public int getAge() {
         return this.age;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + this.age;
         result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
         result = prime * result + ((this.surname == null) ? 0 : this.surname.hashCode());
         return result;
      }

      @Override
      public boolean equals(final Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         final Person other = (Person) obj;
         if (this.age != other.age)
            return false;
         if (this.name == null) {
            if (other.name != null)
               return false;
         } else if (!this.name.equals(other.name))
            return false;
         if (this.surname == null) {
            if (other.surname != null)
               return false;
         } else if (!this.surname.equals(other.surname))
            return false;
         return true;
      }

      @Override
      public String toString() {
         return String.format("Person [name=%s, surname=%s, age=%s]", this.name, this.surname, this.age);
      }
   }
}