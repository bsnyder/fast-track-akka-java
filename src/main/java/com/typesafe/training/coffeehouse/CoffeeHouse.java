package com.typesafe.training.coffeehouse;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import com.typesafe.config.Config;
import scala.PartialFunction;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by i839879 on 8/10/15.
 */
public class CoffeeHouse extends AbstractLoggingActor {

    private final Config config = context().system().settings().config();

    private final FiniteDuration guestFinishcoffeeDuration =
            Duration.create(config.getDuration("coffee-house.guest.finish-coffee-duration", TimeUnit.MILLISECONDS)
            , TimeUnit.MILLISECONDS);

    private final FiniteDuration prepareCoffeeDuration =
            Duration.create(config.getDuration("coffee-house.barista.prepare-coffee-duration", TimeUnit.MILLISECONDS)
                    , TimeUnit.MILLISECONDS);

    private int accuracy = config.getInt("coffee-house.barista.accuracy");

    private int maxComplaintCount = config.getInt("coffee-house.waiter.max-complaint-count");

    private ActorRef barista = createBarista();

    private ActorRef waiter = createWaiter();

    private int caffeineLimit = 0;

    private Map<ActorRef, Integer> guestBook = new ConcurrentHashMap<ActorRef, Integer>();

    public CoffeeHouse(int caffeineLimit) {
        this.caffeineLimit = caffeineLimit;
        log().debug("Coffee House open!");

//        loggingReceive(ReceiveBuilder
//            .matchAny(o -> sender().tell("Coffee brewingggg", self()))
//            .build()
//        );

//        log().error(exception, "Bar closed!");
//        receive(ReceiveBuilder
//                        .matchAny(o -> log().info("Coffee Brewing..."))
//                        .build()
//        );
    }


    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(true, DeciderBuilder
                .match(Guest.CaffeineException.class, e -> SupervisorStrategy.stop())
                .match(Throwable.class, t -> SupervisorStrategy.defaultDecider().apply(t))
                .build()
        );
    }


    public PartialFunction<Object, BoxedUnit> receive() {
        return ReceiveBuilder
                .match(CreateGuest.class, cg -> {
                    createGuest(waiter, cg.favoriteCoffee, cg.caffeineLimit);
                })
                .match(ApproveCoffee.class, ac -> {
                    approveCoffee(ac);
                })
                .match(Terminated.class, t -> {
                    //log().info("Thanks, {}, for being our guest!", t.getActor());
                    guestBook.remove(t.getActor());
                    log().info("Removed guest {} from the guest book", t.getActor());
                })
                .build();
    }

    private void approveCoffee(ApproveCoffee ac) {
        int caffeineCount = guestBook.get(ac.guest);
        if (caffeineCount <= caffeineLimit) {
//            barista.tell(new Barista.PrepareCoffee(ac.coffee, ac.guest), waiter);
            barista.forward(new Barista.PrepareCoffee(ac.coffee, ac.guest), getContext());
            log().info("Guest {} caffeine count incremented.", ac.guest.path());
            ++caffeineCount;
            guestBook.put(ac.guest, caffeineCount);
            context().watch(ac.guest);
        } else {
            log().info("Sorry, {}, but you have reached your limit.", ac.guest);
            context().stop(ac.guest);
        }
    }

    public static Props props(int caffeineLimit) {
        return Props.create(CoffeeHouse.class, () -> new CoffeeHouse(caffeineLimit));
    }

    /**
     * Log all messages received
     * @return
     */
//    private void loggingReceive(PartialFunction<Object, BoxedUnit> behavior) {
//        final PartialFunction<Object, BoxedUnit> unhandled =
//                ReceiveBuilder.matchAny(this::unhandled).build();
//
//        final PartialFunction<Object, BoxedUnit> logs =
//                ReceiveBuilder.matchAny(o -> {
//                    log().info(">>>>> Received message: '{}'",o.toString());
//                    behavior.orElse(unhandled).apply(o);
//                }).build();
//
//        receive(logs);
//    }

    protected ActorRef createBarista() {
        return getContext().actorOf(Barista.props(prepareCoffeeDuration, accuracy), "barista");
    }

    public ActorRef createWaiter() {
        return getContext().actorOf(Waiter.props(self(), barista, maxComplaintCount), "waiter");
    }

    public ActorRef createGuest(ActorRef waiter, Coffee favoriteCoffee, int caffeineLimit) {
        ActorRef guest = getContext().actorOf(
                Guest.props(waiter, favoriteCoffee, guestFinishcoffeeDuration, caffeineLimit)
        );
        addToGuestBook(guest, 0);
        return guest;
    }

    private void addToGuestBook(ActorRef guest, int caffieneCount) {
        guestBook.put(guest, 0);
        log().info("Guest {} added to book", guest);
    }

    public static class CreateGuest implements Serializable {
        public final Coffee favoriteCoffee;
        int caffeineLimit;
        //        public static final CreateGuest instance = new CreateGuest();
        public CreateGuest(Coffee favoriteCoffee, int caffeineLimit) {
            this.favoriteCoffee = favoriteCoffee;
            this.caffeineLimit = caffeineLimit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CreateGuest that = (CreateGuest) o;

            return favoriteCoffee.equals(that.favoriteCoffee);

        }

        @Override
        public int hashCode() {
            return favoriteCoffee.hashCode();
        }
    }

    public static class ApproveCoffee implements Serializable {
        Coffee coffee;
        ActorRef guest;
        public ApproveCoffee(Coffee coffee, ActorRef guest) {
            this.coffee = coffee;
            this.guest = guest;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ApproveCoffee that = (ApproveCoffee) o;

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
