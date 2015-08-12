package com.typesafe.training.coffeehouse;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.PartialFunction;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Random;

/**
 * Created by i839879 on 8/11/15.
 */
public class Barista extends AbstractActorWithStash /*AbstractLoggingActor*/ {

    private FiniteDuration prepareCoffeeDuration;

    int accuracy = 0;

    public Barista(FiniteDuration prepareCoffeeDuration, int accuracy) {
        this.prepareCoffeeDuration = prepareCoffeeDuration;
        this.accuracy = accuracy;
    }

    public PartialFunction<Object, BoxedUnit> receive() {
        return ready();
//        return ReceiveBuilder
//                .match(PrepareCoffee.class, pc -> prepareCoffee(pc))
//                .build();
    }

//    private void prepareCoffee(PrepareCoffee pc) {
//        if (new Random().nextInt(99) < accuracy) {
//            // Prepare the correct coffee
////            Thread.sleep(this.prepareCoffeeDuration.toMillis());
////            busy(this.prepareCoffeeDuration);
//            context().become(busy(sender()));
//            sender().tell(new CoffeePrepared(pc.coffee, pc.guest), self());
//        } else {
//            // Prepare a random coffee
////            Thread.sleep(this.prepareCoffeeDuration.toMillis());
////            busy(this.prepareCoffeeDuration);
//            sender().tell(new CoffeePrepared(Coffee.orderOther(pc.coffee), pc.guest), self());
//        }
//    }

//    private PartialFunction<Object, BoxedUnit> busy(ActorRef sender) {
//        return ReceiveBuilder.match(CoffeePrepared.class, cp -> {
//            sender.tell(cp, self());
//            unstashAll();
//            context().become(ready());
//        })
//                .matchAny(msg -> stash())
//                .build();
//    }

    private PartialFunction<Object,BoxedUnit> ready() {
        return ReceiveBuilder
                .match(PrepareCoffee.class, pc -> {
                    context().system().scheduler()
                            .scheduleOnce(prepareCoffeeDuration, self(),
                                    new CoffeePrepared(makeCoffee(pc.coffee), pc.guest),
                                        context().dispatcher(), sender()
                            );
                    context().become(busy());
                })
                .build();
    }

    private PartialFunction<Object, BoxedUnit> busy() {
        return ReceiveBuilder
                .match(CoffeePrepared.class, cp -> {
                    sender().tell(cp, self());
                    unstashAll();
                    context().become(ready());
                })
                .matchAny(o -> stash())
                .build();
    }

    private Coffee makeCoffee(Coffee coffee) {
        if (new Random().nextInt(99) < accuracy) {
            return coffee;
        } else {
           return Coffee.orderOther(coffee);
        }
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

    private void busy(FiniteDuration duration) {
        pi(duration.toMillis() * 800);
    }

    private BigDecimal pi(long m) {
        int n = 0;
        BigDecimal acc = new BigDecimal(0.0);
        while(n < m) {
            acc = acc.add(gregoryLeibnitz(n));
            n += 1;
        }
        return acc;
    }

    private BigDecimal gregoryLeibnitz(int n) {
        return new BigDecimal(4.0 * (1 - (n % 2) * 2) / (n * 2 + 1));
    }

}
