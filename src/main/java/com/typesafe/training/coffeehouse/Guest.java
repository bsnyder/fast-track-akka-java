package com.typesafe.training.coffeehouse;

import akka.actor.*;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.io.Serializable;

/**
 * Created by i839879 on 8/10/15.
 */
public class Guest extends AbstractLoggingActor {

    private final ActorRef waiter;

    private final Coffee favoriteCoffee;

    private int coffeeCount = 0;

    private int caffeineLimit = 0;

    private FiniteDuration finishCoffeeDuration;

    public Guest(ActorRef waiter, Coffee favoriteCoffee, FiniteDuration finishCoffeeDuration, int caffeineLimit) {
//        receive(ReceiveBuilder.matchAny(this::unhandled).build());
        this.favoriteCoffee = favoriteCoffee;
        this.waiter = waiter;
//    }
        this.finishCoffeeDuration = finishCoffeeDuration;
        this.caffeineLimit = caffeineLimit;

        waiter.tell(new Waiter.ServeCoffee(favoriteCoffee), self());
    }

    @Override
    public PartialFunction<Object, BoxedUnit> receive() {
        return ReceiveBuilder
                .match(Waiter.CoffeeServed.class, cs -> {
                    if (!cs.coffee.equals(favoriteCoffee)) {
                        waiter.tell(new Waiter.Complaint(favoriteCoffee), self());
                        log().info("Expected a {}, but got a {}!", favoriteCoffee, cs.coffee);
                    } else {
                        ++coffeeCount;
                        log().info("Enjoying my {} yummy {}!", coffeeCount, this.favoriteCoffee);
                        scheduleCoffeeFinished();
                    }
                })
                .match(CoffeeFinished.class, cf -> {
                    orderCoffee(waiter, favoriteCoffee);
                })
                .matchAny(o -> sender().tell("Serving coffee", self()))
                .build();

    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        log().info("Goodbye!");
    }

    private void orderCoffee(ActorRef waiter, Coffee favoriteCoffee) {
        if (coffeeCount > caffeineLimit) {
            System.out.println("Throwing CaffeineException");
            throw new CaffeineException();
        } else {
            waiter.tell(new Waiter.ServeCoffee(favoriteCoffee), self());
        }
    }

    private void scheduleCoffeeFinished() {
        context().system().scheduler().scheduleOnce(finishCoffeeDuration, self(), CoffeeFinished.Instance, context().dispatcher(), self());
    }

    public static Props props(ActorRef waiter, Coffee favoriteCoffee, FiniteDuration finishCoffeeDuration, int caffeineLimit) {
        return Props.create(Guest.class, () -> new Guest(waiter, favoriteCoffee, finishCoffeeDuration, caffeineLimit));
    }


    public static class CoffeeFinished implements Serializable {
        public static final CoffeeFinished Instance = new CoffeeFinished();
        public CoffeeFinished() {}
    }

    public static class CaffeineException extends IllegalStateException {

    }
}
