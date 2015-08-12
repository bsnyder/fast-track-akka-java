package com.typesafe.training.coffeehouse;


import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.typesafe.training.coffeehouse.CoffeeHouseApp;
import org.assertj.core.data.MapEntry;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CoffeeHouseAppTest extends BaseAkkaTestCase {

  Timeout statusTimeout = Timeout.apply(100, TimeUnit.MILLISECONDS);
  @Test
  public void argsToOptsShouldConvertArgsToOpts() {
    final Map<String, String> result = CoffeeHouseApp.argsToOpts(Arrays.asList("a=1", "b", "-Dc=2"));
    assertThat(result).contains(MapEntry.entry("a", "1"), MapEntry.entry("-Dc", "2"));
  }


  @Test
  public void applySystemPropertiesShouldConvertOptsToSystemProps() {
    System.setProperty("c", "");
    Map<String, String> opts = new HashMap<>();
    opts.put("a", "1");
    opts.put("-Dc", "2");
    CoffeeHouseApp.applySystemProperties(opts);
    assertThat(System.getProperty("c")).isEqualTo("2");
  }

  @Test
  public void shouldCreateATopLevelActorCalledCoffeeHouse() {
    new JavaTestKit(system) {{
      new CoffeeHouseApp(system, statusTimeout);
      String path = "/user/coffee-house";
      expectActor(this, path);
    }};
  }

  @Test
  public void shouldCreateNGuestsBasedOnCount() {
    new JavaTestKit(system) {{
      new CoffeeHouseApp(system, statusTimeout) {
        @Override
        protected ActorRef createCoffeeHouse() {
          return getRef();
        }
      }.createGuest(2, new Coffee.Akkaccino(), Integer.MAX_VALUE);
      expectMsgAllOf(new CoffeeHouse.CreateGuest(new Coffee.Akkaccino(), Integer.MAX_VALUE),
              new CoffeeHouse.CreateGuest(new Coffee.Akkaccino(), Integer.MAX_VALUE));
    }};
  }

  @Test
  public void getStatusShouldResultInLoggingAskTimeoutExceptionWhenCoffeeHouseNotResponding() {
    new JavaTestKit(system) {{
      CoffeeHouseApp app = new CoffeeHouseApp(system, statusTimeout){
        @Override
        protected ActorRef createCoffeeHouse() {
          return system.deadLetters();
        }
      };
      interceptErrorLogMessage(this, ".*AskTimeoutException.*", Integer.MAX_VALUE, () -> {
        app.getStatus();
      });
    }};
  }

  @Test
  public void getStatusShouldResultInLoggingStatusAtInfo() {
    new JavaTestKit(system) {{
      CoffeeHouseApp app = new CoffeeHouseApp(system, statusTimeout){
        @Override
        protected ActorRef createCoffeeHouse() {
          return createStubActor("stub-coffee-house", () -> new AbstractActor() {{
            receive(
               ReceiveBuilder.match(CoffeeHouse.GetStatus.class, o -> {
                          sender().tell(new CoffeeHouse.Status(42), self());
                    }).build());
          }});
        }
      };
      interceptInfoLogMessage(this, ".*42.*", 1, () -> {
        app.getStatus();
      });
    }};
  }
}
