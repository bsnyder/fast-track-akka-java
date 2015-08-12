package com.typesafe.training.coffeehouse;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.Option;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

import java.io.Serializable;

/**
 * Created by i839879 on 8/10/15.
 */
public class Waiter extends AbstractLoggingActor {

    private ActorRef coffeeHouse;

    private ActorRef barista;

    private int maxComplaints = 0;

    private int complaintCount = 0;

    public Waiter(ActorRef coffeeHouse, ActorRef barista, int maxComplaintCount) {
        this.coffeeHouse = coffeeHouse;
        this.barista = barista;
        this.maxComplaints = maxComplaintCount;
    }

//    @Override
//    public void preRestart(Throwable reason, Option<Object> message) throws Exception {
//        super.preRestart(reason, message);
//        if(message.isDefined()) {
//            barista.tell(new Barista.PrepareCoffee((((Complaint)message.get()).coffee), sender()), self());
//        }
//
//    }

    public PartialFunction<Object, BoxedUnit> receive() {
        return ReceiveBuilder
            .match(ServeCoffee.class, sc -> coffeeHouse.tell(new CoffeeHouse.ApproveCoffee(sc.coffee, sender()), self()))
            .match(Barista.CoffeePrepared.class, cp -> cp.guest.tell(new Waiter.CoffeeServed(cp.coffee), self()))
//            .match(Complaint.class, a -> complaintCount > maxComplaints, b -> throw new FrustratedException())
//            .match(Complaint.class, c -> barista.tell(new Barista.PrepareCoffee(c.coffee, sender()), self()))
            .match(Complaint.class, c -> {
                if (complaintCount < maxComplaints) {
                    barista.tell(new Barista.PrepareCoffee(c.coffee, sender()), self());
                    ++complaintCount;
                } else {
                    log().info("Too many complaints, unable to make coffee {}", c.coffee);
                    log().info("George is getting frustrated!");
                    throw new FrustratedException(c.coffee, sender());
                }
            })
            .build();
    }

    public static Props props(ActorRef coffeeHouse, ActorRef barista, int maxComplaintCount) {
        return Props.create(Waiter.class, () -> new Waiter(coffeeHouse, barista, maxComplaintCount));
    }

    public static class ServeCoffee implements Serializable {
        public final Coffee coffee;
        public ServeCoffee(Coffee coffee) {
            this.coffee = coffee;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServeCoffee that = (ServeCoffee) o;

            return coffee.equals(that.coffee);

        }

        @Override
        public int hashCode() {
            return coffee.hashCode();
        }
    }

    public static class CoffeeServed implements Serializable {
        public final Coffee coffee;

        public CoffeeServed(Coffee coffee) {
            this.coffee = coffee;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CoffeeServed that = (CoffeeServed) o;

            return coffee.equals(that.coffee);

        }

        @Override
        public int hashCode() {
            return coffee.hashCode();
        }
    }

    public static class Complaint implements Serializable {
        Coffee coffee;

        public Complaint(Coffee coffee) {
            this.coffee = coffee;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Complaint complaint = (Complaint) o;

            return coffee.equals(complaint.coffee);

        }

        @Override
        public int hashCode() {
            return coffee.hashCode();
        }
    }

    public static class FrustratedException extends IllegalStateException {
        public Coffee coffee;
        public ActorRef guest;

        public FrustratedException(Coffee coffee, ActorRef guest) {
            this.coffee = coffee;
            this.guest = guest;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FrustratedException that = (FrustratedException) o;

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
