package com.typesafe.training.coffeehouse;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by i839879 on 8/11/15.
 */
public class Barista extends AbstractLoggingActor {

    private FiniteDuration prepareCoffeeDuration;

    int accuracy = 0;

    public Barista(FiniteDuration prepareCoffeeDuration, int accuracy) {
        this.prepareCoffeeDuration = prepareCoffeeDuration;
        this.accuracy = accuracy;
    }

    public PartialFunction<Object, BoxedUnit> receive() {
        return ReceiveBuilder
                .match(PrepareCoffee.class, pc -> {
                    if (new Random().nextInt(99) < accuracy) {
                        // Prepare the correct coffee
                        Thread.sleep(this.prepareCoffeeDuration.toMillis());
                        sender().tell(new Barista.CoffeePrepared(pc.coffee, pc.guest), self());
                    } else {
                        // Prepare a random coffee
                        Thread.sleep(this.prepareCoffeeDuration.toMillis());
                        sender().tell(new Barista.CoffeePrepared(Coffee.orderOther(pc.coffee), pc.guest), self());
                    }
                })
                .build();

    }

    public static Props props(FiniteDuration prepareCoffeeDuration, int accuracy) {
        return Props.create(Barista.class, () -> new Barista(prepareCoffeeDuration, accuracy));
    }

    public static class PrepareCoffee implements Serializable {
        Coffee coffee;
        ActorRef guest;

        public PrepareCoffee(Coffee coffee, ActorRef guest) {
            this.coffee = coffee;
            this.guest = guest;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PrepareCoffee that = (PrepareCoffee) o;

            if (!coffee.equals(that.coffee)) return false;
            return guest.equals(that.guest);

        }

        @Override
        public int hashCode() {
            int result = coffee.hashCode();
            result = 31 * result + guest.hashCode();
            return result;
        }
    }

    public static class CoffeePrepared implements Serializable {
        Coffee coffee;
        ActorRef guest;

        public CoffeePrepared(Coffee coffee, ActorRef guest) {
            this.coffee = coffee;
            this.guest = guest;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CoffeePrepared that = (CoffeePrepared) o;

            if (!coffee.equals(that.coffee)) return false;
            return guest.equals(that.guest);

        }

        @Override
        public int hashCode() {
            int result = coffee.hashCode();
            result = 31 * result + guest.hashCode();
            return result;
        }


    }

}
